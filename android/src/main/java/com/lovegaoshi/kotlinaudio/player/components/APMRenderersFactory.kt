package com.lovegaoshi.kotlinaudio.player.components

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.lovegaoshi.kotlinaudio.processors.FFTAudioProcessor
import com.lovegaoshi.kotlinaudio.processors.TeeListener

@UnstableApi
class APMRenderersFactory(
    context: Context, sampleRate: Int = 4096,
    mFFTListener: FFTAudioProcessor.FFTListener? = null) : DefaultRenderersFactory(context) {

    val mFFTAudioProcessor = FFTAudioProcessor(sampleRate)
    init {
        mFFTAudioProcessor.listener = mFFTListener
    }
    val teeProcessor = TeeAudioProcessor(TeeListener(sampleRate))


    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink? {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(teeProcessor))
            .build()
    }

}