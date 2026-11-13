/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.shaders;

import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;

/**
 * Draws with another mod's own programs while the world draws ({@code ShaderAccess.beginOwnPrograms}): the pipeline
 * neither binds its program on state changes nor writes its uniforms on texture binds until they end. Client thread.
 */
public final class ForeignPrograms {
    private static int depth;

    private ForeignPrograms() {
    }

    public static boolean active() {
        return depth > 0;
    }

    public static void begin() {
        if (depth++ == 0) {
            final WorldRenderingPipeline pipeline = Iris.enabled ? Iris.getPipelineManager().getPipelineNullable() : null;
            if (pipeline != null) {
                pipeline.beginForeignPrograms();
            }
        }
    }

    public static void end() {
        if (depth == 0 || --depth > 0) {
            return;
        }
        final WorldRenderingPipeline pipeline = Iris.enabled ? Iris.getPipelineManager().getPipelineNullable() : null;
        if (pipeline != null) {
            pipeline.endForeignPrograms();
        }
    }

    /** A frame starts: brackets a draw that threw past its end left open are dropped. */
    public static void reset() {
        depth = 0;
    }
}
