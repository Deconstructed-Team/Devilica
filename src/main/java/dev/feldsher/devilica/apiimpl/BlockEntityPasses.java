/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.apiimpl;

import dev.feldsher.devilica.api.render.BlockEntityPassListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The block entity passes' listeners, called in the order they were added and ended in reverse. */
public final class BlockEntityPasses {
    private static final List<BlockEntityPassListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BlockEntityPassListener> SHADOW_LISTENERS = new CopyOnWriteArrayList<>();

    private BlockEntityPasses() {
    }

    public static void add(BlockEntityPassListener listener) {
        LISTENERS.add(listener);
    }

    public static void addShadow(BlockEntityPassListener listener) {
        SHADOW_LISTENERS.add(listener);
    }

    public static void begin() {
        begin(LISTENERS);
    }

    public static void end() {
        end(LISTENERS);
    }

    public static void beginShadow() {
        begin(SHADOW_LISTENERS);
    }

    public static void endShadow() {
        end(SHADOW_LISTENERS);
    }

    private static void begin(List<BlockEntityPassListener> listeners) {
        for (BlockEntityPassListener listener : listeners) {
            listener.begin();
        }
    }

    private static void end(List<BlockEntityPassListener> listeners) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).end();
        }
    }
}
