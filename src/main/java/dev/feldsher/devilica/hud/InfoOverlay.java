/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.hud;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Celeritas Extra's overlay: FPS (with the average and the 1% and 0.1% lows over the last ten seconds), coordinates and
 * memory in a corner of the screen. Hidden while F3 is open.
 */
public final class InfoOverlay {
    public static final InfoOverlay INSTANCE = new InfoOverlay();

    private static final long WINDOW_NANOS = 10_000_000_000L;
    private static final long STATS_EVERY_NANOS = 500_000_000L;

    private final long[] frames = new long[8192];
    private int head;
    private int count;
    private long lastFrame;
    private long statsAt;
    private int average;
    private int low1;
    private int low01;

    private InfoOverlay() {
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        final long now = System.nanoTime();
        if (this.lastFrame != 0) {
            this.frames[this.head] = now - this.lastFrame;
            this.head = (this.head + 1) % this.frames.length;
            this.count = Math.min(this.count + 1, this.frames.length);
        }
        this.lastFrame = now;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        final DevilicaOptions.HudSettings hud = DevilicaOptions.get().hud;
        if (mc.gameSettings.showDebugInfo || !(hud.fps || hud.coordinates || hud.memory)) {
            return;
        }
        final List<String> lines = new ArrayList<>(3);
        if (hud.fps) {
            lines.add(fpsLine(hud.fpsExtended));
        }
        final Entity view = mc.getRenderViewEntity();
        if (hud.coordinates && view != null && !mc.isReducedDebug()) {
            lines.add(String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f", view.posX, view.posY, view.posZ));
        }
        if (hud.memory) {
            final Runtime runtime = Runtime.getRuntime();
            final long used = (runtime.totalMemory() - runtime.freeMemory()) >> 20;
            lines.add("Mem " + used + " / " + (runtime.maxMemory() >> 20) + " MiB");
        }
        draw(mc, event.getResolution(), lines, hud);
    }

    private String fpsLine(boolean extended) {
        final int fps = Minecraft.getDebugFPS();
        if (!extended) {
            return fps + " FPS";
        }
        final long now = System.nanoTime();
        if (now - this.statsAt > STATS_EVERY_NANOS) {
            this.statsAt = now;
            stats();
        }
        return fps + " FPS  avg " + this.average + "  1% " + this.low1 + "  0.1% " + this.low01;
    }

    // Over the frames of the last ten seconds: the average, and the frame rates the 99th and 99.9th percentile frames run at
    private void stats() {
        int n = 0;
        long total = 0;
        final long[] recent = new long[this.count];
        for (int i = 1; i <= this.count && total < WINDOW_NANOS; i++) {
            final long frame = this.frames[Math.floorMod(this.head - i, this.frames.length)];
            recent[n++] = frame;
            total += frame;
        }
        if (n == 0 || total == 0) {
            return;
        }
        Arrays.sort(recent, 0, n);
        this.average = (int) Math.round(n * 1.0E9 / total);
        this.low1 = (int) Math.round(1.0E9 / recent[Math.min(n - 1, (int) (n * 0.99))]);
        this.low01 = (int) Math.round(1.0E9 / recent[Math.min(n - 1, (int) (n * 0.999))]);
    }

    private static void draw(Minecraft mc, ScaledResolution resolution, List<String> lines, DevilicaOptions.HudSettings hud) {
        final FontRenderer font = mc.fontRenderer;
        final boolean right = hud.corner == OverlayCorner.TOP_RIGHT || hud.corner == OverlayCorner.BOTTOM_RIGHT;
        final boolean bottom = hud.corner == OverlayCorner.BOTTOM_LEFT || hud.corner == OverlayCorner.BOTTOM_RIGHT;
        final int lineHeight = font.FONT_HEIGHT + 1;
        int y = bottom ? resolution.getScaledHeight() - 2 - lines.size() * lineHeight : 2;
        for (String line : lines) {
            final int width = font.getStringWidth(line);
            final int x = right ? resolution.getScaledWidth() - 2 - width : 2;
            if (hud.textContrast == TextContrast.BACKGROUND) {
                Gui.drawRect(x - 1, y - 1, x + width + 1, y + font.FONT_HEIGHT, 0x90505050);
            }
            if (hud.textContrast == TextContrast.SHADOW) {
                font.drawStringWithShadow(line, x, y, 0xFFFFFF);
            } else {
                font.drawString(line, x, y, 0xFFFFFF);
            }
            y += lineHeight;
        }
    }
}
