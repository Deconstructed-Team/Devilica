/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.compat;

import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lumenized (with GregTech CEu's bloom, which it carries) hooks the game framebuffer's depth renderbuffer onto its
 * bloom framebuffer. Devilica keeps that depth in a texture and there is no renderbuffer, so the bloom layer drew
 * with no depth to test against, and drive lights glowed through the blocks in front of them. The depth texture is
 * hooked instead, as Lumenized's own {@code hookDepthTexture} would.
 */
@Pseudo
@Mixin(targets = "gregtech.client.utils.RenderUtil", remap = false)
public abstract class LumenizedDepthHookMixin {

    @Inject(method = "hookDepthBuffer", at = @At("HEAD"), cancellable = true)
    private static void devilica$hookDepthTexture(Framebuffer target, int depthBuffer, CallbackInfo ci) {
        if (depthBuffer > -1) {
            return;
        }
        int texture = ((IRenderTargetExt) Minecraft.getMinecraft().getFramebuffer()).iris$getDepthTextureId();
        if (texture < 0) {
            return;
        }
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, target.framebufferObject);
        int attachment = target.isStencilEnabled() ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : OpenGlHelper.GL_DEPTH_ATTACHMENT;
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, texture, 0);
        ci.cancel();
    }
}
