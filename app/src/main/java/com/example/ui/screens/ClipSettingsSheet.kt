package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClipFlowCatalog
import com.example.data.model.GenerationSettings
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.PrimaryGoldButton
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftCreamSurface
import com.example.ui.theme.WarmAccentYellow
import com.example.ui.theme.WarmSandBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipSettingsSheet(
    settings: GenerationSettings,
    onUpdateSettings: ((GenerationSettings) -> GenerationSettings) -> Unit,
    onDismiss: () -> Unit,
    primaryButtonLabel: String = "Apply Generation Settings",
    onPrimaryAction: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Video Generation Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = InkCharcoal
            )

            // A. Video Format
            SettingLabel("A. Video Format")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.aspectRatios.forEach { (ratio, dims) ->
                    FilterChip(
                        selected = settings.aspectRatio == ratio,
                        onClick = { onUpdateSettings { it.copy(aspectRatio = ratio) } },
                        label = { Text("$ratio ($dims)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }

            // B. Number of Clips
            SettingLabel("B. Number of Clips")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.clipCounts.forEach { count ->
                    FilterChip(
                        selected = settings.clipCount == count,
                        onClick = { onUpdateSettings { it.copy(clipCount = count) } },
                        label = { Text("$count clips") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }

            // C. Video Duration
            SettingLabel("C. Target Clip Duration")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.durationPresets.forEach { preset ->
                    FilterChip(
                        selected = settings.durationPreset == preset,
                        onClick = { onUpdateSettings { it.copy(durationPreset = preset) } },
                        label = { Text(preset) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }

            // D. Clipping Intensity
            SettingLabel("D. Clipping Intensity")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.intensities.forEach { intensity ->
                    FilterChip(
                        selected = settings.intensityPreset == intensity,
                        onClick = { onUpdateSettings { it.copy(intensityPreset = intensity) } },
                        label = { Text(intensity) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }

            // E. Caption Language & Mode
            SettingLabel("E. Caption Language")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.languages.forEach { lang ->
                    FilterChip(
                        selected = settings.captionLanguage == lang,
                        onClick = {
                            onUpdateSettings {
                                it.copy(
                                    captionLanguage = lang,
                                    translatedMode = lang != "English"
                                )
                            }
                        },
                        label = { Text(lang) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }
            ToggleRow(
                title = "Translated Captions Mode",
                subtitle = "Translate original speech into ${settings.captionLanguage}",
                checked = settings.translatedMode,
                onCheckedChange = { v -> onUpdateSettings { it.copy(translatedMode = v) } }
            )

            // F. Caption Style & Color
            SettingLabel("F. Caption Visual Template")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.captionStyles.forEach { style ->
                    FilterChip(
                        selected = settings.captionStyle == style,
                        onClick = { onUpdateSettings { it.copy(captionStyle = style) } },
                        label = { Text(style) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Highlight:", style = MaterialTheme.typography.bodyMedium, color = InkCharcoal)
                ClipFlowCatalog.captionColors.forEach { (hex, label) ->
                    val swatchColor = Color(android.graphics.Color.parseColor(hex))
                    val selected = settings.captionColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) InkCharcoal else WarmSandBorder,
                                shape = CircleShape
                            )
                            .clickable { onUpdateSettings { it.copy(captionColorHex = hex) } }
                            .testTag("color_swatch_$label")
                    )
                }
            }

            Column {
                Text(
                    text = "Caption Vertical Position (${(settings.captionPositionY * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlate
                )
                Slider(
                    value = settings.captionPositionY.toFloat(),
                    onValueChange = { y -> onUpdateSettings { it.copy(captionPositionY = y.toDouble()) } },
                    valueRange = 0.2f..0.88f,
                    colors = SliderDefaults.colors(
                        thumbColor = InkCharcoal,
                        activeTrackColor = PrimaryGoldButton
                    )
                )
            }

            // G. AI Face Tracking, Multi-Speaker & Sound Settings
            SettingLabel("G. AI Framing & Audio Enhancement")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.speakerLayouts.forEach { layout ->
                    FilterChip(
                        selected = settings.speakerLayout == layout,
                        onClick = { onUpdateSettings { it.copy(speakerLayout = layout) } },
                        label = { Text(layout) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClipFlowCatalog.platformPresets.forEach { preset ->
                    FilterChip(
                        selected = settings.platformPreset == preset,
                        onClick = { onUpdateSettings { it.copy(platformPreset = preset) } },
                        label = { Text(preset) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarmAccentYellow,
                            selectedLabelColor = InkCharcoal
                        )
                    )
                }
            }

            ToggleRow(
                title = "AI Face Tracking Reframing",
                subtitle = "Keep active speaker centered during 9:16 crop",
                checked = settings.faceTrackingEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(faceTrackingEnabled = v) } }
            )
            ToggleRow(
                title = "AI Voice Enhancement",
                subtitle = "Balance vocal clarity & loudness levels",
                checked = settings.voiceEnhanceEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(voiceEnhanceEnabled = v) } }
            )
            ToggleRow(
                title = "Noise Reduction",
                subtitle = "Suppress background hum & room echo",
                checked = settings.noiseReductionEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(noiseReductionEnabled = v) } }
            )
            ToggleRow(
                title = "Auto Silence Removal",
                subtitle = "Trim long dead pauses smoothly",
                checked = settings.silenceRemovalEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(silenceRemovalEnabled = v) } }
            )
            ToggleRow(
                title = "Background Music & Sound Effects",
                subtitle = "Subtle whoosh/pop transitions",
                checked = settings.sfxEnabled || settings.musicEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(sfxEnabled = v, musicEnabled = v) } }
            )

            GoldPrimaryButton(
                text = primaryButtonLabel,
                onClick = {
                    onPrimaryAction?.invoke()
                    onDismiss()
                },
                modifier = Modifier.testTag("apply_settings_button")
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = InkCharcoal
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoftCreamSurface)
            .border(BorderStroke(1.dp, WarmSandBorder), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = InkCharcoal)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondarySlate)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = InkCharcoal,
                checkedTrackColor = WarmAccentYellow
            )
        )
    }
}
