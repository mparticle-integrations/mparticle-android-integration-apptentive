package com.mparticle.kits;

import com.apptentive.android.sdk.ApptentiveLog;

import java.util.Map;

import static com.apptentive.android.sdk.ApptentiveLogTag.UTIL;

final class StringUtils {
    public static boolean tryParseSettingFlag(Map<String, String> settings, String key, boolean defaultValue) {
        final String value = settings.get(key);
        if (value != null) {
            final Boolean flag = StringUtils.tryParseBoolean(value);
            if (flag != null) {
                return flag;
            }
            ApptentiveLog.w(UTIL, "Unable to parse boolean flag '%s': %s", key, value);
        }

        return defaultValue;
    }

    public static Number tryParseNumber(String value) {
        Long longValue = tryParseLong(value);
        if (longValue != null) {
            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                return longValue.intValue();
            }
            return longValue;
        }

        return tryParseDouble(value);
    }

    public static Long tryParseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double tryParseDouble(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean tryParseBoolean(String value) {
        try {
            return Boolean.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
