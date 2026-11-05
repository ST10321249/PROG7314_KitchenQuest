package com.example.kitchenquest.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class PreparedPhoto(
    val bytes: ByteArray,
    val contentType: String
)

interface DishPhotoReader {
    suspend fun read(uri: String): PreparedPhoto?
}

object NoOpDishPhotoReader : DishPhotoReader {
    override suspend fun read(uri: String): PreparedPhoto? = null
}

class AndroidDishPhotoReader(context: Context) : DishPhotoReader {

    private val appContext = context.applicationContext

    override suspend fun read(uri: String): PreparedPhoto? = withContext(Dispatchers.IO) {
        try {
            val bitmap = PhotoDecoding.decodeUri(appContext, Uri.parse(uri), MAX_SIDE) ?: return@withContext null
            val bytes = compress(bitmap)
            Log.d(TAG, "Prepared dish photo of ${bytes.size} bytes")
            if (bytes.size > MAX_BYTES) null else PreparedPhoto(bytes, JPEG)
        } catch (error: Exception) {
            Log.w(TAG, "Could not read the chosen dish photo", error)
            null
        }
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        var quality = START_QUALITY
        var bytes: ByteArray
        do {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            bytes = output.toByteArray()
            quality -= 10
        } while (bytes.size > MAX_BYTES && quality >= MIN_QUALITY)
        return bytes
    }

    private companion object {
        const val TAG = "DishPhotoReader"
        const val JPEG = "image/jpeg"
        const val MAX_SIDE = 1600
        const val MAX_BYTES = 3 * 1024 * 1024
        const val START_QUALITY = 85
        const val MIN_QUALITY = 40
    }
}

object PhotoDecoding {

    fun decodeUri(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val scale = minOf(1f, maxSide.toFloat() / max(width, height))
                decoder.setTargetSize(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    }

    fun decodeBytes(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }
}