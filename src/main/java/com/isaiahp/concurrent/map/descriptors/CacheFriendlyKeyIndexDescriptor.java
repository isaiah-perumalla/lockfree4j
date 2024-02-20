package com.isaiahp.concurrent.map.descriptors;

import com.isaiahp.ascii.Ascii;
import com.isaiahp.ascii.MutableAsciiSequence;
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
    private static final long DELETED_HASH = 0x1L << 63; //length is -ve high 16 bits
    public static final int NULL_HASH = 0;
    private final int maxKeySize;
    private final int maxNumberOfKeys;
    private final int hashCodesStartOffset;
    private final int keyDataStartOffset;
    private final Ascii.Hasher hasher;
    public CacheFriendlyKeyIndexDescriptor(int maxKeySize, int maxNumberOfKeys, Ascii.Hasher hasher) {
        this.hasher = hasher;
        if (!BitUtil.isPowerOfTwo(maxNumberOfKeys)) {
            throw new IllegalArgumentException("max key must be power of two");
        }
        this.maxKeySize = maxKeySize;
        this.maxNumberOfKeys = maxNumberOfKeys;
        this.hashCodesStartOffset = 0;
        this.keyDataStartOffset = hashCodesSlotSize(maxNumberOfKeys);

    }

    public static long computeRequiredCapacity(int maxKeySize, int maxNumberOfKeys) {
        final int hashCodeSlotsSize = hashCodesSlotSize(maxNumberOfKeys);
        final int totalKeyDataSize = BitUtil.align(maxKeySize * maxNumberOfKeys, 8);
        return BitUtil.align(hashCodeSlotsSize + totalKeyDataSize, 8);
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
        final int hashCodeOffset = getHashCodeOffset(index);
        buffer.putLong(hashCodeOffset, hashcode);
    }


    /**
     * encode length in the 64 bit hashcode
     * high 16 bits is length of string
     * @param hashcode
     * @param length
     * @return
     */
    private static long encodeLength(long hashcode, int length) {
        assert length < Short.MAX_VALUE : "key length cannot exceed Short.MAX_VALUE";

        long newHash = hashcode & 0X0000FFFFFFFFFFFFL; //mask high 16 bits
        long size = length;
        newHash = newHash | (size << 48L);
        assert size == decodeKeyLength(newHash) : "size not encoded in hash";
        return newHash;
    }

    private static short decodeKeyLength(long hashCode) {
        return (short)((hashCode >> 48) & 0xFFFF);
    }
    private int getHashCodeOffset(int index) {
        return hashCodesStartOffset + (Long.BYTES * index);
    }

    @Override
    public boolean valueEquals(int entryIndex, CharSequence key, long hash, DirectBuffer buffer) {
        if (key.length() > maxKeySize) return false;

        final int offset = getKeyOffsetForIndex(entryIndex);
        for (int j = 0; j < key.length(); j++) {
            final byte ch = (byte) key.charAt(j);
            if (ch != buffer.getByte(offset + j)) {
                return false;
            }
        }
        return true;
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

        assert decodeKeyLength(DELETED_HASH) < 0 : "deleted hash, lenght not -ve";
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


    private static int hashCodesSlotSize(int maxNumberOfKeys) {
        final int hashCodeSlotsSize = BitUtil.align(Long.BYTES * maxNumberOfKeys, 8);
        return hashCodeSlotsSize;
    }



    @Override
    public int copyBytes(int entryIndex, DirectBuffer src, MutableAsciiSequence dst) {
        final long hashCode = getHashCodeAt(entryIndex, src);

        final short keyLength = decodeKeyLength(hashCode);
        assert keyLength >= 0 && keyLength <= maxKeySize : "invalid key length";
        final int srcOffset = getKeyOffsetForIndex(entryIndex);
        dst.copyFrom(src, srcOffset, keyLength);
        return keyLength;
    }



    @Override
    public int findKeyEntry(CharSequence target, DirectBuffer buffer) {
        final long hashcode = computeKeyHash(target);

        final int mask = maxKeys() -1;
        final int hashIndex = (int) (hashcode & mask);


        for (int i = 0; i < maxKeys(); i++) {
            final int index = (hashIndex + i) & mask;
            final long candidateHashCode = getHashCodeAt(index, buffer);
            final int candidateLength = decodeKeyLength(candidateHashCode);
            if(candidateHashCode == NULL_HASH) { //empty null slot
                return ~index;
            }
            if(candidateLength != target.length()) continue;
            if (valueEquals(index, target, hashcode, buffer)) {
                return index;
            }
        }
        assert false : "all slots full";
        return ~maxKeys();// notify all items scanned
    }

    @Override
    public long computeKeyHash(CharSequence key) {
        assert key.length() <= Short.MAX_VALUE;

        final long hashCode = hasher.hash(key);
        return encodeLength(hashCode, key.length());
    }

}
