package com.isaiahp.map;

import com.isaiahp.ascii.Ascii;
import com.isaiahp.ascii.Djb2Hasher;
import com.isaiahp.concurrent.map.AsciiIndexMap;
import com.isaiahp.concurrent.map.MutableAsciiString;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import com.isaiahp.shm.MMapFile;
import org.agrona.AsciiSequenceView;
import org.agrona.BitUtil;
import org.agrona.collections.IntHashSet;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;


public class AsciiMapTest {
    // ensure VM property is set --add-opens java.base/sun.nio.ch=ALL-UNNAMED

    public static final Path MMAP_PATH = Path.of("/tmp/AsciiMapTest-junit.bin");
    public static final int MAX_KEY_SIZE = 72;
    private static final int MAX_KEYS = 32;
    private static UnsafeBuffer buffer;
    private static MMapFile mmap;

    @BeforeAll
    public static void setUp() {
        int length = BitUtil.align(MAX_KEY_SIZE * MAX_KEYS, 8);
        mmap = MMapFile.create(false, MMAP_PATH, length);
        buffer = new UnsafeBuffer(mmap.getBufferAddress(), length);
    }

    @AfterAll
    public static void tearDown() {
        if (mmap != null) {
            mmap.close();
        }
        try {
            Files.deleteIfExists(MMAP_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
//    @ParameterizedTest
//    @MethodSource("checkExplicitMethodSourceArgs")
    @Test
    public void testAddRemoveFindEntry() {


        Ascii.Hasher asciiHasher = new Ascii.Hasher() {
            @Override
            public int hash(CharSequence c) {
                return MAX_KEYS - 1;
            }

        };
        KeyIndexDescriptor indexDescriptor = new KeyIndexDescriptor(MAX_KEY_SIZE, MAX_KEYS);
        final AsciiIndexMap map = new AsciiIndexMap(buffer, asciiHasher, indexDescriptor);
        String key1 = "ID:10931:Cyclical_Consumer_Goods:SGMS:E:01-19_M:D:2018-01-29";
        int key1Entry = map.addKey(key1);
        MutableAsciiString asciiString = new MutableAsciiString(128);
        int size = map.readEntry(key1Entry, asciiString.getBuffer(), 0);
        Assertions.assertEquals(key1.length(), size);
        int nextEntry = map.addKey(key1);
        Assertions.assertEquals(~nextEntry, key1Entry);
        String key2 = "ID:10725:Financials:ARI:E:08-18_M:D:2018-07-03";
        int key2Entry = map.getEntry(key2);
        Assertions.assertEquals((key1Entry + 1) % MAX_KEYS, ~key2Entry);
        key2Entry = map.addKey(key2);
        Assertions.assertEquals((key1Entry + 1) % MAX_KEYS, key2Entry);
        size = map.readEntry(key2Entry, asciiString.getBuffer(), 0);
        Assertions.assertEquals(key2.length(), size);
        Assertions.assertEquals(key2.length(), size);
        Assertions.assertTrue(map.removeEntry(key1Entry));
        Assertions.assertEquals(key2Entry, map.getEntry(key2));
        Assertions.assertEquals(key1Entry, map.addKey(key1));
    }

    @Test
    public void testRandomSymbols() {
        String[] randSyms = """
        ID:10909:Financials:C:E:06-18_M:D:2018-05-24
        ID:10672:ETF:SPY:E:04/11-18_W:D:2018-04-10
        ID:10716:Telecommunication:VOD:E:08/03-18_W:D:2018-07-30
        ID:10579:Cyclical_Consumer_Goods:MAS:E:05-19_M:D:2019-04-18
        ID:10874:Basic_Materials:FCX:E:04-18_M:D:2018-04-18
        ID:10918:Technology:AMD:E:03-19_Q:D:2019-03-19
        ID:10627:ETF:IWM:E:09/06-19_W:D:2019-07-29
        ID:10879:Financials:JPM:E:02-18_M:D:2018-01-16
        ID:10864:Cyclical_Consumer_Goods:DSW:E:02-18_M:D:2018-01-19
        ID:10562:Cyclical_Consumer_Goods:P:E:04-18_M:D:2018-03-22
        ID:10728:ETF:SPY:E:05/29-19_W:D:2019-05-10
        ID:10763:Non-Cyclical_Consumer_Goods:BTI:E:01-20_M:D:2018-11-12
        ID:10622:Basic_Materials:EGO:E:07-18_M:D:2018-02-01
        ID:10955:Non-Cyclical_Consumer_Goods:MDLZ:E:11-18_M:D:2018-10-26
        ID:10883:ETF:IWM:E:11/24-17_W:D:2017-11-21
        ID:10619:Financials:BAC:E:05-19_M:D:2019-04-08
        ID:10568:Technology:MSFT:E:07-18_M:D:2018-07-19
        ID:10513:Telecommunication:VZ:E:06-19_M:D:2019-06-11
        ID:10861:Telecommunication:CTL:E:03/01-19_W:D:2019-02-12
        ID:10631:Healthcare:JNJ:E:05-19_M:D:2019-05-07""".split("\n");

        byte[] asciiBytes = new byte[MAX_KEY_SIZE];
        final UnsafeBuffer asciiBuffer = new UnsafeBuffer(asciiBytes);
        AsciiSequenceView asciiSequenceView = new AsciiSequenceView();
        final AsciiIndexMap map = new AsciiIndexMap(buffer, new Djb2Hasher(32), new KeyIndexDescriptor(MAX_KEY_SIZE, 32));
        for (int i = 0; i < randSyms.length; i++) {
            String randSym = randSyms[i];
            final int entry = map.addKey(randSym);
            Assertions.assertTrue(entry >=0 && entry < 32);
            final int size = map.readEntry(entry, asciiBytes, 0);
            asciiSequenceView.wrap(asciiBuffer, 0, size);
            Assertions.assertTrue(Ascii.equals(randSym, asciiSequenceView));

        }
        Random rand = new Random();
        IntHashSet removedIndexes = new IntHashSet(32);
        for (int i = 0; i < randSyms.length; i++) {
            if (!removedIndexes.contains(i)) {
                final String sym = randSyms[i];
                final int entry = map.getEntry(sym);
                Assertions.assertTrue(entry >= 0 && entry < MAX_KEYS);
            }
        }

            int index = rand.nextInt(randSyms.length -1);

            assert index >= 0;
            final String removeSym = randSyms[index];
            final int removeEntry = map.getEntry(removeSym);
            if (!removedIndexes.contains(index)) {
                Assertions.assertTrue(removeEntry >= 0, removeSym);
                Assertions.assertTrue(map.removeEntry(removeEntry));
                Assertions.assertTrue(removeEntry >= 0 && removeEntry < MAX_KEYS);

                removedIndexes.add(index);
                Assertions.assertTrue(map.getEntry(removeSym) < 0);
            }
    }
}
