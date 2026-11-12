package net.coderbot.iris.block_rendering;

import net.minecraft.util.BlockRenderLayer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockMaterialMapping {

	/**
	 * Creates a two-level map structure for block material IDs, and registers the blocks that go by their actual
	 * state on {@link BlockRenderingSettings}.
	 * Based on Iris's BlockState mapping approach adapted for 1.7.10's metadata system.
	 * <p>
	 * Devilica: entries are taken in the file's order and the first to match a block wins, as in OptiFine and Iris.
	 */
	public static Reference2ObjectMap<Block, Int2IntMap> createBlockMetaIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap, boolean skipFlattening) {
		final Reference2ObjectMap<Block, Int2IntMap> blockMatches = new Reference2ObjectOpenHashMap<>();
		final Reference2ObjectMap<Block, Reference2IntMap<IBlockState>> stateMatches = new Reference2ObjectOpenHashMap<>();

		blockPropertiesMap.forEach((intId, entries) -> {
			for (BlockEntry entry : entries) {
				// Devilica: NBT conditions are read and not matched, Angelica matches them in its chunk mesher
				if (!entry.hasNbtProperties()) {
					addBlockMetas(entry, blockMatches, stateMatches, intId, skipFlattening);
				}
			}
		});

		BlockRenderingSettings.INSTANCE.setBlockStateMatches(keepToldApartByActualState(blockMatches, stateMatches));
		return blockMatches;
	}

	/**
	 * Devilica: on 1.12.2 part of a block's state is not in its metadata: the snow on a grass block, the plant of a
	 * double plant's top half, what a flower pot holds. A pack that gives such states different IDs has the block
	 * looked up by its actual state in the chunk mesher; its metadata keeps the ID of the state the metadata alone
	 * stands for, for where there is no world to ask (a held block).
	 */
	private static Reference2ObjectMap<Block, Reference2IntMap<IBlockState>> keepToldApartByActualState(
			Reference2ObjectMap<Block, Int2IntMap> blockMatches, Reference2ObjectMap<Block, Reference2IntMap<IBlockState>> stateMatches) {
		final Reference2ObjectMap<Block, Reference2IntMap<IBlockState>> toldApart = new Reference2ObjectOpenHashMap<>();
		stateMatches.forEach((block, byState) -> {
			final Int2IntMap byMeta = blockMatches.get(block);
			if (byMeta == null) {
				return;
			}
			boolean differs = false;
			for (IBlockState state : block.getBlockState().getValidStates()) {
				differs |= byState.getInt(state) != byMeta.get(block.getMetaFromState(state));
			}
			if (!differs) {
				return;
			}
			toldApart.put(block, byState);
			for (int meta : byMeta.keySet().toIntArray()) {
				try {
					final int id = byState.getInt(block.getStateFromMeta(meta));
					if (id != -1) {
						byMeta.put(meta, id);
					}
				} catch (RuntimeException e) {
					// a metadata value the block has no state for keeps the ID it got
				}
			}
		});
		return toldApart;
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
	private static void addBlockMetas(BlockEntry entry, Reference2ObjectMap<Block, Int2IntMap> idMap,
									  Reference2ObjectMap<Block, Reference2IntMap<IBlockState>> stateMatches, int intId,
									  boolean skipFlattening) {
		final NamespacedId id = entry.id();
		final Map<String, Set<String>> stateProps = entry.stateProperties();

		// Vanilla modern names go through FlatteningMap; legacy-section packs and modded blocks
		// resolve directly from the registry.
		List<BlockEntry> targets = null;
		final boolean vanilla = !skipFlattening && "minecraft".equals(id.getNamespace());
		if (vanilla && (!stateProps.isEmpty() || entry.metas().isEmpty())) {
			targets = FlatteningMap.toLegacy(id.getName(), stateProps);
		}
		if (targets == null) targets = List.of(entry);

		for (BlockEntry target : targets) {
			final Block block = resolveBlockOrNull(target.id());
			if (block == null) continue;

			// The IDs by state are kept from the block's first entry with state properties on
			Reference2IntMap<IBlockState> byState = stateMatches.get(block);
			final boolean everyState = target.metas().isEmpty() && target.stateProperties().isEmpty();
			Set<Integer> metas = target.metas();
			if (!target.stateProperties().isEmpty()) {
				if (byState == null) {
					byState = statesByMeta(block, idMap.get(block));
					stateMatches.put(block, byState);
				}
				metas = new HashSet<>(metas);
				if (!addStateMetas(block, target.stateProperties(), metas, vanilla, byState, intId)) {
					continue;
				}
			}

			if (byState != null) {
				for (IBlockState state : block.getBlockState().getValidStates()) {
					if (everyState || target.metas().contains(block.getMetaFromState(state))) {
						byState.putIfAbsent(state, intId);
					}
				}
			}
			applyMetas(block, metas, idMap, intId);
		}
	}

	private static Reference2IntMap<IBlockState> statesByMeta(Block block, Int2IntMap byMeta) {
		final Reference2IntMap<IBlockState> byState = new Reference2IntOpenHashMap<>();
		byState.defaultReturnValue(-1);
		if (byMeta != null) {
			for (IBlockState state : block.getBlockState().getValidStates()) {
				final int id = byMeta.get(block.getMetaFromState(state));
				if (id != -1) {
					byState.put(state, id);
				}
			}
		}
		return byState;
	}

	private static void applyMetas(Block block, Set<Integer> metas, Reference2ObjectMap<Block, Int2IntMap> idMap, int intId) {
		Int2IntMap metaMap = idMap.get(block);
		if (metaMap == null) {
			metaMap = new Int2IntOpenHashMap();
			metaMap.defaultReturnValue(-1);
			idMap.put(block, metaMap);
		}

		if (metas.isEmpty()) {
			// Add all metadata values (0-15) if there aren't any specific ones
			for (int meta = 0; meta < 16; meta++) metaMap.putIfAbsent(meta, intId);
		} else {
			// Add only specific metadata values
			for (int meta : metas) metaMap.putIfAbsent(meta, intId);
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

	/**
	 * The metadata of every state of the block whose properties match, as OptiFine resolves block states on 1.12,
	 * and the states themselves for {@link #keepToldApartByActualState}.
	 *
	 * @param modern whether the conditions are a modern pack's: properties the 1.12.2 block does not have are
	 *               then left out, and nothing is waterlogged
	 * @return whether any state matched
	 */
	private static boolean addStateMetas(Block block, Map<String, Set<String>> conditions, Set<Integer> metas, boolean modern,
										 Reference2IntMap<IBlockState> byState, int intId) {
		boolean matched = false;
		for (IBlockState state : block.getBlockState().getValidStates()) {
			if (matches(state, conditions, modern)) {
				metas.add(block.getMetaFromState(state));
				byState.putIfAbsent(state, intId);
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
