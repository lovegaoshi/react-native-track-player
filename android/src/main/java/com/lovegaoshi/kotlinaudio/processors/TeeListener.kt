// credits: https://github.com/cora32/Media3Player/blob/df06c3d72598b8e7f75fcfd22c5e9da8bc98e0a0/app/src/main/java/io/iskopasi/player_test/PlayerService.kt#L18
package com.lovegaoshi.kotlinaudio.processors

import androidx.media3.common.util.UnstableApi
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.exoplayer.audio.TeeAudioProcessor.AudioBufferSink
import com.lovegaoshi.kotlinaudio.utils.FFT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

val FREQUENCY_BAND_LIMITS = arrayOf(
    20, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
    800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
    12500, 16000, 20000
)

@UnstableApi
class TeeListener(
    val sampleRate: Int,
    val emitter: FFTEmitter? ) : AudioBufferSink {
    private var sampleRateHz = 0
    private var channelCount = 1
    private var encoding = 0
    private var fft = FFT( sampleRate)
    private var fftBuffer = EMPTY_BUFFER
    private val chartData = FloatArray(sampleRate)
    private val src = FloatArray(sampleRate)
    private val frequencyMap = DoubleArray(FREQUENCY_BAND_LIMITS.size)
    private var isProcessing = false

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        this.encoding = encoding
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (isProcessing) return

        isProcessing = true

        // Cannot access buffer in different thread
        fillBuffer(buffer)

        CoroutineScope(Dispatchers.IO).launch {
            processFFT()

            isProcessing = false
        }
        /**
         * code I copied elsewhere
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
         */
    }

    private fun fillBuffer(buffer: ByteBuffer) {
        val limit = buffer.limit()
        val frameCount = (limit - buffer.position()) / (2 * channelCount)
        val singleChannelOutputSize = frameCount * 2

        if (fftBuffer.capacity() < singleChannelOutputSize) {
            fftBuffer =
                ByteBuffer.allocateDirect(singleChannelOutputSize)
                    .order(ByteOrder.nativeOrder())
        } else {
            fftBuffer.clear()
        }

        // Each signal = short (2 bytes)
        // Use average of 2 channels as an input signal
        for (position in buffer.position() until limit step channelCount * 2) {
            var sum = 0

            for (channelIndex in 0 until channelCount) {
                val current = buffer.getShort(position + channelIndex * 2)
                sum += current
            }

            fftBuffer.putShort((sum / channelCount).toShort())
        }
    }
    private fun processFFT() {
        val maxLimit = min(fftBuffer.limit(), sampleRate)

        for (i in 0 until maxLimit step 2) {
            val short = fftBuffer.getShort(i)

            src[i] = short.toFloat()
        }

        val srcCopy = src.clone()
        // DC bin is located at index 0, 1, nyquist at index n-2, n-1
        fft.fft(srcCopy, FloatArray(sampleRate))
        // Fill amplitude data
        // The resulting graph is mirrored, so get only left part
        for (i in 0 until srcCopy.size / 2) {
            val real = srcCopy[i * 2]
            val imaginary = srcCopy[i * 2 + 1]
            val amplitude = sqrt(real.pow(2) + imaginary.pow(2))

            chartData[i] = amplitude
        }

        val size = sampleRate / 2
        var startIndex = 0
        var maxAvgAmplitude = 0f
        var maxRawAmplitude = 0f

        // Group amplitudes by their frequencies
        for ((index, frequency) in FREQUENCY_BAND_LIMITS.withIndex()) {
            val endIndex = floor(frequency / 20000f * size).toInt()

            var accum = 0f
            // Sum amplitudes for current frequency bin
            for (i in startIndex until endIndex) {
                val amplitude = chartData[i]
                accum += amplitude

                // Find max amplitude of all frequencies
                if (amplitude > maxRawAmplitude) {
                    maxRawAmplitude = amplitude
                }
            }

            // Sometimes frequency group range can be 0
            val amplitude = if (endIndex - startIndex == 0)
                0f
            else {
                // Get avg amplitude for frequency bin
                accum / (endIndex - startIndex).toFloat()
            }

            // Fill avg amplitude for each frequency
            frequencyMap[index] = amplitude.toDouble()

            // Get max avg amplitude
            if (amplitude > maxAvgAmplitude) {
                maxAvgAmplitude = amplitude
            }

            startIndex = endIndex
        }
        emitter?.onSpectrumReady(chartData, maxRawAmplitude)
        emitter?.onFrequencyFFTReady(frequencyMap, maxAvgAmplitude)
    }
}

interface FFTEmitter {
    fun onSpectrumReady(spectrum: FloatArray, maxRawAmp: Float)
    fun onFrequencyFFTReady(fft: DoubleArray, maxAvgAmp: Float)
}