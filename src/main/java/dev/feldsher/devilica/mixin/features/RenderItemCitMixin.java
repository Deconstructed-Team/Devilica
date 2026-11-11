/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.cit.CustomGlints;
import dev.feldsher.devilica.cit.CustomItemTextures;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An item is drawn with a resource pack's texture for it, once the hand or GUI placement is set up; a pack's json
 * model takes the item's place before that, so its own placement applies.
 */
@Mixin(RenderItem.class)
public abstract class RenderItemCitMixin {

    @Inject(method = "getItemModelWithOverrides", at = @At("RETURN"), cancellable = true)
    private void devilica$jsonModel(ItemStack stack, World world, EntityLivingBase entity, CallbackInfoReturnable<IBakedModel> cir) {
        final IBakedModel model = CustomItemTextures.jsonModel(stack);
        if (model != null) {
            cir.setReturnValue(model);
        }
    }

    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V", at = @At("HEAD"), argsOnly = true)
    private IBakedModel devilica$customTexture(IBakedModel model, ItemStack stack) {
        return CustomItemTextures.model(stack, model);
    }

    @WrapOperation(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderItem;renderEffect(Lnet/minecraft/client/renderer/block/model/IBakedModel;)V"))
    private void devilica$customGlint(RenderItem self, IBakedModel model, Operation<Void> original, ItemStack stack, IBakedModel unused) {
        if (!CustomGlints.renderItem(self, model, stack)) {
            original.call(self, model);
        }
    }
}
