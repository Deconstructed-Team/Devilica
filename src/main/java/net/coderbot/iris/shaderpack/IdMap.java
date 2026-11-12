package net.coderbot.iris.shaderpack;

import com.gtnewhorizons.angelica.config.AngelicaConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.materialmap.BlockEntry;
import net.coderbot.iris.shaderpack.materialmap.BlockRenderType;
import net.coderbot.iris.shaderpack.materialmap.EntityFlatteningMap;
import net.coderbot.iris.shaderpack.materialmap.FlatteningMap;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.coderbot.iris.shaderpack.materialmap.PropertiesTokenizer;
import net.coderbot.iris.shaderpack.option.ShaderPackOptions;
import net.coderbot.iris.shaderpack.preprocessor.PropertiesPreprocessor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * A utility class for parsing entries in item.properties, block.properties, and entities.properties files in shaderpacks
 */
public class IdMap {
	/**
	 * Maps a given item ID to an integer ID
	 */
	private final Object2IntMap<NamespacedId> itemIdMap;

	/**
	 * Maps a given entity ID to an integer ID
	 */
	private final Object2IntMap<NamespacedId> entityIdMap;

	/**
	 * Maps block states to block ids defined in block.properties
	 */
	private Int2ObjectMap<List<BlockEntry>> blockPropertiesMap;

	/**
	 * A set of render type overrides for specific blocks. Allows shader packs to move blocks to different render types.
	 */
	private Map<NamespacedId, BlockRenderType> blockRenderTypeMap;

	/**
	 * True when block.properties is read as the game names blocks before the flattening.
	 *
	 * Devilica: Angelica looks for a 1.7.10 section. A pack's section for 1.12.2 is most often the #else of its
	 * 1.13 check, so here the file is read with the game's MC_VERSION and with 1.13's: entries only the former
	 * gives are a section for this version. Without one, the file is read as a modern one only if it names a
	 * block by a name the flattening brought, so that a pack written before 1.13 keeps its reading.
	 */
	private final boolean hasLegacySection;

	public boolean hasLegacySection() {
		return hasLegacySection;
	}

	private static final String FLATTENING_MC_VERSION = "11300";

	public static String modernFallbackMcVersion() {
		return AngelicaConfig.modernFallbackMcVersion > 0
			? String.valueOf(AngelicaConfig.modernFallbackMcVersion) : "260101";
	}

	// Devilica: the modern MC_VERSION is for these three files only. Shader sources and shaders.properties keep the
	// game's version: Angelica runs the modern branches of a pack on its core profile, Devilica runs the 1.12 ones.
	IdMap(Path shaderPath, ShaderPackOptions shaderPackOptions, Iterable<StringPair> environmentDefines) {
		Iterable<StringPair> resolvedDefines = environmentDefines;
		Properties blockProperties = null;
		boolean legacy = true;

		final String blockSource = readProperties(shaderPath, "block.properties");
		if (blockSource != null) {
			blockProperties = loadProperties(blockSource, shaderPath, "block.properties", shaderPackOptions, environmentDefines).orElse(null);

			if (blockProperties != null && !detectLegacySection(blockSource, blockProperties, shaderPath, shaderPackOptions, environmentDefines)) {
				// No section for this version, use modern MC_VERSION and convert entries
				final List<StringPair> modernDefines = withMcVersion(environmentDefines, modernFallbackMcVersion());
				final Properties modernProperties = loadProperties(blockSource, shaderPath, "block.properties", shaderPackOptions, modernDefines).orElse(null);

				if (modernProperties != null && namesModernBlocks(modernProperties)) {
					Iris.logger.info("block.properties has no section for this version, reading the ID maps as MC_VERSION " + modernFallbackMcVersion());
					blockProperties = modernProperties;
					resolvedDefines = modernDefines;
					legacy = false;
				}
			}
		}

		this.hasLegacySection = legacy;

		if (blockProperties != null) {
			blockPropertiesMap = parseBlockMap(blockProperties, "block.", "block.properties");
			blockRenderTypeMap = parseRenderTypeMap(blockProperties, "layer.", "block.properties");
		}

		itemIdMap = loadProperties(shaderPath, "item.properties", shaderPackOptions, resolvedDefines).map(IdMap::parseItemIdMap).orElse(Object2IntMaps.emptyMap());

		entityIdMap = loadProperties(shaderPath, "entity.properties", shaderPackOptions, resolvedDefines).map(IdMap::parseEntityIdMap)
			.map(parsed -> hasLegacySection ? parsed : augmentEntityIdMap(parsed)).orElse(Object2IntMaps.emptyMap());

		// TODO: Properly override block render layers

		if (blockPropertiesMap == null) {
			// Fill in with default values...
			blockPropertiesMap = new Int2ObjectOpenHashMap<>();
			LegacyIdMap.addLegacyValues(blockPropertiesMap);
		}

		if (blockRenderTypeMap == null) {
			blockRenderTypeMap = Collections.emptyMap();
		}
	}

