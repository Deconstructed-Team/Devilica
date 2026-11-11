/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** As many stars as the details settings say ({@link RenderGlobalAccessor} builds the sky's stars again on a change). */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalStarsMixin {

    @ModifyConstant(method = "renderStars", constant = @Constant(intValue = 1500))
    private int devilica$starCount(int count) {
        return DevilicaOptions.get().details.starCount;
    }
}
