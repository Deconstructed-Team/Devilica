/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.resources;

import dev.feldsher.devilica.Devilica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.function.Function;

/** An image outside {@code textures/} (a connected textures tile, an item texture of a resource pack) stitched into the block atlas. */
public final class FileSprite extends TextureAtlasSprite {

    private final ResourceLocation file;

    public FileSprite(String spriteName, ResourceLocation file) {
        super(spriteName);
        this.file = file;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    /** @return false to stitch the sprite, true to leave it out */
    @Override
    public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        try (IResource sizeResource = manager.getResource(this.file); IResource resource = manager.getResource(this.file)) {
            final PngSizeInfo size = PngSizeInfo.makeFromResource(sizeResource);
            this.loadSprite(size, resource.getMetadata("animation") != null);
            this.loadSpriteFrames(resource, Minecraft.getMinecraft().getTextureMapBlocks().getMipmapLevels() + 1);
            return false;
        } catch (IOException | RuntimeException e) {
            Devilica.LOGGER.warn("Could not load {} into the block atlas: {}", this.file, e.toString());
            return true;
        }
    }
}
