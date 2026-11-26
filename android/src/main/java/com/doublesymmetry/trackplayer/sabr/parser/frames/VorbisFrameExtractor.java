package com.doublesymmetry.trackplayer.sabr.parser.frames;

import androidx.media3.extractor.TrackOutput;

public class VorbisFrameExtractor extends BaseFrameExtractor {
    public VorbisFrameExtractor(TrackOutput trackOutput, long startTimeUs, long frameDurationUs) {
        super(trackOutput, startTimeUs, frameDurationUs);
    }

    @Override
    protected int findNextFrameStart(int from) {
        // Vorbis is stored in Ogg pages, each page starts with "OggS"
        for (int i = from; i < buffer.limit() - 3; i++) {
            if (buffer.getData()[i] == 'O' && buffer.getData()[i+1] == 'g' &&
                    buffer.getData()[i+2] == 'g' && buffer.getData()[i+3] == 'S') {
                return i;
            }
        }
        return -1;
    }
}
