/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.tileentity.TileEntityEnchantmentTableRenderer;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The book over enchanting tables off when the render settings say so. */
@Mixin(TileEntityEnchantmentTableRenderer.class)
public abstract class TileEntityEnchantmentTableRendererLimitsMixin {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityEnchantmentTable;DDDFIF)V", at = @At("HEAD"), cancellable = true)
    private void devilica$book(TileEntityEnchantmentTable table, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        if (!DevilicaOptions.get().render.enchantingBooks) {
            ci.cancel();
        }
    }
}
