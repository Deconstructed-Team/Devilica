/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.coloredlight;

import dev.feldsher.devilica.api.light.ColoredLightProvider;
import dev.feldsher.devilica.mixin.lights.BlockStateContainerAccessor;
import dev.feldsher.devilica.mixin.lights.BlockStatePaletteHashMapAccessor;
import dev.feldsher.devilica.mixin.lights.BlockStatePaletteLinearAccessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import net.minecraft.world.chunk.BlockStatePaletteLinear;
import net.minecraft.world.chunk.IBlockStatePalette;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.taumc.celeritas.impl.world.WorldSlice;
import org.taumc.celeritas.impl.world.cloned.ClonedChunkSection;

import java.util.Arrays;

/**
 * The colour of the block light around one section being built, worked out on its builder thread from the 3x3x3
 * sections the build holds. Each source sends red, green and blue apart, the way vanilla light spreads (one level a
 * block, more through what blocks light), so a colour reaches as far as the source's own light. Only the hue is kept:
 * how bright a block is stays vanilla's. With no coloured source near, nothing is worked out and the section is plain.
 */
public final class ColoredLightField {

    /** Plain light as a vertex tint: white, full alpha */
    public static final int PLAIN_TINT = 0xFFFFFFFF;

    private static final ThreadLocal<ColoredLightField> FIELDS = ThreadLocal.withInitial(ColoredLightField::new);

    private static final int SIZE = 48;
    private static final int CELLS = SIZE * SIZE * SIZE;
    /** The section and one block around it, the cells vertices of its geometry take their colour from */
    private static final int BOX_MIN = 15;
    private static final int BOX_MAX = 32;
    private static final int BOX = BOX_MAX - BOX_MIN + 1;
    /** A light level in channel units: level 15 of a full channel is 255 */
    private static final int LEVEL = 17;

    private static final int NO_SOURCE = 0;
    private static final int PLAIN_SOURCE = 1;
    private static final int COLORED_SOURCE = 2;
    private static final int UNKNOWN = 3;

    private final int[] light = new int[CELLS];
    private final byte[] opacity = new byte[CELLS];
    private final int[] box = new int[BOX * BOX * BOX];
    private final IntArrayList[] buckets = new IntArrayList[16];
    private final int[] sectionKinds = new int[27];
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private boolean colored;
    private boolean sourceBlock;

    private ColoredLightField() {
        for (int i = 0; i < this.buckets.length; i++) {
            this.buckets[i] = new IntArrayList();
        }
    }

    public static ColoredLightField forThread() {
        return FIELDS.get();
    }

    public boolean isColored() {
        return this.colored;
    }

    /** Called before each block is meshed: a block giving light keeps its own look, its light is not tinted onto it */
    public void setBlock(IBlockState state) {
        this.sourceBlock = this.colored && state.getLightValue() > 0;
    }

    /** Works out the colour for the section whose lowest corner is at the given block */
    public void compute(WorldSlice slice, int minX, int minY, int minZ) {
        this.colored = false;
        this.sourceBlock = false;
        final ColoredLightProvider provider = ColoredLight.provider();
        if (provider != null) {
            this.fromProvider(provider, slice, minX, minY, minZ);
            return;
        }
        if (!this.classifySections(slice)) {
            return;
        }
        Arrays.fill(this.light, 0);
        Arrays.fill(this.opacity, (byte) -1);
        if (!this.seed(slice, minX - 16, minY - 16, minZ - 16)) {
            return;
        }
        this.spread(slice, minX - 16, minY - 16, minZ - 16);
        this.fillBox();
        this.colored = true;
    }

    /**
     * The tint a vertex at a point of the section (relative to its lowest corner) gets: the colour of the cells around
     * the point, weighted by how close and how bright they are, packed as RGBA bytes.
     */
    public int tintAt(float x, float y, float z) {
        if (!this.colored || this.sourceBlock) {
            return PLAIN_TINT;
        }
        final float px = x + 0.5F;
        final float py = y + 0.5F;
        final float pz = z + 0.5F;
        final int x0 = (int) Math.floor(px);
        final int y0 = (int) Math.floor(py);
        final int z0 = (int) Math.floor(pz);
        final float fx = px - x0;
        final float fy = py - y0;
        final float fz = pz - z0;

        float r = 0;
        float g = 0;
        float b = 0;
        for (int corner = 0; corner < 8; corner++) {
            final int dx = corner & 1;
            final int dy = corner >> 1 & 1;
            final int dz = corner >> 2;
            final int cell = this.box[boxIndex(clampBox(x0 + dx), clampBox(y0 + dy), clampBox(z0 + dz))];
            if (cell == 0) {
                continue;
            }
            final float weight = (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy) * (dz == 0 ? 1 - fz : fz);
            r += weight * (cell >> 16 & 0xFF);
            g += weight * (cell >> 8 & 0xFF);
            b += weight * (cell & 0xFF);
        }
        final float max = Math.max(r, Math.max(g, b));
        if (max <= 0) {
            return PLAIN_TINT;
        }
        final int tr = Math.round(r / max * 255);
        final int tg = Math.round(g / max * 255);
        final int tb = Math.round(b / max * 255);
        return 0xFF000000 | tb << 16 | tg << 8 | tr;
    }

