/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.Iris;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.FloatBuffer;

/**
 * The hurt flash and a creeper's swelling reach shaders as {@code entityColor}, the colour vanilla blends in through
 * texture environment state (OptiFine's way); an entity starts without a held item's ID.
 */
@Mixin(RenderLivingBase.class)
public abstract class RenderLivingBaseMixin {

    @Shadow
    protected FloatBuffer brightnessBuffer;

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private void devilica$resetItemId(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        ShaderHooks.resetItemId();
    }

    @Inject(method = "setBrightness", at = @At("RETURN"))
    private void devilica$captureEntityColor(EntityLivingBase entity, float partialTicks, boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        if (Iris.enabled && cir.getReturnValueZ()) {
            final FloatBuffer color = this.brightnessBuffer;
            CapturedRenderingState.INSTANCE.setCurrentEntityColor(color.get(0), color.get(1), color.get(2), color.get(3));
        }
    }

    @Inject(method = "unsetBrightness", at = @At("HEAD"))
    private void devilica$clearEntityColor(CallbackInfo ci) {
        if (Iris.enabled) {
            CapturedRenderingState.INSTANCE.setCurrentEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }
}
