package com.lovegaoshi.kotlinaudio.utils

import kotlin.math.pow

// i dont understand this
// https://github.com/dzolnai/ExoVisualizer/blob/master/app/src/main/java/com/egeniq/exovisualizer/FFTBandView.kt

// example freq limits:
val DEFAULT_BAND_LIMIT = listOf(
    20, 25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
    800, 1000, 1250, 1600, 2000,
)

fun band2Indices (freqLimits: List<Int>, size: Int): List<Int> {
    val maxRange = freqLimits.last()
    return freqLimits.map { it -> it * size / maxRange }
}

fun calcFFTBand (fft: DoubleArray, sampleSize: Int, freqLimits: List<Int> = DEFAULT_BAND_LIMIT): List<Double> {
    val endIndices = band2Indices(freqLimits, sampleSize / 2)
    val startIndices = listOf(0) + endIndices.subList(0, endIndices.size - 2)

    return startIndices.mapIndexed { index, it ->
        var sum = 0.0
        for (j in it..endIndices[index] - 1 step 2) {
            sum += (fft[j].pow(2.0) + fft[j + 1].pow(2.0))
        }
        sum / (endIndices[index] - it)
    }
}