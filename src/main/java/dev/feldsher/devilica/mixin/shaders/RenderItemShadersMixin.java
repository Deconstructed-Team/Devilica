/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import dev.feldsher.devilica.shaders.ShaderHooks;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The enchantment glint on items, held, dropped or in frames, draws with the pack's glint program. */
@Mixin(RenderItem.class)
public abstract class RenderItemShadersMixin {

    @Inject(method = "renderEffect", at = @At("HEAD"))
    private void devilica$beginGlint(IBakedModel model, CallbackInfo ci) {
        ShaderHooks.beginGlint();
    }

    @Inject(method = "renderEffect", at = @At("RETURN"))
    private void devilica$endGlint(IBakedModel model, CallbackInfo ci) {
        ShaderHooks.endCondition();
    }
}
