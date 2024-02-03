package com.isaiahp.concurrent.map;

import org.agrona.concurrent.UnsafeBuffer;

public class AsciiIndexMap {
    private final UnsafeBuffer buffer;
    private final int maxKeySize;
    private final int maxNumberOfKeys;

    public AsciiIndexMap(UnsafeBuffer buffer, int maxKeySize, int maxNumberOfKeys) {

        this.buffer = buffer;
        this.maxKeySize = maxKeySize;
        this.maxNumberOfKeys = maxNumberOfKeys;
    }

    public int put(CharSequence value) {
        return 0;
    }

    public int getEntry(int entry, MutableAsciiString asciiString) {
        return 0;
    }
}
