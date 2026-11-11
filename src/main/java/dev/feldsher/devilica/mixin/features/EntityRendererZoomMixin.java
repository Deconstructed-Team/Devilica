/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.zoom.Zoom;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Zooming narrows the field of view, slows the mouse down to match, and may turn the cinematic camera on; in third
 * person it moves the camera instead.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererZoomMixin {

    @ModifyReturnValue(method = "getFOVModifier", at = @At("RETURN"))
    private float devilica$zoomFov(float fov) {
        final float zoom = Zoom.viewZoom();
        return zoom > 1.0001F ? fov / zoom : fov;
    }

    @ModifyConstant(method = "updateCameraAndRender", constant = @Constant(floatValue = 8.0F))
    private float devilica$zoomSensitivity(float sensitivity) {
        return sensitivity * Zoom.sensitivityFactor();
    }

    // The first double stored after the camera's position: the third-person distance, before blocks behind shorten it
    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE", ordinal = 0), ordinal = 3)
    private double devilica$zoomDistance(double distance) {
        return Zoom.cameraDistance(distance);
    }

    @Redirect(method = {"updateRenderer", "updateCameraAndRender"}, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/settings/GameSettings;smoothCamera:Z"))
    private boolean devilica$zoomCinematic(GameSettings settings) {
        return settings.smoothCamera || Zoom.useCinematicCamera();
    }
}
