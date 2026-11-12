package net.coderbot.iris.shaderpack.materialmap;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps modern (1.13+ post-flattening) block names to their 1.12.2 equivalents.
 *
 * In 1.13, Minecraft's "flattening" replaced numeric block IDs + metadata with separate
 * named blocks. For example, oak_planks/spruce_planks/birch_planks became distinct blocks,
 * where before they were all "planks" with different metadata values.
 *
 * Modern shader packs reference blocks by their post-flattening names in block.properties.
 * This map allows us to resolve those names to the correct 1.12.2 block + block state.
 *
 * Only entries where the modern name differs from the 1.12.2 name, or stands for fewer of
 * the block's states, are included. Blocks that mean the same across versions (e.g. diamond_ore,
 * glass, obsidian) resolve directly via the registry and don't need entries here.
 *
 * Devilica: the table is written for 1.12.2. A modern name maps to 1.12.2 blocks and the values their
 * block state properties have, not to metadata: 1.12.2 block states have the properties the flattening
 * kept, so the metadata is taken from the block itself and a pack's own state properties (facing, half,
 * age) are matched against the block as they are. Only what exists in 1.12.2 is listed.
 */
public class FlatteningMap {

	/** Modern block name -> the 1.12.2 blocks and states it stands for. */
	private static final Map<String, List<Target>> MODERN_TO_LEGACY = new HashMap<>();

	private static final String[] COLORS = {
		"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
		"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
	};

	private static final String[] WOOD_TYPES = {
		"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"
	};

	private static final String[] HORIZONTAL = {"north", "south", "west", "east"};

