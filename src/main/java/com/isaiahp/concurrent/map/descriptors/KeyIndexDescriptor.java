package com.isaiahp.concurrent.map.descriptors;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public interface KeyIndexDescriptor {

    boolean isEmptySlot(int i, DirectBuffer buffer);

    boolean isDeletedSlot(int i, DirectBuffer buffer);

    void setCharSequenceAt(int index, UnsafeBuffer buffer, long hashcode, CharSequence value);

    boolean valueEquals(int entryIndex, CharSequence key, long hash, DirectBuffer buffer);

    boolean markDeleted(int entry, UnsafeBuffer buffer);

    void checkCapacity(UnsafeBuffer buffer);

    int maxKeys();

    int maxKeySize();

    long requiredCapacity();

    int copyBytes(int entry, DirectBuffer buffer, byte[] dst, int dstOffset);

    int findKeyEntry(CharSequence key, long hashcode, DirectBuffer buffer);
}
