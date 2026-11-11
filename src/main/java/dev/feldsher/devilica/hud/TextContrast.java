/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.hud;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

import java.util.Locale;

/** What keeps the overlay readable over a bright world. */
public enum TextContrast implements TextProvider {
    NONE,
    BACKGROUND,
    SHADOW;

    @Override
    public TextComponent getLocalizedName() {
        return TextComponent.translatable("devilica.options.text_contrast." + this.name().toLowerCase(Locale.ROOT));
    }
}
