/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.colors;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.awt.image.BufferedImage;

/**
 * A resource pack's lightmap ({@code lightmap/world<dimension>.png}, MCPatcher's and OptiFine's format): 16 rows for
 * sky light and 16 for block light, read across by the sun's brightness and the torches' flicker. 32 more rows, when
 * present, are the same under night vision.
 */
final class Lightmap {

    private static final int LEVELS = 16;

    private final int[] pixels;
    private final int width;
    private final boolean nightVisionRows;

    private Lightmap(int[] pixels, int width, boolean nightVisionRows) {
        this.pixels = pixels;
        this.width = width;
        this.nightVisionRows = nightVisionRows;
    }

    /** The lightmap of an image, or null when its height is neither 32 nor 64 */
    static Lightmap of(BufferedImage image) {
        final int height = image.getHeight();
        if (height != 2 * LEVELS && height != 4 * LEVELS) {
            return null;
        }
        final int width = image.getWidth();
        return new Lightmap(image.getRGB(0, 0, width, height, null, 0, width), width, height == 4 * LEVELS);
    }

    /** Fills the 256 lightmap colours, sky light by sixteens and block light within */
    void compute(World world, int[] colors, float torchFlicker, float nightVision, float gamma) {
        final float sun = clamp(world.getLastLightningBolt() > 0 ? 1.0F : 7.0F / 6.0F * (world.getSunBrightness(1.0F) - 0.2F)) * (this.width - 1);
        final float torch = clamp(torchFlicker + 0.5F) * (this.width - 1);
        final float[] sky = new float[3 * LEVELS];
        final float[] block = new float[3 * LEVELS];
        final float[] skyVision = new float[3 * LEVELS];
        final float[] blockVision = new float[3 * LEVELS];
        final boolean customVision = this.nightVisionRows && nightVision > 0.0F;
        for (int level = 0; level < LEVELS; level++) {
            sample(level, sun, sky, 3 * level);
            sample(level + LEVELS, torch, block, 3 * level);
            if (customVision) {
                sample(level + 2 * LEVELS, sun, skyVision, 3 * level);
                sample(level + 3 * LEVELS, torch, blockVision, 3 * level);
            }
        }
        final float[] rgb = new float[3];
        for (int s = 0; s < LEVELS; s++) {
            for (int b = 0; b < LEVELS; b++) {
                for (int k = 0; k < 3; k++) {
                    rgb[k] = clamp(sky[3 * s + k] + block[3 * b + k]);
                }
                if (nightVision > 0.0F) {
                    if (customVision) {
                        for (int k = 0; k < 3; k++) {
                            rgb[k] = clamp((1.0F - nightVision) * rgb[k] + nightVision * (skyVision[3 * s + k] + blockVision[3 * b + k]));
                        }
                    } else {
                        final float brightest = Math.max(Math.max(rgb[0], rgb[1]), rgb[2]);
                        if (brightest > 0.0F) {
                            final float scale = 1.0F - nightVision + nightVision / brightest;
                            for (int k = 0; k < 3; k++) {
                                rgb[k] = clamp(rgb[k] * scale);
                            }
                        }
                    }
                }
                if (gamma != 0.0F) {
                    for (int k = 0; k < 3; k++) {
                        final float dark = 1.0F - rgb[k];
                        rgb[k] = gamma * (1.0F - dark * dark * dark * dark) + (1.0F - gamma) * rgb[k];
                    }
                }
                colors[s * LEVELS + b] = 0xFF000000 | (int) (rgb[0] * 255.0F) << 16 | (int) (rgb[1] * 255.0F) << 8 | (int) (rgb[2] * 255.0F);
            }
        }
    }

    /** The row's colour at a fractional column, as three floats */
    private void sample(int row, float column, float[] out, int offset) {
        final int left = (int) column;
        final int right = Math.min(left + 1, this.width - 1);
        final float weight = column - left;
        final int a = this.pixels[row * this.width + left];
        final int b = this.pixels[row * this.width + right];
        for (int k = 0; k < 3; k++) {
            final int shift = 16 - 8 * k;
            out[offset + k] = ((a >> shift & 0xFF) * (1.0F - weight) + (b >> shift & 0xFF) * weight) / 255.0F;
        }
    }

    private static float clamp(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }
}
