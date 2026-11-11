/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import dev.feldsher.devilica.ctm.OverlayTints;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resource packs' block colour maps over the blocks' own colours. */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {

    @Inject(method = "colorMultiplier", at = @At("HEAD"), cancellable = true)
    private void devilica$customColor(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex, CallbackInfoReturnable<Integer> cir) {
        if (OverlayTints.isCode(tintIndex)) {
            cir.setReturnValue(OverlayTints.color(world, pos, tintIndex));
            return;
        }
        final int color = CustomColors.blockColor(state, world, pos, tintIndex);
        if (color != CustomColors.KEEP) {
            cir.setReturnValue(color);
        }
    }
}
