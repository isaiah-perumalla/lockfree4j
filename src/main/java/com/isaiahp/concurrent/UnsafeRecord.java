package com.isaiahp.concurrent;

import org.agrona.UnsafeAccess;
import sun.misc.Unsafe;

public class UnsafeRecord implements SingleWriterRecord {
    private static final Unsafe UNSAFE = UnsafeAccess.UNSAFE;

    volatile long version = 0;
    long dataLong0 = 0;
    long dataLong1 = 0;

    public long read(long[] result) {
        final long v1 = version;
        if ((v1 & 1) != 0) return -1;

        result[0] = dataLong0;
        result[1] = dataLong1;
        UNSAFE.loadFence(); //ensure data is first loaded before re-loading version

        final long v2 = version;
        if (v2 != v1) return -1;
        return v2;
    }

    public long write(long d0, long d1) {
        final long v = version;
        version = v + 1;
        UNSAFE.storeFence();
        dataLong0 = d0;
        dataLong1 = d1;

        UNSAFE.storeFence();
        final long finalVersion = v + 2;
        version = finalVersion;

        return finalVersion;
    }
}
