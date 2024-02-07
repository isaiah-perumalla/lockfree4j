package com.isaiahp.benchmarks.map;

import com.isaiahp.ascii.Ascii;
import com.isaiahp.ascii.Djb2Hasher;
import com.isaiahp.concurrent.map.AsciiIndexMap;
import com.isaiahp.concurrent.map.MutableAsciiString;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import org.agrona.DirectBuffer;
import org.agrona.collections.MutableInteger;
import org.agrona.collections.Object2IntHashMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
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
    public String findKey;
    private AsciiIndexMap asciiIndexMap;
    private HashMap<String, MutableInteger> stdHashSet;
    private Object2IntHashMap<String> agronaMap;
    private MutableAsciiString mutableAsciiString = new MutableAsciiString(128);
    HashSet<String> syms;
    @Setup
    public void setup() {
        syms = new HashSet<>();
        int maxKeys = 32768;
        findKey = "ID:10682:Non-Cyclical_Consumer_Goods:DWDP:E:05-18_M:D:2018-03-26";
        KeyIndexDescriptor indexDescriptor = new KeyIndexDescriptor(128, maxKeys);
        int capacity = (int) indexDescriptor.requiredCapacity();
        UnsafeBuffer buffer = new UnsafeBuffer(new byte[capacity]);
        asciiIndexMap = new AsciiIndexMap(buffer, new Djb2Hasher(maxKeys), indexDescriptor);
        stdHashSet = new HashMap<>();
        agronaMap = new Object2IntHashMap<>(-1);
        String file = "/home/isaiahp/workspace/seqlock4j/src/jmh/resources/symbols.txt";
        try (
            Scanner scanner = new Scanner(new File(file))) {
            int count = 0;
            while(scanner.hasNext()) {
                String key = scanner.nextLine();
                asciiIndexMap.addKey(key);
                stdHashSet.put(key, new MutableInteger(key.length()));
                agronaMap.put(key, key.length());
                if (++count > maxKeys/2) break;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
//
    }

    @Benchmark
    public void baseLineLayoutSearchMap(Blackhole bh) {
        int entry = asciiIndexMap.getEntry(findKey);
//        int size = asciiIndexMap.readEntry(entry, mutableAsciiString.getBuffer() , 0);
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
        bh.consume(entry);
//        bh.consume(size);
    }

    private static class ConstantHash implements Ascii.Hasher {
        private final int maxKeys;

        public ConstantHash(int maxKeys) {
            this.maxKeys = maxKeys;
        }

        @Override
        public int hash(CharSequence c) {
            return 1;
        }
        
    }
}
