/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Clouds as the details settings say, fast and fancy alike: how much higher, how far out, how large and how opaque.
 * Fast clouds are a flat grid of 32-block squares with a texel every 8 blocks, wrapping every 2048; fancy clouds are
 * cells of 8 by 8 texels of 12 blocks, 8 by 8 cells around the camera.
 */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalCloudsMixin {

    @Unique
    private static final float VANILLA_FAST_PERIOD = 2048.0F;
    @Unique
    private static final float VANILLA_FANCY_TEXEL = 12.0F;

    @ModifyExpressionValue(method = {"renderClouds", "renderCloudsFancy"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;getCloudHeight()F"))
    private float devilica$cloudHeight(float height) {
        return height + DevilicaOptions.get().details.cloudHeight;
    }

    @ModifyArg(method = {"renderClouds", "renderCloudsFancy"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;color(FFFF)Lnet/minecraft/client/renderer/BufferBuilder;"), index = 3)
    private float devilica$cloudOpacity(float alpha) {
        return DevilicaOptions.get().details.cloudOpacity / 100.0F;
    }

    /** How many times the vanilla size the clouds are, from 0.25 to 4 */
    @Unique
    private static float devilica$size() {
        return DevilicaOptions.get().details.cloudScale / 4.0F;
    }

    /** Fast clouds' reach in blocks, a whole number of their 32-block squares */
    @ModifyConstant(method = "renderClouds", constant = @Constant(intValue = 256))
    private int devilica$fastReach(int reach) {
        final int chunks = DevilicaOptions.get().details.cloudDistance;
        return chunks == 0 ? reach : (chunks * 16 + 31) / 32 * 32;
    }

    @ModifyConstant(method = "renderClouds", constant = @Constant(intValue = -256))
    private int devilica$fastReachBack(int reach) {
        return -this.devilica$fastReach(-reach);
    }

    // Fast clouds' texture period in blocks, as a double, an int and its inverse
    @ModifyConstant(method = "renderClouds", constant = @Constant(doubleValue = 2048.0))
    private double devilica$fastPeriod(double period) {
        return VANILLA_FAST_PERIOD * devilica$size();
    }

    @ModifyConstant(method = "renderClouds", constant = @Constant(intValue = 2048))
    private int devilica$fastPeriodInt(int period) {
        return Math.round(VANILLA_FAST_PERIOD * devilica$size());
    }

    @ModifyConstant(method = "renderClouds", constant = @Constant(floatValue = 4.8828125E-4F))
    private float devilica$fastTexel(float texel) {
        return 1.0F / (VANILLA_FAST_PERIOD * devilica$size());
    }

    @ModifyConstant(method = "renderClouds", constant = @Constant(doubleValue = 4.8828125E-4))
    private double devilica$fastTexelDouble(double texel) {
        return 1.0 / (VANILLA_FAST_PERIOD * devilica$size());
    }

    // Fancy clouds' blocks a texel, in the camera's texel position and the scale the cells are drawn at
    @ModifyConstant(method = "renderCloudsFancy", constant = @Constant(doubleValue = 12.0))
    private double devilica$fancyTexel(double texel) {
        return VANILLA_FANCY_TEXEL * devilica$size();
    }

    @ModifyConstant(method = "renderCloudsFancy", constant = @Constant(floatValue = 12.0F))
    private float devilica$fancyScale(float scale) {
        return VANILLA_FANCY_TEXEL * devilica$size();
    }

    /** Fancy clouds' last cell from the camera's, from -3 to 4 in vanilla; a cell is 8 texels across */
    @ModifyConstant(method = "renderCloudsFancy", constant = @Constant(intValue = 4))
    private int devilica$fancyReach(int cells) {
        final int chunks = DevilicaOptions.get().details.cloudDistance;
        if (chunks == 0) {
            return cells;
        }
        final float cellBlocks = 8.0F * VANILLA_FANCY_TEXEL * devilica$size();
        return Math.clamp((int) Math.ceil(chunks * 16 / cellBlocks), 1, 64);
    }

    @ModifyConstant(method = "renderCloudsFancy", constant = @Constant(intValue = -3))
    private int devilica$fancyReachBack(int cells) {
        return 1 - this.devilica$fancyReach(4);
    }
}
