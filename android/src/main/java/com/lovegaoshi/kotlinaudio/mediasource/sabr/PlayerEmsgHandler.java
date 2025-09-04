package com.lovegaoshi.kotlinaudio.mediasource.sabr;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;

import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.source.SampleQueue;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.metadata.MetadataInputBuffer;
import androidx.media3.extractor.metadata.emsg.EventMessage;
import androidx.media3.extractor.metadata.emsg.EventMessageDecoder;
import androidx.media3.common.util.Util;

import com.lovegaoshi.kotlinaudio.mediasource.sabr.manifest.SabrManifest;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

@UnstableApi
public final class PlayerEmsgHandler implements Handler.Callback {
    /** Callbacks for player emsg events encountered during DASH live stream. */
    public interface PlayerEmsgCallback {

        /** Called when the current manifest should be refreshed. */
        void onDashManifestRefreshRequested();

        /**
         * Called when the manifest with the publish time has been expired.
         *
         * @param expiredManifestPublishTimeUs The manifest publish time that has been expired.
         */
        void onDashManifestPublishTimeExpired(long expiredManifestPublishTimeUs);
    }

    private final Allocator allocator;
    private final PlayerEmsgCallback playerEmsgCallback;
    private final EventMessageDecoder decoder;
    private SabrManifest manifest;
    private final Handler handler;
    private final TreeMap<Long, Long> manifestPublishTimeToExpiryTimeUs;

    private long expiredManifestPublishTimeUs;
    private long lastLoadedChunkEndTimeUs;
    private long lastLoadedChunkEndTimeBeforeRefreshUs;
    private boolean isWaitingForManifestRefresh;
    private boolean released;

    /**
     * @param manifest The initial manifest.
     * @param playerEmsgCallback The callback that this event handler can invoke when handling emsg
     *     messages that generate DASH media source events.
     * @param allocator An {@link Allocator} from which allocations can be obtained.
     */
    public PlayerEmsgHandler(
            SabrManifest manifest, PlayerEmsgCallback playerEmsgCallback, Allocator allocator) {
        this.manifest = manifest;
        this.playerEmsgCallback = playerEmsgCallback;
        this.allocator = allocator;

        manifestPublishTimeToExpiryTimeUs = new TreeMap<>();
        handler = Util.createHandlerForCurrentOrMainLooper(/* callback= */ this);
        decoder = new EventMessageDecoder();
    }

    /**
     * Updates the {@link SabrManifest} that this handler works on.
     *
     * @param newManifest The updated manifest.
     */
    public void updateManifest(SabrManifest newManifest) {
        isWaitingForManifestRefresh = false;
        expiredManifestPublishTimeUs = C.TIME_UNSET;
        this.manifest = newManifest;
        removePreviouslyExpiredManifestPublishTimeValues();
    }

    /* package */ boolean maybeRefreshManifestBeforeLoadingNextChunk(long presentationPositionUs) {
        if (!manifest.dynamic) {
            return false;
        }
        if (isWaitingForManifestRefresh) {
            return true;
        }
        boolean manifestRefreshNeeded = false;
        // Find the smallest publishTime (greater than or equal to the current manifest's publish time)
        // that has a corresponding expiry time.
        Map.Entry<Long, Long> expiredEntry = ceilingExpiryEntryForPublishTime(manifest.publishTimeMs);
        if (expiredEntry != null) {
            long expiredPointUs = expiredEntry.getValue();
            if (expiredPointUs < presentationPositionUs) {
                expiredManifestPublishTimeUs = expiredEntry.getKey();
                notifyManifestPublishTimeExpired();
                manifestRefreshNeeded = true;
            }
        }
        if (manifestRefreshNeeded) {
            maybeNotifyDashManifestRefreshNeeded();
        }
        return manifestRefreshNeeded;
    }

    private @Nullable Map.Entry<Long, Long> ceilingExpiryEntryForPublishTime(long publishTimeMs) {
        return manifestPublishTimeToExpiryTimeUs.ceilingEntry(publishTimeMs);
    }

    private void removePreviouslyExpiredManifestPublishTimeValues() {
        for (Iterator<Entry<Long, Long>> it =
             manifestPublishTimeToExpiryTimeUs.entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<Long, Long> entry = it.next();
            long expiredManifestPublishTime = entry.getKey();
            if (expiredManifestPublishTime < manifest.publishTimeMs) {
                it.remove();
            }
        }
    }

    private void notifyManifestPublishTimeExpired() {
        playerEmsgCallback.onDashManifestPublishTimeExpired(expiredManifestPublishTimeUs);
    }

    /** Requests DASH media manifest to be refreshed if necessary. */
    private void maybeNotifyDashManifestRefreshNeeded() {
        if (lastLoadedChunkEndTimeBeforeRefreshUs != C.TIME_UNSET
                && lastLoadedChunkEndTimeBeforeRefreshUs == lastLoadedChunkEndTimeUs) {
            // Already requested manifest refresh.
            return;
        }
        isWaitingForManifestRefresh = true;
        lastLoadedChunkEndTimeBeforeRefreshUs = lastLoadedChunkEndTimeUs;
        playerEmsgCallback.onDashManifestRefreshRequested();
    }

