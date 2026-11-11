/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.light;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Coloured block light, drawn without a shader pack: sources give their light a colour, which tints the terrain they
 * light while the brightness stays the game's. Colours are {@code 0xRRGGBB}; {@code 0xFFFFFF} is the game's own light.
 * The player's {@code coloredLight.blockColors} table in {@code devilica-options.json} wins over what is set here.
 * Since 1003.
 */
public interface ColoredLightAccess {

    /** Whether coloured light is drawn: switched on, and no shader pack in use */
    boolean isEnabled();

    /** The colour every state of the block gives its light */
    void setBlockColor(Block block, int rgb);

    /** The colour one state gives its light */
    void setBlockColor(IBlockState state, int rgb);

    /** The colour a state gives its light, {@code 0xFFFFFF} for the game's own */
    int getBlockColor(IBlockState state);

    /** A light engine's own coloured light, used instead of Devilica's; null goes back to Devilica's */
    void setProvider(@Nullable ColoredLightProvider provider);
}
