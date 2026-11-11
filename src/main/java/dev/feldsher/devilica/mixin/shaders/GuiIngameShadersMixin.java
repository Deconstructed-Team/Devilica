/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shader packs that draw their own vignette turn vanilla's off ({@code vignette=false}). */
@Mixin(GuiIngame.class)
public abstract class GuiIngameShadersMixin {

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void devilica$vignette(float lightLevel, ScaledResolution resolution, CallbackInfo ci) {
        if (ShaderHooks.hidesVignette()) {
            ci.cancel();
        }
    }
}
