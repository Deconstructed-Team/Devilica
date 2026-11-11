/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDrip;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Water drips in the packs' water colour; vanilla sets theirs each tick. */
@Mixin(ParticleDrip.class)
public abstract class ParticleDripMixin extends Particle {

    @Shadow
    @Final
    private Material materialType;

    protected ParticleDripMixin() {
        super(null, 0.0, 0.0, 0.0);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void devilica$color(CallbackInfo ci) {
        if (this.materialType == Material.WATER) {
            CustomColors.waterParticle(this, this.world, this.posX, this.posY, this.posZ);
        }
    }
}
