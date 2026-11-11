/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.cit;

import dev.feldsher.devilica.mixin.features.RenderItemAccessor;
import dev.feldsher.devilica.shaders.ShaderHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * OptiFine's custom enchantment glints ({@code type=enchantment}): the packs' glint textures, layer over layer, with
 * their own blending, scrolling speed and angle, in place of vanilla's glint on items and armour.
 */
public final class CustomGlints {

    private CustomGlints() {
    }

    /** Draws an item's custom glints; false when it has none and gets vanilla's */
    public static boolean renderItem(RenderItem renderItem, IBakedModel model, ItemStack stack) {
        final List<CitRule> glints = CustomItemTextures.glints(stack);
        if (glints.isEmpty()) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getMinecraft();
        ShaderHooks.beginGlint();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_EQUAL);
        GlStateManager.disableLighting();
        final float seconds = Minecraft.getSystemTime() / 1000.0F;
        for (CitRule glint : glints) {
            minecraft.getTextureManager().bindTexture(glint.texture);
            blend(glint.blend);
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            // Item texture coordinates are the atlas's; vanilla's glint spans an eighth of them
            GlStateManager.scale(8.0F, 8.0F, 8.0F);
            GlStateManager.translate(glint.speed * seconds % 1.0F / 8.0F, 0.0F, 0.0F);
            GlStateManager.rotate(glint.rotation, 0.0F, 0.0F, 1.0F);
            ((RenderItemAccessor) renderItem).devilica$renderModel(model, -1);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableLighting();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        ShaderHooks.endCondition();
        return true;
    }

    /** Draws a worn armour piece's custom glints; false when it has none and gets vanilla's */
    public static boolean renderArmor(RenderLivingBase<?> renderer, EntityLivingBase entity, ModelBase model, ItemStack stack, float limbSwing,
                                      float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        final List<CitRule> glints = CustomItemTextures.glints(stack);
        if (glints.isEmpty()) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getMinecraft();
        final float seconds = (entity.ticksExisted + partialTicks) / 20.0F;
        ShaderHooks.beginGlint();
        minecraft.entityRenderer.setupFogColor(true);
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(GL11.GL_EQUAL);
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        for (CitRule glint : glints) {
            renderer.bindTexture(glint.texture);
            blend(glint.blend);
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.loadIdentity();
            GlStateManager.scale(1.0F / 3.0F, 1.0F / 3.0F, 1.0F / 3.0F);
            GlStateManager.rotate(glint.rotation, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(0.0F, glint.speed * seconds % 1.0F, 0.0F);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.disableBlend();
        minecraft.entityRenderer.setupFogColor(false);
        ShaderHooks.endCondition();
        return true;
    }

    /** MCPatcher's and OptiFine's blend methods */
    private static void blend(String method) {
        GlStateManager.enableBlend();
        switch (method) {
            case "replace" -> GlStateManager.disableBlend();
            case "alpha" -> GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            case "subtract" -> GlStateManager.blendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
            case "multiply" -> GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA);
            case "dodge" -> GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
            case "burn" -> GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
            case "screen" -> GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);
            case "overlay" -> GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
            default -> GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
    }
}
