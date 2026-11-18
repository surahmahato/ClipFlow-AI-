package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.GeneratedClip
import com.example.data.model.GenerationSettings
import com.example.data.model.RenderProgress
import com.example.data.model.UiState
import com.example.data.model.VideoProject
import com.example.ui.components.AspectRatioPreviewBox
import com.example.ui.components.ClipFlowCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.ViralScoreBadge
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.InfoBlueText
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.PrimaryGoldButton
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftCreamSurface
import com.example.ui.theme.SoftErrorText
import com.example.ui.theme.ViralMintBg
import com.example.ui.theme.ViralMintText
import com.example.ui.theme.WarmAccentYellow
import com.example.ui.theme.WarmChipSurface
import com.example.ui.theme.WarmSandBorder
import com.example.ui.viewmodel.ClipSearchMatch
import java.io.File
import java.io.FileInputStream

@Composable
fun ClipResultsScreen(
    project: VideoProject?,
    clipsState: UiState<List<GeneratedClip>>,
    settings: GenerationSettings,
    brandName: String,
    localAttachedUri: String?,
    renderProgress: RenderProgress,
    searchResults: List<ClipSearchMatch>,
    searchBusy: Boolean,
    regeneratingClipId: String?,
    onAttachLocalVideo: (Uri) -> Unit,
    onOpenSettings: () -> Unit,
    onGenerateViralClips: (VideoProject, List<GeneratedClip>, String?) -> Unit,
    onSearchClips: (String, List<GeneratedClip>, String) -> Unit,
    onEditClip: (GeneratedClip) -> Unit,
    onSaveToGallery: (VideoProject, GeneratedClip) -> Unit,
    onRegenerateClip: (GeneratedClip, String, String) -> Unit,
    onDeleteClip: (String, String) -> Unit,
    onExportZip: (List<GeneratedClip>, (File) -> Unit) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val selectedClipIds = remember { mutableStateListOf<String>() }
    var pendingZipFile by remember { mutableStateOf<File?>(null) }

    val attachVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAttachLocalVideo(uri)
    }

    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destUri ->
        val srcZip = pendingZipFile
        if (destUri != null && srcZip != null && srcZip.exists()) {
            runCatching {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    FileInputStream(srcZip).use { it.copyTo(out) }
                }
            }
            pendingZipFile = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        if (project == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                ClipFlowCard(modifier = Modifier.widthIn(max = 480.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MovieCreation,
                            contentDescription = "No active project",
                            tint = InkCharcoal,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("No Active Project Selected", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Analyze a video on Home or pick a saved project to view & render viral clips.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondarySlate
                        )
                        GoldPrimaryButton(text = "Analyze a Video", onClick = onGoHome)
                    }
                }
            }
            return@Box
        }

        val effectiveVideoUri = localAttachedUri
            ?: project.sourceUrl.takeIf { project.sourceType != "YOUTUBE_URL" && it.isNotBlank() }

        when (clipsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = InkCharcoal)
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    ClipFlowCard { Text(clipsState.message, color = SoftErrorText) }
                }
            }
            is UiState.Success -> {
                val clips = clipsState.data
                LazyColumn(
                    modifier = Modifier.widthIn(max = 680.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Project Summary + Generate Button Card
                    item {
                        ClipFlowCard(containerColor = PureWhite) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = InkCharcoal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${clips.size} AI Clips • ${settings.aspectRatio} • ${settings.captionLanguage} • ${settings.captionStyle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondarySlate
                                    )
                                }
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier.testTag("results_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "Edit generation settings",
                                        tint = InkCharcoal
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = project.aiSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondarySlate
                            )

                            if (effectiveVideoUri.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = InfoBlueBg,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "YouTube URL analyzed! Attach an owned local MP4 file to render real cropped MP4 clips & save to your Phone Gallery.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InfoBlueText
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                attachVideoLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, InfoBlueText.copy(alpha = 0.4f))
                                        ) {
                                            Icon(Icons.Filled.FileUpload, contentDescription = null, tint = InfoBlueText)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Attach Local MP4", color = InfoBlueText)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            if (renderProgress.isRendering) {
                                Surface(
                                    color = WarmChipSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = renderProgress.stageLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = InkCharcoal
                                            )
                                            Text(
                                                text = "${renderProgress.currentClipPercent}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = InkCharcoal
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { renderProgress.currentClipPercent / 100f },
                                            color = InkCharcoal,
                                            trackColor = WarmSandBorder,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            } else {
                                GoldPrimaryButton(
                                    text = stringResource(R.string.generate_viral_clips),
                                    onClick = { onGenerateViralClips(project, clips, effectiveVideoUri) },
                                    modifier = Modifier.testTag("generate_viral_clips_button"),
                                    leadingIcon = {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                    }
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Batch Export Row (Download Selected / Download All ZIP)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val chosen = clips.filter { it.id in selectedClipIds }
                                            .ifEmpty { clips }
                                        onExportZip(chosen) { zipFile ->
                                            pendingZipFile = zipFile
                                            saveZipLauncher.launch("ClipFlow_Selected_Clips.zip")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("download_selected_zip_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, WarmSandBorder)
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = InkCharcoal, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedClipIds.isEmpty()) "Export ZIP" else "Selected (${selectedClipIds.size})",
                                        color = InkCharcoal,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        onExportZip(clips) { zipFile ->
                                            pendingZipFile = zipFile
                                            saveZipLauncher.launch("ClipFlow_All_Clips.zip")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("download_all_zip_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, WarmSandBorder)
                                ) {
                                    Icon(Icons.Filled.FolderZip, contentDescription = null, tint = InkCharcoal, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Download All", color = InkCharcoal, maxLines = 1)
                                }
                            }
                        }
                    }

                    // AI Clip Search Bar
                    item {
                        ClipFlowCard(containerColor = PureWhite, contentPadding = PaddingValues(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("AI Search: e.g. funniest moment or business") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SoftCreamSurface,
                                        unfocusedContainerColor = SoftCreamSurface,
                                        focusedBorderColor = WarmAccentYellow,
                                        unfocusedBorderColor = WarmSandBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_clip_search_input")
                                )
                                IconButton(
                                    onClick = { onSearchClips(searchQuery, clips, project.transcriptText) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PrimaryGoldButton)
                                        .testTag("ai_clip_search_button")
                                ) {
                                    if (searchBusy) {
                                        CircularProgressIndicator(Modifier.size(20.dp), color = InkCharcoal, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Filled.Search, contentDescription = "Search clips", tint = InkCharcoal)
                                    }
                                }
                            }

                            if (searchResults.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                searchResults.forEach { match ->
                                    Surface(
                                        color = WarmChipSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Clip ${match.clipIndex}: ${match.reason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkCharcoal,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Generated Clip Cards
                    items(clips, key = { it.id }) { clip ->
                        val isSelected = clip.id in selectedClipIds
                        val isRegenerating = regeneratingClipId == clip.id
                        ClipFlowCard(
                            containerColor = PureWhite,
                            modifier = Modifier.testTag("clip_card_${clip.clipIndex}")
                        ) {
                            // Top Bar: Checkbox + Clip Number + Viral Score Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isSelected) selectedClipIds.remove(clip.id)
                                            else selectedClipIds.add(clip.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = "Select clip ${clip.clipIndex}",
                                            tint = InkCharcoal
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Clip %02d • %s".format(clip.clipIndex, clip.title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = InkCharcoal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatDurationMs(clip.durationMs)} • ${formatDurationMs(clip.startMs)}–${formatDurationMs(clip.endMs)} • ${clip.aspectRatio} • ${clip.captionLanguage}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondarySlate
                                        )
                                    }
                                }
                                ViralScoreBadge(score = clip.viralScore)
                            }

                            Spacer(Modifier.height(10.dp))

                            // Live Video Preview Box with Hook & Caption Overlay
                            AspectRatioPreviewBox(
                                videoUriString = effectiveVideoUri ?: clip.localFilePath,
                                startMs = if (clip.localFilePath != null) 0L else clip.startMs,
                                endMs = if (clip.localFilePath != null) clip.durationMs else clip.endMs,
                                aspectRatio = clip.aspectRatio,
                                hookText = clip.hook,
                                captionText = clip.translatedCaptionsText ?: clip.captionsText,
                                captionColorHex = clip.captionColorHex,
                                captionStyle = clip.captionStyle,
                                captionPositionY = clip.captionPositionY,
                                brandName = brandName
                            )

                            Spacer(Modifier.height(12.dp))

                            // AI Selection Reason
                            Surface(
                                color = SoftCreamSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, WarmSandBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Why AI selected this (${clip.hookType} Hook)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = InkCharcoal
                                    )
                                    Text(
                                        text = clip.scoreReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondarySlate
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Quality Report Pills
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Hook ${clip.qualityHook}%" to true,
                                    "Captions ${clip.qualityCaption}%" to true,
                                    "Framing ${clip.qualityFraming}%" to true,
                                    "Audio ${clip.qualityAudio}%" to false,
                                    "Context ${clip.qualityContext}%" to false
                                ).forEach { (label, highlight) ->
                                    Surface(
                                        color = if (highlight) WarmChipSurface else SoftCreamSurface,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, WarmSandBorder)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkCharcoal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (clip.gallerySaved) {
                                Spacer(Modifier.height(8.dp))
                                Surface(color = ViralMintBg, shape = RoundedCornerShape(10.dp)) {
                                    Text(
                                        text = "Saved to Phone Gallery (ClipFlow AI/${clip.platformPreset})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ViralMintText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Primary Actions: Save to Gallery & Edit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoldPrimaryButton(
                                    text = stringResource(R.string.save_to_gallery),
                                    onClick = { onSaveToGallery(project, clip) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_gallery_btn_${clip.clipIndex}"),
                                    leadingIcon = {
                                        Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )

                                OutlinedButton(
                                    onClick = { onEditClip(clip) },
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("edit_clip_btn_${clip.clipIndex}"),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, WarmSandBorder)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit clip", tint = InkCharcoal)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit", color = InkCharcoal)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Secondary Actions: Regenerate, Share, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (!isRegenerating) {
                                            onRegenerateClip(clip, settings.captionLanguage, "Curiosity")
                                        }
                                    },
                                    label = {
                                        Text(if (isRegenerating) "Regenerating…" else "Regenerate Hook & Captions")
                                    },
                                    leadingIcon = {
                                        if (isRegenerating) {
                                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = InkCharcoal)
                                        } else {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = SoftCreamSurface)
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            val file = clip.localFilePath?.let { File(it) }?.takeIf { it.exists() }
                                            if (file != null) {
                                                val contentUri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "video/mp4"
                                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                                    putExtra(Intent.EXTRA_TEXT, "${clip.title}\n${clip.hashtags.orEmpty()}")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Clip"))
                                            } else {
                                                val textShare = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "${clip.hook}\n\n${clip.captionsText}\n${clip.hashtags.orEmpty()}")
                                                }
                                                context.startActivity(Intent.createChooser(textShare, "Share Clip Copy"))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "Share clip", tint = InkCharcoal)
                                    }
                                    IconButton(onClick = { onDeleteClip(project.id, clip.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete clip", tint = SoftErrorText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
