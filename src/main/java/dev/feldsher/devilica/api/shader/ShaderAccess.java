/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.shader;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * The shader pack, for mods drawing their own geometry in the world. With a pack, the programs bound while the world
 * draws are the pack's; geometry drawn with the game's client-side arrays (or {@code BufferBuilder}) goes through them
 * as is, the extra inputs a pack reads come from the generic attributes in {@link ShaderAttribute}, and textures bound
 * through {@code GlStateManager.bindTexture} bring their normal and specular maps along.
 */
public interface ShaderAccess {

    /** No ID: what the pack sees for things it has no ID for */
    int NO_ID = -1;

    /** Whether a shader pack draws the world now (not just installed: loaded and switched on) */
    boolean isShaderPackActive();

    /** Whether the pack's shadow pass is drawing: draws now go into the shadow map */
    boolean isShadowPass();

    /** The ID the pack gives a block state in its {@code block.properties}, {@link #NO_ID} when none */
    int blockId(IBlockState state);

    /** The ID the pack gives an entity in its {@code entity.properties}, {@link #NO_ID} when none */
    int entityId(ResourceLocation entity);

    /** The ID the pack gives an item in its {@code item.properties}, {@link #NO_ID} when none */
    int itemId(ItemStack stack);

    /**
     * What the pack sees as the block entity being drawn, for the draws until {@link #restoreBlockEntityId}.
     *
     * @return the ID to give back to {@link #restoreBlockEntityId}
     */
    int pushBlockEntityId(int id);

    void restoreBlockEntityId(int previous);

    /** What the pack sees as the block entity being drawn now. Since 1004. */
    int currentBlockEntityId();

    /** What the pack sees as the entity being drawn; returns the ID to give back to {@link #restoreEntityId}. */
    int pushEntityId(int id);

    void restoreEntityId(int previous);

    /** What the pack sees as the entity being drawn now. Since 1004. */
    int currentEntityId();

    /** What the pack sees as the item being drawn; returns the ID to give back to {@link #restoreItemId}. */
    int pushItemId(int id);

    void restoreItemId(int previous);

    /**
     * Switches to the pack's programs for entities or block entities, for draws made outside the pass the game draws
     * those in (or inside another one). Does nothing without a pack. Always close with {@link #endPhase}.
     *
     * @return what {@link #endPhase} restores
     */
    Object beginPhase(ShaderPhase phase);

    void endPhase(Object previous);

    /**
     * After draws with a program of your own while the world draws: binds the pack's program for what is being drawn
     * again, with its framebuffer and its blend and alpha settings. Does nothing without a pack. Since 1004.
     */
    void rebindProgram();

    /**
     * Draws with programs of your own while the world draws (glows, effects): until {@link #endOwnPrograms}, the
     * pack's program is not bound again on blend or texture changes, its uniforms are not written into your programs,
     * and its blend and alpha locks are off. Nests. Since 1004.
     */
    void beginOwnPrograms();

    /** Binds the pack's program for what is being drawn again, as {@link #rebindProgram} does. Since 1004. */
    void endOwnPrograms();

    /** The generic vertex attribute a pack's program reads an extra input from */
    int attributeLocation(ShaderAttribute attribute);
}
