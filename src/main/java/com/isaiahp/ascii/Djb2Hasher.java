package com.isaiahp.ascii;

import org.agrona.BitUtil;

/**
 * djb2 has by Daniel Bernstein,
 * work very well for ascii string
 */
public class Djb2Hasher implements Ascii.Hasher {

    private final int mask;

    public Djb2Hasher(int maxKeys) {
        if (!BitUtil.isPowerOfTwo(maxKeys)) {
            throw new IllegalArgumentException("size must be power of two");
        }
        this.mask = maxKeys -1;
    }

    @Override
    public int hash(CharSequence c) {
        long h = 5381L;
        for (int i = 0; i < c.length(); i++) {
            final byte ch = (byte) c.charAt(i);
            h = ((h << 5L) + h) + ch;
        }
        return (int) (h & mask);
    }

}
