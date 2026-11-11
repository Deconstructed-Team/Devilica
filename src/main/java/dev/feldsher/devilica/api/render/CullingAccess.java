/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.render;

/** What the camera can see this frame, as the terrain renderer worked it out. */
public interface CullingAccess {

    /**
     * Whether any part of a world box can be visible this frame: inside the view and in a chunk section the camera can
     * see past the terrain. True when Devilica cannot tell (no world, between frames of a reload).
     */
    boolean isBoxVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
