/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.lights;

import dev.feldsher.devilica.dynamiclights.DynamicLights;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A new client world (or none) starts without the old world's lights. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void devilica$clearDynamicLights(WorldClient world, String loadingMessage, CallbackInfo ci) {
        DynamicLights.clear();
    }
}
