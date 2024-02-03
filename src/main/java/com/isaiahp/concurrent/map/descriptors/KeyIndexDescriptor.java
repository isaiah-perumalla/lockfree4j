package com.isaiahp.concurrent.map.descriptors;

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
public class KeyIndexDescriptor {
    public static final byte NULL_CHAR = (byte) '\0';
    private final int maxKeySize;

    public KeyIndexDescriptor(int maxKeySize, int maxNumberOfKeys) {

        this.maxKeySize = maxKeySize;
    }



    public int getKeyOffsetForIndex(int index) {
        return index * maxKeySize ;
    }


    public boolean isEmptySlot(int i, DirectBuffer buffer) {
        final int offset = getKeyOffsetForIndex(i);
        return buffer.getByte(offset) == '\0';
    }

    public boolean isDeletedSlot(int i, DirectBuffer buffer) {
        final int offset = getKeyOffsetForIndex(i);
        return buffer.getByte(offset) == '\1';
    }

    public void setCharSequenceAt(int index, UnsafeBuffer buffer, CharSequence value) {
        assert value.length() + 1 <= maxKeySize;
        final int offset = getKeyOffsetForIndex(index);
        for (int i = 0; i < value.length(); i++) {
            final byte asciiByte = (byte) value.charAt(i);
            buffer.putByte(offset + i, asciiByte);
        }
        buffer.putByte(offset + value.length(), NULL_CHAR);
    }

    public boolean valueEquals(int i, CharSequence key, DirectBuffer buffer) {
        //1 ch for null terminator
        if (key.length()-1 > maxKeySize) return false;
        final int offset = getKeyOffsetForIndex(i);
        for (int j = 0; j < key.length(); j++) {
            final byte ch = (byte) key.charAt(j);
            if (ch != buffer.getByte(offset + j)) {
                return false;
            }
        }
        final boolean nullTerminated = buffer.getByte(offset + key.length()) == '\0';
        return nullTerminated;
    }
}
