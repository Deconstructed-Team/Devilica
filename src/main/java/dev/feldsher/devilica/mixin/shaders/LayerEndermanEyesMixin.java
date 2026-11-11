/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.minecraft.client.renderer.entity.layers.LayerEndermanEyes;
import net.minecraft.entity.monster.EntityEnderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Glowing eyes draw with the pack's entity eyes program. */
@Mixin(LayerEndermanEyes.class)
public abstract class LayerEndermanEyesMixin {

    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/monster/EntityEnderman;FFFFFFF)V", at = @At("HEAD"))
    private void devilica$beginEyes(EntityEnderman entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        ShaderHooks.beginCondition(SpecialCondition.ENTITY_EYES);
    }

    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/monster/EntityEnderman;FFFFFFF)V", at = @At("RETURN"))
    private void devilica$endEyes(EntityEnderman entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        ShaderHooks.endCondition();
    }
}
