/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packs with real shadows turn off vanilla's round entity shadows. The fire on a burning entity and name tags are
 * entities of their own to shader packs ({@code entity_flame}, {@code name_tag}).
 */
@Mixin(Render.class)
public abstract class RenderMixin {

    @Unique
    private static final NamespacedId devilica$FLAME = new NamespacedId("minecraft", "entity_flame");

    @Unique
    private static final NamespacedId devilica$NAME_TAG = new NamespacedId("minecraft", "name_tag");

    @Unique
    private int devilica$previousEntityId = ShaderHooks.NO_ID;

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private void devilica$skipVanillaShadow(Entity entity, double x, double y, double z, float shadowAlpha, float partialTicks, CallbackInfo ci) {
        final WorldRenderingPipeline pipeline = Iris.enabled ? Iris.getPipelineManager().getPipelineNullable() : null;
        if (pipeline != null && pipeline.shouldDisableVanillaEntityShadows()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEntityOnFire", at = @At("HEAD"))
    private void devilica$beginFlame(Entity entity, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        this.devilica$previousEntityId = ShaderHooks.setSpecialEntityId(devilica$FLAME);
    }

    @Inject(method = "renderEntityOnFire", at = @At("RETURN"))
    private void devilica$endFlame(Entity entity, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        ShaderHooks.restoreEntityId(this.devilica$previousEntityId);
        this.devilica$previousEntityId = ShaderHooks.NO_ID;
    }

    @Inject(method = "renderLivingLabel", at = @At("HEAD"))
    private void devilica$beginNameTag(Entity entity, String name, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        this.devilica$previousEntityId = ShaderHooks.setSpecialEntityId(devilica$NAME_TAG);
    }

    @Inject(method = "renderLivingLabel", at = @At("RETURN"))
    private void devilica$endNameTag(Entity entity, String name, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        ShaderHooks.restoreEntityId(this.devilica$previousEntityId);
        this.devilica$previousEntityId = ShaderHooks.NO_ID;
    }
}
