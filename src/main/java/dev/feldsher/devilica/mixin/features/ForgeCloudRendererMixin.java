/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraftforge.client.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The cloud settings on Forge's cloud renderer, which draws the clouds instead of vanilla's while Forge's
 * {@code forgeCloudsEnabled} is on (its default). It keeps the clouds in a buffer, built again when a setting changes.
 */
@Mixin(value = CloudRenderer.class, remap = false)
public abstract class ForgeCloudRendererMixin {

    @Shadow
    private void dispose() {
    }

    @Shadow
    private boolean isBuilt() {
        return false;
    }

    /** The distance, size and opacity the buffer was built with */
    @Unique
    private long devilica$builtWith = -1L;

    @Inject(method = "checkSettings", at = @At("HEAD"))
    private void devilica$rebuild(CallbackInfo ci) {
        final DevilicaOptions.DetailsSettings details = DevilicaOptions.get().details;
        final long settings = details.cloudDistance | (long) details.cloudScale << 16 | (long) details.cloudOpacity << 32;
        if (settings != this.devilica$builtWith) {
            if (this.isBuilt()) {
                this.dispose();
            }
            this.devilica$builtWith = settings;
        }
    }

    /** Blocks a texel: 12 fancy, 8 fast, times the size setting (a whole number for every size) */
    @ModifyReturnValue(method = "getScale", at = @At("RETURN"))
    private int devilica$size(int scale) {
        return Math.round(scale * DevilicaOptions.get().details.cloudScale / 4.0F);
    }

    /** How far out the clouds reach, twice the render distance unless set */
    @ModifyArg(method = "vertices", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/CloudRenderer;ceilToScale(F)F", ordinal = 0))
    private float devilica$distance(float reach) {
        final int chunks = DevilicaOptions.get().details.cloudDistance;
        return chunks == 0 ? reach : chunks * 16.0F;
    }

    @ModifyArg(method = "vertices", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;color(FFFF)Lnet/minecraft/client/renderer/BufferBuilder;", remap = true), index = 3)
    private float devilica$opacity(float alpha) {
        return DevilicaOptions.get().details.cloudOpacity / 100.0F;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;getCloudHeight()F", remap = true))
    private float devilica$height(float height) {
        return height + DevilicaOptions.get().details.cloudHeight;
    }
}
