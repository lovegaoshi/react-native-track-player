package com.lovegaoshi.media3.sabr

import android.util.Base64
import com.google.protobuf.ByteString

@OptIn(ExperimentalUnsignedTypes::class)
fun base64ToU8(base64: String): ByteString {
    // Replace URL-safe Base64 characters
    val standardBase64 = base64.replace('-', '+').replace('_', '/')
    // Pad with '=' if necessary
    val padding = (4 - standardBase64.length % 4) % 4
    val paddedBase64 = standardBase64.padEnd(standardBase64.length + padding, '=')

    // Decode using Android Base64
    val decodedBytes = Base64.decode(paddedBase64, Base64.DEFAULT)
    return ByteString.copyFrom(decodedBytes)
}
