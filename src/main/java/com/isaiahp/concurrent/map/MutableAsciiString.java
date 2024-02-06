package com.isaiahp.concurrent.map;

public class MutableAsciiString implements CharSequence {
    private final int maxSize;
    private final byte[] chars;
    private int size;

    public MutableAsciiString(int maxSize) {

        this.maxSize = maxSize;
        chars = new byte[maxSize];
    }

    public void set(CharSequence value) {
        assert value.length() <= maxSize;
        for (int i = 0; i < value.length(); i++) {
            chars[i] = (byte) value.charAt(i);
        }
        size = value.length();
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
}
