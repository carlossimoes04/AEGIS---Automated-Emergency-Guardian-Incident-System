package cmov.grupo16.aegis.feature_scanner.data.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {

    /**
     * Resizes a Bitmap to a maximum dimension of [maxDimension] px,
     * compresses it in JPEG format and returns it as a data URL ready
     * to be sent to the NVIDIA NIM.
     */
    fun bitmapToBase64DataUrl(
        bitmap: Bitmap,
        maxDimension: Int = 1024,
        jpegQuality: Int = 85
    ): String {
        val resized = resizeIfNeeded(bitmap, maxDimension)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    private fun resizeIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largestSide = max(width, height)

        if (largestSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / largestSide.toFloat()
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}