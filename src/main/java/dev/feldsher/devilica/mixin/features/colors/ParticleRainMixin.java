/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rain drops and splashes in the packs' water colour. */
@Mixin(ParticleRain.class)
public abstract class ParticleRainMixin extends Particle {

    protected ParticleRainMixin() {
        super(null, 0.0, 0.0, 0.0);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void devilica$color(CallbackInfo ci) {
        CustomColors.waterParticle(this, this.world, this.posX, this.posY, this.posZ);
    }
}
