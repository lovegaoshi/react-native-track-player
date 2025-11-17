package com.lovegaoshi.media3.sabr

import video_streaming.ClientAbrStateOuterClass.ClientAbrState
import video_streaming.StreamerContextOuterClass.StreamerContext.ClientInfo
import video_streaming.StreamerContextOuterClass.StreamerContext
import video_streaming.VideoPlaybackAbrRequestOuterClass

fun buildClientInfo(osName: String, osVersion: String, clientName: Int, clientVersion: String): ClientInfo {
    return ClientInfo.newBuilder()
        .setOsName(osName)
        .setOsVersion(osVersion)
        .setClientName(clientName)
        .setClientVersion(clientVersion)
        .build()
}

fun buildStreamerContext(pot: String, clientInfo: ClientInfo, playbackCookie: String?): StreamerContext {
    // https://github.com/LuanRT/googlevideo/blob/d2fa40d761034a286cf60ee033653307a1295b0c/src/core/SabrStreamingAdapter.ts#L331
    val streamerContext = StreamerContext.newBuilder()
        .setPoToken(base64ToU8(pot))
        .setClientInfo(clientInfo)
    // googlevideo's streamerContext encodes sabrContexts and unsentSabrContexts as [50] and [0] but not in the java version
    if (playbackCookie != null) {
        streamerContext.setPlaybackCookie(base64ToU8(playbackCookie))
    }
    return streamerContext.build()
}

fun buildClientAbrState(
    playbackRate: Float, playerTimeMs: Long, bandWidthEstimate: Long = 0,
    drcEnabled: Boolean = false, trackTypesBitField: Boolean = false, audioTrackId: String): ClientAbrState {
    return ClientAbrState.newBuilder()
        .setPlaybackRate(playbackRate)
        .setPlayerTimeMs(playerTimeMs)
        .setClientViewportIsFlexible(false)
        .setBandwidthEstimate(bandWidthEstimate)
        .setDrcEnabled(drcEnabled)
        // currentFormat.width ? VIDEO_ONLY = 2 : AUDIO_ONLY = 1
        .setEnabledTrackTypesBitfield(if (trackTypesBitField) 2 else 1)
        .setAudioTrackId(audioTrackId)
        .build()
}

fun buildVideoPlaybackAbrRequest(streamerContext: StreamerContext, clientAbrState: ClientAbrState): ByteArray {
    val playbackAbrRequest = VideoPlaybackAbrRequestOuterClass.VideoPlaybackAbrRequest.newBuilder()
        .setStreamerContext(streamerContext)
        .setClientAbrState(clientAbrState)
    return playbackAbrRequest.build().toByteArray()
}