/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.cit.CustomGlints;
import dev.feldsher.devilica.cit.CustomItemTextures;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Worn armour takes a resource pack's layers for that stack (custom item textures of type armor). */
@Mixin(LayerArmorBase.class)
public abstract class LayerArmorBaseCitMixin {

    @ModifyReturnValue(method = "getArmorResource(Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/EntityEquipmentSlot;Ljava/lang/String;)Lnet/minecraft/util/ResourceLocation;", at = @At("RETURN"), remap = false)
    private ResourceLocation devilica$customArmor(ResourceLocation texture, Entity entity, ItemStack stack, EntityEquipmentSlot slot, String type) {
        return CustomItemTextures.armor(stack, texture);
    }

    /** The armour piece's custom enchantment glints in place of vanilla's */
    @WrapOperation(method = "renderArmorLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/LayerArmorBase;renderEnchantedGlint(Lnet/minecraft/client/renderer/entity/RenderLivingBase;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/model/ModelBase;FFFFFFF)V"))
    private void devilica$customGlint(RenderLivingBase<?> renderer, EntityLivingBase entity, ModelBase model, float limbSwing, float limbSwingAmount,
                                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, Operation<Void> original,
                                      EntityLivingBase wearer, float wearerLimbSwing, float wearerLimbSwingAmount, float wearerPartialTicks,
                                      float wearerAgeInTicks, float wearerNetHeadYaw, float wearerHeadPitch, float wearerScale, EntityEquipmentSlot slot) {
        final ItemStack stack = wearer.getItemStackFromSlot(slot);
        if (!CustomGlints.renderArmor(renderer, entity, model, stack, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale)) {
            original.call(renderer, entity, model, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        }
    }
}
