/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.colors.CustomColors;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.resources.FileSprite;
import dev.feldsher.devilica.resources.ResourcePackFiles;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OptiFine's connected textures on 1.12 models: rules come from {@code mcpatcher/ctm} and {@code optifine/ctm} in the
 * resource packs, their tiles are stitched into the block atlas, and a block's quads have their texture swapped for
 * a tile picked from the neighbours while chunks are built.
 */
public final class ConnectedTextures {

    public static final ConnectedTextures INSTANCE = new ConnectedTextures();

    private static final String[] DIRECTORIES = {"mcpatcher/ctm/", "optifine/ctm/"};
    private static final TextureAtlasSprite[] NO_SPRITES = new TextureAtlasSprite[6];

    /** The rules being loaded, between the stitch's start and end */
    private List<TileOverride> loading = Collections.emptyList();
    /** What chunk builds read; replaced as a whole when the atlas is stitched again */
    private static volatile Index index = Index.EMPTY;
    private static int ruleCount;
    /** A block state's texture on each face, for connecting by texture */
    private static final Map<IBlockState, TextureAtlasSprite[]> FACE_SPRITES = new ConcurrentHashMap<>();
    /** The layers a block state is drawn in only for overlays */
    private static final Map<IBlockState, Integer> OVERLAY_LAYERS = new ConcurrentHashMap<>();

    record Index(Map<Block, TileOverride[]> byBlock, Map<TextureAtlasSprite, TileOverride[]> bySprite,
                 Map<TextureAtlasSprite, NaturalTextures.Natural> natural, boolean hasOverlays) {
        static final Index EMPTY = new Index(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);

        boolean hasRules() {
            return !this.byBlock.isEmpty() || !this.bySprite.isEmpty();
        }
    }

    private ConnectedTextures() {
    }

    // ---- loading

    @SubscribeEvent
    public void onStitchPre(TextureStitchEvent.Pre event) {
        final TextureMap map = event.getMap();
        if (map != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }
        final IResourceManager resources = Minecraft.getMinecraft().getResourceManager();
        final List<TileOverride> overrides = new ArrayList<>();
        final Set<String> registered = new HashSet<>();
        for (ResourceLocation location : ResourcePackFiles.find(".properties", DIRECTORIES)) {
            final CtmProperties properties;
            try (IResource resource = resources.getResource(location); Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                final Properties values = new Properties();
                values.load(reader);
                properties = new CtmProperties(location, values);
            } catch (IOException | RuntimeException e) {
                Devilica.LOGGER.warn("Connected textures: could not read {}: {}", location, e.toString());
                continue;
            }
            final TileOverride override;
            try {
                override = properties.create();
            } catch (RuntimeException e) {
                Devilica.LOGGER.warn("Connected textures: {} is malformed: {}", location, e.toString());
                continue;
            }
            for (String error : properties.errors) {
                Devilica.LOGGER.warn("Connected textures: {}: {}", location, error);
            }
            if (override == null) {
                continue;
            }
            for (String[] tile : properties.tileFiles) {
                if (registered.add(tile[0]) && map.getTextureExtry(tile[0]) == null) {
                    map.setTextureEntry(new FileSprite(tile[0], new ResourceLocation(tile[1])));
                }
            }
            for (String name : override.tileNames) {
                if (name != null && registered.add(name) && map.getTextureExtry(name) == null) {
                    map.registerSprite(new ResourceLocation(name));
                }
            }
            overrides.add(override);
        }
        this.loading = overrides;
        EmissiveTextures.register(resources, map);
    }

