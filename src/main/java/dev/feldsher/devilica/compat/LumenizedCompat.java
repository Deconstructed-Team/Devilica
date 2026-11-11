/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.compat;

import net.minecraftforge.fml.common.Loader;

/** Lumenized (the GregTech CEu bloom, carried over), when it is in the game. */
public final class LumenizedCompat {

    public static final boolean LOADED = Loader.isModLoaded("lumenized");

    private LumenizedCompat() {
    }
}
