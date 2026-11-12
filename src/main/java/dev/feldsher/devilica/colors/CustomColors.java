/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.colors;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.ctm.OverlayTints;
import dev.feldsher.devilica.mixin.features.colors.FontRendererAccessor;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OptiFine's and MCPatcher's custom colours from resource packs: block colour maps (water, leaves, swamps, redstone,
 * stems, any block by palette), fog and sky by biome, the nether's and end's fog, lightmaps, text, potion and
 * particle colours.
 */
public final class CustomColors {

    public static final CustomColors INSTANCE = new CustomColors();
    /** What the hooks answer when the packs leave a colour alone */
    public static final int KEEP = Integer.MIN_VALUE;
    /** The tint index Devilica gives the untinted faces of a block with a pack's colour map */
    public static final int UNTINTED = 0x4445564C;

    private static final IBlockColor THROUGH_BLOCK_COLORS = (state, world, pos, tint) -> Minecraft.getMinecraft().getBlockColors().colorMultiplier(state, world, pos, tint);
    private static final Map<BakedQuad, BakedQuad> RETINTED = new ConcurrentHashMap<>();
    private static final Map<FontRenderer, int[]> FONT_CODES = new WeakHashMap<>();

    private static volatile Palette palette = Palette.EMPTY;

    private CustomColors() {
    }

    // ---- loading

