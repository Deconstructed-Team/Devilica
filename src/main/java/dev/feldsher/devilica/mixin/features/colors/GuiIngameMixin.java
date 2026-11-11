/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The experience level's colour from the packs ({@code text.xpbar}). */
@Mixin(GuiIngame.class)
public abstract class GuiIngameMixin {

    @ModifyConstant(method = "renderExpBar", constant = @Constant(intValue = 0x80FF20))
    private int devilica$xpBar(int color) {
        return CustomColors.xpBar(color);
    }
}
