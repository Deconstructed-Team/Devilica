/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.tileentity.TileEntityPistonRenderer;
import net.minecraft.tileentity.TileEntityPiston;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The block a piston moves, not drawn while it moves when the render settings say so. */
@Mixin(TileEntityPistonRenderer.class)
public abstract class TileEntityPistonRendererLimitsMixin {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityPiston;DDDFIF)V", at = @At("HEAD"), cancellable = true)
    private void devilica$moving(TileEntityPiston piston, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        if (!DevilicaOptions.get().render.pistons) {
            ci.cancel();
        }
    }
}
