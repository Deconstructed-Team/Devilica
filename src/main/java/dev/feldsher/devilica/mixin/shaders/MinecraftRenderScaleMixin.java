/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.shaders.RenderScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While the world is drawn at a lower resolution, its target stands in for the main framebuffer. Minecraft's own
 * resizing and final blit use the field, not this getter, so they keep the real one.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftRenderScaleMixin {

    @ModifyReturnValue(method = "getFramebuffer", at = @At("RETURN"))
    private Framebuffer devilica$scaledWorldTarget(Framebuffer framebuffer) {
        return RenderScale.isActive() ? RenderScale.worldTarget() : framebuffer;
    }
}
