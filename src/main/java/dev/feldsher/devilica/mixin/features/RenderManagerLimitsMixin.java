/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.client.RenderLimits;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entities the render settings switch off or keep from drawing past a distance. */
@Mixin(RenderManager.class)
public abstract class RenderManagerLimitsMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void devilica$limits(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (RenderLimits.hidden(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
}
