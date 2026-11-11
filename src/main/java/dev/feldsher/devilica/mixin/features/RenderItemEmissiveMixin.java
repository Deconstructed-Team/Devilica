/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.ctm.ConnectedTextures;
import dev.feldsher.devilica.ctm.EmissiveTextures;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Blocks drawn as items take their connected textures tile for no neighbours, and items draw their glowing copies. */
@Mixin(RenderItem.class)
public abstract class RenderItemEmissiveMixin {

    @ModifyVariable(method = "renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;ILnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), argsOnly = true)
    private IBakedModel devilica$itemModel(IBakedModel model, IBakedModel original, int color, ItemStack stack) {
        return EmissiveTextures.withItemGlow(ConnectedTextures.withHeldTiles(model, stack));
    }
}
