/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.randomentities.RandomEntities;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block entities (chests, beds, banners) get random looks too, kept by their position. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class TileEntityRendererDispatcherRandomMixin {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", at = @At("HEAD"))
    private void devilica$beginRandom(TileEntity tileEntity, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        RandomEntities.beginBlockEntity(tileEntity);
    }

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", at = @At("RETURN"))
    private void devilica$endRandom(TileEntity tileEntity, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        RandomEntities.end();
    }
}
