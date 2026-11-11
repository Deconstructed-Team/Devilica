/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * One layer of a custom sky: a 3x2 skybox image that fades in and out by the time of day and turns with the sun
 * (MCPatcher's better skies, as OptiFine reads them).
 */
final class SkyLayer {

    private static final int SECONDS_PER_DAY = 24 * 60 * 60;
    private static final int TICKS_PER_DAY = 24000;
    /** Tick 0 is six in the morning */
    private static final double TIME_OFFSET = -0.25;
    private static final double DISTANCE = 100.0;

    final ResourceLocation texture;
    final SkyBlend blend;
    private final boolean fade;
    private final boolean rotate;
    private final float speed;
    private final float[] axis;
    /** Weather the layer shows in: clear, rain, thunder; null for MCPatcher's way (dimmed by rain) */
    private final boolean[] weather;
    /** Fade curve f(x) = a cos x + b sin x + c through the fade times */
    private double a;
    private double b;
    private double c;

    final String problem;

    SkyLayer(Properties properties, ResourceLocation texture, boolean celestial, List<String> warnings) {
        this.texture = texture;
        final SkyBlend blend = SkyBlend.parse(get(properties, "blend", "add"));
        this.blend = blend == null ? SkyBlend.ADD : blend;
        if (blend == null) {
            warnings.add("unknown blend " + get(properties, "blend", ""));
        }
        this.rotate = celestial || Boolean.parseBoolean(get(properties, "rotate", "true"));
        this.speed = parseFloat(get(properties, "speed", "1.0"), 1.0F);
        final float[] axis = parseAxis(get(properties, "axis", "0.0 0.0 1.0"));
        // MCPatcher turns the axis into the sky's own frame
        this.axis = new float[]{axis[2], axis[1], -axis[0]};
        this.weather = parseWeather(get(properties, "weather", ""));
        this.fade = !celestial && !"false".equalsIgnoreCase(get(properties, "fade", "true")) && properties.getProperty("startFadeIn") != null;
        this.problem = this.fade ? readFadeTimes(properties) : null;
    }

    private String readFadeTimes(Properties properties) {
        final int startFadeIn = parseTime(get(properties, "startFadeIn", ""));
        int endFadeIn = parseTime(get(properties, "endFadeIn", ""));
        int endFadeOut = parseTime(get(properties, "endFadeOut", ""));
        if (startFadeIn < 0 || endFadeIn < 0 || endFadeOut < 0) {
            return "fade times need startFadeIn, endFadeIn and endFadeOut as hh:mm";
        }
        while (endFadeIn <= startFadeIn) {
            endFadeIn += SECONDS_PER_DAY;
        }
        while (endFadeOut <= endFadeIn) {
            endFadeOut += SECONDS_PER_DAY;
        }
        if (endFadeOut - startFadeIn >= SECONDS_PER_DAY) {
            return "fade times must fall within a day";
        }
        // f(start of fade in) = 0, f(end of fade in) = 1, f(end of fade out) = 0, by Cramer's rule
        final double s0 = angle(startFadeIn, SECONDS_PER_DAY, TIME_OFFSET);
        final double s1 = angle(endFadeIn, SECONDS_PER_DAY, TIME_OFFSET);
        final double e1 = angle(endFadeOut, SECONDS_PER_DAY, TIME_OFFSET);
        final double det = Math.cos(s0) * Math.sin(s1) + Math.cos(e1) * Math.sin(s0) + Math.cos(s1) * Math.sin(e1)
                - Math.cos(s0) * Math.sin(e1) - Math.cos(s1) * Math.sin(s0) - Math.cos(e1) * Math.sin(s1);
        if (det == 0.0) {
            return "fade times give no curve";
        }
        this.a = (Math.sin(e1) - Math.sin(s0)) / det;
        this.b = (Math.cos(s0) - Math.cos(e1)) / det;
        this.c = (Math.cos(e1) * Math.sin(s0) - Math.cos(s0) * Math.sin(e1)) / det;
        return null;
    }

    /** How bright the layer is now, 0 to 1 */
    float brightness(World world, double worldTime, float rainFade) {
        float brightness;
        if (this.weather == null) {
            brightness = rainFade;
        } else {
            final int now = world.isThundering() ? 2 : world.isRaining() ? 1 : 0;
            brightness = this.weather[now] ? 1.0F : 0.0F;
        }
        if (this.fade) {
            final double x = angle(worldTime, TICKS_PER_DAY, 0.0);
            brightness *= (float) (this.a * Math.cos(x) + this.b * Math.sin(x) + this.c);
        }
        return Math.min(1.0F, Math.max(0.0F, brightness));
    }

    void render(float celestialAngle, float brightness) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(this.texture);
        this.blend.apply(brightness);
        GlStateManager.pushMatrix();
        if (this.rotate) {
            GlStateManager.rotate(celestialAngle * 360.0F * this.speed, this.axis[0], this.axis[1], this.axis[2]);
        }
        // north
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
        drawTile(4);
        // top
        GlStateManager.pushMatrix();
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        drawTile(1);
        GlStateManager.popMatrix();
        // bottom
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
        drawTile(0);
        GlStateManager.popMatrix();
        // west, south, east
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        drawTile(5);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        drawTile(2);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        drawTile(3);
        GlStateManager.popMatrix();
    }

    /** One face of the box from its third and half of the image */
    private static void drawTile(int tile) {
        final double u = (tile % 3) / 3.0;
        final double v = (tile / 3) / 2.0;
        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-DISTANCE, -DISTANCE, -DISTANCE).tex(u, v).endVertex();
        buffer.pos(-DISTANCE, -DISTANCE, DISTANCE).tex(u, v + 0.5).endVertex();
        buffer.pos(DISTANCE, -DISTANCE, DISTANCE).tex(u + 1.0 / 3.0, v + 0.5).endVertex();
        buffer.pos(DISTANCE, -DISTANCE, -DISTANCE).tex(u + 1.0 / 3.0, v).endVertex();
        tessellator.draw();
    }

    private static double angle(double time, int period, double offset) {
        return 2.0 * Math.PI * (time / period + offset);
    }

    /** hh:mm[:ss] as seconds of the day, -1 when it is not a time */
    private static int parseTime(String value) {
        final String[] parts = value.split(":");
        if (parts.length < 2) {
            return -1;
        }
        try {
            final int hours = Integer.parseInt(parts[0].trim());
            final int minutes = Integer.parseInt(parts[1].trim());
            final int seconds = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 0;
            return (3600 * hours + 60 * minutes + seconds) % SECONDS_PER_DAY;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static float[] parseAxis(String value) {
        final String[] parts = value.trim().split("\\s+");
        if (parts.length == 3) {
            try {
                final float x = Float.parseFloat(parts[0]);
                final float y = Float.parseFloat(parts[1]);
                final float z = Float.parseFloat(parts[2]);
                if (x * x + y * y + z * z > 0.0F) {
                    return new float[]{x, y, z};
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return new float[]{0.0F, 0.0F, 1.0F};
    }

    private static boolean[] parseWeather(String value) {
        if (value.isEmpty()) {
            return null;
        }
        final boolean[] weather = new boolean[3];
        for (String token : value.toLowerCase(Locale.ROOT).split("\\s+")) {
            switch (token) {
                case "clear" -> weather[0] = true;
                case "rain" -> weather[1] = true;
                case "thunder" -> weather[2] = true;
                default -> {
                }
            }
        }
        return weather;
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String get(Properties properties, String key, String fallback) {
        final String value = properties.getProperty(key);
        return value == null ? fallback : value.trim();
    }
}
