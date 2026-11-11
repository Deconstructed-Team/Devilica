/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.sky;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.resources.ResourcePackFiles;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom skies from resource packs: {@code optifine/sky/world<dimension>/sky<n>.properties} (or {@code mcpatcher/sky})
 * draw skybox layers over the sky's colour, and {@code sun.properties} and {@code moon_phases.properties} put other
 * images in place of the sun and the moon.
 */
public final class CustomSky implements IResourceManagerReloadListener {

    public static final CustomSky INSTANCE = new CustomSky();

    private static final String[] DIRECTORIES = {"optifine/sky/", "mcpatcher/sky/"};
    private static final Pattern FILE = Pattern.compile("(?:optifine|mcpatcher)/sky/world(-?\\d+)/(.+)\\.properties");
    private static final Pattern LAYER = Pattern.compile("sky(\\d*)");
    private static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON = new ResourceLocation("textures/environment/moon_phases.png");

    private record WorldSky(List<SkyLayer> layers, Map<ResourceLocation, SkyLayer> celestial) {
    }

    private static volatile Map<Integer, WorldSky> skies = Collections.emptyMap();

    private CustomSky() {
    }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {
        final Map<Integer, TreeMap<Integer, SkyLayer>> layers = new HashMap<>();
        final Map<Integer, Map<ResourceLocation, SkyLayer>> celestial = new HashMap<>();
        try {
            for (ResourceLocation file : ResourcePackFiles.find(".properties", DIRECTORIES)) {
                final Matcher matcher = FILE.matcher(file.getPath());
                if (!matcher.matches()) {
                    continue;
                }
                final int dimension = Integer.parseInt(matcher.group(1));
                final String name = matcher.group(2);
                final boolean sun = name.equals("sun");
                final boolean moon = name.equals("moon_phases");
                final Matcher layerName = LAYER.matcher(name);
                if (!sun && !moon && !layerName.matches()) {
                    continue;
                }
                final SkyLayer layer = read(resources, file, sun || moon);
                if (layer == null) {
                    continue;
                }
                if (sun || moon) {
                    celestial.computeIfAbsent(dimension, d -> new HashMap<>()).put(sun ? SUN : MOON, layer);
                } else {
                    final int order = layerName.group(1).isEmpty() ? -1 : Integer.parseInt(layerName.group(1));
                    layers.computeIfAbsent(dimension, d -> new TreeMap<>()).put(order, layer);
                }
            }
        } catch (RuntimeException e) {
            Devilica.LOGGER.warn("Custom sky: could not list resource packs", e);
        }
        final Map<Integer, WorldSky> result = new HashMap<>();
        layers.forEach((dimension, byOrder) -> result.put(dimension, new WorldSky(new ArrayList<>(byOrder.values()), celestial.getOrDefault(dimension, Map.of()))));
        celestial.forEach((dimension, objects) -> result.putIfAbsent(dimension, new WorldSky(List.of(), objects)));
        skies = result;
        if (!result.isEmpty()) {
            Devilica.LOGGER.info("Custom sky: {} worlds", result.size());
        }
    }

    private static SkyLayer read(IResourceManager resources, ResourceLocation file, boolean celestial) {
        final Properties properties = new Properties();
        try (IResource resource = resources.getResource(file); Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | RuntimeException e) {
            Devilica.LOGGER.warn("Custom sky: could not read {}: {}", file, e.toString());
            return null;
        }
        final String path = file.getPath();
        final String directory = path.substring(0, path.lastIndexOf('/') + 1);
        final ResourceLocation texture = source(file.getNamespace(), directory, properties.getProperty("source"), path.replaceFirst("\\.properties$", ".png"));
        try (IResource ignored = resources.getResource(texture)) {
            // the image is there
        } catch (IOException e) {
            Devilica.LOGGER.warn("Custom sky: {}: no image {}", file, texture);
            return null;
        }
        final List<String> warnings = new ArrayList<>();
        final SkyLayer layer = new SkyLayer(properties, texture, celestial, warnings);
        warnings.forEach(warning -> Devilica.LOGGER.warn("Custom sky: {}: {}", file, warning));
        if (layer.problem != null) {
            Devilica.LOGGER.warn("Custom sky: {}: {}", file, layer.problem);
            return null;
        }
        return layer;
    }

    /** {@code ./x.png} and {@code x.png} beside the file, {@code ns:path} as is, any other path from the namespace's root */
    private static ResourceLocation source(String namespace, String directory, String source, String fallback) {
        if (source == null || source.trim().isEmpty()) {
            return new ResourceLocation(namespace, fallback);
        }
        final String value = source.trim();
        if (value.contains(":")) {
            return new ResourceLocation(value);
        }
        if (value.startsWith("./")) {
            return new ResourceLocation(namespace, directory + value.substring(2));
        }
        if (!value.contains("/")) {
            return new ResourceLocation(namespace, directory + value);
        }
        return new ResourceLocation(namespace, value.replaceFirst("^/", "").replaceFirst("^assets/[^/]+/", ""));
    }

    /** How many worlds have a sky of a resource pack, for the F3 lines */
    public static int worldCount() {
        return skies.size();
    }

    private static WorldSky current(WorldClient world) {
        if (world == null || !DevilicaOptions.get().customSky) {
            return null;
        }
        return skies.get(world.provider.getDimension());
    }

    /**
     * The layers of the world's sky, in the sky's frame before the sun's turn (vanilla's state for the sun on entry and
     * exit: blending added, colour faded by rain).
     */
    public static void render(WorldClient world, float partialTicks) {
        final WorldSky sky = current(world);
        if (sky == null || sky.layers.isEmpty()) {
            return;
        }
        final float rainFade = 1.0F - world.getRainStrength(partialTicks);
        final double worldTime = world.getWorldTime() + partialTicks;
        final float celestialAngle = world.getCelestialAngle(partialTicks);
        for (SkyLayer layer : sky.layers) {
            final float brightness = layer.brightness(world, worldTime, rainFade);
            if (brightness > 0.0F) {
                layer.render(celestialAngle, brightness);
            }
        }
        SkyBlend.restore(rainFade);
    }

    /** The image for the sun or the moon, with its blending set up; the texture itself when the pack has none */
    public static ResourceLocation celestial(WorldClient world, ResourceLocation texture, float partialTicks) {
        final WorldSky sky = current(world);
        final SkyLayer layer = sky == null ? null : sky.celestial.get(texture);
        if (layer == null) {
            return texture;
        }
        layer.blend.apply(1.0F - world.getRainStrength(partialTicks));
        return layer.texture;
    }
}
