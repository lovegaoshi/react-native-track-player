package com.doublesymmetry.trackplayer.sabr.parser.adapter;

import androidx.annotation.Nullable;
import androidx.media3.common.Format;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;
import androidx.media3.extractor.mp4.Track;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.common.util.TimestampAdjuster;
import com.doublesymmetry.trackplayer.sabr.parser.SabrStream;
import com.doublesymmetry.trackplayer.sabr.parser.misc.SabrExtractorInput;
import com.doublesymmetry.trackplayer.sabr.parser.misc.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Feeds SABR media parts into Media3's fragmented MP4 extractor. */
public class SabrFragmentedMp4Adapter extends FragmentedMp4Extractor {
    private static final String TAG = SabrFragmentedMp4Adapter.class.getSimpleName();
    private final SabrExtractorInput extractorInput;

    public SabrFragmentedMp4Adapter(SabrStream sabrStream) {
        this(/* flags= */ 0, sabrStream);
    }

    public SabrFragmentedMp4Adapter(int flags, SabrStream sabrStream) {
        this(flags, null, null, Collections.emptyList(), null, sabrStream);
    }

    public SabrFragmentedMp4Adapter(
            int flags,
            @Nullable TimestampAdjuster timestampAdjuster,
            @Nullable Track sideloadedTrack,
            List<Format> closedCaptionFormats,
            @Nullable TrackOutput additionalEmsgTrackOutput,
            SabrStream sabrStream) {
        super(
                SubtitleParser.Factory.UNSUPPORTED,
                flags,
                timestampAdjuster,
                sideloadedTrack,
                closedCaptionFormats,
                additionalEmsgTrackOutput);
        this.extractorInput = new SabrExtractorInput(sabrStream);
    }

    @Override
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        int result = RESULT_END_OF_INPUT;

        try {
            extractorInput.init(input);
            result = super.read(extractorInput, seekPosition);
        } finally {
            if (result != RESULT_CONTINUE) {
                Log.e(TAG, "Mp4Adapter: disposing, result=%s", result);
                extractorInput.dispose();
            }
        }

        return result;
    }
}
