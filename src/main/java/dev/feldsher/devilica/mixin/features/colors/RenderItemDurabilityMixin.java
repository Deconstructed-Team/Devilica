/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The durability bar in the colours of {@code durability.png}. */
@Mixin(RenderItem.class)
public abstract class RenderItemDurabilityMixin {

    @WrapOperation(method = "renderItemOverlayIntoGUI", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getRGBDurabilityForDisplay(Lnet/minecraft/item/ItemStack;)I", remap = false))
    private int devilica$durabilityColor(Item item, ItemStack stack, Operation<Integer> original) {
        return CustomColors.durability(item.getDurabilityForDisplay(stack), original.call(item, stack));
    }
}
