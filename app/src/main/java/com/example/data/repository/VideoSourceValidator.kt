package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.FaceDetector
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.SourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class VideoSourceValidator(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun inspectLocalVideo(uri: Uri): Result<SourceInfo> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            val durationMs = durationStr?.toLongOrNull()?.coerceAtLeast(3_000L) ?: 30_000L
            val rawW = widthStr?.toIntOrNull() ?: 1920
            val rawH = heightStr?.toIntOrNull() ?: 1080
            val rot = rotationStr?.toIntOrNull() ?: 0
            val (w, h) = if (rot == 90 || rot == 270) rawH to rawW else rawW to rawH

            val displayName = resolveDisplayName(uri) ?: "Uploaded Video"
            Result.success(
                SourceInfo(
                    sourceType = "LOCAL_VIDEO",
                    sourceUrl = uri.toString(),
                    localVideoUriString = uri.toString(),
                    title = displayName.substringBeforeLast('.').ifBlank { "Uploaded Video" },
                    durationMs = durationMs,
                    width = w,
                    height = h,
                    canRenderMp4Locally = true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect local video $uri", e)
            Result.failure(IllegalArgumentException("Unable to read selected video file. Please pick a valid MP4/MOV video."))
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun validateAndInspectUrl(rawInput: String): Result<SourceInfo> = withContext(Dispatchers.IO) {
        val url = rawInput.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(
                IllegalArgumentException("Please enter a valid https:// YouTube URL or direct video URL.")
            )
        }
        val lower = url.lowercase()
        val isYouTube = lower.contains("youtube.com/") || lower.contains("youtu.be/")
        if (isYouTube) {
            return@withContext inspectYouTubeOEmbed(url)
        }

        // Direct authorized MP4/WebM/MOV URL
        val isDirectMedia = lower.contains(".mp4") || lower.contains(".mov") ||
            lower.contains(".webm") || lower.contains(".m4v")
        if (!isDirectMedia) {
            return@withContext Result.failure(
                IllegalArgumentException("Unsupported URL. Paste a YouTube link, a direct .mp4/.mov URL, or upload a video file from your phone.")
            )
        }

        return@withContext downloadDirectVideoToCache(url)
    }

    private fun inspectYouTubeOEmbed(url: String): Result<SourceInfo> {
        return try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val req = Request.Builder().url(oembedUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "Could not access YouTube video metadata (HTTP ${resp.code}). The video may be private, unavailable, or invalid."
                        )
                    )
                }
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val title = json.optString("title", "YouTube Video")
                val author = json.optString("author_name", "Creator")
                val thumb = json.optString("thumbnail_url", "")
                Result.success(
                    SourceInfo(
                        sourceType = "YOUTUBE_URL",
                        sourceUrl = url,
                        localVideoUriString = null,
                        title = title,
                        author = author,
                        thumbnailUrl = thumb.takeIf { it.isNotBlank() },
                        durationMs = 180_000L,
                        width = 1920,
                        height = 1080,
                        canRenderMp4Locally = false,
                        notice = "YouTube metadata loaded cleanly. Because ClipFlow AI respects YouTube DRM/ToS, AI analysis & hook/caption planning work directly—and you can attach your own local MP4 file anytime for on-device MP4 rendering & Gallery export."
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "YouTube oEmbed check failed", e)
            Result.failure(IllegalStateException("Network error validating YouTube URL: ${e.localizedMessage}"))
        }
    }

    private fun downloadDirectVideoToCache(url: String): Result<SourceInfo> {
        return try {
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(IllegalStateException("Unable to fetch direct video URL (HTTP ${resp.code})."))
                }
                val cacheDir = File(context.cacheDir, "sources").apply { mkdirs() }
                val destFile = File(cacheDir, "source_${System.currentTimeMillis()}.mp4")
                resp.body?.byteStream()?.use { input ->
                    FileOutputStream(destFile).use { out ->
                        input.copyTo(out)
                    }
                } ?: return Result.failure(IllegalStateException("Empty response body from video URL."))

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(destFile.absolutePath)
                    val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()?.coerceAtLeast(3_000L) ?: 30_000L
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull() ?: 1920
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull() ?: 1080
                    val fileUri = Uri.fromFile(destFile)
                    Result.success(
                        SourceInfo(
                            sourceType = "DIRECT_URL",
                            sourceUrl = url,
                            localVideoUriString = fileUri.toString(),
                            title = url.substringAfterLast('/').substringBefore('?').substringBeforeLast('.').ifBlank { "Imported Video" },
                            durationMs = dur,
                            width = w,
                            height = h,
                            canRenderMp4Locally = true
                        )
                    )
                } finally {
                    runCatching { retriever.release() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct video download failed", e)
            Result.failure(IllegalStateException("Failed to import direct video: ${e.localizedMessage}"))
        }
    }

    suspend fun extractKeyframes(uriString: String, count: Int = 6): List<Bitmap> = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext emptyList()
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(2_000L) ?: 10_000L
            val safeCount = count.coerceIn(2, 10)
            for (i in 0 until safeCount) {
                val fraction = (i + 0.5) / safeCount.toDouble()
                val timeUs = (durationMs * fraction * 1000.0).toLong()
                val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (raw != null) {
                    val scaled = scaleBitmapMax(raw, 480)
                    if (scaled !== raw) raw.recycle()
                    frames.add(scaled)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Keyframe extraction warning: ${e.message}")
        } finally {
            runCatching { retriever.release() }
        }
        frames
    }

    /**
     * Real on-device face tracking across [startMs, endMs] using Android FaceDetector
     * to determine the horizontal speaker center (0.0 left .. 1.0 right) for smart reframing.
     */
    suspend fun detectFaceFocusX(uriString: String, startMs: Long, endMs: Long): Double = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext 0.5
        val retriever = MediaMetadataRetriever()
        val detectedCenters = mutableListOf<Float>()
        try {
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val span = (endMs - startMs).coerceAtLeast(1000L)
            val samples = listOf(0.2, 0.5, 0.8)
            for (frac in samples) {
                val timeUs = ((startMs + (span * frac)) * 1000.0).toLong()
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: continue
                val scaled = scaleBitmapMax(frame, 480)
                if (scaled !== frame) frame.recycle()
                // FaceDetector requires RGB_565 and even width
                val evenW = if (scaled.width % 2 == 0) scaled.width else scaled.width - 1
                val evenH = scaled.height
                val rgb565 = scaled.copy(Bitmap.Config.RGB_565, false)
                val prepared = if (rgb565.width != evenW) {
                    Bitmap.createBitmap(rgb565, 0, 0, evenW, evenH).also { rgb565.recycle() }
                } else rgb565
                scaled.recycle()

                val faces = arrayOfNulls<FaceDetector.Face>(3)
                val detector = FaceDetector(prepared.width, prepared.height, 3)
                val found = detector.findFaces(prepared, faces)
                if (found > 0) {
                    val midPoint = android.graphics.PointF()
                    faces[0]?.getMidPoint(midPoint)
                    val normX = (midPoint.x / prepared.width.toFloat()).coerceIn(0.15f, 0.85f)
                    detectedCenters.add(normX)
                }
                prepared.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Face tracking sample warning: ${e.message}")
        } finally {
            runCatching { retriever.release() }
        }
        if (detectedCenters.isNotEmpty()) {
            detectedCenters.average().coerceIn(0.15, 0.85)
        } else {
            0.5
        }
    }

    suspend fun readSmallMediaBytesIfUnderLimit(uriString: String, maxBytes: Long = 12L * 1024L * 1024L): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file" && uri.path != null) {
                    val file = File(uri.path!!)
                    if (file.exists() && file.length() in 1..maxBytes) {
                        return@withContext file.readBytes()
                    }
                } else {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                        if (afd.length in 1..maxBytes) {
                            return@withContext context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapMax(src: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(src.width, src.height)
        if (maxSide <= maxDim) return src
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val w = (src.width * ratio).toInt().coerceAtLeast(2)
        val h = (src.height * ratio).toInt().coerceAtLeast(2)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private companion object {
        const val TAG = "VideoSourceValidator"
    }
}
