/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.colors;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.ctm.BlockMatcher;
import dev.feldsher.devilica.resources.ResourcePackFiles;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the custom colours of the resource packs: {@code color.properties}, {@code colormap/} and {@code lightmap/}. */
final class PaletteLoader {

    private static final String[] ROOTS = {"optifine/", "mcpatcher/"};
    private static final Pattern LIGHTMAP = Pattern.compile("world(-?\\d+)\\.png");

    private final IResourceManager resources;
    private final Palette palette = new Palette();
    /** The block colours the palette and custom colour maps give, which also colour a block's untinted faces */
    private final Map<IBlockState, IBlockColor> mapped = new Reference2ObjectLinkedOpenHashMap<>();
    private String root = ROOTS[0];
    private Colormap.Format defaultFormat = Colormap.Format.VANILLA;

    private PaletteLoader(IResourceManager resources) {
        this.resources = resources;
    }

    static Palette load(IResourceManager resources) {
        return new PaletteLoader(resources).load();
    }

    private Palette load() {
        final Properties properties = readColorProperties();
        if (properties.getProperty("palette.format") != null) {
            this.defaultFormat = format(properties.getProperty("palette.format"), Colormap.Format.VANILLA);
        }
        builtInMaps();
        readColors(properties);
        paletteMaps(properties);
        customMaps();
        this.palette.blocks.putAll(this.mapped);
        untintedFaces();
        lightmaps();
        return this.palette.count == 0 ? Palette.EMPTY : this.palette;
    }

    private Properties readColorProperties() {
        for (String candidate : ROOTS) {
            final Properties properties = properties(new ResourceLocation(candidate + "color.properties"));
            if (properties != null) {
                this.root = candidate;
                return properties;
            }
        }
        return new Properties();
    }

    // ---- colour maps of vanilla's tinted blocks

    private void builtInMaps() {
        final Colormap water = map("colormap/water.png");
        this.palette.water = water;
        if (water != null) {
            final IBlockColor color = byBiome(water);
            putAll(Blocks.WATER, state -> color);
            putAll(Blocks.FLOWING_WATER, state -> color);
        }
        final Colormap pine = map("colormap/pine.png");
        final Colormap birch = map("colormap/birch.png");
        if (pine != null || birch != null) {
            final IBlockColor pineColor = pine == null ? null : byBiome(pine);
            final IBlockColor birchColor = birch == null ? null : byBiome(birch);
            for (IBlockState state : Blocks.LEAVES.getBlockState().getValidStates()) {
                final BlockPlanks.EnumType type = state.getValue(BlockOldLeaf.VARIANT);
                final IBlockColor color = type == BlockPlanks.EnumType.SPRUCE ? pineColor : type == BlockPlanks.EnumType.BIRCH ? birchColor : null;
                if (color != null) {
                    this.palette.blocks.put(state, color);
                }
            }
        }
        this.palette.swampGrass = map("colormap/swampgrass.png");
        this.palette.swampFoliage = map("colormap/swampfoliage.png");
        this.palette.fog = map("colormap/fog0.png");
        this.palette.sky = map("colormap/sky0.png");
        this.palette.underwater = map("colormap/underwater.png");
        this.palette.underlava = map("colormap/underlava.png");

        final Colormap redstone = map("colormap/redstone.png");
        this.palette.redstone = redstone;
        if (redstone != null) {
            putAll(Blocks.REDSTONE_WIRE, state -> {
                final int color = redstone.entry(state.getValue(BlockRedstoneWire.POWER));
                return (s, world, pos, tint) -> color;
            });
        }
        stems(Blocks.PUMPKIN_STEM, map("colormap/pumpkinstem.png"));
        stems(Blocks.MELON_STEM, map("colormap/melonstem.png"));
        this.palette.mycelium = map("colormap/myceliumparticle.png");
        this.palette.xpOrb = map("colormap/xporb.png");
        this.palette.durability = map("colormap/durability.png");
    }

    private void stems(Block stem, Colormap map) {
        if (map != null) {
            putAll(stem, state -> {
                final int color = map.entry(state.getValue(BlockStem.AGE));
                return (s, world, pos, tint) -> color;
            });
        }
    }

    private void putAll(Block block, Function<IBlockState, IBlockColor> colors) {
        for (IBlockState state : block.getBlockState().getValidStates()) {
            this.palette.blocks.put(state, colors.apply(state));
        }
    }

    private static IBlockColor byBiome(Colormap map) {
        if (map.isFixed()) {
            final int color = map.fallback();
            return (state, world, pos, tint) -> color;
        }
        return (state, world, pos, tint) -> world != null && pos != null ? map.color(world, pos) : map.fallback();
    }

