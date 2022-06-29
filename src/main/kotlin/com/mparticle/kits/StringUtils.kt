package com.mparticle.kits

import com.apptentive.android.sdk.ApptentiveLog
import com.apptentive.android.sdk.ApptentiveLogTag

internal object StringUtils {
    @JvmStatic
    fun tryParseSettingFlag(
        settings: Map<String, String>,
        key: String?,
        defaultValue: Boolean
    ): Boolean {
        val value = settings[key]
        if (value != null) {
            val flag = value.toBoolean()
            if (flag != null) {
                return flag
            }
            ApptentiveLog.w(
                ApptentiveLogTag.UTIL,
                "Unable to parse boolean flag '%s': %s",
                key,
                value
            )
        }
        return defaultValue
    }

    @JvmStatic
    fun tryParseNumber(value: String): Number? {
        val longValue = tryParseLong(value)
        return if (longValue != null) {
            if (longValue >= Int.MIN_VALUE && longValue <= Int.MAX_VALUE) {
                longValue.toInt()
            } else longValue
        } else tryParseDouble(value)
    }

    fun tryParseLong(value: String): Long? {
        return try {
            value.toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun tryParseDouble(value: String): Double? {
        return try {
            java.lang.Double.valueOf(value)
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun tryParseBoolean(value: String): Boolean? {
        return try {
            value.toBoolean()
        } catch (e: NumberFormatException) {
            null
        }
    }
}