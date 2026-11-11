/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.hud;

import dev.feldsher.devilica.hud.HudCache;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the HUD is drawn into its cache, ordinary blending also adds up alpha as "over" does, so the cache holds
 * premultiplied colour with the right coverage and lays over the screen exactly.
 */
@Mixin(GlStateManager.class)
public abstract class GlStateManagerHudMixin {

    @Inject(method = "blendFunc(II)V", at = @At("HEAD"), cancellable = true)
    private static void devilica$hudBlend(int srcFactor, int dstFactor, CallbackInfo ci) {
        if (HudCache.isDrawing() && srcFactor == GL11.GL_SRC_ALPHA && dstFactor == GL11.GL_ONE_MINUS_SRC_ALPHA) {
            ci.cancel();
            GlStateManager.tryBlendFuncSeparate(srcFactor, dstFactor, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    @Inject(method = "tryBlendFuncSeparate(IIII)V", at = @At("HEAD"), cancellable = true)
    private static void devilica$hudBlendSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, CallbackInfo ci) {
        if (HudCache.isDrawing() && srcRgb == GL11.GL_SRC_ALPHA && dstRgb == GL11.GL_ONE_MINUS_SRC_ALPHA
                && (srcAlpha != GL11.GL_ONE || dstAlpha != GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            ci.cancel();
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
}
