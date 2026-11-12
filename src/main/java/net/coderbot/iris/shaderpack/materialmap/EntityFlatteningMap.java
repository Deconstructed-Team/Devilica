package net.coderbot.iris.shaderpack.materialmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps modern entity names to their 1.12.2 equivalents.
 *
 * Devilica: entity IDs are namespaced since 1.11, and 1.12.2 already has the entities 1.7.10 kept as
 * NBT variants of one (wither skeleton, donkey, zombie villager) as types of their own. What is left is
 * the entities that were renamed in 1.13 and later, and those later versions split by a variant.
 */
public class EntityFlatteningMap {

	/** Maps modern entity name -> 1.12.2 entity name. */
	private static final Map<String, String> MODERN_TO_LEGACY = new HashMap<>();

	static {
		// Renamed entities
		simple("commandblock_minecart", "command_block_minecart");
		simple("ender_crystal", "end_crystal");
		simple("evocation_fangs", "evoker_fangs");
		simple("evocation_illager", "evoker");
		simple("eye_of_ender_signal", "eye_of_ender");
		simple("fireworks_rocket", "firework_rocket");
		simple("illusion_illager", "illusioner");
		simple("snowman", "snow_golem");
		simple("villager_golem", "iron_golem");
		simple("vindication_illager", "vindicator");
		simple("xp_bottle", "experience_bottle");
		simple("xp_orb", "experience_orb");
		simple("zombie_pigman", "zombified_piglin");

		// Split by a variant in 1.21
		simple("boat", "oak_boat");
		simple("boat", "spruce_boat");
		simple("boat", "birch_boat");
		simple("boat", "jungle_boat");
		simple("boat", "acacia_boat");
		simple("boat", "dark_oak_boat");
		simple("potion", "splash_potion");
		simple("potion", "lingering_potion");
	}

	/**
	 * Returns the 1.12.2 entity name for a modern entity name, or null if no mapping exists.
	 * The input name should not include the "minecraft:" namespace prefix.
	 */
	public static String toLegacy(String modernName) {
		return MODERN_TO_LEGACY.get(modernName);
	}

	private static void simple(String legacy, String modern) {
		MODERN_TO_LEGACY.put(modern, legacy);
	}
}
