/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The end sky's tint from the packs ({@code sky.end}). */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin {

    @WrapOperation(method = "renderSkyEnd", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;color(IIII)Lnet/minecraft/client/renderer/BufferBuilder;"))
    private BufferBuilder devilica$endSky(BufferBuilder buffer, int red, int green, int blue, int alpha, Operation<BufferBuilder> original) {
        final int color = CustomColors.endSky();
        if (color < 0) {
            return original.call(buffer, red, green, blue, alpha);
        }
        return original.call(buffer, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
    }
}
