/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import net.coderbot.iris.Iris;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iris's early setup (shader pack directory, its config) runs right before the game's options load, as in Angelica. */
@Mixin(GameSettings.class)
public abstract class GameSettingsMixin {

    @Unique
    private static boolean devilica$irisInitialized;

    @Inject(method = "loadOptions", at = @At("HEAD"))
    private void devilica$initializeIris(CallbackInfo ci) {
        if (!devilica$irisInitialized) {
            devilica$irisInitialized = true;
            Iris.INSTANCE.onEarlyInitialize();
        }
    }
}
