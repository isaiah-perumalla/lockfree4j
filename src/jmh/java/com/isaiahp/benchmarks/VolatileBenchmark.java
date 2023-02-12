package com.isaiahp.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
public class VolatileBenchmark {

    @Param({"0", "1"})
    int add;
    long val;
    volatile long volatileLong;

    @Benchmark
    public long baseLineRead() {
        long res = val + add;
        return res;
    }

    @Benchmark
    public long baseLineReadModifyWrite() {
        long res = val + add;
        val = res;
        return val;
    }
    @Benchmark
    public long volatileRead() {
        long res = volatileLong + add;
        return res;
    }

    @Benchmark
    public long volatileReadModifyWrite() {
        long res = volatileLong + add;
        volatileLong = res;
        return volatileLong;
    }
}
