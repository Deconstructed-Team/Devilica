/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** A player's cape is an item of its own to shader packs ({@code player_cape}). */
@Mixin(LayerCape.class)
public abstract class LayerCapeMixin {

    @Unique
    private static final NamespacedId devilica$CAPE = new NamespacedId("minecraft", "player_cape");

    @WrapOperation(method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPlayer;renderCape(F)V"))
    private void devilica$capeId(ModelPlayer model, float scale, Operation<Void> original) {
        ShaderHooks.setSpecialItemId(devilica$CAPE);
        original.call(model, scale);
        ShaderHooks.resetItemId();
    }
}
