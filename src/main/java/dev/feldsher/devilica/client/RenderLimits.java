/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.client;

import dev.feldsher.devilica.config.DevilicaOptions;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The render settings' limits: things switched off, drawn only nearer, or not drawn from behind. */
public final class RenderLimits {
    private static final Exemptions ENTITIES = new Exemptions();
    private static final Exemptions BLOCK_ENTITIES = new Exemptions();

    private RenderLimits() {
    }

    /** Whether the entity is not drawn at all, seen from the camera at the given position */
    public static boolean hidden(Entity entity, double camX, double camY, double camZ) {
        final DevilicaOptions.RenderSettings render = DevilicaOptions.get().render;
        if (!render.armorStands && entity instanceof EntityArmorStand
                || !render.paintings && entity instanceof EntityPainting
                || !render.itemFrames && entity instanceof EntityItemFrame) {
            return true;
        }
        final int distance = render.entityDistance;
        return distance > 0 && entity.getDistanceSq(camX, camY, camZ) > (double) distance * distance
                && !ENTITIES.exempt(entity.getClass(), render.entityDistanceExempt);
    }

    /** The block entity's own draw distance, squared, made shorter by the render settings */
    public static double blockEntityDistanceSq(TileEntity tile, double ownSq) {
        final DevilicaOptions.RenderSettings render = DevilicaOptions.get().render;
        final int distance = render.blockEntityDistance;
        if (distance <= 0 || BLOCK_ENTITIES.exempt(tile.getClass(), render.blockEntityDistanceExempt)) {
            return ownSq;
        }
        return Math.min(ownSq, (double) distance * distance);
    }

    /**
     * Whether a sign's text faces away from the camera; {@code x, y, z} is the sign's block corner relative to the
     * camera, as block entity renderers get it.
     */
    public static boolean signTextHidden(TileEntitySign sign, double x, double y, double z) {
        if (!DevilicaOptions.get().render.cullFromBehind || !sign.hasWorld()) {
            return false;
        }
        final double nx;
        final double nz;
        final int meta = sign.getBlockMetadata();
        if (sign.getBlockType() instanceof BlockStandingSign) {
            final double angle = Math.toRadians(meta * 22.5);
            nx = -Math.sin(angle);
            nz = Math.cos(angle);
        } else if (sign.getBlockType() instanceof BlockWallSign) {
            final EnumFacing facing = EnumFacing.byIndex(meta);
            nx = facing.getXOffset();
            nz = facing.getZOffset();
        } else {
            return false;
        }
        // The camera is at minus the sign's position: behind when the sign's centre lies ahead along its front
        return (x + 0.5) * nx + (z + 0.5) * nz > 0.0;
    }

    /** Whether a framed map faces away from the camera at the given position */
    public static boolean mapHidden(EntityItemFrame frame, double camX, double camY, double camZ) {
        if (!DevilicaOptions.get().render.cullFromBehind || frame.facingDirection == null) {
            return false;
        }
        final EnumFacing facing = frame.facingDirection;
        return (camX - frame.posX) * facing.getXOffset() + (camZ - frame.posZ) * facing.getZOffset() < 0.0;
    }

    /** Whether the item in a frame is too far to draw */
    public static boolean frameItemHidden(EntityItemFrame frame, double camX, double camY, double camZ) {
        final int distance = DevilicaOptions.get().render.itemFrameItemDistance;
        return distance > 0 && frame.getDistanceSq(camX, camY, camZ) > (double) distance * distance;
    }

    // Class names and packages ending in ".*", matched once per class until the list changes
    private static final class Exemptions {
        private final Map<Class<?>, Boolean> cache = new ConcurrentHashMap<>();
        private List<String> list;

        boolean exempt(Class<?> type, List<String> names) {
            if (names != this.list) {
                this.cache.clear();
                this.list = names;
            }
            return this.cache.computeIfAbsent(type, t -> matches(t, names));
        }

        private static boolean matches(Class<?> type, List<String> names) {
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                final String name = c.getName();
                for (String entry : names) {
                    if (entry.endsWith(".*") ? name.startsWith(entry.substring(0, entry.length() - 1)) : name.equals(entry)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
