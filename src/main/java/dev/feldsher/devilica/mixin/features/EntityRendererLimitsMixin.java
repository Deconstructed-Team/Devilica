/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spectating a mob without its post effect (creeper, spider, enderman, and mods' through Forge). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererLimitsMixin {

    @Shadow
    public abstract void stopUseShader();

    @Inject(method = "loadEntityShader", at = @At("HEAD"), cancellable = true)
    private void devilica$noMobView(Entity entity, CallbackInfo ci) {
        if (!DevilicaOptions.get().render.mobViewShaders) {
            this.stopUseShader();
            ci.cancel();
        }
    }
}
