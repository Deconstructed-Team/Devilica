/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.hud;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.gui.toasts.AdvancementToast;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.RecipeToast;
import net.minecraft.client.gui.toasts.SystemToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pop-ups the HUD settings switch off, all of them or by kind. */
@Mixin(GuiToast.class)
public abstract class GuiToastFilterMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void devilica$filter(IToast toast, CallbackInfo ci) {
        final DevilicaOptions.HudSettings hud = DevilicaOptions.get().hud;
        if (!hud.toasts
                || !hud.advancementToasts && toast instanceof AdvancementToast
                || !hud.recipeToasts && toast instanceof RecipeToast
                || !hud.systemToasts && toast instanceof SystemToast) {
            ci.cancel();
        }
    }
}
