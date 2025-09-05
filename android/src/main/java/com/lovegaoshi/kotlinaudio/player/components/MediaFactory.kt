package com.lovegaoshi.kotlinaudio.player.components

import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.smoothstreaming.DefaultSsChunkSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import com.lovegaoshi.kotlinaudio.utils.isUriLocalFile
import androidx.core.net.toUri
import com.lovegaoshi.kotlinaudio.mediasource.sabr.DefaultSabrChunkSource
import com.lovegaoshi.kotlinaudio.mediasource.sabr.SabrMediaSource


@OptIn(UnstableApi::class)
class MediaFactory (
    private val context: Context,
    private val cache: SimpleCache?
) : MediaSource.Factory{

    private val mediaFactory = DefaultMediaSourceFactory(context)

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        return mediaFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        return mediaFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
    }

    override fun getSupportedTypes(): IntArray {
        return mediaFactory.supportedTypes
    }

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {

        val userAgent = mediaItem.mediaMetadata.extras?.getString("user-agent")
        val headers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaItem.mediaMetadata.extras?.getSerializable("headers", HashMap::class.java)
        } else {
            mediaItem.mediaMetadata.extras?.getSerializable("headers")
        }
        val resourceId = mediaItem.mediaMetadata.extras?.getInt("resource-id")
        // HACK: why are these capitalized?
        val resourceType = mediaItem.mediaMetadata.extras?.getString("type")?.lowercase()
        val uri = mediaItem.mediaMetadata.extras?.getString("uri")!!.toUri()
        val factory: DataSource.Factory = when {
            resourceId != 0 && resourceId != null -> {
                val raw = RawResourceDataSource(context)
                raw.open(DataSpec(uri))
                DataSource.Factory { raw }
            }
            isUriLocalFile(uri) -> {
                DefaultDataSource.Factory(context)
            }
            else -> {
                val tempFactory = DefaultHttpDataSource.Factory().apply {
                    setUserAgent(userAgent)
                    setAllowCrossProtocolRedirects(true)

                    headers?.let {
                        setDefaultRequestProperties(it as HashMap<String, String>)
                    }
                }

                enableCaching(tempFactory)
            }
        }

        return when (resourceType) {
            "dash" -> createDashSource(mediaItem, factory)
            "hls" -> createHlsSource(mediaItem, factory)
            "smoothstreaming" -> createSsSource(mediaItem, factory)
            "sabr" -> createSabrSource(mediaItem, factory)
            else -> createProgressiveSource(mediaItem, factory)
        }
    }

    private fun createDashSource(mediaItem: MediaItem, factory: DataSource.Factory): MediaSource {
        return DashMediaSource.Factory(DefaultDashChunkSource.Factory(factory), factory)
            .createMediaSource(mediaItem)
    }

    private fun createHlsSource(mediaItem: MediaItem, factory: DataSource.Factory): MediaSource {
        return HlsMediaSource.Factory(factory)
            .createMediaSource(mediaItem)
    }

    private fun createSsSource(mediaItem: MediaItem, factory: DataSource.Factory): MediaSource {
        return SsMediaSource.Factory(DefaultSsChunkSource.Factory(factory), factory)
            .createMediaSource(mediaItem)
    }

    private fun createSabrSource(mediaItem: MediaItem, factory: DataSource.Factory): MediaSource {
        return SabrMediaSource.Factory(
            DefaultSabrChunkSource.Factory(factory), factory
        ).createMediaSource(mediaItem.mediaMetadata.extras?.getString("uri")!!.toUri())
    }

    private fun createProgressiveSource(
        mediaItem: MediaItem,
        factory: DataSource.Factory
    ): ProgressiveMediaSource {
        return ProgressiveMediaSource.Factory(
            factory, DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
        )
            .createMediaSource(mediaItem)
    }

    private fun enableCaching(factory: DataSource.Factory): DataSource.Factory {
        return if (cache == null) {
            factory
        } else {
            CacheDataSource.Factory().apply {
                setCache(cache!!)
                setUpstreamDataSourceFactory(factory)
                setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            }
        }
    }
}