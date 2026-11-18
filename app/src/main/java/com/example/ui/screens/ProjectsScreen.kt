package com.example.ui.screens

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.data.model.VideoProject
import com.example.ui.components.ClipFlowCard
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.formatDurationMs
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
fun ProjectsScreen(
    projectsState: UiState<List<VideoProject>>,
    onOpenProject: (VideoProject) -> Unit,
    onRenameProject: (projectId: String, title: String, folder: String) -> Unit,
    onDeleteProject: (projectId: String) -> Unit,
    onGoCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFolder by remember { mutableStateOf("All") }
    var editingProject by remember { mutableStateOf<VideoProject?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        when (projectsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = InkCharcoal)
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    ClipFlowCard {
                        Text(projectsState.message, color = SoftErrorText)
                    }
                }
            }
            is UiState.Success -> {
                val allProjects = projectsState.data
                val folders = remember(allProjects) {
                    listOf("All") + allProjects.map { it.folderName }.distinct()
                }
                val filtered = remember(allProjects, selectedFolder) {
                    if (selectedFolder == "All") allProjects
                    else allProjects.filter { it.folderName == selectedFolder }
                }

                LazyColumn(
                    modifier = Modifier.widthIn(max = 680.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "My Projects",
                            style = MaterialTheme.typography.headlineMedium,
                            color = InkCharcoal
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            folders.forEach { folder ->
                                FilterChip(
                                    selected = selectedFolder == folder,
                                    onClick = { selectedFolder = folder },
                                    label = { Text(folder) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarmAccentYellow,
                                        selectedLabelColor = InkCharcoal
                                    )
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            ClipFlowCard {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(WarmChipSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VideoLibrary,
                                            contentDescription = "No projects yet",
                                            tint = InkCharcoal,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = "No clipping projects yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = InkCharcoal
                                    )
                                    Text(
                                        text = "Paste a video URL or upload a clip from your phone to analyze viral moments.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondarySlate
                                    )
                                    GoldPrimaryButton(
                                        text = "Create First Project",
                                        onClick = onGoCreate,
                                        modifier = Modifier.testTag("empty_projects_create_btn")
                                    )
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { project ->
                            ClipFlowCard(
                                containerColor = PureWhite,
                                modifier = Modifier
                                    .clickable { onOpenProject(project) }
                                    .testTag("project_card_${project.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = if (project.status == "COMPLETED") ViralMintBg else WarmChipSurface,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = project.status,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (project.status == "COMPLETED") ViralMintText else InkCharcoal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = "${project.aspectRatio} • ${formatDurationMs(project.durationMs)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SecondarySlate
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = project.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = InkCharcoal,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = project.aiSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondarySlate,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FolderOpen,
                                                contentDescription = null,
                                                tint = SecondarySlate,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${project.folderName} • ${project.captionLanguage} • ${project.captionStyle}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SecondarySlate
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { editingProject = project }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Rename project",
                                                tint = InkCharcoal
                                            )
                                        }
                                        IconButton(onClick = { onDeleteProject(project.id) }) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete project",
                                                tint = SoftErrorText
                                            )
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

    editingProject?.let { target ->
        var newTitle by remember(target.id) { mutableStateOf(target.title) }
        var newFolder by remember(target.id) { mutableStateOf(target.folderName) }
        AlertDialog(
            onDismissRequest = { editingProject = null },
            title = { Text("Organize Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Project Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newFolder,
                        onValueChange = { newFolder = it },
                        label = { Text("Folder Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameProject(target.id, newTitle, newFolder)
                    editingProject = null
                }) {
                    Text("Save", color = InkCharcoal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProject = null }) {
                    Text("Cancel", color = SecondarySlate)
                }
            }
        )
    }
}
