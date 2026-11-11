/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.rendering.celeritas.api.IrisShaderProviderHolder;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.minecraft.client.renderer.BlockFluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Vanilla fluids carry their face shading in the vertex colour (sides 0.8 and 0.6, bottom 0.5). A shader pack that
 * shades faces itself ({@code oldLighting=false}) gets them unshaded, like blocks in the fast renderer.
 */
@Mixin(BlockFluidRenderer.class)
public abstract class BlockFluidRendererMixin {

    @Unique
    private static float devilica$shade(float vanilla) {
        return IrisShaderProviderHolder.isActive() && BlockRenderingSettings.INSTANCE.shouldDisableDirectionalShading() ? 1.0F : vanilla;
    }

    @ModifyConstant(method = "renderFluid", constant = {@Constant(floatValue = 0.8F), @Constant(floatValue = 0.6F)})
    private float devilica$sideShade(float vanilla) {
        return devilica$shade(vanilla);
    }

    @ModifyConstant(method = "renderFluid", constant = @Constant(floatValue = 0.5F), slice = @Slice(
        from = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;down()Lnet/minecraft/util/math/BlockPos;"),
        to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getInterpolatedU(D)F", ordinal = 6)))
    private float devilica$bottomShade(float vanilla) {
        return devilica$shade(vanilla);
    }
}
