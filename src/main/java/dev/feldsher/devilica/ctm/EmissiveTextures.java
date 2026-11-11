/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

import dev.feldsher.devilica.Devilica;
import dev.feldsher.devilica.config.DevilicaOptions;
import dev.feldsher.devilica.resources.ResourcePackFiles;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OptiFine's emissive textures: with {@code optifine/emissive.properties} in a resource pack, a block texture
 * {@code x} that has an {@code x_e} beside it is drawn again with {@code x_e} at full light, so ores and machines glow
 * in the dark. The glowing copy carries its light in its vertices (the block vertex format), which both Celeritas's
 * block renderer and Forge's lighter take over the world's light. A solid block with such a texture is drawn once more
 * in the cutout pass, holding only its glowing copies, so their clear pixels stay clear. Items draw their glowing
 * copies at full light too, and entities their textures' {@code _e} images.
 */
public final class EmissiveTextures {

    /** Sky and block light at their brightest, as packed lightmap coordinates */
    private static final int FULL_LIGHT = 0x00F000F0;
    private static final String[] FILES = {"optifine/emissive.properties", "mcpatcher/emissive.properties"};

    private static String suffix;
    private static final List<String[]> REGISTERED = new ArrayList<>();
    private static volatile Map<TextureAtlasSprite, TextureAtlasSprite> emissive = Collections.emptyMap();
    private static final Map<IBlockState, Boolean> EXTRA_PASS = new ConcurrentHashMap<>();
    private static final Map<BakedQuad, BakedQuad> GLOWING = new ConcurrentHashMap<>();
    /** Textures outside the block atlas (entities and the like) and their glowing ones */
    private static volatile Map<ResourceLocation, ResourceLocation> textureGlow = Collections.emptyMap();

    private EmissiveTextures() {
    }

    /** At the stitch's start: the glowing textures of the pack join the atlas, or are noted for entities */
    static void register(IResourceManager resources, TextureMap map) {
        REGISTERED.clear();
        suffix = readSuffix(resources);
        if (suffix == null) {
            textureGlow = Collections.emptyMap();
            return;
        }
        final Map<ResourceLocation, ResourceLocation> textures = new HashMap<>();
        for (ResourceLocation file : ResourcePackFiles.find(suffix + ".png", "textures/")) {
            final String path = file.getPath();
            if (path.matches("textures/(entity|gui|models|environment|painting|particle|font|misc|map|colormap|effect)/.*")) {
                // Not block atlas textures: entities and the like draw them over their own
                textures.put(new ResourceLocation(file.getNamespace(), path.substring(0, path.length() - suffix.length() - ".png".length()) + ".png"), file);
                continue;
            }
            final String sprite = file.getNamespace() + ":" + path.substring("textures/".length(), path.length() - ".png".length());
            final String base = sprite.substring(0, sprite.length() - suffix.length());
            // Models register their textures after this event; whether the base is used shows at the stitch's end
            if (map.getTextureExtry(sprite) == null) {
                map.registerSprite(new ResourceLocation(sprite));
            }
            REGISTERED.add(new String[]{base, sprite});
        }
        textureGlow = textures.isEmpty() ? Collections.emptyMap() : textures;
    }

    /** The glowing texture an entity or the like draws over a texture, or null */
    public static ResourceLocation glowTexture(ResourceLocation texture) {
        final Map<ResourceLocation, ResourceLocation> current = textureGlow;
        return current.isEmpty() || texture == null || !DevilicaOptions.get().emissiveTextures ? null : current.get(texture);
    }

    /**
     * An item model with its glowing copies added, or the model itself when it shows no glowing texture. The copies
     * carry full light in their vertices, which Forge's item renderer draws at that light.
     */
    public static IBakedModel withItemGlow(IBakedModel model) {
        if (!active() || !showsGlowingTexture(model)) {
            return model;
        }
        return new GlowingItemModel(model);
    }

