/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OptiFine's overlay methods: tiles drawn over a face, in a pass of their own, rather than in place of its texture.
 * {@code overlay} draws the edges of neighbouring blocks reaching onto this one (17 tiles); {@code overlay_ctm},
 * {@code overlay_random}, {@code overlay_repeat} and {@code overlay_fixed} pick one tile as their plain methods do.
 */
abstract class Overlay extends TileOverride {

    final BlockRenderLayer layer;
    private final int tint;
    private final Set<String> connectTiles;
    private final Map<BakedQuad, BakedQuad[]> overlays = new ConcurrentHashMap<>();

    Overlay(CtmProperties properties) {
        super(properties);
        this.layer = properties.layer;
        this.tint = properties.tintBlock == null ? properties.tintIndex : OverlayTints.code(properties.tintBlock, properties.tintIndex);
        this.connectTiles = properties.connectTiles;
    }

    @Override
    boolean isOverlay() {
        return true;
    }

    /** Items have no neighbours to draw edges from */
    @Override
    int heldTile() {
        return -1;
    }

    /** An overlay never takes a face's place */
    @Override
    Object render(BakedQuad quad, CtmContext context) {
        return null;
    }

    /** Adds the overlay quads for the context's face over the quad */
    final void addOverlays(BakedQuad quad, CtmContext context, List<BakedQuad> out) {
        if (!applies(context) || !Quads.isFullFace(quad)) {
            return;
        }
        addTiles(context, tile -> {
            if (tile >= 0 && tile < this.tiles.length && this.tiles[tile] != null) {
                out.add(overlay(quad, tile));
            }
        });
    }

    abstract void addTiles(CtmContext context, TileSink tiles);

    interface TileSink {
        void add(int tile);
    }

    @Override
    int chooseTile(CtmContext context) {
        return -1;
    }

    private BakedQuad overlay(BakedQuad quad, int tile) {
        final BakedQuad[] byTile = this.overlays.computeIfAbsent(quad, q -> new BakedQuad[this.tiles.length]);
        BakedQuad result = byTile[tile];
        if (result == null) {
            result = Quads.faceAligned(quad, this.tiles[tile], this.tint);
            byTile[tile] = result;
        }
        return result;
    }

    // ---- neighbours, as OptiFine's overlay reads them

    /**
     * Whether the neighbour at an offset draws its overlay onto this face: a full block from the connect lists that
     * does not join this one, with nothing solid in front of it covering the edge.
     */
    final boolean appliesOverlay(CtmContext context, int[] offset) {
        if (isCovered(context, offset)) {
            return false;
        }
        final BlockPos pos = context.neighbor(offset);
        final IBlockState neighbor = context.world.getBlockState(pos);
        if (!neighbor.isFullCube()) {
            return false;
        }
        if (!this.connectBlocks.isEmpty()) {
            final int meta = neighbor.getBlock().getMetaFromState(neighbor);
            boolean listed = false;
            for (BlockMatcher matcher : this.connectBlocks) {
                if (matcher.matches(neighbor.getBlock(), meta)) {
                    listed = true;
                    break;
                }
            }
            if (!listed) {
                return false;
            }
        }
        if (!this.connectTiles.isEmpty() && !hasTile(neighbor, context.face, this.connectTiles)) {
            return false;
        }
        return !connects(context, neighbor);
    }

    /** Whether the neighbour takes this same overlay, so the corners between them carry on */
    final boolean hasSameOverlay(CtmContext context, int[] offset) {
        if (isCovered(context, offset)) {
            return false;
        }
        final IBlockState neighbor = context.world.getBlockState(context.neighbor(offset));
        if (!this.matchBlocks.isEmpty() && !matchesBlock(neighbor.getBlock(), neighbor.getBlock().getMetaFromState(neighbor))) {
            return false;
        }
        return this.matchTiles.isEmpty() || hasTile(neighbor, context.face, this.matchTiles);
    }

    private static boolean isCovered(CtmContext context, int[] offset) {
        final int[] normal = CtmContext.NORMALS[context.face];
        final BlockPos front = context.neighbor(new int[]{offset[0] + normal[0], offset[1] + normal[1], offset[2] + normal[2]});
        return context.world.getBlockState(front).isOpaqueCube();
    }

