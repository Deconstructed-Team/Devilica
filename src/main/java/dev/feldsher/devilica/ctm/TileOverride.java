/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One OptiFine/MCPatcher connected textures rule ({@code mcpatcher/ctm/**.properties}): which blocks or textures it
 * applies to, and how it picks one of its tiles from the neighbours. The tile methods are MCPatcher's (Angelica's
 * {@code TileOverrideImpl}), with the faces laid out as 1.12 models lay textures.
 */
abstract class TileOverride implements Comparable<TileOverride> {

    static final int CONNECT_BY_BLOCK = 0;
    static final int CONNECT_BY_TILE = 1;
    static final int CONNECT_BY_MATERIAL = 2;
    static final int CONNECT_BY_LIST = 3;

    /** Tile index for an 8-bit neighbour pattern: 128 64 32 / 1 * 16 / 2 4 8 */
    static final int[] CTM_NEIGHBOR_MAP = {
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26,
    };

    private static int nextOrder;

    final String source;
    final List<BlockMatcher> matchBlocks;
    final Set<String> matchTiles;
    final List<BlockMatcher> connectBlocks;
    /** Bit per face, all faces when 0x3F */
    private final int faces;
    private final int connect;
    private final BitSet biomes;
    private final int minHeight;
    private final int maxHeight;
    private final int weight;
    private final int order = nextOrder++;

    /** Sprite names of the tiles, null for a tile that keeps the face's own texture */
    final String[] tileNames;
    TextureAtlasSprite[] tiles;

    private final Map<BakedQuad, BakedQuad[]> retextured = new ConcurrentHashMap<>();

    TileOverride(CtmProperties properties) {
        this.source = properties.source;
        this.matchBlocks = properties.matchBlocks;
        this.matchTiles = properties.matchTiles;
        this.connectBlocks = properties.connectBlocks;
        this.faces = properties.faces;
        this.connect = properties.connect;
        this.biomes = properties.biomes;
        this.minHeight = properties.minHeight;
        this.maxHeight = properties.maxHeight;
        this.weight = properties.weight;
        this.tileNames = properties.tileNames;
    }

    abstract String method();

    /** Whether the rule draws tiles over faces rather than in their place */
    boolean isOverlay() {
        return false;
    }

    /** The tile a block drawn as an item shows, a block with no neighbours (MCPatcher's choice); -1 for its own texture */
    int heldTile() {
        return 0;
    }

    /** The tile for a face of a block drawn as an item, or -1; rules bound to heights or biomes leave items alone */
    final int heldTileFor(int face, Block block, int meta) {
        if ((this.faces & (1 << face)) == 0 || (!this.matchBlocks.isEmpty() && !matchesBlock(block, meta))) {
            return -1;
        }
        if (this.biomes != null || this.minHeight != Integer.MIN_VALUE || this.maxHeight != Integer.MAX_VALUE) {
            return -1;
        }
        final int tile = heldTile();
        return tile >= 0 && tile < this.tiles.length && this.tiles[tile] != null ? tile : -1;
    }

    /** @return why the tile list does not suit the method, or null */
    String checkTiles() {
        return null;
    }

    /** @return the tile to use, or -1 to leave the face alone */
    abstract int chooseTile(CtmContext context);

    /** The tile for the context's face when the rule applies there, -1 otherwise. */
    final int tileFor(CtmContext context) {
        return applies(context) ? chooseTile(context) : -1;
    }

    /** Whether the rule applies to the context's face at all: its faces, blocks, heights and biomes */
    final boolean applies(CtmContext context) {
        if ((this.faces & (1 << context.face)) == 0) {
            return false;
        }
        if (!this.matchBlocks.isEmpty() && !matchesBlock(context.block, context.meta)) {
            return false;
        }
        final int y = context.pos.getY();
        if (y < this.minHeight || y > this.maxHeight) {
            return false;
        }
        if (this.biomes != null) {
            final Biome biome = context.world.getBiome(context.pos);
            return this.biomes.get(Biome.getIdForBiome(biome));
        }
        return true;
    }

    /**
     * The quad as the rule draws it: null when the rule does not apply, else a quad (the same one for a tile that
     * keeps the face's texture) or a list of quads.
     */
    Object render(BakedQuad quad, CtmContext context) {
        final int tile = tileFor(context);
        if (tile < 0) {
            return null;
        }
        return tile < this.tiles.length && this.tiles[tile] != null ? retexture(quad, tile) : quad;
    }

    final boolean matchesBlock(Block block, int meta) {
        for (BlockMatcher matcher : this.matchBlocks) {
            if (matcher.matches(block, meta)) {
                return true;
            }
        }
        return false;
    }

    final boolean shouldConnect(CtmContext context, int relative) {
        return shouldConnect(context, context.offset(context.face, relative));
    }

    final boolean shouldConnect(CtmContext context, int face, int relative) {
        return shouldConnect(context, context.offset(face, relative));
    }

    final int neighborBits(CtmContext context) {
        int bits = 0;
        for (int bit = 0; bit < 8; bit++) {
            if (shouldConnect(context, bit)) {
                bits |= 1 << bit;
            }
        }
        return bits;
    }

    private boolean shouldConnect(CtmContext context, int[] offset) {
        return connects(context, context.world.getBlockState(context.neighbor(offset)));
    }

    /** Whether a neighbour joins the context's block by the rule's {@code connect} */
    final boolean connects(CtmContext context, IBlockState neighbor) {
        final Block neighborBlock = neighbor.getBlock();
        if (neighborBlock == context.block && !this.matchBlocks.isEmpty()
                && !matchesBlock(neighborBlock, neighborBlock.getMetaFromState(neighbor))) {
            return false;
        }
        return switch (this.connect) {
            case CONNECT_BY_TILE -> ConnectedTextures.spriteOf(neighbor, context.face) == context.sprite;
            case CONNECT_BY_BLOCK -> neighborBlock == context.block && neighborBlock.getMetaFromState(neighbor) == context.meta;
            case CONNECT_BY_MATERIAL -> neighbor.getMaterial() == context.state.getMaterial();
            case CONNECT_BY_LIST -> {
                final int meta = neighborBlock.getMetaFromState(neighbor);
                for (BlockMatcher matcher : this.connectBlocks) {
                    if (matcher.matches(neighborBlock, meta)) {
                        yield true;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    /** The quad with its texture swapped for a tile, made once per quad and tile. */
    final BakedQuad retexture(BakedQuad quad, int tile) {
        final BakedQuad[] byTile = this.retextured.computeIfAbsent(quad, q -> new BakedQuad[this.tiles.length]);
        BakedQuad result = byTile[tile];
        if (result == null) {
            result = Quads.retexture(quad, this.tiles[tile], quad.getTintIndex());
            byTile[tile] = result;
        }
        return result;
    }

    @Override
    public int compareTo(TileOverride other) {
        final int byWeight = Integer.compare(other.weight, this.weight);
        return byWeight != 0 ? byWeight : Integer.compare(this.order, other.order);
    }

    @Override
    public String toString() {
        return method() + "[" + this.source + "]";
    }

    // ---- methods

    static final class Ctm extends TileOverride {
        Ctm(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "ctm";
        }

        @Override
        String checkTiles() {
            return this.tileNames.length >= 47 ? null : "needs 47 tiles";
        }

        @Override
        int chooseTile(CtmContext context) {
            return CTM_NEIGHBOR_MAP[neighborBits(context)];
        }
    }

    static class Horizontal extends TileOverride {
        private static final int[] MAP = {3, 2, 0, 1};

        Horizontal(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "horizontal";
        }

        @Override
        int heldTile() {
            return 3;
        }

        @Override
        String checkTiles() {
            return this.tileNames.length == 4 ? null : "needs 4 tiles";
        }

        @Override
        int chooseTile(CtmContext context) {
            int bits = 0;
            if (shouldConnect(context, CtmContext.REL_L)) {
                bits |= 1;
            }
            if (shouldConnect(context, CtmContext.REL_R)) {
                bits |= 2;
            }
            return MAP[bits];
        }
    }

    static final class HorizontalVertical extends Horizontal {
        private static final int[] MAP = {3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 4, 4, 5, 4, 4, 4, 4, 4, 3, 3, 6, 3, 3, 3,
                3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3};

        HorizontalVertical(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "horizontal+vertical";
        }

        @Override
        String checkTiles() {
            return this.tileNames.length == 7 ? null : "needs 7 tiles";
        }

        @Override
        int chooseTile(CtmContext context) {
            final int tile = super.chooseTile(context);
            if (tile != 3) {
                return tile;
            }
            int bits = 0;
            if (shouldConnect(context, CtmContext.REL_DL)) {
                bits |= 1;
            }
            if (shouldConnect(context, CtmContext.REL_D)) {
                bits |= 2;
            }
            if (shouldConnect(context, CtmContext.REL_DR)) {
                bits |= 4;
            }
            if (shouldConnect(context, CtmContext.REL_UR)) {
                bits |= 8;
            }
            if (shouldConnect(context, CtmContext.REL_U)) {
                bits |= 16;
            }
            if (shouldConnect(context, CtmContext.REL_UL)) {
                bits |= 32;
            }
            return MAP[bits];
        }
    }

    static class Vertical extends TileOverride {
        private static final int[] MAP = {3, 2, 0, 1};

        Vertical(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "vertical";
        }

        @Override
        int heldTile() {
            return 3;
        }

        @Override
        String checkTiles() {
            return this.tileNames.length == 4 ? null : "needs 4 tiles";
        }

        @Override
        int chooseTile(CtmContext context) {
            int bits = 0;
            if (shouldConnect(context, CtmContext.REL_D)) {
                bits |= 1;
            }
            if (shouldConnect(context, CtmContext.REL_U)) {
                bits |= 2;
            }
            return MAP[bits];
        }
    }

    static final class VerticalHorizontal extends Vertical {
        private static final int[] MAP = {3, 6, 3, 3, 3, 6, 3, 3, 4, 5, 4, 4, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6,
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3};

        VerticalHorizontal(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "vertical+horizontal";
        }

        @Override
        String checkTiles() {
            return this.tileNames.length == 7 ? null : "needs 7 tiles";
        }

        @Override
        int chooseTile(CtmContext context) {
            final int tile = super.chooseTile(context);
            if (tile != 3) {
                return tile;
            }
            int bits = 0;
            if (shouldConnect(context, CtmContext.REL_L)) {
                bits |= 1;
            }
            if (shouldConnect(context, CtmContext.REL_DL)) {
                bits |= 2;
            }
            if (shouldConnect(context, CtmContext.REL_DR)) {
                bits |= 4;
            }
            if (shouldConnect(context, CtmContext.REL_R)) {
                bits |= 8;
            }
            if (shouldConnect(context, CtmContext.REL_UR)) {
                bits |= 16;
            }
            if (shouldConnect(context, CtmContext.REL_UL)) {
                bits |= 32;
            }
            return MAP[bits];
        }
    }

    /** The side of a block under another of its kind (a sandstone column without its top band). */
    static final class Top extends TileOverride {
        Top(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "top";
        }

        @Override
        int heldTile() {
            return -1;
        }

        @Override
        String checkTiles() {
            return this.tileNames.length == 1 ? null : "needs 1 tile";
        }

        @Override
        int chooseTile(CtmContext context) {
            if (context.face <= CtmContext.UP) {
                return -1;
            }
            return shouldConnect(context, CtmContext.REL_U) ? 0 : -1;
        }
    }

    static final class Random extends TileOverride {
        private final int symmetry;
        private final boolean linked;
        private final WeightedIndex chooser;

        Random(CtmProperties properties) {
            super(properties);
            final String symmetry = properties.get("symmetry", "none");
            this.symmetry = symmetry.equals("all") ? 6 : symmetry.equals("opposite") ? 2 : 1;
            this.linked = Boolean.parseBoolean(properties.get("linked", "false"));
            this.chooser = WeightedIndex.create(this.tileNames.length, properties.get("weights", ""));
        }

        @Override
        String method() {
            return "random";
        }

        @Override
        int chooseTile(CtmContext context) {
            if (this.chooser == null) {
                return -1;
            }
            int y = context.pos.getY();
            if (this.linked && isUpperHalf(context.state)) {
                y--;
            }
            return this.chooser.choose(WeightedIndex.hash128To64(context.pos.getX(), y, context.pos.getZ(), context.face / this.symmetry));
        }

        private static boolean isUpperHalf(IBlockState state) {
            for (var property : state.getPropertyKeys()) {
                if (property.getName().equals("half")) {
                    return "upper".equals(String.valueOf(state.getValue(property)));
                }
            }
            return false;
        }
    }

    static final class Repeat extends TileOverride {
        private final int width;
        private final int height;
        private final int symmetry;

        Repeat(CtmProperties properties) {
            super(properties);
            this.width = parseInt(properties.get("width", "0"));
            this.height = parseInt(properties.get("height", "0"));
            this.symmetry = properties.get("symmetry", "none").equals("opposite") ? ~1 : -1;
        }

        @Override
        String method() {
            return "repeat";
        }

        @Override
        String checkTiles() {
            if (this.width <= 0 || this.height <= 0) {
                return "needs width and height";
            }
            return this.tileNames.length == this.width * this.height ? null : "needs width x height tiles";
        }

        @Override
        int chooseTile(CtmContext context) {
            final int face = context.face & this.symmetry;
            final int x = context.pos.getX();
            final int y = context.pos.getY();
            final int z = context.pos.getZ();
            final int[] right = context.offset(face, CtmContext.REL_R);
            final int[] down = context.offset(face, CtmContext.REL_D);
            int column = x * right[0] + y * right[1] + z * right[2];
            int row = x * down[0] + y * down[1] + z * down[2];
            if (right[0] + right[1] + right[2] < 0) {
                column--;
            }
            column = Math.floorMod(column, this.width);
            row = Math.floorMod(row, this.height);
            return this.width * row + column;
        }
    }

    static final class Fixed extends TileOverride {
        Fixed(CtmProperties properties) {
            super(properties);
        }

        @Override
        String method() {
            return "fixed";
        }

        @Override
        String checkTiles() {
            return this.tileNames.length == 1 ? null : "needs 1 tile";
        }

        @Override
        int chooseTile(CtmContext context) {
            return 0;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
