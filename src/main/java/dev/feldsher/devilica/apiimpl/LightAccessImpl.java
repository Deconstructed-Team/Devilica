/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.apiimpl;

import dev.feldsher.devilica.api.light.LightAccess;
import dev.feldsher.devilica.dynamiclights.DynamicLights;
import net.minecraft.util.math.BlockPos;

/** {@link LightAccess} over the dynamic lights of the frame. */
public final class LightAccessImpl implements LightAccess {

    @Override
    public boolean isEnabled() {
        return DynamicLights.isEnabled();
    }

    @Override
    public double lightAt(double x, double y, double z) {
        return DynamicLights.lightAt(DynamicLights.current(), x, y, z);
    }

    @Override
    public int withDynamicLight(BlockPos pos, int packedLight) {
        return DynamicLights.withDynamicLight(DynamicLights.current(), pos.getX(), pos.getY(), pos.getZ(), packedLight);
    }

    @Override
    public void forEachLight(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, LightConsumer consumer) {
        final float[] lights = DynamicLights.current();
        final double reach = DynamicLights.MAX_RADIUS;
        for (int i = 0; i < lights.length; i += 4) {
            final double x = lights[i];
            final double y = lights[i + 1];
            final double z = lights[i + 2];
            if (x >= minX - reach && x <= maxX + reach && y >= minY - reach && y <= maxY + reach && z >= minZ - reach && z <= maxZ + reach) {
                consumer.accept(x, y, z, (int) lights[i + 3]);
            }
        }
    }

    @Override
    public double maxRadius() {
        return DynamicLights.MAX_RADIUS;
    }
}
