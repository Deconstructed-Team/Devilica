/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The block being drawn and the face being retextured. Directions on a face (left, down, right, up) follow the texture
 * as 1.12 models lay it on that face, so tile maps connect the way they look.
 */
final class CtmContext {

    static final int DOWN = 0;
    static final int UP = 1;
    static final int NORTH = 2;
    static final int SOUTH = 3;
    static final int WEST = 4;
    static final int EAST = 5;

    static final int[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    /** Neighbours around a face, in texture space: */
    static final int REL_L = 0;
    static final int REL_DL = 1;
    static final int REL_D = 2;
    static final int REL_DR = 3;
    static final int REL_R = 4;
    static final int REL_UR = 5;
    static final int REL_U = 6;
    static final int REL_UL = 7;

    /**
     * [face][neighbour][x, y, z]. A face's texture runs (left, down, right, up) along these directions, from 1.12's default
     * face UVs: down u=x v=16-z, up u=x v=z, north u=16-x, south u=x, west u=z, east u=16-z, sides v=16-y.
     */
    static final int[][][] NEIGHBOR_OFFSETS = {
            neighbors(WEST, NORTH, EAST, SOUTH),
            neighbors(WEST, SOUTH, EAST, NORTH),
            neighbors(EAST, DOWN, WEST, UP),
            neighbors(WEST, DOWN, EAST, UP),
            neighbors(NORTH, DOWN, SOUTH, UP),
            neighbors(SOUTH, DOWN, NORTH, UP),
    };

    private static int[][] neighbors(int left, int down, int right, int up) {
        final int[] l = NORMALS[left];
        final int[] d = NORMALS[down];
        final int[] r = NORMALS[right];
        final int[] u = NORMALS[up];
        return new int[][]{l, add(l, d), d, add(d, r), r, add(r, u), u, add(u, l)};
    }

    private static int[] add(int[] a, int[] b) {
        return new int[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    /**
     * The turns and mirrors a model can give a face's texture (random rotations, mirrored stone, logs on their side),
     * as the linear map {m00, m01, m10, m11} from the face's default texture coordinates to the model's.
     */
    static final int[][] ORIENTATIONS = {
            {1, 0, 0, 1}, {0, -1, 1, 0}, {-1, 0, 0, -1}, {0, 1, -1, 0},
            {-1, 0, 0, 1}, {1, 0, 0, -1}, {0, 1, 1, 0}, {0, -1, -1, 0},
    };
    /** Texture space directions L, DL, D, DR, R, UR, U, UL as (u, v) */
    private static final int[] DIRECTION_U = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final int[] DIRECTION_V = {0, 1, 1, 1, 0, -1, -1, -1};
    /** [orientation][direction in the model's texture] = the same direction in the face's default layout */
    private static final int[][] TO_DEFAULT = new int[ORIENTATIONS.length][8];

    static {
        for (int o = 0; o < ORIENTATIONS.length; o++) {
            final int[] m = ORIENTATIONS[o];
            for (int r = 0; r < 8; r++) {
                // The maps are turns and mirrors, so the inverse is the transpose
                final int u = m[0] * DIRECTION_U[r] + m[2] * DIRECTION_V[r];
                final int v = m[1] * DIRECTION_U[r] + m[3] * DIRECTION_V[r];
                for (int d = 0; d < 8; d++) {
                    if (DIRECTION_U[d] == u && DIRECTION_V[d] == v) {
                        TO_DEFAULT[o][r] = d;
                    }
                }
            }
        }
    }

    IBlockAccess world;
    BlockPos pos;
    IBlockState state;
    Block block;
    int meta;
    int face;
    TextureAtlasSprite sprite;
    /** How the model turns or mirrors the texture on this face, so neighbours are read in the texture's own directions */
    int orientation;

    private final BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

    void setBlock(IBlockAccess world, BlockPos pos, IBlockState state) {
        this.world = world;
        this.pos = pos;
        this.state = state;
        this.block = state.getBlock();
        this.meta = this.block.getMetaFromState(state);
    }

    void setFace(int face, TextureAtlasSprite sprite, int orientation) {
        this.face = face;
        this.sprite = sprite;
        this.orientation = orientation;
    }

    /** The offset to a neighbour in a direction of the texture: the face's own texture as the model lays it */
    int[] offset(int face, int relative) {
        return NEIGHBOR_OFFSETS[face][face == this.face ? TO_DEFAULT[this.orientation][relative] : relative];
    }

    BlockPos neighbor(int[] offset) {
        return this.neighborPos.setPos(this.pos.getX() + offset[0], this.pos.getY() + offset[1], this.pos.getZ() + offset[2]);
    }
}
