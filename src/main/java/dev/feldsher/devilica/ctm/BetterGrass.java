/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OptiFine's better grass: the sides of grass, mycelium and podzol show their top (or snow under snow), all of them
 * (fast) or where the ground runs on at the same height, so a slope looks covered (fancy).
 */
final class BetterGrass {

    private static volatile Sprites sprites;

    private record Sprites(TextureAtlasSprite grassSide, TextureAtlasSprite grassSideSnowed, TextureAtlasSprite grassOverlay,
                           TextureAtlasSprite grassTop, TextureAtlasSprite myceliumSide, TextureAtlasSprite myceliumTop,
                           TextureAtlasSprite podzolSide, TextureAtlasSprite podzolTop, TextureAtlasSprite snow) {
    }

    /** Retextured sides: [0] as the top, [1] as snow */
    private static final Map<BakedQuad, BakedQuad[]> CACHE = new ConcurrentHashMap<>();

    private BetterGrass() {
    }

    static void load(TextureMap map) {
        CACHE.clear();
        sprites = new Sprites(sprite(map, "grass_side"), sprite(map, "grass_side_snowed"), sprite(map, "grass_side_overlay"),
                sprite(map, "grass_top"), sprite(map, "mycelium_side"), sprite(map, "mycelium_top"),
                sprite(map, "dirt_podzol_side"), sprite(map, "dirt_podzol_top"), sprite(map, "snow"));
    }

    private static TextureAtlasSprite sprite(TextureMap map, String name) {
        return map.getAtlasSprite("minecraft:blocks/" + name);
    }

    static boolean affects(IBlockState state) {
        final Block block = state.getBlock();
        return block == Blocks.GRASS || block == Blocks.MYCELIUM || (block == Blocks.DIRT && isPodzol(state));
    }

    private static boolean isPodzol(IBlockState state) {
        return state.getBlock() == Blocks.DIRT && state.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.PODZOL;
    }

    /**
     * A side quad of grass, mycelium or podzol as better grass draws it.
     *
     * @return the quad itself, its replacement, or null to leave it out (the grass overlay under a full top texture)
     */
    static BakedQuad apply(BakedQuad quad, IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing face, BetterGrassMode mode) {
        final Sprites sprites = BetterGrass.sprites;
        if (sprites == null) {
            return quad;
        }
        final TextureAtlasSprite sprite = quad.getSprite();
        final boolean overlay = sprite == sprites.grassOverlay;
        if (!overlay && sprite != sprites.grassSide && sprite != sprites.grassSideSnowed && sprite != sprites.myceliumSide && sprite != sprites.podzolSide) {
            return quad;
        }
        final boolean snowy = isSnow(world.getBlockState(pos.up()));
        if (mode == BetterGrassMode.FANCY) {
            final BlockPos side = pos.offset(face);
            if (snowy) {
                if (!isSnow(world.getBlockState(side))) {
                    return quad;
                }
            } else {
                final IBlockState below = world.getBlockState(side.down());
                if (below.getBlock() != state.getBlock() || (state.getBlock() == Blocks.DIRT && !isPodzol(below))) {
                    return quad;
                }
            }
        }
        if (overlay) {
            return null;
        }
        final Block block = state.getBlock();
        final TextureAtlasSprite top = snowy ? sprites.snow : block == Blocks.GRASS ? sprites.grassTop : block == Blocks.MYCELIUM ? sprites.myceliumTop : sprites.podzolTop;
        final int index = snowy ? 1 : 0;
        final BakedQuad[] cached = CACHE.computeIfAbsent(quad, q -> new BakedQuad[2]);
        BakedQuad result = cached[index];
        if (result == null) {
            // Grass's top is tinted by the biome, as its overlay was
            result = Quads.retexture(quad, top, !snowy && block == Blocks.GRASS ? 0 : -1);
            cached[index] = result;
        }
        return result;
    }

    private static boolean isSnow(IBlockState state) {
        final Material material = state.getMaterial();
        return material == Material.SNOW || material == Material.CRAFTED_SNOW;
    }
}
