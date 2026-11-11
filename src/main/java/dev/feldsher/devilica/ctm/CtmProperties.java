/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A connected textures properties file, read the way OptiFine reads them on 1.12. */
final class CtmProperties {

    /** A tile that keeps the face's own texture ({@code <skip>}, {@code <default>}) */
    static final String KEEP = null;

    private static final Pattern RANGE = Pattern.compile("(\\d+)-(\\d+)");

    final String source;
    private final Properties properties;
    /** The directory of the file, {@code namespace:path} without the trailing slash */
    private final ResourceLocation directory;

    final List<BlockMatcher> matchBlocks;
    final Set<String> matchTiles;
    final List<BlockMatcher> connectBlocks;
    final int faces;
    final int connect;
    final BitSet biomes;
    final int minHeight;
    final int maxHeight;
    final int weight;
    /** Atlas sprite names of the tiles; files next to the properties are named after their path */
    final String[] tileNames;
    /** Which tiles are files beside the properties (sprite name to file), to be added to the atlas */
    final List<String[]> tileFiles = new ArrayList<>();

    // Overlays
    final Set<String> connectTiles;
    final int tintIndex;
    /** The block whose colour tints the overlay, null for the block drawn */
    final IBlockState tintBlock;
    final BlockRenderLayer layer;

    final List<String> errors = new ArrayList<>();

    CtmProperties(ResourceLocation location, Properties properties) {
        this.source = location.toString();
        this.properties = properties;
        final String path = location.getPath();
        this.directory = new ResourceLocation(location.getNamespace(), path.substring(0, path.lastIndexOf('/')));
        final String baseName = path.substring(path.lastIndexOf('/') + 1).replaceFirst("\\.properties$", "");

        String blocks = get("matchBlocks", "");
        if (blocks.isEmpty() && baseName.matches("block\\d+.*")) {
            blocks = baseName.replaceFirst("block(\\d+).*", "$1");
        }
        this.matchBlocks = BlockMatcher.parseList(blocks, get("metadata", ""));
        this.matchTiles = parseTiles(get("matchTiles", ""));
        if (this.matchBlocks.isEmpty() && this.matchTiles.isEmpty()) {
            this.matchTiles.add(spriteName(location.getNamespace(), baseName));
        }
        this.connectBlocks = BlockMatcher.parseList(get("connectBlocks", ""), get("connectMetadata", ""));
        this.faces = parseFaces(get("faces", ""));

        this.connect = switch (get("connect", "").toLowerCase(Locale.ROOT)) {
            case "block" -> TileOverride.CONNECT_BY_BLOCK;
            case "tile" -> TileOverride.CONNECT_BY_TILE;
            case "material" -> TileOverride.CONNECT_BY_MATERIAL;
            case "list" -> TileOverride.CONNECT_BY_LIST;
            default -> this.matchTiles.isEmpty() ? TileOverride.CONNECT_BY_BLOCK : TileOverride.CONNECT_BY_TILE;
        };

        this.biomes = parseBiomes(get("biomes", ""));
        final int[] heights = parseHeights(get("heights", ""), get("minHeight", ""), get("maxHeight", ""));
        this.minHeight = heights[0];
        this.maxHeight = heights[1];
        this.weight = parseInt(get("weight", "0"));
        this.tileNames = parseTileList(get("tiles", ""));

        this.connectTiles = parseTiles(get("connectTiles", ""));
        this.tintIndex = get("tintIndex", "").isEmpty() ? -1 : parseInt(get("tintIndex", "-1"));
        final List<BlockMatcher> tint = BlockMatcher.parseList(get("tintBlock", ""), "");
        this.tintBlock = tint.isEmpty() ? null : tint.get(0).block.getDefaultState();
        this.layer = switch (get("layer", "").toLowerCase(Locale.ROOT)) {
            case "cutout" -> BlockRenderLayer.CUTOUT;
            case "translucent" -> BlockRenderLayer.TRANSLUCENT;
            default -> BlockRenderLayer.CUTOUT_MIPPED;
        };
    }

    String get(String key, String fallback) {
        final String value = this.properties.getProperty(key);
        return value == null ? fallback : value.trim();
    }

    TileOverride create() {
        final String method = get("method", "default").toLowerCase(Locale.ROOT);
        final TileOverride override = switch (method) {
            case "default", "glass", "ctm" -> new TileOverride.Ctm(this);
            case "random" -> this.tileNames.length == 1 ? new TileOverride.Fixed(this) : new TileOverride.Random(this);
            case "fixed", "static" -> new TileOverride.Fixed(this);
            case "bookshelf", "horizontal" -> new TileOverride.Horizontal(this);
            case "horizontal+vertical", "h+v" -> new TileOverride.HorizontalVertical(this);
            case "vertical" -> new TileOverride.Vertical(this);
            case "vertical+horizontal", "v+h" -> new TileOverride.VerticalHorizontal(this);
            case "sandstone", "top" -> new TileOverride.Top(this);
            case "repeat", "pattern" -> new TileOverride.Repeat(this);
            case "ctm_compact", "compact" -> new CompactCtm(this);
            case "overlay" -> new Overlay.Edges(this);
            case "overlay_ctm" -> new Overlay.Single(this, new TileOverride.Ctm(this));
            case "overlay_random" -> new Overlay.Single(this, this.tileNames.length == 1 ? new TileOverride.Fixed(this) : new TileOverride.Random(this));
            case "overlay_repeat" -> new Overlay.Single(this, new TileOverride.Repeat(this));
            case "overlay_fixed" -> new Overlay.Single(this, new TileOverride.Fixed(this));
            default -> null;
        };
        if (override == null) {
            this.errors.add("method " + method + " is not supported");
            return null;
        }
        if (this.tileNames.length == 0) {
            this.errors.add("no tiles");
            return null;
        }
        final String problem = override.checkTiles();
        if (problem != null) {
            this.errors.add(method + " " + problem);
            return null;
        }
        return override;
    }

