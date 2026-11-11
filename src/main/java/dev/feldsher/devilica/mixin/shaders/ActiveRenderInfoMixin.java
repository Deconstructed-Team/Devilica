/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.rendering.RenderingState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

/** The camera matrices, as vanilla reads them back each frame, for Iris's matrix uniforms. */
@Mixin(ActiveRenderInfo.class)
public abstract class ActiveRenderInfoMixin {

    @Shadow @Final
    private static FloatBuffer MODELVIEW;

    @Shadow @Final
    private static FloatBuffer PROJECTION;

    @Inject(method = "updateRenderInfo(Lnet/minecraft/entity/Entity;Z)V", at = @At("TAIL"), remap = false)
    private static void devilica$captureMatrices(Entity entity, boolean thirdPersonReverse, CallbackInfo ci) {
        RenderingState.INSTANCE.setProjectionMatrix(PROJECTION);
        RenderingState.INSTANCE.setModelViewMatrix(MODELVIEW);
    }
}
