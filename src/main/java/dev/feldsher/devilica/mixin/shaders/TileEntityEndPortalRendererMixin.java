/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla's portal is texture coordinate generation over many passes, which shader programs do not run; with a shader
 * pack the portal is one textured, fully lit pass the pack can shade by its block entity ID (as Angelica draws it).
 * End gateways go through the same method.
 */
@Mixin(TileEntityEndPortalRenderer.class)
public abstract class TileEntityEndPortalRendererMixin {

    @Unique
    private static final ResourceLocation devilica$PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png");

    @Shadow
    protected abstract float getOffset();

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntityEndPortal;DDDFIF)V", at = @At("HEAD"), cancellable = true)
    private void devilica$shaderPortal(TileEntityEndPortal te, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
        if (!IrisApi.getInstance().isShaderPackInUse()) {
            return;
        }
        ci.cancel();

        final float u0 = (SystemTimeUniforms.TIMER.getFrameTimeCounter() * 0.01F) % 1.0F;
        final float u1 = u0 + 0.2F;
        final float r = 0.075F;
        final float g = 0.15F;
        final float b = 0.2F;
        final double top = y + this.getOffset();

        Minecraft.getMinecraft().getTextureManager().bindTexture(devilica$PORTAL_TEXTURE);
        GlStateManager.enableTexture2D();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);

        if (te.shouldRenderFace(EnumFacing.SOUTH)) {
            buffer.pos(x, y, z + 1).tex(u0, u0).color(r, g, b, 1.0F).normal(0, 0, 1).endVertex();
            buffer.pos(x + 1, y, z + 1).tex(u1, u0).color(r, g, b, 1.0F).normal(0, 0, 1).endVertex();
            buffer.pos(x + 1, y + 1, z + 1).tex(u1, u1).color(r, g, b, 1.0F).normal(0, 0, 1).endVertex();
            buffer.pos(x, y + 1, z + 1).tex(u0, u1).color(r, g, b, 1.0F).normal(0, 0, 1).endVertex();
        }
        if (te.shouldRenderFace(EnumFacing.NORTH)) {
            buffer.pos(x, y + 1, z).tex(u0, u1).color(r, g, b, 1.0F).normal(0, 0, -1).endVertex();
            buffer.pos(x + 1, y + 1, z).tex(u1, u1).color(r, g, b, 1.0F).normal(0, 0, -1).endVertex();
            buffer.pos(x + 1, y, z).tex(u1, u0).color(r, g, b, 1.0F).normal(0, 0, -1).endVertex();
            buffer.pos(x, y, z).tex(u0, u0).color(r, g, b, 1.0F).normal(0, 0, -1).endVertex();
        }
        if (te.shouldRenderFace(EnumFacing.EAST)) {
            buffer.pos(x + 1, y + 1, z).tex(u0, u1).color(r, g, b, 1.0F).normal(1, 0, 0).endVertex();
            buffer.pos(x + 1, y + 1, z + 1).tex(u1, u1).color(r, g, b, 1.0F).normal(1, 0, 0).endVertex();
            buffer.pos(x + 1, y, z + 1).tex(u1, u0).color(r, g, b, 1.0F).normal(1, 0, 0).endVertex();
            buffer.pos(x + 1, y, z).tex(u0, u0).color(r, g, b, 1.0F).normal(1, 0, 0).endVertex();
        }
        if (te.shouldRenderFace(EnumFacing.WEST)) {
            buffer.pos(x, y, z).tex(u0, u0).color(r, g, b, 1.0F).normal(-1, 0, 0).endVertex();
            buffer.pos(x, y, z + 1).tex(u1, u0).color(r, g, b, 1.0F).normal(-1, 0, 0).endVertex();
            buffer.pos(x, y + 1, z + 1).tex(u1, u1).color(r, g, b, 1.0F).normal(-1, 0, 0).endVertex();
            buffer.pos(x, y + 1, z).tex(u0, u1).color(r, g, b, 1.0F).normal(-1, 0, 0).endVertex();
        }
        if (te.shouldRenderFace(EnumFacing.DOWN)) {
            buffer.pos(x, y, z).tex(u0, u0).color(r, g, b, 1.0F).normal(0, -1, 0).endVertex();
            buffer.pos(x + 1, y, z).tex(u1, u0).color(r, g, b, 1.0F).normal(0, -1, 0).endVertex();
            buffer.pos(x + 1, y, z + 1).tex(u1, u1).color(r, g, b, 1.0F).normal(0, -1, 0).endVertex();
            buffer.pos(x, y, z + 1).tex(u0, u1).color(r, g, b, 1.0F).normal(0, -1, 0).endVertex();
        }
        if (te.shouldRenderFace(EnumFacing.UP)) {
            buffer.pos(x, top, z + 1).tex(u0, u1).color(r, g, b, 1.0F).normal(0, 1, 0).endVertex();
            buffer.pos(x + 1, top, z + 1).tex(u1, u1).color(r, g, b, 1.0F).normal(0, 1, 0).endVertex();
            buffer.pos(x + 1, top, z).tex(u1, u0).color(r, g, b, 1.0F).normal(0, 1, 0).endVertex();
            buffer.pos(x, top, z).tex(u0, u0).color(r, g, b, 1.0F).normal(0, 1, 0).endVertex();
        }

        tessellator.draw();
    }
}
