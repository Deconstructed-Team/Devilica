/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.randomentities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One numbered rule of an OptiFine random entities properties file: which textures to pick from, and when the rule
 * applies (biome, height, name, baby, health, weather, time, moon phase, villager profession, colour).
 */
final class RandomRule {

    private static final Pattern RULE_KEY = Pattern.compile("(?:textures|skins)\\.(\\d+)");

    final int[] textures;
    private final int[] weights;
    private final int weightSum;
    private final BitSet biomes;
    private final List<int[]> heights;
    private final Pattern name;
    private final Boolean baby;
    private final List<int[]> health;
    private final boolean healthPercent;
    private final BitSet moonPhases;
    private final List<int[]> dayTimes;
    private final boolean[] weather;
    private final BitSet professions;
    private final BitSet colors;

    private RandomRule(Properties properties, int index, int textureCount) {
        this.textures = parseIndices(value(properties, index, "textures", value(properties, index, "skins", "")), textureCount);
        this.weights = parseWeights(value(properties, index, "weights", ""), this.textures.length);
        int sum = 0;
        if (this.weights != null) {
            for (int weight : this.weights) {
                sum += weight;
            }
        }
        this.weightSum = sum;
        this.biomes = parseBiomes(value(properties, index, "biomes", ""));
        final List<int[]> heights = parseRanges(value(properties, index, "heights", ""));
        final String minHeight = value(properties, index, "minHeight", "");
        final String maxHeight = value(properties, index, "maxHeight", "");
        // Older files give one range as minHeight and maxHeight
        this.heights = heights != null || (minHeight.isEmpty() && maxHeight.isEmpty()) ? heights
                : List.of(new int[]{minHeight.isEmpty() ? Integer.MIN_VALUE : parseInt(minHeight), maxHeight.isEmpty() ? Integer.MAX_VALUE : parseInt(maxHeight)});
        this.name = parseName(value(properties, index, "name", ""));
        final String baby = value(properties, index, "baby", "");
        this.baby = baby.isEmpty() ? null : Boolean.parseBoolean(baby);
        final String health = value(properties, index, "health", "");
        this.healthPercent = health.contains("%");
        this.health = parseRanges(health.replace("%", ""));
        this.moonPhases = parseSet(value(properties, index, "moonPhase", ""));
        this.dayTimes = parseRanges(value(properties, index, "dayTime", ""));
        this.weather = parseWeather(value(properties, index, "weather", ""));
        this.professions = parseProfessions(value(properties, index, "professions", ""));
        this.colors = parseColors(value(properties, index, "colors", value(properties, index, "collarColors", "")));
    }

