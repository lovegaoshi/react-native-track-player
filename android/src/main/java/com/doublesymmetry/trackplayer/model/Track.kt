package com.doublesymmetry.trackplayer.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.lovegaoshi.kotlinaudio.models.AudioItemOptions
import com.lovegaoshi.kotlinaudio.models.MediaType
import com.lovegaoshi.kotlinaudio.models.SabrClientInfo
import com.lovegaoshi.kotlinaudio.models.SabrFormatInfo
import com.lovegaoshi.kotlinaudio.models.SabrOptions
import com.doublesymmetry.trackplayer.utils.BundleUtils

/**
 * @author Milen Pivchev @mpivchev
 */
@OptIn(UnstableApi::class)
class Track
    (context: Context, bundle: Bundle, ratingType: Int) : TrackMetadata() {
    var uri: Uri? = null
    var resourceId: Int?
    var type = MediaType.DEFAULT
    var setRange = false
    var contentType: String?
    var userAgent: String?
    var originalItem: Bundle?
    var headers: HashMap<String, String>? = null
    val queueId: Long

    override fun setMetadata(context: Context, bundle: Bundle?, ratingType: Int) {
        super.setMetadata(context, bundle, ratingType)
        if (originalItem != null && originalItem != bundle) originalItem!!.putAll(bundle)
    }

    fun toAudioItem(): TrackAudioItem {
        return TrackAudioItem(this, type, uri.toString(), artist, title, album, artwork.toString(), duration,
                AudioItemOptions(
                    headers = headers,
                    userAgent = userAgent,
                    resourceId = resourceId,
                    setRange = setRange,
                    sabr = originalItem?.let(::parseSabrOptions),
                ), mediaId)
    }

    init {
        resourceId = BundleUtils.getRawResourceId(context, bundle, "url")
        uri = if (resourceId == 0) {
            resourceId = null
            BundleUtils.getUri(context, bundle, "url")
        } else {
            // RawResourceDataSource.buildRawResourceUri(resourceId!!)
            Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).path(resourceId!!.toString()).build()
        }
        val trackType = bundle.getString("type", "default")
        for (t in MediaType.entries) {
            if (t.name.equals(trackType, ignoreCase = true)) {
                type = t
                break
            }
        }
        setRange = bundle.getBoolean("setRange")
        contentType = bundle.getString("contentType")
        userAgent = bundle.getString("userAgent")
        val httpHeaders = bundle.getBundle("headers")
        if (httpHeaders != null) {
            headers = HashMap()
            for (header in httpHeaders.keySet()) {
                headers!![header] = httpHeaders.getString(header)!!
            }
        }
        setMetadata(context, bundle, ratingType)
        queueId = System.currentTimeMillis()
        originalItem = bundle
    }
}

private fun Any?.asStringKeyMap(): Map<String, Any?>? = when (this) {
    is Bundle -> keySet().associateWith { get(it) }
    is Map<*, *> -> buildMap {
        this@asStringKeyMap.forEach { (key, value) ->
            if (key is String) put(key, value)
        }
    }
    else -> null
}

private fun Map<String, Any?>.string(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    (this[key] as? String)?.takeIf { it.isNotEmpty() }
}

private fun Map<String, Any?>.number(vararg keys: String): Number? = keys.firstNotNullOfOrNull { key ->
    this[key] as? Number
}

private fun Map<String, Any?>.bool(key: String): Boolean = this[key] as? Boolean ?: false

private fun Map<String, Any?>.requiredString(vararg keys: String): String =
    string(*keys) ?: throw IllegalArgumentException("sabr.${keys.first()} is required")

private fun parseSabrFormat(value: Any?, index: Int): SabrFormatInfo? {
    val map = value.asStringKeyMap()
        ?: throw IllegalArgumentException("sabr.formats[$index] must be an object")
    val itag = map.number("itag")
        ?: throw IllegalArgumentException("sabr.formats[$index].itag is required")
    val mimeType = map.string("mimeType", "mime_type")
        ?: throw IllegalArgumentException("sabr.formats[$index].mimeType is required")

    return SabrFormatInfo(
        itag = itag.toInt(),
        mimeType = mimeType,
        codecs = map.string("codecs"),
        bitrate = map.number("bitrate")?.toInt() ?: -1,
        width = map.number("width")?.toInt() ?: -1,
        height = map.number("height")?.toInt() ?: -1,
        fps = map.number("fps", "frameRate")?.toFloat() ?: -1f,
        language = map.string("language"),
        qualityLabel = map.string("qualityLabel", "quality_label"),
        drc = map.bool("drc"),
        lmt = map.number("lmt", "lastModified")?.toLong() ?: 0L,
        xtags = map.string("xtags"),
        audioTrackId = map.string("audioTrackId", "audio_track_id"),
    )
}

private fun parseSabrOptions(bundle: Bundle): SabrOptions? {
    val map = bundle.get("sabr").asStringKeyMap() ?: return null

    val clientMap = (map["client"] ?: map["clientInfo"]).asStringKeyMap()
        ?: throw IllegalArgumentException("sabr.client is required")

    val formats = (map["formats"] as? List<*>)?.mapIndexedNotNull { index, value ->
        parseSabrFormat(value, index)
    } ?: emptyList()
    val subtitles = (map["subtitles"] as? List<*>)?.mapIndexedNotNull { index, value ->
        parseSabrFormat(value, index)
    } ?: emptyList()

    return SabrOptions(
        poToken = map.requiredString("poToken", "po_token"),
        videoPlaybackUstreamerConfig = map.requiredString(
            "videoPlaybackUstreamerConfig",
            "video_playback_ustreamer_config",
        ),
        videoId = map.requiredString("videoId", "video_id"),
        client = SabrClientInfo(
            name = clientMap.string("name", "clientName") ?: "ANDROID",
            version = clientMap.requiredString("version", "clientVersion"),
            osName = clientMap.requiredString("osName", "os_name"),
            osVersion = clientMap.requiredString("osVersion", "os_version"),
        ),
        formats = formats,
        visitorCookie = map.string("visitorCookie", "visitor_cookie"),
        durationMs = map.number("durationMs", "duration_ms")?.toLong(),
        subtitles = subtitles,
    )
}
