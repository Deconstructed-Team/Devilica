/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.colors;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;

import java.awt.image.BufferedImage;

/**
 * A resource pack's colour map: by a biome's temperature and rainfall like vanilla's grass ({@code vanilla}), by biome
 * and height ({@code grid}), or one colour ({@code fixed}). A one-row image is a list read by index (redstone power,
 * stem age).
 */
final class Colormap {

    enum Format {
        VANILLA, GRID, FIXED
    }

    private static final int PLAINS = 1;

    private final Format format;
    private final int[] pixels;
    private final int width;
    private final int height;
    private final int yVariance;
    private final int yOffset;
    /** The colour where no world is at hand (items), and the colour of a fixed map */
    private final int fallback;

    private Colormap(Format format, int[] pixels, int width, int height, int yVariance, int yOffset, int fixed) {
        this.format = format;
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.yVariance = Math.max(0, yVariance);
        this.yOffset = yOffset;
        this.fallback = switch (format) {
            case FIXED -> fixed;
            case VANILLA -> pixel((int) (0.5F * (width - 1)), (int) (0.5F * (height - 1)));
            case GRID -> pixel(Math.min(PLAINS, width - 1), MathHelper.clamp(64 - yOffset, 0, height - 1));
        };
    }

    static Colormap fixed(int color) {
        return new Colormap(Format.FIXED, null, 0, 0, 0, 0, color & 0xFFFFFF);
    }

    static Colormap of(BufferedImage image, Format format, int yVariance, int yOffset) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        return new Colormap(format, pixels, width, height, yVariance, yOffset, 0);
    }

    boolean isFixed() {
        return this.format == Format.FIXED;
    }

    int fallback() {
        return this.fallback;
    }

    /** How many entries a list map has */
    int size() {
        return this.pixels == null ? 1 : this.pixels.length;
    }

    /** The colour of the list entry, for maps such as redstone's (by power) and stems' (by age) */
    int entry(int index) {
        if (this.pixels == null) {
            return this.fallback;
        }
        return this.pixels[MathHelper.clamp(index, 0, this.pixels.length - 1)] & 0xFFFFFF;
    }

    /** The colour at a position, blended over the 3 by 3 columns around it as vanilla blends grass */
    int color(IBlockAccess world, BlockPos pos) {
        if (this.format == Format.FIXED) {
            return this.fallback;
        }
        return blended(world, pos, 1);
    }

    int blended(IBlockAccess world, BlockPos center, int radius) {
        int red = 0;
        int green = 0;
        int blue = 0;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                pos.setPos(center.getX() + dx, center.getY(), center.getZ() + dz);
                final int color = colorAt(world.getBiome(pos), pos);
                red += color >> 16 & 0xFF;
                green += color >> 8 & 0xFF;
                blue += color & 0xFF;
            }
        }
        final int count = (2 * radius + 1) * (2 * radius + 1);
        return (red / count) << 16 | (green / count) << 8 | blue / count;
    }

    int colorAt(Biome biome, BlockPos pos) {
        return colorAt(biome, this.format == Format.VANILLA ? biome.getTemperature(pos) : 0.0F, pos.getX(), pos.getY(), pos.getZ());
    }

    /** One biome's colour; the temperature is the biome's at that height */
    int colorAt(Biome biome, float temperature, int x, int y, int z) {
        return switch (this.format) {
            case FIXED -> this.fallback;
            case VANILLA -> {
                final float warmth = MathHelper.clamp(temperature, 0.0F, 1.0F);
                final float wetness = MathHelper.clamp(biome.getRainfall(), 0.0F, 1.0F) * warmth;
                yield pixel((int) ((1.0F - warmth) * (this.width - 1)), (int) ((1.0F - wetness) * (this.height - 1)));
            }
            case GRID -> {
                int row = y - this.yOffset;
                if (this.yVariance > 0) {
                    row += Math.floorMod(MathHelper.getCoordinateRandom(x, 0, z) >> 16, 2 * this.yVariance + 1) - this.yVariance;
                }
                final int column = MathHelper.clamp(Biome.getIdForBiome(biome), 0, this.width - 1);
                yield pixel(column, MathHelper.clamp(row, 0, this.height - 1));
            }
        };
    }

    private int pixel(int x, int y) {
        return this.pixels[y * this.width + x] & 0xFFFFFF;
    }
}
