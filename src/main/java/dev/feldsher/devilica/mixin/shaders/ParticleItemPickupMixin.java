/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.shaders.ShaderHooks;
import net.minecraft.client.particle.ParticleItemPickup;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** An item flying into the player is drawn by the particle pass, but as the entity it is. */
@Mixin(ParticleItemPickup.class)
public abstract class ParticleItemPickupMixin {

    @WrapOperation(method = "renderParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntity(Lnet/minecraft/entity/Entity;DDDFFZ)V"))
    private void devilica$asEntity(RenderManager renderManager, Entity entity, double x, double y, double z, float yaw, float partialTicks, boolean debug, Operation<Void> original) {
        ShaderHooks.beginEntity(entity);
        try {
            original.call(renderManager, entity, x, y, z, yaw, partialTicks, debug);
        } finally {
            ShaderHooks.endEntity();
        }
    }
}