    /** The rules of a properties file, in their numbered order */
    static List<RandomRule> parse(Properties properties, int textureCount) {
        final TreeSet<Integer> indices = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            final Matcher matcher = RULE_KEY.matcher(key);
            if (matcher.matches()) {
                indices.add(Integer.parseInt(matcher.group(1)));
            }
        }
        final List<RandomRule> rules = new ArrayList<>();
        for (int index : indices) {
            final RandomRule rule = new RandomRule(properties, index, textureCount);
            if (rule.textures.length > 0) {
                rules.add(rule);
            }
        }
        return rules;
    }

    boolean matches(Entity entity) {
        final World world = entity.world;
        final BlockPos pos = new BlockPos(entity);
        if (this.biomes != null && !this.biomes.get(Biome.getIdForBiome(world.getBiome(pos)))) {
            return false;
        }
        if (this.heights != null && !inRanges(this.heights, pos.getY())) {
            return false;
        }
        if (this.name != null && (!entity.hasCustomName() || !this.name.matcher(entity.getCustomNameTag()).matches())) {
            return false;
        }
        if (this.baby != null && (!(entity instanceof EntityLivingBase living) || living.isChild() != this.baby)) {
            return false;
        }
        if (this.health != null) {
            if (!(entity instanceof EntityLivingBase living)) {
                return false;
            }
            final int value = this.healthPercent ? (int) (living.getHealth() * 100.0F / Math.max(1.0F, living.getMaxHealth())) : (int) living.getHealth();
            if (!inRanges(this.health, value)) {
                return false;
            }
        }
        if (this.moonPhases != null && !this.moonPhases.get(world.provider.getMoonPhase(world.getWorldTime()))) {
            return false;
        }
        if (this.dayTimes != null && !inRanges(this.dayTimes, (int) (world.getWorldTime() % 24000L))) {
            return false;
        }
        if (this.weather != null) {
            final int now = world.isThundering() ? 2 : world.isRaining() ? 1 : 0;
            if (!this.weather[now]) {
                return false;
            }
        }
        if (this.professions != null && (!(entity instanceof EntityVillager villager) || !this.professions.get(villager.getProfession()))) {
            return false;
        }
        if (this.colors != null) {
            final EnumDyeColor color = entity instanceof EntitySheep sheep ? sheep.getFleeceColor()
                    : entity instanceof EntityWolf wolf ? wolf.getCollarColor() : null;
            if (color == null || !this.colors.get(color.getMetadata())) {
                return false;
            }
        }
        return true;
    }

    /** One of the rule's textures (1 is the original) for an entity's ID */
    int choose(int id) {
        if (this.weights == null || this.weightSum <= 0) {
            return this.textures[id % this.textures.length];
        }
        int m = id % this.weightSum;
        for (int i = 0; i < this.textures.length; i++) {
            m -= this.weights[i];
            if (m < 0) {
                return this.textures[i];
            }
        }
        return this.textures[this.textures.length - 1];
    }

    // ---- parsing

    private static String value(Properties properties, int index, String key, String fallback) {
        final String value = properties.getProperty(key + "." + index);
        return value == null ? fallback : value.trim();
    }

    private static int[] parseIndices(String list, int textureCount) {
        final List<Integer> indices = new ArrayList<>();
        for (int[] range : parseRangesOrEmpty(list)) {
            for (int i = range[0]; i <= Math.min(range[1], 1000); i++) {
                if (i >= 1 && i <= textureCount) {
                    indices.add(i);
                }
            }
        }
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] parseWeights(String list, int count) {
        if (list.isEmpty()) {
            return null;
        }
        final String[] parts = list.split("\\s+");
        final int[] weights = new int[count];
        for (int i = 0; i < count; i++) {
            weights[i] = i < parts.length ? Math.max(0, parseInt(parts[i])) : 1;
        }
        return weights;
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

    private static List<int[]> parseRanges(String list) {
        if (list.isEmpty()) {
            return null;
        }
        final List<int[]> ranges = parseRangesOrEmpty(list);
        return ranges.isEmpty() ? null : ranges;
    }

    /** {@code 1 2-4 10-} style lists, negative numbers in parentheses as OptiFine writes them */
    private static List<int[]> parseRangesOrEmpty(String list) {
        final List<int[]> ranges = new ArrayList<>();
        for (String token : list.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            final String plain = token.replace("(", "").replace(")", "");
            final int dash = plain.indexOf('-', 1);
            try {
                if (dash < 0) {
                    final int value = Integer.parseInt(plain);
                    ranges.add(new int[]{value, value});
                } else {
                    final String from = plain.substring(0, dash);
                    final String to = plain.substring(dash + 1);
                    ranges.add(new int[]{from.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(from), to.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(to)});
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ranges;
    }

    private static BitSet parseSet(String list) {
        final List<int[]> ranges = parseRanges(list);
        if (ranges == null) {
            return null;
        }
        final BitSet set = new BitSet();
        for (int[] range : ranges) {
            for (int i = Math.max(0, range[0]); i <= Math.min(range[1], 64); i++) {
                set.set(i);
            }
        }
        return set;
    }

    private static boolean[] parseWeather(String list) {
        if (list.isEmpty()) {
            return null;
        }
        final boolean[] weather = new boolean[3];
        for (String token : list.toLowerCase(Locale.ROOT).split("\\s+")) {
            switch (token) {
                case "clear" -> weather[0] = true;
                case "rain" -> weather[1] = true;
                case "thunder" -> weather[2] = true;
                default -> {
                }
            }
        }
        return weather;
    }

    private static BitSet parseProfessions(String list) {
        if (list.isEmpty()) {
            return null;
        }
        final BitSet professions = new BitSet();
        final String[] names = {"farmer", "librarian", "priest", "smith", "butcher", "nitwit"};
        for (String token : list.toLowerCase(Locale.ROOT).split("\\s+")) {
            final String name = token.contains(":") ? token.substring(0, token.indexOf(':')) : token;
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(name.replace("minecraft:", ""))) {
                    professions.set(i);
                }
            }
            if (name.matches("\\d+")) {
                professions.set(Integer.parseInt(name));
            }
        }
        return professions;
    }

    private static BitSet parseColors(String list) {
        if (list.isEmpty()) {
            return null;
        }
        final BitSet colors = new BitSet();
        for (String token : list.toLowerCase(Locale.ROOT).split("\\s+")) {
            for (EnumDyeColor color : EnumDyeColor.values()) {
                if (color.getName().equals(token) || color.getTranslationKey().equalsIgnoreCase(token)) {
                    colors.set(color.getMetadata());
                }
            }
        }
        return colors;
    }

    /** OptiFine's name matching: plain text, {@code pattern:} and {@code ipattern:} with * and ?, {@code regex:}, {@code iregex:} */
    private static Pattern parseName(String value) {
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("regex:")) {
            return Pattern.compile(value.substring(6));
        }
        if (value.startsWith("iregex:")) {
            return Pattern.compile(value.substring(7), Pattern.CASE_INSENSITIVE);
        }
        if (value.startsWith("pattern:")) {
            return Pattern.compile(wildcard(value.substring(8)));
        }
        if (value.startsWith("ipattern:")) {
            return Pattern.compile(wildcard(value.substring(9)), Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(Pattern.quote(value));
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

    private static boolean inRanges(List<int[]> ranges, int value) {
        for (int[] range : ranges) {
            if (value >= range[0] && value <= range[1]) {
                return true;
            }
        }
        return false;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
