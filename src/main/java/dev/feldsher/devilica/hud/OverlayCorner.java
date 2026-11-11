/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.hud;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

import java.util.Locale;

/** Where the overlay sits on the screen. */
public enum OverlayCorner implements TextProvider {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    @Override
    public TextComponent getLocalizedName() {
        return TextComponent.translatable("devilica.options.overlay_corner." + this.name().toLowerCase(Locale.ROOT));
    }
}
