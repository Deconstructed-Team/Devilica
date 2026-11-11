/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** An item's model as drawn, with the glowing copies of its quads after them. */
final class GlowingItemModel implements IBakedModel {

    private final IBakedModel model;

    GlowingItemModel(IBakedModel model) {
        this.model = model;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return EmissiveTextures.plusGlows(this.model.getQuads(state, side, rand));
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.model.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.model.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return this.model.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.model.getParticleTexture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.model.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.model.getOverrides();
    }
}
