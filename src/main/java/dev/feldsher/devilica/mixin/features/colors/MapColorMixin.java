/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.block.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Map colours from the packs ({@code map.<color>}). */
@Mixin(MapColor.class)
public abstract class MapColorMixin {

    @ModifyExpressionValue(method = "getMapColor", at = @At(value = "FIELD", target = "Lnet/minecraft/block/material/MapColor;colorValue:I"))
    private int devilica$mapColor(int color) {
        return CustomColors.mapColor((MapColor) (Object) this, color);
    }
}