    /** Box coordinates run from the block below the section's lowest corner (0) to the one past its top (17) */
    private static int clampBox(int coordinate) {
        return Math.max(0, Math.min(BOX - 1, coordinate));
    }

    private static int boxIndex(int x, int y, int z) {
        return (y * BOX + z) * BOX + x;
    }

    private static int index(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }

    /** False when no section around holds a coloured source, so the section stays plain */
    private boolean classifySections(WorldSlice slice) {
        boolean anyColored = false;
        for (int i = 0; i < 27; i++) {
            final ClonedChunkSection section = slice.getClonedSection(i % 3, i / 3 % 3, i / 9);
            final int kind = section != null ? kindOf(section.getStorage()) : NO_SOURCE;
            this.sectionKinds[i] = kind;
            anyColored |= kind == COLORED_SOURCE || kind == UNKNOWN;
        }
        return anyColored;
    }

    /** What the section's palette says it can hold: no source, plain ones, coloured ones, or (no palette to read) maybe */
    private static int kindOf(ExtendedBlockStorage storage) {
        if (storage == null || storage.isEmpty()) {
            return NO_SOURCE;
        }
        final IBlockStatePalette palette = ((BlockStateContainerAccessor) storage.getData()).devilica$palette();
        int kind = NO_SOURCE;
        if (palette instanceof BlockStatePaletteLinear) {
            final BlockStatePaletteLinearAccessor linear = (BlockStatePaletteLinearAccessor) palette;
            final IBlockState[] states = linear.devilica$states();
            for (int i = 0, n = linear.devilica$arraySize(); i < n; i++) {
                kind = Math.max(kind, sourceKind(states[i]));
            }
        } else if (palette instanceof BlockStatePaletteHashMap) {
            for (IBlockState state : ((BlockStatePaletteHashMapAccessor) palette).devilica$map()) {
                kind = Math.max(kind, sourceKind(state));
            }
        } else {
            return UNKNOWN;
        }
        return kind;
    }

    private static int sourceKind(IBlockState state) {
        if (state == null) {
            return NO_SOURCE;
        }
        if (LightColors.hasColoredStates(state.getBlock()) && LightColors.colorOf(state) != LightColors.PLAIN) {
            return COLORED_SOURCE;
        }
        return state.getLightValue() > 0 ? PLAIN_SOURCE : NO_SOURCE;
    }

