/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.feldsher.devilica.client.RenderLimits;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Block entities drawn only as far as the render settings say. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class TileEntityRendererDispatcherLimitsMixin {

    @ModifyExpressionValue(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntity;getMaxRenderDistanceSquared()D"))
    private double devilica$distance(double ownSq, TileEntity tile) {
        return RenderLimits.blockEntityDistanceSq(tile, ownSq);
    }
}
