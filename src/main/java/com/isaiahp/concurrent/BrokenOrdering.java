package com.isaiahp.concurrent;


public class BrokenOrdering {

    static BrokenSingleWriterRecord record = new BrokenSingleWriterRecord();

    static long readCount;
    static long[] readBuffer = new long[16];

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





    public static class BrokenSingleWriterRecord implements SingleWriterRecord {
        long version = 0;
        long dataLong0;
        long dataLong1;

        @Override
        public long read(long[] result) {
            final long v1 = version;
            if ((v1 & 1) != 0) return -1;

            result[0] = dataLong0;
            result[1] = dataLong1;

            final long v2 = version;
            if (v2 != v1) return -1;
            return v2;
        }

         @Override
         public long write(long d0, long d1) {
            version++;

            dataLong0 = d0;
            dataLong1 = d1;

            version++;
            return version;
        }
    }

}
