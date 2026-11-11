/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.client.RenderLimits;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItemFrame;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item frames' names, their items past a distance, and maps seen from behind, as the render settings say. */
@Mixin(RenderItemFrame.class)
public abstract class RenderItemFrameLimitsMixin {

    @Inject(method = "renderName(Lnet/minecraft/entity/item/EntityItemFrame;DDD)V", at = @At("HEAD"), cancellable = true)
    private void devilica$name(EntityItemFrame frame, double x, double y, double z, CallbackInfo ci) {
        if (!DevilicaOptions.get().render.itemFrameNames) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void devilica$item(EntityItemFrame frame, CallbackInfo ci) {
        final RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        if (RenderLimits.frameItemHidden(frame, manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ)
                || frame.getDisplayedItem().getItem() instanceof ItemMap
                && RenderLimits.mapHidden(frame, manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ)) {
            ci.cancel();
        }
    }
}
