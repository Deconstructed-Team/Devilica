/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.BlockStatePaletteLinear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStatePaletteLinear.class)
public interface BlockStatePaletteLinearAccessor {

    @Accessor("states")
    IBlockState[] devilica$states();

    @Accessor("arraySize")
    int devilica$arraySize();
}