    /** Puts every source of the 3x3x3 sections in; false when none of them turns out coloured */
    private boolean seed(WorldSlice slice, int baseX, int baseY, int baseZ) {
        boolean anyColored = false;
        for (int i = 0; i < 27; i++) {
            if (this.sectionKinds[i] == NO_SOURCE) {
                continue;
            }
            final int sx = i % 3;
            final int sy = i / 3 % 3;
            final int sz = i / 9;
            final ExtendedBlockStorage storage = slice.getClonedSection(sx, sy, sz).getStorage();
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        final IBlockState state = storage.get(lx, ly, lz);
                        final int kind = sourceKind(state);
                        if (kind == NO_SOURCE) {
                            continue;
                        }
                        final int x = sx * 16 + lx;
                        final int y = sy * 16 + ly;
                        final int z = sz * 16 + lz;
                        final int level = lightValue(state, slice, baseX + x, baseY + y, baseZ + z);
                        if (level <= 0) {
                            continue;
                        }
                        final int rgb = kind == COLORED_SOURCE ? LightColors.colorOf(state) : LightColors.PLAIN;
                        anyColored |= rgb != LightColors.PLAIN;
                        this.raise(index(x, y, z), channel(level, rgb >> 16), channel(level, rgb >> 8), channel(level, rgb));
                    }
                }
            }
        }
        return anyColored;
    }

    private int lightValue(IBlockState state, WorldSlice slice, int x, int y, int z) {
        try {
            return state.getLightValue(slice, this.pos.setPos(x, y, z));
        } catch (RuntimeException e) {
            return state.getLightValue();
        }
    }

    private static int channel(int level, int color) {
        return level * LEVEL * (color & 0xFF) / 255;
    }

    /** Takes the brighter of each channel; a cell whose light grew goes in the queue of its new level */
    private void raise(int cell, int r, int g, int b) {
        final int old = this.light[cell];
        final int nr = Math.max(old >> 16 & 0xFF, r);
        final int ng = Math.max(old >> 8 & 0xFF, g);
        final int nb = Math.max(old & 0xFF, b);
        final int merged = nr << 16 | ng << 8 | nb;
        if (merged != old) {
            this.light[cell] = merged;
            this.buckets[Math.max(r, Math.max(g, b)) / LEVEL].add(cell);
        }
    }

    private void spread(WorldSlice slice, int baseX, int baseY, int baseZ) {
        for (int level = 15; level > 1; level--) {
            final IntArrayList bucket = this.buckets[level];
            for (int i = 0; i < bucket.size(); i++) {
                final int cell = bucket.getInt(i);
                final int value = this.light[cell];
                final int x = cell % SIZE;
                final int z = cell / SIZE % SIZE;
                final int y = cell / (SIZE * SIZE);
                this.spreadTo(slice, value, x - 1, y, z);
                this.spreadTo(slice, value, x + 1, y, z);
                this.spreadTo(slice, value, x, y - 1, z);
                this.spreadTo(slice, value, x, y + 1, z);
                this.spreadTo(slice, value, x, y, z - 1);
                this.spreadTo(slice, value, x, y, z + 1);
            }
            bucket.clear();
        }
        this.buckets[0].clear();
        this.buckets[1].clear();
    }

    private void spreadTo(WorldSlice slice, int value, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE) {
            return;
        }
        final int cell = index(x, y, z);
        final int opacity = this.opacityAt(slice, cell, x, y, z);
        if (opacity >= 15) {
            return;
        }
        final int cost = Math.max(1, opacity) * LEVEL;
        final int r = Math.max(0, (value >> 16 & 0xFF) - cost);
        final int g = Math.max(0, (value >> 8 & 0xFF) - cost);
        final int b = Math.max(0, (value & 0xFF) - cost);
        final int max = Math.max(r, Math.max(g, b));
        // A cell whose light runs out before the box is of no use
        if (max <= LEVEL * distanceToBox(x, y, z)) {
            return;
        }
        this.raise(cell, r, g, b);
    }

    private static int distanceToBox(int x, int y, int z) {
        return Math.max(0, Math.max(BOX_MIN - x, x - BOX_MAX))
            + Math.max(0, Math.max(BOX_MIN - y, y - BOX_MAX))
            + Math.max(0, Math.max(BOX_MIN - z, z - BOX_MAX));
    }

    private int opacityAt(WorldSlice slice, int cell, int x, int y, int z) {
        final byte cached = this.opacity[cell];
        if (cached >= 0) {
            return cached;
        }
        final ClonedChunkSection section = slice.getClonedSection(x >> 4, y >> 4, z >> 4);
        final ExtendedBlockStorage storage = section != null ? section.getStorage() : null;
        final int value = storage == null || storage.isEmpty() ? 0 : Math.min(15, storage.get(x & 15, y & 15, z & 15).getLightOpacity());
        this.opacity[cell] = (byte) value;
        return value;
    }

    private void fillBox() {
        for (int y = 0; y < BOX; y++) {
            for (int z = 0; z < BOX; z++) {
                for (int x = 0; x < BOX; x++) {
                    this.box[boxIndex(x, y, z)] = this.light[index(BOX_MIN + x, BOX_MIN + y, BOX_MIN + z)];
                }
            }
        }
    }

    private void fromProvider(ColoredLightProvider provider, WorldSlice slice, int minX, int minY, int minZ) {
        boolean any = false;
        for (int y = 0; y < BOX; y++) {
            for (int z = 0; z < BOX; z++) {
                for (int x = 0; x < BOX; x++) {
                    final int rgb = provider.getBlockLightRGB(slice, minX - 1 + x, minY - 1 + y, minZ - 1 + z);
                    final int value = rgb < 0 ? 0 : (rgb >> 8 & 0xF) * LEVEL << 16 | (rgb >> 4 & 0xF) * LEVEL << 8 | (rgb & 0xF) * LEVEL;
                    this.box[boxIndex(x, y, z)] = value;
                    any |= value != 0;
                }
            }
        }
        this.colored = any;
    }
}
