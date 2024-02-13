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
public class CacheFriendlyKeyIndexDescriptor implements KeyIndexDescriptor {
    public static final byte NULL_CHAR =  '\0';
    private static final long DELETED_HASH = 1L << 63; //length is -ve high 16 bits
    public static final int NULL_HASH = 0;
    private final int maxKeySize;
    private final int maxNumberOfKeys;
    private final int hashCodesStartOffset;
    private final int prefixStartOffset;
    private final int keyDataStartOffset;

    public CacheFriendlyKeyIndexDescriptor(int maxKeySize, int maxNumberOfKeys) {
        if (!BitUtil.isPowerOfTwo(maxNumberOfKeys)) {
            throw new IllegalArgumentException("max key must be power of two");
        }
        this.maxKeySize = maxKeySize;
        this.maxNumberOfKeys = maxNumberOfKeys;
        this.hashCodesStartOffset = 0;
        this.prefixStartOffset = hashCodesSlotSize(maxNumberOfKeys);
        this.keyDataStartOffset = prefixStartOffset + prefixSlotSize(maxNumberOfKeys);
    }

    private int getKeyOffsetForIndex(int index) {
        int pos = this.keyDataStartOffset + (index * maxKeySize);
        return pos;
    }


    @Override
    public boolean isEmptySlot(int i, DirectBuffer buffer) {
        return NULL_HASH == getHashCodeAt(i, buffer);
    }

    @Override
    public boolean isDeletedSlot(int i, DirectBuffer buffer) {
        return DELETED_HASH == getHashCodeAt(i, buffer);
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
        setHashcode(index, buffer, hashcode, value.length());
    }

    private void setHashcode(int index, UnsafeBuffer buffer, long hashcode, int length) {
        final int hashCodeOffset = getHashCodeOffset(index);
        long newHash = computeHash(hashcode, length);
        buffer.putLong(hashCodeOffset, newHash);
    }

    /**
     * encode length in the 64 bit hashcode
     * high 16 bits is length of string
     * @param hashcode
     * @param length
     * @return
     */
    private static long computeHash(long hashcode, int length) {
        assert length < Short.MAX_VALUE : "key length cannot exceed Short.MAX_VALUE";
        long newHash = hashcode & 0X0000FFFFFFFFFFFFL;
        long size = length;
        newHash = newHash | (size << 48L);
        assert size == (newHash >> 48) : "size not encoded in hash";
        return newHash;
    }

    private static short decodeKeyLength(long hashCode) {
        return (short)(hashCode >> 48);
    }
    private int getHashCodeOffset(int index) {
        return hashCodesStartOffset + (Long.BYTES * index);
    }

    @Override
    public boolean valueEquals(int entryIndex, CharSequence key, long hash, DirectBuffer buffer) {
        final long internalHash = computeHash(hash, key.length());
        if (key.length() > maxKeySize) return false;
        final long candidateHashCode = getHashCodeAt(entryIndex, buffer);
        if (candidateHashCode != internalHash) return false; //implies same key length

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

    private long getHashCodeAt(int entryIndex, DirectBuffer buffer) {
        final int offset = getHashCodeOffset(entryIndex);
        return buffer.getLong(offset);
    }

    @Override
    public boolean markDeleted(int entry, UnsafeBuffer buffer) {
        assert entry >= 0 && entry < maxNumberOfKeys;
        final int hashCodeOffset = getHashCodeOffset(entry);
        buffer.putLong(hashCodeOffset, DELETED_HASH);
        assert decodeKeyLength(DELETED_HASH) < 0;
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
        final int hashCodeSlotsSize = hashCodesSlotSize(maxNumberOfKeys);
        final int prefixSlotsSize = prefixSlotSize(maxNumberOfKeys);
        final int totalKeyDataSize = BitUtil.align(maxKeySize * maxNumberOfKeys, 8);
        return BitUtil.align(hashCodeSlotsSize + prefixSlotsSize + totalKeyDataSize, 8);
    }

    private static int hashCodesSlotSize(int maxNumberOfKeys) {
        final int hashCodeSlotsSize = BitUtil.align(Long.BYTES * maxNumberOfKeys, 8);
        return hashCodeSlotsSize;
    }

    private static int prefixSlotSize(int maxNumberOfKeys) {
        //9 char ascii encoded in 63 bit
        final int hashCodeSlotsSize = BitUtil.align(Long.BYTES * maxNumberOfKeys, 8);
        return hashCodeSlotsSize;
    }

    @Override
    public int copyBytes(int entryIndex, DirectBuffer src, byte[] dst, int dstOffset) {
        final long hashCode = getHashCodeAt(entryIndex, src);
        final short keyLength = decodeKeyLength(hashCode);
        assert keyLength >= 0 && keyLength <= maxKeySize : "invalid key length";
        final int srcOffset = getKeyOffsetForIndex(entryIndex);
        for (int i = 0; i < keyLength; i++) {
            final byte b = src.getByte(srcOffset + i);
            dst[dstOffset + i] = b;
        }
        return keyLength;
    }
}
