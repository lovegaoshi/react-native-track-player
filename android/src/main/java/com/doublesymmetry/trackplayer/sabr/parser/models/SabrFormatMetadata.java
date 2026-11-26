package com.doublesymmetry.trackplayer.sabr.parser.models;

import androidx.media3.common.Metadata;

import java.util.Objects;

/** Carries YouTube's SABR format discriminator through ExoPlayer track selection. */
public final class SabrFormatMetadata implements Metadata.Entry {
    public final String xTags;
    public final String audioTrackId;
    /** Media3 removed {@code Format.lastModified}, so it is carried here instead. */
    public final long lastModified;

    public SabrFormatMetadata(String xTags, String audioTrackId) {
        this(xTags, audioTrackId, 0L);
    }

    public SabrFormatMetadata(String xTags, String audioTrackId, long lastModified) {
        this.xTags = xTags;
        this.audioTrackId = audioTrackId;
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof SabrFormatMetadata
                && Objects.equals(xTags, ((SabrFormatMetadata) obj).xTags)
                && Objects.equals(audioTrackId, ((SabrFormatMetadata) obj).audioTrackId)
                && lastModified == ((SabrFormatMetadata) obj).lastModified;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xTags, audioTrackId, lastModified);
    }
}
