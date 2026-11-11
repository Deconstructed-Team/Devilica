/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.randomentities.RandomEntities;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While an entity is drawn, the textures it binds can swap for its random look. */
@Mixin(RenderManager.class)
public abstract class RenderManagerRandomMixin {

    @Inject(method = "renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V", at = @At("HEAD"))
    private void devilica$beginRandom(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, CallbackInfo ci) {
        RandomEntities.beginEntity(entity);
    }

    @Inject(method = "renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V", at = @At("RETURN"))
    private void devilica$endRandom(Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, CallbackInfo ci) {
        RandomEntities.end();
    }

    /** Once a frame, before the world's entities: a renderer that threw past its end leaves nothing behind */
    @Inject(method = "cacheActiveRenderInfo", at = @At("HEAD"))
    private void devilica$newFrame(CallbackInfo ci) {
        RandomEntities.resetFrame();
    }
}
