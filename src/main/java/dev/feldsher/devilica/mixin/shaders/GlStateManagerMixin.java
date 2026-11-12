/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.texture.TextureTracker;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gbuffer_overrides.state.StateTracker;
import net.coderbot.iris.gl.blending.AlphaTestStorage;
import net.coderbot.iris.gl.blending.BlendModeStorage;
import net.coderbot.iris.gl.blending.DepthColorStorage;
import net.coderbot.iris.gl.state.StateUpdateNotifiers;
import net.coderbot.iris.samplers.IrisSamplers;
import net.coderbot.iris.texture.pbr.PBRTextureManager;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

/**
 * What Angelica's own GL state manager did for Iris, done on vanilla's: while a shader pack overrides blending, the
 * alpha test or depth/colour writes, vanilla's and mods' changes to them are deferred instead of applied; texture binds,
 * deletes and unit switches feed Iris's trackers; the viewport and fog colour, which vanilla does not cache, are
 * recorded. The texture unit cache grows from 8 to 32 units for shader pack samplers.
 */
@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {

    @Shadow @Final @Mutable
    private static GlStateManager.TextureState[] textureState;

    @Shadow
    private static int activeTextureUnit;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void devilica$growTextureUnits(CallbackInfo ci) {
        final GlStateManager.TextureState[] grown = new GlStateManager.TextureState[GLStateManager.MAX_TEXTURE_UNITS];
        System.arraycopy(textureState, 0, grown, 0, textureState.length);
        for (int unit = textureState.length; unit < grown.length; unit++) {
            grown[unit] = new GlStateManager.TextureState();
        }
        textureState = grown;
    }

    // ---- blending

    @Inject(method = "enableBlend", at = @At("HEAD"), cancellable = true)
    private static void devilica$enableBlend(CallbackInfo ci) {
        if (Iris.enabled && BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendModeToggle(true);
            BlendModeStorage.gameBlendChanged();
            ci.cancel();
        }
    }

    @Inject(method = "disableBlend", at = @At("HEAD"), cancellable = true)
    private static void devilica$disableBlend(CallbackInfo ci) {
        if (Iris.enabled && BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendModeToggle(false);
            BlendModeStorage.gameBlendChanged();
            ci.cancel();
        }
    }

    @Inject(method = "blendFunc(II)V", at = @At("HEAD"), cancellable = true)
    private static void devilica$blendFunc(int srcFactor, int dstFactor, CallbackInfo ci) {
        if (Iris.enabled && BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendFunc(srcFactor, dstFactor, srcFactor, dstFactor);
            BlendModeStorage.gameBlendChanged();
            ci.cancel();
        }
    }

    @Inject(method = "tryBlendFuncSeparate(IIII)V", at = @At("HEAD"), cancellable = true)
    private static void devilica$tryBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, CallbackInfo ci) {
        if (Iris.enabled && BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendFunc(srcRgb, dstRgb, srcAlpha, dstAlpha);
            BlendModeStorage.gameBlendChanged();
            ci.cancel();
        }
    }

    /** Changes that reached GL (a deferred one returns at the head): the pack's blendFunc uniform follows them. */
    @Inject(method = {"enableBlend", "disableBlend", "blendFunc(II)V", "tryBlendFuncSeparate(IIII)V"}, at = @At("RETURN"))
    private static void devilica$blendChanged(CallbackInfo ci) {
        if (Iris.enabled) {
            StateUpdateNotifiers.blendFuncNotifier.run();
            BlendModeStorage.gameBlendChanged();
        }
    }

    // ---- depth and colour writes

    @Inject(method = "depthMask", at = @At("HEAD"), cancellable = true)
    private static void devilica$depthMask(boolean flag, CallbackInfo ci) {
        if (Iris.enabled && DepthColorStorage.isDepthColorLocked()) {
            DepthColorStorage.deferDepthEnable(flag);
            ci.cancel();
        }
    }

    @Inject(method = "colorMask", at = @At("HEAD"), cancellable = true)
    private static void devilica$colorMask(boolean red, boolean green, boolean blue, boolean alpha, CallbackInfo ci) {
        if (Iris.enabled && DepthColorStorage.isDepthColorLocked()) {
            DepthColorStorage.deferColorMask(red, green, blue, alpha);
            ci.cancel();
        }
    }

    // ---- alpha test

    @Inject(method = "enableAlpha", at = @At("HEAD"), cancellable = true)
    private static void devilica$enableAlpha(CallbackInfo ci) {
        if (Iris.enabled && AlphaTestStorage.isAlphaTestLocked()) {
            AlphaTestStorage.deferAlphaTestToggle(true);
            ci.cancel();
        }
    }

    @Inject(method = "disableAlpha", at = @At("HEAD"), cancellable = true)
    private static void devilica$disableAlpha(CallbackInfo ci) {
        if (Iris.enabled && AlphaTestStorage.isAlphaTestLocked()) {
            AlphaTestStorage.deferAlphaTestToggle(false);
            ci.cancel();
        }
    }

    @Inject(method = "alphaFunc", at = @At("HEAD"), cancellable = true)
    private static void devilica$alphaFunc(int func, float ref, CallbackInfo ci) {
        if (Iris.enabled && AlphaTestStorage.isAlphaTestLocked()) {
            AlphaTestStorage.deferAlphaFunc(func, ref);
            ci.cancel();
        }
    }

    // ---- textures

    /**
     * After the bind reached GL (vanilla skips unchanged binds): the tracker rebinds through direct state access,
     * which GL rejects for a texture name that was never bound. Forge's splash thread binds too; only the render
     * thread tells Iris.
     */
    @Inject(method = "bindTexture", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V", shift = At.Shift.AFTER, remap = false))
    private static void devilica$bindTexture(int texture, CallbackInfo ci) {
        if (GLStateManager.isMainThread()) {
            TextureTracker.INSTANCE.onBindTexture(texture);
            if (GLStateManager.getActiveTextureUnit() == 0) {
                StateUpdateNotifiers.bindTextureNotifier.run();
            }
        }
    }

    @Inject(method = "deleteTexture", at = @At("HEAD"))
    private static void devilica$deleteTexture(int texture, CallbackInfo ci) {
        TextureTracker.INSTANCE.onDeleteTexture(texture);
        if (Iris.enabled) {
            PBRTextureManager.INSTANCE.onDeleteTexture(texture);
        }
    }

    @Inject(method = "enableTexture2D", at = @At("RETURN"))
    private static void devilica$enableTexture2D(CallbackInfo ci) {
        devilica$onTextureUnitState(true);
    }

    @Inject(method = "disableTexture2D", at = @At("RETURN"))
    private static void devilica$disableTexture2D(CallbackInfo ci) {
        devilica$onTextureUnitState(false);
    }

    private static void devilica$onTextureUnitState(boolean enabled) {
        if (!Iris.enabled) {
            return;
        }
        final int unit = activeTextureUnit;
        if (unit == IrisSamplers.ALBEDO_TEXTURE_UNIT) {
            StateTracker.INSTANCE.albedoSampler = enabled;
        } else if (unit == IrisSamplers.LIGHTMAP_TEXTURE_UNIT) {
            StateTracker.INSTANCE.lightmapSampler = enabled;
        } else {
            return;
        }
        Iris.getPipelineManager().getPipeline().ifPresent(p -> p.setInputs(StateTracker.INSTANCE.getInputs()));
    }

    // ---- state vanilla does not cache

    @Inject(method = "viewport", at = @At("RETURN"))
    private static void devilica$viewport(int x, int y, int width, int height, CallbackInfo ci) {
        GLStateManager.onViewport(x, y, width, height);
    }

    @Inject(method = "glFog", at = @At("HEAD"))
    private static void devilica$glFog(int pname, FloatBuffer param, CallbackInfo ci) {
        if (pname == GL11.GL_FOG_COLOR) {
            final int at = param.position();
            GLStateManager.onFogColor(param.get(at), param.get(at + 1), param.get(at + 2), param.get(at + 3));
        }
    }

    @Inject(method = {"enableFog", "disableFog", "setFog", "setFogDensity", "setFogStart", "setFogEnd"}, at = @At("RETURN"))
    private static void devilica$fogChanged(CallbackInfo ci) {
        if (Iris.enabled) {
            StateUpdateNotifiers.fogNotifier.run();
        }
    }

    // ---- draws: a blend change deferred during an override applies once the override ends, before the next draw;
    // a pass that depends on the game's blending is matched again here, not at every change

    @Inject(method = "glDrawArrays", at = @At("HEAD"))
    private static void devilica$beforeDrawArrays(int mode, int first, int count, CallbackInfo ci) {
        if (Iris.enabled) {
            BlendModeStorage.beforeDraw();
        }
    }

    @Inject(method = "callList", at = @At("HEAD"))
    private static void devilica$beforeCallList(int list, CallbackInfo ci) {
        if (Iris.enabled) {
            BlendModeStorage.beforeDraw();
        }
    }

    @Inject(method = "glBegin", at = @At("HEAD"))
    private static void devilica$beforeBegin(int mode, CallbackInfo ci) {
        if (Iris.enabled) {
            BlendModeStorage.beforeDraw();
        }
    }
}
