package com.doublesymmetry.trackplayer.sabr.parser.misc;

import androidx.annotation.Nullable;

/** Factory counterpart of {@link UrlQueryString}. */
public final class UrlQueryStringFactory {
    private UrlQueryStringFactory() {
    }

    public static UrlQueryString parse(@Nullable String url) {
        return UrlQueryString.parse(url);
    }
}
