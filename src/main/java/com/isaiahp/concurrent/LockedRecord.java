package com.isaiahp.concurrent;

public class LockedRecord implements SingleWriterRecord {
    long version = 0;
    long dataLong0 = 0;
    long dataLong1 = 0;

    public synchronized long read(long[] result) {
        final long v1 = version;
        if ((v1 & 1) != 0) return -1;

        result[0] = dataLong0;
        result[1] = dataLong1;
        final long v2 = version;
        if (v2 != v1) return -1;
        return v2;
    }

    public synchronized long write(long d0, long d1) {
        final long v = version;
        version = v + 1;
        dataLong0 = d0;
        dataLong1 = d1;

        final long finalVersion = v + 2;
        version = finalVersion;
        return finalVersion;
    }
}
