/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.cit;

import dev.feldsher.devilica.ctm.Quads;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An item model with a custom texture: quads built from the rule's image, or the original quads with some of their
 * sprites swapped. Where the item is placed (hand, GUI) still comes from the original model.
 */
final class CitModel implements IBakedModel {

    /** Quads with a sprite swapped, by the quad and its new sprite */
    private static final Map<List<Object>, BakedQuad> SWAPPED = new ConcurrentHashMap<>();

    static void clearCache() {
        SWAPPED.clear();
    }

    private final IBakedModel model;
    private final List<BakedQuad> quads;
    private final Map<TextureAtlasSprite, TextureAtlasSprite> swaps;

    CitModel(IBakedModel model, @Nullable List<BakedQuad> quads, @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps) {
        this.model = model;
        this.quads = quads;
        this.swaps = swaps;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (this.quads != null) {
            return side == null ? this.quads : List.of();
        }
        final List<BakedQuad> original = this.model.getQuads(state, side, rand);
        List<BakedQuad> result = null;
        for (int i = 0; i < original.size(); i++) {
            final BakedQuad quad = original.get(i);
            final TextureAtlasSprite replacement = this.swaps.get(quad.getSprite());
            if (replacement != null) {
                if (result == null) {
                    result = new ArrayList<>(original);
                }
                result.set(i, SWAPPED.computeIfAbsent(List.of(quad, replacement), key -> Quads.retexture(quad, replacement, quad.getTintIndex())));
            }
        }
        return result == null ? original : result;
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
        return false;
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
