/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.resources;

import dev.feldsher.devilica.Devilica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Lists files under a directory of the selected resource packs (and the server's), as OptiFine looks for its
 * properties files; the game's resource manager can only open files by name.
 */
public final class ResourcePackFiles {

    private ResourcePackFiles() {
    }

    /** @param directories paths below {@code assets/<namespace>/}, ending in a slash */
    public static Set<ResourceLocation> find(String suffix, String... directories) {
        final Set<ResourceLocation> found = new LinkedHashSet<>();
        final ResourcePackRepository repository = Minecraft.getMinecraft().getResourcePackRepository();
        for (ResourcePackRepository.Entry entry : repository.getRepositoryEntries()) {
            scan(entry.getResourcePack(), suffix, directories, found);
        }
        final IResourcePack server = repository.getServerResourcePack();
        if (server != null) {
            scan(server, suffix, directories, found);
        }
        return found;
    }

    private static void scan(IResourcePack pack, String suffix, String[] directories, Set<ResourceLocation> found) {
        if (pack instanceof LegacyV2Adapter adapter) {
            pack = adapter.pack;
        }
        if (!(pack instanceof AbstractResourcePack abstractPack)) {
            return;
        }
        final File file = abstractPack.resourcePackFile;
        try {
            if (file.isDirectory()) {
                final Path assets = file.toPath().resolve("assets");
                if (Files.isDirectory(assets)) {
                    try (Stream<Path> paths = Files.walk(assets)) {
                        paths.filter(Files::isRegularFile).forEach(path -> add(assets.relativize(path).toString().replace('\\', '/'), suffix, directories, found));
                    }
                }
            } else if (file.isFile()) {
                try (ZipFile zip = new ZipFile(file)) {
                    final Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        final ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory() && entry.getName().startsWith("assets/")) {
                            add(entry.getName().substring("assets/".length()), suffix, directories, found);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Devilica.LOGGER.warn("Could not list resource pack {}: {}", file, e.toString());
        }
    }

    /** @param path {@code <namespace>/<path>} */
    private static void add(String path, String suffix, String[] directories, Set<ResourceLocation> found) {
        final int slash = path.indexOf('/');
        if (slash <= 0 || !path.endsWith(suffix)) {
            return;
        }
        final String inside = path.substring(slash + 1);
        for (String directory : directories) {
            if (inside.startsWith(directory)) {
                found.add(new ResourceLocation(path.substring(0, slash).toLowerCase(Locale.ROOT), inside.toLowerCase(Locale.ROOT)));
                return;
            }
        }
    }
}