    private static boolean showsGlowingTexture(IBakedModel model) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (hasGlowingTexture(model.getQuads(null, facing, 0L))) {
                return true;
            }
        }
        return hasGlowingTexture(model.getQuads(null, null, 0L));
    }

    private static boolean hasGlowingTexture(List<BakedQuad> quads) {
        for (int i = 0; i < quads.size(); i++) {
            if (emissive.containsKey(quads.get(i).getSprite())) {
                return true;
            }
        }
        return false;
    }

    /** The quads followed by the glowing copies of those that have them */
    static List<BakedQuad> plusGlows(List<BakedQuad> quads) {
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            final BakedQuad glow = glowing(quads.get(i));
            if (glow != null) {
                if (result == null) {
                    result = new ArrayList<>(quads);
                }
                result.add(glow);
            }
        }
        return result == null ? quads : result;
    }

    private static String readSuffix(IResourceManager resources) {
        for (String file : FILES) {
            try (IResource resource = resources.getResource(new ResourceLocation(file));
                 Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                final Properties properties = new Properties();
                properties.load(reader);
                final String value = properties.getProperty("suffix.emissive");
                return value == null || value.trim().isEmpty() ? null : value.trim();
            } catch (IOException ignored) {
                // not in any pack
            }
        }
        return null;
    }

    /** At the stitch's end: each texture and its glowing one */
    static void resolve(TextureMap map) {
        EXTRA_PASS.clear();
        GLOWING.clear();
        final TextureAtlasSprite missing = map.getMissingSprite();
        final Map<TextureAtlasSprite, TextureAtlasSprite> result = new IdentityHashMap<>();
        for (String[] pair : REGISTERED) {
            final TextureAtlasSprite base = map.getAtlasSprite(pair[0]);
            final TextureAtlasSprite glowing = map.getAtlasSprite(pair[1]);
            if (base != missing && glowing != missing) {
                result.put(base, glowing);
            }
        }
        emissive = result.isEmpty() ? Collections.emptyMap() : result;
        if (!result.isEmpty()) {
            Devilica.LOGGER.info("Emissive textures: {} textures glow", result.size());
        }
    }

    static boolean active() {
        return !emissive.isEmpty() && DevilicaOptions.get().emissiveTextures;
    }

    /** The glowing copy of a quad, or null when its texture has none */
    private static BakedQuad glowing(BakedQuad quad) {
        final TextureAtlasSprite glow = emissive.get(quad.getSprite());
        if (glow == null) {
            return null;
        }
        final VertexFormat format = quad.getFormat();
        if (format != DefaultVertexFormats.ITEM && format != DefaultVertexFormats.BLOCK) {
            return null;
        }
        return GLOWING.computeIfAbsent(quad, q -> {
            final BakedQuad retextured = Quads.retexture(q, glow, q.getTintIndex());
            final int[] data = retextured.getVertexData().clone();
            // Both formats are seven ints a vertex; the block format's last is the lightmap
            for (int vertex = 0; vertex < data.length / 7; vertex++) {
                data[vertex * 7 + 6] = FULL_LIGHT;
            }
            return new BakedQuad(data, q.getTintIndex(), q.getFace(), glow, false, DefaultVertexFormats.BLOCK);
        });
    }

    /**
     * Adds the glowing copies of the quads that belong in the pass being drawn: none in the solid pass, which has no
     * clear pixels; a solid block's go in its extra cutout pass, any other block's in its own passes.
     *
     * @return the list added to, made when there was none
     */
    static List<BakedQuad> addGlows(List<BakedQuad> quads, IBlockState state, BlockRenderLayer layer, boolean ownPass, List<BakedQuad> out) {
        if (layer == BlockRenderLayer.SOLID || !(ownPass || layer == BlockRenderLayer.CUTOUT_MIPPED && needsCutoutPass(state))) {
            return out;
        }
        for (int i = 0; i < quads.size(); i++) {
            final BakedQuad glow = glowing(quads.get(i));
            if (glow != null) {
                if (out == null) {
                    out = new ArrayList<>(4);
                }
                out.add(glow);
            }
        }
        return out;
    }

    /** Whether a block drawn only in the solid pass shows a glowing texture, so it needs the extra cutout pass */
    public static boolean needsCutoutPass(IBlockState state) {
        if (!active()) {
            return false;
        }
        final Block block = state.getBlock();
        if (!block.canRenderInLayer(state, BlockRenderLayer.SOLID) || block.canRenderInLayer(state, BlockRenderLayer.CUTOUT_MIPPED)
            || block.canRenderInLayer(state, BlockRenderLayer.CUTOUT) || block.canRenderInLayer(state, BlockRenderLayer.TRANSLUCENT)) {
            return false;
        }
        return EXTRA_PASS.computeIfAbsent(state, EmissiveTextures::showsGlowingTexture);
    }

    private static boolean showsGlowingTexture(IBlockState state) {
        try {
            final IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
            for (EnumFacing facing : EnumFacing.VALUES) {
                for (BakedQuad quad : model.getQuads(state, facing, 0L)) {
                    if (emissive.containsKey(quad.getSprite())) {
                        return true;
                    }
                }
            }
            for (BakedQuad quad : model.getQuads(state, null, 0L)) {
                if (emissive.containsKey(quad.getSprite())) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            // a model that needs the world to give its quads
        }
        return false;
    }
}
