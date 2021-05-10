package com.mparticle.kits;

final class StringUtils {
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
}