	static {
		// ==============
		// Simple renames
		// ==============

		// === Grass block (ID 2) ===
		rename("grass_block", "grass");

		// === Note block (ID 25) ===
		rename("note_block", "noteblock");

		// === Powered rail (ID 27) ===
		rename("powered_rail", "golden_rail");

		// === Cobweb (ID 30) ===
		rename("cobweb", "web");

		// === Moving piston (ID 36) ===
		rename("moving_piston", "piston_extension");

		// === Dandelion (ID 37) ===
		rename("dandelion", "yellow_flower");

		// === Bricks (ID 45) ===
		rename("bricks", "brick_block");

		// === Spawner (ID 52) ===
		rename("spawner", "mob_spawner");

		// === Signs (IDs 63, 68) ===
		// 1.12.2 has the oak sign only. It was "sign" in 1.13 and "oak_sign" from 1.14 on.
		rename("sign",          "standing_sign");
		rename("oak_sign",      "standing_sign");
		rename("oak_wall_sign", "wall_sign");

		// === Oak door (ID 64) ===
		rename("oak_door", "wooden_door");

		// === Cobblestone stairs (ID 67) ===
		rename("cobblestone_stairs", "stone_stairs");
		// The stone stairs of 1.14 are another block, which 1.12.2 does not have
		absent("stone_stairs");

		// === Oak pressure plate (ID 72) ===
		rename("oak_pressure_plate", "wooden_pressure_plate");

		// === Snow layers (ID 78) and snow block (ID 80) ===
		// Modern "snow" = layer block, but 1.12.2 "snow" = full snow block.
		rename("snow", "snow_layer");
		rename("snow_block", "snow");

		// === Sugar cane (ID 83) ===
		rename("sugar_cane", "reeds");

		// === Oak fence (ID 85) ===
		rename("oak_fence", "fence");

		// === Carved pumpkin (ID 86) ===
		rename("carved_pumpkin", "pumpkin");

		// === Nether portal (ID 90) ===
		rename("nether_portal", "portal");

		// === Jack-o-lantern (ID 91) ===
		rename("jack_o_lantern", "lit_pumpkin");

		// === Oak trapdoor (ID 96) ===
		rename("oak_trapdoor", "trapdoor");

		// === Melon (ID 103) ===
		rename("melon", "melon_block");

		// === Pumpkin stem (ID 104), melon stem (ID 105) ===
		// The stem turns to the fruit next to it in its actual state.
		multi("pumpkin_stem",           block("pumpkin_stem").with("facing", "up"));
		multi("attached_pumpkin_stem",  block("pumpkin_stem").with("facing", HORIZONTAL));
		multi("melon_stem",             block("melon_stem").with("facing", "up"));
		multi("attached_melon_stem",    block("melon_stem").with("facing", HORIZONTAL));

		// === Oak fence gate (ID 107) ===
		rename("oak_fence_gate", "fence_gate");

		// === Lily pad (ID 111) ===
		rename("lily_pad", "waterlily");

		// === Nether bricks (ID 112) ===
		rename("nether_bricks", "nether_brick");

		// === Nether quartz ore (ID 153) ===
		rename("nether_quartz_ore", "quartz_ore");

		// === Slime block (ID 165) ===
		rename("slime_block", "slime");

		// === Terracotta (ID 172) ===
		rename("terracotta", "hardened_clay");

		// === End stone bricks (ID 206) ===
		rename("end_stone_bricks", "end_bricks");

		// === Dirt path (ID 208), "grass_path" until 1.17 ===
		rename("dirt_path", "grass_path");

		// === Magma block (ID 213) ===
		rename("magma_block", "magma");

		// === Red nether bricks (ID 215) ===
		rename("red_nether_bricks", "red_nether_brick");

		// === Shulker boxes (IDs 219-234), glazed terracotta (IDs 235-250) ===
		rename("light_gray_shulker_box",        "silver_shulker_box");
		rename("light_gray_glazed_terracotta",  "silver_glazed_terracotta");

		// ==========================================
		// Variants (block split by flattening)
		// ==========================================

		// === Stone variants (ID 1) ===
		variant("stone",                "stone", "variant", "stone");
		variant("granite",              "stone", "variant", "granite");
		variant("polished_granite",     "stone", "variant", "smooth_granite");
		variant("diorite",              "stone", "variant", "diorite");
		variant("polished_diorite",     "stone", "variant", "smooth_diorite");
		variant("andesite",             "stone", "variant", "andesite");
		variant("polished_andesite",    "stone", "variant", "smooth_andesite");

		// === Dirt variants (ID 3) ===
		variant("dirt",         "dirt", "variant", "dirt");
		variant("coarse_dirt",  "dirt", "variant", "coarse_dirt");
		variant("podzol",       "dirt", "variant", "podzol");

		// === Planks (ID 5), saplings (ID 6) ===
		for (String wood : WOOD_TYPES) {
			variant(wood + "_planks",   "planks",   "variant",  wood);
			variant(wood + "_sapling",  "sapling",  "type",     wood);
		}

		// === Water / Lava (IDs 8-11) ===
		// In 1.13+, water/lava are single blocks. In 1.12.2, flowing variants are separate.
		// Map modern names to both still and flowing for full coverage; "level" is the same property.
		multi("water",  block("water"), block("flowing_water"));
		multi("lava",   block("lava"),  block("flowing_lava"));

		// === Sand (ID 12) ===
		variant("sand",     "sand", "variant", "sand");
		variant("red_sand", "sand", "variant", "red_sand");

		// === Logs (IDs 17, 162), leaves (IDs 18, 161) ===
		// axis=none is the block with bark on all sides
		for (String wood : WOOD_TYPES) {
			final boolean second = wood.equals("acacia") || wood.equals("dark_oak");
			final Target log = block(second ? "log2" : "log").with("variant", wood);
			final Target leaves = block(second ? "leaves2" : "leaves").with("variant", wood);
			multi(wood + "_log",    log.with("axis", "x", "y", "z"));
			multi(wood + "_wood",   log.with("axis", "none").when("axis", "x", "y", "z"));
			// decayable=false is a player-placed block. Maps to modern persistent=true.
			multi(wood + "_leaves",
				leaves.with("decayable", "true").when("persistent", "false"),
				leaves.with("decayable", "false").when("persistent", "true"));
		}

		// === Sponge (ID 19) ===
		variant("sponge",       "sponge", "wet", "false");
		variant("wet_sponge",   "sponge", "wet", "true");

		// === Sandstone variants (IDs 24, 179) ===
		// The smooth sandstone of 1.12.2 is the cut one; the modern smooth one is the seamless double slab
		variant("sandstone",                "sandstone",        "type", "sandstone");
		variant("chiseled_sandstone",       "sandstone",        "type", "chiseled_sandstone");
		variant("cut_sandstone",            "sandstone",        "type", "smooth_sandstone");
		variant("red_sandstone",            "red_sandstone",    "type", "red_sandstone");
		variant("chiseled_red_sandstone",   "red_sandstone",    "type", "chiseled_red_sandstone");
		variant("cut_red_sandstone",        "red_sandstone",    "type", "smooth_red_sandstone");

		// === Beds (ID 26), banners (IDs 176, 177) ===
		// One block each; the color is in the block entity.
		for (String color : COLORS) {
			rename(color + "_bed",          "bed");
			rename(color + "_banner",       "standing_banner");
			rename(color + "_wall_banner",  "wall_banner");
		}

		// === Tall grass / fern (ID 31), dead bush (ID 32) ===
		variant("grass",        "tallgrass", "type", "tall_grass");   // 1.13-1.20.2 name
		variant("short_grass",  "tallgrass", "type", "tall_grass");   // 1.20.3+ name
		variant("fern",         "tallgrass", "type", "fern");
		multi("dead_bush", block("deadbush"), block("tallgrass").with("type", "dead_bush"));

		// === Flowers (ID 38) ===
		variant("poppy",        "red_flower", "type", "poppy");
		variant("blue_orchid",  "red_flower", "type", "blue_orchid");
		variant("allium",       "red_flower", "type", "allium");
		variant("azure_bluet",  "red_flower", "type", "houstonia");
		variant("red_tulip",    "red_flower", "type", "red_tulip");
		variant("orange_tulip", "red_flower", "type", "orange_tulip");
		variant("white_tulip",  "red_flower", "type", "white_tulip");
		variant("pink_tulip",   "red_flower", "type", "pink_tulip");
		variant("oxeye_daisy",  "red_flower", "type", "oxeye_daisy");

		// === Colored blocks (IDs 35, 95, 159, 160, 171, 251, 252) ===
		colorVariants("wool",               "wool");
		colorVariants("stained_glass",      "stained_glass");
		colorVariants("terracotta",         "stained_hardened_clay");
		colorVariants("stained_glass_pane", "stained_glass_pane");
		colorVariants("carpet",             "carpet");
		colorVariants("concrete",           "concrete");
		colorVariants("concrete_powder",    "concrete_powder");

		// === Slabs (IDs 43, 44, 125, 126, 181, 182, 204, 205) ===
		// Modern: type=bottom/top/double. 1.12.2: half=bottom/top, and the double slab is a block of its own.
		// "stone_slab" was the smooth stone slab in 1.13; the seamless double slabs became blocks of their own.
		final Target stoneSlab = block("double_stone_slab").with("variant", "stone").with("seamless", "false");
		slab("stone_slab",          block("stone_slab").with("variant", "stone"),           stoneSlab);
		slab("smooth_stone_slab",   block("stone_slab").with("variant", "stone"),           stoneSlab);
		slab("sandstone_slab",      block("stone_slab").with("variant", "sandstone"),
			block("double_stone_slab").with("variant", "sandstone").with("seamless", "false"));
		slab("petrified_oak_slab",  block("stone_slab").with("variant", "wood_old"),        block("double_stone_slab").with("variant", "wood_old"));
		slab("cobblestone_slab",    block("stone_slab").with("variant", "cobblestone"),     block("double_stone_slab").with("variant", "cobblestone"));
		slab("brick_slab",          block("stone_slab").with("variant", "brick"),           block("double_stone_slab").with("variant", "brick"));
		slab("stone_brick_slab",    block("stone_slab").with("variant", "stone_brick"),     block("double_stone_slab").with("variant", "stone_brick"));
		slab("nether_brick_slab",   block("stone_slab").with("variant", "nether_brick"),    block("double_stone_slab").with("variant", "nether_brick"));
		slab("quartz_slab",         block("stone_slab").with("variant", "quartz"),
			block("double_stone_slab").with("variant", "quartz").with("seamless", "false"));
		slab("red_sandstone_slab",  block("stone_slab2"),   block("double_stone_slab2").with("seamless", "false"));
		slab("purpur_slab",         block("purpur_slab"),   block("purpur_double_slab"));
		for (String wood : WOOD_TYPES) {
			slab(wood + "_slab", block("wooden_slab").with("variant", wood), block("double_wooden_slab").with("variant", wood));
		}
		multi("smooth_stone",           block("double_stone_slab").with("variant", "stone").with("seamless", "true"));
		multi("smooth_sandstone",       block("double_stone_slab").with("variant", "sandstone").with("seamless", "true"));
		multi("smooth_quartz",          block("double_stone_slab").with("variant", "quartz").with("seamless", "true"));
		multi("smooth_red_sandstone",   block("double_stone_slab2").with("seamless", "true"));

		// === Torch variants (IDs 50, 75, 76) ===
		// In 1.12.2, torch handles both floor and wall via its facing.
		// In 1.13+, wall_torch is a separate block, and the unlit redstone torch is a state.
		variant("torch",        "torch", "facing", "up");
		variant("wall_torch",   "torch", "facing", HORIZONTAL);
		multi("redstone_torch",
			block("redstone_torch").with("facing", "up").when("lit", "true"),
			block("unlit_redstone_torch").with("facing", "up").when("lit", "false"));
		multi("redstone_wall_torch",
			block("redstone_torch").with("facing", HORIZONTAL).when("lit", "true"),
			block("unlit_redstone_torch").with("facing", HORIZONTAL).when("lit", "false"));

		// === Monster egg / Infested blocks (ID 97) ===
		variant("infested_stone",                   "monster_egg", "variant", "stone");
		variant("infested_cobblestone",             "monster_egg", "variant", "cobblestone");
		variant("infested_stone_bricks",            "monster_egg", "variant", "stone_brick");
		variant("infested_mossy_stone_bricks",      "monster_egg", "variant", "mossy_brick");
		variant("infested_cracked_stone_bricks",    "monster_egg", "variant", "cracked_brick");
		variant("infested_chiseled_stone_bricks",   "monster_egg", "variant", "chiseled_brick");

		// === Stone bricks (ID 98) ===
		variant("stone_bricks",             "stonebrick", "variant", "stonebrick");
		variant("mossy_stone_bricks",       "stonebrick", "variant", "mossy_stonebrick");
		variant("cracked_stone_bricks",     "stonebrick", "variant", "cracked_stonebrick");
		variant("chiseled_stone_bricks",    "stonebrick", "variant", "chiseled_stonebrick");

		// === Mushroom blocks (IDs 99, 100) ===
		// In modern MC, mushroom_stem is separate.
		// In 1.12.2, the stem variants of both mushroom blocks are the stem. We need the cap variants
		// so modern "brown_mushroom_block" doesn't claim the states that belong to "mushroom_stem".
		final String[] caps = {
			"north_west", "north", "north_east", "west", "center", "east",
			"south_west", "south", "south_east", "all_inside", "all_outside"
		};
		variant("brown_mushroom_block", "brown_mushroom_block", "variant", caps);
		variant("red_mushroom_block",   "red_mushroom_block",   "variant", caps);
		multi("mushroom_stem",
			block("brown_mushroom_block").with("variant", "stem", "all_stem"),
			block("red_mushroom_block").with("variant", "stem", "all_stem"));

		// === Cauldron variants (ID 118) ===
		// In 1.17+, cauldron was split by contents.
		variant("cauldron",         "cauldron", "level", "0");
		variant("water_cauldron",   "cauldron", "level", "1", "2", "3");

		// === Cobblestone wall variants (ID 139) ===
		variant("cobblestone_wall",         "cobblestone_wall", "variant", "cobblestone");
		variant("mossy_cobblestone_wall",   "cobblestone_wall", "variant", "mossy_cobblestone");

		// === Flower pot (ID 140) ===
		// 1.12.2 takes the potted plant from the block entity into the actual state. 1.13+ has a separate
		// block per potted plant.
		variant("flower_pot",               "flower_pot", "contents", "empty");
		variant("potted_poppy",             "flower_pot", "contents", "rose");
		variant("potted_blue_orchid",       "flower_pot", "contents", "blue_orchid");
		variant("potted_allium",            "flower_pot", "contents", "allium");
		variant("potted_azure_bluet",       "flower_pot", "contents", "houstonia");
		variant("potted_red_tulip",         "flower_pot", "contents", "red_tulip");
		variant("potted_orange_tulip",      "flower_pot", "contents", "orange_tulip");
		variant("potted_white_tulip",       "flower_pot", "contents", "white_tulip");
		variant("potted_pink_tulip",        "flower_pot", "contents", "pink_tulip");
		variant("potted_oxeye_daisy",       "flower_pot", "contents", "oxeye_daisy");
		variant("potted_dandelion",         "flower_pot", "contents", "dandelion");
		variant("potted_red_mushroom",      "flower_pot", "contents", "mushroom_red");
		variant("potted_brown_mushroom",    "flower_pot", "contents", "mushroom_brown");
		variant("potted_dead_bush",         "flower_pot", "contents", "dead_bush");
		variant("potted_fern",              "flower_pot", "contents", "fern");
		variant("potted_cactus",            "flower_pot", "contents", "cactus");
		for (String wood : WOOD_TYPES) {
			variant("potted_" + wood + "_sapling", "flower_pot", "contents", wood + "_sapling");
		}

		// === Skull / Head blocks (ID 144) ===
		// One block; the type is in the block entity. The skull on the floor faces up (or down in old worlds).
		for (String skull : new String[]{
			"skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head"
		}) {
			variant(skull, "skull", "facing", "up", "down");
			variant(skull.replace("_skull", "_wall_skull").replace("_head", "_wall_head"), "skull", "facing", HORIZONTAL);
		}

		// === Anvil (ID 145) ===
		variant("anvil",            "anvil", "damage", "0");
		variant("chipped_anvil",    "anvil", "damage", "1");
		variant("damaged_anvil",    "anvil", "damage", "2");

		// === Quartz block (ID 155), prismarine (ID 168) ===
		variant("quartz_block",             "quartz_block", "variant", "default");
		variant("chiseled_quartz_block",    "quartz_block", "variant", "chiseled");
		multi("quartz_pillar",
			block("quartz_block").with("variant", "lines_y").when("axis", "y"),
			block("quartz_block").with("variant", "lines_x").when("axis", "x"),
			block("quartz_block").with("variant", "lines_z").when("axis", "z"));
		variant("prismarine",           "prismarine", "variant", "prismarine");
		variant("prismarine_bricks",    "prismarine", "variant", "prismarine_bricks");
		variant("dark_prismarine",      "prismarine", "variant", "dark_prismarine");

		// === Double plant (ID 175) ===
		// The bottom half carries the variant; the top half takes it from the bottom one in its actual
		// state, so by metadata every top half is every variant.
		variant("sunflower",    "double_plant", "variant", "sunflower");
		variant("lilac",        "double_plant", "variant", "syringa");
		variant("tall_grass",   "double_plant", "variant", "double_grass");
		variant("large_fern",   "double_plant", "variant", "double_fern");
		variant("rose_bush",    "double_plant", "variant", "double_rose");
		variant("peony",        "double_plant", "variant", "paeonia");

		// ==========================================
		// Blockstate property mappings
		// In modern MC, some 1.12.2 block pairs were merged into one block with a state property.
		// ==========================================

		// === Furnace (IDs 61, 62) ===
		multi("furnace", block("furnace").when("lit", "false"), block("lit_furnace").when("lit", "true"));

		// === Redstone ore (IDs 73, 74) ===
		multi("redstone_ore", block("redstone_ore").when("lit", "false"), block("lit_redstone_ore").when("lit", "true"));

		// === Redstone lamp (IDs 123, 124) ===
		multi("redstone_lamp", block("redstone_lamp").when("lit", "false"), block("lit_redstone_lamp").when("lit", "true"));

		// === Daylight detector (IDs 151, 178) ===
		multi("daylight_detector",
			block("daylight_detector").when("inverted", "false"),
			block("daylight_detector_inverted").when("inverted", "true"));

		// === Repeater (IDs 93, 94) ===
		multi("repeater", block("unpowered_repeater").when("powered", "false"), block("powered_repeater").when("powered", "true"));

		// === Comparator (IDs 149, 150) ===
		// Both blocks have "powered", and the game toggles it on the unpowered one.
		multi("comparator", block("unpowered_comparator"), block("powered_comparator"));

		// === Lever (ID 69) ===
		// Modern: face + facing. 1.12.2: one facing, which on the floor and the ceiling keeps the axis only.
		final List<Target> lever = new ArrayList<>();
		lever.add(block("lever").with("facing", "up_z").when("face", "floor").when("facing", "north", "south"));
		lever.add(block("lever").with("facing", "up_x").when("face", "floor").when("facing", "west", "east"));
		lever.add(block("lever").with("facing", "down_z").when("face", "ceiling").when("facing", "north", "south"));
		lever.add(block("lever").with("facing", "down_x").when("face", "ceiling").when("facing", "west", "east"));
		for (String facing : HORIZONTAL) {
			lever.add(block("lever").with("facing", facing).when("face", "wall").when("facing", facing));
		}
		MODERN_TO_LEGACY.put("lever", lever);

		// === Buttons (IDs 77, 143) ===
		// Modern: face + facing. 1.12.2: one facing, up on the floor and down on the ceiling.
		button("stone_button",  "stone_button");
		button("oak_button",    "wooden_button");
	}

