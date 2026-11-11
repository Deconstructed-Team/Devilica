/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.client.RenderLimits;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sign text not drawn when seen from behind (signs the batcher leaves to their renderer). */
@Mixin(TileEntitySignRenderer.class)
public abstract class TileEntitySignRendererLimitsMixin {

    @WrapOperation(method = "render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private int devilica$text(FontRenderer font, String text, int x, int y, int color, Operation<Integer> original,
                              TileEntitySign sign, double relX, double relY, double relZ, float partialTicks, int destroyStage, float alpha) {
        return RenderLimits.signTextHidden(sign, relX, relY, relZ) ? 0 : original.call(font, text, x, y, color);
    }
}
