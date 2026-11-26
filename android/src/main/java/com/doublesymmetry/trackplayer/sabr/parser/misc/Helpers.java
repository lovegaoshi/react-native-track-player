package com.doublesymmetry.trackplayer.sabr.parser.misc;

import java.util.Collection;
import java.util.function.Predicate;

/** Small helpers formerly supplied by SmartTube's sharedutils module. */
public final class Helpers {
    private Helpers() {
    }

    public static boolean equals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int parseInt(String value) {
        return parseInt(value, 0);
    }

    public static int parseInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long parseLong(String value) {
        return parseLong(value, 0L);
    }

    public static long parseLong(String value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static float parseFloat(String value, float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Inclusive range, matching SmartTube's Helpers.range(start, end, step). */
    public static int[] range(int start, int end, int step) {
        if (step == 0 || (step > 0 && start > end) || (step < 0 && start < end)) {
            return new int[0];
        }
        int size = (end - start) / step + 1;
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = start + i * step;
        }
        return result;
    }

    public static <T> T findFirst(Collection<T> items, Predicate<T> predicate) {
        if (items == null) {
            return null;
        }
        for (T item : items) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }
}
