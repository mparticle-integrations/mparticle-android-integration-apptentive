package com.mparticle.kits;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringUtilsTest {
    @Test
    public void testParseNumber() {
        assertEquals(Integer.valueOf(12345), (Integer) StringUtils.tryParseNumber("12345"));
        assertEquals(Long.valueOf(21474836479L), (Long) StringUtils.tryParseNumber("21474836479"));
        assertEquals(Long.valueOf(-21474836489L), (Long) StringUtils.tryParseNumber("-21474836489"));
        assertEquals(Double.valueOf(12345.0), (Double) StringUtils.tryParseNumber("12345.0"));
        assertNull(StringUtils.tryParseNumber("test"));
    }
}