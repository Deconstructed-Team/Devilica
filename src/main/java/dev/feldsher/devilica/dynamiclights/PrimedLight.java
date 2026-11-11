/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.dynamiclights;

/** Something about to explode glows brighter as its fuse runs down (a lit TNT, a swelling creeper). */
public interface PrimedLight {

    int devilica$getPrimedLuminance();
}
