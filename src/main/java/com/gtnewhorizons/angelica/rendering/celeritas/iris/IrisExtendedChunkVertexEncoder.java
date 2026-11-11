package com.gtnewhorizons.angelica.rendering.celeritas.iris;

import static org.lwjgl.system.MemoryUtil.*;
import static com.gtnewhorizons.angelica.rendering.celeritas.iris.IrisExtendedChunkVertexType.STRIDE;
import static com.gtnewhorizons.angelica.rendering.celeritas.iris.IrisExtendedChunkVertexType.encodeMidTexture;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import lombok.Setter;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.vertices.ExtendedDataHelper;
import net.coderbot.iris.vertices.NormalHelper;
import net.coderbot.iris.vertices.NormI8;
import net.minecraft.block.Block;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.VanillaLikeChunkVertex;
import org.joml.Vector3f;

/**
 * Writes Iris-extended vertices: VANILLA_LIKE's first 28 bytes, then the Iris attributes.
 *
 * Devilica: Celeritas's newer common hands the encoder a whole primitive, so the per-quad values (mid texture
 * coordinate, block ID, normal, tangent) are written once all four vertices are in, as Angelica did on its fourth.
 */
public class IrisExtendedChunkVertexEncoder implements ContextAwareChunkVertexEncoder {
    // Offsets derived from format definition
    private static final int MID_TEX_OFFSET = IrisExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("mc_midTexCoord").getPointer();
    private static final int TANGENT_OFFSET = IrisExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("at_tangent").getPointer();
    private static final int NORMAL_OFFSET = IrisExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("iris_Normal").getPointer();
    private static final int MC_ENTITY_OFFSET = IrisExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("mc_Entity").getPointer();
    private static final int MID_BLOCK_OFFSET = IrisExtendedChunkVertexType.VERTEX_FORMAT.getAttribute("at_midBlock").getPointer();

    private final CeleritasQuadView quad = new CeleritasQuadView();
    private final Vector3f normal = new Vector3f();
    private final Reference2ObjectMap<Block, Int2IntMap> blockMetaMatches;


    public IrisExtendedChunkVertexEncoder() {
        this.blockMetaMatches = BlockRenderingSettings.INSTANCE.getBlockMetaMatches();
    }

    @Setter
    private BlockRenderContext context;

    @Override
    public void prepareToRenderBlock(BlockRenderContext ctx, Block block, int metadata, short renderType, byte lightValue) {
        this.context = ctx;
        Int2IntMap metaMap = blockMetaMatches != null ? blockMetaMatches.get(block) : null;
        ctx.blockId = (short) (metaMap != null ? metaMap.get(metadata) : -1);
        ctx.renderType = renderType;
        ctx.lightValue = lightValue;
    }

    @Override
    public void prepareToRenderFluid(BlockRenderContext ctx, Block block, byte lightValue) {
        this.context = ctx;
        Int2IntMap metaMap = blockMetaMatches != null ? blockMetaMatches.get(block) : null;
        ctx.blockId = (short) (metaMap != null ? metaMap.get(0) : -1);
        ctx.renderType = ExtendedDataHelper.FLUID_RENDER_TYPE;
        ctx.lightValue = lightValue;
    }

    @Override
    public void finishRenderingBlock() {
        if (context != null) {
            context.reset();
        }
    }

    @Override
    public boolean supportsBilinearCorrection() {
        return false;
    }

    @Override
    public long writePrimitive(long ptr, Material material, Vertex[] vertices, int sectionIndex) {
        if (context == null) {
            context = BlockRenderContext.CURRENT.get();
        }
        final BlockRenderContext ctx = context;
        float uSum = 0;
        float vSum = 0;
        long vertexPtr = ptr;

        for (Vertex vertex : vertices) {
            uSum += vertex.u;
            vSum += vertex.v;

            VanillaLikeChunkVertex.writeVertexWithoutRdhFactor(vertexPtr, material, vertex, sectionIndex);

            // Per-vertex: renderType, midBlock, lightValue
            memPutShort(vertexPtr + MC_ENTITY_OFFSET + 2, ctx.renderType);
            final int midBlock = ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, ctx.localPosX, ctx.localPosY, ctx.localPosZ);
            memPutInt(vertexPtr + MID_BLOCK_OFFSET, midBlock);
            memPutByte(vertexPtr + MID_BLOCK_OFFSET + 3, ctx.lightValue);

            vertexPtr += STRIDE;
        }

        // Per-primitive: midTexCoord, blockId, normal, tangent
        final int count = vertices.length;
        final int midUV = encodeMidTexture(uSum / count, vSum / count);
        final long last = vertexPtr - STRIDE;

        quad.setup(last, STRIDE);
        NormalHelper.computeFaceNormal(normal, quad);
        final int packedNormal = NormI8.pack(normal);
        final int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, quad);

        for (long p = ptr; p < vertexPtr; p += STRIDE) {
            memPutInt(p + MID_TEX_OFFSET, midUV);
            memPutShort(p + MC_ENTITY_OFFSET, ctx.blockId);
            memPutInt(p + NORMAL_OFFSET, packedNormal);
            memPutInt(p + TANGENT_OFFSET, tangent);
        }

        return vertexPtr;
    }
}
