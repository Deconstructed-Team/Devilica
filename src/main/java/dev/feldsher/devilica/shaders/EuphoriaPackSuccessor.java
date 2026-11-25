/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.shaders;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Euphoria patcher writes its patched Complementary as "ComplementaryReimagined_r5.10 + EuphoriaPatches_1.11.2"
 * and, when it is updated, renames the previous one to "Outdated ... + EP_..." without moving Devilica's choice: the
 * pack the config names is gone and shaders are off until the player picks the new one by hand. When the chosen pack
 * is missing and is such a patched pack, its newest successor of the same base is taken instead.
 */
public final class EuphoriaPackSuccessor {
    private static final Pattern PATCHED = Pattern.compile("^(?:Outdated )?(.+)_r([0-9][0-9A-Za-z.]*) \\+ (?:EuphoriaPatches|EP)_([0-9][0-9A-Za-z.]*)$");

    private EuphoriaPackSuccessor() {
    }

    /** The newest current patched pack of the same base as {@code missing}, among {@code packs}. */
    public static Optional<String> of(String missing, Collection<String> packs) {
        final Matcher wanted = PATCHED.matcher(missing);
        if (!wanted.matches()) {
            return Optional.empty();
        }
        final String base = wanted.group(1);

        String best = null;
        String bestShader = null;
        String bestPatch = null;
        for (final String pack : packs) {
            if (pack.startsWith("Outdated ") || !pack.contains(" + EuphoriaPatches_")) {
                continue;
            }
            final Matcher m = PATCHED.matcher(pack);
            if (!m.matches() || !m.group(1).equals(base)) {
                continue;
            }
            if (best == null || compareVersions(m.group(2), bestShader) > 0
                || compareVersions(m.group(2), bestShader) == 0 && compareVersions(m.group(3), bestPatch) > 0) {
                best = pack;
                bestShader = m.group(2);
                bestPatch = m.group(3);
            }
        }
        return Optional.ofNullable(best);
    }

    /** Numeric parts compared as numbers, the rest as text: 5.10 is newer than 5.9.3. */
    static int compareVersions(String a, String b) {
        final String[] pa = a.split("\\.");
        final String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            final String x = i < pa.length ? pa[i] : "0";
            final String y = i < pb.length ? pb[i] : "0";
            final int c = x.matches("\\d+") && y.matches("\\d+")
                ? Long.compare(Long.parseLong(x), Long.parseLong(y))
                : x.compareTo(y);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }
}
