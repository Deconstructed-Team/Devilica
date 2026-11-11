/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.leaves.LeavesMode;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The leaves setting over the graphics one, for every kind of leaves, each time the renderers are loaded. */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalLeavesMixin {

    @Inject(method = "loadRenderers", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockLeaves;setGraphicsLevel(Z)V", ordinal = 1, shift = At.Shift.AFTER))
    private void devilica$leaves(CallbackInfo ci) {
        LeavesMode.apply();
    }
}