    @SubscribeEvent
    public void onStitchPost(TextureStitchEvent.Post event) {
        final TextureMap map = event.getMap();
        if (map != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return;
        }
        final TextureAtlasSprite missing = map.getMissingSprite();
        final Map<Block, List<TileOverride>> byBlock = new HashMap<>();
        final Map<TextureAtlasSprite, List<TileOverride>> bySprite = new HashMap<>();
        for (TileOverride override : this.loading) {
            override.tiles = new TextureAtlasSprite[override.tileNames.length];
            for (int i = 0; i < override.tiles.length; i++) {
                final String name = override.tileNames[i];
                final TextureAtlasSprite sprite = name == null ? null : map.getAtlasSprite(name);
                override.tiles[i] = sprite == missing ? null : sprite;
            }
            for (BlockMatcher matcher : override.matchBlocks) {
                byBlock.computeIfAbsent(matcher.block, b -> new ArrayList<>()).add(override);
            }
            for (String tile : override.matchTiles) {
                final TextureAtlasSprite sprite = map.getAtlasSprite(tile);
                if (sprite != missing) {
                    bySprite.computeIfAbsent(sprite, s -> new ArrayList<>()).add(override);
                }
            }
        }
        final boolean hasOverlays = this.loading.stream().anyMatch(TileOverride::isOverlay);
        index = new Index(sorted(byBlock), sorted(bySprite), NaturalTextures.load(Minecraft.getMinecraft().getResourceManager(), map), hasOverlays);
        BetterGrass.load(map);
        EmissiveTextures.resolve(map);
        ruleCount = this.loading.size();
        FACE_SPRITES.clear();
        OVERLAY_LAYERS.clear();
        if (!this.loading.isEmpty()) {
            Devilica.LOGGER.info("Connected textures: {} rules for {} blocks and {} textures", this.loading.size(), byBlock.size(), bySprite.size());
        }
        this.loading = Collections.emptyList();
    }

    private static <K> Map<K, TileOverride[]> sorted(Map<K, List<TileOverride>> lists) {
        final Map<K, TileOverride[]> result = new Reference2ObjectOpenHashMap<>(lists.size());
        lists.forEach((key, list) -> {
            final TileOverride[] array = list.toArray(new TileOverride[0]);
            Arrays.sort(array);
            result.put(key, array);
        });
        return result;
    }

    /** How many rules the resource packs have, for the F3 lines */
    public static int ruleCount() {
        return ruleCount;
    }

    // ---- rendering

    /** A block's item model with the tiles of a block that has no neighbours; the model itself when no rule applies */
    public static IBakedModel withHeldTiles(IBakedModel model, ItemStack stack) {
        final Index current = index;
        if (!current.hasRules() || !DevilicaOptions.get().connectedTextures || !(stack.getItem() instanceof ItemBlock item)) {
            return model;
        }
        final Block block = item.getBlock();
        if (!current.byBlock.containsKey(block) && !showsRuledSprite(model, current)) {
            return model;
        }
        return new HeldCtmModel(model, current, block, item.getMetadata(stack.getMetadata()));
    }

