package com.isaiahp.map;

import com.isaiahp.concurrent.map.AsciiIndexMap;
import com.isaiahp.concurrent.map.MutableAsciiString;
import com.isaiahp.shm.MMapFile;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class AsciiMapTest {
    // ensure VM property is set --add-opens java.base/sun.nio.ch=ALL-UNNAMED

    public static final Path MMAP_PATH = Path.of("/tmp/AsciiMapTest-junit.bin");
    private static UnsafeBuffer buffer;
    private static MMapFile mmap;
    @BeforeAll
    public static void setUp() {
        int length = 1024;
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
    @Test
    public void testEntry() {


        int MAX_KEY_SIZE = 64;
        int maxNumberOfKeys = 32;
        final AsciiIndexMap map = new AsciiIndexMap(buffer, MAX_KEY_SIZE, maxNumberOfKeys);
        String key = "ID:10931:Cyclical_Consumer_Goods:SGMS:E:01-19_M:D:2018-01-29";
        int entry = map.addKey(key);
        MutableAsciiString asciiString = new MutableAsciiString(128);
        int size = map.readEntry(entry, asciiString.getBuffer(), 0);
        Assertions.assertEquals(key.length(), size);
        int nextEntry = map.addKey(key);
        Assertions.assertEquals(~nextEntry, entry);
    }
}
