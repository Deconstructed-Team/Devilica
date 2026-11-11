/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spawn eggs in the packs' colours ({@code egg.shell.<entity>}, {@code egg.spots.<entity>}). */
@Mixin(ItemColors.class)
public abstract class ItemColorsMixin {

    @Inject(method = "colorMultiplier", at = @At("HEAD"), cancellable = true)
    private void devilica$spawnEgg(ItemStack stack, int tintIndex, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() == Items.SPAWN_EGG) {
            final int color = CustomColors.spawnEgg(stack, tintIndex);
            if (color != CustomColors.KEEP) {
                cir.setReturnValue(color);
            }
        }
    }
}
