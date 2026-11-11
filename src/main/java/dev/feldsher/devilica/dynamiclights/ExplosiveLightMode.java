/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.dynamiclights;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

import java.util.Locale;

/** How a swelling creeper or a lit TNT glows: not at all, at one level, or brighter as it nears the blast. */
public enum ExplosiveLightMode implements TextProvider {
    OFF,
    SIMPLE,
    FANCY;

    @Override
    public TextComponent getLocalizedName() {
        return TextComponent.translatable("devilica.options.explosive_light." + this.name().toLowerCase(Locale.ROOT));
    }
}
