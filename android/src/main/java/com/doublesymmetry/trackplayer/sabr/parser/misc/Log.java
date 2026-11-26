package com.doublesymmetry.trackplayer.sabr.parser.misc;

import android.util.SparseArray;

import java.util.Locale;

/** Thin wrapper around {@link android.util.Log} that keeps SmartTube's printf-style calls. */
public final class Log {
    private static final SparseArray<String> LEVELS = new SparseArray<>();

    static {
        LEVELS.put(android.util.Log.DEBUG, "D");
        LEVELS.put(android.util.Log.WARN, "W");
        LEVELS.put(android.util.Log.ERROR, "E");
    }

    private Log() {
    }

    public static void d(String tag, String format, Object... args) {
        android.util.Log.d(tag, format(format, args));
    }

    public static void w(String tag, String format, Object... args) {
        android.util.Log.w(tag, format(format, args));
    }

    public static void e(String tag, String format, Object... args) {
        android.util.Log.e(tag, format(format, args));
    }

    private static String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return String.format(Locale.US, message, args);
        } catch (RuntimeException e) {
            return message;
        }
    }
}
