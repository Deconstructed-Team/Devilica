/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.shaders;

import com.gtnewhorizons.angelica.glsm.RenderSystem;
import dev.feldsher.devilica.Devilica;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.coderbot.iris.gl.texture.TextureType;
import net.coderbot.iris.helpers.Tri;
import net.coderbot.iris.pipeline.transform.PatchShaderType;
import net.coderbot.iris.pipeline.transform.parameter.AttributeParameters;
import net.coderbot.iris.pipeline.transform.parameter.Parameters;
import net.coderbot.iris.shaderpack.texture.TextureStage;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Transformed shader sources, kept on disk between runs. Rewriting a pack's programs for this renderer is most of
 * what loading a pack takes (the driver keeps its own cache of compiled programs), and the result depends only on
 * the source, the transform's parameters, this build and the GL the game runs on. A file is named by the hash of all
 * of that, so a changed pack option or an updated pack simply misses.
 */
public final class TransformDiskCache {

    private static final int FORMAT = 1;
    private static final long MAX_BYTES = 128L << 20;
    private static final boolean ENABLED = !"false".equals(System.getProperty("devilica.shaderDiskCache"));

    private static final AtomicInteger HITS = new AtomicInteger();
    private static final AtomicInteger MISSES = new AtomicInteger();

    /** This build and GL; null until the renderer is up, and nothing is read or written before that */
    private static volatile String environment;
    private static Path directory;

    private TransformDiskCache() {
    }

    /** On the render thread, once GL can be asked. */
    public static void init() {
        if (!ENABLED) {
            return;
        }
        try {
            directory = Minecraft.getMinecraft().gameDir.toPath().resolve("cache").resolve("devilica").resolve("shaders");
            Files.createDirectories(directory);
        } catch (IOException | RuntimeException e) {
            Devilica.LOGGER.warn("Shader cache: no directory, shaders are transformed on every load: {}", e.toString());
            return;
        }
        environment = FORMAT + "|" + build() + "|" + GL11.glGetString(GL11.GL_RENDERER) + "|" + GL11.glGetString(GL11.GL_VERSION)
            + "|" + RenderSystem.getMaxGlslVersion() + "|" + RenderSystem.supportsGpuShader4();
        final Thread pruner = new Thread(TransformDiskCache::prune, "Devilica shader cache");
        pruner.setDaemon(true);
        pruner.setPriority(Thread.MIN_PRIORITY);
        pruner.start();
    }

