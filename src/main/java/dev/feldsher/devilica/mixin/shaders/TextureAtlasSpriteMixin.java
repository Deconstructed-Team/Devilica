/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import com.gtnewhorizons.angelica.mixins.interfaces.ISpriteExt;
import net.coderbot.iris.texture.pbr.PBRSpriteHolder;
import net.coderbot.iris.texture.pbr.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** A sprite's normal and specular counterparts, and its animation state for keeping them in step. */
@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin implements TextureAtlasSpriteExtension, ISpriteExt {

    @Shadow
    private AnimationMetadataSection animationMetadata;

    @Shadow
    protected int frameCounter;

    @Unique
    private PBRSpriteHolder devilica$pbrHolder;

    @Override
    public PBRSpriteHolder getPBRHolder() {
        return this.devilica$pbrHolder;
    }

    @Override
    public PBRSpriteHolder getOrCreatePBRHolder() {
        if (this.devilica$pbrHolder == null) {
            this.devilica$pbrHolder = new PBRSpriteHolder();
        }
        return this.devilica$pbrHolder;
    }

    @Override
    public boolean isAnimation() {
        return this.animationMetadata != null && this.animationMetadata.getFrameCount() > 1;
    }

    @Override
    public int getFrame() {
        return this.frameCounter;
    }

    @Override
    public void callUpload(int frameIndex) {
    }

    @Override
    public AnimationMetadataSection getMetadata() {
        return this.animationMetadata;
    }
}
