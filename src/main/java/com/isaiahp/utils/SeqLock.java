package com.isaiahp.utils;

import org.agrona.UnsafeAccess;
import org.agrona.collections.MutableLong;
import org.agrona.concurrent.AtomicBuffer;

public class SeqLock {

    private static final ThreadLocal<MutableLong> REENTRANT_CHECK = ThreadLocal.withInitial(() -> new MutableLong());

    public static long writeBegin(AtomicBuffer buffer, int versionoffset) {
        //verify with single thread check
        assert (REENTRANT_CHECK.get().value & 1) == 0 : "lock write begin already in use by this thread, not reentrant";
        REENTRANT_CHECK.get().value += 1;
        final long version = buffer.getLongVolatile(versionoffset);
        assert (version & 1) == 0 : "version should be even, illegal state";
        buffer.putLongOrdered(versionoffset, version + 1);

        UnsafeAccess.UNSAFE.storeFence(); // dont want subsequent write to move above,  **putOrderLong is not enough** see https://github.com/isaiah-perumalla/lockfree4j/blob/a65d18db7553834cbe3fd896e4a61794048df3c2/src/jcstress/java/com/isaiahp/jcstress/MemoryOrdering.java ?

        return version + 1;
    }

    public static long writeEnd(AtomicBuffer buffer, int versionOffert, long version) {
        assert (version & 1) != 0;
        assert (REENTRANT_CHECK.get().value & 1) != 0;
        final long value = version + 1;
        buffer.putLongOrdered(versionOffert, value);
        REENTRANT_CHECK.get().value += 1;
        return value;
    }

    public static long readBegin(AtomicBuffer buffer, int versionOffset) {
        return buffer.getLongVolatile(versionOffset);
    }

    public static long readEnd(AtomicBuffer buffer, int versionOffset) {
        UnsafeAccess.UNSAFE.loadFence(); //ensure all reads above this call happens-before all reads and writes below this fence
        final long newVersion = buffer.getLongVolatile(versionOffset);
        return newVersion;
    }
}