	/**
	 * Loads properties from a properties file in a shaderpack path
	 */
	private static Optional<Properties> loadProperties(Path shaderPath, String name, ShaderPackOptions shaderPackOptions,
													   Iterable<StringPair> environmentDefines) {
		String fileContents = readProperties(shaderPath, name);
		if (fileContents == null) {
			return Optional.empty();
		}

		return loadProperties(fileContents, shaderPath, name, shaderPackOptions, environmentDefines);
	}

	private static Optional<Properties> loadProperties(String fileContents, Path shaderPath, String name, ShaderPackOptions shaderPackOptions,
													   Iterable<StringPair> environmentDefines) {
		String processed = PropertiesPreprocessor.preprocessSource(fileContents, shaderPackOptions, environmentDefines);

		StringReader propertiesReader = new StringReader(processed);

		// Note: ordering of properties is significant
		// See https://github.com/IrisShaders/Iris/issues/1327 and the relevant putIfAbsent calls in
		// BlockMaterialMapping
		Properties properties = new OrderBackedProperties();
		try {
			properties.load(propertiesReader);
		} catch (IOException e) {
			Iris.logger.error("Error loading " + name + " at " + shaderPath, e);

			return Optional.empty();
		}

		return Optional.of(properties);
	}

	private static String readProperties(Path shaderPath, String name) {
		try {
			// ID maps should be encoded in ISO_8859_1.
			return new String(Files.readAllBytes(shaderPath.resolve(name)), StandardCharsets.ISO_8859_1);
		} catch (NoSuchFileException e) {
			Iris.logger.debug("An " + name + " file was not found in the current shaderpack");

			return null;
		} catch (IOException e) {
			Iris.logger.error("An IOException occurred reading " + name + " from the current shaderpack", e);

			return null;
		}
	}

	private static List<StringPair> withMcVersion(Iterable<StringPair> environmentDefines, String mcVersion) {
		ArrayList<StringPair> defines = new ArrayList<>();
		for (StringPair define : environmentDefines) {
			if (!"MC_VERSION".equals(define.getKey())) {
				defines.add(define);
			}
		}

		defines.add(new StringPair("MC_VERSION", mcVersion));
		return defines;
	}

	/**
	 * Detects whether a pack's block.properties has a section for the game's version that 1.13 would not read.
	 */
	private static boolean detectLegacySection(String blockSource, Properties blockProperties, Path shaderPath,
											   ShaderPackOptions shaderPackOptions, Iterable<StringPair> environmentDefines) {
		if (!blockSource.contains("MC_VERSION")) {
			return false;
		}

		final Properties flattened = loadProperties(blockSource, shaderPath, "block.properties", shaderPackOptions,
			withMcVersion(environmentDefines, FLATTENING_MC_VERSION)).orElse(null);
		return flattened == null || !blockTokens(flattened).containsAll(blockTokens(blockProperties));
	}

	private static Set<String> blockTokens(Properties properties) {
		final Set<String> tokens = new HashSet<>();

		properties.forEach((keyObject, valueObject) -> {
			final String key = (String) keyObject;
			if (key.startsWith("block.")) {
				for (String part : parseIdentifierList((String) valueObject, "block.properties", key)) {
					tokens.add(key + ' ' + part);
				}
			}
		});

		return tokens;
	}

