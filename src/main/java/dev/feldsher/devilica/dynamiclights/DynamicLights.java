/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.dynamiclights;

import dev.feldsher.devilica.api.light.DynamicLightItem;
import dev.feldsher.devilica.config.DevilicaOptions;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.projectile.EntitySpectralArrow;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Light that moves with entities: a held torch, a dropped glowstone, a burning mob (LambDynamicLights' model, as
 * Angelica has it). The terrain around a light is relit by rebuilding its chunk sections; entities and block entities
 * near it are lit through the client world's light lookup.
 * <p>
 * Everything here runs on the client thread. Chunk builds on worker threads get a snapshot of the lights near their
 * section ({@link #sourcesNear}) when their task is made.
 */
public final class DynamicLights {

    /** How far a light reaches, a little under a chunk section so it touches at most two along each axis */
    public static final double MAX_RADIUS = 7.75;
    private static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
    /** From a section's centre to its corner (8 * sqrt 3) plus the light radius */
    private static final double SECTION_REACH_SQUARED = (13.86 + MAX_RADIUS) * (13.86 + MAX_RADIUS);
    /** A source relights the terrain once it moved this far */
    private static final double MOVE_THRESHOLD = 0.1;

    public static final float[] NO_SOURCES = new float[0];

    private static final Set<DynamicLightSource> SOURCES = new ReferenceOpenHashSet<>();
    /** x, y, z and luminance of every light, as of the last update */
    private static float[] snapshot = NO_SOURCES;
    private static long lastUpdate;
    private static boolean wasEnabled;
    /** Entity classes newly listed in the settings, not yet written to the file */
    private static boolean listed;
    /** The settings' item light table, by item, and the table it was made from */
    private static final Reference2IntOpenHashMap<Item> ITEM_LIGHTS = new Reference2IntOpenHashMap<>();
    private static Map<String, Integer> itemLightsFrom;

    private DynamicLights() {
    }

    public static boolean isEnabled() {
        final DevilicaOptions.DynamicLightsSettings settings = DevilicaOptions.get().dynamicLights;
        return settings.mode != DynamicLightsMode.OFF && (settings.withShaders || !IrisApi.getInstance().isShaderPackInUse());
    }

    /** Each client tick of an entity: how bright it is now. */
    public static void tick(Entity entity) {
        final DynamicLightSource source = (DynamicLightSource) entity;
        LightTracker tracker = source.devilica$getLightTracker();
        if (!wasEnabled) {
            if (tracker != null) {
                tracker.luminance = 0;
            }
            return;
        }
        if (tracker == null && (entity instanceof EntityItem || entity instanceof EntityItemFrame)) {
            // Dropped and framed items keep their stack's light between lookups
            tracker = source.devilica$getOrCreateLightTracker();
        }
        final int luminance = entity.isDead ? 0 : luminance(entity, tracker);
        if (luminance > 0) {
            if (tracker == null) {
                tracker = source.devilica$getOrCreateLightTracker();
            }
            SOURCES.add(source);
        }
        if (tracker != null) {
            tracker.luminance = luminance;
        }
    }

    /** The entity left the world: the terrain it lit loses that light. */
    public static void remove(Entity entity) {
        final DynamicLightSource source = (DynamicLightSource) entity;
        if (SOURCES.remove(source)) {
            final LightTracker tracker = source.devilica$getLightTracker();
            tracker.luminance = 0;
            scheduleRebuild(tracker.litSections);
            reset(tracker);
        }
    }

    /** The client world is gone, and its renderer with it. */
    public static void clear() {
        for (DynamicLightSource source : SOURCES) {
            reset(source.devilica$getLightTracker());
        }
        SOURCES.clear();
        snapshot = NO_SOURCES;
    }

    /** Once a frame, before the terrain is set up: relights the terrain around lights that moved or changed. */
    public static void update() {
        final boolean enabled = isEnabled();
        if (!enabled) {
            if (wasEnabled) {
                wasEnabled = false;
                for (DynamicLightSource source : SOURCES) {
                    final LightTracker tracker = source.devilica$getLightTracker();
                    scheduleRebuild(tracker.litSections);
                    reset(tracker);
                }
                SOURCES.clear();
                snapshot = NO_SOURCES;
            }
            return;
        }
        wasEnabled = true;

        final long now = System.currentTimeMillis();
        if (now - lastUpdate < DevilicaOptions.get().dynamicLights.mode.delay) {
            return;
        }
        lastUpdate = now;

        float[] lights = new float[SOURCES.size() * 4];
        int count = 0;
        final Iterator<DynamicLightSource> iterator = SOURCES.iterator();
        while (iterator.hasNext()) {
            final DynamicLightSource source = iterator.next();
            final Entity entity = (Entity) source;
            final LightTracker tracker = source.devilica$getLightTracker();
            final double x = entity.posX;
            final double y = entity.posY + entity.getEyeHeight();
            final double z = entity.posZ;
            final int luminance = tracker.luminance;

            if (luminance != tracker.litLuminance || Double.isNaN(tracker.litX) || Math.abs(x - tracker.litX) > MOVE_THRESHOLD
                    || Math.abs(y - tracker.litY) > MOVE_THRESHOLD || Math.abs(z - tracker.litZ) > MOVE_THRESHOLD) {
                relight(tracker, x, y, z, luminance);
            }

            if (luminance > 0) {
                lights[count++] = (float) x;
                lights[count++] = (float) y;
                lights[count++] = (float) z;
                lights[count++] = luminance;
            } else if (tracker.litSections.isEmpty()) {
                iterator.remove();
            }
        }

        snapshot = count == lights.length ? lights : Arrays.copyOf(lights, count);
    }

    /** The sections the light reaches now and those it reached before are rebuilt. */
    private static void relight(LightTracker tracker, double x, double y, double z, int luminance) {
        final LongOpenHashSet sections = tracker.spareSections;
        sections.clear();
        if (luminance > 0) {
            // A face takes its light from the block in front of it, which can be one block past the lit sphere
            final double reach = MAX_RADIUS + 1.0;
            final int minX = MathHelper.floor(x - reach) >> 4;
            final int minY = MathHelper.floor(y - reach) >> 4;
            final int minZ = MathHelper.floor(z - reach) >> 4;
            final int maxX = MathHelper.floor(x + reach) >> 4;
            final int maxY = MathHelper.floor(y + reach) >> 4;
            final int maxZ = MathHelper.floor(z + reach) >> 4;
            for (int sx = minX; sx <= maxX; sx++) {
                for (int sy = minY; sy <= maxY; sy++) {
                    for (int sz = minZ; sz <= maxZ; sz++) {
                        sections.add(packSection(sx, sy, sz));
                    }
                }
            }
        }
        scheduleRebuild(sections);
        scheduleRebuild(tracker.litSections);

        tracker.spareSections = tracker.litSections;
        tracker.litSections = sections;
        tracker.litX = x;
        tracker.litY = y;
        tracker.litZ = z;
        tracker.litLuminance = luminance;
    }

    private static void reset(LightTracker tracker) {
        tracker.litSections.clear();
        tracker.litX = Double.NaN;
        tracker.litLuminance = 0;
    }

    private static void scheduleRebuild(LongOpenHashSet sections) {
        final CeleritasWorldRenderer renderer = CeleritasWorldRenderer.instanceNullable();
        if (renderer == null || sections.isEmpty()) {
            return;
        }
        final LongIterator iterator = sections.iterator();
        while (iterator.hasNext()) {
            final long section = iterator.nextLong();
            renderer.scheduleRebuildForChunk(unpackX(section), unpackY(section), unpackZ(section), false);
        }
    }

    /** The lights that can reach a chunk section, for a build of that section on a worker thread. */
    public static float[] sourcesNear(int minX, int minY, int minZ) {
        final float[] lights = snapshot;
        if (lights.length == 0) {
            return NO_SOURCES;
        }
        final double centerX = minX + 8.0;
        final double centerY = minY + 8.0;
        final double centerZ = minZ + 8.0;
        float[] near = null;
        int count = 0;
        for (int i = 0; i < lights.length; i += 4) {
            final double dx = lights[i] - centerX;
            final double dy = lights[i + 1] - centerY;
            final double dz = lights[i + 2] - centerZ;
            if (dx * dx + dy * dy + dz * dz <= SECTION_REACH_SQUARED) {
                if (near == null) {
                    near = new float[lights.length - i];
                }
                System.arraycopy(lights, i, near, count, 4);
                count += 4;
            }
        }
        return near == null ? NO_SOURCES : count == near.length ? near : Arrays.copyOf(near, count);
    }

    /** The light level the lights give at a point, 0 to 15 with fractions. */
    public static double lightAt(float[] lights, double x, double y, double z) {
        double level = 0.0;
        for (int i = 0; i < lights.length; i += 4) {
            final double dx = x - lights[i];
            final double dy = y - lights[i + 1];
            final double dz = z - lights[i + 2];
            final double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared <= MAX_RADIUS_SQUARED) {
                final double light = (1.0 - Math.sqrt(distanceSquared) / MAX_RADIUS) * lights[i + 3];
                if (light > level) {
                    level = light;
                }
            }
        }
        return Math.min(level, 15.0);
    }

    /** Packed sky and block light (as {@code getCombinedLight} returns it) with the dynamic light at the block's centre. */
    public static int withDynamicLight(float[] lights, int x, int y, int z, int packedLight) {
        if (lights.length == 0) {
            return packedLight;
        }
        final double level = lightAt(lights, x + 0.5, y + 0.5, z + 0.5);
        if (level > ((packedLight & 0xFFFF) >> 4)) {
            // Block light in sixteenths, so the light fades smoothly instead of in whole levels
            return (packedLight & 0xFFF00000) | ((int) (level * 16.0) & 0xFFFFF);
        }
        return packedLight;
    }

    /** The lights of this moment, for lighting entities and block entities on the client thread */
    public static float[] current() {
        return wasEnabled ? snapshot : NO_SOURCES;
    }

    /** How many things give off light now, for the F3 lines */
    public static int lightCount() {
        return current().length / 4;
    }

    private static int luminance(Entity entity, LightTracker tracker) {
        final DevilicaOptions.DynamicLightsSettings settings = DevilicaOptions.get().dynamicLights;
        if (entity == Minecraft.getMinecraft().player ? !settings.self : !settings.entities) {
            return 0;
        }
        // Light that stays under water, and light that goes out there (a torch, a blaze)
        int steady = entity.isBurning() ? 15 : 0;
        int doused = 0;
        if (entity instanceof EntityItem || entity instanceof EntityItemFrame) {
            if (entity.ticksExisted >= tracker.itemRecheckTick) {
                final ItemStack stack = entity instanceof EntityItem item ? item.getItem() : ((EntityItemFrame) entity).getDisplayedItem();
                tracker.itemLuminance = luminance(stack);
                tracker.itemGoesOut = goesOutInWater(stack);
                tracker.itemRecheckTick = entity.ticksExisted + 4;
            }
            if (tracker.itemGoesOut) {
                doused = tracker.itemLuminance;
            } else {
                steady = Math.max(steady, tracker.itemLuminance);
            }
        } else if (entity instanceof EntityLivingBase living) {
            for (ItemStack stack : living.getEquipmentAndArmor()) {
                final int light = luminance(stack);
                if (light > 0 && goesOutInWater(stack)) {
                    doused = Math.max(doused, light);
                } else {
                    steady = Math.max(steady, light);
                }
            }
            if (living instanceof EntityBlaze) {
                doused = Math.max(doused, 10);
            } else if (living instanceof EntityMagmaCube cube) {
                steady = Math.max(steady, cube.squishFactor > 0.6F ? 11 : 8);
            } else if (living instanceof EntityEnderman enderman) {
                final IBlockState held = enderman.getHeldBlockState();
                if (held != null) {
                    steady = Math.max(steady, held.getLightValue());
                }
            }
        } else if (entity instanceof EntitySpectralArrow) {
            steady = Math.max(steady, 8);
        }
        if (entity instanceof PrimedLight primed) {
            final int light = primed.devilica$getPrimedLuminance();
            if (entity instanceof EntityCreeper) {
                doused = Math.max(doused, light);
            } else {
                steady = Math.max(steady, light);
            }
        }

        int luminance = steady;
        if (doused > steady && !(settings.waterSensitive && entity.isInsideOfMaterial(Material.WATER))) {
            luminance = doused;
        }
        return luminance > 0 && lightsByClass(entity, settings) ? luminance : 0;
    }

    /** Whether this kind of entity may give off light; a kind seen glowing the first time is listed, and may */
    private static boolean lightsByClass(Entity entity, DevilicaOptions.DynamicLightsSettings settings) {
        final String name = entity.getClass().getName();
        final Boolean lights = settings.entityClasses.get(name);
        if (lights == null) {
            settings.entityClasses.put(name, true);
            listed = true;
            return true;
        }
        return lights;
    }

    /** Writes the entity classes found since the last time to the settings file, so the settings list them next time too */
    public static void saveListed() {
        if (listed) {
            listed = false;
            DevilicaOptions.get().save();
        }
    }

    public static int luminance(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        final Item item = stack.getItem();
        if (item instanceof DynamicLightItem lightItem) {
            return lightItem.getLuminance(stack);
        }
        final int tabled = itemLight(item);
        if (tabled > 0) {
            return tabled;
        }
        if (item instanceof ItemBlock itemBlock) {
            final Block block = itemBlock.getBlock();
            try {
                return block.getStateFromMeta(itemBlock.getMetadata(stack.getMetadata())).getLightValue();
            } catch (RuntimeException e) {
                return block.getDefaultState().getLightValue();
            }
        }
        return 0;
    }

    /** Whether the item's light goes out under water: torches, fire, lava */
    public static boolean goesOutInWater(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final Item item = stack.getItem();
        if (item instanceof DynamicLightItem lightItem) {
            return lightItem.goesOutInWater(stack);
        }
        if (item instanceof ItemBlock itemBlock) {
            final Block block = itemBlock.getBlock();
            return block instanceof BlockTorch || block instanceof BlockFire || block.getDefaultState().getMaterial() == Material.LAVA;
        }
        return item == Items.LAVA_BUCKET || item == Items.FIRE_CHARGE;
    }

    /** The light level the settings give the item, 0 when they do not list it */
    private static int itemLight(Item item) {
        final Map<String, Integer> table = DevilicaOptions.get().dynamicLights.itemLights;
        if (table != itemLightsFrom) {
            ITEM_LIGHTS.clear();
            for (Map.Entry<String, Integer> entry : table.entrySet()) {
                final Item named = Item.getByNameOrId(entry.getKey());
                if (named != null && entry.getValue() != null) {
                    ITEM_LIGHTS.put(named, Math.clamp(entry.getValue(), 0, 15));
                }
            }
            itemLightsFrom = table;
        }
        return ITEM_LIGHTS.getInt(item);
    }

    private static long packSection(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 42);
    }

    private static int unpackY(long packed) {
        return (int) (packed << 22 >> 44);
    }

    private static int unpackZ(long packed) {
        return (int) (packed << 42 >> 42);
    }
}
