/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.cit;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.resources.FileSprite;
import dev.feldsher.devilica.resources.ResourcePackFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * OptiFine's custom item textures: resource packs give items other looks by damage, enchantments, NBT (a renamed
 * sword) and stack size, from {@code optifine/cit/**.properties} (or {@code mcpatcher/cit}). An item's own texture is
 * built into a model the way an item's texture always is (its edges follow the new image); armour gets other layers.
 */
public final class CustomItemTextures {

    public static final CustomItemTextures INSTANCE = new CustomItemTextures();

    private static final String[] DIRECTORIES = {"optifine/cit/", "mcpatcher/cit/"};

    private List<CitRule> loading = Collections.emptyList();
    /** Sprite name of each texture file a rule uses */
    private final Map<ResourceLocation, String> spriteNames = new HashMap<>();
    private final CitModelPack models = new CitModelPack();

    private static volatile Map<Item, CitRule[]> itemRules = Collections.emptyMap();
    private static volatile Map<Item, CitRule[]> armorRules = Collections.emptyMap();
    /** Enchantment glints, by layer */
    private static volatile CitRule[] glintRules = new CitRule[0];

    private CustomItemTextures() {
    }

    // ---- loading

    @SubscribeEvent
    public void onStitchPre(TextureStitchEvent.Pre event) {
        final TextureMap map = event.getMap();
        if (map != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }
        final IResourceManager resources = Minecraft.getMinecraft().getResourceManager();
        final List<CitRule> rules = new ArrayList<>();
        this.spriteNames.clear();
        for (ResourceLocation location : ResourcePackFiles.find(".properties", DIRECTORIES)) {
            final Properties properties = new Properties();
            try (IResource resource = resources.getResource(location); Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException | RuntimeException e) {
                Devilica.LOGGER.warn("Custom item textures: could not read {}: {}", location, e.toString());
                continue;
            }
            final CitRule rule;
            try {
                rule = read(resources, location, properties);
            } catch (RuntimeException e) {
                // A pack's file is whatever its author typed (a broken regex, a number past int): never the game's crash
                Devilica.LOGGER.warn("Custom item textures: {} is malformed: {}", location, e.toString());
                continue;
            }
            if (rule == null) {
                continue;
            }
            if (rule.type == CitRule.Type.ITEM) {
                final List<ResourceLocation> files = new ArrayList<>(rule.textures.values());
                if (rule.texture != null) {
                    files.add(rule.texture);
                }
                for (ResourceLocation file : files) {
                    sprite(file, map);
                }
            }
            rules.add(rule);
        }
        loadModels(resources, rules, map);
        this.loading = rules;
    }

    /** The atlas name of a texture file, added to the atlas the first time */
    private String sprite(ResourceLocation file, TextureMap map) {
        return this.spriteNames.computeIfAbsent(file, f -> {
            final String name = f.getNamespace() + ":" + f.getPath().replaceFirst("\\.png$", "");
            if (map.getTextureExtry(name) == null) {
                map.setTextureEntry(new FileSprite(name, f));
            }
            return name;
        });
    }

    /** The rules' json models, through Forge's model loader, with their textures added to the atlas */
    private void loadModels(IResourceManager resources, List<CitRule> rules, TextureMap map) {
        this.models.clear();
        final Map<CitRule, ResourceLocation> locations = new IdentityHashMap<>();
        for (CitRule rule : rules) {
            if (rule.type == CitRule.Type.ITEM && rule.modelFile != null) {
                try {
                    locations.put(rule, this.models.add(withAtlasTextures(resources, rule.modelFile, map)));
                } catch (IOException | RuntimeException e) {
                    Devilica.LOGGER.warn("Custom item textures: could not read {}: {}", rule.modelFile, e.toString());
                }
            }
        }
        if (locations.isEmpty() || !(resources instanceof SimpleReloadableResourceManager reloadable)) {
            return;
        }
        reloadable.reloadResourcePack(this.models);
        locations.forEach((rule, location) -> {
            try {
                rule.model = ModelLoaderRegistry.getModel(location);
                for (ResourceLocation texture : rule.model.getTextures()) {
                    map.registerSprite(texture);
                }
            } catch (Exception e) {
                Devilica.LOGGER.warn("Custom item textures: could not load {}: {}", rule.modelFile, e.toString());
            }
        });
    }

    /** A json model with its texture paths beside it or under {@code optifine/} made atlas names */
    private byte[] withAtlasTextures(IResourceManager resources, ResourceLocation file, TextureMap map) throws IOException {
        final JsonObject json;
        try (IResource resource = resources.getResource(file); Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            json = new JsonParser().parse(reader).getAsJsonObject();
        }
        final String path = file.getPath();
        final String directory = path.substring(0, path.lastIndexOf('/') + 1);
        if (json.has("textures")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
                final String value = entry.getValue().getAsString();
                if (value.startsWith("#")) {
                    continue;
                }
                final ResourceLocation texture = file(resources, file.getNamespace(), directory, value, ".png");
                if (texture != null) {
                    entry.setValue(new JsonPrimitive(sprite(texture, map)));
                }
            }
        }
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static CitRule read(IResourceManager resources, ResourceLocation location, Properties properties) {
        final String path = location.getPath();
        final String directory = path.substring(0, path.lastIndexOf('/') + 1);
        final String baseName = path.substring(directory.length()).replaceFirst("\\.properties$", "");
        final String namespace = location.getNamespace();

        final Set<Item> items = new HashSet<>();
        for (String token : CitRule.get(properties, "items", CitRule.get(properties, "matchItems", "")).split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            final Item item = StringUtils.isNumeric(token) ? Item.getItemById(Integer.parseInt(token)) : Item.REGISTRY.getObject(new ResourceLocation(token));
            if (item != null) {
                items.add(item);
            }
        }
        // An enchantment glint without items is for every item with the enchantments
        if (items.isEmpty() && CitRule.type(CitRule.get(properties, "type", "item")) != CitRule.Type.ENCHANTMENT) {
            return null;
        }

        final ResourceLocation texture = file(resources, namespace, directory, CitRule.get(properties, "texture", CitRule.get(properties, "tile", baseName)), ".png");
        final ResourceLocation model = file(resources, namespace, directory, CitRule.get(properties, "model", ""), ".json");
        final Map<String, ResourceLocation> textures = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("texture.")) {
                final ResourceLocation file = file(resources, namespace, directory, properties.getProperty(key).trim(), ".png");
                if (file != null) {
                    textures.put(key.substring("texture.".length()), file);
                }
            }
        }
        final CitRule rule = new CitRule(location.toString(), properties, items, texture, model, textures);
        if (rule.texture == null && rule.modelFile == null && rule.textures.isEmpty()) {
            Devilica.LOGGER.warn("Custom item textures: {}: no texture found", location);
            return null;
        }
        return rule;
    }

    /** A file: {@code name} and {@code ./name} beside the properties, {@code ns:path} or a path from the namespace; null when missing */
    private static ResourceLocation file(IResourceManager resources, String namespace, String directory, String value, String extension) {
        if (value.isEmpty()) {
            return null;
        }
        final String name = value.endsWith(extension) ? value : value + extension;
        final ResourceLocation file;
        if (name.contains(":")) {
            file = new ResourceLocation(name);
        } else if (name.startsWith("./")) {
            file = new ResourceLocation(namespace, directory + name.substring(2));
        } else if (!name.contains("/")) {
            file = new ResourceLocation(namespace, directory + name);
        } else {
            file = new ResourceLocation(namespace, name.replaceFirst("^/", "").replaceFirst("^assets/[^/]+/", ""));
        }
        try (IResource ignored = resources.getResource(file)) {
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    @SubscribeEvent
    public void onStitchPost(TextureStitchEvent.Post event) {
        final TextureMap map = event.getMap();
        if (map != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }
        final TextureAtlasSprite missing = map.getMissingSprite();
        final Map<Item, List<CitRule>> items = new HashMap<>();
        final Map<Item, List<CitRule>> armor = new HashMap<>();
        final List<CitRule> glints = new ArrayList<>();
        for (CitRule rule : this.loading) {
            if (rule.type == CitRule.Type.ENCHANTMENT) {
                if (rule.texture != null) {
                    glints.add(rule);
                }
                continue;
            }
            if (rule.type == CitRule.Type.ITEM) {
                if (rule.texture != null) {
                    final TextureAtlasSprite sprite = map.getAtlasSprite(this.spriteNames.get(rule.texture));
                    if (sprite != missing) {
                        rule.quads = ItemLayerModel.getQuadsForSprite(0, sprite, DefaultVertexFormats.ITEM, Optional.empty());
                    }
                }
                final Map<TextureAtlasSprite, TextureAtlasSprite> swaps = new IdentityHashMap<>();
                rule.textures.forEach((name, file) -> {
                    final TextureAtlasSprite replacement = map.getAtlasSprite(this.spriteNames.get(file));
                    final TextureAtlasSprite original = original(map, name);
                    if (original != null && replacement != missing) {
                        swaps.put(original, replacement);
                    }
                });
                rule.spriteSwaps = swaps.isEmpty() ? null : swaps;
                if (rule.model != null) {
                    try {
                        rule.bakedModel = rule.model.bake(rule.model.getDefaultState(), DefaultVertexFormats.ITEM, location -> map.getAtlasSprite(location.toString()));
                    } catch (RuntimeException e) {
                        Devilica.LOGGER.warn("Custom item textures: could not bake {}: {}", rule.modelFile, e.toString());
                    }
                    rule.model = null;
                }
                if (rule.quads == null && rule.spriteSwaps == null && rule.bakedModel == null) {
                    continue;
                }
            }
            for (Item item : rule.items) {
                (rule.type == CitRule.Type.ARMOR ? armor : items).computeIfAbsent(item, i -> new ArrayList<>()).add(rule);
            }
        }
        itemRules = sorted(items);
        armorRules = sorted(armor);
        glints.sort(Comparator.comparingInt((CitRule rule) -> rule.layer).thenComparing(Comparator.naturalOrder()));
        glintRules = glints.toArray(new CitRule[0]);
        CitModel.clearCache();
        if (!this.loading.isEmpty()) {
            Devilica.LOGGER.info("Custom item textures: {} rules for {} items", this.loading.size(), items.size() + armor.size());
        }
        this.loading = Collections.emptyList();
    }

    /** The item sprite a {@code texture.<name>} entry replaces: {@code bow_pulling_0} is {@code minecraft:items/bow_pulling_0} */
    private static TextureAtlasSprite original(TextureMap map, String name) {
        final TextureAtlasSprite missing = map.getMissingSprite();
        for (String candidate : new String[]{name, "minecraft:items/" + name, "minecraft:blocks/" + name}) {
            if (map.getTextureExtry(candidate) != null) {
                final TextureAtlasSprite sprite = map.getAtlasSprite(candidate);
                if (sprite != missing) {
                    return sprite;
                }
            }
        }
        return null;
    }

    private static Map<Item, CitRule[]> sorted(Map<Item, List<CitRule>> lists) {
        final Map<Item, CitRule[]> result = new Reference2ObjectOpenHashMap<>(lists.size());
        lists.forEach((item, list) -> {
            final CitRule[] array = list.toArray(new CitRule[0]);
            Arrays.sort(array);
            result.put(item, array);
        });
        return result;
    }

    // ---- drawing

    /** The model an item stack is drawn with: the original, or the first matching rule's. */
    public static IBakedModel model(ItemStack stack, IBakedModel model) {
        if (stack.isEmpty() || model.isBuiltInRenderer() || !DevilicaOptions.get().customItemTextures) {
            return model;
        }
        final CitRule[] rules = itemRules.get(stack.getItem());
        if (rules == null) {
            return model;
        }
        for (CitRule rule : rules) {
            if (rule.matches(stack)) {
                if (rule.bakedModel != null) {
                    // Already chosen with its display transforms by jsonModel
                    return model;
                }
                return rule.quads != null ? new CitModel(model, rule.quads, null) : new CitModel(model, null, rule.spriteSwaps);
            }
        }
        return model;
    }

    /** The enchantment glints a stack shows, bottom layer first; empty for vanilla's glint */
    static List<CitRule> glints(ItemStack stack) {
        final CitRule[] rules = glintRules;
        if (rules.length == 0 || stack.isEmpty() || !DevilicaOptions.get().customItemTextures) {
            return List.of();
        }
        List<CitRule> matching = null;
        for (CitRule rule : rules) {
            if ((rule.items.isEmpty() || rule.items.contains(stack.getItem())) && rule.matches(stack)) {
                if (matching == null) {
                    matching = new ArrayList<>(2);
                }
                matching.add(rule);
            }
        }
        return matching == null ? List.of() : matching;
    }

    /** The json model the first matching rule draws a stack with, display transforms and all; null for none */
    public static IBakedModel jsonModel(ItemStack stack) {
        if (stack.isEmpty() || !DevilicaOptions.get().customItemTextures) {
            return null;
        }
        final CitRule[] rules = itemRules.get(stack.getItem());
        if (rules == null) {
            return null;
        }
        for (CitRule rule : rules) {
            if (rule.matches(stack)) {
                return rule.bakedModel;
            }
        }
        return null;
    }

    /** An armour layer's texture for a stack: the original, or the first matching rule's layer of that name */
    public static ResourceLocation armor(ItemStack stack, ResourceLocation texture) {
        if (stack.isEmpty() || !DevilicaOptions.get().customItemTextures) {
            return texture;
        }
        final CitRule[] rules = armorRules.get(stack.getItem());
        if (rules == null) {
            return texture;
        }
        final String path = texture.getPath();
        final String layer = path.substring(path.lastIndexOf('/') + 1).replaceFirst("\\.png$", "");
        for (CitRule rule : rules) {
            final ResourceLocation replacement = rule.textures.get(layer);
            if (replacement != null && rule.matches(stack)) {
                return replacement;
            }
        }
        return texture;
    }

    /** How many items have rules, for the F3 lines */
    public static int itemCount() {
        return itemRules.size() + armorRules.size();
    }
}
