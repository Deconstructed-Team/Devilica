/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The packs' lightmaps, and their fog colours under water and lava. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    @Final
    private int[] lightmapColors;

    @Shadow
    private float torchFlickerX;

    @Shadow
    private float getNightVisionBrightness(EntityLivingBase entity, float partialTicks) {
        throw new AssertionError();
    }

    @Inject(method = "updateLightmap", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V"))
    private void devilica$customLightmap(float partialTicks, CallbackInfo ci) {
        final EntityPlayerSP player = this.mc.player;
        final float nightVision = player != null && player.isPotionActive(MobEffects.NIGHT_VISION) ? this.getNightVisionBrightness(player, partialTicks) : 0.0F;
        CustomColors.lightmap(this.mc.world, this.lightmapColors, this.torchFlickerX, nightVision);
    }

    @WrapOperation(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getFogColor(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;F)Lnet/minecraft/util/math/Vec3d;", remap = false))
    private Vec3d devilica$inMaterialFog(Block block, World world, BlockPos pos, IBlockState state, Entity entity, Vec3d fog, float partialTicks, Operation<Vec3d> original) {
        return CustomColors.inMaterialFog(world, pos, state, original.call(block, world, pos, state, entity, fog, partialTicks));
    }
}
