package com.isaiahp.concurrent.map;

import com.isaiahp.ascii.MutableAsciiSequence;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import org.agrona.concurrent.UnsafeBuffer;

public class AsciiIndexMap {
    private final UnsafeBuffer buffer;
    private final  int mask;
    private final KeyIndexDescriptor keyIndexDescriptor;

    public AsciiIndexMap(UnsafeBuffer buffer, KeyIndexDescriptor keyIndexDescriptor1) {
        this.keyIndexDescriptor = keyIndexDescriptor1;
        this.keyIndexDescriptor.checkCapacity(buffer);
        this.buffer = buffer;
        this.mask = keyIndexDescriptor.maxKeys() -1;
    }

    public int addKey(CharSequence key) {
        final long hashcode = keyIndexDescriptor.computeKeyHash(key);
        assert hashcode != 0  : "invalid hash code";
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



    public int readEntry(int entry, MutableAsciiSequence dst) {
        if (entry < 0 || entry > keyIndexDescriptor.maxKeys()) {
            throw new IllegalArgumentException("invalid entry");
        };
        return keyIndexDescriptor.copyBytes(entry, buffer, dst);
    }




    public int getEntry(CharSequence key) {

        final int index = keyIndexDescriptor.findKeyEntry(key, buffer);
        return index;

    }



    public boolean removeEntry(int entry) {
        assert entry >= 0 && entry < keyIndexDescriptor.maxKeySize();
        return keyIndexDescriptor.markDeleted(entry, buffer);
    }
}
