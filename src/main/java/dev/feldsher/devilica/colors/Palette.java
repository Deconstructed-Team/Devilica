/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.colors;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.potion.Potion;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Everything the resource packs' custom colours set, read once a reload and then only read. -1 is "not set". */
final class Palette {

    static final Palette EMPTY = new Palette();

    /** Block colours by state, over the blocks' own */
    final Reference2ObjectOpenHashMap<IBlockState, IBlockColor> blocks = new Reference2ObjectOpenHashMap<>();
    /** Colours for the untinted faces of blocks with a palette colour map */
    final Reference2ObjectOpenHashMap<IBlockState, IBlockColor> untinted = new Reference2ObjectOpenHashMap<>();

    Colormap water;
    Colormap swampGrass;
    Colormap swampFoliage;
    Colormap fog;
    Colormap sky;
    Colormap underwater;
    Colormap underlava;

    Vec3d netherFog;
    Vec3d endFog;
    int endSky = -1;
    /** 0 none, 1 fast, 2 fancy */
    int clouds = -1;

    final int[] textCodes = new int[32];
    final Int2IntOpenHashMap text = new Int2IntOpenHashMap();
    int xpBar = -1;
    int boss = -1;
    int sign = -1;

    final Reference2IntOpenHashMap<Potion> potions = new Reference2IntOpenHashMap<>();
    int waterBottle = -1;

    int waterParticles = -1;
    int portalParticles = -1;
    Colormap redstone;
    Colormap mycelium;
    Colormap xpOrb;
    Colormap durability;

    /** By dye metadata */
    final int[] sheep = new int[16];
    final int[] collar = new int[16];
    final int[] dye = new int[16];
    /** By entity name, lower case without underscores */
    final Map<String, Integer> eggShell = new HashMap<>();
    final Map<String, Integer> eggSpots = new HashMap<>();
    /** By map colour index */
    final int[] mapColors = new int[64];

    final Int2ObjectOpenHashMap<Lightmap> lightmaps = new Int2ObjectOpenHashMap<>();

    /** How many colour maps and colours the packs set, for the log and F3 */
    int count;

    Palette() {
        Arrays.fill(this.textCodes, -1);
        Arrays.fill(this.sheep, -1);
        Arrays.fill(this.collar, -1);
        Arrays.fill(this.dye, -1);
        Arrays.fill(this.mapColors, -1);
        this.text.defaultReturnValue(-1);
        this.potions.defaultReturnValue(-1);
    }
}
