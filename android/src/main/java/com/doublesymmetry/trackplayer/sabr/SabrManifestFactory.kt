package com.doublesymmetry.trackplayer.sabr

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.doublesymmetry.trackplayer.sabr.manifest.AdaptationSet
import com.doublesymmetry.trackplayer.sabr.manifest.Period
import com.doublesymmetry.trackplayer.sabr.manifest.Representation
import com.doublesymmetry.trackplayer.sabr.manifest.SabrManifest
import com.doublesymmetry.trackplayer.sabr.manifest.SegmentBase
import com.doublesymmetry.trackplayer.sabr.parser.models.SabrFormatMetadata
import com.doublesymmetry.trackplayer.sabr.protos.videostreaming.StreamerContext.ClientInfo
import com.doublesymmetry.trackplayer.sabr.protos.videostreaming.StreamerContext.ClientName
import com.lovegaoshi.kotlinaudio.models.SabrFormatInfo
import com.lovegaoshi.kotlinaudio.models.SabrOptions
import java.util.Locale

/** Builds a sideloaded [SabrManifest] from the JS-facing [SabrOptions]. */
@OptIn(UnstableApi::class)
object SabrManifestFactory {
    @JvmStatic
    fun create(
        options: SabrOptions,
        serverAbrStreamingUrl: String,
        fallbackDurationMs: Long? = null,
    ): SabrManifest {
        require(serverAbrStreamingUrl.isNotBlank()) { "sabr track url is required" }
        require(options.poToken.isNotBlank()) { "sabr.poToken is required" }
        require(options.videoPlaybackUstreamerConfig.isNotBlank()) {
            "sabr.videoPlaybackUstreamerConfig is required"
        }
        require(options.videoId.isNotBlank()) { "sabr.videoId is required" }

        val allFormats = options.formats + options.subtitles
        require(allFormats.isNotEmpty()) { "sabr.formats must not be empty" }
        require(allFormats.any { trackTypeOf(sampleMimeTypeOf(it)) != C.TRACK_TYPE_UNKNOWN }) {
            "sabr.formats must contain at least one audio or video format"
        }

        val durationMs = options.durationMs ?: fallbackDurationMs
        require(durationMs != null && durationMs > 0) {
            "SABR requires a duration. Set track.duration or sabr.durationMs."
        }

        val clientName = try {
            ClientName.valueOf(options.client.name.trim().uppercase(Locale.ROOT))
        } catch (_: IllegalArgumentException) {
            val valid = ClientName.values().filter { it != ClientName.UNRECOGNIZED }.joinToString { it.name }
            throw IllegalArgumentException("Unknown sabr.client.name '${options.client.name}'. Expected one of: $valid")
        }
        val clientInfo = ClientInfo.newBuilder()
            .setClientName(clientName)
            .setClientVersion(options.client.version)
            .setOsName(options.client.osName)
            .setOsVersion(options.client.osVersion)
            .build()

        val representationsByType = LinkedHashMap<Int, MutableList<Representation>>()
        allFormats.forEach { formatInfo ->
            val format = buildFormat(formatInfo)
            val trackType = trackTypeOf(format.sampleMimeType)
            require(trackType != C.TRACK_TYPE_UNKNOWN) {
                "Unsupported SABR format ${formatInfo.itag}: ${formatInfo.mimeType}"
            }
            val representation = Representation.newInstance(
                /* revisionId= */ Representation.REVISION_ID_DEFAULT,
                /* format= */ format,
                /* baseUrl= */ serverAbrStreamingUrl,
                /* segmentBase= */ SegmentBase.SingleSegmentBase(),
            )
            representationsByType.getOrPut(trackType) { mutableListOf() }.add(representation)
        }

        val adaptationSets = representationsByType.entries.mapIndexed { index, (trackType, representations) ->
            AdaptationSet(index, trackType, representations)
        }
        val periods = listOf(Period("0", 0, adaptationSets))

        return SabrManifest(
            C.TIME_UNSET,
            durationMs,
            C.TIME_UNSET,
            false,
            C.TIME_UNSET,
            C.TIME_UNSET,
            C.TIME_UNSET,
            C.TIME_UNSET,
            periods,
            serverAbrStreamingUrl,
            options.videoPlaybackUstreamerConfig,
            options.poToken,
            options.videoId,
            clientInfo,
            options.visitorCookie,
        )
    }

    private fun buildFormat(info: SabrFormatInfo): Format {
        val containerMimeType = info.mimeType
        val sampleMimeType = sampleMimeTypeOf(info)
        val isText = MimeTypes.isText(containerMimeType) ||
            MimeTypes.isText(sampleMimeType) ||
            containerMimeType == MimeTypes.APPLICATION_TTML ||
            containerMimeType == MimeTypes.APPLICATION_MP4VTT

        val metadata = if (
            !info.xtags.isNullOrEmpty() || !info.audioTrackId.isNullOrEmpty() || info.lmt > 0
        ) {
            Metadata(SabrFormatMetadata(info.xtags, info.audioTrackId, info.lmt))
        } else {
            null
        }

        val builder = Format.Builder()
            .setId(info.itag.toString())
            .setLabel(info.qualityLabel)
            .setContainerMimeType(containerMimeType)
            .setSampleMimeType(sampleMimeType)
            .setCodecs(info.codecs)
            .setAverageBitrate(info.bitrate)
            .setWidth(info.width)
            .setHeight(info.height)
            .setFrameRate(info.fps)
            .setLanguage(info.language)
            .setRoleFlags(if (isText) C.ROLE_FLAG_SUBTITLE else C.ROLE_FLAG_MAIN)
            .setSelectionFlags(if (isText) 0 else C.SELECTION_FLAG_DEFAULT)
            .setMetadata(metadata)

        return builder.build()
    }

    private fun sampleMimeTypeOf(info: SabrFormatInfo): String? {
        val containerMimeType = info.mimeType
        val codecs = info.codecs
        return when {
            MimeTypes.isAudio(containerMimeType) -> MimeTypes.getAudioMediaMimeType(codecs)
            MimeTypes.isVideo(containerMimeType) -> MimeTypes.getVideoMediaMimeType(codecs)
            MimeTypes.isText(containerMimeType) ||
                containerMimeType == MimeTypes.APPLICATION_TTML ||
                containerMimeType == MimeTypes.APPLICATION_MP4VTT -> containerMimeType
            MimeTypes.APPLICATION_MP4 == containerMimeType -> when {
                codecs?.startsWith("stpp") == true -> MimeTypes.APPLICATION_TTML
                codecs?.startsWith("wvtt") == true -> MimeTypes.APPLICATION_MP4VTT
                else -> null
            }
            else -> null
        }
    }

    private fun sampleMimeTypeOf(format: Format): String? = format.sampleMimeType

    private fun trackTypeOf(sampleMimeType: String?): Int = when {
        sampleMimeType == null -> C.TRACK_TYPE_UNKNOWN
        MimeTypes.isVideo(sampleMimeType) -> C.TRACK_TYPE_VIDEO
        MimeTypes.isAudio(sampleMimeType) -> C.TRACK_TYPE_AUDIO
        MimeTypes.isText(sampleMimeType) ||
            sampleMimeType == MimeTypes.APPLICATION_TTML ||
            sampleMimeType == MimeTypes.APPLICATION_MP4VTT -> C.TRACK_TYPE_TEXT
        else -> C.TRACK_TYPE_UNKNOWN
    }
}
