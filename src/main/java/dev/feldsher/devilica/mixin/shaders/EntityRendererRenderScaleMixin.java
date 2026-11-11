/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.shaders.RenderScale;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The world drawn into the lower resolution target and stretched back before the GUI. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererRenderScaleMixin {

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void devilica$beginScaledWorld(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        RenderScale.begin();
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void devilica$endScaledWorld(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        RenderScale.end();
    }

    /** Should the world's drawing end early, the main framebuffer still comes back */
    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void devilica$ensureScaledWorldEnded(float partialTicks, long nanoTime, CallbackInfo ci) {
        RenderScale.end();
    }

    /** The world pass sets the window's viewport before clearing; the target is smaller */
    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;viewport(IIII)V"))
    private void devilica$scaledViewport(int x, int y, int width, int height, Operation<Void> original) {
        if (RenderScale.isActive()) {
            original.call(0, 0, RenderScale.worldTarget().framebufferWidth, RenderScale.worldTarget().framebufferHeight);
        } else {
            original.call(x, y, width, height);
        }
    }
}
