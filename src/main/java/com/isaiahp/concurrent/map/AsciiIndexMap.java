package com.isaiahp.concurrent.map;

import com.isaiahp.ascii.Ascii;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import org.agrona.concurrent.UnsafeBuffer;

public class AsciiIndexMap {
    private final UnsafeBuffer buffer;
    private final Ascii.Hasher hasher;
    private final  int mask;
    private final KeyIndexDescriptor keyIndexDescriptor;

    public AsciiIndexMap(UnsafeBuffer buffer, Ascii.Hasher asciiHasher, KeyIndexDescriptor keyIndexDescriptor1) {
        this.keyIndexDescriptor = keyIndexDescriptor1;
        this.keyIndexDescriptor.checkCapacity(buffer);
        this.hasher = asciiHasher;
        this.buffer = buffer;
        this.mask = keyIndexDescriptor.maxKeys() -1;
    }

    public int addKey(CharSequence key) {
        final long hashcode = getHashcode(key);
        assert hashcode != 0 && hashcode != 1 : "invalid hash code";
        final int hashIndex = (int) (hashcode & mask);

        for (int i = 0; i < keyIndexDescriptor.maxKeys(); i++) {
            final int index = (hashIndex + i) & mask;
            if (keyIndexDescriptor.isEmptySlot(index, buffer) || keyIndexDescriptor.isDeletedSlot(index, buffer)) {
                keyIndexDescriptor.setCharSequenceAt(index, buffer, hashcode, key);
                return index;
            }

            if (keyIndexDescriptor.valueEquals(index, key, hashcode, buffer)) {
                return ~index; //key already exists
            }
        }
        assert false : "all slots full illegal state";
        return ~keyIndexDescriptor.maxKeys(); // notify full
    }

    private long getHashcode(CharSequence key) {
        final long hashcode = this.hasher.hash(key);
        return hashcode;
    }


    public int readEntry(int entry, byte[] dst, int dstOffset) {
        if (entry < 0 || entry > keyIndexDescriptor.maxKeys()) {
            throw new IllegalArgumentException("invalid entry");
        };
        return keyIndexDescriptor.copyBytes(entry, buffer, dst, dstOffset);
    }



    public int getEntry(CharSequence key) {
        final long hashcode = getHashcode(key);
        assert hashcode != 0 && hashcode != 1 : "invalid hash code";
        final int hashIndex = (int) (hashcode & mask);
        for (int i = 0; i < keyIndexDescriptor.maxKeys(); i++) {
            final int index = (hashIndex + i) & mask;
            if(keyIndexDescriptor.isEmptySlot(index, buffer)) {
                return ~index;
            }
            if (keyIndexDescriptor.valueEquals(index, key, hashcode, buffer)) {
                return index; //match
            }
        }
        assert false : "all slots full";
        return ~keyIndexDescriptor.maxKeys();// notify all items scanned
    }

    public boolean removeEntry(int entry) {
        assert entry >= 0 && entry < keyIndexDescriptor.maxKeySize();
        return keyIndexDescriptor.markDeleted(entry, buffer);
    }
}