	/**
	 * Returns the 1.12.2 BlockEntries for a modern block name + optional state properties.
	 * The name should not include the "minecraft:" namespace prefix.
	 *
	 * When multiple state properties are specified, only the states that satisfy ALL of them are
	 * returned. Properties this table does not translate are left on the entries, to be matched
	 * against the block itself; those the block does not have are treated as wildcards.
	 *
	 * @return null when the name is not in the table; an empty list when it has no 1.12.2 equivalent
	 */
	public static List<BlockEntry> toLegacy(String modernName, Map<String, Set<String>> stateProperties) {
		final List<Target> targets = MODERN_TO_LEGACY.get(modernName);
		if (targets == null) {
			return null;
		}

		final List<BlockEntry> entries = new ArrayList<>(targets.size());
		for (Target target : targets) {
			final Map<String, Set<String>> states = target.narrow(stateProperties);
			if (states != null) {
				entries.add(new BlockEntry(new NamespacedId("minecraft", target.block), Collections.emptySet(), states));
			}
		}
		return entries;
	}

	/**
	 * Whether the name is one only the flattened versions have: it is in the table and no 1.12.2 block is called so.
	 */
	public static boolean isModernName(String name) {
		return MODERN_TO_LEGACY.containsKey(name) && !Block.REGISTRY.containsKey(new ResourceLocation("minecraft", name));
	}

