package com.isaiahp.concurrent.map.descriptors;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
* Metadata Layout
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +---------------------------------------------------------------+
 *  |                   KeyItem_VERSION                             |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                   prefix                                      |
 *  |                                                               |
 *  +---------------------------------------------------------------+

                        repeats
 *  +---------------------------------------------------------------+
 *  |                   KeyItem_VERSION                             |
 *  |                                                               |
 *  +---------------------------------------------------------------+
 *  |                  prefix                                       |
 *  |                                                               |
 *  +---------------------------------------------------------------+

 * </pre>


 */
public class DefaultKeyIndexDescriptor implements KeyIndexDescriptor {
    public static final byte NULL_CHAR =  '\0';
    private static final byte DELETED_CHAR = '\1';
    private final int maxKeySize;
    private final int maxNumberOfKeys;

    public DefaultKeyIndexDescriptor(int maxKeySize, int maxNumberOfKeys) {
        if (!BitUtil.isPowerOfTwo(maxNumberOfKeys)) {
            throw new IllegalArgumentException("max key must be power of two");
        }
        this.maxKeySize = maxKeySize;
        this.maxNumberOfKeys = maxNumberOfKeys;
    }




    private int getKeyOffsetForIndex(int index) {
        return index * maxKeySize ;
    }


    @Override
    public boolean isEmptySlot(int i, DirectBuffer buffer) {
        final int offset = getKeyOffsetForIndex(i);
        return buffer.getByte(offset) == '\0';
    }

    @Override
    public boolean isDeletedSlot(int i, DirectBuffer buffer) {
        final int offset = getKeyOffsetForIndex(i);
        return buffer.getByte(offset) == '\1';
    }

    @Override
    public int findKeyEntry(CharSequence key, long hashcode, DirectBuffer buffer) {
        final int mask = maxKeys() -1;
        final int hashIndex = (int) (hashcode & mask);
        for (int i = 0; i < mask; i++) {
            final int index = (hashIndex + i) & mask;
            if(isEmptySlot(index, buffer)) {
                return ~index;
            }
            if (valueEquals(index, key, hashcode, buffer)) {
                return index;
            }
        }
        assert false : "all slots full";
        return ~maxKeys();// notify all items scanned
    }

    @Override
    public void setCharSequenceAt(int index, UnsafeBuffer buffer, long hashcode, CharSequence value) {
        assert value.length() + 1 <= maxKeySize;
        final int offset = getKeyOffsetForIndex(index);
        for (int i = 0; i < value.length(); i++) {
            final byte asciiByte = (byte) value.charAt(i);
            buffer.putByte(offset + i, asciiByte);
        }
        buffer.putByte(offset + value.length(), NULL_CHAR);
    }

    @Override
    public boolean valueEquals(int entryIndex, CharSequence key, long hash, DirectBuffer buffer) {
        //1 ch for null terminator
        if (key.length()-1 > maxKeySize) return false;
        final int offset = getKeyOffsetForIndex(entryIndex);
        for (int j = 0; j < key.length(); j++) {
            final byte ch = (byte) key.charAt(j);
            if (ch != buffer.getByte(offset + j)) {
                return false;
            }
        }
        final boolean nullTerminated = buffer.getByte(offset + key.length()) == '\0';
        return nullTerminated;
    }

    @Override
    public boolean markDeleted(int entry, UnsafeBuffer buffer) {
        final int offset = getKeyOffsetForIndex(entry);
        byte b = buffer.getByte(offset);
        if (b == NULL_CHAR || b == DELETED_CHAR) return false;
        buffer.putByte(offset, DELETED_CHAR);
        return true;
    }

    @Override
    public void checkCapacity(UnsafeBuffer buffer) {
        assert buffer.capacity() >= maxKeySize * maxNumberOfKeys;
        if (buffer.capacity() < maxKeySize * maxNumberOfKeys) {
            throw new IllegalArgumentException("buffer size too small");
        }
    }

    @Override
    public int maxKeys() {
        return maxNumberOfKeys;
    }

    @Override
    public int maxKeySize() {
        return maxKeySize;
    }

    @Override
    public long requiredCapacity() {
        return BitUtil.align(maxKeySize * maxNumberOfKeys, 8);
    }

    @Override
    public int copyBytes(int entryIndex, DirectBuffer src, byte[] dst, int dstOffset) {
        final int srcOffset = getKeyOffsetForIndex(entryIndex);
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
