/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.renderer.entity.layers.LayerEnderDragonDeath;
import net.minecraft.entity.boss.EntityDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The dying dragon's beams draw with the pack's lightning program, and are an entity of their own to the shader packs
 * that name it.
 */
@Mixin(LayerEnderDragonDeath.class)
public abstract class LayerEnderDragonDeathMixin {

    @Unique
    private static final NamespacedId devilica$RAYS = new NamespacedId("minecraft", "dragon_death_rays");

    @Unique
    private int devilica$previousEntityId = ShaderHooks.NO_ID;

    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/boss/EntityDragon;FFFFFFF)V", at = @At("HEAD"))
    private void devilica$beginRays(EntityDragon entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (entity.deathTicks > 0) {
            this.devilica$previousEntityId = ShaderHooks.setNamedEntityId(devilica$RAYS);
            ShaderHooks.beginCondition(SpecialCondition.LIGHTNING);
        }
    }

    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/boss/EntityDragon;FFFFFFF)V", at = @At("RETURN"))
    private void devilica$endRays(EntityDragon entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (entity.deathTicks > 0) {
            ShaderHooks.endCondition();
            ShaderHooks.restoreEntityId(this.devilica$previousEntityId);
            this.devilica$previousEntityId = ShaderHooks.NO_ID;
        }
    }
}
