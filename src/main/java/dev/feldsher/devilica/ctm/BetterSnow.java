/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * OptiFine's better snow: plants, fences, panes, walls and the like standing in snow get a snow layer under them, so
 * the snow does not stop in a ring around them.
 */
public final class BetterSnow {

    public static final IBlockState SNOW = Blocks.SNOW_LAYER.getDefaultState();

    private BetterSnow() {
    }

    /** Whether to draw a snow layer at the position, under the block there */
    public static boolean shouldRender(IBlockAccess world, IBlockState state, BlockPos pos) {
        if (!DevilicaOptions.get().betterSnow || !standsInSnow(state)) {
            return false;
        }
        final BlockPos.MutableBlockPos around = new BlockPos.MutableBlockPos();
        boolean snow = false;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            around.setPos(pos.getX() + facing.getXOffset(), pos.getY(), pos.getZ() + facing.getZOffset());
            if (world.getBlockState(around).getBlock() == Blocks.SNOW_LAYER) {
                snow = true;
                break;
            }
        }
        return snow && world.getBlockState(around.setPos(pos.getX(), pos.getY() - 1, pos.getZ())).isOpaqueCube();
    }

    private static boolean standsInSnow(IBlockState state) {
        final Block block = state.getBlock();
        if (state.isFullCube() || state.isOpaqueCube() || block instanceof BlockSnow) {
            return false;
        }
        if (block instanceof BlockBush || block instanceof BlockFence || block instanceof BlockFenceGate || block instanceof BlockFlowerPot
                || block instanceof BlockPane || block instanceof BlockReed || block instanceof BlockWall) {
            return true;
        }
        if (block instanceof BlockRedstoneTorch) {
            return state.getValue(BlockTorch.FACING) == EnumFacing.UP;
        }
        if (block instanceof BlockLever) {
            final BlockLever.EnumOrientation orientation = state.getValue(BlockLever.FACING);
            return orientation == BlockLever.EnumOrientation.UP_X || orientation == BlockLever.EnumOrientation.UP_Z;
        }
        return false;
    }
}
