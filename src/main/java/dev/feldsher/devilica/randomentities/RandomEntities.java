/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.randomentities;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.resources.ResourcePackFiles;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OptiFine's random entities: resource packs give a mob more looks as {@code creeper2.png}, {@code creeper3.png}... in
 * {@code optifine/random/entity/} (or {@code optifine/mob/}, {@code mcpatcher/mob/}), and a properties file can tie
 * them to biomes, heights, names and more. Each mob keeps its look, picked from its UUID; every texture it binds
 * while it is drawn (its eyes, its saddle) swaps for the same look.
 */
public final class RandomEntities implements IResourceManagerReloadListener {

    public static final RandomEntities INSTANCE = new RandomEntities();

    private static final String[] DIRECTORIES = {"optifine/random/entity/", "optifine/mob/", "mcpatcher/mob/"};
    private static final Pattern VARIANT = Pattern.compile("(.*?)(\\d+)\\.png");

    /** Looks of each texture: index 1 is the texture itself */
    private record Variants(ResourceLocation[] textures, List<RandomRule> rules) {
    }

    private static volatile Map<ResourceLocation, Variants> variants = Collections.emptyMap();

    /** What is being drawn: an entity, or a block entity, innermost last */
    private static final ArrayDeque<Object> DRAWING = new ArrayDeque<>();

    private RandomEntities() {
    }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {
        final Map<ResourceLocation, TreeMap<Integer, ResourceLocation>> found = new HashMap<>();
        final Map<ResourceLocation, ResourceLocation> propertiesFiles = new HashMap<>();
        try {
            for (ResourceLocation file : ResourcePackFiles.find(".png", DIRECTORIES)) {
                final String relative = relative(file.getPath());
                final Matcher matcher = relative == null ? null : VARIANT.matcher(relative);
                if (matcher == null || !matcher.matches()) {
                    continue;
                }
                final int number = Integer.parseInt(matcher.group(2));
                if (number < 2) {
                    continue;
                }
                final ResourceLocation base = new ResourceLocation(file.getNamespace(), "textures/entity/" + matcher.group(1) + ".png");
                if (!exists(resources, base)) {
                    continue;
                }
                found.computeIfAbsent(base, b -> new TreeMap<>()).put(number, file);
            }
            for (ResourceLocation file : ResourcePackFiles.find(".properties", DIRECTORIES)) {
                final String relative = relative(file.getPath());
                if (relative != null) {
                    propertiesFiles.put(new ResourceLocation(file.getNamespace(), "textures/entity/" + relative.replaceFirst("\\.properties$", ".png")), file);
                }
            }
        } catch (RuntimeException e) {
            Devilica.LOGGER.warn("Random entities: could not list resource packs", e);
        }

        final Map<ResourceLocation, Variants> result = new HashMap<>();
        found.forEach((base, numbered) -> {
            final int count = numbered.lastKey();
            final ResourceLocation[] textures = new ResourceLocation[count + 1];
            textures[1] = base;
            for (int i = 2; i <= count; i++) {
                // A gap in the numbering keeps the original texture there
                textures[i] = numbered.getOrDefault(i, base);
            }
            final ResourceLocation propertiesFile = propertiesFiles.get(base);
            final List<RandomRule> rules = propertiesFile == null ? null : readRules(resources, propertiesFile, count);
            result.put(base, new Variants(textures, rules));
        });
        variants = result;
        DRAWING.clear();
        if (!result.isEmpty()) {
            Devilica.LOGGER.info("Random entities: {} textures with other looks", result.size());
        }
    }

    private static String relative(String path) {
        for (String directory : DIRECTORIES) {
            if (path.startsWith(directory)) {
                return path.substring(directory.length());
            }
        }
        return null;
    }

    private static boolean exists(IResourceManager resources, ResourceLocation location) {
        try (IResource ignored = resources.getResource(location)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static List<RandomRule> readRules(IResourceManager resources, ResourceLocation file, int count) {
        try (IResource resource = resources.getResource(file); Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            final Properties properties = new Properties();
            properties.load(reader);
            final List<RandomRule> rules = RandomRule.parse(properties, count);
            return rules.isEmpty() ? null : rules;
        } catch (IOException | RuntimeException e) {
            Devilica.LOGGER.warn("Random entities: could not read {}: {}", file, e.toString());
            return null;
        }
    }

    // ---- drawing

    public static void beginEntity(Entity entity) {
        DRAWING.addLast(entity);
    }

    public static void beginBlockEntity(TileEntity tileEntity) {
        DRAWING.addLast(tileEntity);
    }

    public static void end() {
        DRAWING.pollLast();
    }

    /** A new frame: nothing is being drawn (a renderer that threw past its end leaves nothing behind) */
    public static void resetFrame() {
        DRAWING.clear();
    }

    /** The look of a texture for what is being drawn now; the texture itself when it has no other looks. */
    public static ResourceLocation choose(ResourceLocation texture) {
        final Object drawing = DRAWING.peekLast();
        if (drawing == null) {
            return texture;
        }
        final Variants looks = variants.get(texture);
        if (looks == null || !DevilicaOptions.get().randomEntities) {
            return texture;
        }
        final int index;
        if (drawing instanceof Entity entity) {
            index = choose(looks, (int) (entity.getUniqueID().getLeastSignificantBits() & Integer.MAX_VALUE), entity);
        } else {
            index = choose(looks, (int) (MathHelper.getPositionRandom(((TileEntity) drawing).getPos()) & Integer.MAX_VALUE), null);
        }
        return looks.textures[index];
    }

    private static int choose(Variants looks, int id, Entity entity) {
        if (looks.rules == null) {
            return 1 + id % (looks.textures.length - 1);
        }
        if (entity != null) {
            for (RandomRule rule : looks.rules) {
                if (rule.matches(entity)) {
                    return rule.choose(id);
                }
            }
        }
        return 1;
    }

    /** How many textures have other looks, for the F3 lines */
    public static int textureCount() {
        return variants.size();
    }
}
