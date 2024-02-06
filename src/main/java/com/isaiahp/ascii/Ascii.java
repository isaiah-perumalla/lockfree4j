package com.isaiahp.ascii;

import org.agrona.AsciiSequenceView;

public class Ascii {
    public static boolean equals(CharSequence a, CharSequence b) {
        assert a != null && b != null;
        if (a.length() != b.length()) return false;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) return false;
        }
        return true;
    }

    public interface Hasher {
        int hash(CharSequence c);

        int index(int h);
    }
    public static int hash(CharSequence value) {
        assert value != null;
        final int length = value.length();
        int h = 0;
        for (int i = 0; i < length; i += 4) {
            final byte b0 = (byte) value.charAt(i);
            final byte b1 = (byte) (i + 1 < length ? value.charAt(i+1) : 0);
            final byte b2 = (byte) (i + 2 < length ? value.charAt(i+2) : 0);
            final byte b3 = (byte) (i + 3 < length ? value.charAt(i+3) : 0);
            final int intVal = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
            h += intVal;
        }
        return h;
    }
}
