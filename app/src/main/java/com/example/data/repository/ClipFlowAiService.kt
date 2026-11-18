package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.GeneratedClip
import com.example.data.model.GenerationSettings
import com.example.data.model.SourceInfo
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiAnalysisResult(
    val title: String,
    val summary: String,
    val transcript: String,
    val thresholdNote: String,
    val clips: List<GeneratedClip>
)

data class AiEditDelta(
    val explanation: String,
    val hook: String? = null,
    val captionsText: String? = null,
    val translatedCaptionsText: String? = null,
    val captionLanguage: String? = null,
    val captionStyle: String? = null,
    val captionColorHex: String? = null,
    val captionPositionY: Double? = null,
    val aspectRatio: String? = null,
    val trimStartDeltaMs: Long = 0L,
    val trimEndDeltaMs: Long = 0L
)

class ClipFlowAiService {

    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "videoTitle" to Schema.string(description = "Concise descriptive title for the source video"),
            "summary" to Schema.string(description = "2-sentence overview of the video's main themes"),
            "transcript" to Schema.string(description = "Clean readable transcript or visual/audio narrative of the video"),
            "thresholdNote" to Schema.string(description = "Explains whether all requested clips met the quality threshold"),
            "clips" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Catchy short-form title for this clip"),
                        "hook" to Schema.string(description = "Strong natural opening hook based on actual content"),
                        "hookType" to Schema.string(description = "Curiosity, Emotional, Question, Educational, Storytelling, or Contrarian"),
                        "hookOptions" to Schema.array(
                            items = Schema.string(),
                            description = "3 alternative hook variations for this clip"
                        ),
                        "startSeconds" to Schema.integer(description = "Clip start time in seconds (>= 0)"),
                        "endSeconds" to Schema.integer(description = "Clip end time in seconds (> startSeconds)"),
                        "viralScore" to Schema.integer(description = "AI Viral Potential Score 0..100"),
                        "scoreReason" to Schema.string(description = "Why AI selected this moment (never guarantee virality)"),
                        "transcriptExcerpt" to Schema.string(description = "Spoken or visual transcript excerpt for this clip"),
                        "captionsText" to Schema.string(description = "Clean synced short-form captions in original language"),
                        "translatedCaptionsText" to Schema.string(description = "Captions translated into target captionLanguage if requested"),
                        "youtubeTitle" to Schema.string(description = "YouTube Shorts title"),
                        "instagramCaption" to Schema.string(description = "Instagram Reels caption"),
                        "tiktokDescription" to Schema.string(description = "TikTok description"),
                        "hashtags" to Schema.string(description = "4 to 6 relevant hashtags"),
                        "qualityHook" to Schema.integer(description = "Hook quality score 0..100"),
                        "qualityCaption" to Schema.integer(description = "Caption clarity score 0..100"),
                        "qualityAudio" to Schema.integer(description = "Audio/delivery score 0..100"),
                        "qualityFraming" to Schema.integer(description = "Visual framing potential 0..100"),
                        "qualityContext" to Schema.integer(description = "Standalone context completeness 0..100")
                    )
                ),
                description = "Ranked non-overlapping viral short clip candidates"
            )
        )
    )

    private val analysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = analysisSchema
            }
        )
    }

    private val singleClipSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(),
            "hook" to Schema.string(),
            "hookType" to Schema.string(),
            "hookOptions" to Schema.array(items = Schema.string()),
            "captionsText" to Schema.string(),
            "translatedCaptionsText" to Schema.string(),
            "scoreReason" to Schema.string(),
            "youtubeTitle" to Schema.string(),
            "instagramCaption" to Schema.string(),
            "tiktokDescription" to Schema.string(),
            "hashtags" to Schema.string()
        )
    )

    private val singleClipModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = singleClipSchema
            }
        )
    }

    private val editDeltaSchema = Schema.obj(
        properties = mapOf(
            "explanation" to Schema.string(description = "Short confirmation of the edit applied"),
            "hook" to Schema.string(nullable = true),
            "captionsText" to Schema.string(nullable = true),
            "translatedCaptionsText" to Schema.string(nullable = true),
            "captionLanguage" to Schema.string(nullable = true),
            "captionStyle" to Schema.string(nullable = true),
            "captionColorHex" to Schema.string(description = "Hex color like #F4D96B or #FFFFFF or #FACC15", nullable = true),
            "captionPositionY" to Schema.double(description = "Vertical position 0.15 (top) to 0.88 (bottom)", nullable = true),
            "aspectRatio" to Schema.string(description = "One of 9:16, 16:9, 1:1, 4:5", nullable = true),
            "trimStartDeltaMs" to Schema.integer(description = "Milliseconds to shift start time (+ trims start)"),
            "trimEndDeltaMs" to Schema.integer(description = "Milliseconds to shift end time (- shortens end)")
        )
    )

    private val editAssistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = editDeltaSchema
            }
        )
    }

    private val searchSchema = Schema.obj(
        properties = mapOf(
            "matches" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "clipIndex" to Schema.integer(),
                        "timestampLabel" to Schema.string(),
                        "relevanceReason" to Schema.string(),
                        "matchedQuote" to Schema.string()
                    )
                )
            )
        )
    )

    private val searchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = searchSchema
            }
        )
    }

    suspend fun analyzeVideo(
        source: SourceInfo,
        settings: GenerationSettings,
        userId: String,
        keyframes: List<Bitmap>,
        inlineVideoBytes: ByteArray?,
        userTranscriptOrContext: String,
        copilotInstruction: String
    ): AiAnalysisResult {
        val totalSeconds = (source.durationMs / 1000L).coerceAtLeast(10L)
        val promptText = buildString {
            appendLine("You are CLIPFLOW AI's Viral Video Analysis Engine.")
            appendLine("Analyze the provided video carefully and select the strongest standalone short-form clips.")
            appendLine("Source Title: ${source.title}")
            if (source.author.isNotBlank()) appendLine("Creator/Author: ${source.author}")
            appendLine("Source Type: ${source.sourceType}")
            appendLine("Total Video Duration: $totalSeconds seconds (${source.durationMs} ms). ALL startSeconds and endSeconds MUST be strictly within 0..$totalSeconds and non-overlapping!")
            appendLine("Category: ${settings.videoCategory}")
            appendLine("Requested Clip Count: up to ${settings.clipCount} clips (prioritize quality; if fewer strong moments exist in a short video, return only high-quality non-overlapping clips and explain in thresholdNote).")
            appendLine("Target Duration Preset: ${settings.durationPreset}")
            appendLine("Clipping Intensity: ${settings.intensityPreset}")
            appendLine("Target Caption Language: ${settings.captionLanguage} (Translated Mode: ${settings.translatedMode})")
            appendLine("Target Platform Preset: ${settings.platformPreset}")
            if (copilotInstruction.isNotBlank()) {
                appendLine("USER AI COPILOT INSTRUCTION (PRIORITIZE THIS): $copilotInstruction")
            }
            if (userTranscriptOrContext.isNotBlank()) {
                appendLine("USER PROVIDED TRANSCRIPT / NOTES:\n$userTranscriptOrContext")
            }
            appendLine("Rules:")
            appendLine("- Never invent fake quotes or claim any score guarantees virality.")
            appendLine("- Evaluate each clip across hook strength, curiosity, emotion, storytelling, info value, standalone context, visual engagement, audio clarity, and natural ending.")
            appendLine("- If Target Caption Language is not English or Translated Mode is true, provide accurate natural translatedCaptionsText in ${settings.captionLanguage}.")
            appendLine("- Keep hooks crisp, mobile-readable, and truthful to the video content.")
        }

        val inputContent = content {
            if (inlineVideoBytes != null) {
                inlineData(inlineVideoBytes, "video/mp4")
            } else {
                keyframes.take(6).forEach { bmp ->
                    image(bmp)
                }
            }
            text(promptText)
        }

        val response = try {
            withTimeout(60_000L) {
                analysisModel.generateContent(inputContent)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI analysis timed out", e)
            throw IllegalStateException("AI video analysis timed out after 60 seconds. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI analysis failed", e)
            throw IllegalStateException(e.localizedMessage ?: "AI video analysis failed.")
        }

        val rawJson = response.text ?: throw IllegalStateException("AI returned an empty analysis response.")
        val json = JSONObject(rawJson)
        val videoTitle = json.optString("videoTitle", source.title).ifBlank { source.title }.take(140)
        val summary = json.optString("summary", "AI analyzed your video for viral moments.").take(1000)
        val transcript = json.optString("transcript", userTranscriptOrContext).take(20_000)
        val thresholdNote = json.optString("thresholdNote", "")
        val clipsJson = json.optJSONArray("clips")

        val parsedClips = mutableListOf<GeneratedClip>()
        if (clipsJson != null) {
            for (i in 0 until clipsJson.length()) {
                val obj = clipsJson.getJSONObject(i)
                val rawStartSec = obj.optLong("startSeconds", 0L).coerceIn(0L, (totalSeconds - 2L).coerceAtLeast(0L))
                val minEndSec = (rawStartSec + 3L).coerceAtMost(totalSeconds)
                val rawEndSec = obj.optLong("endSeconds", minEndSec).coerceIn(minEndSec, totalSeconds)
                val startMs = rawStartSec * 1000L
                val endMs = (rawEndSec * 1000L).coerceAtLeast(startMs + 2000L)

                val hooksArr = obj.optJSONArray("hookOptions")
                val hookOpts = buildList {
                    if (hooksArr != null) {
                        for (h in 0 until hooksArr.length()) {
                            val item = hooksArr.optString(h).trim()
                            if (item.isNotBlank()) add(item)
                        }
                    }
                }
                val mainHook = obj.optString("hook", hookOpts.firstOrNull() ?: "Watch this key moment...")
                val captions = obj.optString("captionsText", obj.optString("transcriptExcerpt", ""))
                val translated = obj.optString("translatedCaptionsText", "").takeIf { it.isNotBlank() }

                parsedClips.add(
                    GeneratedClip(
                        userId = userId,
                        projectId = "",
                        clipIndex = (i + 1).toLong(),
                        title = obj.optString("title", "Clip ${i + 1}").take(140),
                        hook = mainHook.take(240),
                        hookType = obj.optString("hookType", "Curiosity").take(50),
                        hookOptions = (listOf(mainHook) + hookOpts).distinct().take(6),
                        startMs = startMs,
                        endMs = endMs,
                        viralScore = obj.optLong("viralScore", 84L).coerceIn(0L, 100L),
                        scoreReason = obj.optString("scoreReason", "High engagement moment.").take(600),
                        transcriptExcerpt = obj.optString("transcriptExcerpt", captions).take(4000),
                        captionsText = captions.take(4000),
                        translatedCaptionsText = translated?.take(4000),
                        captionLanguage = settings.captionLanguage,
                        captionStyle = settings.captionStyle,
                        captionColorHex = settings.captionColorHex,
                        captionPositionY = settings.captionPositionY,
                        aspectRatio = settings.aspectRatio,
                        faceFocusX = 0.5,
                        platformPreset = settings.platformPreset,
                        youtubeTitle = obj.optString("youtubeTitle", "").take(150).takeIf { it.isNotBlank() },
                        instagramCaption = obj.optString("instagramCaption", "").take(1000).takeIf { it.isNotBlank() },
                        tiktokDescription = obj.optString("tiktokDescription", "").take(1000).takeIf { it.isNotBlank() },
                        hashtags = obj.optString("hashtags", "#shorts #viral").take(300).takeIf { it.isNotBlank() },
                        qualityHook = obj.optLong("qualityHook", 88L).coerceIn(0L, 100L),
                        qualityCaption = obj.optLong("qualityCaption", 90L).coerceIn(0L, 100L),
                        qualityAudio = obj.optLong("qualityAudio", 85L).coerceIn(0L, 100L),
                        qualityFraming = obj.optLong("qualityFraming", 90L).coerceIn(0L, 100L),
                        qualityContext = obj.optLong("qualityContext", 87L).coerceIn(0L, 100L)
                    )
                )
            }
        }

        // Deduplicate & sort descending by viralScore, then re-index
        val deduplicated = parsedClips
            .sortedByDescending { it.viralScore }
            .distinctBy { "${it.startMs / 3000L}-${it.title.lowercase()}" }
            .take(settings.clipCount)
            .mapIndexed { idx, clip -> clip.copy(clipIndex = (idx + 1).toLong()) }

        return AiAnalysisResult(
            title = videoTitle,
            summary = summary,
            transcript = transcript,
            thresholdNote = thresholdNote,
            clips = deduplicated
        )
    }

    suspend fun regenerateSingleClip(
        clip: GeneratedClip,
        targetLanguage: String,
        hookAngle: String
    ): GeneratedClip {
        val prompt = """
            Regenerate creative hooks, captions, and social descriptions for this existing video clip WITHOUT changing its factual meaning.
            Clip Title: ${clip.title}
            Transcript Excerpt: ${clip.transcriptExcerpt.ifBlank { clip.captionsText }}
            Requested Hook Angle: $hookAngle
            Target Language: $targetLanguage
            Provide 4 diverse truthful hooks in hookOptions, an improved primary hook, refined captionsText, translatedCaptionsText (if $targetLanguage != English), and fresh social titles/hashtags.
        """.trimIndent()

        val response = withTimeout(45_000L) {
            singleClipModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
        val hooksArr = json.optJSONArray("hookOptions")
        val opts = buildList {
            if (hooksArr != null) {
                for (i in 0 until hooksArr.length()) {
                    val v = hooksArr.optString(i).trim()
                    if (v.isNotBlank()) add(v)
                }
            }
        }
        val newHook = json.optString("hook", clip.hook).ifBlank { clip.hook }
        return clip.copy(
            title = json.optString("title", clip.title).take(140),
            hook = newHook.take(240),
            hookType = json.optString("hookType", hookAngle).take(50),
            hookOptions = (listOf(newHook) + opts).distinct().take(6),
            captionsText = json.optString("captionsText", clip.captionsText).take(4000),
            translatedCaptionsText = json.optString("translatedCaptionsText", "").take(4000).takeIf { it.isNotBlank() },
            captionLanguage = targetLanguage,
            scoreReason = json.optString("scoreReason", clip.scoreReason).take(600),
            youtubeTitle = json.optString("youtubeTitle", clip.youtubeTitle ?: "").take(150).takeIf { it.isNotBlank() },
            instagramCaption = json.optString("instagramCaption", clip.instagramCaption ?: "").take(1000).takeIf { it.isNotBlank() },
            tiktokDescription = json.optString("tiktokDescription", clip.tiktokDescription ?: "").take(1000).takeIf { it.isNotBlank() },
            hashtags = json.optString("hashtags", clip.hashtags ?: "").take(300).takeIf { it.isNotBlank() }
        )
    }

    suspend fun applyAiEditInstruction(
        clip: GeneratedClip,
        instruction: String
    ): AiEditDelta {
        val prompt = """
            You are ClipFlow AI's Video Editing Assistant.
            Apply the user's editing instruction to this clip's current state. Only set fields that should change; leave others null.
            Current clip state:
            - hook: ${clip.hook}
            - captionsText: ${clip.captionsText}
            - captionLanguage: ${clip.captionLanguage}
            - captionStyle: ${clip.captionStyle}
            - captionColorHex: ${clip.captionColorHex}
            - captionPositionY: ${clip.captionPositionY} (0.15=top, 0.5=middle, 0.82=bottom)
            - aspectRatio: ${clip.aspectRatio} (valid: 9:16, 16:9, 1:1, 4:5)
            - durationMs: ${clip.durationMs}

            User instruction: "$instruction"
        """.trimIndent()

        val response = withTimeout(45_000L) {
            editAssistantModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI edit response"))
        return AiEditDelta(
            explanation = json.optString("explanation", "Applied your edit."),
            hook = json.optString("hook", "").takeIf { it.isNotBlank() && it != "null" },
            captionsText = json.optString("captionsText", "").takeIf { it.isNotBlank() && it != "null" },
            translatedCaptionsText = json.optString("translatedCaptionsText", "").takeIf { it.isNotBlank() && it != "null" },
            captionLanguage = json.optString("captionLanguage", "").takeIf { it.isNotBlank() && it != "null" },
            captionStyle = json.optString("captionStyle", "").takeIf { it.isNotBlank() && it != "null" },
            captionColorHex = json.optString("captionColorHex", "").takeIf { it.startsWith("#") },
            captionPositionY = if (json.has("captionPositionY") && !json.isNull("captionPositionY")) json.optDouble("captionPositionY").coerceIn(0.15, 0.88) else null,
            aspectRatio = json.optString("aspectRatio", "").takeIf { it in listOf("9:16", "16:9", "1:1", "4:5") },
            trimStartDeltaMs = json.optLong("trimStartDeltaMs", 0L),
            trimEndDeltaMs = json.optLong("trimEndDeltaMs", 0L)
        )
    }

    suspend fun searchClipsInTranscript(
        query: String,
        clips: List<GeneratedClip>,
        fullTranscript: String
    ): List<Pair<Int, String>> {
        val clipSummary = clips.joinToString("\n") {
            "Clip ${it.clipIndex} (${it.startMs / 1000}s-${it.endMs / 1000}s): ${it.title} | ${it.transcriptExcerpt}"
        }
        val prompt = """
            Find the most relevant clips matching the user's search query: "$query".
            Clips:
            $clipSummary
            Full Transcript:
            ${fullTranscript.take(4000)}
        """.trimIndent()

        val response = withTimeout(30_000L) {
            searchModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: return emptyList())
        val arr = json.optJSONArray("matches") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                add(item.optInt("clipIndex", 1) to item.optString("relevanceReason", ""))
            }
        }
    }

    private companion object {
        const val TAG = "ClipFlowAiService"
    }
}
