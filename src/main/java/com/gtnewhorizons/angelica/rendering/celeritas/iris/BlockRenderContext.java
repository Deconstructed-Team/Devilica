package com.gtnewhorizons.angelica.rendering.celeritas.iris;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.minecraft.block.Block;

public class BlockRenderContext {
    /**
     * Devilica: the block a chunk build thread is meshing now. Celeritas's forge122 mesher sets it before each block
     * (and before each converted run of vanilla buffer quads); the Iris vertex encoder reads it.
     */
    public static final ThreadLocal<BlockRenderContext> CURRENT = ThreadLocal.withInitial(BlockRenderContext::new);

    public int localPosX, localPosY, localPosZ;
    public short blockId = -1;
    public short renderType = -1;
    public byte lightValue;

    public void set(int localX, int localY, int localZ, short blockId, short renderType, byte lightValue) {
        this.localPosX = localX;
        this.localPosY = localY;
        this.localPosZ = localZ;
        this.blockId = blockId;
        this.renderType = renderType;
        this.lightValue = lightValue;
    }

    /** Sets the block from the shader pack's block.properties IDs, by block and metadata as Angelica keys them. */
    public void setBlock(Block block, int metadata, int localX, int localY, int localZ, short renderType, byte lightValue) {
        final Reference2ObjectMap<Block, Int2IntMap> matches = BlockRenderingSettings.INSTANCE.getBlockMetaMatches();
        final Int2IntMap metaMap = matches != null ? matches.get(block) : null;
        set(localX, localY, localZ, (short) (metaMap != null ? metaMap.get(metadata) : -1), renderType, lightValue);
    }

    public void reset() {
        blockId = -1;
        renderType = -1;
        lightValue = 0;
    }
}
