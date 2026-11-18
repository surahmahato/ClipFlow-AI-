package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.ClipTemplate
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ClipFlowCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SoftErrorText
import com.example.ui.theme.ViralMintBg
import com.example.ui.theme.ViralMintText
import com.example.ui.theme.WarmAccentYellow
import com.example.ui.theme.WarmChipSurface
import com.example.ui.theme.WarmSandBorder

@Composable
fun TemplatesBrandKitScreen(
    userProfileState: UiState<UserProfile?>,
    templatesState: UiState<List<ClipTemplate>>,
    onSaveBrandKit: (String, String, String, String?, String?) -> Unit,
    onSaveTemplate: (String) -> Unit,
    onApplyTemplate: (ClipTemplate) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (userProfileState as? UiState.Success)?.data
    var brandName by remember(profile?.brandName) { mutableStateOf(profile?.brandName ?: "ClipFlow Studio") }
    var brandColorHex by remember(profile?.brandPrimaryHex) { mutableStateOf(profile?.brandPrimaryHex ?: "#F4D96B") }
    var introTag by remember(profile?.brandIntroTag) { mutableStateOf(profile?.brandIntroTag.orEmpty()) }
    var outroTag by remember(profile?.brandOutroTag) { mutableStateOf(profile?.brandOutroTag.orEmpty()) }
    var newTemplateName by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        if (profile != null) {
            brandName = profile.brandName
            brandColorHex = profile.brandPrimaryHex
            introTag = profile.brandIntroTag.orEmpty()
            outroTag = profile.brandOutroTag.orEmpty()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand Kit Card
            item {
                ClipFlowCard(containerColor = PureWhite) {
                    Text("Brand Kit", style = MaterialTheme.typography.headlineSmall, color = InkCharcoal)
                    Text(
                        "Watermark name, accent color, and tags applied automatically to exported clips.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlate
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = brandName,
                        onValueChange = { brandName = it },
                        label = { Text("Brand / Channel Watermark") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("brand_name_input")
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Brand Accent:", style = MaterialTheme.typography.bodySmall, color = InkCharcoal)
                        ClipFlowCatalog.captionColors.forEach { (hex, _) ->
                            val swatch = Color(android.graphics.Color.parseColor(hex))
                            val sel = brandColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(if (sel) 2.5.dp else 1.dp, if (sel) InkCharcoal else WarmSandBorder, CircleShape)
                                    .clickable { brandColorHex = hex }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = introTag,
                        onValueChange = { introTag = it },
                        label = { Text("Intro Tagline (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = outroTag,
                        onValueChange = { outroTag = it },
                        label = { Text("Outro CTA (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    GoldPrimaryButton(
                        text = "Save Brand Kit",
                        onClick = {
                            onSaveBrandKit(
                                brandName,
                                brandColorHex,
                                "Space Grotesk",
                                introTag.takeIf { it.isNotBlank() },
                                outroTag.takeIf { it.isNotBlank() }
                            )
                        },
                        modifier = Modifier.testTag("save_brand_kit_button")
                    )
                }
            }

            // Saved Templates Section
            item {
                ClipFlowCard(containerColor = PureWhite) {
                    Text("Saved Clip Templates", style = MaterialTheme.typography.titleLarge, color = InkCharcoal)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTemplateName,
                            onValueChange = { newTemplateName = it },
                            placeholder = { Text("Preset name (e.g. Nepali Shorts)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("new_template_input")
                        )
                        IconButton(
                            onClick = {
                                if (newTemplateName.isNotBlank()) {
                                    onSaveTemplate(newTemplateName.trim())
                                    newTemplateName = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(WarmAccentYellow)
                                .testTag("save_template_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Save template", tint = InkCharcoal)
                        }
                    }
                }
            }

            val templates = (templatesState as? UiState.Success)?.data.orEmpty()
            items(templates, key = { it.id }) { tpl ->
                ClipFlowCard(containerColor = PureWhite, contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onApplyTemplate(tpl) }
                        ) {
                            Text(tpl.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = InkCharcoal)
                            Text(
                                "${tpl.aspectRatio} • ${tpl.captionStyle} • ${tpl.captionLanguage} • ${tpl.platformPreset}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondarySlate
                            )
                        }
                        IconButton(onClick = { onDeleteTemplate(tpl.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete template", tint = SoftErrorText)
                        }
                    }
                }
            }

            // Subscription Plans Overview (Transparent, no fake payment checkout)
            item {
                ClipFlowCard(containerColor = PureWhite) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = InkCharcoal)
                        Text("ClipFlow AI Plans", style = MaterialTheme.typography.titleLarge, color = InkCharcoal)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Current Plan: ${profile?.planTier ?: "FREE"} • Clips Generated: ${profile?.clipsGeneratedCount ?: 0L}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlate
                    )
                    Spacer(Modifier.height(12.dp))

                    PlanTierCard(
                        tier = "FREE",
                        summary = "Standard clipping, 9:16 exports, core caption templates, MediaStore gallery saving.",
                        active = (profile?.planTier ?: "FREE") == "FREE",
                        onClick = { onNotify("You are on the FREE plan.") }
                    )
                    Spacer(Modifier.height(8.dp))
                    PlanTierCard(
                        tier = "PRO",
                        summary = "Higher limits, HD export presets, multilingual captions, and batch ZIP archives.",
                        active = profile?.planTier == "PRO",
                        onClick = { onNotify("Configure Google Play Billing credentials to activate PRO upgrades.") }
                    )
                    Spacer(Modifier.height(8.dp))
                    PlanTierCard(
                        tier = "PREMIUM",
                        summary = "Max limits, AI dubbing workflows, custom Brand Kit automation & 4K export when supported.",
                        active = profile?.planTier == "PREMIUM",
                        onClick = { onNotify("Configure Google Play Billing credentials to activate PREMIUM upgrades.") }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanTierCard(
    tier: String,
    summary: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) WarmChipSurface else CreamBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (active) WarmAccentYellow else WarmSandBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(tier, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = InkCharcoal)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = SecondarySlate)
            }
            if (active) {
                Surface(color = ViralMintBg, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = ViralMintText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
