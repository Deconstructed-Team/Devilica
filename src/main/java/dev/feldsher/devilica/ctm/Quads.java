/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;

/** New quads from baked ones: another texture, its corners turned or mirrored. */
public final class Quads {

    private static final float EPSILON = 1.0E-4F;

    private Quads() {
    }

    /** Whether the quad covers a whole face of its block, on the block's side */
    public static boolean isFullFace(BakedQuad quad) {
        final EnumFacing face = quad.getFace();
        if (face == null) {
            return false;
        }
        final int[] data = quad.getVertexData();
        final int stride = quad.getFormat().getIntegerSize();
        final float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        final float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int axis = 0; axis < 3; axis++) {
                final float value = Float.intBitsToFloat(data[vertex * stride + axis]);
                min[axis] = Math.min(min[axis], value);
                max[axis] = Math.max(max[axis], value);
            }
        }
        final int normal = face.getAxis().ordinal();
        final float side = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 1.0F : 0.0F;
        for (int axis = 0; axis < 3; axis++) {
            if (axis == normal) {
                if (Math.abs(min[axis] - side) > EPSILON || Math.abs(max[axis] - side) > EPSILON) {
                    return false;
                }
            } else if (min[axis] > EPSILON || max[axis] < 1.0F - EPSILON) {
                return false;
            }
        }
        return true;
    }

    /** The quad with its texture coordinates moved from its sprite to another, and the given tint */
    public static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite target, int tintIndex) {
        final TextureAtlasSprite source = quad.getSprite();
        final VertexFormat format = quad.getFormat();
        final int[] data = quad.getVertexData().clone();
        final int stride = format.getIntegerSize();
        final int uv = format.getUvOffsetById(0) / 4;
        for (int vertex = 0; vertex < data.length / stride; vertex++) {
            final int at = vertex * stride + uv;
            data[at] = Float.floatToRawIntBits(target.getInterpolatedU(source.getUnInterpolatedU(Float.intBitsToFloat(data[at]))));
            data[at + 1] = Float.floatToRawIntBits(target.getInterpolatedV(source.getUnInterpolatedV(Float.intBitsToFloat(data[at + 1]))));
        }
        return new BakedQuad(data, tintIndex, quad.getFace(), target, quad.shouldApplyDiffuseLighting(), format);
    }

    /**
     * A copy of the quad with a sprite laid as the face's default texture coordinates lay it (u along x on the up face
     * and so on), whatever turn or mirror the model gives its own texture.
     */
    public static BakedQuad faceAligned(BakedQuad quad, TextureAtlasSprite target, int tintIndex) {
        final VertexFormat format = quad.getFormat();
        final int[] data = quad.getVertexData().clone();
        final int stride = format.getIntegerSize();
        final int uv = format.getUvOffsetById(0) / 4;
        final float[] faceUv = new float[2];
        for (int vertex = 0; vertex < data.length / stride; vertex++) {
            faceUv(quad.getFace(), data, vertex * stride, faceUv);
            data[vertex * stride + uv] = Float.floatToRawIntBits(target.getInterpolatedU(faceUv[0]));
            data[vertex * stride + uv + 1] = Float.floatToRawIntBits(target.getInterpolatedV(faceUv[1]));
        }
        return new BakedQuad(data, tintIndex, quad.getFace(), target, quad.shouldApplyDiffuseLighting(), format);
    }

    /** The default texture coordinate (0 to 16) of a vertex on a face, from its position: 1.12's FaceBakery layout */
    private static void faceUv(EnumFacing face, int[] data, int at, float[] out) {
        final float x = Float.intBitsToFloat(data[at]) * 16.0F;
        final float y = Float.intBitsToFloat(data[at + 1]) * 16.0F;
        final float z = Float.intBitsToFloat(data[at + 2]) * 16.0F;
        switch (face) {
            case DOWN -> {
                out[0] = x;
                out[1] = 16.0F - z;
            }
            case UP -> {
                out[0] = x;
                out[1] = z;
            }
            case NORTH -> {
                out[0] = 16.0F - x;
                out[1] = 16.0F - y;
            }
            case SOUTH -> {
                out[0] = x;
                out[1] = 16.0F - y;
            }
            case WEST -> {
                out[0] = z;
                out[1] = 16.0F - y;
            }
            case EAST -> {
                out[0] = 16.0F - z;
                out[1] = 16.0F - y;
            }
        }
    }

    /**
     * How the model turns or mirrors its texture on the face, against the face's default layout: one of
     * {@link CtmContext#ORIENTATIONS}, 0 when it lies as the default does or cannot be told.
     */
    static int orientation(BakedQuad quad) {
        final EnumFacing face = quad.getFace();
        final int[] data = quad.getVertexData();
        final int stride = quad.getFormat().getIntegerSize();
        if (face == null || data.length < 4 * stride) {
            return 0;
        }
        final TextureAtlasSprite sprite = quad.getSprite();
        final int uv = quad.getFormat().getUvOffsetById(0) / 4;
        final float[] c0 = new float[2];
        final float[] c1 = new float[2];
        final float[] c3 = new float[2];
        faceUv(face, data, 0, c0);
        faceUv(face, data, stride, c1);
        faceUv(face, data, 3 * stride, c3);
        // The texture's own coordinates at the same corners
        final float a0u = sprite.getUnInterpolatedU(Float.intBitsToFloat(data[uv]));
        final float a0v = sprite.getUnInterpolatedV(Float.intBitsToFloat(data[uv + 1]));
        final float a1u = sprite.getUnInterpolatedU(Float.intBitsToFloat(data[stride + uv])) - a0u;
        final float a1v = sprite.getUnInterpolatedV(Float.intBitsToFloat(data[stride + uv + 1])) - a0v;
        final float a3u = sprite.getUnInterpolatedU(Float.intBitsToFloat(data[3 * stride + uv])) - a0u;
        final float a3v = sprite.getUnInterpolatedV(Float.intBitsToFloat(data[3 * stride + uv + 1])) - a0v;
        final float d1u = c1[0] - c0[0];
        final float d1v = c1[1] - c0[1];
        final float d3u = c3[0] - c0[0];
        final float d3v = c3[1] - c0[1];
        final float det = d1u * d3v - d3u * d1v;
        if (Math.abs(det) < EPSILON) {
            return 0;
        }
        // The linear map from default to own coordinates: [a1 a3] * inverse([d1 d3])
        final float m00 = (a1u * d3v - a3u * d1v) / det;
        final float m01 = (a3u * d1u - a1u * d3u) / det;
        final float m10 = (a1v * d3v - a3v * d1v) / det;
        final float m11 = (a3v * d1u - a1v * d3u) / det;
        int best = 0;
        float bestScore = -Float.MAX_VALUE;
        for (int o = 0; o < CtmContext.ORIENTATIONS.length; o++) {
            final int[] m = CtmContext.ORIENTATIONS[o];
            final float score = m00 * m[0] + m01 * m[1] + m10 * m[2] + m11 * m[3];
            if (score > bestScore) {
                bestScore = score;
                best = o;
            }
        }
        return best;
    }

    /**
     * The quad's texture turned by quarter turns and optionally mirrored across, within its sprite: each corner takes
     * the texture coordinate of the corner {@code quarterTurns} further round.
     */
    public static BakedQuad turn(BakedQuad quad, int quarterTurns, boolean mirror) {
        final TextureAtlasSprite sprite = quad.getSprite();
        final VertexFormat format = quad.getFormat();
        final int[] original = quad.getVertexData();
        final int[] data = original.clone();
        final int stride = format.getIntegerSize();
        final int uv = format.getUvOffsetById(0) / 4;
        final int vertices = data.length / stride;
        if (vertices != 4) {
            return quad;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            final int from = ((vertex + quarterTurns) & 3) * stride + uv;
            float u = Float.intBitsToFloat(original[from]);
            final float v = Float.intBitsToFloat(original[from + 1]);
            if (mirror) {
                u = sprite.getMinU() + sprite.getMaxU() - u;
            }
            data[vertex * stride + uv] = Float.floatToRawIntBits(u);
            data[vertex * stride + uv + 1] = Float.floatToRawIntBits(v);
        }
        return new BakedQuad(data, quad.getTintIndex(), quad.getFace(), sprite, quad.shouldApplyDiffuseLighting(), format);
    }
}
