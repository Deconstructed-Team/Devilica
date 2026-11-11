/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api;

import dev.feldsher.devilica.api.light.ColoredLightAccess;
import dev.feldsher.devilica.api.light.LightAccess;
import dev.feldsher.devilica.api.render.BlockEntityPassListener;
import dev.feldsher.devilica.api.render.CullingAccess;
import dev.feldsher.devilica.api.shader.ShaderAccess;
import dev.feldsher.devilica.apiimpl.BlockEntityPasses;
import dev.feldsher.devilica.apiimpl.ColoredLightAccessImpl;
import dev.feldsher.devilica.apiimpl.CullingAccessImpl;
import dev.feldsher.devilica.apiimpl.LightAccessImpl;
import dev.feldsher.devilica.apiimpl.ShaderAccessImpl;

/**
 * Devilica's API for mods that draw things themselves: what the shader pack does, dynamic lights, what the camera can
 * see. Everything here is client-side and, unless a method says otherwise, for the client thread.
 * <p>
 * Check {@code Loader.isModLoaded("devilica")} before the first use and keep the calls in classes that load only then.
 * {@link #VERSION} goes up when something is added; nothing is removed or changed within a major version.
 */
public final class DevilicaApi {

    /** Major version times 1000 plus additions within it */
    public static final int VERSION = 1003;

    private static final ShaderAccess SHADERS = new ShaderAccessImpl();
    private static final LightAccess LIGHTS = new LightAccessImpl();
    private static final ColoredLightAccess COLORED_LIGHT = new ColoredLightAccessImpl();
    private static final CullingAccess CULLING = new CullingAccessImpl();

    private DevilicaApi() {
    }

    public static ShaderAccess shaders() {
        return SHADERS;
    }

    public static LightAccess lights() {
        return LIGHTS;
    }

    /** Coloured block light without a shader pack. Since 1003. */
    public static ColoredLightAccess coloredLight() {
        return COLORED_LIGHT;
    }

    public static CullingAccess culling() {
        return CULLING;
    }

    /** Calls the listener around every block entity pass of the camera from now on. Since 1001. */
    public static void addBlockEntityPassListener(BlockEntityPassListener listener) {
        BlockEntityPasses.add(listener);
    }
}
