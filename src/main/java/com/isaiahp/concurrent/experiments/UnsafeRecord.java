package com.isaiahp.concurrent.experiments;

import org.agrona.UnsafeAccess;
import sun.misc.Unsafe;

public class UnsafeRecord implements SingleWriterRecord {
    private static final Unsafe UNSAFE = UnsafeAccess.UNSAFE;
    private static final long VERSION_OFFSET;

    static {
        try {
            VERSION_OFFSET = UNSAFE.objectFieldOffset(UnsafeRecord.class.getDeclaredField("version"));
        }
        catch (Exception ex) { throw new Error(ex); }
    }

    long version = 0;
    long dataLong0 = 0;
    long dataLong1 = 0;

    public long read(long[] result) {
        //volatile read just a mov on x86 and needed to ensure data is not
        //read prior to reading the version
        final long v1 = UNSAFE.getLongVolatile(this, VERSION_OFFSET);
        if ((v1 & 1) != 0) return -1;

        result[0] = dataLong0;
        result[1] = dataLong1;

        UNSAFE.loadFence(); //ensure data is first loaded before re-loading version

        final long v2 = UNSAFE.getLongVolatile(this, VERSION_OFFSET);
        if (v2 != v1) return -1;
        return v2;
    }

    public long write(long d0, long d1) {
        final long v = version;
        version = v + 1;
//        UNSAFE.storeFence(); //ensure data write don't happen prior to version update

        dataLong0 = d0;
        dataLong1 = d1;


        UNSAFE.putOrderedLong(this, VERSION_OFFSET, v + 2);

        return v + 2;
    }
}
