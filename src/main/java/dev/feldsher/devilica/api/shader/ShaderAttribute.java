/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.api.shader;

/**
 * Inputs a shader pack reads besides position, colour, texture, light and normal. Set them per vertex with
 * {@code glVertexAttribPointer}, or once for a draw with {@code glVertexAttrib*} at
 * {@link ShaderAccess#attributeLocation}; Devilica sets neutral values before each block entity and entity.
 */
public enum ShaderAttribute {
    /** {@code mc_Entity}: (block ID, render type) as two shorts */
    ENTITY,
    /** {@code mc_midTexCoord}: the texture coordinate of the face's centre */
    MID_TEX_COORD,
    /** {@code at_tangent}: the face's tangent and handedness */
    TANGENT,
    /** {@code at_midBlock}: offset from the block's centre and light level */
    MID_BLOCK
}
