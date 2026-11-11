/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Worn armour carries the armour item's ID; its enchantment glint draws with the glint program. */
@Mixin(LayerArmorBase.class)
public abstract class LayerArmorBaseMixin {

    @Inject(method = "renderArmorLayer", at = @At("HEAD"))
    private void devilica$setArmorId(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, EntityEquipmentSlot slot, CallbackInfo ci) {
        ShaderHooks.setItemId(entity.getItemStackFromSlot(slot));
    }

    @Inject(method = "renderArmorLayer", at = @At("RETURN"))
    private void devilica$resetArmorId(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, EntityEquipmentSlot slot, CallbackInfo ci) {
        ShaderHooks.resetItemId();
    }

    @Inject(method = "renderEnchantedGlint", at = @At("HEAD"))
    private static void devilica$beginGlint(RenderLivingBase<?> renderer, EntityLivingBase entity, ModelBase model, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        ShaderHooks.beginGlint();
    }

    @Inject(method = "renderEnchantedGlint", at = @At("RETURN"))
    private static void devilica$endGlint(RenderLivingBase<?> renderer, EntityLivingBase entity, ModelBase model, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        ShaderHooks.endCondition();
    }
}
