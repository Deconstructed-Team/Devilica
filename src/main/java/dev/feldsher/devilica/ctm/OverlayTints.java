/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Overlays tinted by another block's colour ({@code tintBlock}): their quads carry a tint index from this table, which
 * the block colours answer with that block's colour at the position.
 */
public final class OverlayTints {

    private static final int BASE = 0x4F560000;
    private static final int MASK = 0xFFFF0000;

    private record Tint(IBlockState state, int tintIndex) {
    }

    private static final List<Tint> TINTS = new CopyOnWriteArrayList<>();

    private OverlayTints() {
    }

    static synchronized int code(IBlockState state, int tintIndex) {
        final Tint tint = new Tint(state, tintIndex);
        int index = TINTS.indexOf(tint);
        if (index < 0) {
            index = TINTS.size();
            TINTS.add(tint);
        }
        return BASE | index;
    }

    public static boolean isCode(int tintIndex) {
        return (tintIndex & MASK) == BASE;
    }

    public static int color(IBlockAccess world, BlockPos pos, int code) {
        final Tint tint = TINTS.get(code & ~MASK);
        return Minecraft.getMinecraft().getBlockColors().colorMultiplier(tint.state(), world, pos, tint.tintIndex());
    }
}
