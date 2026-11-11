package net.coderbot.iris.uniforms;

import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.parsing.BiomeCategories;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.coderbot.iris.gl.uniform.UniformUpdateFrequency.PER_TICK;

/**
 * Devilica: the biome uniforms for 1.12.2. Biome IDs are unique here, so {@code biome} is the registry ID, as in
 * OptiFine. {@code biome_category} (OptiFine's names, a 1.16 notion) is worked out once per biome: vanilla IDs, then
 * Forge's {@link BiomeDictionary}, then the registry name, then temperature and rainfall.
 */
public class BiomeUniforms {

    private static final Minecraft client = Minecraft.getMinecraft();
    private static final Map<Biome, BiomeCategories> CATEGORIES = new IdentityHashMap<>();

    private static Biome cachedBiome;
    private static long cachedWorldTime = -1;
    private static BlockPos cachedPos;

    public static void addBiomeUniforms(UniformHolder uniforms) {
        uniforms.uniform1i(PER_TICK, "biome", BiomeUniforms::getBiomeId)
                .uniform1i(PER_TICK, "biome_category", BiomeUniforms::getBiomeCategory)
                .uniform1i(PER_TICK, "biome_precipitation", BiomeUniforms::getBiomePrecipitation)
                .uniform1f(PER_TICK, "rainfall", BiomeUniforms::getBiomeRainfall)
                .uniform1f(PER_TICK, "temperature", BiomeUniforms::getBiomeTemperature);
    }

    private static Biome getCachedBiome() {
        if (client.player == null || client.world == null) {
            return null;
        }
        final long worldTime = client.world.getTotalWorldTime();
        final BlockPos pos = new BlockPos(client.player);
        if (cachedBiome == null || cachedWorldTime != worldTime || !pos.equals(cachedPos)) {
            cachedBiome = client.world.getBiome(pos);
            cachedWorldTime = worldTime;
            cachedPos = pos;
        }
        return cachedBiome;
    }

    public static int getBiomePrecipitation() {
        final Biome biome = getCachedBiome();
        if (biome == null || (!biome.canRain() && !biome.getEnableSnow())) {
            return 0;
        }
        return biome.getTemperature(cachedPos) > 0.15F ? 1 : 2;
    }

    public static float getBiomeRainfall() {
        final Biome biome = getCachedBiome();
        return biome != null ? biome.getRainfall() : 0.0F;
    }

    public static float getBiomeTemperature() {
        final Biome biome = getCachedBiome();
        return biome != null ? biome.getDefaultTemperature() : 0.0F;
    }

    public static int getBiomeId() {
        final Biome biome = getCachedBiome();
        return biome != null ? Biome.getIdForBiome(biome) : 0;
    }

    public static int getBiomeCategory() {
        final Biome biome = getCachedBiome();
        if (biome == null) {
            return BiomeCategories.NONE.ordinal();
        }
        return CATEGORIES.computeIfAbsent(biome, BiomeUniforms::determineBiomeCategory).ordinal();
    }

    private static BiomeCategories determineBiomeCategory(Biome biome) {
        Biome lookup = biome.isMutation() ? Biome.MUTATION_TO_BASE_ID_MAP.getByValue(Biome.getIdForBiome(biome)) : biome;
        if (lookup == null) {
            lookup = biome;
        }
        ResourceLocation name = lookup.getRegistryName();
        if (name != null && "minecraft".equals(name.getNamespace())) {
            BiomeCategories vanilla = getVanillaBiomeCategory(Biome.getIdForBiome(lookup));
            if (vanilla != null) {
                return vanilla;
            }
        }
        BiomeCategories category = detectBiomeByBiomeDictionary(biome);
        if (category == null) {
            category = detectBiomeByName(biome);
        }
        if (category == null) {
            category = detectBiomeByProperties(biome);
        }
        return category != null ? category : BiomeCategories.NONE;
    }

    private static BiomeCategories getVanillaBiomeCategory(int biomeID) {
        return switch (biomeID) {
            case 0, 10, 24 -> BiomeCategories.OCEAN;
            case 1 -> BiomeCategories.PLAINS;
            case 2, 17 -> BiomeCategories.DESERT;
            case 3, 20, 34 -> BiomeCategories.EXTREME_HILLS;
            case 4, 18, 27, 28, 29 -> BiomeCategories.FOREST;
            case 5, 19, 30, 31, 32, 33 -> BiomeCategories.TAIGA;
            case 6 -> BiomeCategories.SWAMP;
            case 7, 11 -> BiomeCategories.RIVER;
            case 8 -> BiomeCategories.NETHER;
            case 9 -> BiomeCategories.THE_END;
            case 12, 13 -> BiomeCategories.ICY;
            case 14, 15 -> BiomeCategories.MUSHROOM;
            case 16, 25, 26 -> BiomeCategories.BEACH;
            case 21, 22, 23 -> BiomeCategories.JUNGLE;
            case 35, 36 -> BiomeCategories.SAVANNA;
            case 37, 38, 39 -> BiomeCategories.MESA;
            case 127 -> BiomeCategories.NONE; // the void
            default -> null;
        };
    }