    private static boolean hasTile(IBlockState state, int face, Set<String> tiles) {
        final TextureAtlasSprite sprite = ConnectedTextures.spriteOf(state, face);
        return sprite != null && tiles.contains(sprite.getIconName());
    }

    // ---- methods

    /** {@code overlay}: 17 tiles for the edges and corners of the neighbours that reach onto the face */
    static final class Edges extends Overlay {
        private static final int[] SIDES = {CtmContext.REL_L, CtmContext.REL_D, CtmContext.REL_R, CtmContext.REL_U};
        /** The corner between side i and side i + 1, and its tile */
        private static final int[] CORNERS = {CtmContext.REL_DL, CtmContext.REL_DR, CtmContext.REL_UR, CtmContext.REL_UL};
        private static final int[] CORNER_TILES = {2, 0, 14, 16};

        Edges(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "overlay";
        }

        @Override
        String checkTiles() {
            return this.tileNames.length >= 17 ? null : "needs 17 tiles";
        }

        @Override
        void addTiles(CtmContext context, TileSink tiles) {
            int sides = 0;
            for (int i = 0; i < 4; i++) {
                if (appliesOverlay(context, context.offset(context.face, SIDES[i]))) {
                    sides |= 1 << i;
                }
            }
            switch (sides) {
                case 0b1111 -> tiles.add(8);
                case 0b0111 -> tiles.add(5);
                case 0b1011 -> tiles.add(6);
                case 0b1101 -> tiles.add(13);
                case 0b1110 -> tiles.add(12);
                case 0b0101 -> {
                    tiles.add(9);
                    tiles.add(7);
                }
                case 0b1010 -> {
                    tiles.add(1);
                    tiles.add(15);
                }
                case 0b0011 -> twoSides(context, tiles, 4, 2);
                case 0b0110 -> twoSides(context, tiles, 3, 3);
                case 0b1100 -> twoSides(context, tiles, 10, 0);
                case 0b1001 -> twoSides(context, tiles, 11, 1);
                case 0b0001 -> oneSide(context, tiles, 9, 0);
                case 0b0010 -> oneSide(context, tiles, 1, 1);
                case 0b0100 -> oneSide(context, tiles, 7, 2);
                case 0b1000 -> oneSide(context, tiles, 15, 3);
                default -> {
                    for (int corner = 0; corner < 4; corner++) {
                        if ((same(context, corner) || same(context, (corner + 1) & 3)) && cornerApplies(context, corner)) {
                            tiles.add(CORNER_TILES[corner]);
                        }
                    }
                }
            }
        }

        /** Two neighbouring sides reach in; the corner across from them may too */
        private void twoSides(CtmContext context, TileSink tiles, int tile, int corner) {
            tiles.add(tile);
            if ((same(context, corner) || same(context, (corner + 1) & 3)) && cornerApplies(context, corner)) {
                tiles.add(CORNER_TILES[corner]);
            }
        }

        /** One side reaches in; so may the two corners away from it */
        private void oneSide(CtmContext context, TileSink tiles, int tile, int side) {
            tiles.add(tile);
            final int first = (side + 1) & 3;
            final int across = (side + 2) & 3;
            final int last = (side + 3) & 3;
            final boolean acrossSame = same(context, across);
            if ((acrossSame || same(context, first)) && cornerApplies(context, first)) {
                tiles.add(CORNER_TILES[first]);
            }
            if ((acrossSame || same(context, last)) && cornerApplies(context, across)) {
                tiles.add(CORNER_TILES[across]);
            }
        }

        private boolean same(CtmContext context, int side) {
            return hasSameOverlay(context, context.offset(context.face, SIDES[side]));
        }

        private boolean cornerApplies(CtmContext context, int corner) {
            return appliesOverlay(context, context.offset(context.face, CORNERS[corner]));
        }
    }

    /** {@code overlay_ctm}, {@code overlay_random}, {@code overlay_repeat}, {@code overlay_fixed}: one tile by a plain method */
    static final class Single extends Overlay {
        private final TileOverride method;

        Single(CtmProperties properties, TileOverride method) {
            super(properties);
            this.method = method;
        }

        @Override
        String method() {
            return "overlay_" + this.method.method();
        }

        @Override
        String checkTiles() {
            return this.method.checkTiles();
        }

        @Override
        void addTiles(CtmContext context, TileSink tiles) {
            tiles.add(this.method.chooseTile(context));
        }
    }
}
