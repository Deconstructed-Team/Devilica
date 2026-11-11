/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.compat.LumenizedCompat;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lumenized draws its bloom layer right after vanilla's translucent layer and then the translucent layer again, on every
 * path (its bloom, its shader pack one, and a skip of an unseen bloom). So the translucent layer went down twice a
 * frame: water and glass blended twice, and under a shader pack the hand and the translucent phase ran twice as well.
 * With Lumenized in the game, vanilla's call is left to it.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererLumenizedMixin {

    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I"))
    private int devilica$translucentOnce(RenderGlobal renderGlobal, BlockRenderLayer layer, double partialTicks, int pass, Entity entity,
                                         Operation<Integer> original) {
        if (layer == BlockRenderLayer.TRANSLUCENT && LumenizedCompat.LOADED) {
            return 0;
        }
        return original.call(renderGlobal, layer, partialTicks, pass, entity);
    }
}
