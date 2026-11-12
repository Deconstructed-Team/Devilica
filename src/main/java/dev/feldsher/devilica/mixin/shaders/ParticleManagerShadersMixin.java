/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.shaders.ShaderHooks;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Which particle queue draws next, for the pack's opaque and translucent particle programs: vanilla turns depth writes
 * off before a queue of translucent particles, on before a queue of the others and once all are drawn.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerShadersMixin {

    @WrapOperation(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V"))
    private void devilica$particleQueue(boolean writesDepth, Operation<Void> original) {
        original.call(writesDepth);
        ShaderHooks.particleQueue(!writesDepth);
    }
}
