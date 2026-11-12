package net.coderbot.iris.block_rendering;

import net.minecraft.util.BlockRenderLayer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.coderbot.iris.shaderpack.materialmap.BlockEntry;
import net.coderbot.iris.shaderpack.materialmap.BlockRenderType;
import net.coderbot.iris.shaderpack.materialmap.FlatteningMap;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockMaterialMapping {

	// Meta-key bits OR'd in at runtime
	// Devilica: nothing ORs it in yet. On 1.12.2 "snowy" is in the actual state only, which the chunk mesher does
	// not pass to the block ID lookup, so the snow-covered entries of a modern pack wait under this bit.
	public static final int SNOWY_META_BIT = 1 << 16;

	/**
	 * Creates a two-level map structure for block material IDs, and registers snowy blocks on
	 * {@link BlockRenderingSettings}.
	 * Based on Iris's BlockState mapping approach adapted for 1.7.10's metadata system.
	 */
	public static Reference2ObjectMap<Block, Int2IntMap> createBlockMetaIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap, boolean skipFlattening) {
		final Reference2ObjectMap<Block, Int2IntMap> blockMatches = new Reference2ObjectOpenHashMap<>();
		final ReferenceSet<Block> snowyBlocks = new ReferenceOpenHashSet<>();

		blockPropertiesMap.forEach((intId, entries) -> {
			for (BlockEntry entry : entries) {
				// Devilica: NBT conditions are read and not matched, Angelica matches them in its chunk mesher
				if (!entry.hasNbtProperties()) {
					addBlockMetas(entry, blockMatches, intId, skipFlattening, snowyBlocks);
				}
			}
		});

		BlockRenderingSettings.INSTANCE.setSnowyBlocks(snowyBlocks);
		return blockMatches;
	}

	public static Map<Block, BlockRenderLayer> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap, boolean skipFlattening) {
		final Map<Block, BlockRenderLayer> blockTypeIds = new Reference2ReferenceOpenHashMap<>();

		blockPropertiesMap.forEach((id, blockType) -> {
			final BlockRenderLayer layer = convertBlockToRenderLayer(blockType);
			if (layer == null) return;

			// Devilica: as the block IDs go, a modern pack's names through FlatteningMap first (render type applies per-block)
			final List<BlockEntry> legacyEntries = skipFlattening || !"minecraft".equals(id.getNamespace())
				? null : FlatteningMap.toLegacy(id.getName(), Collections.emptyMap());
			if (legacyEntries == null) {
				final Block block = Block.getBlockFromName(new ResourceLocation(id.getNamespace(), id.getName()).toString());
				if (block != null && block != Blocks.AIR) blockTypeIds.put(block, layer);
				return;
			}

			for (BlockEntry legacyEntry : legacyEntries) {
				final Block block = resolveBlockOrNull(legacyEntry.id());
				if (block != null) blockTypeIds.put(block, layer);
			}
		});

		return blockTypeIds;
	}

	private static BlockRenderLayer convertBlockToRenderLayer(BlockRenderType type) {
		if (type == null) {
			return null;
		}

		return switch (type) {
			case SOLID -> BlockRenderLayer.SOLID;
			case CUTOUT -> BlockRenderLayer.CUTOUT;
			case CUTOUT_MIPPED -> BlockRenderLayer.CUTOUT_MIPPED;
			case TRANSLUCENT -> BlockRenderLayer.TRANSLUCENT;
		};
	}

	/**
	 * Adds block+metadata combinations to the material ID map.
	 * Based on Iris's addBlockStates method, adapted for 1.7.10 metadata system.
	 */
	private static void addBlockMetas(BlockEntry entry, Reference2ObjectMap<Block, Int2IntMap> idMap, int intId,
									  boolean skipFlattening, ReferenceSet<Block> snowyBlocks) {
		final NamespacedId id = entry.id();
		// Devilica: a pack with a section for this version keeps OptiFine's reading of 1.12 block states, where
		// "snowy" matches the metadata the block is stored as
		final Set<String> snowy = skipFlattening ? null : entry.stateProperties().get("snowy");
		final BlockEntry plain = snowy == null ? entry : new BlockEntry(id, entry.metas(), without(entry.stateProperties(), "snowy"));
		final Map<String, Set<String>> stateProps = plain.stateProperties();

		// Vanilla modern names go through FlatteningMap; legacy-section packs and modded blocks
		// resolve directly from the registry.
		List<BlockEntry> targets = null;
		final boolean vanilla = !skipFlattening && "minecraft".equals(id.getNamespace());
		if (vanilla && (!stateProps.isEmpty() || entry.metas().isEmpty())) {
			targets = FlatteningMap.toLegacy(id.getName(), stateProps);
		}
		if (targets == null) targets = List.of(plain);

		for (BlockEntry target : targets) {
			final Block block = resolveBlockOrNull(target.id());
			if (block == null) continue;

			Set<Integer> metas = target.metas();
			if (!target.stateProperties().isEmpty()) {
				metas = new HashSet<>(metas);
				if (!addStateMetas(block, target.stateProperties(), metas, vanilla)) {
					continue;
				}
			}

			if (snowy == null || snowy.contains("false")) applyMetas(block, metas, idMap, intId, 0);
			if (snowy != null && snowy.contains("true")) applyMetas(block, metas, idMap, intId, SNOWY_META_BIT);
			if (snowy != null) snowyBlocks.add(block);
		}
	}

	private static void applyMetas(Block block, Set<Integer> metas, Reference2ObjectMap<Block, Int2IntMap> idMap, int intId, int extraBits) {
		Int2IntMap metaMap = idMap.get(block);
		if (metaMap == null) {
			metaMap = new Int2IntOpenHashMap();
			metaMap.defaultReturnValue(-1);
			idMap.put(block, metaMap);
		}

		if (metas.isEmpty()) {
			// Add all metadata values (0-15) if there aren't any specific ones
			for (int meta = 0; meta < 16; meta++) metaMap.putIfAbsent(meta | extraBits, intId);
		} else {
			// Add only specific metadata values
			for (int meta : metas) metaMap.putIfAbsent(meta | extraBits, intId);
		}
	}

	/**
	 * Returns the registered Block for an id, or null if unknown.
	 * The registry returns Blocks.air (its default) for missing keys, which we coerce to null.
	 */
	private static Block resolveBlockOrNull(NamespacedId id) {
		final Block block = Block.REGISTRY.getObject(new ResourceLocation(id.getNamespace(), id.getName()));
		return block == Blocks.AIR ? null : block;
	}

	private static Map<String, Set<String>> without(Map<String, Set<String>> properties, String name) {
		final Map<String, Set<String>> rest = new HashMap<>(properties);
		rest.remove(name);
		return rest;
	}

	/**
	 * The metadata of every state of the block whose properties match, as OptiFine resolves block states on 1.12;
	 * properties that only the actual state has (a grass block's snow) match the metadata it is stored as.
	 *
	 * @param modern whether the conditions are a modern pack's: properties the 1.12.2 block does not have are
	 *               then left out, and nothing is waterlogged
	 * @return whether any state matched
	 */
	private static boolean addStateMetas(Block block, Map<String, Set<String>> conditions, Set<Integer> metas, boolean modern) {
		boolean matched = false;
		for (IBlockState state : block.getBlockState().getValidStates()) {
			if (matches(state, conditions, modern)) {
				metas.add(block.getMetaFromState(state));
				matched = true;
			}
		}
		return matched;
	}

	private static boolean matches(IBlockState state, Map<String, Set<String>> conditions, boolean modern) {
		for (Map.Entry<String, Set<String>> condition : conditions.entrySet()) {
			final IProperty<?> property = findProperty(state, condition.getKey());
			if (property == null) {
				if (modern && (!condition.getKey().equals("waterlogged") || condition.getValue().contains("false"))) {
					continue;
				}
				return false;
			}
			if (!condition.getValue().contains(valueName(state, property))) {
				return false;
			}
		}
		return true;
	}

	private static IProperty<?> findProperty(IBlockState state, String name) {
		for (IProperty<?> property : state.getPropertyKeys()) {
			if (property.getName().equals(name)) {
				return property;
			}
		}
		return null;
	}

	private static <T extends Comparable<T>> String valueName(IBlockState state, IProperty<T> property) {
		return property.getName(state.getValue(property));
	}
}
