package com.mparticle.kits;

import com.apptentive.android.sdk.ApptentiveLog;

import java.util.HashMap;
import java.util.Map;

final class CustomDataParser {
    public static Map<String, Object> parseCustomData(Map<String, String> map) {
        Map<String, Object> res = new HashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            res.put(e.getKey(), parseValue(e.getValue()));
        }
        return res;
    }

    public static Object parseValue(String value) {
        try {
            return value != null ? parseValueGuarded(value) : null;
        } catch (Exception e) {
            ApptentiveLog.e(e, "Unable to parse value: '%s'", value);
            return value;
        }
    }

    private static Object parseValueGuarded(String value) {
        // check for boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        // check for number
        Number number = StringUtils.tryParseNumber(value);
        if (number != null) {
            return number;
        }

        // default to the original value
        return value;
    }
}
