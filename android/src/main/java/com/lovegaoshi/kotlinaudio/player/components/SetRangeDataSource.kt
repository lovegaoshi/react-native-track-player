@file:OptIn(UnstableApi::class)

package com.lovegaoshi.kotlinaudio.player.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpUtil
import androidx.media3.datasource.TransferListener
import java.io.IOException
import kotlin.math.min

/**
 * A [DataSource] that replaces ExoPlayer's open ended ranges (`bytes=0-`, `bytes=110000-`) with
 * bounded ones, and transparently opens the next range when the current one has been consumed.
 *
 * The default range length is 1 MiB, which matches ExoPlayer's
 * `ProgressiveMediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES`: progressive playback loads roughly
 * 1 MiB at a time before checking whether it should keep loading.
 *
 * The wrapped source still reports the length of the whole resource to its caller (taken from the
 * `Content-Range` response header), so seeking, buffering and caching keep working as they do with
 * an open ended range.
 */
class SetRangeDataSource internal constructor(
    private val upstreamFactory: DataSource.Factory,
    private val rangeLength: Long = DEFAULT_RANGE_LENGTH,
) : DataSource {

    private val transferListeners = mutableListOf<TransferListener>()

    private var originalSpec: DataSpec? = null
    private var upstream: DataSource? = null
    private var position = 0L
    private var remaining = C.LENGTH_UNSET.toLong()
    private var rangeRemaining = 0L
    private var fullLength = C.LENGTH_UNSET.toLong()

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners += transferListener
        upstream?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        closeQuietly()
        originalSpec = dataSpec
        position = dataSpec.position
        remaining = dataSpec.length
        fullLength = C.LENGTH_UNSET.toLong()
        openNextRange()

        // Never report the length of a single chunk: ExoPlayer would treat it as the whole file.
        return if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            fullLength
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }
        checkNotNull(originalSpec) { "DataSource is not open" }

        var totalRead = 0
        while (totalRead < length && remaining != 0L) {
            if (rangeRemaining == 0L) {
                closeQuietly()
                openNextRange()
            }

            val source = upstream ?: break
            val maxRead = min((length - totalRead).toLong(), rangeRemaining)
            val readLength = if (remaining == C.LENGTH_UNSET.toLong()) {
                maxRead
            } else {
                min(maxRead, remaining)
            }

            val read = source.read(buffer, offset + totalRead, readLength.toInt())
            if (read == C.RESULT_END_OF_INPUT) {
                // The server ended the resource before the bounded range was fulfilled.
                remaining = 0
                break
            }

            totalRead += read
            rangeRemaining -= read
            position += read
            if (remaining != C.LENGTH_UNSET.toLong()) {
                remaining -= read
            }
        }

        return if (totalRead == 0) C.RESULT_END_OF_INPUT else totalRead
    }

    override fun close() {
        closeQuietly()
        originalSpec = null
        remaining = C.LENGTH_UNSET.toLong()
        rangeRemaining = 0
    }

    override fun getUri(): Uri? = upstream?.uri ?: originalSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        upstream?.responseHeaders ?: emptyMap()

    private fun openNextRange() {
        val spec = checkNotNull(originalSpec)
        val length = if (remaining == C.LENGTH_UNSET.toLong()) {
            rangeLength
        } else {
            min(remaining, rangeLength)
        }
        if (length <= 0) {
            return
        }

        val rangedSpec = DataSpec.Builder()
            .setUri(spec.uri)
            .setPosition(position)
            .setLength(length)
            .setKey(spec.key)
            .setFlags(spec.flags)
            .setHttpRequestHeaders(spec.httpRequestHeaders)
            .setHttpMethod(spec.httpMethod)
            .setHttpBody(spec.httpBody)
            .build()

        val source = upstreamFactory.createDataSource()
        transferListeners.forEach(source::addTransferListener)
        val openLength = source.open(rangedSpec)
        upstream = source

        val contentRange = source.responseHeaders.entries
            .firstOrNull { it.key.equals(CONTENT_RANGE_HEADER, ignoreCase = true) }
            ?.value
            ?.firstOrNull()
        // A server may honour the range but cap it (for example at the end of the resource).
        // Trust what it actually returned so the next request starts at the right offset.
        val deliveredLength = HttpUtil.getContentLength(null, contentRange)
        rangeRemaining = if (deliveredLength != C.LENGTH_UNSET.toLong()) deliveredLength else length

        val documentSize = HttpUtil.getDocumentSize(contentRange)
        fullLength = when {
            documentSize != C.LENGTH_UNSET.toLong() -> documentSize - spec.position
            // A 200 means the server ignored the range header and sent the whole resource.
            (source as? HttpDataSource)?.responseCode == 200 &&
                openLength != C.LENGTH_UNSET.toLong() -> openLength
            else -> C.LENGTH_UNSET.toLong()
        }
    }

    private fun closeQuietly() {
        try {
            upstream?.close()
        } catch (_: IOException) {
            // Nothing useful to do while replacing a range or closing the datasource.
        } finally {
            upstream = null
            rangeRemaining = 0
        }
    }

    companion object {
        /**
         * 1 MiB, matching `ProgressiveMediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES`.
         */
        const val DEFAULT_RANGE_LENGTH: Long = 1024L * 1024L

        fun factory(
            upstreamFactory: DataSource.Factory,
            rangeLength: Long = DEFAULT_RANGE_LENGTH,
        ): DataSource.Factory = DataSource.Factory { SetRangeDataSource(upstreamFactory, rangeLength) }
    }
}

private const val CONTENT_RANGE_HEADER = "Content-Range"

/**
 * Builds the HTTP datasource factory used for remote media: a bounded-range [SetRangeDataSource]
 * backed by [DefaultHttpDataSource], with the caller's user agent and request headers applied.
 */
fun setRangeSource(
    userAgent: String?,
    headers: Map<String, String>?,
): DataSource.Factory {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
        setUserAgent(userAgent)
        setAllowCrossProtocolRedirects(true)
        headers?.let { setDefaultRequestProperties(it) }
    }
    return SetRangeDataSource.factory(httpDataSourceFactory)
}
