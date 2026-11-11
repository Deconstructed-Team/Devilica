/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.cit;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.model.IModel;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One OptiFine custom item texture rule ({@code optifine/cit/**.properties}): which stacks it takes (items, damage,
 * stack size, enchantments, NBT such as the name) and the texture they get, on the item or on the armour worn.
 */
final class CitRule implements Comparable<CitRule> {

    enum Type {ITEM, ARMOR, ENCHANTMENT}

    private static int nextOrder;

    final String source;
    final Type type;
    final Set<Item> items;
    private final List<int[]> damage;
    private final boolean damagePercent;
    private final int damageMask;
    private final List<int[]> stackSize;
    private final int[] enchantments;
    private final List<int[]> enchantmentLevels;
    private final List<NbtCondition> nbt;
    private final int weight;
    // Enchantment glints
    final String blend;
    final float speed;
    final float rotation;
    final int layer;
    private final int order = nextOrder++;

    /** The item's own texture: a file to build the item from */
    final ResourceLocation texture;
    /** A json model file to draw the item with ({@code model=}), or null */
    final ResourceLocation modelFile;
    /** Textures in place of named sprites of the item's model (bow pulls, potion overlays), or armour layers */
    final Map<String, ResourceLocation> textures;

    /** Filled once the atlas is stitched */
    List<BakedQuad> quads;
    Map<TextureAtlasSprite, TextureAtlasSprite> spriteSwaps;
    /** The json model, loaded at the stitch's start and baked at its end */
    IModel model;
    IBakedModel bakedModel;

    private record NbtCondition(String[] path, Pattern pattern, String exact) {
    }

    CitRule(String source, Properties properties, Set<Item> items, ResourceLocation texture, ResourceLocation modelFile,
            Map<String, ResourceLocation> textures) {
        this.source = source;
        this.type = type(get(properties, "type", "item"));
        this.items = items;
        final String damage = get(properties, "damage", "");
        this.damagePercent = damage.contains("%");
        this.damage = ranges(damage.replace("%", ""));
        this.damageMask = parseInt(get(properties, "damageMask", "0"));
        this.stackSize = ranges(get(properties, "stackSize", ""));
        this.enchantments = parseEnchantments(get(properties, "enchantments", get(properties, "enchantmentIDs", "")));
        this.enchantmentLevels = ranges(get(properties, "enchantmentLevels", ""));
        this.nbt = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("nbt.")) {
                this.nbt.add(nbtCondition(key.substring(4).split("\\."), properties.getProperty(key).trim()));
            }
        }
        this.weight = parseInt(get(properties, "weight", "0"));
        this.blend = get(properties, "blend", "add").toLowerCase(Locale.ROOT);
        this.speed = parseFloat(get(properties, "speed", "0"));
        this.rotation = parseFloat(get(properties, "rotation", "0"));
        this.layer = parseInt(get(properties, "layer", "0"));
        this.texture = texture;
        this.modelFile = modelFile;
        this.textures = textures;
    }

    boolean matches(ItemStack stack) {
        if (this.damage != null) {
            int damage = stack.getItemDamage();
            if (this.damageMask != 0) {
                damage &= this.damageMask;
            }
            if (this.damagePercent) {
                final int max = stack.getMaxDamage();
                damage = max <= 0 ? 0 : damage * 100 / max;
            }
            if (!inRanges(this.damage, damage)) {
                return false;
            }
        }
        if (this.stackSize != null && !inRanges(this.stackSize, stack.getCount())) {
            return false;
        }
        if (this.enchantments != null || this.enchantmentLevels != null) {
            if (!matchesEnchantments(stack)) {
                return false;
            }
        }
        if (!this.nbt.isEmpty()) {
            final NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                return false;
            }
            for (NbtCondition condition : this.nbt) {
                if (!matchesNbt(tag, condition, 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchesEnchantments(ItemStack stack) {
        final NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return false;
        }
        final NBTTagList list = tag.hasKey("ench", 9) ? tag.getTagList("ench", 10) : tag.getTagList("StoredEnchantments", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound entry = list.getCompoundTagAt(i);
            final int id = entry.getShort("id");
            final int level = entry.getShort("lvl");
            boolean idMatches = this.enchantments == null;
            if (!idMatches) {
                for (int wanted : this.enchantments) {
                    if (wanted == id) {
                        idMatches = true;
                        break;
                    }
                }
            }
            if (idMatches && (this.enchantmentLevels == null || inRanges(this.enchantmentLevels, level))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesNbt(NBTBase tag, NbtCondition condition, int depth) {
        if (depth == condition.path.length) {
            final String value = tag instanceof NBTTagString string ? string.getString() : tag.toString();
            return matchesValue(condition, value) || matchesValue(condition, TextFormatting.getTextWithoutFormattingCodes(value));
        }
        final String key = condition.path[depth];
        if (tag instanceof NBTTagCompound compound) {
            final NBTBase child = compound.getTag(key);
            return child != null && matchesNbt(child, condition, depth + 1);
        }
        if (tag instanceof NBTTagList list) {
            if (key.equals("*")) {
                for (int i = 0; i < list.tagCount(); i++) {
                    if (matchesNbt(list.get(i), condition, depth + 1)) {
                        return true;
                    }
                }
                return false;
            }
            if (StringUtils.isNumeric(key)) {
                final int index = Integer.parseInt(key);
                return index < list.tagCount() && matchesNbt(list.get(index), condition, depth + 1);
            }
        }
        return false;
    }

    private static boolean matchesValue(NbtCondition condition, String value) {
        if (value == null) {
            return false;
        }
        return condition.pattern != null ? condition.pattern.matcher(value).matches() : condition.exact.equals(value);
    }

    /** OptiFine's NBT values: plain text, {@code pattern:} and {@code ipattern:} with * and ?, {@code regex:}, {@code iregex:} */
    private static NbtCondition nbtCondition(String[] path, String value) {
        if (value.startsWith("regex:")) {
            return new NbtCondition(path, Pattern.compile(value.substring(6)), null);
        }
        if (value.startsWith("iregex:")) {
            return new NbtCondition(path, Pattern.compile(value.substring(7), Pattern.CASE_INSENSITIVE), null);
        }
        if (value.startsWith("pattern:")) {
            return new NbtCondition(path, Pattern.compile(wildcard(value.substring(8))), null);
        }
        if (value.startsWith("ipattern:")) {
            return new NbtCondition(path, Pattern.compile(wildcard(value.substring(9)), Pattern.CASE_INSENSITIVE), null);
        }
        return new NbtCondition(path, null, value);
    }

    private static String wildcard(String pattern) {
        final StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                default -> regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return regex.toString();
    }

    private static int[] parseEnchantments(String list) {
        if (list.isEmpty()) {
            return null;
        }
        final List<Integer> ids = new ArrayList<>();
        for (String token : list.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (StringUtils.isNumeric(token)) {
                ids.add(Integer.parseInt(token));
            } else {
                final Enchantment enchantment = Enchantment.getEnchantmentByLocation(token.toLowerCase(Locale.ROOT));
                if (enchantment != null) {
                    ids.add(Enchantment.getEnchantmentID(enchantment));
                }
            }
        }
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    /** {@code 1 2-4 10-} lists; null when empty */
    private static List<int[]> ranges(String list) {
        if (list.isEmpty()) {
            return null;
        }
        final List<int[]> ranges = new ArrayList<>();
        for (String token : list.trim().split("\\s+")) {
            final int dash = token.indexOf('-', 1);
            try {
                if (dash < 0) {
                    final int value = Integer.parseInt(token);
                    ranges.add(new int[]{value, value});
                } else {
                    final String to = token.substring(dash + 1);
                    ranges.add(new int[]{Integer.parseInt(token.substring(0, dash)), to.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(to)});
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ranges.isEmpty() ? null : ranges;
    }

    private static boolean inRanges(List<int[]> ranges, int value) {
        for (int[] range : ranges) {
            if (value >= range[0] && value <= range[1]) {
                return true;
            }
        }
        return false;
    }

    static String get(Properties properties, String key, String fallback) {
        final String value = properties.getProperty(key);
        return value == null ? fallback : value.trim();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static float parseFloat(String value) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    static Type type(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "armor" -> Type.ARMOR;
            case "enchantment" -> Type.ENCHANTMENT;
            default -> Type.ITEM;
        };
    }

    @Override
    public int compareTo(CitRule other) {
        final int byWeight = Integer.compare(other.weight, this.weight);
        return byWeight != 0 ? byWeight : Integer.compare(this.order, other.order);
    }
}
