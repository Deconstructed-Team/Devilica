/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.hud;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOverlayDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * F3's text worked out once every few ticks instead of every frame. Forge appends its lines to what it gets, so each
 * frame gets a copy.
 */
@Mixin(GuiOverlayDebug.class)
public abstract class GuiOverlayDebugCacheMixin {
    @Unique
    private List<String> devilica$left;
    @Unique
    private int devilica$leftAt;
    @Unique
    private List<String> devilica$right;
    @Unique
    private int devilica$rightAt;

    @Unique
    private static int devilica$tick() {
        return Minecraft.getMinecraft().ingameGUI.getUpdateCounter();
    }

    @Unique
    private static boolean devilica$fresh(List<String> cached, int at) {
        final int every = DevilicaOptions.get().hud.debugRefreshTicks;
        return every > 0 && cached != null && devilica$tick() - at < every;
    }

    @Inject(method = "call", at = @At("HEAD"), cancellable = true)
    private void devilica$cachedLeft(CallbackInfoReturnable<List<String>> cir) {
        if (devilica$fresh(this.devilica$left, this.devilica$leftAt)) {
            cir.setReturnValue(new ArrayList<>(this.devilica$left));
        }
    }

    @Inject(method = "call", at = @At("RETURN"))
    private void devilica$keepLeft(CallbackInfoReturnable<List<String>> cir) {
        this.devilica$left = new ArrayList<>(cir.getReturnValue());
        this.devilica$leftAt = devilica$tick();
    }

    @Inject(method = "getDebugInfoRight", at = @At("HEAD"), cancellable = true)
    private void devilica$cachedRight(CallbackInfoReturnable<List<String>> cir) {
        if (devilica$fresh(this.devilica$right, this.devilica$rightAt)) {
            cir.setReturnValue(new ArrayList<>(this.devilica$right));
        }
    }

    @Inject(method = "getDebugInfoRight", at = @At("RETURN"))
    private void devilica$keepRight(CallbackInfoReturnable<List<String>> cir) {
        this.devilica$right = new ArrayList<>(cir.getReturnValue());
        this.devilica$rightAt = devilica$tick();
    }
}
