/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.features;

import dev.feldsher.devilica.randomentities.RandomEntities;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A texture bound while an entity is drawn swaps for that entity's look of it. */
@Mixin(TextureManager.class)
public abstract class TextureManagerRandomMixin {

    @ModifyVariable(method = "bindTexture", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation devilica$randomLook(ResourceLocation texture) {
        return RandomEntities.choose(texture);
    }
}
