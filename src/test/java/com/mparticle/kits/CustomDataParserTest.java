package com.mparticle.kits;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CustomDataParserTest {
    @Test
    public void testParseData() {
        Map<String, String> data = new HashMap<>();

        // boolean
        data.put("key-1", "true");
        data.put("key-2", "True");
        data.put("key-3", "false");
        data.put("key-4", "False");

        // integer
        data.put("key-5", "12345");
        data.put("key-6", "-12345");
        data.put("key-7", Integer.toString(Integer.MIN_VALUE));
        data.put("key-8", Integer.toString(Integer.MAX_VALUE));

        // long
        data.put("key-9", Long.toString(Long.MIN_VALUE));
        data.put("key-10", Long.toString(Long.MAX_VALUE));

        // double
        data.put("key-11", "3.14");
        data.put("key-12", "-3.14");

        // string
        data.put("key-13", "test");

        Map<String, Object> expected = new HashMap<>();

        // boolean
        expected.put("key-1", Boolean.TRUE);
        expected.put("key-2", Boolean.TRUE);
        expected.put("key-3", Boolean.FALSE);
        expected.put("key-4", Boolean.FALSE);

        // integer
        expected.put("key-5", 12345);
        expected.put("key-6", -12345);
        expected.put("key-7", Integer.MIN_VALUE);
        expected.put("key-8", Integer.MAX_VALUE);

        // long
        expected.put("key-9", Long.MIN_VALUE);
        expected.put("key-10", Long.MAX_VALUE);

        // double
        expected.put("key-11", 3.14);
        expected.put("key-12", -3.14);

        // string
        expected.put("key-13", "test");

        Map<String, Object> actual = CustomDataParser.parseCustomData(data);
        assertEquals(expected, actual);
    }
}