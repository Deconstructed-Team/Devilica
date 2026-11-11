/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

/**
 * Framebuffers keep their depth in a texture instead of a renderbuffer, so shader packs can sample the main
 * framebuffer depth. Buffer versions let Iris notice a resize.
 */
@Mixin(Framebuffer.class)
public abstract class FramebufferMixin implements IRenderTargetExt {

    @Shadow
    public boolean useDepth;

    @Shadow(remap = false)
    private boolean stencilEnabled;

    @Unique private int devilica$depthBufferVersion;
    @Unique private int devilica$colorBufferVersion;
    @Unique private int devilica$depthTextureId = -1;

    @Override
    public int iris$getDepthBufferVersion() {
        return this.devilica$depthBufferVersion;
    }

    @Override
    public int iris$getColorBufferVersion() {
        return this.devilica$colorBufferVersion;
    }

    @Override
    public int iris$getDepthTextureId() {
        return this.devilica$depthTextureId;
    }

    @Inject(method = "deleteFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;unbindFramebuffer()V", shift = At.Shift.AFTER))
    private void devilica$onDeleteBuffers(CallbackInfo ci) {
        this.devilica$depthBufferVersion++;
        this.devilica$colorBufferVersion++;
        if (this.devilica$depthTextureId > -1) {
            GLStateManager.glDeleteTextures(this.devilica$depthTextureId);
            this.devilica$depthTextureId = -1;
        }
    }

    /** No depth renderbuffer; the depth texture below takes its place. */
    @Redirect(method = "createFramebuffer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/shader/Framebuffer;useDepth:Z"))
    private boolean devilica$noDepthRenderbuffer(Framebuffer framebuffer) {
        return false;
    }

    @Inject(method = "createFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferClear()V", ordinal = 1))
    private void devilica$createDepthTexture(int width, int height, CallbackInfo ci) {
        if (!this.useDepth) {
            return;
        }
        this.devilica$depthTextureId = GL11.glGenTextures();
        GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, this.devilica$depthTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
        if (this.stencilEnabled) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, (IntBuffer) null);
            OpenGlHelper.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.devilica$depthTextureId, 0);
            OpenGlHelper.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, this.devilica$depthTextureId, 0);
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer) null);
            OpenGlHelper.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.devilica$depthTextureId, 0);
        }
    }
}
