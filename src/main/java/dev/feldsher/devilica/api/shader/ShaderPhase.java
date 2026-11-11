/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.shader;

/** A part of the world a shader pack has programs of its own for. */
public enum ShaderPhase {
    /** {@code gbuffers_entities}: mobs, dropped items, things worn */
    ENTITIES,
    /** {@code gbuffers_block}: block entities (machines, chests, anything a TESR draws) */
    BLOCK_ENTITIES
}
