package com.isaiahp.map;

import com.isaiahp.ascii.Ascii;
import com.isaiahp.concurrent.map.AsciiIndexMap;
import com.isaiahp.concurrent.map.descriptors.CacheFriendlyKeyIndexDescriptor;
import com.isaiahp.concurrent.map.descriptors.KeyIndexDescriptor;
import com.isaiahp.shm.MMapFile;
import org.agrona.AsciiSequenceView;
import org.agrona.LangUtil;
import org.agrona.collections.IntHashSet;
import org.agrona.collections.Object2IntHashMap;
import org.agrona.collections.ObjectHashSet;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Consumer;


public class AsciiMapTest {
    // ensure VM property is set --add-opens java.base/sun.nio.ch=ALL-UNNAMED

    public static final Path MMAP_PATH = Path.of("/tmp/AsciiMapTest-junit.bin");
    public static final int MAX_KEY_SIZE = 72;
    private static final int MAX_KEYS = 32;
    private static UnsafeBuffer buffer;
    private static MMapFile mmap;
    static KeyIndexDescriptor indexDescriptor;
    @BeforeAll
    public static void setUp() {
        indexDescriptor = new CacheFriendlyKeyIndexDescriptor(MAX_KEY_SIZE, MAX_KEYS);

        final long length = indexDescriptor.requiredCapacity();
        mmap = MMapFile.create(false, MMAP_PATH, length);
        buffer = new UnsafeBuffer(mmap.getBufferAddress(), (int) length);
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


        Ascii.Hasher asciiHasher = c -> MAX_KEYS - 1;
        final AsciiIndexMap map = new AsciiIndexMap(buffer, asciiHasher, indexDescriptor);
        String key1 = "ID:10931:Cyclical_Consumer_Goods:SGMS:E:01-19_M:D:2018-01-29";
        int key1Entry = map.addKey(key1);
        Ascii.MutableString asciiString = new Ascii.MutableString(128);
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
    public void testShortStringsSample() {
        HashSet<String> shortSyms = new HashSet<>();
        int count = loadSymbols(1024, "short_syms.txt", s -> shortSyms.add(s));
        CacheFriendlyKeyIndexDescriptor newIndexDescriptor = new CacheFriendlyKeyIndexDescriptor(8, 1024);
        UnsafeBuffer mutableBuffer = new UnsafeBuffer(new byte[(int) newIndexDescriptor.requiredCapacity()]);
        final AsciiIndexMap map = new AsciiIndexMap(mutableBuffer, Ascii.MutableString::hash, newIndexDescriptor);
        int MISSING_VALUE = -1;
        final Object2IntHashMap<String> symbolToEntry = new Object2IntHashMap<>(MISSING_VALUE);
        for (String s: shortSyms) {
            final int entry = map.addKey(s);
            Assertions.assertTrue(entry >=0 && entry <= 1024);
            Assertions.assertEquals(MISSING_VALUE, symbolToEntry.put(s, entry), "ERROR: entry already exists");
        }
        for (String s: shortSyms) {
            final int entry = map.getEntry(s);
            Assertions.assertTrue(entry >=0 && entry <= 1024);
            Assertions.assertEquals(entry, symbolToEntry.getValue(s), "incorrect entry for key");
        }
    }
    @Test
    public void testSampleSet() {
        int maxKeys = 32768;
        float loadFactor = 0.75f;
        String findStr = "ID:10682:Non-Cyclical_Consumer_Goods:DWDP:E:05-18_M:D:2018-03-26";
        CacheFriendlyKeyIndexDescriptor newIndexDescriptor = new CacheFriendlyKeyIndexDescriptor(128, maxKeys);
        UnsafeBuffer mutableBuffer = new UnsafeBuffer(new byte[(int) newIndexDescriptor.requiredCapacity()]);
        final AsciiIndexMap map = new AsciiIndexMap(mutableBuffer, Ascii.MutableString::hash, newIndexDescriptor);

        final int limit = (int) (maxKeys * loadFactor);
        final int symCount = loadSymbols(limit, "symbols.txt", s -> map.addKey(s));

        Ascii.MutableString mutableAscii = createMutableAsciiString("ID:10995:Cyclical_Consumer_Goods:WMT:E:11/09-18_W:D:2018-10-30");
        Assertions.assertTrue(map.getEntry(findStr) >= 0);
        Assertions.assertTrue(map.getEntry(mutableAscii) >= 0);
    }

    private static InputStream getInputStream(String file) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(file);
        if (is == null) {
            throw new IllegalArgumentException("symbol file not found ");
        }
        return is;
    }
    private static int loadSymbols(int limit, String file, Consumer<String> stringConsumer) {
        int count = 0;
        final InputStream inputStream = getInputStream(file);
        try (Scanner scanner = new Scanner(inputStream)) {
            while(scanner.hasNext()) {
                String key = scanner.nextLine();
                stringConsumer.accept(key);
                if (++count == limit) {
                    break;
                }
            }
        } catch (Exception e) {
            LangUtil.rethrowUnchecked(e);
        }
        finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LangUtil.rethrowUnchecked(e);
            }
        }

        return count;
    }

    @Test
    public void testAddRemoveFindMutableAsciiEntry() {

        Ascii.Hasher asciiHasher = c -> MAX_KEYS - 1;
        clearBuffer(buffer);
        final AsciiIndexMap map = new AsciiIndexMap(buffer, asciiHasher, indexDescriptor);
        Ascii.MutableString mutableAscii = createMutableAsciiString("ID:10931:Cyclical_Consumer_Goods:SGMS:E:01-19_M:D:2018-01-29");
        CharSequence key1 = mutableAscii ;
        int key1Entry = map.addKey(key1);
        Assertions.assertTrue(key1Entry >= 0);
        Ascii.MutableString asciiString = new Ascii.MutableString(128);
        int size = map.readEntry(key1Entry, asciiString.getBuffer(), 0);
        Assertions.assertEquals(key1.length(), size);
        int nextEntry = map.addKey(key1);
        Assertions.assertEquals(~nextEntry, key1Entry);
        CharSequence key2 = createMutableAsciiString("ID:10725:Financials:ARI:E:08-18_M:D:2018-07-03");
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

    private static void clearBuffer(UnsafeBuffer buffer1) {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer1.putByte(i, (byte) 0);
        }
    }

    private static Ascii.MutableString createMutableAsciiString(String str) {
        Ascii.MutableString mutableAscii = new Ascii.MutableString(128);
        mutableAscii.set(str);
        return mutableAscii;
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
        clearBuffer(buffer);
        byte[] asciiBytes = new byte[MAX_KEY_SIZE];
        final UnsafeBuffer asciiBuffer = new UnsafeBuffer(asciiBytes);
        AsciiSequenceView asciiSequenceView = new AsciiSequenceView();
//        KeyIndexDescriptor keyIndexDescriptor1 = new DefaultKeyIndexDescriptor(MAX_KEY_SIZE, 32);
        final AsciiIndexMap map = new AsciiIndexMap(buffer, Ascii::djb2Hash, indexDescriptor);
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
