package com.lovegaoshi.kotlinaudio.player.components

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.lovegaoshi.kotlinaudio.processors.FFTAudioProcessor
import timber.log.Timber

@UnstableApi
class APMRenderersFactory(
    context: Context, mFFTListener: FFTAudioProcessor.FFTListener? = null) : DefaultRenderersFactory(context) {

    val mFFTAudioProcessor = FFTAudioProcessor()
    init {
        mFFTAudioProcessor.listener = mFFTListener
    }


    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink? {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(mFFTAudioProcessor))
            .build()
    }

}