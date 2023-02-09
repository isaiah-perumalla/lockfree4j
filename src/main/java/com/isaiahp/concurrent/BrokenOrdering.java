package com.isaiahp.concurrent;

import org.agrona.BufferUtil;
import org.agrona.UnsafeAccess;
import org.agrona.concurrent.UnsafeBuffer;

public class BrokenOrdering {

    public static final int PAGE_SIZE = 4096;
    static Record record = new Record();

    static long readCount;
    static byte[] readBuffer = new byte[16];
    static UnsafeBuffer data = new UnsafeBuffer(BufferUtil.allocateDirectAligned(PAGE_SIZE, 8));

    public static void main(String[] args) throws InterruptedException {

        Thread reader = new Thread(() -> {

            do {
                long r = record.read(readBuffer);
                readCount++;


            } while (true);
        });

        Thread writer = new Thread(() -> {
            do {
                record.write(0xFFFFFFFF_FFFFFFFFL, 0xFFFFFFFF_FFFFFFFFL);
                Thread.yield();
            } while (true);
        });


        reader.start();
        writer.start();

        writer.join();
        reader.join();
        System.out.println(readCount + readBuffer[0]);
    }





    public static class Record {
        long version = 0;
        long dataLong0;
        long dataLong1;

        public long read(byte[] result) {
            final long v1 = version;
            if ((v1 & 1) != 0) return -1;
            for (int i = 0; i < 8; i ++) {
                int shift = i * 8;
                byte b = (byte) ((dataLong0 >> shift) & 0xFF );
                result[i] = b;
            }
            for (int i = 8; i < 16; i ++) {
                long shift = (i - 8) * 8;
                byte b = (byte) ((dataLong1 >> shift) & 0xFF);
                result[i] = b;
            }
            final long v2 = version;
            if (v2 != v1) return -1;
            return v2;
        }

         public long write(long d0, long d1) {
            version++;

            dataLong0 = d0;
            dataLong1 = d1;

            version++;
            return version;
        }
    }

    public static class VolatileRecord {
        volatile long version = 0;
        long dataLong0;
        long dataLong1;

        public long read(byte[] result) {
            final long v1 = version;
            if ((v1 & 1) != 0) return -1;

            long d= dataLong0;
            long d1= dataLong1;
            UnsafeAccess.UNSAFE.loadFence();
            for (int i = 0; i < 8; i ++) {
                int shift = i * 8;
                byte b = (byte) ((d >> shift) & 0xFF );
                result[i] = b;
            }
            for (int i = 8; i < 16; i ++) {
                long shift = (i - 8) * 8;
                byte b = (byte) ((d1 >> shift) & 0xFF);
                result[i] = b;
            }
            UnsafeAccess.UNSAFE.loadFence();
            final long v2 = version;
            if (v2 != v1) return -1;
            return v2;
        }

        public long write(long d0, long d1) {
            long v = version;
            version = v + 1;
            UnsafeAccess.UNSAFE.storeFence();
            dataLong0 = d0;
            dataLong1 = d1;

            long finalVersion = v + 2;
            version = finalVersion;
            return finalVersion;
        }
    }
}
