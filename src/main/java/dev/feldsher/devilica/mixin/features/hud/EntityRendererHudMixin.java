/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.hud.HudCache;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The HUD through its cache. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererHudMixin {

    @WrapOperation(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V"))
    private void devilica$cachedHud(GuiIngame gui, float partialTicks, Operation<Void> original) {
        HudCache.render(gui, partialTicks, () -> original.call(gui, partialTicks));
    }
}
