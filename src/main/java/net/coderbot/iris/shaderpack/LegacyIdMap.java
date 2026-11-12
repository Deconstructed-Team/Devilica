package net.coderbot.iris.shaderpack;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.coderbot.iris.shaderpack.materialmap.BlockEntry;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LegacyIdMap {

	/**
	 * Fills the block ID map with all vanilla blocks in the case where block.properties is not found.
	 *
	 * Devilica: Angelica wraps IDs 128-255 to -128..-1, as ShadersMod on 1.7.10 passed them as a signed byte.
	 * OptiFine on 1.12.2 passes the block ID as it is, so shader packs written for it expect e.g. 175 for
	 * double plants.
	 */
	public static void addLegacyValues(Int2ObjectMap<List<BlockEntry>> blockIdMap) {
		for (ResourceLocation name : Block.REGISTRY.getKeys()) {
			// Only include vanilla minecraft blocks
			if (!"minecraft".equals(name.getNamespace())) {
				continue;
			}

			final Block block = Block.REGISTRY.getObject(name);

			final int id = Block.REGISTRY.getIDForObject(block);
			if (id <= 0 || id > 255) {
				continue;
			}

			final BlockEntry entry = new BlockEntry(new NamespacedId("minecraft", name.getPath()), Collections.emptySet());

			blockIdMap.computeIfAbsent(id, k -> new ArrayList<>()).add(entry);
		}
	}
}
