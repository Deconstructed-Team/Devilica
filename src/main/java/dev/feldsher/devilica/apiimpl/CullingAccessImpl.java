/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.apiimpl;

import dev.feldsher.devilica.api.render.CullingAccess;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

/** {@link CullingAccess} over the terrain renderer's visible sections. */
public final class CullingAccessImpl implements CullingAccess {

    @Override
    public boolean isBoxVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        final CeleritasWorldRenderer renderer = CeleritasWorldRenderer.instanceNullable();
        return renderer == null || renderer.isBoxVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
