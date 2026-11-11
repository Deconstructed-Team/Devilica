/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.tesr;

import dev.feldsher.devilica.client.RenderLimits;
import dev.feldsher.devilica.colors.CustomColors;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The text of batched signs, drawn after their boards the way {@code TileEntitySignRenderer} draws it: line by line
 * through the game's font renderer, so a font mod draws it too. Each sign's lines are laid out once (vanilla wraps and
 * measures every line of every sign every frame) and again when its text changes, and every few seconds for a font
 * that changed its widths. A font mod that holds text over the block entity pass (see {@code DevilicaApi}) draws all
 * of it at once.
 */
final class SignTexts {
    private static final long RELAYOUT_NANOS = 2_000_000_000L;

    private static final Map<TileEntitySign, Layout> LAYOUTS = new WeakHashMap<>();
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static int size;

    private SignTexts() {
    }

    static void add(TileEntitySign sign, boolean standing, int meta, double x, double y, double z, int light, int blockEntityId) {
        // Sign text writes no depth, so it casts no shadow
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered() || RenderLimits.signTextHidden(sign, x, y, z)) {
            return;
        }
        final Layout layout = layout(sign);
        if (layout.empty) {
            return;
        }
        if (size == ENTRIES.size()) {
            ENTRIES.add(new Entry());
        }
        final Entry entry = ENTRIES.get(size++);
        entry.layout = layout;
        entry.standing = standing;
        entry.meta = meta;
        entry.x = (float) x;
        entry.y = (float) y;
        entry.z = (float) z;
        entry.light = light;
        entry.blockEntityId = blockEntityId;
    }

    static void draw() {
        if (size == 0) {
            return;
        }
        final FontRenderer font = TileEntityRendererDispatcher.instance.getFontRenderer();
        final int color = CustomColors.signText(0);
        GlStateManager.enableRescaleNormal();
        GlStateManager.depthMask(false);
        for (int i = 0; i < size; i++) {
            final Entry entry = ENTRIES.get(i);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, entry.light & 0xFFFF, entry.light >>> 16);
            CapturedRenderingState.INSTANCE.setCurrentBlockEntity(entry.blockEntityId);
            GlStateManager.pushMatrix();
            GlStateManager.translate(entry.x + 0.5F, entry.y + 0.5F, entry.z + 0.5F);
            if (entry.standing) {
                GlStateManager.rotate(-(entry.meta * 360) / 16.0F, 0.0F, 1.0F, 0.0F);
            } else {
                GlStateManager.rotate(-wallAngle(entry.meta), 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
            }
            GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
            GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
            GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
            final Layout layout = entry.layout;
            for (int j = 0; j < layout.lines.length; j++) {
                if (layout.lines[j] != null) {
                    font.drawString(layout.lines[j], layout.xs[j], j * 10 - layout.lines.length * 5, color);
                }
            }
            GlStateManager.popMatrix();
            entry.layout = null;
        }
        size = 0;
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableRescaleNormal();
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(0);
    }

    private static float wallAngle(int meta) {
        return switch (meta) {
            case 2 -> 180.0F;
            case 4 -> 90.0F;
            case 5 -> -90.0F;
            default -> 0.0F;
        };
    }

    // TileEntitySignRenderer's lines: the first wrapped piece of each, marked while it is edited, centred
    private static Layout layout(TileEntitySign sign) {
        final FontRenderer font = TileEntityRendererDispatcher.instance.getFontRenderer();
        final long now = System.nanoTime();
        Layout layout = LAYOUTS.get(sign);
        if (layout != null && layout.font == font && layout.editing == sign.lineBeingEdited && now - layout.at < RELAYOUT_NANOS
            && layout.same(sign.signText)) {
            return layout;
        }
        if (layout == null || layout.source.length != sign.signText.length) {
            layout = new Layout(sign.signText.length);
            LAYOUTS.put(sign, layout);
        }
        layout.font = font;
        layout.editing = sign.lineBeingEdited;
        layout.at = now;
        layout.empty = true;
        for (int j = 0; j < sign.signText.length; j++) {
            final ITextComponent component = sign.signText[j];
            layout.source[j] = component;
            layout.lines[j] = null;
            if (component == null) {
                continue;
            }
            final List<ITextComponent> pieces = GuiUtilRenderComponents.splitText(component, 90, font, false, true);
            String line = pieces != null && !pieces.isEmpty() ? pieces.get(0).getFormattedText() : "";
            if (j == sign.lineBeingEdited) {
                line = "> " + line + " <";
            }
            if (!line.isEmpty()) {
                layout.lines[j] = line;
                layout.xs[j] = -font.getStringWidth(line) / 2;
                layout.empty = false;
            }
        }
        return layout;
    }

    private static final class Layout {
        final ITextComponent[] source;
        final String[] lines;
        final int[] xs;
        FontRenderer font;
        int editing;
        long at;
        boolean empty;

        Layout(int count) {
            this.source = new ITextComponent[count];
            this.lines = new String[count];
            this.xs = new int[count];
        }

        // The sign swaps in new components when its text changes
        boolean same(ITextComponent[] text) {
            for (int j = 0; j < text.length; j++) {
                if (text[j] != this.source[j]) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class Entry {
        Layout layout;
        boolean standing;
        int meta;
        float x;
        float y;
        float z;
        int light;
        int blockEntityId;
    }
}
