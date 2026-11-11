/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.client;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.particle.Particle;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** The share of each particle class that spawns; a class seen for the first time is listed at 100%. */
public final class ParticleShares {
    private static boolean listed;

    private ParticleShares() {
    }

    public static boolean spawns(Particle particle, DevilicaOptions.AnimationSettings settings) {
        final Map<String, Integer> classes = settings.particleClasses;
        final String name = particle.getClass().getName();
        final Integer percent = classes.get(name);
        if (percent == null) {
            classes.put(name, 100);
            listed = true;
            return true;
        }
        return percent >= 100 || percent > 0 && ThreadLocalRandom.current().nextInt(100) < percent;
    }

    /** Writes the classes found since the last time to the settings file, so the settings list them next time too */
    public static void saveListed() {
        if (listed) {
            listed = false;
            DevilicaOptions.get().save();
        }
    }
}
