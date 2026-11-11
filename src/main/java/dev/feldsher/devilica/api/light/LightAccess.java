/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.light;

import net.minecraft.util.math.BlockPos;

/**
 * Dynamic lights: light that moves with entities (a held torch, a dropped glowstone, a burning mob). The terrain and
 * the client world's {@code getCombinedLight} already include it; this is for mods lighting their own geometry.
 */
public interface LightAccess {

    /** Whether dynamic lights are on: switched on, and not off because of a shader pack */
    boolean isEnabled();

    /** The dynamic light level at a point, 0 to 15 with fractions; 0 when off */
    double lightAt(double x, double y, double z);

    /** Sky and block light packed as {@code getCombinedLight} returns them, with the dynamic light at the block added */
    int withDynamicLight(BlockPos pos, int packedLight);

    /** Calls back with every light whose light can reach into the box, as of this frame */
    void forEachLight(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, LightConsumer consumer);

    /** How far a light reaches, in blocks */
    double maxRadius();

    @FunctionalInterface
    interface LightConsumer {
        void accept(double x, double y, double z, int luminance);
    }
}