    private static BiomeCategories detectBiomeByBiomeDictionary(Biome biome) {
        if (!BiomeDictionary.hasAnyType(biome)) {
            return null;
        }
        final Set<BiomeDictionary.Type> types = BiomeDictionary.getTypes(biome);
        if (types.contains(BiomeDictionary.Type.SAVANNA)) return BiomeCategories.SAVANNA;
        if (types.contains(BiomeDictionary.Type.JUNGLE)) return BiomeCategories.JUNGLE;
        if (types.contains(BiomeDictionary.Type.NETHER)) return BiomeCategories.NETHER;
        if (types.contains(BiomeDictionary.Type.END)) return BiomeCategories.THE_END;
        if (types.contains(BiomeDictionary.Type.MUSHROOM)) return BiomeCategories.MUSHROOM;
        if (types.contains(BiomeDictionary.Type.OCEAN)) return BiomeCategories.OCEAN;
        if (types.contains(BiomeDictionary.Type.RIVER)) return BiomeCategories.RIVER;
        if (types.contains(BiomeDictionary.Type.BEACH)) return BiomeCategories.BEACH;
        if (types.contains(BiomeDictionary.Type.MESA)) return BiomeCategories.MESA;
        if (types.contains(BiomeDictionary.Type.SWAMP)) return BiomeCategories.SWAMP;
        if (types.contains(BiomeDictionary.Type.SNOWY) && !types.contains(BiomeDictionary.Type.FOREST)) return BiomeCategories.ICY;
        if (types.contains(BiomeDictionary.Type.CONIFEROUS)) return BiomeCategories.TAIGA;
        if (types.contains(BiomeDictionary.Type.FOREST)) return BiomeCategories.FOREST;
        if (types.contains(BiomeDictionary.Type.MOUNTAIN) || types.contains(BiomeDictionary.Type.HILLS)) return BiomeCategories.EXTREME_HILLS;
        if (types.contains(BiomeDictionary.Type.PLAINS)) return BiomeCategories.PLAINS;
        if (types.contains(BiomeDictionary.Type.SANDY)) return BiomeCategories.DESERT;
        return null;
    }

    private static BiomeCategories detectBiomeByName(Biome biome) {
        final ResourceLocation id = biome.getRegistryName();
        final String name = (id != null ? id.getPath() : biome.getBiomeName()).toLowerCase(Locale.ROOT);

        if (name.contains("nether") || name.contains("hell")) return BiomeCategories.NETHER;
        if (name.contains("end")) return BiomeCategories.THE_END;
        if (name.contains("ocean") || name.contains("sea")) return BiomeCategories.OCEAN;
        if (name.contains("river") || name.contains("stream")) return BiomeCategories.RIVER;
        if (name.contains("beach") || name.contains("shore") || name.contains("coast")) return BiomeCategories.BEACH;
        if (name.contains("mushroom")) return BiomeCategories.MUSHROOM;
        if (name.contains("swamp") || name.contains("marsh") || name.contains("bog")) return BiomeCategories.SWAMP;
        if (name.contains("jungle")) return BiomeCategories.JUNGLE;
        if (name.contains("savanna")) return BiomeCategories.SAVANNA;
        if (name.contains("mesa") || name.contains("badlands")) return BiomeCategories.MESA;
        if (name.contains("desert")) return BiomeCategories.DESERT;
        if (name.contains("mountain") || name.contains("peak") || name.contains("alpine") || name.contains("cliff") || name.contains("crag")) return BiomeCategories.MOUNTAIN;
        if (name.contains("ice") || name.contains("frozen") || name.contains("snow") || name.contains("arctic") || name.contains("tundra") || name.contains("glacier")) return BiomeCategories.ICY;
        if (name.contains("taiga") || name.contains("boreal") || name.contains("conifer")) return BiomeCategories.TAIGA;
        if (name.contains("forest") || name.contains("wood") || name.contains("grove") || name.contains("thicket")) return BiomeCategories.FOREST;
        if (name.contains("plain") || name.contains("field") || name.contains("meadow") || name.contains("grassland") || name.contains("prairie")) return BiomeCategories.PLAINS;
        return null;
    }

    private static BiomeCategories detectBiomeByProperties(Biome biome) {
        final float temp = biome.getDefaultTemperature();
        final float rain = biome.getRainfall();
        if (temp <= 0.0F && biome.getEnableSnow()) return BiomeCategories.ICY;
        if (temp >= 2.0F && !biome.canRain()) return BiomeCategories.DESERT;
        if (temp >= 1.0F && rain <= 0.1F && !biome.canRain()) return BiomeCategories.SAVANNA;
        if (rain >= 0.85F && temp >= 0.5F && temp <= 1.0F) return BiomeCategories.SWAMP;
        if (temp >= 0.0F && temp <= 0.4F && rain >= 0.4F) return BiomeCategories.TAIGA;
        if (temp >= 0.4F && temp <= 0.9F && rain >= 0.5F) return BiomeCategories.FOREST;
        if (temp >= 0.4F && temp <= 1.0F && rain >= 0.3F && rain <= 0.6F) return BiomeCategories.PLAINS;
        return null;
    }
}
