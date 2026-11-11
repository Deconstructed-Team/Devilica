/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Potion effects' colours from the packs ({@code potion.<name>}). */
@Mixin(Potion.class)
public abstract class PotionMixin {

    @ModifyReturnValue(method = "getLiquidColor", at = @At("RETURN"))
    private int devilica$liquidColor(int color) {
        return CustomColors.potion((Potion) (Object) this, color);
    }
}
