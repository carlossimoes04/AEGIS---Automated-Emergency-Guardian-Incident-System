package cmov.grupo16.aegis.feature_scanner.data.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {

    /**
     * Resizes a Bitmap to a maximum dimension of [maxDimension] px,
     * applies [rotationDegrees] if needed, compresses it in JPEG format
     * and returns it as a data URL ready for the NVIDIA NIM.
     */
    fun bitmapToBase64DataUrl(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        maxDimension: Int = 1024,
        jpegQuality: Int = 85
    ): String {
        val resized = resizeIfNeeded(bitmap, maxDimension)
        val upright = rotateIfNeeded(resized, rotationDegrees)

        val outputStream = ByteArrayOutputStream()
        upright.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        // Recycle bitmaps to free up memory
        if (upright !== resized && upright !== bitmap) upright.recycle()
        if (resized !== bitmap) resized.recycle()

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

    private fun rotateIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        if (normalizedRotation == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(normalizedRotation.toFloat())
        }
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}