/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.tileentity.TileEntityBeacon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Beacon beams off when the render settings say so. */
@Mixin(TileEntityBeaconRenderer.class)
public abstract class TileEntityBeaconRendererLimitsMixin {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityBeacon;DDDFIF)V", at = @At("HEAD"), cancellable = true)
    private void devilica$beam(TileEntityBeacon beacon, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        if (!DevilicaOptions.get().render.beaconBeams) {
            ci.cancel();
        }
    }
}