    // ---- color.properties

    private void readColors(Properties properties) {
        this.palette.netherFog = vector(properties, "fog.nether");
        this.palette.endFog = vector(properties, "fog.end");
        this.palette.endSky = color(properties, "sky.end");
        final String clouds = properties.getProperty("clouds");
        if (clouds != null) {
            this.palette.clouds = switch (clouds.trim().toLowerCase(Locale.ROOT)) {
                case "none" -> 0;
                case "fast" -> 1;
                case "fancy" -> 2;
                default -> -1;
            };
            count(this.palette.clouds >= 0);
        }
        final int lilyPad = color(properties, "lilypad");
        if (lilyPad >= 0) {
            putAll(Blocks.WATERLILY, state -> (s, world, pos, tint) -> lilyPad);
        }
        this.palette.waterParticles = color(properties, "particle.water");
        this.palette.portalParticles = color(properties, "particle.portal");
        this.palette.xpBar = color(properties, "text.xpbar");
        this.palette.boss = color(properties, "text.boss");
        this.palette.sign = color(properties, "text.sign");
        textColors(properties);
        potionColors(properties);
        dyeColors(properties, "sheep.", this.palette.sheep);
        dyeColors(properties, "collar.", this.palette.collar);
        dyeColors(properties, "dye.", this.palette.dye);
        namedColors(properties, "egg.shell.", this.palette.eggShell);
        namedColors(properties, "egg.spots.", this.palette.eggSpots);
        mapColors(properties);
    }

