package com.doublesymmetry.trackplayer.sabr.parser.misc;

import android.net.Uri;

import androidx.annotation.Nullable;

/** Minimal URL query-string reader used by the SABR redirect handling. */
public final class UrlQueryString {
    private final Uri uri;

    private UrlQueryString(Uri uri) {
        this.uri = uri;
    }

    public @Nullable String get(String key) {
        return uri.getQueryParameter(key);
    }

    public static UrlQueryString parse(@Nullable String url) {
        return new UrlQueryString(Uri.parse(url == null ? "" : url));
    }
}
