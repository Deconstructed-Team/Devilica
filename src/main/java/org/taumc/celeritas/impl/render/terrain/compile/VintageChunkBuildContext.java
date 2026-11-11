package org.taumc.celeritas.impl.render.terrain.compile;

//? if 1.10.2 {
//?}
import com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.impl.extensions.TextureMapExtension;
import org.taumc.celeritas.impl.render.terrain.compile.light.LightDataCache;
import org.taumc.celeritas.impl.render.terrain.compile.pipeline.VintageBlockRenderer;
import org.taumc.celeritas.impl.world.WorldSlice;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class VintageChunkBuildContext extends ChunkBuildContext {
    public static final BlockRenderLayer[] LAYERS = BlockRenderLayer.values();
    private final TextureMapExtension textureAtlas;
    private final net.minecraft.client.renderer.BufferBuilder[] worldRenderers = new net.minecraft.client.renderer.BufferBuilder[LAYERS.length];
    private final boolean[] usedWorldRenderers = new boolean[LAYERS.length];
    @Getter
    private int offX, offY, offZ;
    @Getter
    private final WorldSlice worldSlice;
    @Getter
    private final VintageBlockRenderer blockRenderer;
    private final RenderPassConfiguration<?> renderPassConfiguration;
    private final LightDataCache lightDataCache;
    private final boolean useRenderPassOptimization;

    /**
     * Devilica: per layer, runs of vanilla-buffer quads and the block they belong to, as (end quad, block ID, render
     * type, light, packed local position). Blocks that draw through a vanilla {@code BufferBuilder} (fluids, mods'
     * renderers) are converted after the whole section is meshed; these runs give each converted quad its block back
     * for the shader pack's vertex attributes. Filled only while a shader pack is active.
     */
    private final int[][] quadRuns = new int[LAYERS.length][64];
    private final int[] quadRunCount = new int[LAYERS.length];

    public VintageChunkBuildContext(WorldClient world, RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
        this.renderPassConfiguration = renderPassConfiguration;
        this.worldSlice = new WorldSlice(world);
        this.lightDataCache = new LightDataCache(this.worldSlice);
        this.blockRenderer = new VintageBlockRenderer(this, lightDataCache);
        this.textureAtlas = (TextureMapExtension) Minecraft.getMinecraft().getTextureMapBlocks();
        this.useRenderPassOptimization = CeleritasVintage.options().performance.useRenderPassOptimization;
    }

    public void setupTranslation(int x, int y, int z) {
        this.lightDataCache.reset(x, y, z);

        this.offX = x;
        this.offY = y;
        this.offZ = z;
    }

    public net.minecraft.client.renderer.BufferBuilder getBufferForLayer(BlockRenderLayer layer) {
        var builder = this.worldRenderers[layer.ordinal()];
        if (builder == null) {
            builder = new ChunkBufferBuilder(131072, this, layer);
            this.worldRenderers[layer.ordinal()] = builder;
        }
        if (!this.usedWorldRenderers[layer.ordinal()]) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            builder.setTranslation(-this.offX, -this.offY, -this.offZ);
            this.usedWorldRenderers[layer.ordinal()] = true;
        }
        return builder;
    }

    /**
     * Records that the vanilla buffer of {@code layer} received vertices {@code [startVertex, endVertex)} for the block
     * now in {@link BlockRenderContext#CURRENT}.
     */
    public void recordVanillaQuads(BlockRenderLayer layer, int startVertex, int endVertex) {
        if (endVertex <= startVertex) {
            return;
        }
        final int index = layer.ordinal();
        int[] runs = this.quadRuns[index];
        final int at = this.quadRunCount[index] * 5;
        if (at + 5 > runs.length) {
            runs = Arrays.copyOf(runs, runs.length * 2);
            this.quadRuns[index] = runs;
        }
        final BlockRenderContext ctx = BlockRenderContext.CURRENT.get();
        runs[at] = endVertex / 4;
        runs[at + 1] = ctx.blockId;
        runs[at + 2] = ctx.renderType;
        runs[at + 3] = ctx.lightValue;
        runs[at + 4] = (ctx.localPosX & 15) | ((ctx.localPosY & 15) << 4) | ((ctx.localPosZ & 15) << 8);
        this.quadRunCount[index]++;
    }

    public void convertVanillaDataToCeleritasData(ChunkBuildBuffers buffers) {
        var renderers = this.worldRenderers;
        var used = this.usedWorldRenderers;
        for (int i = 0; i < renderers.length; i++) {
            if(!used[i]) {
                continue;
            }
            var bufferBuilder = Objects.requireNonNull(renderers[i]);
            bufferBuilder.finishDrawing();
            used[i] = false;
            ByteBuffer rawBuffer = bufferBuilder.getByteBuffer();
            var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(LAYERS[i]);
            copyBlockData(rawBuffer, buffers, material, i);
            this.quadRunCount[i] = 0;
        }
        BlockRenderContext.CURRENT.get().reset();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.worldSlice.reset();
        for (int i = 0; i < LAYERS.length; i++) {
            if (this.usedWorldRenderers[i]) {
                this.worldRenderers[i].finishDrawing();
                this.usedWorldRenderers[i] = false;
            }
            this.quadRunCount[i] = 0;
        }
    }

    private Material selectMaterial(Material material, TextureAtlasSprite sprite) {
        if (sprite != null && sprite.getClass() == TextureAtlasSprite.class && !sprite.hasAnimationMetadata() && this.useRenderPassOptimization) {
            var transparencyLevel = ((SpriteTransparencyLevel.Holder)sprite).embeddium$getTransparencyLevel();
            if (transparencyLevel == SpriteTransparencyLevel.OPAQUE) {
                // Downgrade to solid
                return this.renderPassConfiguration.defaultSolidMaterial();
            } else if (material == this.renderPassConfiguration.defaultTranslucentMaterial() && transparencyLevel != SpriteTransparencyLevel.TRANSLUCENT) {
                // Downgrade to cutout
                return this.renderPassConfiguration.defaultCutoutMippedMaterial();
            }
        }
        return material;
    }

    private static final int BLOCK_VERTEX_FORMAT_SIZE;

    static {
        var format = DefaultVertexFormats.BLOCK;
        int size = 0;
        for (int i = 0; i < format.getElementCount(); i++) {
            size += format.getElement(i).getSize();
        }
        BLOCK_VERTEX_FORMAT_SIZE = size;
    }

    private void copyBlockData(ByteBuffer source, ChunkBuildBuffers buffers, Material material, int layerIndex) {
        int vsize = BLOCK_VERTEX_FORMAT_SIZE;
        int numQuads = source.limit() / (vsize * 4);
        long ptr = LWJGL.memAddress(source);
        var quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
        var animatedSpritesList = ((MinecraftBuiltRenderSectionData<TextureAtlasSprite, TileEntity>)buffers.getSectionContextBundle()).animatedSprites;
        final int[] runs = this.quadRuns[layerIndex];
        final int runCount = this.quadRunCount[layerIndex];
        final BlockRenderContext ctx = runCount > 0 ? BlockRenderContext.CURRENT.get() : null;
        int run = -1;
        int runStart = 0;
        int runEnd = 0;
        for(int q = 0; q < numQuads; q++) {
            if (ctx != null) {
                while (q >= runEnd && run + 1 < runCount) {
                    run++;
                    runStart = runEnd;
                    runEnd = runs[run * 5];
                }
                if (q >= runStart && q < runEnd) {
                    final int packed = runs[run * 5 + 4];
                    ctx.set(packed & 15, (packed >> 4) & 15, (packed >> 8) & 15, (short) runs[run * 5 + 1], (short) runs[run * 5 + 2], (byte) runs[run * 5 + 3]);
                } else {
                    ctx.reset();
                }
            }
            float uSum = 0, vSum = 0;
            for(int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.x = LWJGL.memGetFloat(ptr);
                vertex.y = LWJGL.memGetFloat(ptr + 4);
                vertex.z = LWJGL.memGetFloat(ptr + 8);
                vertex.color = LWJGL.memGetInt(ptr + 12);
                vertex.u = LWJGL.memGetFloat(ptr + 16);
                vertex.v = LWJGL.memGetFloat(ptr + 20);
                uSum += vertex.u;
                vSum += vertex.v;
                vertex.light = LWJGL.memGetInt(ptr + 24);
                ptr += vsize;
            }
            TextureAtlasSprite sprite = this.textureAtlas.celeritas$findFromUV(uSum * 0.25f, vSum * 0.25f);
            if (sprite != null && sprite.hasAnimationMetadata()) {
                animatedSpritesList.add(sprite);
            }
            int trueNormal = QuadUtil.calculateNormal(quad);
            for (int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.vanillaNormal = trueNormal;
                vertex.trueNormal = trueNormal;
            }
            ModelQuadFacing facing = QuadUtil.findNormalFace(trueNormal);
            Material correctMaterial = selectMaterial(material, sprite);
            buffers.get(correctMaterial).getVertexBuffer(facing).push(quad, correctMaterial);
        }
    }
}
