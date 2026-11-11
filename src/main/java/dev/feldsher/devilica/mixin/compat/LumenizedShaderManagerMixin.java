/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.coderbot.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lumenized asks only OptiFine whether a shader pack is on. Under an Iris pack it now takes its OptiFine path too: the
 * glowing quads go to the cutout layer for the pack to light, and it draws no bloom of its own.
 */
@Pseudo
@Mixin(targets = "github.kasuminova.lumenized.client.ShaderManager", remap = false)
public abstract class LumenizedShaderManagerMixin {

    @ModifyReturnValue(method = "isOptifineShaderPackLoaded", at = @At("RETURN"))
    private static boolean devilica$irisPack(boolean optifine) {
        return optifine || Iris.getCurrentPack().isPresent();
    }
}
