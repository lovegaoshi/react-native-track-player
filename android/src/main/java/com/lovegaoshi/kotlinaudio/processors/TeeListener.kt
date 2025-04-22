package com.lovegaoshi.kotlinaudio.processors

import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor.AudioBufferSink
import com.lovegaoshi.kotlinaudio.utils.FFT
import java.nio.ByteBuffer


@UnstableApi
class TeeListener(val sampleRate: Int) : AudioBufferSink {
    var sampleRateHz = 0
    var channelCount = 1
    var encoding = 0
    private var fft: FFT? = null

    init {
        fft = FFT( sampleRate)
    }

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        this.encoding = encoding
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val doubleBuffer = buffer.asDoubleBuffer()
        val data = DoubleArray(doubleBuffer.limit())
        doubleBuffer.get(data)
        val numFrames: Int = data.size / channelCount
        val rawData = DoubleArray(numFrames)
        Log.d("APMFFT", "buffer handling size: $numFrames, $sampleRateHz, $channelCount")
        //took average of all channel data
        for (i in 0..<numFrames) {
            var sum = 0.0
            for (ch in 0..<channelCount) {
                sum += (data[channelCount * i + ch] + 32768)
            }
            rawData[i] = (sum / channelCount)
        }
    }
}