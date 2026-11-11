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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A block's model as drawn at one position: its quads come back with its textures changed by the settings. */
final class CtmModel implements IBakedModel {

    private final IBakedModel model;
    private final IBlockAccess world;
    private final BlockPos pos;
    final ConnectedTextures.Index index;
    final boolean ctm;
    final boolean natural;
    final BetterGrassMode betterGrass;
    final boolean emissive;
    /** The untinted quads take the pack's colour map of the block */
    final boolean tint;

    private final IBlockState state;
    private CtmContext context;

    CtmModel(IBakedModel model, IBlockAccess world, BlockPos pos, IBlockState state, ConnectedTextures.Index index, boolean ctm,
             boolean natural, BetterGrassMode betterGrass, boolean emissive, boolean tint) {
        this.model = model;
        this.world = world;
        this.pos = pos;
        this.state = state;
        this.index = index;
        this.ctm = ctm;
        this.natural = natural;
        this.betterGrass = betterGrass;
        this.emissive = emissive;
        this.tint = tint;
    }

    IBlockAccess world() {
        return this.world;
    }

    BlockPos pos() {
        return this.pos;
    }

    /** The plain state of the block drawn (the model gets the extended one); not always the world's, as with better snow */
    IBlockState state() {
        return this.state;
    }

    CtmContext context() {
        if (this.context == null) {
            this.context = new CtmContext();
            this.context.setBlock(this.world, this.pos, state());
        }
        return this.context;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return ConnectedTextures.process(this.model.getQuads(state, side, rand), this);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.model.isAmbientOcclusion();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return this.model.isAmbientOcclusion(state);
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
