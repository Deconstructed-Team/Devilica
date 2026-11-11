/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.sky;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/** How a sky layer blends over what is behind it, and how its fade shows (MCPatcher's {@code BlendMethod}). */
enum SkyBlend {
    ALPHA(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, true, false, true),
    ADD(GL11.GL_SRC_ALPHA, GL11.GL_ONE, true, false, true),
    SUBTRACT(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO, true, true, false),
    MULTIPLY(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA, true, true, true),
    DODGE(GL11.GL_ONE, GL11.GL_ONE, true, true, false),
    BURN(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR, true, true, false),
    SCREEN(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR, true, true, false),
    OVERLAY(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR, true, true, false),
    REPLACE(0, 0, false, false, true);

    private final int source;
    private final int destination;
    private final boolean blend;
    private final boolean fadeColor;
    private final boolean fadeAlpha;

    SkyBlend(int source, int destination, boolean blend, boolean fadeColor, boolean fadeAlpha) {
        this.source = source;
        this.destination = destination;
        this.blend = blend;
        this.fadeColor = fadeColor;
        this.fadeAlpha = fadeAlpha;
    }

    static SkyBlend parse(String name) {
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "alpha" -> ALPHA;
            case "add" -> ADD;
            case "subtract" -> SUBTRACT;
            case "multiply" -> MULTIPLY;
            case "dodge" -> DODGE;
            case "burn" -> BURN;
            case "screen" -> SCREEN;
            case "overlay", "color" -> OVERLAY;
            case "replace", "none" -> REPLACE;
            default -> null;
        };
    }

    void apply(float fade) {
        if (this.fadeColor && this.fadeAlpha) {
            GlStateManager.color(fade, fade, fade, fade);
        } else if (this.fadeColor) {
            GlStateManager.color(fade, fade, fade, 1.0F);
        } else if (this.fadeAlpha) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, fade);
        }
        if (this.blend) {
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(this.source, this.destination, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
            GlStateManager.disableBlend();
        }
        GlStateManager.enableTexture2D();
    }

    /** Back to vanilla's state for the sun, moon and stars */
    static void restore(float rainFade) {
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, rainFade);
    }
}
