/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.ctm.EmissiveTextures;
import dev.feldsher.devilica.randomentities.RandomEntities;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A mob whose texture has an emissive {@code _e} image draws its model once more with it, at full light. */
@Mixin(RenderLivingBase.class)
public abstract class RenderLivingBaseEmissiveMixin<T extends EntityLivingBase> extends Render<T> {

    @Shadow
    protected ModelBase mainModel;

    protected RenderLivingBaseEmissiveMixin() {
        super(null);
    }

    @Inject(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.AFTER))
    private void devilica$emissive(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        final ResourceLocation texture = this.getEntityTexture(entity);
        // The look random entities picked, as the texture manager binds it
        final ResourceLocation glow = EmissiveTextures.glowTexture(RandomEntities.choose(texture));
        if (glow == null) {
            return;
        }
        final float lastX = OpenGlHelper.lastBrightnessX;
        final float lastY = OpenGlHelper.lastBrightnessY;
        this.bindTexture(glow);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        this.mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        this.bindTexture(texture);
    }
}
