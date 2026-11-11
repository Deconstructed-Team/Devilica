/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IntIdentityHashBiMap;
import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStatePaletteHashMap.class)
public interface BlockStatePaletteHashMapAccessor {

    @Accessor("statePaletteMap")
    IntIdentityHashBiMap<IBlockState> devilica$map();
}
