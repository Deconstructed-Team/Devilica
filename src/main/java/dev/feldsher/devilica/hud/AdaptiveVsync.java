/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.hud;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLVideo;

/**
 * Adaptive vsync: SDL's swap interval -1, where a frame that misses the refresh shows at once instead of waiting a whole
 * one. The game's window layer only knows on and off, so this follows it each time the game sets vsync.
 */
public final class AdaptiveVsync {

    private AdaptiveVsync() {
    }

    public static void apply() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings == null || !mc.gameSettings.enableVsync) {
            return;
        }
        final boolean adaptive = DevilicaOptions.get().hud.adaptiveVsync;
        // Not every driver has late swap tearing; plain vsync stays then
        if (!adaptive || !SDLVideo.SDL_GL_SetSwapInterval(-1)) {
            SDLVideo.SDL_GL_SetSwapInterval(1);
        }
    }
}
