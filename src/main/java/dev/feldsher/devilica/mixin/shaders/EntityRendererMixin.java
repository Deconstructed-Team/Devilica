/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.compat.mojang.Camera;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.mixins.interfaces.EntityRendererAccessor;
import com.gtnewhorizons.angelica.rendering.RenderingState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.program.Program;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris's world rendering phases on 1.12.2's {@code renderWorldPass}, the counterpart of Angelica's hooks on 1.7.10's
 * {@code renderWorld}: the pipeline begins once the frustum is set up, the shadow pass runs before the terrain is set
 * up, the sky, clouds, weather, particles and block damage each run in their phase, and the pipeline's composite and
 * final passes run before the hand. The translucent hand is drawn by Iris; the solid hand before translucent terrain
 * ({@code ShaderHooks}).
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin implements EntityRendererAccessor {

    @Shadow @Final
    private Minecraft mc;

    @Unique
    private WorldRenderingPipeline devilica$pipeline;

    @Invoker("getNightVisionBrightness")
    @Override
    public abstract float invokeGetNightVisionBrightness(EntityLivingBase entity, float partialTicks);

    @Accessor("lightmapTexture")
    @Override
    public abstract DynamicTexture getLightmapTexture();

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;updateRenderInfo(Lnet/minecraft/entity/Entity;Z)V", shift = At.Shift.AFTER, remap = false))
    private void devilica$captureCamera(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        final Entity viewEntity = this.mc.getRenderViewEntity();
        Camera.INSTANCE.update(viewEntity, partialTicks);
        // The camera position uniform is the view entity's feet (the origin 1.12 draws the world from), not the third-person camera
        RenderingState.INSTANCE.setCameraPosition(Camera.INSTANCE.getEntityPos().x, Camera.INSTANCE.getEntityPos().y, Camera.INSTANCE.getEntityPos().z);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/ClippingHelperImpl;getInstance()Lnet/minecraft/client/renderer/culling/ClippingHelper;", shift = At.Shift.AFTER))
    private void devilica$beginLevelRendering(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setTickDelta(partialTicks);
        SystemTimeUniforms.COUNTER.beginFrame();
        SystemTimeUniforms.TIMER.beginFrame(System.nanoTime());

        Program.unbind();

        this.devilica$pipeline = Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimensionName());
        this.devilica$pipeline.beginLevelRendering();
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V"))
    private void devilica$renderShadows(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (this.devilica$pipeline != null) {
            this.devilica$pipeline.renderShadows((EntityRenderer) (Object) this, Camera.INSTANCE);
        }
    }

    /** Shader packs draw their sky even at render distances vanilla skips it at. */
    @Redirect(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I", ordinal = 0))
    private int devilica$alwaysRenderSky(GameSettings settings) {
        return Math.max(settings.renderDistanceChunks, 4);
    }

    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V"))
    private void devilica$sky(RenderGlobal renderGlobal, float partialTicks, int pass, Operation<Void> original) {
        devilica$phase(WorldRenderingPhase.CUSTOM_SKY);
        original.call(renderGlobal, partialTicks, pass);
        devilica$phase(WorldRenderingPhase.NONE);
    }

    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderCloudsCheck(Lnet/minecraft/client/renderer/RenderGlobal;FIDDD)V"))
    private void devilica$clouds(EntityRenderer instance, RenderGlobal renderGlobal, float partialTicks, int pass, double x, double y, double z, Operation<Void> original) {
        devilica$phase(WorldRenderingPhase.CLOUDS);
        original.call(instance, renderGlobal, partialTicks, pass, x, y, z);
        devilica$phase(WorldRenderingPhase.NONE);
    }

    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderRainSnow(F)V"))
    private void devilica$weather(EntityRenderer instance, float partialTicks, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = this.devilica$pipeline;
        if (pipeline == null) {
            original.call(instance, partialTicks);
            return;
        }
        pipeline.setPhase(WorldRenderingPhase.RAIN_SNOW);
        if (pipeline.shouldWriteRainAndSnowToDepthBuffer()) {
            GLStateManager.glDepthMask(true);
        }
        if (pipeline.shouldRenderWeather()) {
            original.call(instance, partialTicks);
        }
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;F)V"))
    private void devilica$blockDamage(RenderGlobal renderGlobal, Tessellator tessellator, BufferBuilder buffer, Entity entity, float partialTicks, Operation<Void> original) {
        devilica$phase(WorldRenderingPhase.DESTROY);
        original.call(renderGlobal, tessellator, buffer, entity, partialTicks);
        devilica$phase(WorldRenderingPhase.NONE);
    }

    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderLitParticles(Lnet/minecraft/entity/Entity;F)V"))
    private void devilica$litParticles(ParticleManager particles, Entity entity, float partialTicks, Operation<Void> original) {
        devilica$phase(WorldRenderingPhase.PARTICLES);
        original.call(particles, entity, partialTicks);
        devilica$phase(WorldRenderingPhase.NONE);
    }

    @WrapOperation(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/entity/Entity;F)V"))
    private void devilica$particles(ParticleManager particles, Entity entity, float partialTicks, Operation<Void> original) {
        devilica$phase(WorldRenderingPhase.PARTICLES);
        original.call(particles, entity, partialTicks);
        devilica$phase(WorldRenderingPhase.NONE);
    }

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V", remap = false))
    private void devilica$finishLevelRendering(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        final WorldRenderingPipeline pipeline = this.devilica$pipeline;
        if (pipeline == null) {
            return;
        }
        HandRenderer.INSTANCE.renderTranslucent(partialTicks, Camera.INSTANCE, this.mc.renderGlobal, pipeline);
        this.mc.profiler.endStartSection("iris_final");
        pipeline.finalizeLevelRendering();
        this.devilica$pipeline = null;
        Program.unbind();
        GLStateManager.glDepthMask(true);
    }

    /** With a shader pack, Iris draws the first-person hand itself, in its own passes. */
    @WrapOperation(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemInFirstPerson(F)V"))
    private void devilica$skipVanillaHand(ItemRenderer itemRenderer, float partialTicks, Operation<Void> original) {
        if (!IrisApi.getInstance().isShaderPackInUse()) {
            original.call(itemRenderer, partialTicks);
        }
    }

    @WrapOperation(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;addRainParticles()V"))
    private void devilica$rainParticles(EntityRenderer instance, Operation<Void> original) {
        final WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline == null || pipeline.shouldRenderWeatherParticles()) {
            original.call(instance);
        }
    }

    @ModifyArg(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", ordinal = 0, remap = false), index = 0)
    private float devilica$captureFov(float fov) {
        RenderingState.INSTANCE.setFov(fov);
        return fov;
    }

    @Unique
    private void devilica$phase(WorldRenderingPhase phase) {
        if (this.devilica$pipeline != null) {
            this.devilica$pipeline.setPhase(phase);
        }
    }
}
