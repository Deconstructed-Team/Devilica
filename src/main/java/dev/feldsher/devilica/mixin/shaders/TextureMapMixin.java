/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import net.coderbot.iris.texture.pbr.PBRAtlasHolder;
import net.coderbot.iris.texture.pbr.TextureAtlasExtension;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The block atlas's normal and specular atlases, animated along with it. */
@Mixin(TextureMap.class)
public abstract class TextureMapMixin extends AbstractTexture implements TextureAtlasExtension {

    @Unique
    private PBRAtlasHolder devilica$pbrHolder;

    @Inject(method = "updateAnimations", at = @At("TAIL"))
    private void devilica$animatePbrAtlases(CallbackInfo ci) {
        if (this.devilica$pbrHolder != null) {
            this.devilica$pbrHolder.cycleAnimationFrames();
        }
    }

    @Override
    public PBRAtlasHolder getPBRHolder() {
        return this.devilica$pbrHolder;
    }

    @Override
    public PBRAtlasHolder getOrCreatePBRHolder() {
        if (this.devilica$pbrHolder == null) {
            this.devilica$pbrHolder = new PBRAtlasHolder();
        }
        return this.devilica$pbrHolder;
    }
}
