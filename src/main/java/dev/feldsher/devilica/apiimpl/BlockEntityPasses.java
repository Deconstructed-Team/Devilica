/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.apiimpl;

import dev.feldsher.devilica.api.render.BlockEntityPassListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The block entity pass's listeners, called in the order they were added and ended in reverse. */
public final class BlockEntityPasses {
    private static final List<BlockEntityPassListener> LISTENERS = new CopyOnWriteArrayList<>();

    private BlockEntityPasses() {
    }

    public static void add(BlockEntityPassListener listener) {
        LISTENERS.add(listener);
    }

    public static void begin() {
        for (BlockEntityPassListener listener : LISTENERS) {
            listener.begin();
        }
    }

    public static void end() {
        for (int i = LISTENERS.size() - 1; i >= 0; i--) {
            LISTENERS.get(i).end();
        }
    }
}
