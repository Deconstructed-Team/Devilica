/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.minecraft.client.renderer.entity.RenderLightningBolt;
import net.minecraft.entity.effect.EntityLightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A lightning bolt draws with the pack's lightning program. */
@Mixin(RenderLightningBolt.class)
public abstract class RenderLightningBoltMixin {

    @Inject(method = "doRender(Lnet/minecraft/entity/effect/EntityLightningBolt;DDDFF)V", at = @At("HEAD"))
    private void devilica$beginBolt(EntityLightningBolt entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        ShaderHooks.beginCondition(SpecialCondition.LIGHTNING);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/effect/EntityLightningBolt;DDDFF)V", at = @At("RETURN"))
    private void devilica$endBolt(EntityLightningBolt entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        ShaderHooks.endCondition();
    }
}
