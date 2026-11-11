/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.cit;

import dev.feldsher.devilica.Devilica;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The json models of custom item textures, as {@code devilica:models/cit/<n>.json}, with their texture paths made
 * atlas names, so Forge's own model loader loads them with their parents and display transforms.
 */
final class CitModelPack implements IResourcePack {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    void clear() {
        this.files.clear();
    }

    /** Adds a model and gives the location to load it by */
    ResourceLocation add(byte[] json) {
        final String name = "cit/" + this.files.size();
        this.files.put("models/" + name + ".json", json);
        return new ResourceLocation(Devilica.MOD_ID, name);
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws FileNotFoundException {
        final byte[] json = Devilica.MOD_ID.equals(location.getNamespace()) ? this.files.get(location.getPath()) : null;
        if (json == null) {
            throw new FileNotFoundException(location.toString());
        }
        return new ByteArrayInputStream(json);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return Devilica.MOD_ID.equals(location.getNamespace()) && this.files.containsKey(location.getPath());
    }

    @Override
    public Set<String> getResourceDomains() {
        return Set.of(Devilica.MOD_ID);
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws FileNotFoundException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return "Devilica item models";
    }
}
