package com.lovegaoshi.media3.sabr

import video_streaming.StreamerContextOuterClass.StreamerContext.ClientInfo
import video_streaming.StreamerContextOuterClass.StreamerContext

fun buildStreamerContext(pot: String): ByteArray? {
    // https://github.com/LuanRT/googlevideo/blob/d2fa40d761034a286cf60ee033653307a1295b0c/src/core/SabrStreamingAdapter.ts#L331
    val clientInfo = ClientInfo.newBuilder()
    return StreamerContext.newBuilder()
        .setPoToken(base64ToU8(pot))
        .setClientInfo(clientInfo)
        .build()
        .toByteArray()
}

fun handleSabrRequest(requestNumber: Number) {

}