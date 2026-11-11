package org.taumc.celeritas.impl.world.biome;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColorHelper;
import org.taumc.celeritas.impl.world.WorldSlice;

public class BiomeColorCache extends org.embeddedt.embeddium.impl.biome.BiomeColorCache<Biome, BiomeColorHelper.ColorResolver> {
    private final BlockPos.MutableBlockPos biomeCursor = new BlockPos.MutableBlockPos();

    public BiomeColorCache(WorldSlice slice, int blendRadius) {
        super(slice::getBiome, blendRadius);
    }

    @Override
    protected int resolveColor(BiomeColorHelper.ColorResolver colorResolver, Biome biome, int relativeX, int relativeY, int relativeZ) {
        return colorResolver.getColorAtPos(DevilicaOptions.get().details.biomeColors ? biome : Biomes.PLAINS, biomeCursor.setPos(relativeX, relativeY, relativeZ));
    }
}
