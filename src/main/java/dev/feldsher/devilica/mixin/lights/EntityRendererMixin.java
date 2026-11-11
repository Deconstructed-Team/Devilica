/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import dev.feldsher.devilica.dynamiclights.DynamicLights;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moved lights relight the terrain before the frame sets it up. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow @Final
    private Minecraft mc;

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void devilica$updateDynamicLights(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        this.mc.profiler.startSection("devilica_dynamic_lights");
        DynamicLights.update();
        this.mc.profiler.endSection();
    }
}
