package com.mparticle.kits;

import org.junit.Test;

import static com.mparticle.kits.CustomDataParser.parseValue;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;

public class CustomDataParserTest {
    @Test
    public void testParseData() {
        // boolean
        assertEquals(TRUE, parseValue("true"));
        assertEquals(TRUE, parseValue("True"));
        assertEquals(FALSE, parseValue("false"));
        assertEquals(FALSE, parseValue("False"));

        // integer
        assertEquals(12345, parseValue("12345"));
        assertEquals(-12345, parseValue("-12345"));
        assertEquals(Integer.MIN_VALUE, parseValue(Integer.toString(Integer.MIN_VALUE)));
        assertEquals(Integer.MAX_VALUE, parseValue(Integer.toString(Integer.MAX_VALUE)));

        // long
        assertEquals(Long.MIN_VALUE, parseValue(Long.toString(Long.MIN_VALUE)));
        assertEquals(Long.MAX_VALUE, parseValue(Long.toString(Long.MAX_VALUE)));

        // double
        assertEquals(3.14, parseValue("3.14"));
        assertEquals(-3.14, parseValue("-3.14"));

        // string
        assertEquals("test", parseValue("test"));
    }
}