/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.entity.RenderXPOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Experience orbs in the colours of {@code xporb.png}, following vanilla's pulse. */
@Mixin(RenderXPOrb.class)
public abstract class RenderXPOrbMixin {

    @WrapOperation(method = "doRender(Lnet/minecraft/entity/item/EntityXPOrb;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;color(IIII)Lnet/minecraft/client/renderer/BufferBuilder;"))
    private BufferBuilder devilica$orbColor(BufferBuilder buffer, int red, int green, int blue, int alpha, Operation<BufferBuilder> original) {
        final int color = CustomColors.xpOrb(red);
        if (color < 0) {
            return original.call(buffer, red, green, blue, alpha);
        }
        return original.call(buffer, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
    }
}
