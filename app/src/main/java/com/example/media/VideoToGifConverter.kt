package com.example.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.min

/**
 * Fast, pure-Kotlin Video-to-GIF converter.
 * Extracts video frames at specified FPS/resolution and encodes them into a genuine GIF89a file.
 */
object VideoToGifConverter {

    data class ConversionResult(
        val success: Boolean,
        val filePath: String? = null,
        val uri: Uri? = null,
        val frameCount: Int = 0,
        val fileSizeFormatted: String = "",
        val errorMessage: String? = null
    )

    suspend fun convertVideoToGif(
        context: Context,
        videoUri: Uri,
        startMs: Long,
        endMs: Long,
        fps: Int = 8,
        maxWidth: Int = 400,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ConversionResult = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
        } catch (e: Exception) {
            return@withContext ConversionResult(
                success = false,
                errorMessage = "تعذر قراءة ملف الفيديو: ${e.message}"
            )
        }

        try {
            val totalDurationMs = (endMs - startMs).coerceAtLeast(500L)
            val intervalMs = (1000L / fps).coerceAtLeast(50L)
            val estimatedFrames = (totalDurationMs / intervalMs).toInt().coerceIn(2, 60)
            val frames = mutableListOf<Bitmap>()

            for (i in 0 until estimatedFrames) {
                val timeUs = (startMs + (i * intervalMs)) * 1000L
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)

                if (frame != null) {
                    val scaled = scaleBitmap(frame, maxWidth)
                    frames.add(scaled)
                    if (scaled != frame && !frame.isRecycled) {
                        frame.recycle()
                    }
                }
                val progress = (i.toFloat() / estimatedFrames) * 0.6f
                withContext(Dispatchers.Main) {
                    onProgress(progress, "جاري استخراج الإطارات (${i + 1}/$estimatedFrames)...")
                }
            }

            if (frames.isEmpty()) {
                return@withContext ConversionResult(
                    success = false,
                    errorMessage = "لم يتم العثور على إطارات صالحة للتحويل في النطاق الزمني المحدد."
                )
            }

            withContext(Dispatchers.Main) {
                onProgress(0.7f, "جاري ترميز صورة GIF المتحركة...")
            }

            // Encode frames to GIF
            val gifDelayMs = (1000 / fps).coerceIn(40, 200)
            val gifBytes = encodeGif(frames, gifDelayMs) { encodeProgress ->
                val totalProgress = 0.7f + (encodeProgress * 0.25f)
                onProgress(totalProgress, "جاري حفظ الصورة وتطبيق ضغط الألوان...")
            }

            // Clean up bitmap memory
            frames.forEach { if (!it.isRecycled) it.recycle() }

            // Save GIF to public Pictures/CinemaX_GIFs
            val timestamp = System.currentTimeMillis()
            val fileName = "CinemaX_GIF_$timestamp.gif"
            val savedResult = saveGifFile(context, fileName, gifBytes)

            withContext(Dispatchers.Main) {
                onProgress(1.0f, "اكتمل التحويل بنجاح! 🎉")
            }

