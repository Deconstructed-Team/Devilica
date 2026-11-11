/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.dynamiclights;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

import java.util.Locale;

/** How often moving lights relight the terrain around them. */
public enum DynamicLightsMode implements TextProvider {
    OFF(-1),
    FASTEST(500),
    FAST(250),
    FANCY(50),
    REALTIME(0);

    /** Milliseconds between updates */
    public final int delay;

    DynamicLightsMode(int delay) {
        this.delay = delay;
    }

    @Override
    public TextComponent getLocalizedName() {
        return TextComponent.translatable("devilica.options.dynamic_lights." + this.name().toLowerCase(Locale.ROOT));
    }
}
