/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.renderer.entity.layers.LayerSheepWool;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.item.EnumDyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sheep wool in the packs' colours ({@code sheep.<color>}). */
@Mixin(LayerSheepWool.class)
public abstract class LayerSheepWoolMixin {

    @WrapOperation(method = "doRenderLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/EntitySheep;getDyeRgb(Lnet/minecraft/item/EnumDyeColor;)[F"))
    private float[] devilica$woolColor(EnumDyeColor dye, Operation<float[]> original) {
        return CustomColors.sheep(dye, original.call(dye));
    }
}
