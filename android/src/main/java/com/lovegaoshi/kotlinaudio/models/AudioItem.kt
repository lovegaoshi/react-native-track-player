package com.lovegaoshi.kotlinaudio.models

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lovegaoshi.kotlinaudio.utils.getEmbeddedBitmapArray
import com.lovegaoshi.kotlinaudio.utils.saveMediaCoverToPng
import java.io.Serializable
import java.util.UUID


data class DefaultAudioItem(
    override var audioUrl: String,

    /**
     * Set to [MediaType.DEFAULT] by default.
     */
    override val type: MediaType = MediaType.DEFAULT,

    override var artist: String? = null,
    override var title: String? = null,
    override var albumTitle: String? = null,
    override var artwork: String? = null,
    override val duration: Long? = null,
    override val options: AudioItemOptions? = null,
    override val mediaId: String? = null,
) : AudioItem

class AudioItemHolder(
    var audioItem: AudioItem
) {
    var artworkBitmap: Bitmap? = null
}

interface AudioItem {
    var audioUrl: String
    val type: MediaType
    var artist: String?
    var title: String?
    var albumTitle: String?
    val artwork: String?
    val duration: Long?
    val options: AudioItemOptions?
    val mediaId: String?
}

data class AudioItemOptions(
    val headers: HashMap<String, String>? = null,
    val userAgent: String? = null,
    val resourceId: Int? = null,
    val setRange: Boolean = false,
    val sabr: SabrOptions? = null,
)

/** YouTube client identity used in the SABR protobuf body. It must match the poToken's client. */
data class SabrClientInfo(
    val name: String,
    val version: String,
    val osName: String,
    val osVersion: String,
) : Serializable

/** A SABR representation supplied by the caller, normally derived from a player response. */
data class SabrFormatInfo(
    val itag: Int,
    val mimeType: String,
    val codecs: String? = null,
    val bitrate: Int = -1,
    val width: Int = -1,
    val height: Int = -1,
    val fps: Float = -1f,
    val language: String? = null,
    val qualityLabel: String? = null,
    val drc: Boolean = false,
    val lmt: Long = 0L,
    val xtags: String? = null,
    val audioTrackId: String? = null,
) : Serializable

/**
 * Sideloaded SABR configuration. A URL and poToken alone are not enough: the streaming config,
 * client identity, video id and format metadata are all needed to build the ABR request.
 */
data class SabrOptions(
    val poToken: String,
    val videoPlaybackUstreamerConfig: String,
    val videoId: String,
    val client: SabrClientInfo,
    val formats: List<SabrFormatInfo>,
    val visitorCookie: String? = null,
    val durationMs: Long? = null,
    val subtitles: List<SabrFormatInfo> = emptyList(),
) : Serializable

enum class MediaType(val value: String) {
    /**
     * The default media type. Should be used for streams over HTTP or files
     */
    DEFAULT("default"),

    /**
     * The DASH media type for adaptive streams. Should be used with DASH manifests.
     */
    DASH("dash"),

    /**
     * The HLS media type for adaptive streams. Should be used with HLS playlists.
     */
    HLS("hls"),

    /**
     * The SmoothStreaming media type for adaptive streams. Should be used with SmoothStreaming manifests.
     */
    SMOOTH_STREAMING("smoothstreaming"),

    /**
     * Android-only YouTube SABR streams. The track URL is the signed server ABR streaming URL and
     * the remaining request data is provided through the track's `sabr` option.
     */
    SABR("sabr");
}



fun audioItem2MediaItem(audioItem: AudioItem, context: Context? = null): MediaItem {
    return MediaItem.Builder()
        .setMediaId(audioItem.mediaId ?: UUID.randomUUID().toString())
        .setUri(audioItem.audioUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
            .setTitle(audioItem.title)
            .setArtist(audioItem.artist)
            .setArtworkUri((
                if (context != null && audioItem.audioUrl.startsWith("file://")) {
                    saveMediaCoverToPng(
                        audioItem.audioUrl,
                        context.contentResolver,
                        audioItem.mediaId ?: audioItem.audioUrl
                    )
                        ?: audioItem.artwork
                }
                else audioItem.artwork)?.toUri())
            .setArtworkData(if (audioItem.audioUrl.startsWith("file://")) getEmbeddedBitmapArray(
                audioItem.audioUrl.substring(7)) else null, MediaMetadata.PICTURE_TYPE_MEDIA)
            .setExtras(Bundle().apply {
                audioItem.options?.headers?.let {
                    putSerializable("headers", audioItem.options!!.headers)
                }
                audioItem.options?.userAgent?.let {
                    putString("user-agent", it)
                }
                audioItem.options?.resourceId?.let {
                    putInt("resource-id", it)
                }
                putBoolean("setRange", audioItem.options?.setRange == true)
                audioItem.options?.sabr?.let { putSerializable("sabr", it) }
                putString("type", audioItem.type.toString())
                putString("uri", audioItem.audioUrl)
            }).build())
        .setTag(audioItem)
        .build()
}

fun mediaItem2AudioItem(item: MediaItem?): AudioItem? {
    return item?.localConfiguration?.tag as AudioItem?
}