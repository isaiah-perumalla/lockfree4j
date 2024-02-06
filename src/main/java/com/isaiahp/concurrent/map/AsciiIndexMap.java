package com.isaiahp.concurrent.map;

import com.isaiahp.ascii.Ascii;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import org.agrona.concurrent.UnsafeBuffer;

public class AsciiIndexMap {
    private final UnsafeBuffer buffer;
    private final int maxKeySize;
    private final int maxNumberOfKeys;
    private final Ascii.Hasher hasher;
    private KeyIndexDescriptor keyIndexDescriptor;

    public AsciiIndexMap(UnsafeBuffer buffer, int maxKeySize, int maxNumberOfKeys, Ascii.Hasher asciiHasher) {
        this.keyIndexDescriptor = new KeyIndexDescriptor(maxKeySize, maxNumberOfKeys);
        this.keyIndexDescriptor.checkCapacity(buffer);
        this.hasher = asciiHasher;
        this.buffer = buffer;
        this.maxKeySize = maxKeySize;
        this.maxNumberOfKeys = maxNumberOfKeys;

    }

    public int addKey(CharSequence key) {
        final int hashIndex = this.hasher.hash(key);
        for (int i = 0; i < maxNumberOfKeys; i++) {
            final int index = hasher.index(hashIndex + i);
            if (keyIndexDescriptor.isEmptySlot(index, buffer) || keyIndexDescriptor.isDeletedSlot(index, buffer)) {
                keyIndexDescriptor.setCharSequenceAt(index, buffer, key);
                return index;
            }

            if (keyIndexDescriptor.valueEquals(index, key, buffer)) {
                return ~index; //key already exists
            }
        }
        assert false : "all slots full illegal state";
        return ~maxNumberOfKeys; // notify full
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

    public int getEntry(CharSequence key) {
        final int hashIndex = this.hasher.hash(key);

        for (int i = 0; i < maxKeySize; i++) {
            final int index = hasher.index(hashIndex + i);
            if(keyIndexDescriptor.isEmptySlot(index, buffer)) {
                return ~index;
            }
            if (keyIndexDescriptor.valueEquals(index, key, buffer)) {
                return index; //match
            }
            ;
        }
        assert false : "all slots full";
        return ~maxNumberOfKeys;// notify all items scanned
    }

    public boolean removeEntry(int entry) {
        assert entry >= 0 && entry < maxKeySize;
        return keyIndexDescriptor.markDeleted(entry, buffer);
    }
}
