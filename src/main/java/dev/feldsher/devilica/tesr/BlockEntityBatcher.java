/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.tesr;

import dev.feldsher.devilica.client.RenderLimits;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.randomentities.RandomEntities;
import dev.feldsher.devilica.shaders.ShaderHooks;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityEnderChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Draws block entities with known models in one go. Inside a block entity pass ({@link #begin}, {@link #end}) a chest
 * or a sign its vanilla renderer would draw alone adds its posed quads to the group of its texture instead, with its
 * light on every vertex; the pass ends with one draw per group in the state the vanilla renderer draws in, then the
 * signs' text. Anything else (an open lid, the breaking overlay, a renderer or block entity class a mod replaced) still
 * draws itself.
 */
public final class BlockEntityBatcher {
    /** A model quad's vertex with the block entity's light on it */
    static final VertexFormat FORMAT = new VertexFormat()
        .addElement(DefaultVertexFormats.POSITION_3F)
        .addElement(DefaultVertexFormats.COLOR_4UB)
        .addElement(DefaultVertexFormats.TEX_2F)
        .addElement(DefaultVertexFormats.TEX_2S)
        .addElement(DefaultVertexFormats.NORMAL_3B)
        .addElement(DefaultVertexFormats.PADDING_1B);
    private static final int INTS_PER_VERTEX = 8;

    private static final ResourceLocation CHEST = new ResourceLocation("textures/entity/chest/normal.png");
    private static final ResourceLocation CHEST_DOUBLE = new ResourceLocation("textures/entity/chest/normal_double.png");
    private static final ResourceLocation TRAPPED = new ResourceLocation("textures/entity/chest/trapped.png");
    private static final ResourceLocation TRAPPED_DOUBLE = new ResourceLocation("textures/entity/chest/trapped_double.png");
    private static final ResourceLocation CHRISTMAS = new ResourceLocation("textures/entity/chest/christmas.png");
    private static final ResourceLocation CHRISTMAS_DOUBLE = new ResourceLocation("textures/entity/chest/christmas_double.png");
    private static final ResourceLocation ENDER = new ResourceLocation("textures/entity/chest/ender.png");
    private static final ResourceLocation SIGN = new ResourceLocation("textures/entity/sign.png");

    private static final List<Group> GROUPS = new ArrayList<>();
    private static ByteBuffer upload = GLAllocation.createDirectByteBuffer(1 << 20);
    private static int depth;

    private BlockEntityBatcher() {
    }

    /** A block entity pass starts: batchable block entities wait for {@link #end} */
    public static void begin() {
        depth++;
    }

    /** The pass ends: the batched block entities draw */
    public static void end() {
        if (--depth == 0) {
            flush();
        }
    }

    /**
     * The world's way in, before the dispatcher sets up item lighting and the light of this one block entity (about
     * half its cost per block entity): a batchable one is taken here, with its light read from the world.
     *
     * @return whether it was taken (or is out of range, which vanilla draws as nothing)
     */
    public static boolean addFirst(TileEntityRendererDispatcher dispatcher, TileEntity tile, int destroyStage) {
        // The class first: the renderer lookup is a map lookup for every block entity in view
        if (depth == 0 || destroyStage >= 0 || !batchableType(tile) || !tile.hasWorld() || !DevilicaOptions.get().batchBlockEntities) {
            return false;
        }
        final TileEntitySpecialRenderer<TileEntity> renderer = dispatcher.getRenderer(tile);
        if (renderer == null || !batchable(renderer, tile)) {
            return false;
        }
        if (tile.getDistanceSq(dispatcher.entityX, dispatcher.entityY, dispatcher.entityZ) >= RenderLimits.blockEntityDistanceSq(tile, tile.getMaxRenderDistanceSquared())) {
            return true;
        }
        final BlockPos pos = tile.getPos();
        if (!dispatcher.world.isBlockLoaded(pos, false)) {
            return true;
        }
        RandomEntities.beginBlockEntity(tile);
        try {
            return add(renderer, tile, pos.getX() - TileEntityRendererDispatcher.staticPlayerX, pos.getY() - TileEntityRendererDispatcher.staticPlayerY,
                pos.getZ() - TileEntityRendererDispatcher.staticPlayerZ, dispatcher.world.getCombinedLight(pos, 0), ShaderHooks.blockEntityId(tile));
        } finally {
            RandomEntities.end();
        }
    }

    /**
     * Takes a block entity its renderer is about to draw, from where the dispatcher calls the renderer: the caller
     * has set its light, its pack ID and its random look (the shadow pass, mods drawing block entities themselves).
     *
     * @return whether it was taken; false leaves it to its renderer
     */
    public static boolean add(TileEntitySpecialRenderer<?> renderer, TileEntity tile, double x, double y, double z, int destroyStage, float alpha) {
        if (depth == 0 || destroyStage >= 0 || alpha != 1.0F || !tile.hasWorld() || !DevilicaOptions.get().batchBlockEntities || !batchable(renderer, tile)) {
            return false;
        }
        final int light = (int) OpenGlHelper.lastBrightnessY << 16 | (int) OpenGlHelper.lastBrightnessX & 0xFFFF;
        return add(renderer, tile, x, y, z, light, CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
    }

    private static boolean batchableType(TileEntity tile) {
        final Class<?> type = tile.getClass();
        return type == TileEntityChest.class || type == TileEntityEnderChest.class || type == TileEntitySign.class;
    }

    // Vanilla's classes only: a mod's subclass or replacement renderer may draw anything
    private static boolean batchable(TileEntitySpecialRenderer<?> renderer, TileEntity tile) {
        return tile.getClass() == TileEntityChest.class && renderer.getClass() == TileEntityChestRenderer.class
            || tile.getClass() == TileEntityEnderChest.class && renderer.getClass() == TileEntityEnderChestRenderer.class
            || tile.getClass() == TileEntitySign.class && renderer.getClass() == TileEntitySignRenderer.class;
    }

    private static boolean add(TileEntitySpecialRenderer<?> renderer, TileEntity tile, double x, double y, double z, int light, int blockEntityId) {
        if (tile instanceof TileEntityChest chest) {
            return addChest((TileEntityChestRenderer) renderer, chest, x, y, z, light, blockEntityId);
        }
        if (tile instanceof TileEntitySign sign) {
            final boolean standing = sign.getBlockType() == Blocks.STANDING_SIGN;
            final int meta = sign.getBlockMetadata();
            addMesh(ModelMeshes.sign(standing, meta), SIGN, x, y, z, light, blockEntityId);
            SignTexts.add(sign, standing, meta, x, y, z, light, blockEntityId);
            return true;
        }
        final TileEntityEnderChest chest = (TileEntityEnderChest) tile;
        if (chest.lidAngle != 0.0F || chest.prevLidAngle != 0.0F) {
            return false;
        }
        addMesh(ModelMeshes.chest(false, tile.getBlockMetadata(), 0), ENDER, x, y, z, light, blockEntityId);
        return true;
    }

    // TileEntityChestRenderer.render's choices, for a closed lid
    private static boolean addChest(TileEntityChestRenderer renderer, TileEntityChest chest, double x, double y, double z, int light, int blockEntityId) {
        final Block block = chest.getBlockType();
        int meta = chest.getBlockMetadata();
        if (block instanceof BlockChest blockChest && meta == 0) {
            blockChest.checkForSurroundingChests(chest.getWorld(), chest.getPos(), chest.getWorld().getBlockState(chest.getPos()));
            meta = chest.getBlockMetadata();
        }
        chest.checkForAdjacentChests();

        if (chest.adjacentChestZNeg != null || chest.adjacentChestXNeg != null) {
            // The other half draws the double chest
            return true;
        }
        if (chest.lidAngle != 0.0F || chest.prevLidAngle != 0.0F) {
            return false;
        }

        final boolean large = chest.adjacentChestXPos != null || chest.adjacentChestZPos != null;
        final boolean trapped = chest.getChestType() == BlockChest.Type.TRAP;
        final ResourceLocation texture;
        if (renderer.isChristmas) {
            texture = large ? CHRISTMAS_DOUBLE : CHRISTMAS;
        } else if (trapped) {
            texture = large ? TRAPPED_DOUBLE : TRAPPED;
        } else {
            texture = large ? CHEST_DOUBLE : CHEST;
        }
        final int shift = meta == 2 && chest.adjacentChestXPos != null ? 1 : meta == 5 && chest.adjacentChestZPos != null ? 2 : 0;
        addMesh(ModelMeshes.chest(large, meta, shift), texture, x, y, z, light, blockEntityId);
        return true;
    }

    /** @param light the lightmap coordinates, block light low and sky light high, as {@code getCombinedLight} packs them */
    private static void addMesh(int[] mesh, ResourceLocation texture, double x, double y, double z, int light, int blockEntityId) {
        final Group group = group(RandomEntities.choose(texture), blockEntityId);
        final float fx = (float) x;
        final float fy = (float) y;
        final float fz = (float) z;

        int[] data = group.data;
        int at = group.size;
        if (at + mesh.length > data.length) {
            data = group.data = Arrays.copyOf(data, Math.max(data.length * 2, at + mesh.length));
        }
        for (int i = 0; i < mesh.length; i += INTS_PER_VERTEX, at += INTS_PER_VERTEX) {
            data[at] = Float.floatToRawIntBits(Float.intBitsToFloat(mesh[i]) + fx);
            data[at + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(mesh[i + 1]) + fy);
            data[at + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(mesh[i + 2]) + fz);
            data[at + 3] = mesh[i + 3];
            data[at + 4] = mesh[i + 4];
            data[at + 5] = mesh[i + 5];
            data[at + 6] = light;
            data[at + 7] = mesh[i + 7];
        }
        group.size = at;
    }

    private static Group group(ResourceLocation texture, int blockEntityId) {
        for (int i = 0, n = GROUPS.size(); i < n; i++) {
            final Group group = GROUPS.get(i);
            if (group.texture.equals(texture) && group.blockEntityId == blockEntityId) {
                return group;
            }
        }
        final Group group = new Group(texture, blockEntityId);
        GROUPS.add(group);
        return group;
    }

    // In the state TileEntityChestRenderer draws in: item lighting, depth tested and written, white. Sign text after
    // the boards it lies on, as its renderer draws it.
    private static void flush() {
        boolean any = false;
        for (Group group : GROUPS) {
            any |= group.size > 0;
        }
        if (any) {
            drawGroups();
        }
        SignTexts.draw();
    }

    private static void drawGroups() {

        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        final int stride = FORMAT.getSize();
        final List<VertexFormatElement> elements = FORMAT.getElements();
        for (Group group : GROUPS) {
            if (group.size == 0) {
                continue;
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(group.texture);
            CapturedRenderingState.INSTANCE.setCurrentBlockEntity(group.blockEntityId);

            final int bytes = group.size * 4;
            if (upload.capacity() < bytes) {
                upload = GLAllocation.createDirectByteBuffer(Math.max(upload.capacity() * 2, bytes));
            }
            upload.clear();
            upload.asIntBuffer().put(group.data, 0, group.size);
            upload.limit(bytes);

            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).getUsage().preDraw(FORMAT, i, stride, upload);
            }
            GlStateManager.glDrawArrays(GL11.GL_QUADS, 0, group.size / INTS_PER_VERTEX);
            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).getUsage().postDraw(FORMAT, i, stride, upload);
            }
            group.size = 0;
        }
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(0);
        GlStateManager.disableRescaleNormal();
    }

    private static final class Group {
        final ResourceLocation texture;
        final int blockEntityId;
        int[] data = new int[INTS_PER_VERTEX * 4 * 64];
        int size;

        Group(ResourceLocation texture, int blockEntityId) {
            this.texture = texture;
            this.blockEntityId = blockEntityId;
        }
    }
}
