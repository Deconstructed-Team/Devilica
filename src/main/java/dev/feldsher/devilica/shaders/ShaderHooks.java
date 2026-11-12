/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.shaders;

import com.gtnewhorizons.angelica.compat.mojang.Camera;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.coderbot.iris.Iris;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.EntityIdHelper;
import net.coderbot.iris.uniforms.ItemIdManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;

/**
 * Where Celeritas's 1.12.2 renderer tells Iris what it is drawing: terrain layers, entities and block entities. The
 * calls do nothing while no shader pack is active, and nothing while the shadow pass draws, which sets its own state.
 */
public final class ShaderHooks {

    private ShaderHooks() {
    }

    private static WorldRenderingPipeline pipeline() {
        return Iris.enabled && !ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? Iris.getPipelineManager().getPipelineNullable() : null;
    }

    /** The pack turned off vanilla's underwater overlay ({@code underwaterOverlay=false}) */
    public static boolean hidesUnderwaterOverlay() {
        final WorldRenderingPipeline pipeline = pipeline();
        return pipeline != null && !pipeline.shouldRenderUnderwaterOverlay();
    }

    /** The pack turned off vanilla's vignette ({@code vignette=false}) */
    public static boolean hidesVignette() {
        final WorldRenderingPipeline pipeline = pipeline();
        return pipeline != null && !pipeline.shouldRenderVignette();
    }

    /** Before a terrain layer draws. The translucent layer first draws the solid hand, as Iris does. */
    public static void beginTerrainLayer(BlockRenderLayer layer) {
        final WorldRenderingPipeline pipeline = pipeline();
        if (pipeline == null) {
            return;
        }
        switch (layer) {
            case SOLID -> pipeline.setPhase(WorldRenderingPhase.TERRAIN_SOLID);
            case CUTOUT_MIPPED -> pipeline.setPhase(WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED);
            case CUTOUT -> pipeline.setPhase(WorldRenderingPhase.TERRAIN_CUTOUT);
            case TRANSLUCENT -> {
                pipeline.beginHand();
                HandRenderer.INSTANCE.renderSolid(Camera.INSTANCE.getPartialTicks(), Camera.INSTANCE, Minecraft.getMinecraft().renderGlobal, pipeline);
                pipeline.beginTranslucents();
                pipeline.setPhase(WorldRenderingPhase.TERRAIN_TRANSLUCENT);
            }
        }
    }

    public static void endTerrainLayer() {
        final WorldRenderingPipeline pipeline = pipeline();
        if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.NONE);
        }
    }

    /**
     * Around the world's entity loop: the entity program stays bound for every entity, which only changes the entity
     * ID ({@code RenderManagerMixin}). Switching the program per entity cost about 30 µs each.
     */
    /**
     * The game can survive a crash in a world. A frame that threw midway left its phase flags, the pack's pass and its
     * program in use: the next world frame threw on the flags, in every world joined after. Called when a world frame
     * starts and when the game leaves a world.
     */
    public static void abandonFrame() {
        if (!Iris.enabled) {
            return;
        }
        GbufferPrograms.abandon();
        HandRenderer.INSTANCE.abandon();
        RenderScale.abandon();
        final WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            pipeline.abandonFrame();
        }
    }

    public static void beginEntities() {
        if (Iris.enabled) {
            GbufferPrograms.beginEntities();
        }
    }

    public static void endEntities() {
        if (Iris.enabled) {
            GbufferPrograms.endEntities();
        }
    }

    public static void beginEntity(Entity entity) {
        if (Iris.enabled) {
            CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
            GbufferPrograms.beginEntities();
        }
    }

    public static void endEntity() {
        if (Iris.enabled) {
            CapturedRenderingState.INSTANCE.setCurrentEntity(-1);
            GbufferPrograms.endEntities();
        }
    }

    /** The block entity ID shader packs see while it draws: its block's ID from block.properties, 0 for none */
    public static int blockEntityId(TileEntity tile) {
        final Reference2ObjectMap<Block, Int2IntMap> matches = BlockRenderingSettings.INSTANCE.getBlockMetaMatches();
        final Block block = matches != null ? tile.getBlockType() : null;
        final Int2IntMap metaMap = block != null ? matches.get(block) : null;
        return metaMap != null ? Math.max(0, metaMap.get(tile.getBlockMetadata())) : 0;
    }

    public static void beginBlockEntities() {
        if (Iris.enabled) {
            GbufferPrograms.beginBlockEntities();
            GbufferPrograms.setBlockEntityDefaults();
        }
    }

    public static void endBlockEntities() {
        if (Iris.enabled) {
            GbufferPrograms.endBlockEntities();
        }
    }

    /**
     * Shader packs address some effects as entities of their own (flames, name tags, crystal beams).
     *
     * @return the entity ID to give back to {@link #restoreEntityId}, or {@link #NO_ID} when nothing changed
     */
    public static int setSpecialEntityId(NamespacedId id) {
        final Object2IntFunction<NamespacedId> ids = Iris.enabled ? BlockRenderingSettings.INSTANCE.getEntityIds() : null;
        if (ids == null) {
            return NO_ID;
        }
        final int previous = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        CapturedRenderingState.INSTANCE.setCurrentEntity(ids.applyAsInt(id));
        return previous;
    }

    /**
     * As {@link #setSpecialEntityId}, for an effect that drew under its entity's ID before packs could name it: only a
     * pack that names it sees it, for the others nothing changes.
     */
    public static int setNamedEntityId(NamespacedId id) {
        final Object2IntFunction<NamespacedId> ids = Iris.enabled ? BlockRenderingSettings.INSTANCE.getEntityIds() : null;
        return ids != null && ids.containsKey(id) ? setSpecialEntityId(id) : NO_ID;
    }

    public static void restoreEntityId(int previous) {
        if (previous != NO_ID) {
            CapturedRenderingState.INSTANCE.setCurrentEntity(previous);
        }
    }

    public static final int NO_ID = Integer.MIN_VALUE;

    /** An item ID that is not an item (a player's cape). */
    public static void setSpecialItemId(NamespacedId id) {
        final Object2IntFunction<NamespacedId> ids = Iris.enabled ? BlockRenderingSettings.INSTANCE.getItemIds() : null;
        if (ids != null) {
            CapturedRenderingState.INSTANCE.setCurrentRenderedItem(ids.applyAsInt(id));
        }
    }

    public static void setItemId(ItemStack stack) {
        if (Iris.enabled) {
            ItemIdManager.setItemId(stack);
        }
    }

    public static void resetItemId() {
        if (Iris.enabled) {
            ItemIdManager.resetItemId();
        }
    }

    public static void beginCondition(SpecialCondition condition) {
        if (Iris.enabled) {
            GbufferPrograms.setupSpecialRenderCondition(condition);
        }
    }

    public static void endCondition() {
        if (Iris.enabled) {
            GbufferPrograms.teardownSpecialRenderCondition();
        }
    }

    /** The enchantment glint is drawn without the item's ID, so packs treat it as glint and not as the item. */
    public static void beginGlint() {
        if (Iris.enabled) {
            ItemIdManager.resetItemId();
            GbufferPrograms.setupSpecialRenderCondition(SpecialCondition.GLINT);
        }
    }
}
