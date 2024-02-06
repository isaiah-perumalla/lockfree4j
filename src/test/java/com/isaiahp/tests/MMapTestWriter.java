package com.isaiahp.tests;

import com.isaiahp.shm.MMapFile;
import org.agrona.BitUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.status.ConcurrentCountersManager;
import org.agrona.concurrent.status.CountersManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class MMapTestWriter {

    public static void main(String[] args) throws IOException {
        Path path = Path.of("/dev/shm", "asciiMap.bin");
        MMapFile mmap = MMapFile.create(false, path, 2048 * 1024);
        final int numCounters = 8;
        final int offset = 0;
        final CountersManager countersManager = createCounterManager(numCounters, mmap, offset);
        final AtomicCounter writeCounter = countersManager.newCounter("writeCount");
        writeCounter.incrementOrdered();
        System.in.read();
    }

    private static CountersManager createCounterManager(int numCounters, MMapFile mmap, int offset) {
        final int metaDataLength = BitUtil.align(CountersManager.METADATA_LENGTH * numCounters, 8);
        final int valuesLength = BitUtil.align(CountersManager.COUNTER_LENGTH * numCounters, 8);
        final long totalSize = metaDataLength + valuesLength;
        final long startAddress = mmap.getBufferAddress() + offset;
        final UnsafeBuffer metdaDataBuffer = new UnsafeBuffer(startAddress, metaDataLength);
        final long valuesBufferAddress = startAddress + metaDataLength;
        final UnsafeBuffer valuesDataBuffer = new UnsafeBuffer(valuesBufferAddress, CountersManager.COUNTER_LENGTH * numCounters);

        final CountersManager countersManager = new ConcurrentCountersManager(metdaDataBuffer, valuesDataBuffer, StandardCharsets.US_ASCII);

        return countersManager;
    }
}
