package org.taumc.celeritas.impl.render.terrain;

import com.gtnewhorizons.angelica.rendering.celeritas.api.IrisShaderProvider;
import com.gtnewhorizons.angelica.rendering.celeritas.api.IrisShaderProviderHolder;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.render.chunk.*;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.render.chunk.sprite.GenericSectionSpriteTicker;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.position.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.impl.render.terrain.fog.GLStateManagerFogService;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.impl.render.terrain.compile.VintageChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.compile.task.ChunkBuilderMeshingTask;
import org.taumc.celeritas.impl.render.terrain.sprite.SpriteUtil;
import org.taumc.celeritas.impl.world.WorldSlice;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;
import org.taumc.celeritas.impl.world.cloned.ClonedChunkSectionCache;

import java.util.List;

public class VintageRenderSectionManager extends RenderSectionManager {
    private final WorldClient world;
    @Getter
    private final ClonedChunkSectionCache sectionCache;

    public VintageRenderSectionManager(RenderPassConfiguration<?> configuration, WorldClient world, int renderDistance, CommandList commandList, int minSection, int maxSection) {
        super(configuration, () -> new VintageChunkBuildContext(world, configuration), ChunkRenderer::new, renderDistance, commandList, minSection, maxSection, CeleritasVintage.options().performance.chunkBuilderThreads, IrisShaderProviderHolder.isActive());
        this.world = world;
        this.sectionCache = new ClonedChunkSectionCache(world);
    }

    public static VintageRenderSectionManager create(ChunkVertexType vertexType, WorldClient world, int renderDistance, CommandList commandList) {
        return new VintageRenderSectionManager(VintageRenderPassConfigurationBuilder.build(vertexType), world, renderDistance, commandList, 0, 16);
    }

    // Devilica: the shadow pass draws its own lists; without this it drew the camera's sections into the shadow map,
    // and whatever the player turned away from lost its shadow
    @Override
    public boolean isInShadowPass() {
        return IrisShaderProviderHolder.isShadowPass();
    }

    @Override
    protected AsyncOcclusionMode getAsyncOcclusionMode() {
        return CeleritasVintage.options().performance.asyncOcclusionMode;
    }

    @Override
    protected boolean shouldRespectUpdateTaskQueueSizeLimit() {
        return true;
    }

    // Devilica: only under a shader pack, where the GPU is the bottleneck; without one the frame waits on the CPU and
    // the coverage buffer costs more than the terrain it hides
    @Override
    protected boolean useRasterOcclusionCulling() {
        return CeleritasVintage.options().performance.useRasterOcclusionCulling && IrisShaderProviderHolder.isActive();
    }

    @Override
    public FogService getFogService() {
        return GLStateManagerFogService.INSTANCE;
    }

    @Override
    protected boolean useFogOcclusion() {
        return CeleritasVintage.options().performance.useFogOcclusion;
    }

    @Override
    protected boolean shouldUseOcclusionCulling(Viewport positionedViewport, boolean spectator) {
        final boolean useOcclusionCulling;
        var camBlockPos = positionedViewport.getBlockCoord();
        BlockPos origin = new BlockPos(camBlockPos.x(), camBlockPos.y(), camBlockPos.z());

        if (spectator && this.world.getBlockState(origin).isOpaqueCube())
        {
            useOcclusionCulling = false;
        } else {
            useOcclusionCulling = Minecraft.getMinecraft().renderChunksMany;
        }

        return useOcclusionCulling;
    }

    @Override
    protected boolean isSectionVisuallyEmpty(int x, int y, int z) {
        Chunk chunk = this.world.getChunk(x, z);
        if (chunk.isEmpty()) {
            return true;
        }
        var array = chunk.getBlockStorageArray();
        if (y < 0 || y >= array.length) {
            return true;
        }
        return array[y] == Chunk.NULL_BLOCK_STORAGE || array[y].isEmpty();
    }