	// ==========================================
	// Helper methods
	// ==========================================

	/**
	 * A 1.12.2 block, the values its state properties have, and the values of the modern properties those states stand for.
	 */
	private record Target(String block, Map<String, Set<String>> states, Map<String, Set<String>> modern) {
		Target with(String property, String... values) {
			final Map<String, Set<String>> states = new HashMap<>(this.states);
			states.put(property, Set.of(values));
			return new Target(block, states, modern);
		}

		Target when(String property, String... values) {
			final Map<String, Set<String>> modern = new HashMap<>(this.modern);
			modern.put(property, Set.of(values));
			return new Target(block, states, modern);
		}

		/** The states of this target a pack's state properties leave, or null when they leave none. */
		Map<String, Set<String>> narrow(Map<String, Set<String>> stateProperties) {
			final Map<String, Set<String>> narrowed = new HashMap<>(states);
			for (Map.Entry<String, Set<String>> property : stateProperties.entrySet()) {
				final Set<String> stoodFor = modern.get(property.getKey());
				if (stoodFor != null) {
					if (Collections.disjoint(stoodFor, property.getValue())) {
						return null;
					}
					continue;
				}

				final Set<String> own = narrowed.get(property.getKey());
				if (own == null) {
					narrowed.put(property.getKey(), property.getValue());
					continue;
				}

				final Set<String> both = new HashSet<>(own);
				both.retainAll(property.getValue());
				if (both.isEmpty()) {
					return null;
				}
				narrowed.put(property.getKey(), both);
			}
			return narrowed;
		}
	}

