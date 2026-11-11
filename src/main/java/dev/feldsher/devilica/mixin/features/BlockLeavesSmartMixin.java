/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.leaves.LeavesMode;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Smart leaves: see-through, but no face between two leaves of the same kind. */
@Mixin(BlockLeaves.class)
public abstract class BlockLeavesSmartMixin {

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void devilica$smartLeaves(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        if (DevilicaOptions.get().leaves == LeavesMode.SMART && world.getBlockState(pos.offset(side)).getBlock() == (Object) this) {
            cir.setReturnValue(false);
        }
    }
}
