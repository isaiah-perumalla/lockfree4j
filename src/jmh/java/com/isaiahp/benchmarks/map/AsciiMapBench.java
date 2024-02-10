package com.isaiahp.benchmarks.map;

import com.isaiahp.ascii.Ascii;
import com.isaiahp.concurrent.map.AsciiIndexMap;
import com.isaiahp.concurrent.map.descriptors.CacheFriendlyKeyIndexDescriptor;
import com.isaiahp.concurrent.map.descriptors.DefaultKeyIndexDescriptor;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import org.agrona.collections.MutableInteger;
import org.agrona.collections.Object2IntHashMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS, time = 5000)

public class AsciiMapBench {
    public CharSequence findKey;
    public CharSequence notFound;
    public String findStr;
    private AsciiIndexMap asciiIndexBaseLineMap;
    private AsciiIndexMap asciiIndexHashCodeMap;
    private HashMap<CharSequence, MutableInteger> stdHashSet;
    private Object2IntHashMap<CharSequence> agronaMap;
    private Ascii.MutableString mutableAsciiString = new Ascii.MutableString(128);
    @Param(value = {
            "0.5",
            "0.75",
               "0.9"})
    private float loadFactor;
    HashSet<String> syms;
    @Setup
    public void setup() {
        syms = new HashSet<>();
        int maxKeys = 32768;
        String notFound = "ID:10985:Non-Cyclical_Consumer_Goods:BYND:E:06/28-19_W:D:2019-06-21-notfound";
        findStr = "ID:10985:Non-Cyclical_Consumer_Goods:BYND:E:06/28-19_W:D:2019-06-21";
        Ascii.MutableString notFoundMutableStr = new Ascii.MutableString(128);
        notFoundMutableStr.set(notFound);
        this.notFound = notFoundMutableStr;
        mutableAsciiString.set(findStr);
        findKey = mutableAsciiString;
        DefaultKeyIndexDescriptor indexDescriptor = new DefaultKeyIndexDescriptor(128, maxKeys);
        KeyIndexDescriptor cacheFriendly = new CacheFriendlyKeyIndexDescriptor(128, maxKeys);
        int capacity = (int) indexDescriptor.requiredCapacity();
        UnsafeBuffer buffer = new UnsafeBuffer(new byte[capacity]);
        asciiIndexBaseLineMap = new AsciiIndexMap(buffer, Ascii.DJB_2_HASH, indexDescriptor);
        UnsafeBuffer buffer2 = new UnsafeBuffer(new byte[(int) cacheFriendly.requiredCapacity()]);;
        asciiIndexHashCodeMap = new AsciiIndexMap(buffer2, Ascii.DJB_2_HASH, cacheFriendly);
        stdHashSet = new HashMap<>();
        agronaMap = new Object2IntHashMap<>( -1);
        String file = "/home/isaiahp/workspace/seqlock4j/src/jmh/resources/symbols.txt";
        try (
            Scanner scanner = new Scanner(new File(file))) {
            int count = 0;
            final int maxCount = (int)(loadFactor * maxKeys);
            while(scanner.hasNext()) {
                String s = scanner.nextLine();
                Ascii.MutableString key = new Ascii.MutableString(128);
                key.set(s);
                asciiIndexBaseLineMap.addKey(key);
                asciiIndexHashCodeMap.addKey(key);
                stdHashSet.put(key, new MutableInteger(key.length()));
                agronaMap.put(key, key.length());
                if (++count > maxCount) break;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
//
    }

    @Benchmark
    public void asciiMapBaseLineLayoutSearchMap(Blackhole bh) {
        int entry = asciiIndexBaseLineMap.getEntry(findKey);
        if (entry < 0) throw new IllegalStateException();
//        int size = asciiIndexMap.readEntry(entry, mutableAsciiString.getBuffer() , 0);
        bh.consume(entry);
        entry = asciiIndexBaseLineMap.getEntry(notFound);
        bh.consume(entry);

//        bh.consume(size);
    }

    @Benchmark
    public void asciiMapOptimizedLayoutSearchMap(Blackhole bh) {
        int entry = asciiIndexHashCodeMap.getEntry(findKey);
        if (entry < 0) throw new IllegalStateException();
//        int size = asciiIndexMap.readEntry(entry, mutableAsciiString.getBuffer() , 0);
        bh.consume(entry);
        entry = asciiIndexHashCodeMap.getEntry(notFound);
        bh.consume(entry);

//        bh.consume(size);
    }

    @Benchmark
    public void javaStdHashSet(Blackhole bh) {
        MutableInteger entry = stdHashSet.get(findKey);
//        int size = asciiIndexMap.readEntry(entry, mutableAsciiString.getBuffer() , 0);
        bh.consume(entry.value);
//        bh.consume(size);
    }

    @Benchmark
    public void agronaMap(Blackhole bh) {
        int entry = agronaMap.getValue(findKey);
//        int size = asciiIndexMap.readEntry(entry, mutableAsciiString.getBuffer() , 0);
        if (entry == -1) throw new IllegalStateException();
        bh.consume(entry);
        entry = agronaMap.getValue(notFound);
        bh.consume(entry);

//        bh.consume(size);
    }

    private static class ConstantHash implements Ascii.Hasher {

        @Override
        public long hash(CharSequence c) {
            return 1;
        }

    }
}
