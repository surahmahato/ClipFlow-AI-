package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.PrimaryGoldButton
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.ViralMintBg
import com.example.ui.theme.ViralMintText
import com.example.ui.theme.WarmAccentYellow
import com.example.ui.theme.WarmSandBorder

@Composable
fun ClipFlowTopHeader(
    planTier: String,
    userName: String,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(WarmAccentYellow)
                    .border(1.dp, WarmSandBorder, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MovieCreation,
                    contentDescription = "ClipFlow AI logo",
                    tint = InkCharcoal,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column {
                Text(
                    text = "CLIPFLOW AI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = InkCharcoal
                )
                Text(
                    text = userName.take(24),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = WarmAccentYellow.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, WarmSandBorder)
            ) {
                Text(
                    text = planTier,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkCharcoal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            IconButton(
                onClick = onSignOutClick,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign out",
                    tint = InkCharcoal
                )
            }
        }
    }
}

@Composable
fun ClipFlowCard(
    modifier: Modifier = Modifier,
    containerColor: Color = PureWhite,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, WarmSandBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GoldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGoldButton,
            contentColor = InkCharcoal,
            disabledContainerColor = WarmSandBorder,
            disabledContentColor = SecondarySlate
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun ViralScoreBadge(
    score: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ViralMintBg,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = "$score/100 score",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ViralMintText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AspectRatioPreviewBox(
    videoUriString: String?,
    startMs: Long,
    endMs: Long,
    aspectRatio: String,
    hookText: String,
    captionText: String,
    captionColorHex: String,
    captionStyle: String,
    captionPositionY: Double,
    brandName: String,
    modifier: Modifier = Modifier
) {
    val ratioFloat = when (aspectRatio) {
        "16:9" -> 16f / 9f
        "1:1" -> 1f
        "4:5" -> 4f / 5f
        else -> 9f / 16f
    }
    val accentColor = remember(captionColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(captionColorHex)) }
            .getOrDefault(WarmAccentYellow)
    }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (aspectRatio == "16:9") 1f else 0.68f)
                .aspectRatio(ratioFloat)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1F1F1F))
                .border(1.5.dp, WarmSandBorder, RoundedCornerShape(20.dp))
        ) {
            if (!videoUriString.isNullOrBlank()) {
                val exoPlayer = remember(videoUriString, startMs, endMs) {
                    ExoPlayer.Builder(context).build().apply {
                        val item = MediaItem.Builder()
                            .setUri(Uri.parse(videoUriString))
                            .setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                                    .setEndPositionMs(endMs.coerceAtLeast(startMs + 1000L))
                                    .build()
                            )
                            .build()
                        setMediaItem(item)
                        repeatMode = Player.REPEAT_MODE_ONE
                        prepare()
                        playWhenReady = false
                    }
                }
                DisposableEffect(exoPlayer) {
                    onDispose { exoPlayer.release() }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view -> view.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Visual preview card when analyzing URL without local MP4 stream
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CreamBackground.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(WarmAccentYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Preview frame",
                                tint = InkCharcoal,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "Attach local MP4 for live playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Live Brand Badge Overlay
            if (brandName.isNotBlank()) {
                Surface(
                    color = InkCharcoal.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = brandName.take(20),
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Live Hook Overlay
            if (hookText.isNotBlank()) {
                Surface(
                    color = accentColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 42.dp, start = 14.dp, end = 14.dp)
                ) {
                    Text(
                        text = hookText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = InkCharcoal,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Live Caption Overlay positioned dynamically by captionPositionY
            if (captionText.isNotBlank()) {
                val verticalBias = ((captionPositionY.toFloat() - 0.5f) * 2f).coerceIn(-0.85f, 0.85f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 20.dp),
                    contentAlignment = androidx.compose.ui.BiasAlignment(0f, verticalBias)
                ) {
                    val captionBg = if (captionStyle == "Minimal Subtitles") Color.Transparent else InkCharcoal.copy(alpha = 0.78f)
                    val textColor = if (captionStyle == "Clean White" || captionStyle == "Minimal Subtitles") PureWhite else accentColor
                    Surface(
                        color = captionBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = captionText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%02d:%02d".format(min, sec)
}
