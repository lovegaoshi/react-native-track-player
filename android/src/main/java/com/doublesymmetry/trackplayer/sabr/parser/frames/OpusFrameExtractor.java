package com.doublesymmetry.trackplayer.sabr.parser.frames;

import androidx.media3.extractor.TrackOutput;

public class OpusFrameExtractor extends BaseFrameExtractor {
    public OpusFrameExtractor(TrackOutput trackOutput, long startTimeUs, long frameDurationUs) {
        super(trackOutput, startTimeUs, frameDurationUs);
    }

    @Override
    protected int findNextFrameStart(int from) {
        // Opus packets start with TOC byte (0x00..0xFF)
        // Simplified: treat each byte as possible start (or implement exact TOC parsing)
        if (from < buffer.limit()) return from;
        return -1;
    }
}
