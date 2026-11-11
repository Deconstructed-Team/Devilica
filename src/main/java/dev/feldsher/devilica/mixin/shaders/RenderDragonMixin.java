/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.renderer.entity.RenderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The beam between an end crystal and what it heals is an entity of its own to shader packs. */
@Mixin(RenderDragon.class)
public abstract class RenderDragonMixin {

    @Unique
    private static final NamespacedId devilica$BEAM = new NamespacedId("minecraft", "end_crystal_beam");

    @Unique
    private static int devilica$previousEntityId = ShaderHooks.NO_ID;

    @Inject(method = "renderCrystalBeams", at = @At("HEAD"))
    private static void devilica$beginBeam(double x, double y, double z, float partialTicks, double beamX, double beamY, double beamZ, int ticks, double targetX, double targetY, double targetZ, CallbackInfo ci) {
        devilica$previousEntityId = ShaderHooks.setSpecialEntityId(devilica$BEAM);
    }

    @Inject(method = "renderCrystalBeams", at = @At("RETURN"))
    private static void devilica$endBeam(double x, double y, double z, float partialTicks, double beamX, double beamY, double beamZ, int ticks, double targetX, double targetY, double targetZ, CallbackInfo ci) {
        ShaderHooks.restoreEntityId(devilica$previousEntityId);
        devilica$previousEntityId = ShaderHooks.NO_ID;
    }
}
