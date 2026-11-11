package net.coderbot.iris.gl.shader;

import com.google.common.collect.ImmutableList;
import dev.feldsher.devilica.Devilica;
import com.gtnewhorizons.angelica.config.AngelicaConfig;
import net.minecraftforge.fml.common.Loader;
import net.coderbot.iris.parsing.BiomeCategories;
import net.coderbot.iris.pipeline.HandRenderer;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.shaderpack.IrisLimits;
import net.coderbot.iris.shaderpack.StringPair;
import net.coderbot.iris.texture.format.TextureFormat;
import net.coderbot.iris.texture.format.TextureFormatLoader;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import org.taumc.celeritas.CeleritasVintage;
import org.lwjgl.system.Platform;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StandardMacros {
	private static final Pattern SEMVER_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.*(?<bugfix>\\d*)(.*)");
	private static final String ANGELICA_BASE_VERSION = "20000025";

	private static void define(List<StringPair> defines, String key) {
		defines.add(new StringPair(key, ""));
	}

	private static void define(List<StringPair> defines, String key, String value) {
		defines.add(new StringPair(key, value));
	}

    /**
     * major, minor, patch and the number of a pre-release part ("dev.12" -> 12) as MMmmppsss, without leading zeros:
     * the preprocessor reads 0-prefixed numbers as octal.
     */
    private static String makeDevilicaVersion()
    {
        String[] parts = Devilica.VERSION.split("[.-]");
        int[] numbers = new int[4];
        int found = 0;
        for (String part : parts) {
            String digits = part.replaceAll("\\D+", "");
            if (!digits.isEmpty() && found < numbers.length) {
                numbers[found++] = Integer.parseInt(digits);
            }
        }
        return String.valueOf(numbers[0] * 10_000_000L + numbers[1] * 100_000L + numbers[2] * 1_000L + numbers[3]);
    }

	public static Iterable<StringPair> createStandardEnvironmentDefines() {
		ArrayList<StringPair> standardDefines = new ArrayList<>();

		define(standardDefines, "MC_VERSION", getMcVersion());
		define(standardDefines, "MC_GL_VERSION", getGlVersion(GL11.GL_VERSION));
		define(standardDefines, "MC_GLSL_VERSION", getGlVersion(GL20.GL_SHADING_LANGUAGE_VERSION));
		define(standardDefines, getOsString());
		define(standardDefines, getVendor());
		define(standardDefines, getRenderer());

		for (String glExtension : getGlExtensions()) {
			define(standardDefines, glExtension);
		}

		define(standardDefines, "MC_NORMAL_MAP");
		define(standardDefines, "MC_SPECULAR_MAP");
		define(standardDefines, "MC_RENDER_QUALITY", "1.0");
		define(standardDefines, "MC_SHADOW_QUALITY", "1.0");
        define(standardDefines, "IS_DEVILICA");
        // Packs gate Angelica's features by its version (1.7.10 itself comes from MC_VERSION): this is the Angelica
        // Devilica's Iris was taken from, so features of later Angelica versions stay off.
        define(standardDefines, "IS_ANGELICA");
        define(standardDefines, "ANGELICA_VERSION", ANGELICA_BASE_VERSION);
        if (AngelicaConfig.defineIsIris) {
            define(standardDefines, "IS_IRIS");
            // What Angelica reports, so packs take the paths they were checked with there.
            define(standardDefines, "IRIS_VERSION", "10803");
            define(standardDefines, "MAX_COLOR_BUFFERS", String.valueOf(IrisLimits.MAX_COLOR_BUFFERS));
        }
        if (CeleritasVintage.options().performance.useTranslucentFaceSorting) {
            define(standardDefines, "IRIS_HAS_TRANSLUCENCY_SORTING");
        }
        define(standardDefines, "DEVILICA_VERSION", makeDevilicaVersion());
		define(standardDefines, "MC_HAND_DEPTH", Float.toString(HandRenderer.DEPTH));

		TextureFormat textureFormat = TextureFormatLoader.getFormat();
		if (textureFormat != null) {
			for (String define : textureFormat.getDefines()) {
				define(standardDefines, define);
			}
		}

		getRenderStages().forEach((stage, index) -> define(standardDefines, stage, index));

		for (String irisDefine : getIrisDefines()) {
			define(standardDefines, irisDefine);
		}

		getBiomeCategoryDefines().forEach((category, ordinal) -> define(standardDefines, category, ordinal));
		getVanillaBiomeDefines().forEach((biome, id) -> define(standardDefines, biome, id));

		return ImmutableList.copyOf(standardDefines);
	}

	/**
	 * Gets the current mc version String in a 5 digit format
	 *
	 * @return mc version string
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L696-L699">Optifine Doc</a>
	 */
	public static String getMcVersion() {
		final String version = Loader.MC_VERSION;

		String[] splitVersion = version.split("\\.");

		if (splitVersion.length < 2) {
			throw new IllegalStateException("Could not parse game version \"" + version +  "\"");
		}

		String major = splitVersion[0];
		String minor = splitVersion[1];
		String bugfix;

		if (splitVersion.length < 3) {
			bugfix = "00";
		} else {
			bugfix = splitVersion[2];
		}

		if (minor.length() == 1) {
			minor = "0" + minor;
		}
		if (bugfix.length() == 1) {
			bugfix = "0" + bugfix;
		}

		return major + minor + bugfix;
	}

	/**
	 * Returns the current GL Version using regex
	 *
	 * @param name the name of the gl attribute to parse
	 * @return current gl version stripped of semantic versioning
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L701-L703">Optifine Doc for GL Version</a>
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L705-L707">Optifine Doc for GLSL Version</a>
	 */
	public static String getGlVersion(int name) {
		final String info = GL11.glGetString(name);

		Matcher matcher = SEMVER_PATTERN.matcher(Objects.requireNonNull(info));

		if (!matcher.matches()) {
			throw new IllegalStateException("Could not parse GL version from \"" + info + "\"");
		}

		String major = group(matcher, "major");
		String minor = group(matcher, "minor");
		String bugfix = group(matcher, "bugfix");

		if (bugfix == null) {
			// if bugfix is not there, it is 0
			bugfix = "0";
		}

		if (major == null || minor == null) {
			throw new IllegalStateException("Could not parse GL version from \"" + info + "\"");
		}

		return major + minor + bugfix;
	}

	/**
	 * Expanded version of {@link Matcher#group(String)} that does not throw an exception.
	 * If the argument is incorrect (normally resulting in an exception), it returns null
	 *
	 * @param matcher matcher to check the group by
	 * @param name    name of the group
	 * @return the section of the matcher that is a group, or null, if that matcher does not contain said group
	 */
	public static String group(Matcher matcher, String name) {
		try {
			return matcher.group(name);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			return null;
		}
	}

	/**
	 * Returns the current OS String
	 *
	 * @return the string based on the current OS
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L709-L714">Optifine Doc</a>
	 */
	public static String getOsString() {
        return switch (Platform.get()) {
            case MACOSX -> "MC_OS_MAC";
            case LINUX, FREEBSD -> "MC_OS_LINUX";
            case WINDOWS -> "MC_OS_WINDOWS";
        };
	}

	/**
	 * Returns a string indicating the graphics card being used
	 *
	 * @return the graphics card prefixed with "MC_GL_VENDOR_"
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L716-L723">Optifine Doc</a>
	 */
	public static String getVendor() {
		String vendor = Objects.requireNonNull(GL11.glGetString(GL11.GL_VENDOR)).toLowerCase(Locale.ROOT);
		if (vendor.startsWith("ati")) {
			return "MC_GL_VENDOR_ATI";
		} else if (vendor.startsWith("intel")) {
			return "MC_GL_VENDOR_INTEL";
		} else if (vendor.startsWith("nvidia")) {
			return "MC_GL_VENDOR_NVIDIA";
		} else if (vendor.startsWith("amd")) {
			return "MC_GL_VENDOR_AMD";
		} else if (vendor.startsWith("x.org")) {
			return "MC_GL_VENDOR_XORG";
		}
		return "MC_GL_VENDOR_OTHER";
	}

	/**
	 * Returns the graphics driver being used
	 *
	 * @return graphics driver prefixed with "MC_GL_RENDERER_"
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L725-L733">Optifine Doc</a>
	 */
	public static String getRenderer() {
		String renderer = Objects.requireNonNull(GL11.glGetString(GL11.GL_RENDERER)).toLowerCase(Locale.ROOT);
		if (renderer.startsWith("amd")) {
			return "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("ati")) {
			return "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("radeon")) {
			return "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("gallium")) {
			return "MC_GL_RENDERER_GALLIUM";
		} else if (renderer.startsWith("intel")) {
			return "MC_GL_RENDERER_INTEL";
		} else if (renderer.startsWith("geforce")) {
			return "MC_GL_RENDERER_GEFORCE";
		} else if (renderer.startsWith("nvidia")) {
			return "MC_GL_RENDERER_GEFORCE";
		} else if (renderer.startsWith("quadro")) {
			return "MC_GL_RENDERER_QUADRO";
		} else if (renderer.startsWith("nvs")) {
			return "MC_GL_RENDERER_QUADRO";
		} else if (renderer.startsWith("mesa")) {
			return "MC_GL_RENDERER_MESA";
		}
		return "MC_GL_RENDERER_OTHER";
	}

	/**
	 * Returns the list of currently enabled GL extensions
	 * This is done by calling {@link GL11#glGetString} with the arg {@link GL11#GL_EXTENSIONS}
	 *
	 * @return list of activated extensions prefixed with "MC_"
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L735-L738">Optifine Doc</a>
	 */
	public static Set<String> getGlExtensions() {
		String[] extensions = Objects.requireNonNull(GL11.glGetString(GL11.GL_EXTENSIONS)).split("\\s+");

		// TODO note that we do not add extensions based on if the shader uses them and if they are supported
		// see https://github.com/sp614x/optifine/blob/master/OptiFineDoc/doc/shaders.txt#L738

		// NB: Use Collectors.toSet(). In some cases, there are duplicate extensions in the extension list.
		// RenderDoc is one example - it causes the GL_KHR_debug extension to appear twice:

		return Arrays.stream(extensions).map(s -> "MC_" + s).collect(Collectors.toSet());
	}

	public static Map<String, String> getRenderStages() {
		Map<String, String> stages = new HashMap<>();
		for (WorldRenderingPhase phase : WorldRenderingPhase.values()) {
			stages.put("MC_RENDER_STAGE_" + phase.name(), String.valueOf(phase.ordinal()));
		}
		return stages;
	}

	/**
	 * Returns the list of Iris-exclusive uniforms supported in the current version of Iris.
	 *
	 * @return List of definitions corresponding to the uniform names prefixed with "MC_"
	 */
	public static List<String> getIrisDefines() {
		List<String> defines = new ArrayList<>();
		// All Iris-exclusive uniforms should have a corresponding definition here. Example:
		// defines.add("MC_UNIFORM_DRAGON_DEATH_PROGRESS");

		return defines;
	}

	/**
	 * Returns biome category defines for use in shaders.
	 * Generates #define CAT_<CATEGORY_NAME> <ordinal> for each BiomeCategories enum value.
	 *
	 * @return Map of biome category defines and their ordinal values
	 */
	public static Map<String, String> getBiomeCategoryDefines() {
		Map<String, String> defines = new HashMap<>();
		for (BiomeCategories category : BiomeCategories.values()) {
			defines.put("CAT_" + category.name(), String.valueOf(category.ordinal()));
		}
		return defines;
	}

	/**
	 * Returns vanilla biome ID defines for use in shaders.
	 * Generates #define BIOME_<BIOME_NAME> <biomeID> for all vanilla biomes.
	 *
	 * @return Map of vanilla biome defines and their IDs
	 */
	public static Map<String, String> getVanillaBiomeDefines() {
		final Map<String, String> defines = new HashMap<>();

		// Devilica: every registered biome as BIOME_<registry path>, as OptiFine 1.12.2 defines them; vanilla ones first
		// so a mod biome with the same path does not take a vanilla name
		for (Biome biome : Biome.REGISTRY) {
			final ResourceLocation name = biome.getRegistryName();
			if (name != null && "minecraft".equals(name.getNamespace())) {
				defines.put("BIOME_" + name.getPath().toUpperCase(Locale.ROOT), String.valueOf(Biome.getIdForBiome(biome)));
			}
		}
		for (Biome biome : Biome.REGISTRY) {
			final ResourceLocation name = biome.getRegistryName();
			if (name != null) {
				defines.putIfAbsent("BIOME_" + name.getPath().toUpperCase(Locale.ROOT), String.valueOf(Biome.getIdForBiome(biome)));
			}
		}

		// Names newer versions and Angelica's 1.7.10 list use for vanilla 1.12.2 biomes
		addModernBiomeAlias(defines, "SWAMP", Biomes.SWAMPLAND);
		addModernBiomeAlias(defines, "SWAMP_HILLS", Biomes.MUTATED_SWAMPLAND);
		addModernBiomeAlias(defines, "EXTREME_HILLS_EDGE", Biomes.EXTREME_HILLS_EDGE);
		addModernBiomeAlias(defines, "EXTREME_HILLS_PLUS", Biomes.EXTREME_HILLS_WITH_TREES);
		addModernBiomeAlias(defines, "MUSHROOM_ISLAND", Biomes.MUSHROOM_ISLAND);
		addModernBiomeAlias(defines, "MUSHROOM_ISLAND_SHORE", Biomes.MUSHROOM_ISLAND_SHORE);
		addModernBiomeAlias(defines, "ICE_PLAINS", Biomes.ICE_PLAINS);
		addModernBiomeAlias(defines, "ICE_MOUNTAINS", Biomes.ICE_MOUNTAINS);
		addModernBiomeAlias(defines, "COLD_BEACH", Biomes.COLD_BEACH);
		addModernBiomeAlias(defines, "COLD_TAIGA", Biomes.COLD_TAIGA);
		addModernBiomeAlias(defines, "COLD_TAIGA_HILLS", Biomes.COLD_TAIGA_HILLS);
		addModernBiomeAlias(defines, "MEGA_TAIGA", Biomes.REDWOOD_TAIGA);
		addModernBiomeAlias(defines, "MEGA_TAIGA_HILLS", Biomes.REDWOOD_TAIGA_HILLS);
		addModernBiomeAlias(defines, "MESA_PLATEAU_F", Biomes.MESA_ROCK);
		addModernBiomeAlias(defines, "MESA_PLATEAU", Biomes.MESA_CLEAR_ROCK);
		addModernBiomeAlias(defines, "SAVANNA_PLATEAU", Biomes.SAVANNA_PLATEAU);
		addModernBiomeAlias(defines, "ROOFED_FOREST", Biomes.ROOFED_FOREST);
		addModernBiomeAlias(defines, "DEEP_OCEAN", Biomes.DEEP_OCEAN);
		addModernBiomeAlias(defines, "STONE_BEACH", Biomes.STONE_BEACH);
		addModernBiomeAlias(defines, "THE_END", Biomes.SKY);
		addModernBiomeAlias(defines, "SMALL_END_ISLANDS", Biomes.SKY);
		addModernBiomeAlias(defines, "END_MIDLANDS", Biomes.SKY);
		addModernBiomeAlias(defines, "END_HIGHLANDS", Biomes.SKY);
		addModernBiomeAlias(defines, "END_BARRENS", Biomes.SKY);

		// Modern biomes that don't exist in 1.12.2 - add dummy IDs that will never match
		// This allows shader expressions to parse without errors while always evaluating to false/0
		// Using negative IDs to avoid conflicts with actual biome IDs
		defines.put("BIOME_NETHER_WASTES", "-1000");
		defines.put("BIOME_SOUL_SAND_VALLEY", "-1001");
		defines.put("BIOME_CRIMSON_FOREST", "-1002");
		defines.put("BIOME_WARPED_FOREST", "-1003");
		defines.put("BIOME_BASALT_DELTAS", "-1004");
		defines.put("BIOME_LUSH_CAVES", "-1005");
		defines.put("BIOME_PALE_GARDEN", "-1006");

		return defines;
	}

	private static void addModernBiomeAlias(Map<String, String> defines, String modernName, Biome biome) {
		if (biome != null) {
			defines.putIfAbsent("BIOME_" + modernName, String.valueOf(Biome.getIdForBiome(biome)));
		}
	}
}
