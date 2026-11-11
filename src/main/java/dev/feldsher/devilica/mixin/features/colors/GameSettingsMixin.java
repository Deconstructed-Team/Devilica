/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A pack's cloud kind ({@code clouds=fast|fancy|none}) while clouds are on. */
@Mixin(GameSettings.class)
public abstract class GameSettingsMixin {

    @ModifyReturnValue(method = "shouldRenderClouds", at = @At("RETURN"))
    private int devilica$clouds(int clouds) {
        return CustomColors.clouds(clouds);
    }
}
