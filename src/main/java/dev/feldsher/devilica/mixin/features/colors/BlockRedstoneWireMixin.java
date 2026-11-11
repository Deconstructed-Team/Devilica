/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

/** Redstone dust's particles in the colour of {@code redstone.png} for the wire's power. */
@Mixin(BlockRedstoneWire.class)
public abstract class BlockRedstoneWireMixin {

    @WrapOperation(method = "randomDisplayTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V"))
    private void devilica$particleColor(World world, EnumParticleTypes type, double x, double y, double z, double red, double green, double blue,
                                        int[] parameters, Operation<Void> original, IBlockState state, World sameWorld, BlockPos pos, Random random) {
        final float[] color = CustomColors.redstoneParticle(state.getValue(BlockRedstoneWire.POWER));
        if (color == null) {
            original.call(world, type, x, y, z, red, green, blue, parameters);
        } else {
            original.call(world, type, x, y, z, (double) color[0], (double) color[1], (double) color[2], parameters);
        }
    }
}