    /** The mod's version, and its jar's size and date: a development build keeps its version while its code changes */
    private static String build() {
        final StringBuilder build = new StringBuilder(Devilica.VERSION);
        try {
            final URL location = TransformDiskCache.class.getProtectionDomain().getCodeSource().getLocation();
            String path = location.toURI().getSchemeSpecificPart();
            final int inJar = path.indexOf("!/");
            if (inJar >= 0) {
                path = path.substring(0, inJar);
            }
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
            }
            final Path jar = Paths.get(path.matches("^/[A-Za-z]:.*") ? path.substring(1) : path);
            if (Files.isRegularFile(jar)) {
                build.append('|').append(Files.size(jar)).append('|').append(Files.getLastModifiedTime(jar).toMillis());
            }
        } catch (Exception e) {
            // the version alone
        }
        return build.toString();
    }

    public static Map<PatchShaderType, String> load(Parameters parameters, String vertex, String geometry, String tessControl,
                                                    String tessEval, String fragment) {
        final Path file = file(parameters, vertex, geometry, tessControl, tessEval, fragment);
        if (file == null) {
            return null;
        }
        if (!Files.isRegularFile(file)) {
            MISSES.incrementAndGet();
            return null;
        }
        final Map<PatchShaderType, String> result = new EnumMap<>(PatchShaderType.class);
        try (InputStream raw = Files.newInputStream(file); DataInputStream in = new DataInputStream(new GZIPInputStream(raw, 1 << 16))) {
            if (in.readInt() != FORMAT) {
                throw new IOException("format");
            }
            for (int i = 0, count = in.readInt(); i < count; i++) {
                final PatchShaderType type = PatchShaderType.valueOf(in.readUTF());
                final byte[] source = new byte[in.readInt()];
                in.readFully(source);
                result.put(type, new String(source, StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException e) {
            // A file cut short by a crash or from another format: transformed again and written over
            MISSES.incrementAndGet();
            return null;
        }
        try {
            // What was used last is what the pruning keeps
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException e) {
            // read-only is fine
        }
        HITS.incrementAndGet();
        return result;
    }

    public static void store(Parameters parameters, String vertex, String geometry, String tessControl, String tessEval,
                             String fragment, Map<PatchShaderType, String> result) {
        final Path file = file(parameters, vertex, geometry, tessControl, tessEval, fragment);
        if (file == null || result == null) {
            return;
        }
        final Path temporary = file.resolveSibling(file.getFileName() + "." + Thread.currentThread().threadId() + ".tmp");
        try {
            try (OutputStream raw = Files.newOutputStream(temporary); DataOutputStream out = new DataOutputStream(new GZIPOutputStream(raw, 1 << 16))) {
                int count = 0;
                for (String source : result.values()) {
                    if (source != null) {
                        count++;
                    }
                }
                out.writeInt(FORMAT);
                out.writeInt(count);
                for (Map.Entry<PatchShaderType, String> entry : result.entrySet()) {
                    if (entry.getValue() == null) {
                        continue;
                    }
                    final byte[] source = entry.getValue().getBytes(StandardCharsets.UTF_8);
                    out.writeUTF(entry.getKey().name());
                    out.writeInt(source.length);
                    out.write(source);
                }
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // left for the pruning
            }
        }
    }

    /** How the last load went, for the log; resets the count */
    public static String report() {
        final int hits = HITS.getAndSet(0);
        final int misses = MISSES.getAndSet(0);
        return environment == null ? "off" : hits + " from disk, " + misses + " transformed";
    }

    private static Path file(Parameters parameters, String vertex, String geometry, String tessControl, String tessEval, String fragment) {
        final String environment = TransformDiskCache.environment;
        if (environment == null) {
            return null;
        }
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        add(digest, environment);
        add(digest, describe(parameters));
        add(digest, vertex);
        add(digest, geometry);
        add(digest, tessControl);
        add(digest, tessEval);
        add(digest, fragment);
        final byte[] hash = digest.digest();
        final StringBuilder name = new StringBuilder(40);
        for (int i = 0; i < 16; i++) {
            name.append(Character.forDigit(hash[i] >> 4 & 15, 16)).append(Character.forDigit(hash[i] & 15, 16));
        }
        return directory.resolve(name.append(".gz").toString());
    }

    private static void add(MessageDigest digest, String part) {
        if (part == null) {
            digest.update((byte) 0);
            return;
        }
        final byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) 1);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    /** Everything about the parameters that a transform reads, in a fixed order */
    private static String describe(Parameters parameters) {
        final StringBuilder text = new StringBuilder(parameters.getClass().getName())
            .append('|').append(parameters.patch).append('|').append(parameters.getTextureStage());
        if (parameters instanceof AttributeParameters attributes) {
            text.append('|').append(attributes.hasGeometry);
            if (attributes.inputs != null) {
                text.append('|').append(attributes.inputs.texture).append('|').append(attributes.inputs.lightmap);
            }
        }
        final Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textures = parameters.getTextureMap();
        if (textures != null && !textures.isEmpty()) {
            final List<String> renames = new ArrayList<>(textures.size());
            textures.forEach((from, to) -> renames.add(from.first() + ":" + from.second() + ":" + from.third() + "=" + to));
            renames.sort(Comparator.naturalOrder());
            text.append('|').append(renames);
        }
        return text.toString();
    }

    /** Oldest first, down to three quarters of the limit; leftovers of interrupted writes go as well */
    private static void prune() {
        final List<Path> files = new ArrayList<>();
        try (Stream<Path> listing = Files.list(directory)) {
            listing.forEach(files::add);
        } catch (IOException | RuntimeException e) {
            return;
        }
        final long[] sizes = new long[files.size()];
        final long[] times = new long[files.size()];
        long total = 0;
        final List<Integer> order = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            final Path file = files.get(i);
            try {
                if (file.getFileName().toString().endsWith(".tmp")) {
                    Files.deleteIfExists(file);
                    continue;
                }
                sizes[i] = Files.size(file);
                times[i] = Files.getLastModifiedTime(file).toMillis();
                total += sizes[i];
                order.add(i);
            } catch (IOException e) {
                // gone meanwhile
            }
        }
        if (total <= MAX_BYTES) {
            return;
        }
        order.sort(Comparator.comparingLong(i -> times[i]));
        for (int i : order) {
            if (total <= MAX_BYTES / 4 * 3) {
                break;
            }
            try {
                Files.deleteIfExists(files.get(i));
                total -= sizes[i];
            } catch (IOException e) {
                // next start
            }
        }
    }
}