            return@withContext ConversionResult(
                success = true,
                filePath = savedResult.first,
                uri = savedResult.second,
                frameCount = frames.size,
                fileSizeFormatted = formatBytes(gifBytes.size.toLong())
            )
        } catch (e: Exception) {
            return@withContext ConversionResult(
                success = false,
                errorMessage = "خطأ أثناء تحويل الفيديو إلى GIF: ${e.message}"
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width <= targetWidth) return bitmap
        val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun saveGifFile(context: Context, fileName: String, bytes: ByteArray): Pair<String?, Uri?> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CinemaX_GIFs")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
                return Pair(uri.toString(), uri)
            }
        }

        // Fallback for older Android or direct file write
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CinemaX_GIFs")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            out.write(bytes)
            out.flush()
        }
        val fileUri = Uri.fromFile(file)
        return Pair(file.absolutePath, fileUri)
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.1f KB", kb)
    }

    /**
     * Standard GIF89a encoder producing valid animated GIFs with quantized global palette.
     */
    private fun encodeGif(
        frames: List<Bitmap>,
        delayMs: Int,
        onProgress: (Float) -> Unit
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val first = frames.first()
        val width = first.width
        val height = first.height

        // 1. GIF Header
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))

        // 2. Logical Screen Descriptor
        writeShort(out, width)
        writeShort(out, height)
        // GCT Flag = 1, Color Resolution = 7 (8 bits), Sort Flag = 0, GCT Size = 7 (256 colors) -> 0xF7
        out.write(0xF7)
        out.write(0) // Background color index
        out.write(0) // Pixel aspect ratio

        // 3. Global Color Table (Build from sample colors)
        val palette = buildColorPalette(frames)
        for (color in palette) {
            out.write((color shr 16) and 0xFF) // R
            out.write((color shr 8) and 0xFF)  // G
            out.write(color and 0xFF)         // B
        }

        // 4. Netscape 2.0 Loop Extension (Infinite looping)
        out.write(0x21) // Extension introducer
        out.write(0xFF) // Application extension
        out.write(11)   // Block size
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)    // Sub-block size
        out.write(1)    // Loop sub-block ID
        writeShort(out, 0) // Loop count 0 = infinite
        out.write(0)    // Block terminator

        // 5. Write each frame
        val delayHundredths = (delayMs / 10).coerceIn(2, 65535)
        for ((idx, frame) in frames.withIndex()) {
            // Graphic Control Extension
            out.write(0x21) // Extension
            out.write(0xF9) // Graphic Control
            out.write(4)    // Block size
            out.write(0x00) // Disposal method 0, no transparency
            writeShort(out, delayHundredths)
            out.write(0)    // Transparent color index (unused)
            out.write(0)    // Terminator

            // Image Descriptor
            out.write(0x2C) // Image separator
            writeShort(out, 0) // Left
            writeShort(out, 0) // Top
            writeShort(out, width)
            writeShort(out, height)
            out.write(0) // No local color table

            // Map pixels to palette indices
            val pixels = IntArray(width * height)
            frame.getPixels(pixels, 0, width, 0, 0, width, height)
            val indexedPixels = ByteArray(pixels.size)
            for (p in pixels.indices) {
                indexedPixels[p] = findClosestColorIndex(pixels[p], palette).toByte()
            }

            // LZW Compression
            compressLZW(out, indexedPixels, 8)

            onProgress((idx + 1).toFloat() / frames.size)
        }

        // 6. GIF Trailer
        out.write(0x3B)

        return out.toByteArray()
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun buildColorPalette(frames: List<Bitmap>): IntArray {
        // High quality standard 6x6x6 color cube (216 colors) + 40 shades of gray
        val palette = IntArray(256)
        var idx = 0
        val steps = intArrayOf(0, 51, 102, 153, 204, 255)
        for (r in steps) {
            for (g in steps) {
                for (b in steps) {
                    if (idx < 216) {
                        palette[idx++] = (r shl 16) or (g shl 8) or b
                    }
                }
            }
        }
        // 40 Grayscale shades for smooth gradients and shadows
        for (i in 0 until 40) {
            val v = (i * 255) / 39
            palette[idx++] = (v shl 16) or (v shl 8) or v
        }
        return palette
    }

    private fun findClosestColorIndex(color: Int, palette: IntArray): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        // Fast quantization index calculation for 6x6x6 cube
        val qr = (r + 25) / 51
        val qg = (g + 25) / 51
        val qb = (b + 25) / 51
        val cubeIndex = (qr.coerceIn(0, 5) * 36) + (qg.coerceIn(0, 5) * 6) + qb.coerceIn(0, 5)
        return min(cubeIndex, 255)
    }

    private fun compressLZW(out: OutputStream, pixels: ByteArray, colorDepth: Int) {
        val initCodeSize = colorDepth.coerceAtLeast(2)
        out.write(initCodeSize)

        val clearCode = 1 shl initCodeSize
        val eoiCode = clearCode + 1
        var codeSize = initCodeSize + 1
        var maxCode = 1 shl codeSize

        val prefixTable = IntArray(5003) { -1 }
        val suffixTable = IntArray(5003)
        val codeTable = IntArray(5003)
        var freeCode = eoiCode + 1

        val bitBuffer = ByteArray(256)
        var bitCount = 0
        var bitBufVal = 0

        fun writeBits(code: Int) {
            bitBufVal = bitBufVal or (code shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                val byteVal = bitBufVal and 0xFF
                bitBufVal = bitBufVal shr 8
                bitCount -= 8

                bitBuffer[bitBuffer[0].toInt() + 1] = byteVal.toByte()
                bitBuffer[0] = (bitBuffer[0] + 1).toByte()

                if (bitBuffer[0].toInt() == 254) {
                    out.write(bitBuffer[0].toInt())
                    out.write(bitBuffer, 1, bitBuffer[0].toInt())
                    bitBuffer[0] = 0
                }
            }
        }

        fun flushBits() {
            while (bitCount > 0) {
                val byteVal = bitBufVal and 0xFF
                bitBufVal = bitBufVal shr 8
                bitCount -= 8

                bitBuffer[bitBuffer[0].toInt() + 1] = byteVal.toByte()
                bitBuffer[0] = (bitBuffer[0] + 1).toByte()

                if (bitBuffer[0].toInt() == 254) {
                    out.write(bitBuffer[0].toInt())
                    out.write(bitBuffer, 1, bitBuffer[0].toInt())
                    bitBuffer[0] = 0
                }
            }
            if (bitBuffer[0].toInt() > 0) {
                out.write(bitBuffer[0].toInt())
                out.write(bitBuffer, 1, bitBuffer[0].toInt())
                bitBuffer[0] = 0
            }
        }

        writeBits(clearCode)

        var prefix = pixels[0].toInt() and 0xFF

        for (i in 1 until pixels.size) {
            val suffix = pixels[i].toInt() and 0xFF
            var hash = ((prefix shl 8) or suffix) % 5003
            var found = false

            while (prefixTable[hash] != -1) {
                if (prefixTable[hash] == prefix && suffixTable[hash] == suffix) {
                    prefix = codeTable[hash]
                    found = true
                    break
                }
                hash = (hash + 1) % 5003
            }

            if (!found) {
                writeBits(prefix)

                if (freeCode < 4096) {
                    prefixTable[hash] = prefix
                    suffixTable[hash] = suffix
                    codeTable[hash] = freeCode++

                    if (freeCode > maxCode && codeSize < 12) {
                        codeSize++
                        maxCode = 1 shl codeSize
                    }
                } else {
                    writeBits(clearCode)
                    prefixTable.fill(-1)
                    codeSize = initCodeSize + 1
                    maxCode = 1 shl codeSize
                    freeCode = eoiCode + 1
                }

                prefix = suffix
            }
        }

        writeBits(prefix)
        writeBits(eoiCode)
        flushBits()

        // Terminating sub-block (0 bytes)
        out.write(0)
    }
}
