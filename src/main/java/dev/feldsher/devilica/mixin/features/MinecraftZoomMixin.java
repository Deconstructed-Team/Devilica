/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.feldsher.devilica.zoom.Zoom;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While zoomed, the mouse wheel zooms instead of changing the hotbar slot. */
@Mixin(Minecraft.class)
public abstract class MinecraftZoomMixin {

    @ModifyExpressionValue(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
    private int devilica$zoomWheel(int wheel) {
        if (Zoom.scrolls()) {
            Zoom.scroll(wheel);
            return 0;
        }
        return wheel;
    }
}
