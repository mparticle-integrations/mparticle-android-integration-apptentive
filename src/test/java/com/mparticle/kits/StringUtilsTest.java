package com.mparticle.kits;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {
    @Test
    public void testParseNumber() {
        assertEquals(Integer.valueOf(12345), (Integer) StringUtils.tryParseNumber("12345"));
        assertEquals(Long.valueOf(21474836479L), (Long) StringUtils.tryParseNumber("21474836479"));
        assertEquals(Long.valueOf(-21474836489L), (Long) StringUtils.tryParseNumber("-21474836489"));
        assertEquals(Double.valueOf(12345.0), (Double) StringUtils.tryParseNumber("12345.0"));
        assertNull(StringUtils.tryParseNumber("test"));
    }

    @Test
    public void testSettingsFlag() {
        Map<String, String> data = new HashMap<>();
        data.put("key", "true");
        assertTrue(StringUtils.tryParseSettingFlag(data, "key", false));
    }

    @Test
    public void testMissingSettingsFlag() {
        Map<String, String> data = new HashMap<>();
        assertFalse(StringUtils.tryParseSettingFlag(data, "key", false));
    }

    @Test
    public void testMissingCorruptedSettingsFlag() {
        Map<String, String> data = new HashMap<>();
        data.put("key", "123");
        assertFalse(StringUtils.tryParseSettingFlag(data, "key", false));
    }
}