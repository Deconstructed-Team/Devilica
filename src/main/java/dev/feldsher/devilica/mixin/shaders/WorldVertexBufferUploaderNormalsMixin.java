/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks drawn as they are baked, in the block vertex format, carry no normal: a falling block, a block a piston moves.
 * A shader pack lights by the normal, and took whatever the draw before had left: the falling sand turned darker and
 * lighter in flight. Each quad gets the normal of its own plane.
 */
@Mixin(WorldVertexBufferUploader.class)
public abstract class WorldVertexBufferUploaderNormalsMixin {

    @Unique
    private static final VertexFormat DEVILICA$BLOCK_NORMAL = new VertexFormat(DefaultVertexFormats.BLOCK)
        .addElement(DefaultVertexFormats.NORMAL_3B).addElement(DefaultVertexFormats.PADDING_1B);

    @Unique
    private BufferBuilder devilica$withNormals;

    @Shadow
    public abstract void draw(BufferBuilder buffer);

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void devilica$addNormals(BufferBuilder buffer, CallbackInfo ci) {
        final int vertices = buffer.getVertexCount();
        if (buffer.getVertexFormat() != DefaultVertexFormats.BLOCK || buffer.getDrawMode() != GL11.GL_QUADS || vertices == 0
            || vertices % 4 != 0 || !IrisApi.getInstance().isShaderPackInUse()) {
            return;
        }
        if (this.devilica$withNormals == null) {
            this.devilica$withNormals = new BufferBuilder(DEVILICA$BLOCK_NORMAL.getIntegerSize() * 4 * 64);
        }
        final BufferBuilder target = this.devilica$withNormals;
        target.begin(GL11.GL_QUADS, DEVILICA$BLOCK_NORMAL);
        final int stride = DefaultVertexFormats.BLOCK.getIntegerSize();
        final IntBuffer source = buffer.getByteBuffer().asIntBuffer();
        final int[] quad = new int[DEVILICA$BLOCK_NORMAL.getIntegerSize() * 4];
        for (int first = 0; first < vertices; first += 4) {
            final int base = first * stride;
            // The diagonals' cross product: a quad that is not quite flat still gets one normal
            final float ax = devilica$coordinate(source, base, 2, 0) - devilica$coordinate(source, base, 0, 0);
            final float ay = devilica$coordinate(source, base, 2, 1) - devilica$coordinate(source, base, 0, 1);
            final float az = devilica$coordinate(source, base, 2, 2) - devilica$coordinate(source, base, 0, 2);
            final float bx = devilica$coordinate(source, base, 3, 0) - devilica$coordinate(source, base, 1, 0);
            final float by = devilica$coordinate(source, base, 3, 1) - devilica$coordinate(source, base, 1, 1);
            final float bz = devilica$coordinate(source, base, 3, 2) - devilica$coordinate(source, base, 1, 2);
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            final float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 1.0E-6F) {
                nx /= length;
                ny /= length;
                nz /= length;
            } else {
                nx = 0.0F;
                ny = 1.0F;
                nz = 0.0F;
            }
            final int normal = devilica$packNormal(nx, ny, nz);
            for (int vertex = 0; vertex < 4; vertex++) {
                final int from = base + vertex * stride;
                final int to = vertex * (stride + 1);
                for (int i = 0; i < stride; i++) {
                    quad[to + i] = source.get(from + i);
                }
                quad[to + stride] = normal;
            }
            target.addVertexData(quad);
        }
        target.finishDrawing();
        this.draw(target);
        buffer.reset();
        ci.cancel();
    }

    @Unique
    private static float devilica$coordinate(IntBuffer source, int base, int vertex, int axis) {
        return Float.intBitsToFloat(source.get(base + vertex * DefaultVertexFormats.BLOCK.getIntegerSize() + axis));
    }

    /** Three signed bytes and padding, in the buffer's own (native) byte order */
    @Unique
    private static int devilica$packNormal(float x, float y, float z) {
        final int bx = Math.round(x * 127.0F) & 0xFF;
        final int by = Math.round(y * 127.0F) & 0xFF;
        final int bz = Math.round(z * 127.0F) & 0xFF;
        return ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? bx | by << 8 | bz << 16 : bx << 24 | by << 16 | bz << 8;
    }
}
