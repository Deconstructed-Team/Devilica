/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.sky.CustomSky;
import net.coderbot.iris.Iris;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The sky's phases for Iris on 1.12.2's {@code renderSky}, and the parts a shader pack may turn off (sun, moon,
 * stars, sky disc) because it draws its own. The disc and the stars are a VBO or a display list depending on the VBO
 * setting, so both are wrapped. Also the block selection outline.
 */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalShadersMixin {

    @Shadow
    private WorldClient world;

    @Unique
    private static WorldRenderingPipeline devilica$pipeline() {
        return Iris.enabled ? Iris.getPipelineManager().getPipelineNullable() : null;
    }

    @Unique
    private static void devilica$phase(WorldRenderingPhase phase) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if (pipeline != null) {
            pipeline.setPhase(phase);
        }
    }

    // ---- phases

    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;getInstance()Lnet/minecraft/client/renderer/Tessellator;", ordinal = 0))
    private void devilica$skyPhase(float partialTicks, int pass, CallbackInfo ci) {
        devilica$phase(WorldRenderingPhase.SKY);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;calcSunriseSunsetColors(FF)[F"))
    private void devilica$sunsetPhase(float partialTicks, int pass, CallbackInfo ci) {
        devilica$phase(WorldRenderingPhase.SUNSET);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;SUN_TEXTURES:Lnet/minecraft/util/ResourceLocation;"))
    private void devilica$sunPhase(float partialTicks, int pass, CallbackInfo ci) {
        devilica$phase(WorldRenderingPhase.SUN);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;MOON_PHASES_TEXTURES:Lnet/minecraft/util/ResourceLocation;"))
    private void devilica$moonPhase(float partialTicks, int pass, CallbackInfo ci) {
        devilica$phase(WorldRenderingPhase.MOON);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getStarBrightness(F)F"))
    private void devilica$starsPhase(float partialTicks, int pass, CallbackInfo ci) {
        devilica$phase(WorldRenderingPhase.STARS);
    }

    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;getPositionEyes(F)Lnet/minecraft/util/math/Vec3d;"))
    private void devilica$voidPhase(float partialTicks, int pass, CallbackInfo ci) {
        devilica$phase(WorldRenderingPhase.VOID);
    }

    /**
     * Before vanilla turns the sun and moon by the time of day: a resource pack's custom sky layers (OptiFine draws them
     * here), then the shader pack's sun path rotation tilts the sun and moon.
     */
    @Inject(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getCelestialAngle(F)F"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getRainStrength(F)F")))
    private void devilica$customSkyAndTilt(float partialTicks, int pass, CallbackInfo ci) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);
        }
        CustomSky.render(this.world, partialTicks);
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.SUN);
            GlStateManager.rotate(pipeline.getSunPathRotation(), 0.0F, 0.0F, 1.0F);
        }
    }

    /** A resource pack's own sun and moon images */
    @ModifyArg(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0))
    private ResourceLocation devilica$customSun(ResourceLocation texture) {
        return CustomSky.celestial(this.world, texture, Minecraft.getMinecraft().getRenderPartialTicks());
    }

    @ModifyArg(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 1))
    private ResourceLocation devilica$customMoon(ResourceLocation texture) {
        return CustomSky.celestial(this.world, texture, Minecraft.getMinecraft().getRenderPartialTicks());
    }

    // ---- parts a pack or the details settings turn off

    @WrapOperation(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V", ordinal = 0))
    private void devilica$skyDiscVbo(VertexBuffer buffer, int mode, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if ((pipeline == null || pipeline.shouldRenderSkyDisc()) && DevilicaOptions.get().details.sky) {
            original.call(buffer, mode);
        }
    }

    @WrapOperation(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 0))
    private void devilica$skyDiscList(int list, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if ((pipeline == null || pipeline.shouldRenderSkyDisc()) && DevilicaOptions.get().details.sky) {
            original.call(list);
        }
    }

    @WrapOperation(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V", ordinal = 0),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;SUN_TEXTURES:Lnet/minecraft/util/ResourceLocation;"),
            to = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;MOON_PHASES_TEXTURES:Lnet/minecraft/util/ResourceLocation;")))
    private void devilica$sun(Tessellator tessellator, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if ((pipeline == null || pipeline.shouldRenderSun()) && DevilicaOptions.get().details.sun) {
            original.call(tessellator);
        } else {
            devilica$discard(tessellator.getBuffer());
        }
    }

    @WrapOperation(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V", ordinal = 0),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;MOON_PHASES_TEXTURES:Lnet/minecraft/util/ResourceLocation;"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getStarBrightness(F)F")))
    private void devilica$moon(Tessellator tessellator, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if ((pipeline == null || pipeline.shouldRenderMoon()) && DevilicaOptions.get().details.moon) {
            original.call(tessellator);
        } else {
            devilica$discard(tessellator.getBuffer());
        }
    }

    @WrapOperation(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getStarBrightness(F)F")))
    private void devilica$starsVbo(VertexBuffer buffer, int mode, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if ((pipeline == null || pipeline.shouldRenderStars()) && DevilicaOptions.get().details.stars) {
            original.call(buffer, mode);
        }
    }

    @WrapOperation(method = "renderSky(FI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getStarBrightness(F)F")))
    private void devilica$starsList(int list, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = devilica$pipeline();
        if ((pipeline == null || pipeline.shouldRenderStars()) && DevilicaOptions.get().details.stars) {
            original.call(list);
        }
    }

    @Unique
    private static void devilica$discard(BufferBuilder buffer) {
        buffer.finishDrawing();
        buffer.reset();
    }

    // ---- block selection outline

    @Inject(method = "drawSelectionBox", at = @At("HEAD"))
    private void devilica$beginOutline(EntityPlayer player, RayTraceResult hit, int execute, float partialTicks, CallbackInfo ci) {
        if (Iris.enabled) {
            GbufferPrograms.beginOutline();
        }
    }

    @Inject(method = "drawSelectionBox", at = @At("RETURN"))
    private void devilica$endOutline(EntityPlayer player, RayTraceResult hit, int execute, float partialTicks, CallbackInfo ci) {
        if (Iris.enabled) {
            GbufferPrograms.endOutline();
        }
    }
}
