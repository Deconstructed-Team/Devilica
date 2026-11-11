package org.taumc.celeritas.impl.render.terrain.compile.pipeline;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import com.gtnewhorizons.angelica.rendering.celeritas.api.IrisShaderProviderHolder;
import dev.feldsher.devilica.colors.CustomColors;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.pipeline.VertexBufferConsumer;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BakedQuadGroupAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.impl.render.terrain.compile.VintageChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.compile.light.LightDataCache;
import org.taumc.celeritas.impl.render.terrain.compile.light.VintageDiffuseProvider;
import org.taumc.celeritas.impl.world.cloned.CeleritasBlockAccess;
import org.taumc.celeritas.mixin.core.terrain.BlockColorsAccessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class VintageBlockRenderer {
    private final VintageChunkBuildContext context;
    private final VertexBufferConsumer consumer;
    private final LightPipelineProvider lighters;
    private final Map<IRegistryDelegate<Block>, IBlockColor> blockColors;

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private final ModelQuadOrientation[] currentOrientations = new ModelQuadOrientation[EnumFacing.VALUES.length];
    private final boolean useRenderPassOptimization;

    private IBlockState currentState;
    private CeleritasBlockAccess currentBlockAccess;

    private final BakedQuadGroupAnalyzer analyzer = new BakedQuadGroupAnalyzer();

    private int currentQuadRenderingFlags;

    // Devilica: a shader pack shades faces itself (oldLighting=false) and may want ambient occlusion in the vertex
    // alpha (separateAo=true); without it vanilla's dimming is baked in under the pack's own lighting
    private boolean shadeFaces = true;
    private ChunkColorWriter colorWriter = ChunkColorWriter.EMBEDDIUM;

    public VintageBlockRenderer(VintageChunkBuildContext context, LightDataCache cache) {
        this.consumer = new VertexBufferConsumer();
        this.context = context;
        this.lighters = new LightPipelineProvider(cache, VintageDiffuseProvider.INSTANCE, true);
        this.blockColors = ((BlockColorsAccessor)Minecraft.getMinecraft().getBlockColors()).getBlockColorMap();
        this.useRenderPassOptimization = CeleritasVintage.options().performance.useRenderPassOptimization;
    }

    public void resetSharedState() {
        Arrays.fill(this.currentOrientations, null);
    }

    /**
     * Equivalent of {@link net.minecraft.client.renderer.BlockModelRenderer#renderModel}, so the state must already be
     * the extended state.
     *
     * @return whether any quads were emitted
     */
    public boolean renderModel(IBakedModel model, IBlockState state, BlockPos pos, CeleritasBlockAccess blockAccess, BlockRenderLayer layer) {
        int defaultFlags = BakedQuadGroupAnalyzer.USE_ALL_THINGS;
        if (!useRenderPassOptimization) {
            defaultFlags &= ~BakedQuadGroupAnalyzer.USE_RENDER_PASS_OPTIMIZATION;
        }
        this.analyzer.setDefaultRenderingFlags(defaultFlags);

        boolean rendered = false;
        this.currentState = state;
        this.currentBlockAccess = blockAccess;

        final boolean shaders = IrisShaderProviderHolder.isActive();
        this.shadeFaces = !shaders || !BlockRenderingSettings.INSTANCE.shouldDisableDirectionalShading();
        this.colorWriter = shaders && BlockRenderingSettings.INSTANCE.shouldUseSeparateAo() ? ChunkColorWriter.SEPARATE_AO : ChunkColorWriter.EMBEDDIUM;

        var buffers = this.context.buffers;
        var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(layer);
        var buffer = buffers.get(material);

        long rand = MathHelper.getPositionRandom(pos);

        // Devilica: resource packs' colour maps
        IBlockColor colorProvider = CustomColors.provider(state, this.blockColors.get(state.getBlock().delegate));

        var offset = this.currentState.getOffset(blockAccess, pos);

        var aoEnabled = Minecraft.isAmbientOcclusionEnabled() && state.getLightValue(blockAccess, pos) == 0 && model.isAmbientOcclusion(state);

        var lighter = this.lighters.getLighter(aoEnabled ? LightMode.SMOOTH : LightMode.FLAT);

        for (var dir : EnumFacing.VALUES) {
            var quads = model.getQuads(state, dir, rand);

            if (quads.isEmpty() || !state.shouldSideBeRendered(blockAccess, pos, dir)) {
                continue;
            }

            this.currentQuadRenderingFlags = this.analyzer.getFlagsForRendering(VintageDiffuseProvider.fromEnumFacing(dir), BakedQuadView.ofList(quads));
            renderQuadList(buffer, buffers, material, pos, dir, lighter, colorProvider, offset, quads);
            rendered = true;
        }

        var quads = model.getQuads(state, null, rand);

        if (!quads.isEmpty()) {
            this.currentQuadRenderingFlags = this.analyzer.getFlagsForRendering(ModelQuadFacing.UNASSIGNED, BakedQuadView.ofList(quads));
            renderQuadList(buffer, buffers, material, pos, null, lighter, colorProvider, offset, quads);
            rendered = true;
        }

        this.currentBlockAccess = null;

        return rendered;
    }

    private QuadLightData getVertexLight(LightPipeline lighter, BlockPos pos, EnumFacing cullFace, BakedQuadView quad) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos.getX(), pos.getY(), pos.getZ(), light, VintageDiffuseProvider.fromEnumFacingOrUnassigned(cullFace), quad.getLightFace(), quad.hasShade() && this.shadeFaces, false);

        return light;
    }

    private int[] getVertexColors(BlockPos pos, IBlockColor colorProvider, BakedQuadView quad) {
        final int[] vertexColors = this.quadColors;

        // Devilica: quads tinted by resource packs take their colour through the block colours
        final IBlockColor provider = quad.hasColor() ? CustomColors.forQuad(colorProvider, quad.getColorIndex()) : null;
        if (provider != null) {
            // Force full alpha on all colors
            Arrays.fill(vertexColors, 0xFF000000 | ColorARGB.toABGR(provider.colorMultiplier(this.currentState, this.currentBlockAccess, pos, quad.getColorIndex())));
        } else {
            Arrays.fill(vertexColors, 0xFFFFFFFF);
        }

        return vertexColors;
    }

    private void renderQuadList(ChunkModelBuilder defaultBuffer, ChunkBuildBuffers buffers,
                                Material material, BlockPos pos, EnumFacing cullFace,
                                LightPipeline lighter, IBlockColor colorProvider, Vec3d offset,
                                List<BakedQuad> quads) {
        int localX = pos.getX() & 15, localY = pos.getY() & 15, localZ = pos.getZ() & 15;
        var config = buffers.getRenderPassConfiguration();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            var quad = quads.get(i);
            var format = quad.getFormat();
            if (format != DefaultVertexFormats.ITEM && format != DefaultVertexFormats.BLOCK && format.getElement(0).getUsage() != VertexFormatElement.EnumUsage.POSITION) {
                throw new IllegalStateException("Vertex format does not have POSITION as first element: " + format);
            }

            BakedQuadView quadView = (BakedQuadView)quad;

            // Run through our light pipeline
            var light = this.getVertexLight(lighter, pos, cullFace, quadView);
            var colors = this.getVertexColors(pos, colorProvider, quadView);

            ModelQuadOrientation orientation = cullFace != null ? this.currentOrientations[cullFace.ordinal()] : ModelQuadOrientation.NORMAL;
            if (orientation == null) {
                this.currentOrientations[cullFace.ordinal()] = orientation = ModelQuadOrientation.orientByBrightness(light.br, light.lm);
            }

            var quadMaterial = BakedQuadGroupAnalyzer.chooseOptimalMaterial(this.currentQuadRenderingFlags, material, config, BakedQuadView.of(quad));
            ChunkModelBuilder buffer = (quadMaterial == material) ? defaultBuffer : buffers.get(quadMaterial);

            this.writeGeometry(localX, localY, localZ, buffer, offset, quadMaterial,
                    quadView, colors, light, orientation);

            TextureAtlasSprite sprite = (TextureAtlasSprite)quadView.celeritas$getSprite();

            if (sprite.hasAnimationMetadata() && buffer.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                //noinspection unchecked
                ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
            }
        }
    }

    private void writeGeometry(int localX, int localY, int localZ, ChunkModelBuilder builder,
                               Vec3d offset,
                               Material material,
                               BakedQuadView quad,
                               int[] colors,
                               QuadLightData light,
                               ModelQuadOrientation orientation)
    {
        var vertices = this.vertices;

        ModelQuadFacing normalFace = quad.getNormalFace();

        int vanillaNormal = normalFace.getPackedNormal();
        int trueNormal = quad.getComputedFaceNormal();

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            var out = vertices[dstIndex];
            out.x = localX + quad.getX(srcIndex) + (float) offset.x;
            out.y = localY + quad.getY(srcIndex) + (float) offset.y;
            out.z = localZ + quad.getZ(srcIndex) + (float) offset.z;

            out.color = this.colorWriter.writeColor(ModelQuadUtil.mixARGBColors(colors[srcIndex], quad.getColor(srcIndex)), light.br[srcIndex]);

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), quad.getVanillaLightEmission(), light.lm[srcIndex]);

            out.vanillaNormal = vanillaNormal;
            out.trueNormal = trueNormal;
        }

        var vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);
    }
}
