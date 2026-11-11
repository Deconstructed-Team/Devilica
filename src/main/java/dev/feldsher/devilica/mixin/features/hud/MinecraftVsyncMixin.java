/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.hud;

import dev.feldsher.devilica.hud.AdaptiveVsync;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adaptive vsync after the game sets vsync at start and on a fullscreen switch. */
@Mixin(Minecraft.class)
public abstract class MinecraftVsyncMixin {

    @Inject(method = {"init", "toggleFullscreen"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setVSyncEnabled(Z)V", shift = At.Shift.AFTER, remap = false))
    private void devilica$adaptiveVsync(CallbackInfo ci) {
        AdaptiveVsync.apply();
    }
}
