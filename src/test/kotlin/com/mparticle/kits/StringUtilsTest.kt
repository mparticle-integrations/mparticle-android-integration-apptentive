package com.mparticle.kits

import com.mparticle.kits.StringUtils.tryParseNumber
import com.mparticle.kits.StringUtils.tryParseSettingFlag
import org.junit.Assert
import org.junit.Test
import java.util.HashMap

class StringUtilsTest {
    @Test
    fun testParseNumber() {
        Assert.assertEquals(Integer.valueOf(12345), tryParseNumber("12345") as Int?)
        Assert.assertEquals(
            java.lang.Long.valueOf(21474836479L),
            tryParseNumber("21474836479") as Long?
        )
        Assert.assertEquals(
            java.lang.Long.valueOf(-21474836489L),
            tryParseNumber("-21474836489") as Long?
        )
        Assert.assertEquals(java.lang.Double.valueOf(12345.0), tryParseNumber("12345.0") as Double?)
        Assert.assertNull(tryParseNumber("test"))
    }

    @Test
    fun testSettingsFlag() {
        val data= HashMap<String, String>()
        data["key"] = "true"
        Assert.assertTrue(tryParseSettingFlag(data, "key", false))
    }

    @Test
    fun testMissingSettingsFlag() {
        val data= HashMap<String, String>()
        Assert.assertFalse(tryParseSettingFlag(data, "key", false))
    }

    @Test
    fun testMissingCorruptedSettingsFlag() {
        val data = HashMap<String, String>()
        data["key"] = "123"
        Assert.assertFalse(tryParseSettingFlag(data, "key", false))
    }
}