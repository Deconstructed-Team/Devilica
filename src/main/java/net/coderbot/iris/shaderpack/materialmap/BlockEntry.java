package net.coderbot.iris.shaderpack.materialmap;

import lombok.Getter;
import net.coderbot.iris.Iris;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
public class BlockEntry {
	private final NamespacedId id;
	private final Set<Integer> metas;
	/** Block state conditions as OptiFine reads them on 1.8+: a property name and the values it may have. */
	private final Map<String, Set<String>> properties;

	public BlockEntry(NamespacedId id, Set<Integer> metas) {
		this(id, metas, Collections.emptyMap());
	}

	public BlockEntry(NamespacedId id, Set<Integer> metas, Map<String, Set<String>> properties) {
		this.id = id;
		this.metas = metas;
		this.properties = properties;
	}

	/**
	 * Parses a block ID entry.
	 *
	 * @param entry The string representation of the entry. Must not be empty.
	 */
	@NotNull
	public static BlockEntry parse(@NotNull String entry) {
		if (entry.isEmpty()) {
			throw new IllegalArgumentException("Called BlockEntry::parse with an empty string");
		}

		// We can assume that this array is of at least array length because the input string is non-empty.
		final String[] splitStates = entry.split(":");

		// Trivial case: no states, no namespace
		if (splitStates.length == 1) {
			return new BlockEntry(new NamespacedId("minecraft", entry), Collections.emptySet());
		}

        // Examples of what we'll accept
        // stone
        // stone:0
        // minecraft:stone:0
        // minecraft:stone:0,1,2
        // grass:snowy=true
        // minecraft:double_plant:half=lower
        // natura:overworld_leaves:type=maple,tiger

		// Less trivial case: no metas involved, just a namespace
		//
		// The first term MUST be a valid ResourceLocation component
		// The second term, if it is not numeric or a state, must be a valid ResourceLocation component.
		if (splitStates.length == 2 && !isState(splitStates[1])) {
			return new BlockEntry(new NamespacedId(splitStates[0], splitStates[1]), Collections.emptySet());
		}

		// Complex case: One or more states involved...
		final int statesStart;
		final NamespacedId id;

		if (isState(splitStates[1])) {
			// We have an entry of the form "stone:0" or "grass:snowy=true"
			statesStart = 1;
			id = new NamespacedId("minecraft", splitStates[0]);
		} else {
			// We have an entry of the form "minecraft:stone:0"
			statesStart = 2;
			id = new NamespacedId(splitStates[0], splitStates[1]);
		}

        final Set<Integer> metas = new HashSet<>();
        final Map<String, Set<String>> properties = new HashMap<>();

		for (int index = statesStart; index < splitStates.length; index++) {
			final String state = splitStates[index];
			final int equals = state.indexOf('=');

			if (equals > 0) {
				properties.computeIfAbsent(state.substring(0, equals), k -> new HashSet<>())
					.addAll(Arrays.asList(state.substring(equals + 1).split(",")));
				continue;
			}

			// Parse out one or more metadata ids
			for (String metaPart : state.split(",")) {
                try {
                    metas.add(Integer.parseInt(metaPart));
                } catch (NumberFormatException e) {
                    Iris.logger.warn("Warning: the block ID map entry \"" + entry + "\" could not be fully parsed:");
                    Iris.logger.warn("- Metadata ids must be a comma separated list of one or more integers, but "+ state + " is not of that form!");
                }
            }
        }

		return new BlockEntry(id, metas, properties);
	}

	private static boolean isState(String part) {
		return !part.isEmpty() && (StringUtils.isNumeric(part.substring(0, 1)) || part.indexOf('=') > 0);
	}

    @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final BlockEntry that = (BlockEntry) o;
		return Objects.equals(id, that.id) && Objects.equals(metas, that.metas) && Objects.equals(properties, that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, metas, properties);
	}
}
