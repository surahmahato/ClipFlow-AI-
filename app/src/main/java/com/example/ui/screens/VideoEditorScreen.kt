package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClipFlowCatalog
import com.example.data.model.GeneratedClip
import com.example.data.model.RenderProgress
import com.example.data.model.VideoProject
import com.example.ui.components.AspectRatioPreviewBox
import com.example.ui.components.ClipFlowCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.PrimaryGoldButton
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftCreamSurface
import com.example.ui.theme.WarmAccentYellow
import com.example.ui.theme.WarmChipSurface
import com.example.ui.theme.WarmSandBorder

@Composable
fun VideoEditorScreen(
    project: VideoProject?,
    clip: GeneratedClip?,
    brandName: String,
    localAttachedUri: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    aiAssistantBusy: Boolean,
    renderProgress: RenderProgress,
    onBack: () -> Unit,
    onUpdateClip: (GeneratedClip) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveChanges: () -> Unit,
    onRenderSingleClip: (VideoProject, GeneratedClip) -> Unit,
    onSaveToGallery: (VideoProject, GeneratedClip) -> Unit,
    onRunAiAssistant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    if (clip == null || project == null) {
        Box(
            modifier = modifier.fillMaxSize().background(CreamBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a clip to edit.", color = SecondarySlate)
        }
        return
    }

    val effectiveVideoUri = localAttachedUri
        ?: project.sourceUrl.takeIf { project.sourceType != "YOUTUBE_URL" && it.isNotBlank() }

    var aiPrompt by remember { mutableStateOf("") }
    val totalMaxMs = project.durationMs.coerceAtLeast(clip.endMs + 5_000L).toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Editor Top Bar with Undo / Redo / Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to clips", tint = InkCharcoal)
                    }
                    Text(
                        text = "Clip %02d Studio".format(clip.clipIndex),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkCharcoal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.testTag("editor_undo_btn")) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) InkCharcoal else SecondarySlate.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.testTag("editor_redo_btn")) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) InkCharcoal else SecondarySlate.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = onSaveChanges, modifier = Modifier.testTag("editor_save_btn")) {
                        Icon(Icons.Filled.Check, contentDescription = "Save edits", tint = InkCharcoal)
                    }
                }
            }

            // Center Video Preview
            AspectRatioPreviewBox(
                videoUriString = effectiveVideoUri,
                startMs = clip.startMs,
                endMs = clip.endMs,
                aspectRatio = clip.aspectRatio,
                hookText = clip.hook,
                captionText = clip.translatedCaptionsText ?: clip.captionsText,
                captionColorHex = clip.captionColorHex,
                captionStyle = clip.captionStyle,
                captionPositionY = clip.captionPositionY,
                brandName = brandName
            )

            // AI Editing Assistant Card
            ClipFlowCard(containerColor = PureWhite) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = InkCharcoal, modifier = Modifier.size(18.dp))
                    Text("AI Editing Assistant", style = MaterialTheme.typography.titleSmall, color = InkCharcoal)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        placeholder = { Text("e.g. Change captions to yellow, Make 9:16") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SoftCreamSurface,
                            unfocusedContainerColor = SoftCreamSurface,
                            focusedBorderColor = WarmAccentYellow,
                            unfocusedBorderColor = WarmSandBorder
                        ),
                        modifier = Modifier.weight(1f).testTag("ai_editor_assistant_input")
                    )
                    IconButton(
                        onClick = {
                            if (aiPrompt.isNotBlank() && !aiAssistantBusy) {
                                onRunAiAssistant(aiPrompt)
                                aiPrompt = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryGoldButton)
                            .testTag("ai_editor_assistant_send")
                    ) {
                        if (aiAssistantBusy) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = InkCharcoal, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Apply AI edit", tint = InkCharcoal)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Make the opening more engaging",
                        "Move subtitles slightly higher",
                        "Change captions to yellow",
                        "Translate captions to Nepali"
                    ).forEach { cmd ->
                        Surface(
                            color = WarmChipSurface,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable { onRunAiAssistant(cmd) }
                        ) {
                            Text(
                                text = cmd,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkCharcoal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Timeline Trimmer & Aspect Ratio / Face Focus
            ClipFlowCard(containerColor = PureWhite) {
                Text("Timeline Trim & Smart Reframing", style = MaterialTheme.typography.titleSmall, color = InkCharcoal)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start: ${formatDurationMs(clip.startMs)}", style = MaterialTheme.typography.labelSmall, color = SecondarySlate)
                    Text("Duration: ${formatDurationMs(clip.durationMs)}", style = MaterialTheme.typography.labelSmall, color = InkCharcoal)
                    Text("End: ${formatDurationMs(clip.endMs)}", style = MaterialTheme.typography.labelSmall, color = SecondarySlate)
                }

                RangeSlider(
                    value = clip.startMs.toFloat()..clip.endMs.toFloat().coerceAtLeast(clip.startMs + 2000f),
                    onValueChange = { range ->
                        val newStart = range.start.toLong().coerceAtLeast(0L)
                        val newEnd = range.endInclusive.toLong().coerceAtLeast(newStart + 2000L)
                        onUpdateClip(clip.copy(startMs = newStart, endMs = newEnd))
                    },
                    valueRange = 0f..totalMaxMs,
                    colors = SliderDefaults.colors(
                        thumbColor = InkCharcoal,
                        activeTrackColor = PrimaryGoldButton
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = InkCharcoal)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClipFlowCatalog.aspectRatios.forEach { (ratio, _) ->
                        FilterChip(
                            selected = clip.aspectRatio == ratio,
                            onClick = { onUpdateClip(clip.copy(aspectRatio = ratio)) },
                            label = { Text(ratio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarmAccentYellow,
                                selectedLabelColor = InkCharcoal
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Face-Track Horizontal Pan (${(clip.faceFocusX * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkCharcoal
                )
                Slider(
                    value = clip.faceFocusX.toFloat(),
                    onValueChange = { x -> onUpdateClip(clip.copy(faceFocusX = x.toDouble())) },
                    valueRange = 0.1f..0.9f,
                    colors = SliderDefaults.colors(thumbColor = InkCharcoal, activeTrackColor = PrimaryGoldButton)
                )
            }

            // Hook & Caption Editor Card
            ClipFlowCard(containerColor = PureWhite) {
                Text("Hook & Dynamic Captions", style = MaterialTheme.typography.titleSmall, color = InkCharcoal)
                Spacer(Modifier.height(8.dp))

                if (clip.hookOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        clip.hookOptions.forEach { opt ->
                            FilterChip(
                                selected = clip.hook == opt,
                                onClick = { onUpdateClip(clip.copy(hook = opt)) },
                                label = { Text(opt.take(28) + if (opt.length > 28) "…" else "") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarmAccentYellow,
                                    selectedLabelColor = InkCharcoal
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                OutlinedTextField(
                    value = clip.hook,
                    onValueChange = { onUpdateClip(clip.copy(hook = it)) },
                    label = { Text("Opening Hook") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = clip.captionsText,
                    onValueChange = { onUpdateClip(clip.copy(captionsText = it)) },
                    label = { Text("Captions (${clip.captionLanguage})") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                Text("Caption Style", style = MaterialTheme.typography.labelMedium, color = InkCharcoal)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClipFlowCatalog.captionStyles.forEach { style ->
                        FilterChip(
                            selected = clip.captionStyle == style,
                            onClick = { onUpdateClip(clip.copy(captionStyle = style)) },
                            label = { Text(style) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarmAccentYellow,
                                selectedLabelColor = InkCharcoal
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Color:", style = MaterialTheme.typography.labelMedium, color = InkCharcoal)
                    ClipFlowCatalog.captionColors.forEach { (hex, _) ->
                        val swatch = Color(android.graphics.Color.parseColor(hex))
                        val isSel = clip.captionColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(if (isSel) 2.5.dp else 1.dp, if (isSel) InkCharcoal else WarmSandBorder, CircleShape)
                                .clickable { onUpdateClip(clip.copy(captionColorHex = hex)) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Caption Vertical Position (${(clip.captionPositionY * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkCharcoal
                )
                Slider(
                    value = clip.captionPositionY.toFloat(),
                    onValueChange = { y -> onUpdateClip(clip.copy(captionPositionY = y.toDouble())) },
                    valueRange = 0.2f..0.88f,
                    colors = SliderDefaults.colors(thumbColor = InkCharcoal, activeTrackColor = PrimaryGoldButton)
                )
            }

            // Social Copy Card
            ClipFlowCard(containerColor = PureWhite) {
                Text("Social Media Copy & Hashtags", style = MaterialTheme.typography.titleSmall, color = InkCharcoal)
                Spacer(Modifier.height(6.dp))
                Text("YouTube Shorts: ${clip.youtubeTitle ?: clip.title}", style = MaterialTheme.typography.bodySmall, color = InkCharcoal)
                Spacer(Modifier.height(4.dp))
                Text("Instagram: ${clip.instagramCaption ?: clip.hook}", style = MaterialTheme.typography.bodySmall, color = SecondarySlate)
                Spacer(Modifier.height(4.dp))
                Text("Hashtags: ${clip.hashtags ?: "#shorts #clipflow"}", style = MaterialTheme.typography.labelSmall, color = InkCharcoal)
            }

            // Export Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSaveChanges()
                        onRenderSingleClip(project, clip)
                    },
                    enabled = !renderProgress.isRendering,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("editor_render_mp4_btn"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WarmSandBorder)
                ) {
                    Icon(Icons.Filled.MovieCreation, contentDescription = null, tint = InkCharcoal)
                    Spacer(Modifier.width(6.dp))
                    Text(if (renderProgress.isRendering) "${renderProgress.currentClipPercent}%" else "Render MP4", color = InkCharcoal)
                }

                GoldPrimaryButton(
                    text = "Save Gallery",
                    onClick = {
                        onSaveChanges()
                        onSaveToGallery(project, clip)
                    },
                    modifier = Modifier.weight(1f).testTag("editor_save_gallery_btn"),
                    leadingIcon = { Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