    /** OptiFine's and MCPatcher's names differ in case and underscores ({@code lightBlue}, {@code light_blue}) */
    static String normalName(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private void dyeColors(Properties properties, String prefix, int[] colors) {
        final Map<String, Integer> byName = new HashMap<>();
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            byName.put(normalName(dye.getName()), dye.getMetadata());
            byName.put(normalName(dye.getTranslationKey()), dye.getMetadata());
        }
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                final Integer meta = byName.get(normalName(key.substring(prefix.length())));
                final int color = color(properties, key);
                if (meta != null && color >= 0) {
                    colors[meta] = color;
                }
            }
        }
    }

    private void namedColors(Properties properties, String prefix, Map<String, Integer> colors) {
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                final int color = color(properties, key);
                if (color >= 0) {
                    colors.put(normalName(key.substring(prefix.length())), color);
                }
            }
        }
    }

    /** OptiFine's names of the map colours, by index */
    private static final String[] MAP_COLORS = {
            "air", "grass", "sand", "cloth", "tnt", "ice", "iron", "foliage", "snow", "clay", "dirt", "stone", "water",
            "wood", "quartz", "adobe", "magenta", "lightBlue", "yellow", "lime", "pink", "gray", "silver", "cyan",
            "purple", "blue", "brown", "green", "red", "black", "gold", "diamond", "lapis", "emerald", "obsidian",
            "netherrack", "whiteTerracotta", "orangeTerracotta", "magentaTerracotta", "lightBlueTerracotta",
            "yellowTerracotta", "limeTerracotta", "pinkTerracotta", "grayTerracotta", "silverTerracotta",
            "cyanTerracotta", "purpleTerracotta", "blueTerracotta", "brownTerracotta", "greenTerracotta",
            "redTerracotta", "blackTerracotta",
    };

    private void mapColors(Properties properties) {
        final Map<String, Integer> byName = new HashMap<>();
        for (int i = 0; i < MAP_COLORS.length; i++) {
            byName.put(normalName(MAP_COLORS[i]), i);
        }
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("map.")) {
                final Integer index = byName.get(normalName(key.substring("map.".length())));
                final int color = color(properties, key);
                if (index != null && color >= 0) {
                    this.palette.mapColors[index] = color;
                }
            }
        }
    }

    private void textColors(Properties properties) {
        final int[] codes = this.palette.textCodes;
        for (int i = 0; i < codes.length; i++) {
            codes[i] = color(properties, "text.code." + i);
        }
        for (int i = 0; i < 16; i++) {
            // A code's shadow follows it unless the pack sets the shadow too
            if (codes[i] >= 0 && codes[i + 16] < 0) {
                codes[i + 16] = (codes[i] & 0xFCFCFC) >> 2;
            }
        }
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("text.") || key.startsWith("text.code.")) {
                continue;
            }
            final String name = key.substring("text.".length());
            if (name.equals("xpbar") || name.equals("boss") || name.equals("sign")) {
                continue;
            }
            final int from = hex(name);
            final int to = color(properties, key);
            if (from >= 0 && to >= 0) {
                this.palette.text.put(from, to);
            }
        }
    }

    private void potionColors(Properties properties) {
        final Map<String, Potion> byName = new HashMap<>();
        for (Potion potion : Potion.REGISTRY) {
            final String name = potion.getName();
            byName.put(name.startsWith("effect.") ? name.substring("effect.".length()) : name, potion);
            final ResourceLocation id = Potion.REGISTRY.getNameForObject(potion);
            if (id != null) {
                byName.putIfAbsent(id.getPath(), potion);
            }
        }
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("potion.")) {
                continue;
            }
            final String name = key.substring("potion.".length());
            final int color = color(properties, key);
            if (color < 0) {
                continue;
            }
            if (name.equals("water")) {
                this.palette.waterBottle = color;
                continue;
            }
            final Potion potion = byName.get(name);
            if (potion != null) {
                this.palette.potions.put(potion, color);
            } else {
                Devilica.LOGGER.warn("Custom colours: no potion effect {}", name);
            }
        }
    }

    // ---- block colour maps of the packs

    /** {@code palette.block.<image>=<blocks>} */
    private void paletteMaps(Properties properties) {
        for (String key : new TreeSet<>(properties.stringPropertyNames())) {
            if (!key.startsWith("palette.block.")) {
                continue;
            }
            final ResourceLocation source = resolve(key.substring("palette.block.".length()), this.root, "minecraft", this.root);
            final BufferedImage image = image(source);
            if (image == null) {
                Devilica.LOGGER.warn("Custom colours: no colour map {}", source);
                continue;
            }
            addBlocks(properties.getProperty(key), Colormap.of(image, this.defaultFormat, 0, 0));
        }
    }

    /** {@code colormap/custom/*.properties}: a colour map, its format and its blocks */
    private void customMaps() {
        final String[] directories = new String[2 * ROOTS.length];
        for (int i = 0; i < ROOTS.length; i++) {
            directories[2 * i] = ROOTS[i] + "colormap/custom/";
            directories[2 * i + 1] = ROOTS[i] + "colormap/blocks/";
        }
        for (ResourceLocation location : ResourcePackFiles.find(".properties", directories)) {
            final Properties properties = properties(location);
            if (properties == null) {
                continue;
            }
            final String path = location.getPath();
            final String directory = path.substring(0, path.lastIndexOf('/') + 1);
            final String name = path.substring(directory.length(), path.length() - ".properties".length());
            final String fileRoot = path.startsWith(ROOTS[1]) ? ROOTS[1] : ROOTS[0];
            final Colormap map = customMap(location, properties, name, directory, fileRoot);
            if (map != null) {
                addBlocks(properties.getProperty("blocks", name), map);
            }
        }
    }

    private Colormap customMap(ResourceLocation location, Properties properties, String name, String directory, String fileRoot) {
        final Colormap.Format format = format(properties.getProperty("format"), this.defaultFormat);
        if (format == Colormap.Format.FIXED) {
            final int color = color(properties, "color");
            if (color < 0) {
                Devilica.LOGGER.warn("Custom colours: {} has a fixed format but no color", location);
                return null;
            }
            return Colormap.fixed(color);
        }
        final ResourceLocation source = resolve(properties.getProperty("source", name + ".png"), directory, location.getNamespace(), fileRoot);
        final BufferedImage image = image(source);
        if (image == null) {
            Devilica.LOGGER.warn("Custom colours: {} has no image {}", location, source);
            return null;
        }
        return Colormap.of(image, format, integer(properties, "yVariance"), integer(properties, "yOffset"));
    }

    private void addBlocks(String blocks, Colormap map) {
        if (blocks == null) {
            return;
        }
        final IBlockColor color = byBiome(map);
        boolean any = false;
        for (BlockMatcher matcher : BlockMatcher.parseList(blocks, "")) {
            for (IBlockState state : matcher.block.getBlockState().getValidStates()) {
                if (matcher.matches(matcher.block, matcher.block.getMetaFromState(state))) {
                    this.mapped.put(state, color);
                    any = true;
                }
            }
        }
        count(any);
    }

    /** Untinted faces of mapped blocks take the map too; a grass block's take dirt's, redstone's none */
    private void untintedFaces() {
        final IBlockColor dirt = this.mapped.get(Blocks.DIRT.getDefaultState());
        for (Map.Entry<IBlockState, IBlockColor> entry : this.mapped.entrySet()) {
            final Block block = entry.getKey().getBlock();
            if (block == Blocks.GRASS) {
                if (dirt != null) {
                    this.palette.untinted.put(entry.getKey(), dirt);
                }
            } else if (block != Blocks.REDSTONE_WIRE) {
                this.palette.untinted.put(entry.getKey(), entry.getValue());
            }
        }
    }

    // ---- lightmaps

    private void lightmaps() {
        for (String candidate : ROOTS) {
            for (ResourceLocation location : ResourcePackFiles.find(".png", candidate + "lightmap/")) {
                final String path = location.getPath();
                final Matcher matcher = LIGHTMAP.matcher(path.substring(path.lastIndexOf('/') + 1));
                if (!matcher.matches()) {
                    continue;
                }
                final int dimension = Integer.parseInt(matcher.group(1));
                if (this.palette.lightmaps.containsKey(dimension)) {
                    continue;
                }
                final BufferedImage image = image(location);
                final Lightmap lightmap = image == null ? null : Lightmap.of(image);
                if (lightmap == null) {
                    Devilica.LOGGER.warn("Custom colours: {} must be 32 or 64 pixels high", location);
                    continue;
                }
                this.palette.lightmaps.put(dimension, lightmap);
                this.palette.count++;
            }
        }
    }

    // ---- reading

    /** A colour map in the colour properties' folder, or the other one; with its own properties beside it */
    private Colormap map(String path) {
        for (String candidate : new String[]{this.root, this.root.equals(ROOTS[0]) ? ROOTS[1] : ROOTS[0]}) {
            final BufferedImage image = image(new ResourceLocation(candidate + path));
            if (image == null) {
                continue;
            }
            final Properties properties = properties(new ResourceLocation(candidate + path.substring(0, path.length() - ".png".length()) + ".properties"));
            this.palette.count++;
            if (properties == null) {
                return Colormap.of(image, this.defaultFormat, 0, 0);
            }
            final Colormap.Format format = format(properties.getProperty("format"), this.defaultFormat);
            if (format == Colormap.Format.FIXED && color(properties, "color") >= 0) {
                return Colormap.fixed(color(properties, "color"));
            }
            return Colormap.of(image, format == Colormap.Format.FIXED ? this.defaultFormat : format, integer(properties, "yVariance"), integer(properties, "yOffset"));
        }
        return null;
    }

    /** {@code ~/} is the pack's OptiFine folder, a {@code /} the namespace's root, and anything else beside the file */
    private static ResourceLocation resolve(String path, String directory, String namespace, String fileRoot) {
        String file = path.trim();
        if (!file.endsWith(".png")) {
            file += ".png";
        }
        if (file.startsWith("~/")) {
            return new ResourceLocation(namespace, fileRoot + file.substring(2));
        }
        if (file.indexOf(':') > 0) {
            return new ResourceLocation(file);
        }
        if (file.startsWith("/")) {
            return new ResourceLocation(namespace, file.substring(1));
        }
        return new ResourceLocation(namespace, directory + file);
    }

    private Properties properties(ResourceLocation location) {
        try (IResource resource = this.resources.getResource(location);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            final Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedImage image(ResourceLocation location) {
        try (IResource resource = this.resources.getResource(location)) {
            return TextureUtil.readBufferedImage(resource.getInputStream());
        } catch (IOException e) {
            return null;
        }
    }

    private static Colormap.Format format(String value, Colormap.Format fallback) {
        if (value == null) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "vanilla" -> Colormap.Format.VANILLA;
            case "grid" -> Colormap.Format.GRID;
            case "fixed" -> Colormap.Format.FIXED;
            default -> fallback;
        };
    }

    private int color(Properties properties, String key) {
        final String value = properties.getProperty(key);
        if (value == null) {
            return -1;
        }
        final int color = hex(value);
        if (color < 0) {
            Devilica.LOGGER.warn("Custom colours: {}={} is not a colour", key, value);
        }
        count(color >= 0);
        return color;
    }

    private Vec3d vector(Properties properties, String key) {
        final int color = color(properties, key);
        return color < 0 ? null : new Vec3d((color >> 16 & 0xFF) / 255.0, (color >> 8 & 0xFF) / 255.0, (color & 0xFF) / 255.0);
    }

    private static int hex(String value) {
        String text = value.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        } else if (text.startsWith("0x")) {
            text = text.substring(2);
        }
        try {
            return Integer.parseInt(text, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int integer(Properties properties, String key) {
        try {
            return Integer.parseInt(properties.getProperty(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void count(boolean set) {
        if (set) {
            this.palette.count++;
        }
    }
}
