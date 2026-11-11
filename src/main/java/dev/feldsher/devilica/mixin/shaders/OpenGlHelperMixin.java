/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import dev.feldsher.devilica.Devilica;
import net.coderbot.iris.Iris;
import net.coderbot.iris.samplers.IrisSamplers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Once vanilla has set up its GL entry points, GL can be used: the GL capabilities Iris relies on are read (what
 * Angelica's GLStateManager.init did) and Iris loads the shader pack.
 */
@Mixin(OpenGlHelper.class)
public abstract class OpenGlHelperMixin {

    @Inject(method = "initializeTextures", at = @At("RETURN"))
    private static void devilica$initializeIrisRenderer(CallbackInfo ci) {
        final Minecraft mc = Minecraft.getMinecraft();
        GLStateManager.onViewport(0, 0, mc.displayWidth, mc.displayHeight);
        devilica$checkError("before Iris init");
        RenderSystem.initRenderer();
        devilica$checkError("RenderSystem.initRenderer");
        IrisSamplers.initRenderer();
        devilica$checkError("IrisSamplers.initRenderer");
        Iris.onRenderSystemInit();
        devilica$checkError("Iris.onRenderSystemInit");
    }

    /** GL errors left here would surface in Forge's splash screen, which checks for them; log and clear them. */
    @Unique
    private static void devilica$checkError(String step) {
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            Devilica.LOGGER.warn("GL error 0x{} after {}", Integer.toHexString(error), step);
        }
    }
}
