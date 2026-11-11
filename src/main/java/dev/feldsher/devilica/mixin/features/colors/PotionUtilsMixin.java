/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/** The water bottle's colour from the packs ({@code potion.water}). */
@Mixin(PotionUtils.class)
public abstract class PotionUtilsMixin {

    @Inject(method = "getPotionColorFromEffectList", at = @At("HEAD"), cancellable = true)
    private static void devilica$waterBottle(Collection<PotionEffect> effects, CallbackInfoReturnable<Integer> cir) {
        if (effects.isEmpty()) {
            final int color = CustomColors.waterBottle(-1);
            if (color >= 0) {
                cir.setReturnValue(color);
            }
        }
    }
}
