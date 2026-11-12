/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.tesr.BlockEntityBatcher;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block entities with known models go to the batch instead of their renderer, inside a block entity pass. The high
 * priority puts the hook after other mods' own at the head of the method: those that hide a block entity (an
 * occlusion check, a hidden area) still do.
 */
@Mixin(value = TileEntityRendererDispatcher.class, priority = 1500)
public abstract class TileEntityRendererDispatcherBatchMixin {

    // The world's block entities, before the lighting set up for each one
    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"), cancellable = true)
    private void devilica$batchFirst(TileEntity tile, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (BlockEntityBatcher.addFirst((TileEntityRendererDispatcher) (Object) this, tile, destroyStage)) {
            ci.cancel();
        }
    }

    // Callers that set the light themselves (the shadow pass)
    @WrapOperation(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V"))
    private void devilica$batch(TileEntitySpecialRenderer<TileEntity> renderer, TileEntity tile, double x, double y, double z,
                                float partialTicks, int destroyStage, float alpha, Operation<Void> original) {
        if (!BlockEntityBatcher.add(renderer, tile, x, y, z, destroyStage, alpha)) {
            original.call(renderer, tile, x, y, z, partialTicks, destroyStage, alpha);
        }
    }
}
