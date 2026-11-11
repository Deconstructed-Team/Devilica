/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.RenderScale;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With the world at a lower resolution, binding a framebuffer without its viewport keeps the last one, which belongs
 * to a target of another size: the world target after the window-sized entity outline target, and the other way
 * round. Each framebuffer bound then gets its own viewport. The shadow pass sets and restores its own.
 */
@Mixin(Framebuffer.class)
public abstract class FramebufferRenderScaleMixin {

    @Shadow
    public int framebufferWidth;

    @Shadow
    public int framebufferHeight;

    @Inject(method = "bindFramebuffer", at = @At("RETURN"))
    private void devilica$ownViewport(boolean setViewport, CallbackInfo ci) {
        if (!setViewport && RenderScale.isActive() && !ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            GlStateManager.viewport(0, 0, this.framebufferWidth, this.framebufferHeight);
        }
    }
}
