/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A dropped item carries its item ID. */
@Mixin(RenderEntityItem.class)
public abstract class RenderEntityItemMixin {

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("HEAD"))
    private void devilica$setItemId(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        ShaderHooks.setItemId(entity.getItem());
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("RETURN"))
    private void devilica$resetItemId(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        ShaderHooks.resetItemId();
    }
}
