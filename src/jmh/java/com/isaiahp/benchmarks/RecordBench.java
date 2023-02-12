package com.isaiahp.benchmarks;

import com.isaiahp.concurrent.experiments.LockedRecord;
import com.isaiahp.concurrent.experiments.SingleWriterRecord;
import com.isaiahp.concurrent.experiments.UnsafeRecord;
import com.isaiahp.concurrent.experiments.VolatileRecord;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Group)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 10000)
public class RecordBench {

    @Param(value = {
                "VolatileRecord",
                "UnsafeRecord",
//                "LockedRecord"
    })
    private String implementation;
    @State(Scope.Thread)
    public static class ReaderState {
        private final long[] buffer = new long[4];
    }

    @State(Scope.Thread)
    public static class WriterState {
        private long version;
        private Random random = new Random();
    }

    private SingleWriterRecord record;
    @Setup
    public void setup() {
//        record = new BrokenOrdering.BrokenSingleWriterRecord();
        if (implementation != null) {
            switch (implementation) {
                case "VolatileRecord":
                    record = new VolatileRecord();
                    break;
                case "LockedRecord":
                    record = new LockedRecord();
                    break;
                    case "UnsafeRecord":
                    record = new UnsafeRecord();
                    break;
            }
        }

    }

    @Benchmark
    @Group("sharedRecord")
    @GroupThreads(1)
    public long writeRand(WriterState state, Blackhole bh) {
        long v = state.random.nextLong();
        long write = record.write(v, v);
        bh.consume(write);
        bh.consume(v);
//        Thread.yield();
        return write;
    }

    @Benchmark
    @Group("sharedRecord")
    @GroupThreads(2)
    public long read(ReaderState state, Blackhole bh) {
        clearBuffer(state.buffer);
        long v;
        do {
            v = record.read(state.buffer);
        } while(v < 0);

        if (v > 0 && state.buffer[0] != state.buffer[1]) {
            //incorrect read
            throw new IllegalStateException("incorrect read");
        }
        bh.consume(state.buffer[0]);
        bh.consume(state.buffer[1]);
        return v;
    }

    private static void clearBuffer(long[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
    }
}
