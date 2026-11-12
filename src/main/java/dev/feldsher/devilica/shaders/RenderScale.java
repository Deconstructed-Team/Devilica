/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.shaders;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * With a shader pack, the world can be drawn at a share of the window's resolution and stretched to it, which makes
 * heavy packs cheaper. A smaller framebuffer stands in for Minecraft's main one while the world is drawn, so the pack's
 * render targets and its {@code viewWidth} follow it; the GUI stays sharp. It is stretched back with AMD's FSR 1 or
 * bilinearly. After Celeritas's {@code RenderScale} (LGPL-3.0), which does this for modern Minecraft.
 */
public final class RenderScale {

    private static Framebuffer worldTarget;
    private static boolean active;
    private static FsrUpscaler fsr;
    /** FSR failed to build on this driver; bilinear from then on */
    private static boolean fsrUnavailable;

    private RenderScale() {
    }

    public static boolean isActive() {
        return active;
    }

    public static Framebuffer worldTarget() {
        return worldTarget;
    }

    private static float scale() {
        final int percent = DevilicaOptions.get().renderScale;
        if (percent >= 100 || !IrisApiV0Impl.INSTANCE.isShaderPackInUse()) {
            return 1.0F;
        }
        return Math.max(25, percent) / 100.0F;
    }

    /** Before the world is drawn: binds the smaller target, made or resized as needed */
    public static void begin() {
        if (active) {
            return;
        }
        final float scale = scale();
        if (scale >= 1.0F) {
            release();
            return;
        }
        // Not active yet, so this is Minecraft's own
        final Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        final int width = Math.max(1, MathHelper.ceil(main.framebufferWidth * scale));
        final int height = Math.max(1, MathHelper.ceil(main.framebufferHeight * scale));
        if (worldTarget == null) {
            worldTarget = new Framebuffer(width, height, true);
            worldTarget.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        } else if (worldTarget.framebufferWidth != width || worldTarget.framebufferHeight != height) {
            worldTarget.createBindFramebuffer(width, height);
        }
        if (main.isStencilEnabled() && !worldTarget.isStencilEnabled()) {
            worldTarget.enableStencil();
        }
        active = true;
        worldTarget.bindFramebuffer(true);
    }

    /** A frame that threw midway never reached {@link #end()}: its picture is not stretched over what comes next */
    public static void abandon() {
        active = false;
    }

    /** After the world is drawn: stretches it into Minecraft's main framebuffer and binds that for the GUI */
    public static void end() {
        if (!active) {
            return;
        }
        active = false;
        final Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        final DevilicaOptions options = DevilicaOptions.get();
        if (options.fsr && !fsrUnavailable) {
            if (fsr == null) {
                fsr = FsrUpscaler.createOrNull(main.framebufferWidth, main.framebufferHeight);
                fsrUnavailable = fsr == null;
            }
            if (fsr != null) {
                fsr.process(worldTarget, main, (100 - MathHelper.clamp(options.fsrSharpness, 0, 100)) * 0.02F);
                return;
            }
        }
        OpenGlHelper.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, worldTarget.framebufferObject);
        OpenGlHelper.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.framebufferObject);
        GL30.glBlitFramebuffer(0, 0, worldTarget.framebufferWidth, worldTarget.framebufferHeight,
            0, 0, main.framebufferWidth, main.framebufferHeight, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        main.bindFramebuffer(true);
    }

    private static void release() {
        if (worldTarget != null) {
            worldTarget.deleteFramebuffer();
            worldTarget = null;
        }
        if (fsr != null) {
            fsr.destroy();
            fsr = null;
        }
        fsrUnavailable = false;
    }
}
