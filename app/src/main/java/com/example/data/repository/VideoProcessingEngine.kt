package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.model.GeneratedClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VideoProcessingEngine(private val context: Context) {

    @OptIn(UnstableApi::class)
    suspend fun renderClipToMp4(
        sourceUriString: String,
        sourceWidth: Int,
        sourceHeight: Int,
        clip: GeneratedClip,
        brandName: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeLang = clip.captionLanguage.replace(Regex("[^A-Za-z0-9]"), "")
            val fileName = if (safeLang.equals("English", ignoreCase = true)) {
                "ClipFlow_Clip_%02d.mp4".format(clip.clipIndex)
            } else {
                "ClipFlow_${safeLang}_Clip_%02d.mp4".format(clip.clipIndex)
            }
            val outputFile = File(exportDir, fileName)
            if (outputFile.exists()) outputFile.delete()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(sourceUriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs.coerceAtLeast(0L))
                        .setEndPositionMs(clip.endMs.coerceAtLeast(clip.startMs + 1000L))
                        .build()
                )
                .build()

            val effectsList = buildVideoEffects(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                clip = clip,
                brandName = brandName
            )

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(androidx.media3.transformer.Effects(emptyList(), effectsList))
                .build()

            val handler = Handler(Looper.getMainLooper())
            val progressHolder = ProgressHolder()

            val exportedFile = suspendCancellableCoroutine<File> { continuation ->
                var isPolling = true
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            isPolling = false
                            onProgress(100)
                            if (continuation.isActive) {
                                continuation.resume(outputFile)
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            isPolling = false
                            Log.e(TAG, "Media3 Transformer error", exportException)
                            if (continuation.isActive) {
                                continuation.resumeWithException(exportException)
                            }
                        }
                    })
                    .build()

                continuation.invokeOnCancellation {
                    isPolling = false
                    transformer.cancel()
                }

                transformer.start(editedMediaItem, outputFile.absolutePath)

                val pollRunnable = object : Runnable {
                    override fun run() {
                        if (!isPolling) return
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress.coerceIn(0, 99))
                        }
                        if (state != Transformer.PROGRESS_STATE_NOT_STARTED && isPolling) {
                            handler.postDelayed(this, 250L)
                        }
                    }
                }
                handler.postDelayed(pollRunnable, 250L)
            }

            Result.success(exportedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render MP4 clip", e)
            Result.failure(e)
        }
    }

    @OptIn(UnstableApi::class)
    private fun buildVideoEffects(
        sourceWidth: Int,
        sourceHeight: Int,
        clip: GeneratedClip,
        brandName: String
    ): List<Effect> {
        val effects = mutableListOf<Effect>()
        val srcAspect = (sourceWidth.toFloat() / sourceHeight.coerceAtLeast(1).toFloat()).coerceIn(0.3f, 3.0f)
        val (targetW, targetH) = when (clip.aspectRatio) {
            "16:9" -> 1280 to 720
            "1:1" -> 1080 to 1080
            "4:5" -> 864 to 1080
            else -> 720 to 1280 // 9:16 crisp HD portrait
        }
        val targetAspect = targetW.toFloat() / targetH.toFloat()

        // Smart Face-Tracked Crop in NDC space [-1, 1]
        if (srcAspect > targetAspect + 0.05f) {
            val widthFrac = (targetAspect / srcAspect).coerceIn(0.28f, 1.0f)
            val span = 2f * widthFrac
            val centerNdc = (clip.faceFocusX.toFloat() * 2f - 1f)
            val left = (centerNdc - span / 2f).coerceIn(-1f, 1f - span)
            val right = (left + span).coerceIn(left + 0.1f, 1f)
            effects.add(Crop(left, right, -1f, 1f))
        } else if (srcAspect < targetAspect - 0.05f) {
            val heightFrac = (srcAspect / targetAspect).coerceIn(0.28f, 1.0f)
            val span = 2f * heightFrac
            val bottom = (-span / 2f).coerceIn(-1f, 1f - span)
            val top = (bottom + span).coerceIn(bottom + 0.1f, 1f)
            effects.add(Crop(-1f, 1f, bottom, top))
        }

        effects.add(Presentation.createForWidthAndHeight(targetW, targetH, Presentation.LAYOUT_SCALE_TO_FIT))

        val overlayBitmap = renderOverlayBitmap(
            width = targetW,
            height = targetH,
            clip = clip,
            brandName = brandName
        )
        val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
        effects.add(OverlayEffect(listOf(bitmapOverlay)))

        return effects
    }

    private fun renderOverlayBitmap(
        width: Int,
        height: Int,
        clip: GeneratedClip,
        brandName: String
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT)

        val accentColor = runCatching { Color.parseColor(clip.captionColorHex) }
            .getOrDefault(Color.parseColor("#F4D96B"))

        // 1. Subtle Brand Kit badge top-left
        if (brandName.isNotBlank()) {
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 37, 37, 37)
                style = Paint.Style.FILL
            }
            val brandTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (width * 0.032f).coerceIn(22f, 36f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val label = brandName.take(24)
            val textW = brandTextPaint.measureText(label)
            val padX = width * 0.035f
            val padY = height * 0.045f
            val badgeRect = RectF(padX, padY, padX + textW + 32f, padY + brandTextPaint.textSize + 24f)
            canvas.drawRoundRect(badgeRect, 18f, 18f, brandPaint)
            canvas.drawText(label, badgeRect.left + 16f, badgeRect.bottom - 14f, brandTextPaint)
        }

        // 2. Opening Hook pill near top-center
        val hookText = clip.hook.take(72)
        if (hookText.isNotBlank()) {
            val hookPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#252525")
                textSize = (width * 0.04f).coerceIn(26f, 44f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val boxW = (width * 0.84f).toInt()
            val layout = StaticLayout.Builder.obtain(hookText, 0, hookText.length, hookPaint, boxW)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .build()
            val topY = height * 0.12f
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            val rect = RectF(
                (width - boxW) / 2f - 18f,
                topY - 12f,
                (width + boxW) / 2f + 18f,
                topY + layout.height + 16f
            )
            canvas.drawRoundRect(rect, 24f, 24f, bgPaint)
            canvas.save()
            canvas.translate((width - boxW) / 2f, topY)
            layout.draw(canvas)
            canvas.restore()
        }

        // 3. Burned-in Caption Banner at user's chosen captionPositionY
        val captionStr = (clip.translatedCaptionsText?.takeIf { it.isNotBlank() } ?: clip.captionsText)
            .take(120)
        if (captionStr.isNotBlank()) {
            val captionTextColor = if (clip.captionStyle == "Clean White" || clip.captionStyle == "Minimal Subtitles") {
                Color.WHITE
            } else {
                accentColor
            }
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = captionTextColor
                textSize = (width * 0.045f).coerceIn(28f, 52f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                setShadowLayer(6f, 2f, 2f, Color.BLACK)
            }
            val maxW = (width * 0.86f).toInt()
            val layout = StaticLayout.Builder.obtain(captionStr, 0, captionStr.length, textPaint, maxW)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(3)
                .build()
            val yPos = (height * clip.captionPositionY.toFloat() - layout.height / 2f)
                .coerceIn(height * 0.25f, height - layout.height - 48f)

            if (clip.captionStyle != "Minimal Subtitles") {
                val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(175, 20, 20, 20)
                    style = Paint.Style.FILL
                }
                val bgRect = RectF(
                    (width - maxW) / 2f - 20f,
                    yPos - 14f,
                    (width + maxW) / 2f + 20f,
                    yPos + layout.height + 14f
                )
                canvas.drawRoundRect(bgRect, 22f, 22f, pillBg)
            }
            canvas.save()
            canvas.translate((width - maxW) / 2f, yPos)
            layout.draw(canvas)
            canvas.restore()
        }

        return bmp
    }

    /**
     * METHOD A — Native Android MediaStore Gallery Save (Scoped Storage, zero storage permission on API 29+).
     * Saves under Movies/ClipFlow AI/<PlatformFolder>/ClipFlow_Clip_XX.mp4 and confirms actual insertion.
     */
    suspend fun saveClipToGallery(
        sourceMp4: File,
        clip: GeneratedClip
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (!sourceMp4.exists() || sourceMp4.length() == 0L) {
            return@withContext Result.failure(IllegalStateException("Rendered MP4 file does not exist yet. Generate clips first."))
        }
        try {
            val platformFolder = when (clip.platformPreset) {
                "YouTube Shorts" -> "YouTube Shorts"
                "Instagram Reels" -> "Instagram Reels"
                "TikTok" -> "TikTok"
                else -> "Original Exports"
            }
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, sourceMp4.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/ClipFlow AI/Generated Clips/$platformFolder"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val insertedUri = resolver.insert(collection, values)
                ?: return@withContext Result.failure(IllegalStateException("MediaStore rejected video insert."))

            resolver.openOutputStream(insertedUri)?.use { out ->
                FileInputStream(sourceMp4).use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open MediaStore output stream."))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(insertedUri, publishValues, null, null)
            }

            Result.success(insertedUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video to MediaStore Gallery", e)
            Result.failure(e)
        }
    }

    suspend fun createZipArchive(files: List<File>, archiveName: String = "ClipFlow_Clips_Export.zip"): Result<File> =
        withContext(Dispatchers.IO) {
            val existingFiles = files.filter { it.exists() && it.length() > 0L }
            if (existingFiles.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No rendered MP4 clips available to zip yet."))
            }
            try {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val zipFile = File(exportDir, archiveName)
                if (zipFile.exists()) zipFile.delete()
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    val buffer = ByteArray(8192)
                    existingFiles.forEach { file ->
                        BufferedInputStream(FileInputStream(file)).use { bis ->
                            zos.putNextEntry(ZipEntry(file.name))
                            var count: Int
                            while (bis.read(buffer).also { count = it } != -1) {
                                zos.write(buffer, 0, count)
                            }
                            zos.closeEntry()
                        }
                    }
                }
                Result.success(zipFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build ZIP archive", e)
                Result.failure(e)
            }
        }

    private companion object {
        const val TAG = "VideoProcessingEngine"
    }
}
