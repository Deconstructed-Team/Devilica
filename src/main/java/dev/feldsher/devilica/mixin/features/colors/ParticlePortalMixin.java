/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticlePortal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Portal particles in the packs' colour ({@code particle.portal}); vanilla's blue is each one's brightness. */
@Mixin(ParticlePortal.class)
public abstract class ParticlePortalMixin extends Particle {

    protected ParticlePortalMixin() {
        super(null, 0.0, 0.0, 0.0);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void devilica$color(CallbackInfo ci) {
        CustomColors.portalParticle(this, this.particleBlue);
    }
}
