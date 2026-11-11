/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features.colors;

import dev.feldsher.devilica.colors.CustomColors;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleSuspendedTown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mycelium's particles in the colours of {@code myceliumparticle.png}. */
@Mixin(ParticleSuspendedTown.class)
public abstract class ParticleSuspendedTownMixin extends Particle {

    protected ParticleSuspendedTownMixin() {
        super(null, 0.0, 0.0, 0.0);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void devilica$color(CallbackInfo ci) {
        CustomColors.myceliumParticle(this, this.rand);
    }
}