	private static Target block(String legacy) {
		return new Target(legacy, Collections.emptyMap(), Collections.emptyMap());
	}

	/** Simple rename with no specific state (matches all states). */
	private static void rename(String modern, String legacy) {
		multi(modern, block(legacy));
	}

	/** The states of a block where a property has one of the values. */
	private static void variant(String modern, String legacy, String property, String... values) {
		multi(modern, block(legacy).with(property, values));
	}

	/** Maps a modern name to multiple 1.12.2 blocks or groups of states. */
	private static void multi(String modern, Target... targets) {
		MODERN_TO_LEGACY.put(modern, List.of(targets));
	}

	/** A modern name whose block 1.12.2 does not have, while another 1.12.2 block is called so. */
	private static void absent(String modern) {
		MODERN_TO_LEGACY.put(modern, Collections.emptyList());
	}

	/**
	 * Creates 16 color variant mappings.
	 * Modern names are "{color}_{suffix}", legacy is a single block with a color property.
	 */
	private static void colorVariants(String modernSuffix, String legacyName) {
		for (String color : COLORS) {
			variant(color + "_" + modernSuffix, legacyName, "color", color.equals("light_gray") ? "silver" : color);
		}
	}

	/**
	 * Slab type=bottom/top/double: the half of the single slab, or the double slab block.
	 */
	private static void slab(String modern, Target single, Target doubled) {
		multi(modern,
			single.with("half", "bottom").when("type", "bottom"),
			single.with("half", "top").when("type", "top"),
			doubled.when("type", "double"));
	}

	/**
	 * Button face=floor/wall/ceiling + facing.
	 */
	private static void button(String modern, String legacy) {
		final List<Target> targets = new ArrayList<>();
		targets.add(block(legacy).with("facing", "up").when("face", "floor").when("facing", HORIZONTAL));
		targets.add(block(legacy).with("facing", "down").when("face", "ceiling").when("facing", HORIZONTAL));
		for (String facing : HORIZONTAL) {
			targets.add(block(legacy).with("facing", facing).when("face", "wall").when("facing", facing));
		}
		MODERN_TO_LEGACY.put(modern, targets);
	}
}