    @SubscribeEvent
    public void onStitchPost(TextureStitchEvent.Post event) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (event.getMap() != minecraft.getTextureMapBlocks()) {
            return;
        }
        // At the block atlas's end, so the chunks the renderer rebuilds after a reload see the new colours
        Palette loaded;
        try {
            loaded = PaletteLoader.load(minecraft.getResourceManager());
        } catch (RuntimeException e) {
            Devilica.LOGGER.warn("Custom colours: a pack's colour file is malformed, none are used: {}", e.toString());
            loaded = Palette.EMPTY;
        }
        RETINTED.clear();
        palette = loaded;
        applyFontCodes();
        if (loaded.count > 0) {
            Devilica.LOGGER.info("Custom colours: {} colour maps and colours", loaded.count);
        }
    }

    /** The count for F3, 0 when off */
    public static int count() {
        return active() == null ? 0 : palette.count;
    }

    private static Palette active() {
        final Palette current = palette;
        return current == Palette.EMPTY || !DevilicaOptions.get().customColors ? null : current;
    }

    /** Puts the packs' text colour codes into the fonts, or the fonts' own back */
    public static void applyFontCodes() {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final Palette current = active();
        for (FontRenderer font : new FontRenderer[]{minecraft.fontRenderer, minecraft.standardGalacticFontRenderer}) {
            if (font == null) {
                continue;
            }
            final int[] codes = ((FontRendererAccessor) font).devilica$colorCodes();
            final int[] own = FONT_CODES.computeIfAbsent(font, f -> codes.clone());
            for (int i = 0; i < codes.length; i++) {
                codes[i] = current != null && current.textCodes[i] >= 0 ? current.textCodes[i] : own[i];
            }
        }
    }

    // ---- blocks

    /** A pack's colour for a block, or {@link #KEEP} */
    public static int blockColor(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex) {
        final Palette current = active();
        if (current == null) {
            return KEEP;
        }
        final IBlockState key = clean(state);
        if (tintIndex == UNTINTED) {
            final IBlockColor color = current.untinted.get(key);
            return color == null ? -1 : color.colorMultiplier(state, world, pos, tintIndex);
        }
        final IBlockColor color = current.blocks.get(key);
        return color == null ? KEEP : color.colorMultiplier(state, world, pos, tintIndex);
    }

    /** The colour provider Celeritas's block renderer takes for a block: through the block colours when a pack colours it */
    public static IBlockColor provider(IBlockState state, IBlockColor own) {
        final Palette current = active();
        if (current == null) {
            return own;
        }
        final IBlockState key = clean(state);
        return current.blocks.containsKey(key) || current.untinted.containsKey(key) ? THROUGH_BLOCK_COLORS : own;
    }

    /** The provider for one quad: the block colours for the tint indices Devilica gives (untinted faces, overlays) */
    public static IBlockColor forQuad(IBlockColor provider, int tintIndex) {
        return tintIndex == UNTINTED || OverlayTints.isCode(tintIndex) ? THROUGH_BLOCK_COLORS : provider;
    }

    /** Whether a pack's colour map also colours the block's untinted faces */
    public static boolean tintsUntinted(IBlockState state) {
        final Palette current = active();
        return current != null && current.untinted.containsKey(clean(state));
    }

    /** The quads with their untinted ones tinted by {@link #UNTINTED} */
    public static List<BakedQuad> tintUntinted(List<BakedQuad> quads) {
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            final BakedQuad quad = quads.get(i);
            if (quad.hasTintIndex()) {
                if (result != null) {
                    result.add(quad);
                }
                continue;
            }
            if (result == null) {
                result = new ArrayList<>(quads.size());
                result.addAll(quads.subList(0, i));
            }
            result.add(RETINTED.computeIfAbsent(quad, q -> new BakedQuad(q.getVertexData(), UNTINTED, q.getFace(), q.getSprite(), q.shouldApplyDiffuseLighting(), q.getFormat())));
        }
        return result == null ? quads : result;
    }

    private static IBlockState clean(IBlockState state) {
        return state instanceof IExtendedBlockState extended ? extended.getClean() : state;
    }

    /** Swamp grass and leaves from {@code swampgrass.png} and {@code swampfoliage.png} */
    public static int swamp(Biome biome, BlockPos pos, boolean foliage) {
        final Palette current = active();
        final Colormap map = current == null ? null : foliage ? current.swampFoliage : current.swampGrass;
        return map == null ? KEEP : map.colorAt(biome, pos);
    }

    // ---- fog and sky

    /** The overworld's fog by the biomes around the camera, at vanilla's brightness for the time of day */
    public static Vec3d overworldFog(Vec3d fog) {
        final Palette current = active();
        final Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        if (current == null || current.fog == null || camera == null) {
            return fog;
        }
        final int color = current.fog.blended(camera.world, new BlockPos(camera.posX, camera.posY + 1.0, camera.posZ), 3);
        // Vanilla's daytime fog is 0.753, 0.847, 1.0; the map takes its place at the same brightness
        return new Vec3d(fog.x / 0.7529412 * (color >> 16 & 0xFF) / 255.0, fog.y / 0.84705883 * (color >> 8 & 0xFF) / 255.0, fog.z * (color & 0xFF) / 255.0);
    }

    /** One biome's sky colour from {@code sky0.png}, in the overworld */
    public static int skyColor(World world, Biome biome, float temperature, BlockPos pos) {
        final Palette current = active();
        if (current == null || current.sky == null || world.provider.getDimension() != 0) {
            return KEEP;
        }
        return current.sky.colorAt(biome, temperature, pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3d netherFog(Vec3d fog) {
        final Palette current = active();
        return current == null || current.netherFog == null ? fog : current.netherFog;
    }

    public static Vec3d endFog(Vec3d fog) {
        final Palette current = active();
        return current == null || current.endFog == null ? fog : current.endFog;
    }

    /** The fog under water and lava from {@code underwater.png} and {@code underlava.png} */
    public static Vec3d inMaterialFog(World world, BlockPos pos, IBlockState state, Vec3d fog) {
        final Palette current = active();
        if (current == null) {
            return fog;
        }
        final Material material = state.getMaterial();
        final Colormap map = material == Material.WATER ? current.underwater : material == Material.LAVA ? current.underlava : null;
        if (map == null) {
            return fog;
        }
        final int color = map.blended(world, pos, 2);
        return new Vec3d((color >> 16 & 0xFF) / 255.0, (color >> 8 & 0xFF) / 255.0, (color & 0xFF) / 255.0);
    }

    /** The end sky's tint, or -1 */
    public static int endSky() {
        final Palette current = active();
        return current == null ? -1 : current.endSky;
    }

    public static int clouds(int clouds) {
        final Palette current = active();
        return current == null || current.clouds < 0 || clouds == 0 ? clouds : current.clouds;
    }

    // ---- lightmaps

    /** Fills the lightmap from the dimension's {@code lightmap/world<id>.png}; false when there is none */
    public static boolean lightmap(World world, int[] colors, float torchFlicker, float nightVision) {
        final Palette current = active();
        if (current == null || current.lightmaps.isEmpty()) {
            return false;
        }
        final Lightmap lightmap = current.lightmaps.get(world.provider.getDimension());
        if (lightmap == null) {
            return false;
        }
        final float gamma = MathHelper.clamp(Minecraft.getMinecraft().gameSettings.gammaSetting, 0.0F, 1.0F);
        lightmap.compute(world, colors, torchFlicker, nightVision, gamma);
        return true;
    }

    // ---- text, potions, particles

    /** A drawn text colour the packs map to another ({@code text.<from>=<to>}) */
    public static int text(int color) {
        final Palette current = active();
        if (current == null || current.text.isEmpty()) {
            return color;
        }
        final int mapped = current.text.get(color & 0xFFFFFF);
        return mapped < 0 ? color : color & 0xFF000000 | mapped;
    }

    public static int xpBar(int color) {
        final Palette current = active();
        return current == null || current.xpBar < 0 ? color : current.xpBar;
    }

    public static int bossName(int color) {
        final Palette current = active();
        return current == null || current.boss < 0 ? color : current.boss;
    }

    public static int signText(int color) {
        final Palette current = active();
        return current == null || current.sign < 0 ? color : current.sign;
    }

    public static int potion(Potion potion, int color) {
        final Palette current = active();
        if (current == null) {
            return color;
        }
        final int custom = current.potions.getInt(potion);
        return custom < 0 ? color : custom;
    }

    public static int waterBottle(int color) {
        final Palette current = active();
        return current == null || current.waterBottle < 0 ? color : current.waterBottle;
    }

    /** Rain, splashes and drips take {@code particle.water}, else the water colour map where they are */
    public static void waterParticle(Particle particle, World world, double x, double y, double z) {
        final Palette current = active();
        if (current == null) {
            return;
        }
        int color = current.waterParticles;
        if (color < 0 && current.water != null) {
            color = current.water.color(world, new BlockPos(x, y, z));
        }
        if (color >= 0) {
            particle.setRBGColorF((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
        }
    }

    /** Mycelium's particles in a random colour of {@code myceliumparticle.png} */
    public static void myceliumParticle(Particle particle, Random random) {
        final Palette current = active();
        if (current != null && current.mycelium != null) {
            final int color = current.mycelium.entry(random.nextInt(current.mycelium.size()));
            particle.setRBGColorF((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
        }
    }

    /** A redstone wire's particle colour by power from {@code redstone.png}, or null */
    public static float[] redstoneParticle(int power) {
        final Palette current = active();
        if (current == null || current.redstone == null) {
            return null;
        }
        final int color = current.redstone.entry(power);
        // Redstone particles read a red of 0 as 1
        return new float[]{Math.max(0.001F, (color >> 16 & 0xFF) / 255.0F), (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F};
    }

    /** An experience orb's colour from {@code xporb.png}, by vanilla's red for the moment (its pulse), or -1 */
    public static int xpOrb(int red) {
        final Palette current = active();
        return current == null || current.xpOrb == null ? -1 : current.xpOrb.entry(Math.round(red / 255.0F * (current.xpOrb.size() - 1)));
    }

    /** The durability bar's colour from {@code durability.png}, by how much durability is left */
    public static int durability(double damage, int color) {
        final Palette current = active();
        if (current == null || current.durability == null) {
            return color;
        }
        final double left = MathHelper.clamp(1.0 - damage, 0.0, 1.0);
        return current.durability.entry((int) Math.round(left * (current.durability.size() - 1)));
    }

    public static float[] sheep(EnumDyeColor dye, float[] color) {
        final Palette current = active();
        return current == null || current.sheep[dye.getMetadata()] < 0 ? color : components(current.sheep[dye.getMetadata()]);
    }

    public static float[] collar(EnumDyeColor dye, float[] color) {
        final Palette current = active();
        return current == null || current.collar[dye.getMetadata()] < 0 ? color : components(current.collar[dye.getMetadata()]);
    }

    /** A dye's colour from {@code dye.<color>} (banners, shields), as components */
    public static float[] dyeComponents(EnumDyeColor dye, float[] color) {
        final Palette current = active();
        return current == null || current.dye[dye.getMetadata()] < 0 ? color : components(current.dye[dye.getMetadata()]);
    }

    public static int dyeValue(EnumDyeColor dye, int color) {
        final Palette current = active();
        return current == null || current.dye[dye.getMetadata()] < 0 ? color : current.dye[dye.getMetadata()];
    }

    /** A spawn egg's shell (tint 0) or spots (tint 1) colour from {@code egg.shell.<entity>}, {@code egg.spots.<entity>}, or {@link #KEEP} */
    public static int spawnEgg(ItemStack stack, int tintIndex) {
        final Palette current = active();
        if (current == null || (current.eggShell.isEmpty() && current.eggSpots.isEmpty())) {
            return KEEP;
        }
        final ResourceLocation id = ItemMonsterPlacer.getNamedIdFrom(stack);
        if (id == null) {
            return KEEP;
        }
        final Map<String, Integer> colors = tintIndex == 0 ? current.eggShell : current.eggSpots;
        Integer color = colors.get(PaletteLoader.normalName(id.getPath()));
        if (color == null) {
            final String legacy = EntityList.getTranslationName(id);
            color = legacy == null ? null : colors.get(PaletteLoader.normalName(legacy));
        }
        return color == null ? KEEP : color;
    }

    public static int mapColor(MapColor mapColor, int color) {
        final Palette current = active();
        return current == null || current.mapColors[mapColor.colorIndex] < 0 ? color : current.mapColors[mapColor.colorIndex];
    }

    private static float[] components(int color) {
        return new float[]{(color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F};
    }

    /** A portal particle in {@code particle.portal}'s colour at its own brightness */
    public static void portalParticle(Particle particle, float brightness) {
        final Palette current = active();
        if (current == null || current.portalParticles < 0) {
            return;
        }
        final int color = current.portalParticles;
        particle.setRBGColorF((color >> 16 & 0xFF) / 255.0F * brightness, (color >> 8 & 0xFF) / 255.0F * brightness, (color & 0xFF) / 255.0F * brightness);
    }
}
