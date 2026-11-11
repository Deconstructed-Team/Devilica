/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.light;

import net.minecraft.item.ItemStack;

/**
 * An item that gives off light while held, worn or dropped, when dynamic lights are on. Items of blocks give off their
 * block's light without this; other items give off none unless the settings list them.
 */
public interface DynamicLightItem {

    /** @return the light level, 0 to 15 */
    int getLuminance(ItemStack stack);

    /** @return whether the light goes out under water, as a torch's does; since API 1002 */
    default boolean goesOutInWater(ItemStack stack) {
        return false;
    }
}
