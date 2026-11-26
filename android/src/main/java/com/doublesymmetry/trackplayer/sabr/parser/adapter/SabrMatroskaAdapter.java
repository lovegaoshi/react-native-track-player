package com.doublesymmetry.trackplayer.sabr.parser.adapter;

import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.mkv.MatroskaExtractor;
import androidx.media3.extractor.text.SubtitleParser;
import com.doublesymmetry.trackplayer.sabr.parser.SabrStream;
import com.doublesymmetry.trackplayer.sabr.parser.exceptions.SabrPropagationError;
import com.doublesymmetry.trackplayer.sabr.parser.misc.SabrExtractorInput;
import com.doublesymmetry.trackplayer.sabr.parser.misc.Log;

import java.io.IOException;

/**
 * Media3's {@link MatroskaExtractor#read} is final, so SABR's pull-based input is injected by
 * wrapping the extractor instead of subclassing it.
 */
public final class SabrMatroskaAdapter implements Extractor {
    private static final String TAG = SabrMatroskaAdapter.class.getSimpleName();
    private final MatroskaExtractor extractor;
    private final SabrExtractorInput extractorInput;

    public SabrMatroskaAdapter(SabrStream sabrStream) {
        this(/* flags= */ 0, sabrStream);
    }

    public SabrMatroskaAdapter(int flags, SabrStream sabrStream) {
        extractor = new MatroskaExtractor(SubtitleParser.Factory.UNSUPPORTED, flags);
        extractorInput = new SabrExtractorInput(sabrStream);
    }

    @Override
    public boolean sniff(ExtractorInput input) throws IOException {
        return extractor.sniff(input);
    }

    @Override
    public void init(ExtractorOutput output) {
        extractor.init(output);
    }

    @Override
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        int result = RESULT_END_OF_INPUT;
        try {
            extractorInput.init(input);
            result = extractor.read(extractorInput, seekPosition);
        } catch (SabrPropagationError e) {
            Log.e(TAG, "Propagating SABR control signal: %s", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            Log.e(TAG, "User doing seek? %s: %s", e.getClass().getSimpleName(), e.getMessage());
        } finally {
            if (result != RESULT_CONTINUE) {
                Log.e(TAG, "MatroskaAdapter: disposing, result=%s", result);
                extractorInput.dispose();
            }
        }
        return result;
    }

    @Override
    public void seek(long position, long timeUs) {
        extractor.seek(position, timeUs);
    }

    @Override
    public void release() {
        extractor.release();
    }

    @Override
    public Extractor getUnderlyingImplementation() {
        return extractor.getUnderlyingImplementation();
    }
}
