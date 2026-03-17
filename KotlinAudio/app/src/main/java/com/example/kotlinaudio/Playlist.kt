package com.example.kotlinaudio

import com.lovegaoshi.kotlinaudio.models.audioItem2MediaItem
import com.lovegaoshi.kotlinaudio.models.AudioItemOptions
import com.lovegaoshi.kotlinaudio.models.DefaultAudioItem
import com.lovegaoshi.kotlinaudio.models.MediaType

class Playlist {
    val playlist = listOf(
        audioItem2MediaItem(DefaultAudioItem(
            audioUrl = "https://doublesymmetry.github.io/react-native-track-player/example/Longing.mp3",
            title = "Longing",
            artist = "David Chavez",
            artwork = "https://doublesymmetry.github.io/react-native-track-player/example/Longing.jpeg",
            options = AudioItemOptions(
                userAgent = "myuseragent",
                headers = hashMapOf("some-header" to "some-result")
            )
        )),
        audioItem2MediaItem(DefaultAudioItem(
            audioUrl = "https://doublesymmetry.github.io/react-native-track-player/example/Soul%20Searching.mp3",
            title = "LSoul Searching (Demo)",
            artist = "David Chavez",
            artwork = "https://doublesymmetry.github.io/react-native-track-player/example/Soul%20Searching.jpeg"
        )),
        audioItem2MediaItem(DefaultAudioItem(
            audioUrl = "https://doublesymmetry.github.io/react-native-track-player/example/hls/whip/playlist.m3u8",
            title = "Whip",
            artwork = "https://doublesymmetry.github.io/react-native-track-player/example/hls/whip/whip.jpeg",
            type = MediaType.HLS

        )),
            audioItem2MediaItem(DefaultAudioItem(
                audioUrl = "https://ais-sa5.cdnstream1.com/b75154_128mp3",
            title = "Smooth Jazz 24/7",
            artist = "David Chavez",
            artwork = "https://doublesymmetry.github.io/react-native-track-player/example/smooth-jazz-24-7.jpeg"
            )),)
}