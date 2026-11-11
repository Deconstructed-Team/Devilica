/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.hud;

import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.mixin.features.hud.GuiIngameAccessor;
import dev.feldsher.devilica.mixin.features.hud.GuiIngameForgeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.GuiIngameForge;
import org.lwjgl.opengl.GL11;

/**
 * The HUD drawn once a game tick into a framebuffer and laid over every frame from there, instead of drawn anew each
 * frame. It is kept with premultiplied colour (see {@link #isDrawing}) so it lays over exactly. The vignette and the
 * crosshair blend with what is under them, so they stay drawn every frame.
 */
public final class HudCache {

    private static Framebuffer framebuffer;
    private static int drawnAt = Integer.MIN_VALUE;
    private static int drawnSlot = -1;
    private static GuiScreen drawnUnder;
    private static boolean drawing;

    private HudCache() {
    }

    /** Whether the HUD is being drawn into the cache, so ordinary blending keeps the alpha it needs */
    public static boolean isDrawing() {
        return drawing;
    }

    /** Draws the HUD, through the cache when it is on */
    public static void render(GuiIngame gui, float partialTicks, Runnable hud) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (!DevilicaOptions.get().hudCaching || !OpenGlHelper.isFramebufferEnabled() || !(gui instanceof GuiIngameForge) || minecraft.player == null) {
            hud.run();
            return;
        }
        final int width = minecraft.displayWidth;
        final int height = minecraft.displayHeight;
        if (framebuffer == null) {
            framebuffer = new Framebuffer(width, height, true);
            framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            drawnAt = Integer.MIN_VALUE;
        } else if (framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            framebuffer.createBindFramebuffer(width, height);
            drawnAt = Integer.MIN_VALUE;
        }
        final int slot = minecraft.player.inventory.currentItem;
        if (gui.getUpdateCounter() != drawnAt || slot != drawnSlot || minecraft.currentScreen != drawnUnder) {
            drawnAt = gui.getUpdateCounter();
            drawnSlot = slot;
            drawnUnder = minecraft.currentScreen;
            redraw(minecraft, hud);
        }

        final ScaledResolution resolution = new ScaledResolution(minecraft);
        GlStateManager.enableBlend();
        if (GuiIngameForge.renderVignette && Minecraft.isFancyGraphicsEnabled()) {
            ((GuiIngameAccessor) gui).devilica$renderVignette(minecraft.player.getBrightness(), resolution);
        }
        layOver(resolution);
        if (GuiIngameForge.renderCrosshairs) {
            ((GuiIngameForgeAccessor) gui).devilica$renderCrosshairs(partialTicks);
        }
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void redraw(Minecraft minecraft, Runnable hud) {
        // Clearing binds the framebuffer and then unbinds it
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        final boolean vignette = GuiIngameForge.renderVignette;
        final boolean crosshairs = GuiIngameForge.renderCrosshairs;
        GuiIngameForge.renderVignette = false;
        GuiIngameForge.renderCrosshairs = false;
        drawing = true;
        try {
            hud.run();
        } finally {
            drawing = false;
            GuiIngameForge.renderVignette = vignette;
            GuiIngameForge.renderCrosshairs = crosshairs;
            minecraft.getFramebuffer().bindFramebuffer(false);
        }
    }

    /** The cached HUD over the screen, as premultiplied colour */
    private static void layOver(ScaledResolution resolution) {
        GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        framebuffer.bindFramebufferTexture();
        final double width = resolution.getScaledWidth_double();
        final double height = resolution.getScaledHeight_double();
        final double u = (double) framebuffer.framebufferWidth / framebuffer.framebufferTextureWidth;
        final double v = (double) framebuffer.framebufferHeight / framebuffer.framebufferTextureHeight;
        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0.0, height, 0.0).tex(0.0, 0.0).endVertex();
        buffer.pos(width, height, 0.0).tex(u, 0.0).endVertex();
        buffer.pos(width, 0.0, 0.0).tex(u, v).endVertex();
        buffer.pos(0.0, 0.0, 0.0).tex(0.0, v).endVertex();
        tessellator.draw();
        framebuffer.unbindFramebufferTexture();
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
    }
}
