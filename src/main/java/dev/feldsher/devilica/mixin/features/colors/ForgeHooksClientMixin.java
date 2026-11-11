/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.feldsher.devilica.colors.CustomColors;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The overworld's sky colour by biome from the packs' sky colour map, blended as Forge blends the biomes' own; without
 * biome sky colours, the plains' sky everywhere.
 */
@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class ForgeHooksClientMixin {

    @WrapOperation(method = "getSkyBlendColour", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getSkyColorByTemp(F)I", remap = true))
    private static int devilica$skyColor(Biome biome, float temperature, Operation<Integer> original, World world, BlockPos center) {
        if (!DevilicaOptions.get().details.biomeSkyColors) {
            biome = Biomes.PLAINS;
            temperature = Biomes.PLAINS.getDefaultTemperature();
        }
        final int color = CustomColors.skyColor(world, biome, temperature, center);
        return color == CustomColors.KEEP ? original.call(biome, temperature) : color;
    }
}
