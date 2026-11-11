/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Text colours a pack maps to others ({@code text.<from>=<to>}). */
@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {

    @ModifyVariable(method = "renderString", at = @At("HEAD"), argsOnly = true)
    private int devilica$textColor(int color) {
        return CustomColors.text(color);
    }
}
