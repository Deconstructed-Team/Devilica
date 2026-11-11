/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Players' name tags off when the render settings say so. */
@Mixin(RenderPlayer.class)
public abstract class RenderPlayerLimitsMixin {

    @Inject(method = "renderEntityName(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDLjava/lang/String;D)V", at = @At("HEAD"), cancellable = true)
    private void devilica$name(AbstractClientPlayer player, double x, double y, double z, String name, double distanceSq, CallbackInfo ci) {
        if (!DevilicaOptions.get().render.playerNames) {
            ci.cancel();
        }
    }
}
