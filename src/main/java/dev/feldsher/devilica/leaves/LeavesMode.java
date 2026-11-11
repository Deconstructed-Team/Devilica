/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.leaves;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

import java.util.Locale;

/**
 * OptiFine's leaves: as the graphics setting has them, fast (solid, inner faces culled), fancy (see-through), or smart
 * (see-through, with the faces between two leaves of a kind culled, so a tree looks fancy from outside with far fewer
 * faces).
 */
public enum LeavesMode implements TextProvider {
    DEFAULT,
    FAST,
    FANCY,
    SMART;

    public boolean fancy() {
        return switch (this) {
            case DEFAULT -> Minecraft.getMinecraft().gameSettings.fancyGraphics;
            case FAST -> false;
            case FANCY, SMART -> true;
        };
    }

    /** Every kind of leaves made fast or fancy as the setting says, mods' too, when the world's renderers are loaded */
    public static void apply() {
        final boolean fancy = DevilicaOptions.get().leaves.fancy();
        for (Block block : Block.REGISTRY) {
            if (block instanceof BlockLeaves leaves) {
                leaves.setGraphicsLevel(fancy);
            }
        }
    }

    @Override
    public TextComponent getLocalizedName() {
        return TextComponent.translatable("devilica.options.leaves." + this.name().toLowerCase(Locale.ROOT));
    }
}
