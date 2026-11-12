/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the game has finished loading, Iris prepares the overworld pipeline so joining a world is faster. Leaving a
 * world drops whatever a frame that threw midway left in use.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void devilica$irisLoadingComplete(CallbackInfo ci) {
        Iris.onLoadingComplete();
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void devilica$abandonFrame(WorldClient world, String loadingMessage, CallbackInfo ci) {
        if (world == null) {
            ShaderHooks.abandonFrame();
        }
    }
}
