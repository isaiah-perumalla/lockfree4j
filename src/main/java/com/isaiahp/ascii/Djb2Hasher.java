package com.isaiahp.ascii;

/**
 * djb2 has by Daniel Bernstein,
 * work very well for ascii string
 */
public class Djb2Hasher implements Ascii.Hasher {

    private final int maxKeys;

    public Djb2Hasher(int maxKeys) {

        this.maxKeys = maxKeys;
    }

    @Override
    public int hash(CharSequence c) {
        int h = 5381;
        for (int i = 0; i < c.length(); i++) {
            final byte ch = (byte) c.charAt(i);
            h = ((h << 5) + h) + ch;
        }
        return Math.abs(h);
    }

    @Override
    public int index(int h) {
        return h % maxKeys;
    }
}
