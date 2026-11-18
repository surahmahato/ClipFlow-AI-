package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.firebase.authStateFlow
import com.example.core.firebase.configureAppCheck
import com.example.core.firebase.signOut
import com.example.data.model.UiState
import com.example.data.repository.ClipFlowAiService
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.VideoProcessingEngine
import com.example.data.repository.VideoSourceValidator
import com.example.ui.components.ClipFlowTopHeader
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClipResultsScreen
import com.example.ui.screens.ClipSettingsSheet
import com.example.ui.screens.HomeCreateScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.TemplatesBrandKitScreen
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.theme.ClipFlowTheme
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.WarmAccentYellow
import com.example.ui.viewmodel.AnalysisUiState
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable data object HomeCreateRoute
@Serializable data object ClipResultsRoute
@Serializable data object ProjectsRoute
@Serializable data object TemplatesBrandKitRoute
@Serializable data object VideoEditorRoute

private data class TopTabItem<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check with launch intent before UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            ClipFlowTheme {
                ClipFlowAppRoot()
            }
        }
    }
}

@Composable
fun ClipFlowAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            ClipFlowAuthenticatedWorkspace(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "Creator",
                userEmail = user.email
            )
        }
    }
}

@Composable
private fun ClipFlowAuthenticatedWorkspace(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?
) {
    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    firestoreRepository = FirestoreRepository(db),
                    aiService = ClipFlowAiService(),
                    sourceValidator = VideoSourceValidator(app),
                    videoProcessor = VideoProcessingEngine(app)
                )
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val templatesState by viewModel.templatesState.collectAsStateWithLifecycle()
    val activeProjectId by viewModel.activeProjectId.collectAsStateWithLifecycle()
    val activeClipsState by viewModel.activeClipsState.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val generationSettings by viewModel.generationSettings.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val renderProgress by viewModel.renderProgress.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val editingClip by viewModel.editingClip.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val aiAssistantBusy by viewModel.aiAssistantBusy.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchBusy by viewModel.searchBusy.collectAsStateWithLifecycle()
    val regeneratingClipId by viewModel.regeneratingClipId.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    val userProfile = (userProfileState as? UiState.Success)?.data
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val activeProject = remember(projects, activeProjectId) {
        projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()
    }

    LaunchedEffect(projects, activeProjectId) {
        if (activeProjectId == null && projects.isNotEmpty()) {
            viewModel.selectProject(projects.first().id)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isInEditor = currentDest?.hasRoute<VideoEditorRoute>() == true

    val tabs = remember {
        listOf(
            TopTabItem(HomeCreateRoute, "Create", Icons.Filled.Home, Icons.Outlined.Home, "nav_create"),
            TopTabItem(ClipResultsRoute, "Clips", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_clips"),
            TopTabItem(ProjectsRoute, "Projects", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen, "nav_projects"),
            TopTabItem(TemplatesBrandKitRoute, "Studio", Icons.Filled.Palette, Icons.Outlined.Palette, "nav_studio")
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && !isInEditor) {
                NavigationRail(
                    containerColor = PureWhite,
                    modifier = Modifier.statusBarsPadding().navigationBarsPadding()
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDest?.hasRoute(tab.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = InkCharcoal,
                                selectedTextColor = InkCharcoal,
                                indicatorColor = WarmAccentYellow,
                                unselectedIconColor = SecondarySlate,
                                unselectedTextColor = SecondarySlate
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = CreamBackground,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (!isInEditor) {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            ClipFlowTopHeader(
                                planTier = userProfile?.planTier ?: "FREE",
                                userName = userProfile?.displayName ?: userDisplayName,
                                onSignOutClick = { signOut(credentialManager, scope) }
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!useRail && !isInEditor) {
                        NavigationBar(
                            containerColor = PureWhite,
                            tonalElevation = 2.dp
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = InkCharcoal,
                                        selectedTextColor = InkCharcoal,
                                        indicatorColor = WarmAccentYellow,
                                        unselectedIconColor = SecondarySlate,
                                        unselectedTextColor = SecondarySlate
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeCreateRoute
                    ) {
                        composable<HomeCreateRoute> {
                            HomeCreateScreen(
                                selectedSource = selectedSource,
                                settings = generationSettings,
                                analysisState = analysisState,
                                onLocalVideoPicked = viewModel::onLocalVideoPicked,
                                onUpdateSettings = viewModel::updateSettings,
                                onAnalyzeClicked = { url, transcript, copilot ->
                                    viewModel.analyzeUrlOrSelectedSource(
                                        urlInput = url,
                                        transcriptInput = transcript,
                                        copilotInstruction = copilot,
                                        onProjectReady = {
                                            navController.navigate(ClipResultsRoute) {
                                                launchSingleTop = true
                                            }
                                            if (analysisState is AnalysisUiState.Done) {
                                                viewModel.resetAnalysisState()
                                            }
                                        }
                                    )
                                },
                                onOpenSettingsSheet = { showSettingsSheet = true }
                            )
                        }

                        composable<ClipResultsRoute> {
                            ClipResultsScreen(
                                project = activeProject,
                                clipsState = activeClipsState,
                                settings = generationSettings,
                                brandName = userProfile?.brandName ?: "ClipFlow Studio",
                                localAttachedUri = selectedSource?.localVideoUriString,
                                renderProgress = renderProgress,
                                searchResults = searchResults,
                                searchBusy = searchBusy,
                                regeneratingClipId = regeneratingClipId,
                                onAttachLocalVideo = viewModel::onLocalVideoPicked,
                                onOpenSettings = { showSettingsSheet = true },
                                onGenerateViralClips = viewModel::applySettingsAndRenderClips,
                                onSearchClips = viewModel::searchClips,
                                onEditClip = { clip ->
                                    viewModel.openClipInEditor(clip)
                                    navController.navigate(VideoEditorRoute)
                                },
                                onSaveToGallery = viewModel::saveClipToPhoneGallery,
                                onRegenerateClip = viewModel::regenerateSingleClip,
                                onDeleteClip = viewModel::deleteClip,
                                onExportZip = viewModel::exportClipsAsZip,
                                onGoHome = {
                                    navController.navigate(HomeCreateRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable<ProjectsRoute> {
                            ProjectsScreen(
                                projectsState = projectsState,
                                onOpenProject = { project ->
                                    viewModel.selectProject(project.id)
                                    navController.navigate(ClipResultsRoute) {
                                        launchSingleTop = true
                                    }
                                },
                                onRenameProject = viewModel::renameProject,
                                onDeleteProject = viewModel::deleteProject,
                                onGoCreate = {
                                    navController.navigate(HomeCreateRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable<TemplatesBrandKitRoute> {
                            TemplatesBrandKitScreen(
                                userProfileState = userProfileState,
                                templatesState = templatesState,
                                onSaveBrandKit = viewModel::saveBrandKit,
                                onSaveTemplate = viewModel::saveCustomTemplate,
                                onApplyTemplate = viewModel::applyTemplate,
                                onDeleteTemplate = viewModel::deleteTemplate,
                                onNotify = viewModel::showToast
                            )
                        }

                        composable<VideoEditorRoute> {
                            VideoEditorScreen(
                                project = activeProject,
                                clip = editingClip,
                                brandName = userProfile?.brandName ?: "ClipFlow Studio",
                                localAttachedUri = selectedSource?.localVideoUriString,
                                canUndo = canUndo,
                                canRedo = canRedo,
                                aiAssistantBusy = aiAssistantBusy,
                                renderProgress = renderProgress,
                                onBack = { navController.popBackStack() },
                                onUpdateClip = viewModel::pushEditorState,
                                onUndo = viewModel::undoEdit,
                                onRedo = viewModel::redoEdit,
                                onSaveChanges = viewModel::saveEditorChanges,
                                onRenderSingleClip = viewModel::renderSingleClip,
                                onSaveToGallery = viewModel::saveClipToPhoneGallery,
                                onRunAiAssistant = viewModel::runAiEditAssistant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        ClipSettingsSheet(
            settings = generationSettings,
            onUpdateSettings = viewModel::updateSettings,
            onDismiss = { showSettingsSheet = false },
            primaryButtonLabel = "Save Settings"
        )
    }
}
