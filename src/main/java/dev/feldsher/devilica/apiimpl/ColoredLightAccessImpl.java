/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.apiimpl;

import dev.feldsher.devilica.api.light.ColoredLightAccess;
import dev.feldsher.devilica.api.light.ColoredLightProvider;
import dev.feldsher.devilica.coloredlight.ColoredLight;
import dev.feldsher.devilica.coloredlight.LightColors;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.jetbrains.annotations.Nullable;

/** {@link ColoredLightAccess} over Devilica's colour table and provider. */
public final class ColoredLightAccessImpl implements ColoredLightAccess {

    @Override
    public boolean isEnabled() {
        return ColoredLight.isInUse();
    }

    @Override
    public void setBlockColor(Block block, int rgb) {
        LightColors.register(block, rgb);
        ColoredLight.rebuildChunks();
    }

    @Override
    public void setBlockColor(IBlockState state, int rgb) {
        LightColors.register(state, rgb);
        ColoredLight.rebuildChunks();
    }

    @Override
    public int getBlockColor(IBlockState state) {
        return LightColors.colorOf(state);
    }

    @Override
    public void setProvider(@Nullable ColoredLightProvider provider) {
        ColoredLight.setProvider(provider);
    }
}
