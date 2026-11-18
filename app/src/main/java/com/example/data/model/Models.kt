package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val email: String? = null,
    val planTier: String = "FREE",
    val clipsGeneratedCount: Long = 0L,
    val brandName: String = "ClipFlow Studio",
    val brandPrimaryHex: String = "#F4D96B",
    val brandFontStyle: String = "Space Grotesk",
    val brandIntroTag: String? = null,
    val brandOutroTag: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName.take(80).ifBlank { "Creator" },
            "email" to email?.take(120)?.takeIf { it.isNotBlank() },
            "planTier" to planTier.takeIf { it in listOf("FREE", "PRO", "PREMIUM") }.orEmpty().ifBlank { "FREE" },
            "clipsGeneratedCount" to clipsGeneratedCount.coerceAtLeast(0L),
            "brandName" to brandName.take(60).ifBlank { "ClipFlow Studio" },
            "brandPrimaryHex" to brandPrimaryHex.take(9).ifBlank { "#F4D96B" },
            "brandFontStyle" to brandFontStyle.take(40).ifBlank { "Space Grotesk" },
            "brandIntroTag" to brandIntroTag?.take(80)?.takeIf { it.isNotBlank() },
            "brandOutroTag" to brandOutroTag?.take(80)?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class VideoProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Untitled Project",
    val folderName: String = "All Projects",
    val sourceType: String = "LOCAL_VIDEO",
    val sourceUrl: String = "",
    val thumbnailUrl: String? = null,
    val videoCategory: String = "Podcast",
    val status: String = "ANALYZED",
    val durationMs: Long = 60_000L,
    val transcriptText: String = "",
    val aiSummary: String = "",
    val copilotInstruction: String? = null,
    val aspectRatio: String = "9:16",
    val captionStyle: String = "Bold Highlight",
    val captionLanguage: String = "English",
    val clipCountRequested: Long = 5L,
    val durationPreset: String = "30-45s",
    val intensityPreset: String = "High Engagement",
    val voiceEnhanceEnabled: Boolean = true,
    val noiseReductionEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val sfxEnabled: Boolean = true,
    val silenceRemovalEnabled: Boolean = true,
    val faceTrackingEnabled: Boolean = true,
    val speakerLayout: String = "Active Speaker",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val validRatio = aspectRatio.takeIf { it in listOf("9:16", "16:9", "1:1", "4:5") } ?: "9:16"
        val validSource = sourceType.takeIf { it in listOf("LOCAL_VIDEO", "YOUTUBE_URL", "DIRECT_URL") } ?: "LOCAL_VIDEO"
        val validStatus = status.takeIf { it in listOf("ANALYZED", "RENDERING", "COMPLETED", "FAILED") } ?: "ANALYZED"
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "title" to title.take(140).ifBlank { "Untitled Project" },
            "folderName" to folderName.take(60).ifBlank { "All Projects" },
            "sourceType" to validSource,
            "sourceUrl" to sourceUrl.take(600),
            "thumbnailUrl" to thumbnailUrl?.take(600)?.takeIf { it.isNotBlank() },
            "videoCategory" to videoCategory.take(40).ifBlank { "Podcast" },
            "status" to validStatus,
            "durationMs" to durationMs.coerceIn(0L, 86_400_000L),
            "transcriptText" to transcriptText.take(20_000),
            "aiSummary" to aiSummary.take(1000).ifBlank { "AI analyzed video moments." },
            "copilotInstruction" to copilotInstruction?.take(500)?.takeIf { it.isNotBlank() },
            "aspectRatio" to validRatio,
            "captionStyle" to captionStyle.take(40).ifBlank { "Bold Highlight" },
            "captionLanguage" to captionLanguage.take(40).ifBlank { "English" },
            "clipCountRequested" to clipCountRequested.coerceIn(1L, 25L),
            "durationPreset" to durationPreset.take(40).ifBlank { "30-45s" },
            "intensityPreset" to intensityPreset.take(40).ifBlank { "High Engagement" },
            "voiceEnhanceEnabled" to voiceEnhanceEnabled,
            "noiseReductionEnabled" to noiseReductionEnabled,
            "musicEnabled" to musicEnabled,
            "sfxEnabled" to sfxEnabled,
            "silenceRemovalEnabled" to silenceRemovalEnabled,
            "faceTrackingEnabled" to faceTrackingEnabled,
            "speakerLayout" to speakerLayout.take(40).ifBlank { "Active Speaker" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class GeneratedClip(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val clipIndex: Long = 1L,
    val title: String = "Viral Clip",
    val hook: String = "Wait until you hear this...",
    val hookType: String = "Curiosity",
    val hookOptions: List<String> = emptyList(),
    val startMs: Long = 0L,
    val endMs: Long = 30_000L,
    val viralScore: Long = 85L,
    val scoreReason: String = "Strong opening hook and clear takeaway.",
    val transcriptExcerpt: String = "",
    val captionsText: String = "",
    val translatedCaptionsText: String? = null,
    val captionLanguage: String = "English",
    val captionStyle: String = "Bold Highlight",
    val captionColorHex: String = "#F4D96B",
    val captionPositionY: Double = 0.78,
    val aspectRatio: String = "9:16",
    val faceFocusX: Double = 0.5,
    val platformPreset: String = "YouTube Shorts",
    val youtubeTitle: String? = null,
    val instagramCaption: String? = null,
    val tiktokDescription: String? = null,
    val hashtags: String? = null,
    val qualityHook: Long = 88L,
    val qualityCaption: Long = 90L,
    val qualityAudio: Long = 86L,
    val qualityFraming: Long = 92L,
    val qualityContext: Long = 89L,
    val localFilePath: String? = null,
    val gallerySaved: Boolean = false,
    val rendered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1000L)

    fun toCreateMap(): Map<String, Any> {
        val safeStart = startMs.coerceAtLeast(0L)
        val safeEnd = endMs.coerceAtLeast(safeStart + 1000L).coerceAtMost(86_400_000L)
        val validRatio = aspectRatio.takeIf { it in listOf("9:16", "16:9", "1:1", "4:5") } ?: "9:16"
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "projectId" to projectId,
            "clipIndex" to clipIndex.coerceIn(1L, 100L),
            "title" to title.take(140).ifBlank { "Viral Clip $clipIndex" },
            "hook" to hook.take(240).ifBlank { "Watch this key moment..." },
            "hookType" to hookType.take(50).ifBlank { "Curiosity" },
            "hookOptions" to hookOptions.take(10).map { it.take(200) },
            "startMs" to safeStart,
            "endMs" to safeEnd,
            "viralScore" to viralScore.coerceIn(0L, 100L),
            "scoreReason" to scoreReason.take(600).ifBlank { "Selected for strong clarity and engagement." },
            "transcriptExcerpt" to transcriptExcerpt.take(4000),
            "captionsText" to captionsText.take(4000),
            "translatedCaptionsText" to translatedCaptionsText?.take(4000)?.takeIf { it.isNotBlank() },
            "captionLanguage" to captionLanguage.take(40).ifBlank { "English" },
            "captionStyle" to captionStyle.take(40).ifBlank { "Bold Highlight" },
            "captionColorHex" to captionColorHex.take(9).ifBlank { "#F4D96B" },
            "captionPositionY" to captionPositionY.coerceIn(0.05, 0.95),
            "aspectRatio" to validRatio,
            "faceFocusX" to faceFocusX.coerceIn(0.0, 1.0),
            "platformPreset" to platformPreset.take(40).ifBlank { "YouTube Shorts" },
            "youtubeTitle" to youtubeTitle?.take(150)?.takeIf { it.isNotBlank() },
            "instagramCaption" to instagramCaption?.take(1000)?.takeIf { it.isNotBlank() },
            "tiktokDescription" to tiktokDescription?.take(1000)?.takeIf { it.isNotBlank() },
            "hashtags" to hashtags?.take(300)?.takeIf { it.isNotBlank() },
            "qualityHook" to qualityHook.coerceIn(0L, 100L),
            "qualityCaption" to qualityCaption.coerceIn(0L, 100L),
            "qualityAudio" to qualityAudio.coerceIn(0L, 100L),
            "qualityFraming" to qualityFraming.coerceIn(0L, 100L),
            "qualityContext" to qualityContext.coerceIn(0L, 100L),
            "localFilePath" to localFilePath?.take(500)?.takeIf { it.isNotBlank() },
            "gallerySaved" to gallerySaved,
            "rendered" to rendered,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class ClipTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Bold Podcast",
    val aspectRatio: String = "9:16",
    val captionStyle: String = "Bold Highlight",
    val captionColorHex: String = "#F4D96B",
    val captionLanguage: String = "English",
    val platformPreset: String = "YouTube Shorts",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val validRatio = aspectRatio.takeIf { it in listOf("9:16", "16:9", "1:1", "4:5") } ?: "9:16"
        return mapOf(
            "userId" to userId,
            "name" to name.take(80).ifBlank { "Custom Preset" },
            "aspectRatio" to validRatio,
            "captionStyle" to captionStyle.take(40).ifBlank { "Bold Highlight" },
            "captionColorHex" to captionColorHex.take(9).ifBlank { "#F4D96B" },
            "captionLanguage" to captionLanguage.take(40).ifBlank { "English" },
            "platformPreset" to platformPreset.take(40).ifBlank { "YouTube Shorts" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class GenerationSettings(
    val aspectRatio: String = "9:16",
    val clipCount: Int = 5,
    val durationPreset: String = "30-45s",
    val intensityPreset: String = "High Engagement",
    val captionLanguage: String = "English",
    val translatedMode: Boolean = false,
    val captionStyle: String = "Bold Highlight",
    val captionColorHex: String = "#F4D96B",
    val captionPositionY: Double = 0.78,
    val voiceEnhanceEnabled: Boolean = true,
    val noiseReductionEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val sfxEnabled: Boolean = true,
    val silenceRemovalEnabled: Boolean = true,
    val faceTrackingEnabled: Boolean = true,
    val speakerLayout: String = "Active Speaker",
    val platformPreset: String = "YouTube Shorts",
    val videoCategory: String = "Podcast"
)

data class SourceInfo(
    val sourceType: String,
    val sourceUrl: String,
    val localVideoUriString: String?,
    val title: String,
    val author: String = "",
    val thumbnailUrl: String? = null,
    val durationMs: Long = 60_000L,
    val width: Int = 1920,
    val height: Int = 1080,
    val canRenderMp4Locally: Boolean = false,
    val notice: String? = null
)

data class RenderProgress(
    val isRendering: Boolean = false,
    val currentClipIndex: Int = 0,
    val totalClips: Int = 0,
    val currentClipPercent: Int = 0,
    val stageLabel: String = ""
)

object ClipFlowCatalog {
    val categories = listOf(
        "Podcast", "Educational", "Motivational", "Interview",
        "Business", "Storytelling", "Vlog", "Gaming", "Entertainment"
    )

    val aspectRatios = listOf(
        "9:16" to "1080 × 1920",
        "16:9" to "1920 × 1080",
        "1:1" to "1080 × 1080",
        "4:5" to "1080 × 1350"
    )

    val clipCounts = listOf(3, 5, 10, 15, 20)

    val durationPresets = listOf("15-30s", "30-45s", "45-60s", "60-90s", "90-180s", "Automatic")

    val intensities = listOf(
        "Balanced", "High Engagement", "Storytelling",
        "Educational", "Emotional", "Maximum Quality"
    )

    val languages = listOf(
        "English", "Nepali", "Hindi", "Spanish", "Portuguese",
        "French", "German", "Arabic", "Japanese", "Korean",
        "Chinese", "Bengali", "Urdu"
    )

    val captionStyles = listOf(
        "Bold Highlight", "Clean White", "Dynamic Word-by-Word",
        "Karaoke", "Cinematic", "Modern Podcast",
        "Colorful Emphasis", "Minimal Subtitles", "Custom Captions"
    )

    val captionColors = listOf(
        "#F4D96B" to "ClipFlow Gold",
        "#FFFFFF" to "Pure White",
        "#4ADE80" to "Creator Mint",
        "#38BDF8" to "Sky Cyan",
        "#FB7185" to "Coral Pop"
    )

    val speakerLayouts = listOf(
        "Active Speaker", "Split Screen", "Side-by-Side", "Auto Switch", "Custom Crop"
    )

    val platformPresets = listOf(
        "YouTube Shorts", "Instagram Reels", "TikTok", "Facebook Reels"
    )
}
