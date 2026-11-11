package com.gtnewhorizons.angelica.rendering.celeritas.iris;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexLayout;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

/**
 * VANILLA_LIKE without its RDH factor (28 bytes) + Iris extensions (20 bytes) = 48 bytes total.
 *
 * Iris extensions:
 *   mc_midTexCoord  ushort[2]  - quad center UV (average of 4 vertices)
 *   at_tangent      byte[4]    - tangent vector, normalized
 *   iris_Normal     byte[3]+1  - face normal, normalized
 *   mc_Entity       short[2]   - (blockId, renderType)
 *   at_midBlock     byte[4]    - (xyz offset from block center, lightValue)
 *
 * Devilica: Celeritas's newer common describes vertex data with a {@link ChunkVertexLayout}; this one has no
 * per-primitive header, so every attribute is fetched per vertex as in Angelica.
 */
public class IrisExtendedChunkVertexType implements ChunkVertexType {
    public static final ChunkVertexType BASE_TYPE = ChunkMeshFormats.VANILLA_LIKE;
    public static final int STRIDE = 48;

    public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(STRIDE)
        .addElements(ChunkVertexLayout.requireAttributes(BASE_TYPE.getLayout()).getAttributes().stream().filter(a -> !a.getName().equals("a_RdhFactor")).toList())
        .addElement("mc_midTexCoord", 28, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
        .addElement("at_tangent", 32, GlVertexAttributeFormat.BYTE, 4, true, false)
        .addElement("iris_Normal", 36, GlVertexAttributeFormat.BYTE, 3, true, false)
        .addElement("mc_Entity", 40, GlVertexAttributeFormat.SHORT, 2, false, false)
        .addElement("at_midBlock", 44, GlVertexAttributeFormat.BYTE, 4, false, false)
        .build();

    public static final ChunkVertexLayout LAYOUT = ChunkVertexLayout.of(VERTEX_FORMAT);

    private static final int TEXTURE_MAX_VALUE = 32768;
    public static final float MID_TEX_SCALE = 1.0f / TEXTURE_MAX_VALUE;

    /** Pack UV as two unsigned shorts: low=u, high=v. */
    public static int encodeMidTexture(float u, float v) {
        return ((Math.round(u * TEXTURE_MAX_VALUE) & 0xFFFF)) |
               ((Math.round(v * TEXTURE_MAX_VALUE) & 0xFFFF) << 16);
    }

    @Override
    public ChunkVertexEncoder createEncoder() {
        return new IrisExtendedChunkVertexEncoder();
    }

    @Override
    public float getPositionScale() {
        return BASE_TYPE.getPositionScale();
    }

    @Override
    public float getPositionOffset() {
        return BASE_TYPE.getPositionOffset();
    }

    @Override
    public float getTextureScale() {
        return BASE_TYPE.getTextureScale();
    }

    @Override
    public ChunkVertexLayout getLayout() {
        return LAYOUT;
    }

    public GlVertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }
}
