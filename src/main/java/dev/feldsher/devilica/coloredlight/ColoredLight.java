/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.coloredlight;

import dev.feldsher.devilica.api.light.ColoredLightProvider;
import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.jetbrains.annotations.Nullable;

/**
 * Coloured block light without a shader pack. The colour is worked out per section on the builder threads
 * ({@link ColoredLightField}), goes into each vertex as a tint ({@link TintedChunkVertexType}), and the terrain shader
 * applies it to the block light part of the lightmap only, so daylight washes it out and night shows it.
 */
public final class ColoredLight {

    private static volatile boolean inUse;
    private static volatile ColoredLightProvider provider;

    private ColoredLight() {
    }

    /** The terrain's vertex type: the tinted one while coloured light is on and no shader pack has the terrain */
    public static ChunkVertexType vertexType(ChunkVertexType base, boolean shaders) {
        inUse = !shaders && DevilicaOptions.get().coloredLight.enabled;
        return inUse ? TintedChunkVertexType.of(base) : base;
    }

    /** Whether chunk builds work the colour out: the terrain's vertex type carries it */
    public static boolean isInUse() {
        return inUse;
    }

    @Nullable
    static ColoredLightProvider provider() {
        return provider;
    }

    public static void setProvider(@Nullable ColoredLightProvider newProvider) {
        provider = newProvider;
        rebuildChunks();
    }

    /** Rebuilds the terrain so a changed colour shows */
    public static void rebuildChunks() {
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (inUse && minecraft.renderGlobal != null) {
            minecraft.addScheduledTask(() -> minecraft.renderGlobal.loadRenderers());
        }
    }
}
