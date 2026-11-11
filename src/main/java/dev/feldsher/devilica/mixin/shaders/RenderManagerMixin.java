/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.EntityIdHelper;
import net.coderbot.iris.uniforms.ItemIdManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The entity ID shader packs see while an entity draws, in the main and the shadow pass alike. */
@Mixin(RenderManager.class)
public abstract class RenderManagerMixin {

    @WrapOperation(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V"))
    private void devilica$entityId(Render<Entity> render, Entity entity, double x, double y, double z, float yaw, float partialTicks, Operation<Void> original) {
        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
        ItemIdManager.resetItemId();
        try {
            original.call(render, entity, x, y, z, yaw, partialTicks);
        } finally {
            CapturedRenderingState.INSTANCE.setCurrentEntity(-1);
        }
    }
}
