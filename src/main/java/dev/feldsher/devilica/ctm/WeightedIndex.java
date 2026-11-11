/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.ctm;

/** MCPatcher's weighted choice of a tile from a position hash, so a block keeps its tile between rebuilds. */
abstract class WeightedIndex {

    private static final long K1 = 0xb492b66fbe98f273L;
    private static final long KMUL = 0x9ddfea08eb382d69L;

    static WeightedIndex create(int size, String weightList) {
        if (size <= 0) {
            return null;
        }
        final int[] weights = new int[size];
        int sum = 0;
        boolean weighted = false;
        final String[] list = weightList == null ? new String[0] : weightList.trim().split("\\s+");
        for (int i = 0; i < size; i++) {
            weights[i] = i < list.length && list[i].matches("\\d+") ? Math.max(Integer.parseInt(list[i]), 0) : 1;
            if (i > 0 && weights[i] != weights[0]) {
                weighted = true;
            }
            sum += weights[i];
        }
        if (!weighted || sum <= 0) {
            return new WeightedIndex() {
                @Override
                int choose(long key) {
                    return mod(key, size);
                }
            };
        }
        final int total = sum;
        return new WeightedIndex() {
            @Override
            int choose(long key) {
                int m = mod(key, total);
                int index = 0;
                while (index < size - 1 && m >= weights[index]) {
                    m -= weights[index];
                    index++;
                }
                return index;
            }
        };
    }

    abstract int choose(long key);

    static int mod(long n, int modulus) {
        return (int) (((n >> 32) ^ n) & 0x7fffffff) % modulus;
    }

    /** From CityHash (MIT licence), as MCPatcher has it */
    static long hash128To64(int i, int j, int k, int l) {
        long a = ((long) i << 32) | ((long) j & 0xffffffffL);
        long b = ((long) k << 32) | ((long) l & 0xffffffffL);
        a = shiftMix(a * K1) * K1;
        final long c = b * K1 + mix128to64(a, b);
        final long d = shiftMix(a + b);
        a = mix128to64(a, c);
        b = mix128to64(d, b);
        return a ^ b ^ mix128to64(b, a);
    }

    private static long shiftMix(long val) {
        return val ^ (val >>> 47);
    }

    private static long mix128to64(long u, long v) {
        final long a = shiftMix((u ^ v) * KMUL);
        long b = shiftMix((u ^ a) * KMUL);
        b *= KMUL;
        return b;
    }
}
