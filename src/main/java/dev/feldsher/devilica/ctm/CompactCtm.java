/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compact connected textures ({@code method=ctm_compact}): five tiles (lone, all joined, joined left and right,
 * joined up and down, inner corner) make every pattern a 47-tile set would, each quarter of a face taking the quarter
 * of the tile its two sides and corner call for (MCPatcher's compact CTM, in texture space for 1.12 quads).
 */
final class CompactCtm extends TileOverride {

    private static final int LONE = 0;
    private static final int ALL = 1;
    private static final int HORIZONTAL = 2;
    private static final int VERTICAL = 3;
    private static final int CORNER = 4;

    /** Quarters in texture space: top-left, top-right, bottom-right, bottom-left; their two sides and corner */
    private static final int[] SIDE_A = {CtmContext.REL_L, CtmContext.REL_U, CtmContext.REL_R, CtmContext.REL_D};
    private static final int[] SIDE_B = {CtmContext.REL_U, CtmContext.REL_R, CtmContext.REL_D, CtmContext.REL_L};
    private static final int[] CORNERS = {CtmContext.REL_UL, CtmContext.REL_UR, CtmContext.REL_DR, CtmContext.REL_DL};

    /** Split quads by quad and tiles of the four quarters (5^4 combinations) */
    private final Map<BakedQuad, List<BakedQuad>[]> split = new ConcurrentHashMap<>();

    CompactCtm(CtmProperties properties) {
        super(properties);
    }

    @Override
    String method() {
        return "ctm_compact";
    }

    @Override
    String checkTiles() {
        return this.tileNames.length == 5 ? null : "needs 5 tiles";
    }

    @Override
    int chooseTile(CtmContext context) {
        return LONE;
    }

    @Override
    Object render(BakedQuad quad, CtmContext context) {
        if (!applies(context)) {
            return null;
        }
        final int bits = neighborBits(context);
        final int[] quarters = new int[4];
        for (int quarter = 0; quarter < 4; quarter++) {
            quarters[quarter] = quarterTile(quarter, bits);
        }
        if (quarters[0] == quarters[1] && quarters[1] == quarters[2] && quarters[2] == quarters[3]) {
            final int tile = quarters[0];
            return this.tiles[tile] != null ? retexture(quad, tile) : quad;
        }
        final int key = ((quarters[0] * 5 + quarters[1]) * 5 + quarters[2]) * 5 + quarters[3];
        @SuppressWarnings("unchecked")
        final List<BakedQuad>[] byKey = this.split.computeIfAbsent(quad, q -> new List[625]);
        List<BakedQuad> result = byKey[key];
        if (result == null) {
            result = splitQuad(quad, quarters);
            byKey[key] = result;
        }
        return result;
    }

    private static int quarterTile(int quarter, int bits) {
        final boolean a = (bits & (1 << SIDE_A[quarter])) != 0;
        final boolean b = (bits & (1 << SIDE_B[quarter])) != 0;
        if (a && b) {
            return (bits & (1 << CORNERS[quarter])) != 0 ? ALL : CORNER;
        }
        if (a ^ b) {
            final int joined = a ? SIDE_A[quarter] : SIDE_B[quarter];
            // Joined to the left or right, the quarter keeps its top and bottom edges: the horizontal-line tile
            return joined == CtmContext.REL_L || joined == CtmContext.REL_R ? HORIZONTAL : VERTICAL;
        }
        return LONE;
    }

    /** The quad cut in four through the middle of its texture, each part with the tile of its quarter */
    private List<BakedQuad> splitQuad(BakedQuad quad, int[] quarters) {
        final VertexFormat format = quad.getFormat();
        final int stride = format.getIntegerSize();
        final int uvOffset = format.getUvOffsetById(0) / 4;
        final int[] data = quad.getVertexData();
        if (data.length / stride != 4) {
            return List.of(quad);
        }
        final TextureAtlasSprite sprite = quad.getSprite();
        final List<BakedQuad> parts = new ArrayList<>(4);
        for (int sPart = 0; sPart < 2; sPart++) {
            for (int tPart = 0; tPart < 2; tPart++) {
                final float s0 = sPart * 0.5F;
                final float t0 = tPart * 0.5F;
                final float[][] corners = {{s0, t0}, {s0 + 0.5F, t0}, {s0 + 0.5F, t0 + 0.5F}, {s0, t0 + 0.5F}};
                final int[] part = new int[4 * stride];
                for (int vertex = 0; vertex < 4; vertex++) {
                    System.arraycopy(data, 0, part, vertex * stride, stride);
                    final float s = corners[vertex][0];
                    final float t = corners[vertex][1];
                    for (int c = 0; c < 3; c++) {
                        part[vertex * stride + c] = Float.floatToRawIntBits(bilerp(data, stride, c, s, t));
                    }
                    part[vertex * stride + uvOffset] = Float.floatToRawIntBits(bilerp(data, stride, uvOffset, s, t));
                    part[vertex * stride + uvOffset + 1] = Float.floatToRawIntBits(bilerp(data, stride, uvOffset + 1, s, t));
                }
                // Which quarter of the texture the part shows, from its centre
                final float u = sprite.getUnInterpolatedU(bilerp(data, stride, uvOffset, s0 + 0.25F, t0 + 0.25F));
                final float v = sprite.getUnInterpolatedV(bilerp(data, stride, uvOffset + 1, s0 + 0.25F, t0 + 0.25F));
                final int quarter = v < 8.0F ? (u < 8.0F ? 0 : 1) : (u < 8.0F ? 3 : 2);
                final BakedQuad piece = new BakedQuad(part, quad.getTintIndex(), quad.getFace(), sprite, quad.shouldApplyDiffuseLighting(), format);
                final TextureAtlasSprite tile = this.tiles[quarters[quarter]];
                parts.add(tile == null ? piece : Quads.retexture(piece, tile, quad.getTintIndex()));
            }
        }
        return parts;
    }

    /** A float of the quad at (s, t) between its corners 0, 1, 2, 3 */
    private static float bilerp(int[] data, int stride, int offset, float s, float t) {
        final float a = Float.intBitsToFloat(data[offset]);
        final float b = Float.intBitsToFloat(data[stride + offset]);
        final float c = Float.intBitsToFloat(data[2 * stride + offset]);
        final float d = Float.intBitsToFloat(data[3 * stride + offset]);
        return (1 - s) * (1 - t) * a + s * (1 - t) * b + s * t * c + (1 - s) * t * d;
    }
}
