package com.example.ui.screens

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ClipFlowCatalog
import com.example.data.model.GenerationSettings
import com.example.data.model.SourceInfo
import com.example.ui.components.ClipFlowCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftCreamSurface
import com.example.ui.theme.SoftErrorBg
import com.example.ui.theme.SoftErrorText
import com.example.ui.theme.ViralMintBg
import com.example.ui.theme.ViralMintText
import com.example.ui.theme.WarmAccentYellow
import com.example.ui.theme.WarmChipSurface
import com.example.ui.theme.WarmSandBorder
import com.example.ui.viewmodel.AnalysisUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeCreateScreen(
    selectedSource: SourceInfo?,
    settings: GenerationSettings,
    analysisState: AnalysisUiState,
    onLocalVideoPicked: (Uri) -> Unit,
    onUpdateSettings: ((GenerationSettings) -> GenerationSettings) -> Unit,
    onAnalyzeClicked: (url: String, transcript: String, copilot: String) -> Unit,
    onOpenSettingsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by rememberSaveable { mutableStateOf("") }
    var copilotInput by rememberSaveable { mutableStateOf("") }
    var transcriptInput by rememberSaveable { mutableStateOf("") }
    var showTranscriptBox by rememberSaveable { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            urlInput = ""
            onLocalVideoPicked(uri)
        }
    }

    val documentVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            urlInput = ""
            onLocalVideoPicked(uri)
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Headline Card
            ClipFlowCard(containerColor = PureWhite) {
                Text(
                    text = stringResource(R.string.hero_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkCharcoal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondarySlate
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Rounded URL Input + Upload Icon
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.url_placeholder),
                            color = SecondarySlate
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "Video URL",
                            tint = InkCharcoal
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.testTag("upload_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = "Upload video from device",
                                tint = InkCharcoal
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmAccentYellow,
                        unfocusedBorderColor = WarmSandBorder,
                        focusedContainerColor = SoftCreamSurface,
                        unfocusedContainerColor = SoftCreamSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selected Local Video or YouTube Preview Banner
                if (selectedSource != null) {
                    Surface(
                        color = ViralMintBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!selectedSource.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = selectedSource.thumbnailUrl,
                                    contentDescription = "Video thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Source ready",
                                    tint = ViralMintText
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSource.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkCharcoal,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${selectedSource.sourceType} • ${formatDurationMs(selectedSource.durationMs)} • ${if (selectedSource.canRenderMp4Locally) "MP4 Export Ready" else "AI Script & Hook Mode"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ViralMintText
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Upload & Settings Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("upload_video_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WarmSandBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = null,
                            tint = InkCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Video", color = InkCharcoal)
                    }

                    OutlinedButton(
                        onClick = onOpenSettingsSheet,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_settings_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WarmSandBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = InkCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${settings.aspectRatio} • ${settings.clipCount} clips", color = InkCharcoal)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Or browse files via system picker",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlate,
                    modifier = Modifier
                        .clickable { documentVideoLauncher.launch(arrayOf("video/*")) }
                        .padding(vertical = 4.dp)
                        .testTag("browse_video_files_link")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // AI Viral Clipping Copilot Input
                OutlinedTextField(
                    value = copilotInput,
                    onValueChange = { copilotInput = it },
                    label = { Text("AI Viral Clipping Copilot (Optional)") },
                    placeholder = { Text(stringResource(R.string.copilot_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Copilot",
                            tint = InkCharcoal
                        )
                    },
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmAccentYellow,
                        unfocusedBorderColor = WarmSandBorder,
                        focusedContainerColor = SoftCreamSurface,
                        unfocusedContainerColor = SoftCreamSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copilot_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Copilot Quick Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Strongest hooks",
                        "Most emotional moments",
                        "Make 5 Nepali captions",
                        "Educational insights"
                    ).forEach { suggestion ->
                        Surface(
                            color = WarmChipSurface,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, WarmSandBorder),
                            modifier = Modifier.clickable { copilotInput = suggestion }
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkCharcoal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Video Category Selector
                Text(
                    text = "Video Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = InkCharcoal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClipFlowCatalog.categories.forEach { cat ->
                        val selected = settings.videoCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateSettings { it.copy(videoCategory = cat) } },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarmAccentYellow,
                                selectedLabelColor = InkCharcoal
                            )
                        )
                    }
                }

                // Optional Transcript / Context toggle
                Text(
                    text = if (showTranscriptBox) "Hide transcript / context notes" else "+ Add transcript or topic notes (optional for URLs)",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkCharcoal,
                    modifier = Modifier
                        .clickable { showTranscriptBox = !showTranscriptBox }
                        .padding(vertical = 8.dp)
                )
                if (showTranscriptBox) {
                    OutlinedTextField(
                        value = transcriptInput,
                        onValueChange = { transcriptInput = it },
                        placeholder = { Text("Paste transcript or key talking points for even sharper AI hook matching…") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transcript_context_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (analysisState is AnalysisUiState.Error) {
                    Surface(
                        color = SoftErrorBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = analysisState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftErrorText,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (analysisState is AnalysisUiState.Running) {
                    Surface(
                        color = WarmChipSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = InkCharcoal,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = analysisState.stage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkCharcoal
                            )
                        }
                    }
                } else {
                    GoldPrimaryButton(
                        text = stringResource(R.string.analyze_video),
                        onClick = { onAnalyzeClicked(urlInput, transcriptInput, copilotInput) },
                        modifier = Modifier.testTag("analyze_video_button"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            // Feature Highlights Grid
            Text(
                text = "Everything Included in ClipFlow AI",
                style = MaterialTheme.typography.titleMedium,
                color = InkCharcoal,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                FeatureMiniCard(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Smart Viral Clipping",
                    subtitle = "AI scores hook, story & payoff 0–100",
                    modifier = Modifier.weight(1f)
                )
                FeatureMiniCard(
                    icon = Icons.Filled.Face,
                    title = "AI Face Tracking",
                    subtitle = "Auto centers speaker in 9:16",
                    modifier = Modifier.weight(1f)
                )
                FeatureMiniCard(
                    icon = Icons.Filled.ClosedCaption,
                    title = "Dynamic Captions",
                    subtitle = "9 customizable animated styles",
                    modifier = Modifier.weight(1f)
                )
                FeatureMiniCard(
                    icon = Icons.Filled.Translate,
                    title = "13+ Languages",
                    subtitle = "English, Nepali, Hindi, Spanish & more",
                    modifier = Modifier.weight(1f)
                )
                FeatureMiniCard(
                    icon = Icons.Filled.HighQuality,
                    title = "On-Device MP4 Render",
                    subtitle = "Real Media3 crop & burned captions",
                    modifier = Modifier.weight(1f)
                )
                FeatureMiniCard(
                    icon = Icons.Filled.FileUpload,
                    title = "Direct Gallery Save",
                    subtitle = "Saves straight to ClipFlow AI album",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureMiniCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = PureWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, WarmSandBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmChipSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = InkCharcoal,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = InkCharcoal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondarySlate
            )
        }
    }
}
