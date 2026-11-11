/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import dev.feldsher.devilica.Devilica;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OptiFine's natural textures: natural blocks (grass, dirt, sand...) turn and mirror their texture by position so a
 * field of them shows no grid. A resource pack's {@code optifine/natural.properties} lists them as
 * {@code texture=4F} (4 or 2 turns, F to mirror); without one, a default list is used.
 */
final class NaturalTextures {

    record Natural(int turns, boolean mirror) {
    }

    private static final String[] FILES = {"optifine/natural.properties", "mcpatcher/natural.properties"};
    private static final String[][] DEFAULTS = {
            {"grass_top", "4F"}, {"dirt", "4F"}, {"sand", "4F"}, {"red_sand", "4F"}, {"gravel", "2"}, {"snow", "4F"},
            {"clay", "4F"}, {"mycelium_top", "4F"}, {"dirt_podzol_top", "4F"}, {"soul_sand", "4F"}, {"netherrack", "4F"},
            {"end_stone", "4F"}, {"bedrock", "4F"}, {"ice", "4F"}, {"farmland_dry", "2"}, {"farmland_wet", "2"},
    };

    private static final Map<BakedQuad, BakedQuad[]> VARIANTS = new ConcurrentHashMap<>();

    private NaturalTextures() {
    }

    static Map<TextureAtlasSprite, Natural> load(IResourceManager resources, TextureMap map) {
        VARIANTS.clear();
        Properties properties = null;
        for (String file : FILES) {
            try (IResource resource = resources.getResource(new ResourceLocation(file));
                 Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                properties = new Properties();
                properties.load(reader);
                break;
            } catch (IOException ignored) {
                // not in any pack
            }
        }
        if (properties == null) {
            properties = new Properties();
            for (String[] entry : DEFAULTS) {
                properties.setProperty(entry[0], entry[1]);
            }
        }
        final TextureAtlasSprite missing = map.getMissingSprite();
        final Map<TextureAtlasSprite, Natural> result = new IdentityHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            final String value = properties.getProperty(name).trim().toUpperCase(Locale.ROOT);
            final int turns = value.startsWith("4") ? 4 : value.startsWith("2") ? 2 : 1;
            final boolean mirror = value.endsWith("F");
            if (turns == 1 && !mirror) {
                continue;
            }
            final TextureAtlasSprite sprite = map.getAtlasSprite(CtmProperties.spriteName("minecraft", name));
            if (sprite != missing) {
                result.put(sprite, new Natural(turns, mirror));
            }
        }
        if (result.size() != DEFAULTS.length) {
            Devilica.LOGGER.info("Natural textures: {} textures", result.size());
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    static BakedQuad apply(BakedQuad quad, Natural natural, BlockPos pos, int face) {
        final long hash = MathHelper.getPositionRandom(pos) * 31L + face;
        final int random = (int) (hash ^ (hash >>> 32));
        final int turn = natural.turns == 4 ? random & 3 : natural.turns == 2 ? (random & 1) * 2 : 0;
        final boolean mirror = natural.mirror && ((random >>> 4) & 1) != 0;
        if (turn == 0 && !mirror) {
            return quad;
        }
        final int variant = turn | (mirror ? 4 : 0);
        final BakedQuad[] variants = VARIANTS.computeIfAbsent(quad, q -> new BakedQuad[8]);
        BakedQuad result = variants[variant];
        if (result == null) {
            result = Quads.turn(quad, turn, mirror);
            variants[variant] = result;
        }
        return result;
    }
}
