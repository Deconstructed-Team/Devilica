/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.render;

/**
 * Told when the world's block entities draw (the camera's pass, not the shadow pass), on the client thread. Everything
 * the block entity renderers draw happens between the two calls, Devilica's batched chests and signs last; a font mod
 * can hold the text drawn in between (sign text among it) and draw it all at {@link #end}.
 */
public interface BlockEntityPassListener {

    void begin();

    void end();
}
