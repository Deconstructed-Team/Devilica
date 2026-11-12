package org.taumc.celeritas.impl.render.terrain.compile.task;

import com.gtnewhorizons.angelica.rendering.celeritas.api.IrisShaderProviderHolder;
import com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext;
import dev.feldsher.devilica.coloredlight.ColoredLight;
import dev.feldsher.devilica.coloredlight.ColoredLightField;
import dev.feldsher.devilica.ctm.BetterSnow;
import dev.feldsher.devilica.ctm.ConnectedTextures;
import net.coderbot.iris.vertices.ExtendedDataHelper;
import net.minecraft.util.EnumBlockRenderType;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import org.embeddedt.embeddium.impl.asm.ProxyClassGenerator;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionVisibilityBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.joml.Vector3d;
import org.taumc.celeritas.impl.compat.fluidlogged.FluidloggedCompat;
import org.taumc.celeritas.impl.render.terrain.compile.VintageChunkBuildContext;
import org.taumc.celeritas.impl.world.WorldSlice;
import org.taumc.celeritas.impl.world.cloned.CeleritasBlockAccess;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;

import java.util.*;

public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
    public static boolean USE_NEW_BLOCK_RENDERER = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment") || Boolean.getBoolean("celeritas.useVintageFastBlockRenderer");
    private static final ProxyClassGenerator<WorldSlice, CeleritasBlockAccess> WORLD_SLICE_LOCAL_GENERATOR = new ProxyClassGenerator<>(WorldSlice.class, "WorldSliceLocal", CeleritasBlockAccess.class);
    private final RenderSection render;
    private final int buildTime;
    private final Vector3d camera;
    private final ChunkRenderContext renderContext;
    private final boolean rasterOcclusion;

    public ChunkBuilderMeshingTask(RenderSection render, ChunkRenderContext context, int time, Vector3d camera, boolean rasterOcclusion) {
        this.render = render;
        this.buildTime = time;
        this.camera = camera;
        this.renderContext = context;
        this.rasterOcclusion = rasterOcclusion;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext context, CancellationToken cancellationToken) {
        VintageChunkBuildContext buildContext = (VintageChunkBuildContext)context;
        MinecraftBuiltRenderSectionData<TextureAtlasSprite, TileEntity> renderData = new MinecraftBuiltRenderSectionData<>();
        SectionVisibilityBuilder occluder = new SectionVisibilityBuilder();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getSectionIndex());

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(minX, minY, minZ);

        buildContext.getWorldSlice().copyData(this.renderContext);

        // Devilica: the colour of the block light, for the tinted vertex type
        final ColoredLightField coloredLight = ColoredLight.isInUse() ? ColoredLightField.forThread() : null;
        if (coloredLight != null) {
            coloredLight.compute(buildContext.getWorldSlice(), minX, minY, minZ);
        }

        var slice = WORLD_SLICE_LOCAL_GENERATOR.generateWrapper(buildContext.getWorldSlice());

        var dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

        buildContext.setupTranslation(minX, minY, minZ);

        final boolean shaders = IrisShaderProviderHolder.isActive();
        final BlockRenderContext irisBlock = BlockRenderContext.CURRENT.get();
        // Devilica: the pack's layer.* lines in block.properties move blocks to another pass (Complementary does it for
        // glass on Angelica); the block is still drawn as its own layers, into that pass's buffer
        final Map<Block, BlockRenderLayer> layerOverrides = shaders ? IrisShaderProviderHolder.getProvider().getBlockTypeIds() : null;

        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        blockPos.setPos(x, y, z);

                        IBlockState blockState = slice.getBlockState(blockPos);
                        var block = blockState.getBlock();

                        if (block == Blocks.AIR) {
                            continue;
                        }

                        if (this.rasterOcclusion) {
                            occluder.markRenderable(x, y, z);
                        }

                        if (block.hasTileEntity(blockState)) {
                            TileEntity tileEntity = slice.getTileEntity(blockPos);
                            if (tileEntity != null) {
                                TileEntitySpecialRenderer<TileEntity> tesr = TileEntityRendererDispatcher.instance.getRenderer(tileEntity);

                                if (tesr != null) {
                                    final boolean global = tesr.isGlobalRenderer(tileEntity);
                                    (global ? renderData.globalBlockEntities : renderData.culledBlockEntities).add(tileEntity);
                                    // Devilica: a block entity is drawn while its section is, and its renderer may draw past the block
                                    if (this.rasterOcclusion && !global) {
                                        occluder.markSectionRenderable();
                                    }
                                }
                            }
                        }

                        buildContext.getBlockRenderer().resetSharedState();
                        if (coloredLight != null) {
                            coloredLight.setBlock(blockState);
                        }

                        // Devilica: better snow, a snow layer under plants and fences standing in snow
                        if (BetterSnow.shouldRender(slice, blockState, blockPos)) {
                            if (shaders) {
                                irisBlock.setBlock(Blocks.SNOW_LAYER, 0, x & 15, y & 15, z & 15, ExtendedDataHelper.BLOCK_RENDER_TYPE, (byte) 0);
                            }
                            ForgeHooksClient.setRenderLayer(BlockRenderLayer.SOLID);
                            var buffer = buildContext.getBufferForLayer(BlockRenderLayer.SOLID);
                            final int verticesBefore = buffer.getVertexCount();
                            dispatcher.renderBlock(BetterSnow.SNOW, blockPos, slice, buffer);
                            if (shaders) {
                                buildContext.recordVanillaQuads(BlockRenderLayer.SOLID, verticesBefore, buffer.getVertexCount());
                                irisBlock.reset();
                            }
                            buildContext.getBlockRenderer().resetSharedState();
                        }

                        // Devilica: the shader pack's vertex attributes need the block being meshed
                        if (shaders) {
                            final boolean fluid = blockState.getRenderType() == EnumBlockRenderType.LIQUID;
                            irisBlock.setBlock(blockState, slice, blockPos,
                                fluid ? ExtendedDataHelper.FLUID_RENDER_TYPE : ExtendedDataHelper.BLOCK_RENDER_TYPE,
                                (byte) blockState.getLightValue(slice, blockPos));
                        }

                        // Devilica: passes a block is drawn in only for its overlays and glowing textures
                        final int extraLayers = ConnectedTextures.extraLayers(blockState);
                        final BlockRenderLayer override = layerOverrides != null ? layerOverrides.get(block) : null;
                        for (BlockRenderLayer layer : VintageChunkBuildContext.LAYERS) {
                            final boolean own = block.canRenderInLayer(blockState, layer);
                            if (own || (extraLayers & 1 << layer.ordinal()) != 0) {
                                ForgeHooksClient.setRenderLayer(layer);
                                final BlockRenderLayer target = own && override != null ? override : layer;
                                // Model rendering is redirected to the fast block renderer by BlockRendererDispatcherMixin
                                var buffer = buildContext.getBufferForLayer(target);
                                final int verticesBefore = buffer.getVertexCount();
                                dispatcher.renderBlock(blockState, blockPos, slice, buffer);
                                if (shaders) {
                                    buildContext.recordVanillaQuads(target, verticesBefore, buffer.getVertexCount());
                                }
                            }
                        }

                        if (shaders) {
                            irisBlock.reset();
                        }

                        if (FluidloggedCompat.IS_LOADED) {
                            FluidloggedCompat.renderFluidState(slice, blockPos, blockState, buildContext, dispatcher);
                        }

                        if (blockState.isOpaqueCube()) {
                            occluder.markOpaque(x, y, z);
                        }
                    }
                }
            }
        } catch (ReportedException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getCrashReport(), slice, blockPos);
        } catch (Throwable ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.makeCrashReport(ex, "Encountered exception while building chunk meshes"), slice, blockPos);
        }

        if (coloredLight != null) {
            coloredLight.endBlocks();
        }
        buildContext.convertVanillaDataToCeleritasData(buffers);

        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buffers, (float) (camera.x - minX), (float) (camera.y - minY), (float) (camera.z - minZ));

        if (!meshes.isEmpty()) {
            renderData.hasBlockGeometry = true;
        }

        if (this.rasterOcclusion) {
            renderData.occluderBoxes = occluder.computeOccluderBoxes();
        }

        renderData.visibilityData = occluder.computeVisibilityEncoding();

        return new ChunkBuildOutput(this.render, renderData, meshes, this.buildTime);
    }

    private ReportedException fillCrashInfo(CrashReport report, IBlockAccess slice, BlockPos pos) {
        CrashReportCategory crashReportSection = report.makeCategory("Block being rendered");

        IBlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportCategory.addBlockInfo(crashReportSection, pos, state);

        crashReportSection.addCrashSection("Chunk section", this.render);
        /*
        if (this.renderContext != null) {
            crashReportSection.addCrashSection("Render context volume", this.renderContext.getVolume());
        }

         */

        return new ReportedException(report);
    }
}
