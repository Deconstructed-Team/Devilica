/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The details settings: where the land's fog starts and ends, no fog, no rain and snow, no darker fog near the void. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererDetailsMixin {

    @Shadow
    private float farPlaneDistance;

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 0.75F))
    private float devilica$fogStart(float start) {
        return DevilicaOptions.get().details.fogStart / 100.0F;
    }

    /** Only the land's fog branch posts this event; fog in water, lava and blindness stays. */
    @Inject(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;onFogRender(Lnet/minecraft/client/renderer/EntityRenderer;Lnet/minecraft/entity/Entity;Lnet/minecraft/block/state/IBlockState;FIF)V", remap = false))
    private void devilica$noFog(int startCoords, float partialTicks, CallbackInfo ci) {
        final DevilicaOptions.DetailsSettings details = DevilicaOptions.get().details;
        if (!details.fog) {
            GlStateManager.setFogStart(this.farPlaneDistance * 100.0F);
            GlStateManager.setFogEnd(this.farPlaneDistance * 101.0F);
        } else if (details.fogDistance > 0 && startCoords != -1) {
            final float end = details.fogDistance * 16.0F;
            GlStateManager.setFogStart(end * details.fogStart / 100.0F);
            GlStateManager.setFogEnd(end);
        }
    }

    // The fog darkens below y = 1 / factor; a factor this large keeps it bright down to the void (blindness still darkens)
    @ModifyExpressionValue(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;getVoidFogYFactor()D"))
    private double devilica$voidFog(double factor) {
        return DevilicaOptions.get().details.voidFog ? factor : 1.0E6;
    }

    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true)
    private void devilica$noWeather(float partialTicks, CallbackInfo ci) {
        if (!DevilicaOptions.get().details.weather) {
            ci.cancel();
        }
    }
}
