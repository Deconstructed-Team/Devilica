/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A block, and optionally which of its states, as OptiFine reads block lists on 1.12: {@code stone},
 * {@code minecraft:stained_glass:14}, {@code stained_glass:color=red,blue}, or a numeric ID.
 */
public final class BlockMatcher {

    public final Block block;
    /** Metadata values that match, or null for all */
    private final BitSet metas;

    private BlockMatcher(Block block, BitSet metas) {
        this.block = block;
        this.metas = metas;
    }

    public boolean matches(Block block, int meta) {
        return this.block == block && (this.metas == null || this.metas.get(meta));
    }

    public static List<BlockMatcher> parseList(String list, String defaultMetadata) {
        final List<BlockMatcher> matchers = new ArrayList<>();
        for (String token : list.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            final BlockMatcher matcher = parse(defaultMetadata.isEmpty() || token.split(":").length > 2 ? token : token + ":" + defaultMetadata);
            if (matcher != null) {
                matchers.add(matcher);
            }
        }
        return matchers;
    }

    private static BlockMatcher parse(String token) {
        final String[] parts = token.split(":");
        final Block block;
        final int statesStart;
        if (StringUtils.isNumeric(parts[0])) {
            block = Block.getBlockById(Integer.parseInt(parts[0]));
            statesStart = 1;
        } else if (parts.length > 1 && !isState(parts[1])) {
            block = Block.REGISTRY.getObject(new ResourceLocation(parts[0], parts[1]));
            statesStart = 2;
        } else {
            block = Block.REGISTRY.getObject(new ResourceLocation("minecraft", parts[0]));
            statesStart = 1;
        }
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        if (statesStart >= parts.length) {
            return new BlockMatcher(block, null);
        }

        final BitSet metas = new BitSet(16);
        final Map<String, Set<String>> properties = new HashMap<>();
        for (int i = statesStart; i < parts.length; i++) {
            final int equals = parts[i].indexOf('=');
            if (equals > 0) {
                properties.computeIfAbsent(parts[i].substring(0, equals), k -> new HashSet<>())
                        .addAll(Arrays.asList(parts[i].substring(equals + 1).split(",")));
            } else {
                for (String range : parts[i].split(",")) {
                    final String[] bounds = range.split("-");
                    try {
                        final int from = Integer.parseInt(bounds[0]);
                        final int to = bounds.length > 1 ? Integer.parseInt(bounds[1]) : from;
                        for (int meta = Math.max(0, from); meta <= Math.min(15, to); meta++) {
                            metas.set(meta);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        if (!properties.isEmpty()) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                if (matches(state, properties)) {
                    metas.set(block.getMetaFromState(state));
                }
            }
        }
        return new BlockMatcher(block, metas);
    }

    private static boolean isState(String part) {
        return !part.isEmpty() && (Character.isDigit(part.charAt(0)) || part.indexOf('=') > 0);
    }

    private static boolean matches(IBlockState state, Map<String, Set<String>> conditions) {
        for (Map.Entry<String, Set<String>> condition : conditions.entrySet()) {
            boolean found = false;
            for (IProperty<?> property : state.getPropertyKeys()) {
                if (property.getName().equals(condition.getKey())) {
                    found = condition.getValue().contains(valueName(state, property));
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> String valueName(IBlockState state, IProperty<T> property) {
        return property.getName(state.getValue(property));
    }
}
