/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.item.EnumDyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dye colours from the packs ({@code dye.<color>}): banners, shields and whatever else asks the dye. */
@Mixin(EnumDyeColor.class)
public abstract class EnumDyeColorMixin {

    @ModifyReturnValue(method = "getColorComponentValues", at = @At("RETURN"))
    private float[] devilica$components(float[] color) {
        return CustomColors.dyeComponents((EnumDyeColor) (Object) this, color);
    }

    @ModifyReturnValue(method = "getColorValue", at = @At("RETURN"))
    private int devilica$value(int color) {
        return CustomColors.dyeValue((EnumDyeColor) (Object) this, color);
    }
}