    private String[] parseTileList(String list) {
        final List<String> names = new ArrayList<>();
        if (list.isEmpty()) {
            return new String[0];
        }
        for (String token : list.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            final Matcher range = RANGE.matcher(token);
            if (range.matches()) {
                final int from = Integer.parseInt(range.group(1));
                final int to = Integer.parseInt(range.group(2));
                for (int i = from; i <= to; i++) {
                    names.add(fileTile(String.valueOf(i)));
                }
            } else if (token.equals("<skip>") || token.equals("<default>")) {
                names.add(KEEP);
            } else if (token.matches("\\d+") || token.endsWith(".png") || !token.contains("/")) {
                names.add(fileTile(token.endsWith(".png") ? token.substring(0, token.length() - 4) : token));
            } else {
                // A texture of the atlas, as in textures/blocks/stone.png, blocks/stone or mod:blocks/stone
                names.add(spriteName(this.directory.getNamespace(), token));
            }
        }
        return names.toArray(new String[0]);
    }

    /** A tile image next to the properties file (or under a path of its own) */
    private String fileTile(String name) {
        final ResourceLocation file;
        if (name.contains(":")) {
            file = new ResourceLocation(name + ".png");
        } else if (name.startsWith("/") || name.startsWith("assets/")) {
            final String path = name.replaceFirst("^/", "").replaceFirst("^assets/[^/]+/", "");
            file = new ResourceLocation(this.directory.getNamespace(), path + ".png");
        } else {
            file = new ResourceLocation(this.directory.getNamespace(), this.directory.getPath() + "/" + name + ".png");
        }
        final String sprite = file.getNamespace() + ":" + file.getPath().substring(0, file.getPath().length() - 4);
        this.tileFiles.add(new String[]{sprite, file.toString()});
        return sprite;
    }

    private Set<String> parseTiles(String list) {
        final Set<String> tiles = new HashSet<>();
        for (String token : list.split("\\s+")) {
            if (!token.isEmpty()) {
                tiles.add(spriteName(this.directory.getNamespace(), token));
            }
        }
        return tiles;
    }

    /** OptiFine's texture names: {@code glass} is {@code minecraft:blocks/glass}, paths drop {@code textures/} and {@code .png} */
    static String spriteName(String defaultNamespace, String name) {
        String namespace = "minecraft";
        String path = name;
        final int colon = name.indexOf(':');
        if (colon >= 0) {
            namespace = name.substring(0, colon);
            path = name.substring(colon + 1);
        } else if (name.startsWith("textures/") || name.contains("/")) {
            namespace = defaultNamespace;
        }
        path = path.replaceFirst("^textures/", "").replaceFirst("\\.png$", "");
        if (!path.contains("/")) {
            path = "blocks/" + path;
        }
        return namespace + ":" + path;
    }

    private static int parseFaces(String list) {
        if (list.isEmpty()) {
            return 0x3F;
        }
        int faces = 0;
        for (String face : list.toLowerCase(Locale.ROOT).split("\\s+")) {
            faces |= switch (face) {
                case "bottom", "down" -> 1 << CtmContext.DOWN;
                case "top", "up" -> 1 << CtmContext.UP;
                case "north" -> 1 << CtmContext.NORTH;
                case "south" -> 1 << CtmContext.SOUTH;
                case "west" -> 1 << CtmContext.WEST;
                case "east" -> 1 << CtmContext.EAST;
                case "side", "sides" -> (1 << CtmContext.NORTH) | (1 << CtmContext.SOUTH) | (1 << CtmContext.WEST) | (1 << CtmContext.EAST);
                case "all" -> 0x3F;
                default -> 0;
            };
        }
        return faces;
    }

    private static BitSet parseBiomes(String list) {
        if (list.isEmpty()) {
            return null;
        }
        final BitSet biomes = new BitSet();
        for (String name : list.split("\\s+")) {
            if (name.isEmpty()) {
                continue;
            }
            Biome biome = Biome.REGISTRY.getObject(new ResourceLocation(name.toLowerCase(Locale.ROOT)));
            if (biome == null) {
                for (Biome candidate : Biome.REGISTRY) {
                    if (candidate.getBiomeName().replace(" ", "").equalsIgnoreCase(name)) {
                        biome = candidate;
                        break;
                    }
                }
            }
            if (biome != null) {
                biomes.set(Biome.getIdForBiome(biome));
            }
        }
        return biomes;
    }

    private static int[] parseHeights(String heights, String min, String max) {
        int low = Integer.MIN_VALUE;
        int high = Integer.MAX_VALUE;
        if (!heights.isEmpty()) {
            final Matcher range = Pattern.compile("(-?\\d+)-(-?\\d+)").matcher(heights.split("\\s+")[0]);
            if (range.matches()) {
                low = Integer.parseInt(range.group(1));
                high = Integer.parseInt(range.group(2));
            }
        }
        if (!min.isEmpty()) {
            low = parseInt(min);
        }
        if (!max.isEmpty()) {
            high = parseInt(max);
        }
        return new int[]{low, high};
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
