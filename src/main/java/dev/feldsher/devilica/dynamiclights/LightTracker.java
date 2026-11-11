/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.dynamiclights;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/** What a light source last lit: where it was, how bright, and which chunk sections hold that light. */
public final class LightTracker {

    /** Light level now, refreshed every tick */
    int luminance;

    /** Position and level the terrain was last lit with */
    double litX = Double.NaN;
    double litY;
    double litZ;
    int litLuminance;

    LongOpenHashSet litSections = new LongOpenHashSet();
    LongOpenHashSet spareSections = new LongOpenHashSet();

    /** A dropped or framed item's light, looked up again every few ticks */
    int itemLuminance;
    boolean itemGoesOut;
    int itemRecheckTick;

    public int getLuminance() {
        return this.luminance;
    }
}
