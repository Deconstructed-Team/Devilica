/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.apiimpl;

import dev.feldsher.devilica.api.shader.ShaderAccess;
import dev.feldsher.devilica.api.shader.ShaderAttribute;
import dev.feldsher.devilica.api.shader.ShaderPhase;
import dev.feldsher.devilica.shaders.ForeignPrograms;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.coderbot.iris.Iris;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.gl.shader.ProgramCreator;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.ItemMaterialHelper;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** {@link ShaderAccess} over Iris. */
public final class ShaderAccessImpl implements ShaderAccess {

    @Override
    public boolean isShaderPackActive() {
        return Iris.enabled && IrisApi.getInstance().isShaderPackInUse();
    }

    @Override
    public boolean isShadowPass() {
        return Iris.enabled && ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }

    @Override
    public int blockId(IBlockState state) {
        final Reference2ObjectMap<Block, Int2IntMap> ids = Iris.enabled ? BlockRenderingSettings.INSTANCE.getBlockMetaMatches() : null;
        if (ids == null) {
            return NO_ID;
        }
        final Int2IntMap byMeta = ids.get(state.getBlock());
        return byMeta == null ? NO_ID : byMeta.get(state.getBlock().getMetaFromState(state));
    }

    @Override
    public int entityId(ResourceLocation entity) {
        final Object2IntFunction<NamespacedId> ids = Iris.enabled ? BlockRenderingSettings.INSTANCE.getEntityIds() : null;
        if (ids == null) {
            return NO_ID;
        }
        final int id = ids.applyAsInt(new NamespacedId(entity.getNamespace(), entity.getPath()));
        return id == Integer.MIN_VALUE ? NO_ID : id;
    }

    @Override
    public int itemId(ItemStack stack) {
        return Iris.enabled ? ItemMaterialHelper.getMaterialId(stack) : NO_ID;
    }

    @Override
    public int pushBlockEntityId(int id) {
        final int previous = CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(id);
        return previous;
    }

    @Override
    public void restoreBlockEntityId(int previous) {
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(previous);
    }

    @Override
    public int currentBlockEntityId() {
        return CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
    }

    @Override
    public int pushEntityId(int id) {
        final int previous = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        CapturedRenderingState.INSTANCE.setCurrentEntity(id);
        return previous;
    }

    @Override
    public void restoreEntityId(int previous) {
        CapturedRenderingState.INSTANCE.setCurrentEntity(previous);
    }

    @Override
    public int currentEntityId() {
        return CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
    }

    @Override
    public int pushItemId(int id) {
        final int previous = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(id);
        return previous;
    }

    @Override
    public void restoreItemId(int previous) {
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(previous);
    }

    @Override
    public Object beginPhase(ShaderPhase phase) {
        final WorldRenderingPipeline pipeline = isShaderPackActive() ? Iris.getPipelineManager().getPipelineNullable() : null;
        if (pipeline == null) {
            return null;
        }
        final WorldRenderingPhase previous = pipeline.getPhase();
        pipeline.setPhase(phase == ShaderPhase.ENTITIES ? WorldRenderingPhase.ENTITIES : WorldRenderingPhase.BLOCK_ENTITIES);
        GbufferPrograms.setBlockEntityDefaults();
        return previous;
    }

    @Override
    public void endPhase(Object previous) {
        final WorldRenderingPipeline pipeline = Iris.enabled ? Iris.getPipelineManager().getPipelineNullable() : null;
        if (pipeline != null && previous instanceof WorldRenderingPhase phase) {
            pipeline.setPhase(phase);
        }
    }

    @Override
    public void rebindProgram() {
        final WorldRenderingPipeline pipeline = isShaderPackActive() ? Iris.getPipelineManager().getPipelineNullable() : null;
        if (pipeline != null) {
            pipeline.rebindPass();
        }
    }

    @Override
    public void beginOwnPrograms() {
        ForeignPrograms.begin();
    }

    @Override
    public void endOwnPrograms() {
        ForeignPrograms.end();
    }

    @Override
    public int attributeLocation(ShaderAttribute attribute) {
        return switch (attribute) {
            case ENTITY -> ProgramCreator.MC_ENTITY;
            case MID_TEX_COORD -> ProgramCreator.MC_MID_TEX_COORD;
            case TANGENT -> ProgramCreator.AT_TANGENT;
            case MID_BLOCK -> ProgramCreator.AT_MIDBLOCK;
        };
    }
}