    /** Returns a {@link TrackOutput} that emsg messages could be written to. */
    public PlayerTrackEmsgHandler newPlayerTrackEmsgHandler() {
        return new PlayerTrackEmsgHandler(SampleQueue.createWithoutDrm(allocator));
    }

    /** Release this emsg handler. It should not be reused after this call. */
    public void release() {
        released = true;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (released) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether an event with given schemeIdUri and value is a DASH emsg event targeting the
     * player.
     */
    public static boolean isPlayerEmsgEvent(String schemeIdUri, String value) {
        return "urn:mpeg:sabr:event:2025".equals(schemeIdUri)
                && ("1".equals(value) || "2".equals(value) || "3".equals(value));
    }

    /** Handles emsg messages for a specific track for the player. */
    public final class PlayerTrackEmsgHandler implements TrackOutput {
        private final SampleQueue sampleQueue;
        private final FormatHolder formatHolder;
        private final MetadataInputBuffer buffer;

        public PlayerTrackEmsgHandler(SampleQueue sampleQueue) {
            this.sampleQueue = sampleQueue;

            formatHolder = new FormatHolder();
            buffer = new MetadataInputBuffer();
        }

        @Override
        public void durationUs(long durationUs) {
            TrackOutput.super.durationUs(durationUs);
        }

        @Override
        public void format(Format format) {
            sampleQueue.format(format);
        }

        @Override
        public int sampleData(DataReader input, int length, boolean allowEndOfInput) throws IOException {
            return sampleQueue.sampleData(input, length, allowEndOfInput);
        }

        @Override
        public void sampleData(ParsableByteArray data, int length) {
            sampleQueue.sampleData(data, length);
        }

        // HACK: what is sampleDataPart?
        @Override
        public int sampleData(DataReader input, int length, boolean allowEndOfInput, int sampleDataPart) throws IOException {
            return sampleQueue.sampleData(input, length, allowEndOfInput);
        }

        // HACK: what is sampleDataPart?
        @Override
        public void sampleData(ParsableByteArray data, int length, int sampleDataPart) {
            sampleQueue.sampleData(data, length);
        }

        @Override
        public void sampleMetadata(long timeUs, int flags, int size, int offset, @Nullable CryptoData encryptionData) {
            sampleQueue.sampleMetadata(timeUs, flags, size, offset, encryptionData);
            parseAndDiscardSamples();
        }

        /**
         * For live streaming, check if the DASH manifest is expired before the next segment start time.
         * If it is, the DASH media source will be notified to refresh the manifest.
         *
         * @param presentationPositionUs The next load position in presentation time.
         * @return True if manifest refresh has been requested, false otherwise.
         */
        public boolean maybeRefreshManifestBeforeLoadingNextChunk(long presentationPositionUs) {
            return PlayerEmsgHandler.this.maybeRefreshManifestBeforeLoadingNextChunk(
                    presentationPositionUs);
        }

        /** Release this track emsg handler. It should not be reused after this call. */
        public void release() {
            sampleQueue.reset();
        }

        private void parseAndDiscardSamples() {
            // HACK: none of these seem to do anything besides logging. removing as hasNextSample
            // is now private
            /**
            while (sampleQueue.hasNextSample()) {
                MetadataInputBuffer inputBuffer = dequeueSample();
                if (inputBuffer == null) {
                    continue;
                }
                long eventTimeUs = inputBuffer.timeUs;
                Metadata metadata = decoder.decode(inputBuffer);
                if (metadata == null) {
                    continue;
                }
                EventMessage eventMessage = (EventMessage) metadata.get(0);
                if (isPlayerEmsgEvent(eventMessage.schemeIdUri, eventMessage.value)) {
                    parsePlayerEmsgEvent(eventTimeUs, eventMessage);
                }
            }
             **/
            sampleQueue.discardToRead();
        }

        /**
        private void parsePlayerEmsgEvent(long eventTimeUs, EventMessage eventMessage) {
            // NOP
        }

        @Nullable
        private MetadataInputBuffer dequeueSample() {
            buffer.clear();
            int result = sampleQueue.read(formatHolder, buffer, false, false, 0);
            if (result == C.RESULT_BUFFER_READ) {
                buffer.flip();
                return buffer;
            }
            return null;
        }
        **/
    }

    /** Holds information related to a manifest expiry event. */
    private static final class ManifestExpiryEventInfo {

        public final long eventTimeUs;
        public final long manifestPublishTimeMsInEmsg;

        public ManifestExpiryEventInfo(long eventTimeUs, long manifestPublishTimeMsInEmsg) {
            this.eventTimeUs = eventTimeUs;
            this.manifestPublishTimeMsInEmsg = manifestPublishTimeMsInEmsg;
        }
    }
}
