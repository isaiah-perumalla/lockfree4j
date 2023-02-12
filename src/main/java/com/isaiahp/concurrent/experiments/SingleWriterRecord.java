package com.isaiahp.concurrent.experiments;

public interface SingleWriterRecord {
    long read(long[] result);

    long write(long d0, long d1);
}
