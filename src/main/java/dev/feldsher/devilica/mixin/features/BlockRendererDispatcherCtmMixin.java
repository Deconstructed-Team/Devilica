/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.ctm.ConnectedTextures;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A block's model goes to whichever renderer draws it with connected textures applied. Applied after Celeritas's
 * wrap of the same call (higher priority), so its fast renderer gets the wrapped model too.
 */
@Mixin(value = BlockRendererDispatcher.class, priority = 1100)
public abstract class BlockRendererDispatcherCtmMixin {

    @WrapOperation(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockModelRenderer;renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Z)Z"))
    private boolean devilica$connectedTextures(BlockModelRenderer renderer, IBlockAccess world, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, Operation<Boolean> original) {
        return original.call(renderer, world, ConnectedTextures.wrap(model, world, state, pos), state, pos, buffer, checkSides);
    }
}
