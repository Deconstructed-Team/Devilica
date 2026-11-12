/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.client.ParticleShares;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleBubble;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.client.particle.ParticleExplosion;
import net.minecraft.client.particle.ParticleExplosionHuge;
import net.minecraft.client.particle.ParticleExplosionLarge;
import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticlePortal;
import net.minecraft.client.particle.ParticleRain;
import net.minecraft.client.particle.ParticleRedstone;
import net.minecraft.client.particle.ParticleSmokeLarge;
import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.client.particle.ParticleSpell;
import net.minecraft.client.particle.ParticleSplash;
import net.minecraft.client.particle.ParticleSuspendedTown;
import net.minecraft.client.particle.ParticleWaterWake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particles the animation settings switch off never join the world: all of them, kinds of them, or a share of each
 * class (classes are listed in the settings as they first spawn).
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    private void devilica$particleSetting(Particle particle, CallbackInfo ci) {
        if (particle == null) {
            // Forge drops these itself, right after this hook
            return;
        }
        final DevilicaOptions.AnimationSettings settings = DevilicaOptions.get().animations;
        if (!settings.allParticles || !devilica$allowed(particle, settings) || !ParticleShares.spawns(particle, settings)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean devilica$allowed(Particle particle, DevilicaOptions.AnimationSettings settings) {
        if (particle instanceof ParticleSplash) {
            // A splash is a rain drop's kind of particle, but it is water too
            return settings.waterParticles;
        }
        if (particle instanceof ParticleRain) {
            return settings.rainSplash;
        }
        if (particle instanceof ParticleDigging) {
            return settings.blockBreaking;
        }
        if (particle instanceof ParticleExplosion || particle instanceof ParticleExplosionLarge || particle instanceof ParticleExplosionHuge) {
            return settings.explosions;
        }
        if (particle instanceof ParticleSpell) {
            return settings.potions;
        }
        if (particle instanceof ParticleDrip) {
            return settings.drips;
        }
        if (particle instanceof ParticleSmokeNormal || particle instanceof ParticleSmokeLarge) {
            return settings.smoke;
        }
        if (particle instanceof ParticleFlame) {
            return settings.flames;
        }
        if (particle instanceof ParticlePortal) {
            return settings.portalParticles;
        }
        if (particle instanceof ParticleBubble || particle instanceof ParticleWaterWake) {
            return settings.waterParticles;
        }
        if (particle instanceof ParticleRedstone) {
            return settings.redstone;
        }
        if (particle instanceof ParticleSuspendedTown) {
            return settings.voidParticles;
        }
        return true;
    }
}
