/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.gui.GuiBossOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The boss name's colour from the packs ({@code text.boss}). */
@Mixin(GuiBossOverlay.class)
public abstract class GuiBossOverlayMixin {

    @ModifyConstant(method = "renderBossHealth", constant = @Constant(intValue = 0xFFFFFF))
    private int devilica$bossName(int color) {
        return CustomColors.bossName(color);
    }
}
