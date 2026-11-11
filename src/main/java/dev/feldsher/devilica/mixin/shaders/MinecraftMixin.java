/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** When the game has finished loading, Iris prepares the overworld pipeline so joining a world is faster. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void devilica$irisLoadingComplete(CallbackInfo ci) {
        Iris.onLoadingComplete();
    }
}
