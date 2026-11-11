/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.compat.mojang.InteractionHand;
import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.pipeline.HandRenderer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Held items carry their item ID, in first and third person. With a shader pack, Iris draws the first-person hands in
 * two passes, solid and translucent; each hand is drawn in the pass its item belongs to.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V", at = @At("HEAD"), cancellable = true)
    private void devilica$handPass(AbstractClientPlayer player, float partialTicks, float pitch, EnumHand hand, float swingProgress, ItemStack stack, float equipProgress, CallbackInfo ci) {
        if (IrisApi.getInstance().isShaderPackInUse()) {
            final boolean translucent = HandRenderer.INSTANCE.isHandTranslucent(hand == EnumHand.MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            if (HandRenderer.INSTANCE.isRenderingSolid() == translucent) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderWaterOverlayTexture", at = @At("HEAD"), cancellable = true)
    private void devilica$underwaterOverlay(float partialTicks, CallbackInfo ci) {
        if (ShaderHooks.hidesUnderwaterOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemSide", at = @At("HEAD"))
    private void devilica$setItemId(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType transform, boolean leftHanded, CallbackInfo ci) {
        ShaderHooks.setItemId(stack);
    }

    @Inject(method = "renderItemSide", at = @At("RETURN"))
    private void devilica$resetItemId(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType transform, boolean leftHanded, CallbackInfo ci) {
        ShaderHooks.resetItemId();
    }
}
