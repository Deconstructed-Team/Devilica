/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.light;

import net.minecraft.world.IBlockAccess;

/**
 * A light engine's own coloured block light, used instead of the colour Devilica works out. Called on chunk builder
 * threads for every block of a section and one block around it, with the build's copy of the world. Since 1003.
 */
@FunctionalInterface
public interface ColoredLightProvider {

    /**
     * The block light at a block as red, green and blue levels 0 to 15, packed {@code r << 8 | g << 4 | b} (as
     * Angelica's {@code BlockLightProvider} packs them), or -1 when there is no light there. Only the hue is used: how
     * bright the block is stays the game's own light.
     */
    int getBlockLightRGB(IBlockAccess world, int x, int y, int z);
}
