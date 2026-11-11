/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.Block;
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

/** A block's item model with the connected textures tile of a block that has no neighbours, as MCPatcher draws them. */
final class HeldCtmModel implements IBakedModel {

    private final IBakedModel model;
    private final ConnectedTextures.Index index;
    private final Block block;
    private final int meta;

    HeldCtmModel(IBakedModel model, ConnectedTextures.Index index, Block block, int meta) {
        this.model = model;
        this.index = index;
        this.block = block;
        this.meta = meta;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        final List<BakedQuad> quads = this.model.getQuads(state, side, rand);
        final TileOverride[] forBlock = this.index.byBlock().get(this.block);
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            final BakedQuad quad = quads.get(i);
            final BakedQuad held = held(quad, forBlock);
            if (held != quad) {
                if (result == null) {
                    result = new ArrayList<>(quads);
                }
                result.set(i, held);
            }
        }
        return result == null ? quads : result;
    }

    private BakedQuad held(BakedQuad quad, TileOverride[] forBlock) {
        final EnumFacing face = quad.getFace();
        if (face == null) {
            return quad;
        }
        final BakedQuad byBlock = held(quad, face.getIndex(), forBlock);
        return byBlock != quad ? byBlock : held(quad, face.getIndex(), this.index.bySprite().get(quad.getSprite()));
    }

    private BakedQuad held(BakedQuad quad, int face, TileOverride[] rules) {
        if (rules != null) {
            for (TileOverride rule : rules) {
                final int tile = rule.heldTileFor(face, this.block, this.meta);
                if (tile >= 0) {
                    return rule.retexture(quad, tile);
                }
            }
        }
        return quad;
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
