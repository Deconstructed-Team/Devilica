/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.coloredlight;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexLayout;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.memPutInt;

/** A terrain vertex type with the coloured light tint ({@code a_LightTint}, RGBA bytes) after the base type's data. */
public final class TintedChunkVertexType implements ChunkVertexType {

    private static final Map<ChunkVertexType, TintedChunkVertexType> TYPES = new IdentityHashMap<>();

    private final ChunkVertexType base;
    private final int baseStride;
    private final int stride;
    private final ChunkVertexLayout layout;

    private TintedChunkVertexType(ChunkVertexType base) {
        this.base = base;
        final GlVertexFormat baseFormat = ChunkVertexLayout.requireAttributes(base.getLayout());
        this.baseStride = baseFormat.getStride();
        this.stride = this.baseStride + 4;
        this.layout = ChunkVertexLayout.of(GlVertexFormat.builder(this.stride)
            .addAllElements(baseFormat)
            .addElement("a_LightTint", this.baseStride, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .build());
    }

    public static synchronized TintedChunkVertexType of(ChunkVertexType base) {
        return TYPES.computeIfAbsent(base, TintedChunkVertexType::new);
    }

    @Override
    public float getPositionScale() {
        return this.base.getPositionScale();
    }

    @Override
    public float getPositionOffset() {
        return this.base.getPositionOffset();
    }

    @Override
    public float getTextureScale() {
        return this.base.getTextureScale();
    }

    @Override
    public ChunkVertexLayout getLayout() {
        return this.layout;
    }

    @Override
    public Map<String, String> getDefines() {
        return this.base.getDefines();
    }

    @Override
    public ChunkVertexEncoder createEncoder() {
        return new Encoder(this.base.createEncoder(), this.baseStride, this.stride);
    }

    /** The base type's encoder one vertex at a time, each followed by its tint */
    private static final class Encoder implements ChunkVertexEncoder {
        private final ChunkVertexEncoder base;
        private final int baseStride;
        private final int stride;
        private final Vertex[] one = new Vertex[1];

        Encoder(ChunkVertexEncoder base, int baseStride, int stride) {
            this.base = base;
            this.baseStride = baseStride;
            this.stride = stride;
        }

        @Override
        public long writePrimitive(long ptr, Material material, Vertex[] vertices, int sectionIndex) {
            final ColoredLightField field = ColoredLightField.forThread();
            for (Vertex vertex : vertices) {
                this.one[0] = vertex;
                this.base.writePrimitive(ptr, material, this.one, sectionIndex);
                memPutInt(ptr + this.baseStride, field.tintAt(vertex.x, vertex.y, vertex.z));
                ptr += this.stride;
            }
            return ptr;
        }

        @Override
        public boolean supportsBilinearCorrection() {
            return this.base.supportsBilinearCorrection();
        }
    }
}
