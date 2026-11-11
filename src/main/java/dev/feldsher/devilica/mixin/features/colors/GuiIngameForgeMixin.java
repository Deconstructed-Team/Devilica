/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The experience level's colour from the packs ({@code text.xpbar}) on Forge's HUD. */
@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class GuiIngameForgeMixin {

    @ModifyConstant(method = "renderExperience", constant = @Constant(intValue = 0x80FF20))
    private int devilica$xpBar(int color) {
        return CustomColors.xpBar(color);
    }
}
