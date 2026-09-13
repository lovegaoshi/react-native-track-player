package com.doublesymmetry.trackplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.Util.isBitmapFactorySupportedMimeType
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.lovegaoshi.kotlinaudio.utils.getEmbeddedBitmap
import com.google.common.util.concurrent.ListenableFuture
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.guava.future
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get

// https://github.com/androidx/media/issues/121

@UnstableApi
class CoilBitmapLoader @Inject constructor(
    private val context: Context
) : BitmapLoader {

    companion object {
        /** Do not crop to square (default). */
        const val NO_CROP_SQUARE = 0
        /** Follow the automatic shouldCropToSquare edge-colour analysis. */
        const val IGNORED_CROP_SQUARE = 1
        /** Always crop to square. */
        const val ALWAYS_CROP_SQUARE = 2
    }

    /**
     * Crop-square mode:
     *  - DEFAULT_CROP_SQUARE (0) - no crop
     *  - IGNORED_CROP_SQUARE (1) - delegate to shouldCropToSquare()
     *  - ALWAYS_CROP_SQUARE  (2) - always crop
     *
     * Samsung devices always crop regardless of this value.
     */
    var cropSquare: Int = NO_CROP_SQUARE
        set(value) {
            field = when (value) {
                NO_CROP_SQUARE, IGNORED_CROP_SQUARE, ALWAYS_CROP_SQUARE -> value
                else -> NO_CROP_SQUARE
            }
        }

    private val scope = MainScope()
    private val imageLoader = ImageLoader(context)

    override fun supportsMimeType(mimeType: String): Boolean =
        isBitmapFactorySupportedMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        return scope.future { bitmap ?: throw IOException("Unable to decode bitmap") }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future {
        var bitmap: Bitmap? = null
        val parsedUri = uri.toString()
        if (parsedUri.startsWith("file://")) {
            bitmap = getEmbeddedBitmap(parsedUri.substring(7))
        } else {
            val imgrequest = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
            // HACK: header implementation should be done via parsed data from uri
            val response = imageLoader.execute(imgrequest.build())
            bitmap = (response.drawable as? BitmapDrawable)?.bitmap
        }
        if (bitmap == null) {
            bitmap = createBitmap(1, 1, Bitmap.Config.RGB_565)
        }
        val doCrop = Build.MANUFACTURER == "samsung" ||
            cropSquare == ALWAYS_CROP_SQUARE ||
            (cropSquare == IGNORED_CROP_SQUARE && shouldCropToSquare(bitmap))
        if (doCrop) {
            bitmap = CropSquareTransformation().transform(bitmap, Size.ORIGINAL)
        }
        bitmap
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun shouldCropToSquare(bitmap: Bitmap): Boolean {
        return try {
            val source = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
            } else {
                bitmap
            }

            val width = source.width
            val height = source.height
            if (width <= height) return false

            val pillarWidth = (width - height) / 2
            if (pillarWidth < 8) return false

            val checkWidth = maxOf(1, (pillarWidth * 0.8).toInt())
            val colorTolerance = 20

            val refColor = source[0, height / 2]
            val refAlpha = Color.alpha(refColor)
            val refRed = Color.red(refColor)
            val refGreen = Color.green(refColor)
            val refBlue = Color.blue(refColor)

            fun isMatchingColor(color: Int): Boolean {
                val alpha = Color.alpha(color)
                if (refAlpha < 10 && alpha < 10) return true
                if (abs(refAlpha - alpha) > colorTolerance) return false
                val rDiff = abs(refRed - Color.red(color))
                val gDiff = abs(refGreen - Color.green(color))
                val bDiff = abs(refBlue - Color.blue(color))
                return rDiff <= colorTolerance && gDiff <= colorTolerance && bDiff <= colorTolerance
            }

            val yPositions = intArrayOf(
                height / 6,
                height / 3,
                height / 2,
                (height * 2) / 3,
                (height * 5) / 6
            )

            val xFractions = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)

            for (y in yPositions) {
                for (f in xFractions) {
                    val leftX = (checkWidth * f).toInt().coerceIn(0, width - 1)
                    val rightX = (width - 1 - (checkWidth * f).toInt()).coerceIn(0, width - 1)
                    if (!isMatchingColor(source[leftX, y])) return false
                    if (!isMatchingColor(source[rightX, y])) return false
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
