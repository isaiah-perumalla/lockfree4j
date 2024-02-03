package com.isaiahp.concurrent.map;

import com.isaiahp.Ascii;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import org.agrona.concurrent.UnsafeBuffer;

public class AsciiIndexMap {
    private final UnsafeBuffer buffer;
    private final int maxKeySize;
    private final int maxNumberOfKeys;
    private final Ascii.Hasher hasher;
    private KeyIndexDescriptor keyIndexDescriptor;

    public AsciiIndexMap(UnsafeBuffer buffer, int maxKeySize, int maxNumberOfKeys) {
        this.hasher = Ascii.DEFAULT;
        this.buffer = buffer;
        this.maxKeySize = maxKeySize;
        this.maxNumberOfKeys = maxNumberOfKeys;
        this.keyIndexDescriptor = new KeyIndexDescriptor(maxKeySize, maxNumberOfKeys);
    }

    public int addKey(CharSequence key) {
        final int hashIndex = this.hasher.hash(key, maxNumberOfKeys);
        int i = hashIndex;

        while (!keyIndexDescriptor.isEmptySlot(i, buffer) && !keyIndexDescriptor.isDeletedSlot(i, buffer)) {
                if (keyIndexDescriptor.valueEquals(i, key, buffer)) {
                    return ~i; //key already exists
                }
        }
        keyIndexDescriptor.setCharSequenceAt(i, buffer, key);
        return i;
    }



    public int readEntry(int entry, byte[] dst, int dstOffset) {
        final int offset = keyIndexDescriptor.getKeyOffsetForIndex(entry);
        return copyBytes(offset, buffer, dst, dstOffset);
    }

    private int copyBytes(int srcOffset, UnsafeBuffer src, byte[] dst, int dstOffset) {
        for (int i = 0; i < maxKeySize; i++) {
            final byte b = src.getByte(srcOffset + i);
            if (b == '\0') {
                return i;
            }
            dst[dstOffset + i] = b;
        }
        return maxKeySize;
    }
}