	private static boolean namesModernBlocks(Properties properties) {
		for (Map.Entry<Object, Object> property : properties.entrySet()) {
			final String key = (String) property.getKey();
			if (!key.startsWith("block.")) {
				continue;
			}

			for (String part : parseIdentifierList((String) property.getValue(), "block.properties", key)) {
				final NamespacedId id = PropertiesTokenizer.parseBlockIdentifier(part).id();
				if ("minecraft".equals(id.getNamespace()) && FlatteningMap.isModernName(id.getName())) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * For modern shaderpacks: also expose entries under their 1.12.2 entity names
	 * so runtime lookups match EntityList's registered names directly.
	 *
	 * Devilica: the registered name runtime lookups take is still the one from before 1.11 ("PigZombie" for
	 * minecraft:zombie_pigman), so every entity a pack names gets that name too, from the registry.
	 */
	private static Object2IntMap<NamespacedId> augmentEntityIdMap(Object2IntMap<NamespacedId> idMap) {
		Object2IntMap<NamespacedId> augmented = new Object2IntOpenHashMap<>(idMap);
		augmented.defaultReturnValue(-1);

		for (Object2IntMap.Entry<NamespacedId> entry : idMap.object2IntEntrySet()) {
			NamespacedId id = entry.getKey();
			if (!"minecraft".equals(id.getNamespace())) {
				continue;
			}

			String legacyName = EntityFlatteningMap.toLegacy(id.getName());
			if (legacyName != null) {
				augmented.putIfAbsent(new NamespacedId(legacyName), entry.getIntValue());
			}
		}

		for (EntityEntry entity : ForgeRegistries.ENTITIES) {
			ResourceLocation name = entity.getRegistryName();
			int intId = augmented.getInt(new NamespacedId(name.getNamespace(), name.getPath()));
			if (intId != -1) {
				augmented.putIfAbsent(new NamespacedId(entity.getName()), intId);
			}
		}

		return Object2IntMaps.unmodifiable(augmented);
	}

	private static Object2IntMap<NamespacedId> parseItemIdMap(Properties properties) {
		return parseIdMap(properties, "item.", "item.properties");
	}

	private static Object2IntMap<NamespacedId> parseEntityIdMap(Properties properties) {
		return parseIdMap(properties, "entity.", "entity.properties");
	}

    /**
	 * Parses a space-delimited list of identifiers.
	 * Identifiers with spaces or special characters can be enclosed in double quotes.
	 * Use backslash to escape quotes or backslashes within quoted strings.
	 *
	 * Examples (input -> parsed result):
	 *   minecraft:stone natura:berry
	 *     -> minecraft:stone, natura:berry
	 *
	 *   minecraft:stone "Natura:N Crops"
	 *     -> minecraft:stone, Natura:N Crops
	 *
	 *   "SomeDumbMod:Foo\\ \"Bar\":3"
	 *     -> SomeDumbMod:Foo\ "Bar":3
	 *
	 * @param value The space-delimited string to parse
	 * @param fileName The name of the properties file (for error messages)
	 * @param key The property key being parsed (for error messages)
	 * @return List of parsed identifiers
	 */
	static List<String> parseIdentifierList(String value, String fileName, String key) {
		return PropertiesTokenizer.tokenizeValues(value, ' ');
	}

	/**
	 * Parses a NamespacedId map in OptiFine format
	 */
	private static Object2IntMap<NamespacedId> parseIdMap(Properties properties, String keyPrefix, String fileName) {
		Object2IntMap<NamespacedId> idMap = new Object2IntOpenHashMap<>();
		idMap.defaultReturnValue(-1);

		properties.forEach((keyObject, valueObject) -> {
			String key = (String) keyObject;
			String value = (String) valueObject;

			if (!key.startsWith(keyPrefix)) {
				// Not a valid line, ignore it
				return;
			}

			int intId;

			try {
				intId = Integer.parseInt(key.substring(keyPrefix.length()));
			} catch (NumberFormatException e) {
				// Not a valid property line
				Iris.logger.warn("Failed to parse line in " + fileName + ": invalid key " + key);
				return;
			}

			// Parse identifiers
			for (String part : parseIdentifierList(value, fileName, key)) {
				if (part.contains("[")) {
					// NBT-conditional entry
					// Devilica: read, so that the condition is not taken for a part of the name, and not matched
					try {
						BlockEntry entry = BlockEntry.parse(part);
						if (!entry.hasNbtProperties()) {
							idMap.put(entry.id(), intId);
						}
					} catch (Exception e) {
						Iris.logger.warn("Failed to parse NBT entry in " + fileName + " for key " + key + ": " + part, e);
					}
					continue;
				}
				if (part.contains("=")) {
					// Avoid tons of logspam for now
					Iris.logger.warn("Failed to parse an ResourceLocation in " + fileName + " for the key " + key + ": state properties are currently not supported: " + part);
					continue;
				}

				// Note: NamespacedId performs no validation on the content. That will need to be done by whatever is
				//       converting these things to ResourceLocations.
				idMap.put(new NamespacedId(part), intId);
			}
		});

		return Object2IntMaps.unmodifiable(idMap);
	}

	private static Int2ObjectMap<List<BlockEntry>> parseBlockMap(Properties properties, String keyPrefix, String fileName) {
		Int2ObjectMap<List<BlockEntry>> entriesById = new Int2ObjectOpenHashMap<>();

		properties.forEach((keyObject, valueObject) -> {
			final String key = (String) keyObject;
			StringBuilder value = new StringBuilder((String) valueObject);

			if (!key.startsWith(keyPrefix)) {
				// Not a valid line, ignore it
				return;
			}

			final int intId;

			try {
				intId = Integer.parseInt(key.substring(keyPrefix.length()));
			} catch (NumberFormatException e) {
				// Not a valid property line
				Iris.logger.warn("Failed to parse line in " + fileName + ": invalid key " + key);
				return;
			}

			final List<BlockEntry> entries = new ArrayList<>();

			if (value.toString().contains("minecraft:leaves")) {
				List<ItemStack> leaves = OreDictionary.getOres("treeLeaves");
				for (ItemStack leaf : leaves) {
					if (leaf.getItem() instanceof ItemBlock) {
						Iris.logger.warn("Found leaf " + leaf.getItem().getRegistryName());
						value.append(" ").append(leaf.getItem().getRegistryName());
					}
				}
			}

			// Parse identifiers
			for (String part : parseIdentifierList(value.toString(), fileName, key)) {
				if (part.isEmpty()) {
					continue;
				}

				try {
					entries.add(BlockEntry.parse(part));
				} catch (Exception e) {
					Iris.logger.warn("Unexpected error while parsing an entry from " + fileName + " for the key " + key + ":", e);
				}
			}

			entriesById.put(intId, Collections.unmodifiableList(entries));
		});

		return Int2ObjectMaps.unmodifiable(entriesById);
	}

	/**
	 * Parses a render layer map.
	 *
	 * This feature is used by Chocapic v9 and Wisdom Shaders. Otherwise, it is a rarely-used feature.
	 */
	private static Map<NamespacedId, BlockRenderType> parseRenderTypeMap(Properties properties, String keyPrefix, String fileName) {
		Map<NamespacedId, BlockRenderType> overrides = new HashMap<>();

		properties.forEach((keyObject, valueObject) -> {
			String key = (String) keyObject;
			String value = (String) valueObject;

			if (!key.startsWith(keyPrefix)) {
				// Not a valid line, ignore it
				return;
			}

			// Note: We have to remove the prefix "layer." because fromString expects "cutout", not "layer.cutout".
			String keyWithoutPrefix = key.substring(keyPrefix.length());

			BlockRenderType renderType = BlockRenderType.fromString(keyWithoutPrefix).orElse(null);

			if (renderType == null) {
				Iris.logger.warn("Failed to parse line in " + fileName + ": invalid block render type: " + key);
				return;
			}

			for (String part : parseIdentifierList(value, fileName, key)) {
				// Note: NamespacedId performs no validation on the content. That will need to be done by whatever is
				//       converting these things to ResourceLocations.
				overrides.put(new NamespacedId(part), renderType);
			}
		});

		return overrides;
	}

	public Int2ObjectMap<List<BlockEntry>> getBlockProperties() {
		return blockPropertiesMap;
	}

	public Object2IntFunction<NamespacedId> getItemIdMap() {
		return itemIdMap;
	}

	public Object2IntFunction<NamespacedId> getEntityIdMap() {
		return entityIdMap;
	}

	public Map<NamespacedId, BlockRenderType> getBlockRenderTypeMap() {
		return blockRenderTypeMap;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IdMap idMap = (IdMap) o;

		return Objects.equals(itemIdMap, idMap.itemIdMap)
				&& Objects.equals(entityIdMap, idMap.entityIdMap)
				&& Objects.equals(blockPropertiesMap, idMap.blockPropertiesMap)
				&& Objects.equals(blockRenderTypeMap, idMap.blockRenderTypeMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemIdMap, entityIdMap, blockPropertiesMap, blockRenderTypeMap);
	}
}
