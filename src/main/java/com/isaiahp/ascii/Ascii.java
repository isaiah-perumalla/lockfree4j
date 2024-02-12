package com.isaiahp.ascii;

public class Ascii {
    public static final Hasher DJB_2_HASH = new Djb2Hash();
    public static final Hasher DEFAULT_HASH = new DefaultHash();
    public static boolean equals(CharSequence a, CharSequence b) {
        assert a != null && b != null;
        if (a.length() != b.length()) return false;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) return false;
        }
        return true;
    }

    public interface Hasher {
        long hash(CharSequence c);

    }
    /**
     * djb2 has by Daniel Bernstein,
     * work very well for ascii string
     */
    public static long djb2Hash(CharSequence c) {
        long h = 5381L;
        for (int i = 0; i < c.length(); i++) {
            final byte ch = (byte) c.charAt(i);
            h = ((h << 5L) + h) + ch;
        }
        return  h;
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

    public static class Djb2Hash implements Hasher {

        @Override
        public long hash(CharSequence c) {

            return djb2Hash(c);
        }
    }
    public static class MutableString implements CharSequence {

        private static final Hasher hasher = DJB_2_HASH;
        public static long hash(CharSequence ch) {
            if (ch == null) return 0;
            if (ch instanceof MutableString) {
                MutableString mutable = (MutableString) ch;
                return mutable.longHash();
            }
            return hasher.hash(ch);
        }
        private final int maxSize;
        private final byte[] chars;
        private int size;
        private long hash = 0;

        public MutableString(int maxSize) {
            this.maxSize = maxSize;
            this.chars = new byte[maxSize];
        }

        public void set(CharSequence value) {
            assert value.length() <= maxSize;
            for (int i = 0; i < value.length(); i++) {
                chars[i] = (byte) value.charAt(i);
            }
            size = value.length();
            hash = longHash();
        }
        @Override
        public int length() {
            return size;
        }

        @Override
        public char charAt(int index) {
            assert index < size && index >= 0;
            return (char) chars[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }

        public byte[] getBuffer() {
            return chars;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null ) return false;

            if (o instanceof CharSequence) {
                CharSequence chSequence = (CharSequence) o;
                return Ascii.equals(this, chSequence);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (int) longHash();
        }

        public long longHash() {
            if (this.hash == 0) {
                hash = hasher.hash(this);
            }
            return  hash;
        }


    }

    private static class DefaultHash implements Hasher {
        @Override
        public long hash(CharSequence c) {
            return Ascii.hash(c);
        }
    }
}
