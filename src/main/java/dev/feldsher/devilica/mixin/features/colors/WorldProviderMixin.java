/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The fog colours of the packs: the overworld's by biome, the nether's and the end's. */
@Mixin({WorldProvider.class, WorldProviderHell.class, WorldProviderEnd.class})
public abstract class WorldProviderMixin {

    @ModifyReturnValue(method = "getFogColor", at = @At("RETURN"))
    private Vec3d devilica$fogColor(Vec3d fog) {
        final Object provider = this;
        if (provider instanceof WorldProviderHell) {
            return CustomColors.netherFog(fog);
        }
        if (provider instanceof WorldProviderEnd) {
            return CustomColors.endFog(fog);
        }
        return ((WorldProvider) provider).getDimensionType() == DimensionType.OVERWORLD ? CustomColors.overworldFog(fog) : fog;
    }
}