    @Override
    protected @Nullable ChunkBuilderTask<ChunkBuildOutput> createRebuildTask(RenderSection render, int frame) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, new SectionPos(render.getChunkX(), render.getChunkY(), render.getChunkZ()), this.sectionCache);

        if (context == null) {
            return null;
        }

        return new ChunkBuilderMeshingTask(render, context, frame, this.cameraPosition, this.useRasterOcclusionCulling());
    }

    @Override
    protected boolean allowImportantRebuilds() {
        return !CeleritasVintage.options().performance.alwaysDeferChunkUpdates;
    }

    @Override
    protected void scheduleSectionForRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);
        super.scheduleSectionForRebuild(x, y, z, important);
    }

    @Override
    public void updateChunks(boolean updateImmediately) {
        this.sectionCache.cleanup();
        super.updateChunks(updateImmediately);
    }

    /**
     * This ridiculous workaround is needed because some mods rely on side effects of calling getRenderBoundingBox
     * to initialize tile entity state. It can be removed if we start using getRenderBoundingBox for TE rendering
     * again.
     */
    @SuppressWarnings("unchecked")
    private static void retrieveBBForList(List<?> blockEntities) {
        if (!blockEntities.isEmpty()) {
            for (var be : (List<TileEntity>)blockEntities) {
                be.getRenderBoundingBox();
            }
        }
    }

    @Override
    protected boolean updateSectionInfo(RenderSection render, @Nullable BuiltRenderSectionData info) {
        if (info instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
            retrieveBBForList(mcData.culledBlockEntities);
            retrieveBBForList(mcData.globalBlockEntities);
        }
        return super.updateSectionInfo(render, info);
    }

    @Override
    protected @Nullable SectionTicker createSectionTicker() {
        return new GenericSectionSpriteTicker<>(SpriteUtil::markSpriteActive);
    }

    /**
     * Devilica: with a shader pack, Iris supplies the terrain programs (gbuffers_terrain, _cutout, _water, shadow)
     * through {@link IrisShaderProviderHolder}; as in Angelica's chunk renderer, they replace Celeritas's own for the
     * passes the pack covers.
     */
    private static class ChunkRenderer extends DefaultChunkRenderer {
        private GlProgram<? extends ChunkShaderInterface> irisProgram;

        public ChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
            super(device, renderPassConfiguration, GLStateManagerFogService.INSTANCE);

            final IrisShaderProvider provider = IrisShaderProviderHolder.getProvider();
            if (provider != null) {
                provider.setRenderPassConfiguration(renderPassConfiguration);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void begin(TerrainRenderPass pass) {
            final IrisShaderProvider provider = IrisShaderProviderHolder.getProvider();
            if (provider != null && provider.isShadersEnabled()) {
                final GlProgram<? extends ChunkShaderInterface> override = provider.getShaderOverride(pass);
                if (override != null) {
                    pass.startDrawing();
                    override.bind();
                    override.getInterface().setupState(pass);
                    this.activeProgram = (GlProgram<ChunkShaderInterface>) override;
                    this.irisProgram = override;
                    return;
                }
            }
            this.irisProgram = null;
            super.begin(pass);
        }

        @Override
        protected void end(TerrainRenderPass pass) {
            if (this.irisProgram != null) {
                this.irisProgram.getInterface().restoreState();
                this.irisProgram.unbind();
                this.irisProgram = null;
                this.activeProgram = null;
                pass.endDrawing();
                return;
            }
            super.end(pass);
        }

        @Override
        public boolean useBlockFaceCulling(){
            return CeleritasVintage.options().performance.useBlockFaceCulling && IrisShaderProviderHolder.shouldUseFaceCulling();
        }

        @Override
        protected void configureShaderInterface(ChunkShaderInterface shader) {
            if (this.irisProgram == null) {
                shader.setTextureSlot(ChunkShaderTextureSlot.BLOCK, 0);
                shader.setTextureSlot(ChunkShaderTextureSlot.LIGHT, 1);
            }
        }
    }
}
