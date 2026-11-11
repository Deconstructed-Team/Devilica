/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeSwamp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swamps' grass and leaves from the packs' swamp colour maps. */
@Mixin(BiomeSwamp.class)
public abstract class BiomeSwampMixin {

    @Inject(method = "getGrassColorAtPos", at = @At("HEAD"), cancellable = true)
    private void devilica$grass(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        final int color = CustomColors.swamp((Biome) (Object) this, pos, false);
        if (color != CustomColors.KEEP) {
            cir.setReturnValue(color);
        }
    }

    @Inject(method = "getFoliageColorAtPos", at = @At("HEAD"), cancellable = true)
    private void devilica$foliage(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        final int color = CustomColors.swamp((Biome) (Object) this, pos, true);
        if (color != CustomColors.KEEP) {
            cir.setReturnValue(color);
        }
    }
}