    private static boolean showsRuledSprite(IBakedModel model, Index current) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            for (BakedQuad quad : model.getQuads(null, facing, 0L)) {
                if (current.bySprite.containsKey(quad.getSprite())) {
                    return true;
                }
            }
        }
        for (BakedQuad quad : model.getQuads(null, null, 0L)) {
            if (current.bySprite.containsKey(quad.getSprite())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The model as drawn at a position, with better grass, connected and natural textures applied; the model itself
     * when none of them can apply.
     */
    public static IBakedModel wrap(IBakedModel model, IBlockAccess world, IBlockState state, BlockPos pos) {
        final Index current = index;
        final DevilicaOptions options = DevilicaOptions.get();
        final boolean ctm = options.connectedTextures && current.hasRules();
        final boolean natural = options.naturalTextures && !current.natural.isEmpty();
        final boolean grass = options.betterGrass != BetterGrassMode.OFF && BetterGrass.affects(state);
        final boolean emissive = EmissiveTextures.active();
        final boolean tint = CustomColors.tintsUntinted(state);
        if (!ctm && !natural && !grass && !emissive && !tint) {
            return model;
        }
        final IBlockState plain = state instanceof IExtendedBlockState extended ? extended.getClean() : state;
        return new CtmModel(model, world, pos, plain, current, ctm, natural, grass ? options.betterGrass : BetterGrassMode.OFF, emissive, tint);
    }

    /**
     * The quads of the pass being drawn: the block's own, changed by the settings, then the overlays of this pass and
     * the glowing copies. A pass the block is drawn in only for those ({@link #extraLayers}) holds only them.
     */
    static List<BakedQuad> process(List<BakedQuad> quads, CtmModel model) {
        final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
        final IBlockState state = model.state();
        final boolean ownPass = layer == null || state.getBlock().canRenderInLayer(state, layer);
        List<BakedQuad> drawn = quads;
        if (ownPass || model.emissive) {
            drawn = retexture(quads, model);
            if (model.tint) {
                drawn = CustomColors.tintUntinted(drawn);
            }
        }
        List<BakedQuad> extra = null;
        if (model.ctm && layer != null && model.index.hasOverlays()) {
            extra = overlays(quads, model, layer);
        }
        if (model.emissive) {
            extra = EmissiveTextures.addGlows(drawn, state, layer, ownPass, extra);
        }
        if (extra == null || extra.isEmpty()) {
            return ownPass ? drawn : List.of();
        }
        if (!ownPass) {
            return extra;
        }
        final List<BakedQuad> result = new ArrayList<>(drawn.size() + extra.size());
        result.addAll(drawn);
        result.addAll(extra);
        return result;
    }

    /** The overlay quads of the rules drawn in this pass, over the faces they apply to */
    private static List<BakedQuad> overlays(List<BakedQuad> quads, CtmModel model, BlockRenderLayer layer) {
        final TileOverride[] forBlock = model.index.byBlock.get(model.state().getBlock());
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            final BakedQuad quad = quads.get(i);
            final EnumFacing face = quad.getFace();
            final TileOverride[] forSprite = model.index.bySprite.get(quad.getSprite());
            if (face == null || (forBlock == null && forSprite == null)) {
                continue;
            }
            final CtmContext context = model.context();
            context.setFace(face.getIndex(), quad.getSprite(), 0);
            out = addOverlays(quad, context, forBlock, layer, out);
            out = addOverlays(quad, context, forSprite, layer, out);
        }
        return out;
    }

    private static List<BakedQuad> addOverlays(BakedQuad quad, CtmContext context, TileOverride[] rules, BlockRenderLayer layer, List<BakedQuad> out) {
        if (rules == null) {
            return out;
        }
        for (TileOverride rule : rules) {
            if (rule instanceof Overlay overlay && overlay.layer == layer) {
                if (out == null) {
                    out = new ArrayList<>(4);
                }
                overlay.addOverlays(quad, context, out);
            }
        }
        return out;
    }

    /**
     * The render layers a block is drawn in only for its overlays and glowing textures, as bits by layer ordinal; the
     * chunk builder adds these passes.
     */
    public static int extraLayers(IBlockState state) {
        int layers = EmissiveTextures.needsCutoutPass(state) ? 1 << BlockRenderLayer.CUTOUT_MIPPED.ordinal() : 0;
        if (index.hasOverlays() && DevilicaOptions.get().connectedTextures) {
            layers |= OVERLAY_LAYERS.computeIfAbsent(state, ConnectedTextures::overlayLayers);
        }
        return layers;
    }

    private static int overlayLayers(IBlockState state) {
        final Index current = index;
        final Block block = state.getBlock();
        int layers = 0;
        final TileOverride[] forBlock = current.byBlock.get(block);
        if (forBlock != null) {
            final int meta = block.getMetaFromState(state);
            for (TileOverride rule : forBlock) {
                if (rule instanceof Overlay overlay && overlay.matchesBlock(block, meta)) {
                    layers |= 1 << overlay.layer.ordinal();
                }
            }
        }
        for (int face = 0; face < 6; face++) {
            final TextureAtlasSprite sprite = spriteOf(state, face);
            final TileOverride[] forSprite = sprite == null ? null : current.bySprite.get(sprite);
            if (forSprite != null) {
                for (TileOverride rule : forSprite) {
                    if (rule instanceof Overlay overlay) {
                        layers |= 1 << overlay.layer.ordinal();
                    }
                }
            }
        }
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            if (block.canRenderInLayer(state, layer)) {
                layers &= ~(1 << layer.ordinal());
            }
        }
        return layers;
    }

    private static List<BakedQuad> retexture(List<BakedQuad> quads, CtmModel model) {
        if (quads.isEmpty() || (!model.ctm && !model.natural && model.betterGrass == BetterGrassMode.OFF)) {
            return quads;
        }
        final TileOverride[] forBlock = model.ctm ? model.index.byBlock.get(model.state().getBlock()) : null;
        List<BakedQuad> result = null;
        final int count = quads.size();
        for (int i = 0; i < count; i++) {
            final BakedQuad quad = quads.get(i);
            final Object processed = processQuad(quad, model, forBlock);
            if (result == null && processed != quad) {
                result = new ArrayList<>(count + 3);
                for (int j = 0; j < i; j++) {
                    result.add(quads.get(j));
                }
            }
            if (result != null) {
                if (processed instanceof BakedQuad single) {
                    result.add(single);
                } else if (processed instanceof List<?> several) {
                    for (Object part : several) {
                        result.add((BakedQuad) part);
                    }
                }
            }
        }
        return result == null ? quads : result;
    }

    /**
     * OptiFine's order: better grass, then connected textures, then natural textures for faces CTM left alone.
     *
     * @return the quad, its replacement, a list of quads, or null to leave it out
     */
    private static Object processQuad(BakedQuad quad, CtmModel model, TileOverride[] forBlock) {
        final EnumFacing face = quad.getFace();
        if (face == null) {
            return quad;
        }
        BakedQuad current = quad;
        if (model.betterGrass != BetterGrassMode.OFF && face.getAxis() != EnumFacing.Axis.Y) {
            current = BetterGrass.apply(current, model.world(), model.pos(), model.state(), face, model.betterGrass);
            if (current == null) {
                return null;
            }
        }
        final TextureAtlasSprite sprite = current.getSprite();
        if (model.ctm) {
            final TileOverride[] forSprite = model.index.bySprite.get(sprite);
            if (forBlock != null || forSprite != null) {
                final CtmContext context = model.context();
                context.setFace(face.getIndex(), sprite, Quads.orientation(current));
                final Object connected = apply(current, context, forBlock, forSprite);
                if (connected != current) {
                    return connected;
                }
            }
        }
        if (model.natural) {
            final NaturalTextures.Natural natural = model.index.natural.get(sprite);
            if (natural != null) {
                current = NaturalTextures.apply(current, natural, model.pos(), face.getIndex());
            }
        }
        return current;
    }

    /** The first rule that applies, block rules before texture rules: a quad, or a list of them */
    private static Object apply(BakedQuad quad, CtmContext context, TileOverride[] forBlock, TileOverride[] forSprite) {
        if (forBlock != null) {
            for (TileOverride override : forBlock) {
                final Object rendered = override.render(quad, context);
                if (rendered != null) {
                    return rendered;
                }
            }
        }
        if (forSprite != null) {
            for (TileOverride override : forSprite) {
                final Object rendered = override.render(quad, context);
                if (rendered != null) {
                    return rendered;
                }
            }
        }
        return quad;
    }

    /** The texture a block state shows on a face, for connecting by texture; null when it shows none there. */
    static TextureAtlasSprite spriteOf(IBlockState state, int face) {
        TextureAtlasSprite[] sprites = FACE_SPRITES.get(state);
        if (sprites == null) {
            sprites = faceSprites(state);
            FACE_SPRITES.put(state, sprites);
        }
        return sprites[face];
    }

    private static TextureAtlasSprite[] faceSprites(IBlockState state) {
        try {
            final IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
            final TextureAtlasSprite[] sprites = new TextureAtlasSprite[6];
            for (EnumFacing facing : EnumFacing.VALUES) {
                final List<BakedQuad> quads = model.getQuads(state, facing, 0L);
                if (!quads.isEmpty()) {
                    sprites[facing.getIndex()] = quads.get(0).getSprite();
                }
            }
            for (BakedQuad quad : model.getQuads(state, null, 0L)) {
                final EnumFacing facing = quad.getFace();
                if (facing != null && sprites[facing.getIndex()] == null) {
                    sprites[facing.getIndex()] = quad.getSprite();
                }
            }
            return sprites;
        } catch (RuntimeException e) {
            return NO_SPRITES;
        }
    }
}
