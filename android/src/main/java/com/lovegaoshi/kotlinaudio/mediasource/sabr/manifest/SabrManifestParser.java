package com.lovegaoshi.kotlinaudio.mediasource.sabr.manifest;

import android.util.Pair;

import androidx.annotation.NonNull;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData.SchemeData;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;

import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;

import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class SabrManifestParser {
    public SabrManifest parse(@NonNull MediaItemFormatInfo formatInfo) {
        return parseSabrManifest(formatInfo);
    }

    private SabrManifest parseSabrManifest(MediaItemFormatInfo formatInfo) {
        long availabilityStartTime = formatInfo.getStartTimeMs();
        long durationMs = getDurationMs(formatInfo);
        long minBufferTimeMs = C.TIME_UNSET;
        long timeShiftBufferDepthMs = C.TIME_UNSET;
        long suggestedPresentationDelayMs = C.TIME_UNSET;
        long publishTimeMs = formatInfo.getStartTimeMs();

        List<Period> periods = new ArrayList<>();
        long nextPeriodStartMs = 0;

        Pair<Period, Long> periodWithDurationMs = parsePeriod(formatInfo, nextPeriodStartMs);
        if (periodWithDurationMs != null) {
            Period period = periodWithDurationMs.first;
            periods.add(period);
        }

        return new SabrManifest(
                availabilityStartTime,
                durationMs,
                minBufferTimeMs,
                timeShiftBufferDepthMs,
                suggestedPresentationDelayMs,
                publishTimeMs,
                periods);
    }

    private static long getDurationMs(MediaItemFormatInfo formatInfo) {
        try {
            return Long.parseLong(formatInfo.getLengthSeconds()) * 1_000;
        } catch (Exception e) {
            return -1_000;
        }
    }

    private Pair<Period, Long> parsePeriod(MediaItemFormatInfo formatInfo, long nextPeriodStartMs) {
        String id = formatInfo.getVideoId();
        long startMs = formatInfo.getStartTimeMs();
        long durationMs = getDurationMs(formatInfo);
        List<AdaptationSet> adaptationSets = new ArrayList<>();

        for (MediaFormat format : formatInfo.getAdaptiveFormats()) {
            adaptationSets.add(parseAdaptationSet(format));
        }

        return Pair.create(new Period(id, startMs, adaptationSets), durationMs);
    }

    private AdaptationSet parseAdaptationSet(MediaFormat format) {
        // parseRepresentation

        // TODO: not implemented
        return null;
    }

    private RepresentationInfo parseRepresentation() {
        // buildFormat

        // TODO: not implemented
        return null;
    }

    protected Format buildFormat(
            String id,
            String containerMimeType,
            int width,
            int height,
            float frameRate,
            int audioChannels,
            int audioSamplingRate,
            int bitrate,
            String language,
            String codecs) {
        String sampleMimeType = getSampleMimeType(containerMimeType, codecs);
        @C.SelectionFlags int selectionFlags = C.SELECTION_FLAG_DEFAULT;
        @C.RoleFlags int roleFlags = C.ROLE_FLAG_MAIN;
        if (sampleMimeType != null) {
            if (MimeTypes.isVideo(sampleMimeType)) {
                return new Format.Builder()
                        .setId(id)
                        .setContainerMimeType(containerMimeType)
                        .setSampleMimeType(sampleMimeType)
                        .setCodecs(codecs)
                        .setAverageBitrate(bitrate)
                        .setWidth(width)
                        .setHeight(height)
                        .setFrameRate(frameRate)
                        .setSelectionFlags(selectionFlags)
                        .setRoleFlags(roleFlags)
                        .build();
            } else if (MimeTypes.isAudio(sampleMimeType)) {
                return new Format.Builder()
                        .setId(id)
                        .setContainerMimeType(containerMimeType)
                        .setSampleMimeType(sampleMimeType)
                        .setCodecs(codecs)
                        .setAverageBitrate(bitrate)
                        .setSampleRate(audioSamplingRate)
                        .setSelectionFlags(selectionFlags)
                        .setRoleFlags(roleFlags)
                        .setLanguage(language)
                        .build();
            } else if (mimeTypeIsRawText(sampleMimeType)) {
                return new Format.Builder()
                        .setId(id)
                        .setContainerMimeType(containerMimeType)
                        .setSampleMimeType(sampleMimeType)
                        .setCodecs(codecs)
                        .setAverageBitrate(bitrate)
                        .setSelectionFlags(selectionFlags)
                        .setRoleFlags(roleFlags)
                        .setLanguage(language)
                        .setAccessibilityChannel(Format.NO_VALUE)
                        .build();
            }
        }
        return new Format.Builder()
                .setId(id)
                .setContainerMimeType(containerMimeType)
                .setSampleMimeType(sampleMimeType)
                .setCodecs(codecs)
                .setAverageBitrate(bitrate)
                .setSelectionFlags(selectionFlags)
                .setRoleFlags(roleFlags)
                .setLanguage(language)
                .build();
    }

    /**
     * Derives a sample mimeType from a container mimeType and codecs attribute.
     *
     * @param containerMimeType The mimeType of the container.
     * @param codecs The codecs attribute.
     * @return The derived sample mimeType, or null if it could not be derived.
     */
    private static String getSampleMimeType(String containerMimeType, String codecs) {
        if (MimeTypes.isAudio(containerMimeType)) {
            return MimeTypes.getAudioMediaMimeType(codecs);
        } else if (MimeTypes.isVideo(containerMimeType)) {
            return MimeTypes.getVideoMediaMimeType(codecs);
        } else if (mimeTypeIsRawText(containerMimeType)) {
            return containerMimeType;
        } else if (MimeTypes.APPLICATION_MP4.equals(containerMimeType)) {
            if (codecs != null) {
                if (codecs.startsWith("stpp")) {
                    return MimeTypes.APPLICATION_TTML;
                } else if (codecs.startsWith("wvtt")) {
                    return MimeTypes.APPLICATION_MP4VTT;
                }
            }
        } else if (MimeTypes.APPLICATION_RAWCC.equals(containerMimeType)) {
            if (codecs != null) {
                if (codecs.contains("cea708")) {
                    return MimeTypes.APPLICATION_CEA708;
                } else if (codecs.contains("eia608") || codecs.contains("cea608")) {
                    return MimeTypes.APPLICATION_CEA608;
                }
            }
            return null;
        }
        return null;
    }

    /**
     * Returns whether a mimeType is a text sample mimeType.
     *
     * @param mimeType The mimeType.
     * @return Whether the mimeType is a text sample mimeType.
     */
    private static boolean mimeTypeIsRawText(String mimeType) {
        return MimeTypes.isText(mimeType)
                || MimeTypes.APPLICATION_TTML.equals(mimeType)
                || MimeTypes.APPLICATION_MP4VTT.equals(mimeType)
                || MimeTypes.APPLICATION_CEA708.equals(mimeType)
                || MimeTypes.APPLICATION_CEA608.equals(mimeType);
    }

    /** A parsed Representation element. */
    protected static final class RepresentationInfo {

        public final Format format;
        public final String baseUrl;
        public final SegmentBase segmentBase;
        public final String drmSchemeType;
        public final ArrayList<SchemeData> drmSchemeDatas;
        public final long revisionId;

        @OptIn(markerClass = UnstableApi.class)
        public RepresentationInfo(Format format, String baseUrl, SegmentBase segmentBase,
                                  String drmSchemeType, ArrayList<SchemeData> drmSchemeDatas,
                                  long revisionId) {
            this.format = format;
            this.baseUrl = baseUrl;
            this.segmentBase = segmentBase;
            this.drmSchemeType = drmSchemeType;
            this.drmSchemeDatas = drmSchemeDatas;
            this.revisionId = revisionId;
        }

    }
}
