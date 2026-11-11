/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

import java.util.Locale;

/** OptiFine's better grass: off, every side as the top (fast), or sides that run on into grass below (fancy). */
public enum BetterGrassMode implements TextProvider {
    OFF,
    FAST,
    FANCY;

    @Override
    public TextComponent getLocalizedName() {
        return TextComponent.translatable("devilica.options.better_grass." + this.name().toLowerCase(Locale.ROOT));
    }
}
