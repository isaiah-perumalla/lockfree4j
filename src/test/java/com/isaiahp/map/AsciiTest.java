package com.isaiahp.map;

import com.isaiahp.ascii.Ascii;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AsciiTest {


    @Test
    public void testEncodeDecodePrefix() {
        Assertions.assertEquals(0x45,  Ascii.encodePrefix("E"));
        Assertions.assertEquals(2358921964229L,  Ascii.encodePrefix("EURUSD"));
        Ascii.MutableString mutableString = new Ascii.MutableString(16);
        Ascii.decodePrefix(2358921964229L, mutableString);

        Assertions.assertEquals("EURUSD", mutableString.toString());
        Ascii.decodePrefix(Ascii.encodePrefix("USDJPY_1M"), mutableString);
        Assertions.assertEquals("USDJPY_1M", mutableString.toString());

        boolean fullDecode = Ascii.decodePrefix(Ascii.encodePrefix("USDJPY_1MMJAM"), mutableString);
        Assertions.assertFalse(fullDecode);
        Assertions.assertEquals("USDJPY_1M", mutableString.toString());

    }
}
