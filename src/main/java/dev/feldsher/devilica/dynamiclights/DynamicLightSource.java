/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.dynamiclights;

import org.jetbrains.annotations.Nullable;

/** An entity as a moving light: every entity is one, most give off no light and never get a tracker. */
public interface DynamicLightSource {

    @Nullable
    LightTracker devilica$getLightTracker();

    LightTracker devilica$getOrCreateLightTracker();
}
