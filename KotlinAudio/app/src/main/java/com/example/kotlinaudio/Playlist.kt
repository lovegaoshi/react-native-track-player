package com.example.kotlinaudio

import com.lovegaoshi.kotlinaudio.models.audioItem2MediaItem
import com.lovegaoshi.kotlinaudio.models.AudioItemOptions
import com.lovegaoshi.kotlinaudio.models.DefaultAudioItem
import com.lovegaoshi.kotlinaudio.models.MediaType

class Playlist {
    val playlist = listOf(
        audioItem2MediaItem(DefaultAudioItem(
            audioUrl = "https://rntp.dev/example/Longing.mp3",
            title = "Longing",
            artist = "David Chavez",
            artwork = "https://rntp.dev/example/Longing.jpeg",
            options = AudioItemOptions(
                userAgent = "myuseragent",
                headers = hashMapOf("some-header" to "some-result")
            )
        )),
        audioItem2MediaItem(DefaultAudioItem(
            audioUrl = "https://rntp.dev/example/Soul%20Searching.mp3",
            title = "LSoul Searching (Demo)",
            artist = "David Chavez",
            artwork = "https://rntp.dev/example/Soul%20Searching.jpeg"
        )),
        audioItem2MediaItem(DefaultAudioItem(
            audioUrl = "https://rntp.dev/example/hls/whip/playlist.m3u8",
            title = "Whip",
            artwork = "https://rntp.dev/example/hls/whip/whip.jpeg",
            type = MediaType.HLS
        )),
        audioItem2MediaItem(DefaultAudioItem(
            audioUrl = "https://ais-sa5.cdnstream1.com/b75154_128mp3",
        title = "Smooth Jazz 24/7",
        artist = "David Chavez",
        artwork = "https://rntp.dev/example/smooth-jazz-24-7.jpeg"
        )),
        )
}