/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.coloredlight;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.config.DevilicaOptions;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The colour each light source gives, as {@code 0xRRGGBB}. Sources not listed give vanilla's light, which is already
 * warm; only the ones with a light of their own (redstone, sea lanterns, portals...) are listed. From the
 * {@code coloredLight.blockColors} table of the options, then what mods register; the table wins.
 */
public final class LightColors {

    public static final int PLAIN = 0xFFFFFF;

    private static final Map<Block, Integer> registeredBlocks = new ConcurrentHashMap<>();
    private static final Map<IBlockState, Integer> registeredStates = new ConcurrentHashMap<>();

    private static volatile Snapshot snapshot;
    private static Map<String, String> builtFrom;

    private LightColors() {
    }

    /** The colour of the light the state gives, or {@link #PLAIN} */
    public static int colorOf(IBlockState state) {
        return current().colors.getInt(state);
    }

    /** Whether any state of the block gives coloured light */
    public static boolean hasColoredStates(Block block) {
        return current().blocks.contains(block);
    }

    public static void register(Block block, int rgb) {
        registeredBlocks.put(block, rgb & PLAIN);
        snapshot = null;
    }

    public static void register(IBlockState state, int rgb) {
        registeredStates.put(state, rgb & PLAIN);
        snapshot = null;
    }

    private static Snapshot current() {
        final Snapshot current = snapshot;
        final Map<String, String> table = DevilicaOptions.get().coloredLight.blockColors;
        if (current != null && table == builtFrom) {
            return current;
        }
        synchronized (LightColors.class) {
            if (snapshot == null || table != builtFrom) {
                snapshot = build(table);
                builtFrom = table;
            }
            return snapshot;
        }
    }

    private static Snapshot build(Map<String, String> table) {
        final Reference2IntOpenHashMap<IBlockState> colors = new Reference2IntOpenHashMap<>();
        colors.defaultReturnValue(PLAIN);
        registeredBlocks.forEach((block, rgb) -> block.getBlockState().getValidStates().forEach(state -> colors.put(state, rgb.intValue())));
        registeredStates.forEach((state, rgb) -> colors.put(state, rgb.intValue()));
        for (Map.Entry<String, String> entry : table.entrySet()) {
            final int rgb = parseColor(entry.getValue());
            if (rgb < 0 || !putStates(colors, entry.getKey(), rgb)) {
                Devilica.LOGGER.warn("Coloured light: skipping {} = {}", entry.getKey(), entry.getValue());
            }
        }
        colors.values().removeIf(rgb -> rgb == PLAIN);

        final ReferenceSet<Block> blocks = new ReferenceOpenHashSet<>();
        for (IBlockState state : colors.keySet()) {
            blocks.add(state.getBlock());
        }
        return new Snapshot(colors, blocks);
    }

    /** {@code modid:block} for every state, or {@code modid:block[property=value,...]} for the matching ones */
    private static boolean putStates(Reference2IntMap<IBlockState> colors, String key, int rgb) {
        final int bracket = key.indexOf('[');
        final ResourceLocation id = new ResourceLocation(bracket < 0 ? key : key.substring(0, bracket));
        if (!Block.REGISTRY.containsKey(id)) {
            return false;
        }
        final Block block = Block.REGISTRY.getObject(id);
        final Map<String, String> wanted = new HashMap<>();
        if (bracket >= 0) {
            if (!key.endsWith("]")) {
                return false;
            }
            for (String pair : key.substring(bracket + 1, key.length() - 1).split(",")) {
                final int eq = pair.indexOf('=');
                if (eq <= 0) {
                    return false;
                }
                wanted.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        for (IBlockState state : block.getBlockState().getValidStates()) {
            if (matches(state, wanted)) {
                colors.put(state, rgb);
            }
        }
        return true;
    }

    private static boolean matches(IBlockState state, Map<String, String> wanted) {
        for (Map.Entry<String, String> entry : wanted.entrySet()) {
            boolean found = false;
            for (Map.Entry<IProperty<?>, Comparable<?>> property : state.getProperties().entrySet()) {
                if (property.getKey().getName().equals(entry.getKey())) {
                    found = name(property.getKey(), property.getValue()).equals(entry.getValue());
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String name(IProperty<T> property, Comparable<?> value) {
        return property.getName((T) value);
    }

    /** {@code #rrggbb} or {@code rrggbb}; -1 when it is neither */
    static int parseColor(String text) {
        if (text == null) {
            return -1;
        }
        final String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private record Snapshot(Reference2IntOpenHashMap<IBlockState> colors, ReferenceSet<Block> blocks) {
    }
}
