package net.coderbot.iris.block_rendering;

import net.minecraft.util.BlockRenderLayer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.coderbot.iris.shaderpack.materialmap.BlockEntry;
import net.coderbot.iris.shaderpack.materialmap.BlockRenderType;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockMaterialMapping {
	/**
	 * Creates a two-level map structure for block material IDs.
	 * Based on Iris's BlockState mapping approach adapted for 1.7.10's metadata system.
	 */
	public static Reference2ObjectMap<Block, Int2IntMap> createBlockMetaIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap) {
		Reference2ObjectMap<Block, Int2IntMap> blockMatches = new Reference2ObjectOpenHashMap<>();

		blockPropertiesMap.forEach((intId, entries) -> {
			for (BlockEntry entry : entries) {
				addBlockMetas(entry, blockMatches, intId);
			}
		});

		return blockMatches;
	}

	public static Map<Block, BlockRenderLayer> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap) {
		Map<Block, BlockRenderLayer> blockTypeIds = new Reference2ReferenceOpenHashMap<>();

		blockPropertiesMap.forEach((id, blockType) -> {
			final ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());
			final Block block = Block.getBlockFromName(resourceLocation.toString());

			if (block == null || block == Blocks.AIR) {
				return;
			}

			final BlockRenderLayer layer = convertBlockToRenderLayer(blockType);
			if (layer != null) {
				blockTypeIds.put(block, layer);
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
	private static void addBlockMetas(BlockEntry entry, Reference2ObjectMap<Block, Int2IntMap> idMap, int intId) {
		final NamespacedId id = entry.getId();
		final ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());

		final Block block = Block.REGISTRY.getObject(resourceLocation);

		// If the block doesn't exist, by default the registry will return AIR. That probably isn't what we want.
		// TODO: Assuming that Registry.BLOCK.getDefaultId() == "minecraft:air" here
		if (block == null || block == Blocks.AIR) {
			return;
		}

		Set<Integer> metas = entry.getMetas();
		if (!entry.getProperties().isEmpty()) {
			metas = new HashSet<>(metas);
			if (!addStateMetas(block, entry.getProperties(), metas)) {
				return;
			}
		}

		Int2IntMap metaMap = idMap.get(block);
		if (metaMap == null) {
			metaMap = new Int2IntOpenHashMap();
			metaMap.defaultReturnValue(-1);
			idMap.put(block, metaMap);
		}

		if (metas.isEmpty()) {
			// Add all metadata values (0-15) if there aren't any specific ones
			for (int meta = 0; meta < 16; meta++) {
				metaMap.putIfAbsent(meta, intId);
			}
		} else {
			// Add only specific metadata values
			for (int meta : metas) {
				metaMap.putIfAbsent(meta, intId);
			}
		}
	}

	/**
	 * The metadata of every state of the block whose properties match, as OptiFine resolves block states on 1.12;
	 * properties that only the actual state has (a grass block's snow) match the metadata it is stored as.
	 *
	 * @return whether any state matched
	 */
	private static boolean addStateMetas(Block block, Map<String, Set<String>> conditions, Set<Integer> metas) {
		boolean matched = false;
		for (IBlockState state : block.getBlockState().getValidStates()) {
			if (matches(state, conditions)) {
				metas.add(block.getMetaFromState(state));
				matched = true;
			}
		}
		return matched;
	}

	private static boolean matches(IBlockState state, Map<String, Set<String>> conditions) {
		for (Map.Entry<String, Set<String>> condition : conditions.entrySet()) {
			final IProperty<?> property = findProperty(state, condition.getKey());
			if (property == null || !condition.getValue().contains(valueName(state, property))) {
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
