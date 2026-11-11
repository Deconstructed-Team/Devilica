/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.renderer.entity.layers.LayerWolfCollar;
import net.minecraft.item.EnumDyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wolf collars in the packs' colours ({@code collar.<color>}). */
@Mixin(LayerWolfCollar.class)
public abstract class LayerWolfCollarMixin {

    @WrapOperation(method = "doRenderLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/EnumDyeColor;getColorComponentValues()[F"))
    private float[] devilica$collarColor(EnumDyeColor dye, Operation<float[]> original) {
        return CustomColors.collar(dye, original.call(dye));
    }
}
