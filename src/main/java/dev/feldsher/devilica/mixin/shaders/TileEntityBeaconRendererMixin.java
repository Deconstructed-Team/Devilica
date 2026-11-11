/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.tileentity.TileEntityBeacon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The beacon beam draws with the pack's beacon beam program. */
@Mixin(TileEntityBeaconRenderer.class)
public abstract class TileEntityBeaconRendererMixin {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityBeacon;DDDFIF)V", at = @At("HEAD"))
    private void devilica$beginBeam(TileEntityBeacon beacon, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        ShaderHooks.beginCondition(SpecialCondition.BEACON_BEAM);
    }

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityBeacon;DDDFIF)V", at = @At("RETURN"))
    private void devilica$endBeam(TileEntityBeacon beacon, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        ShaderHooks.endCondition();
    }
}
