/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The block entity ID shader packs see while a block entity draws: the block ID from block.properties. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class TileEntityRendererDispatcherMixin {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", at = @At("HEAD"))
    private void devilica$blockEntityId(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(ShaderHooks.blockEntityId(tile));
    }

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", at = @At("RETURN"))
    private void devilica$resetBlockEntityId(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(0);
    }
}
