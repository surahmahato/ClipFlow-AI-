package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ClipTemplate
import com.example.data.model.GeneratedClip
import com.example.data.model.GenerationSettings
import com.example.data.model.RenderProgress
import com.example.data.model.SourceInfo
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoProject
import com.example.data.repository.ClipFlowAiService
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.VideoProcessingEngine
import com.example.data.repository.VideoSourceValidator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Running(val stage: String) : AnalysisUiState
    data class Done(val projectId: String, val note: String) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

data class ClipSearchMatch(
    val clipIndex: Int,
    val reason: String
)

class MainViewModel(
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?,
    private val firestoreRepository: FirestoreRepository,
    private val aiService: ClipFlowAiService,
    private val sourceValidator: VideoSourceValidator,
    private val videoProcessor: VideoProcessingEngine
) : ViewModel() {

    init {
        viewModelScope.launch {
            firestoreRepository.ensureUserProfile(currentUserId, userDisplayName, userEmail)
        }
    }

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        firestoreRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(UiState.Error((fsErr ?: error).message ?: "Failed to load profile"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val projectsState: StateFlow<UiState<List<VideoProject>>> =
        firestoreRepository.observeProjects(currentUserId)
            .map<List<VideoProject>, UiState<List<VideoProject>>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(UiState.Error((fsErr ?: error).message ?: "Failed to load projects"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val templatesState: StateFlow<UiState<List<ClipTemplate>>> =
        firestoreRepository.observeTemplates(currentUserId)
            .map<List<ClipTemplate>, UiState<List<ClipTemplate>>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(UiState.Error((fsErr ?: error).message ?: "Failed to load templates"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeClipsState: StateFlow<UiState<List<GeneratedClip>>> =
        _activeProjectId.flatMapLatest { projId ->
            if (projId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                firestoreRepository.observeClips(currentUserId, projId)
                    .map<List<GeneratedClip>, UiState<List<GeneratedClip>>> { UiState.Success(it) }
                    .catch { error ->
                        val fsErr = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsErr == null && error is CancellationException) throw error
                        emit(UiState.Error((fsErr ?: error).message ?: "Failed to load clips"))
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // Source / Input state
    private val _selectedSource = MutableStateFlow<SourceInfo?>(null)
    val selectedSource: StateFlow<SourceInfo?> = _selectedSource.asStateFlow()

    private val _generationSettings = MutableStateFlow(GenerationSettings())
    val generationSettings: StateFlow<GenerationSettings> = _generationSettings.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _renderProgress = MutableStateFlow(RenderProgress())
    val renderProgress: StateFlow<RenderProgress> = _renderProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Editor state with Undo/Redo
    private val _editingClip = MutableStateFlow<GeneratedClip?>(null)
    val editingClip: StateFlow<GeneratedClip?> = _editingClip.asStateFlow()

    private val undoStack = mutableListOf<GeneratedClip>()
    private val redoStack = mutableListOf<GeneratedClip>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _aiAssistantBusy = MutableStateFlow(false)
    val aiAssistantBusy: StateFlow<Boolean> = _aiAssistantBusy.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ClipSearchMatch>>(emptyList())
    val searchResults: StateFlow<List<ClipSearchMatch>> = _searchResults.asStateFlow()

    private val _searchBusy = MutableStateFlow(false)
    val searchBusy: StateFlow<Boolean> = _searchBusy.asStateFlow()

    private val _regeneratingClipId = MutableStateFlow<String?>(null)
    val regeneratingClipId: StateFlow<String?> = _regeneratingClipId.asStateFlow()

    private val _lastZipFile = MutableStateFlow<File?>(null)
    val lastZipFile: StateFlow<File?> = _lastZipFile.asStateFlow()

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun resetAnalysisState() {
        _analysisState.value = AnalysisUiState.Idle
    }

    fun selectProject(projectId: String) {
        _activeProjectId.value = projectId
        _searchResults.value = emptyList()
    }

    fun updateSettings(transform: (GenerationSettings) -> GenerationSettings) {
        _generationSettings.update(transform)
    }

    fun onLocalVideoPicked(uri: Uri) {
        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Running("Inspecting local video metadata...")
            sourceValidator.inspectLocalVideo(uri)
                .onSuccess { info ->
                    _selectedSource.value = info
                    _analysisState.value = AnalysisUiState.Idle
                    _toastMessage.value = "Loaded \"${info.title}\" (${info.durationMs / 1000}s)"
                }
                .onFailure { err ->
                    _analysisState.value = AnalysisUiState.Error(err.localizedMessage ?: "Failed to read video.")
                }
        }
    }

    fun analyzeUrlOrSelectedSource(
        urlInput: String,
        transcriptInput: String,
        copilotInstruction: String,
        onProjectReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentSrc = _selectedSource.value
                val trimmedUrl = urlInput.trim()

                val source: SourceInfo = when {
                    trimmedUrl.isNotBlank() && (currentSrc == null || currentSrc.sourceUrl != trimmedUrl) -> {
                        _analysisState.value = AnalysisUiState.Running("Validating video URL & retrieving metadata...")
                        sourceValidator.validateAndInspectUrl(trimmedUrl).getOrElse { err ->
                            _analysisState.value = AnalysisUiState.Error(err.localizedMessage ?: "Invalid URL.")
                            return@launch
                        }.also { _selectedSource.value = it }
                    }
                    currentSrc != null -> currentSrc
                    else -> {
                        _analysisState.value = AnalysisUiState.Error("Paste a YouTube/MP4 URL or upload a video from your device.")
                        return@launch
                    }
                }

                val settings = _generationSettings.value
                val localUri = source.localVideoUriString

                _analysisState.value = AnalysisUiState.Running("Extracting video frames & audio context...")
                val keyframes = if (!localUri.isNullOrBlank()) {
                    sourceValidator.extractKeyframes(localUri, count = 6)
                } else {
                    emptyList()
                }
                val smallBytes = if (!localUri.isNullOrBlank()) {
                    sourceValidator.readSmallMediaBytesIfUnderLimit(localUri)
                } else {
                    null
                }

                _analysisState.value = AnalysisUiState.Running("Gemini AI analyzing transcript, hooks & viral moments...")
                val aiResult = aiService.analyzeVideo(
                    source = source,
                    settings = settings,
                    userId = currentUserId,
                    keyframes = keyframes,
                    inlineVideoBytes = smallBytes,
                    userTranscriptOrContext = transcriptInput,
                    copilotInstruction = copilotInstruction
                )
                keyframes.forEach { it.recycle() }

                if (aiResult.clips.isEmpty()) {
                    _analysisState.value = AnalysisUiState.Error("No strong clip segments met the quality threshold. Try adding notes or adjusting intensity.")
                    return@launch
                }

                _analysisState.value = AnalysisUiState.Running("Running face tracking & saving project...")
                val trackedClips = aiResult.clips.map { clip ->
                    val faceX = if (!localUri.isNullOrBlank() && settings.faceTrackingEnabled) {
                        sourceValidator.detectFaceFocusX(localUri, clip.startMs, clip.endMs)
                    } else {
                        0.5
                    }
                    clip.copy(faceFocusX = faceX)
                }

                val project = VideoProject(
                    userId = currentUserId,
                    title = aiResult.title,
                    folderName = settings.videoCategory,
                    sourceType = source.sourceType,
                    sourceUrl = source.sourceUrl,
                    thumbnailUrl = source.thumbnailUrl,
                    videoCategory = settings.videoCategory,
                    status = "ANALYZED",
                    durationMs = source.durationMs,
                    transcriptText = aiResult.transcript,
                    aiSummary = aiResult.summary,
                    copilotInstruction = copilotInstruction.takeIf { it.isNotBlank() },
                    aspectRatio = settings.aspectRatio,
                    captionStyle = settings.captionStyle,
                    captionLanguage = settings.captionLanguage,
                    clipCountRequested = settings.clipCount.toLong(),
                    durationPreset = settings.durationPreset,
                    intensityPreset = settings.intensityPreset,
                    voiceEnhanceEnabled = settings.voiceEnhanceEnabled,
                    noiseReductionEnabled = settings.noiseReductionEnabled,
                    musicEnabled = settings.musicEnabled,
                    sfxEnabled = settings.sfxEnabled,
                    silenceRemovalEnabled = settings.silenceRemovalEnabled,
                    faceTrackingEnabled = settings.faceTrackingEnabled,
                    speakerLayout = settings.speakerLayout
                )

                val projectId = firestoreRepository.createProjectWithClips(project, trackedClips)
                    .getOrElse { err ->
                        _analysisState.value = AnalysisUiState.Error(err.localizedMessage ?: "Failed to save project.")
                        return@launch
                    }

                firestoreRepository.incrementClipsGenerated(currentUserId, trackedClips.size.toLong())
                _activeProjectId.value = projectId
                _analysisState.value = AnalysisUiState.Done(projectId, aiResult.thresholdNote)
                onProjectReady(projectId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Analysis pipeline failed", e)
                _analysisState.value = AnalysisUiState.Error(e.localizedMessage ?: "Video analysis failed.")
            }
        }
    }

    fun applySettingsAndRenderClips(
        project: VideoProject,
        clips: List<GeneratedClip>,
        overrideLocalUri: String? = null
    ) {
        viewModelScope.launch {
            val settings = _generationSettings.value
            val updatedProject = project.copy(
                aspectRatio = settings.aspectRatio,
                captionStyle = settings.captionStyle,
                captionLanguage = settings.captionLanguage,
                clipCountRequested = settings.clipCount.toLong(),
                durationPreset = settings.durationPreset,
                intensityPreset = settings.intensityPreset,
                voiceEnhanceEnabled = settings.voiceEnhanceEnabled,
                noiseReductionEnabled = settings.noiseReductionEnabled,
                musicEnabled = settings.musicEnabled,
                sfxEnabled = settings.sfxEnabled,
                silenceRemovalEnabled = settings.silenceRemovalEnabled,
                faceTrackingEnabled = settings.faceTrackingEnabled,
                speakerLayout = settings.speakerLayout,
                status = "RENDERING"
            )
            firestoreRepository.updateProjectSettings(currentUserId, updatedProject)

            val playableUri = overrideLocalUri
                ?: project.sourceUrl.takeIf { project.sourceType != "YOUTUBE_URL" && it.isNotBlank() }

            if (playableUri.isNullOrBlank()) {
                firestoreRepository.updateProjectSettings(
                    currentUserId,
                    updatedProject.copy(status = "ANALYZED")
                )
                _toastMessage.value = "Settings saved! Upload or attach a local MP4 file to render MP4 clips on-device."
                return@launch
            }

            val profile = (userProfileState.value as? UiState.Success)?.data
            val brandName = profile?.brandName ?: "ClipFlow Studio"
            val targetClips = clips.take(settings.clipCount)
            val total = targetClips.size

            _renderProgress.value = RenderProgress(
                isRendering = true,
                currentClipIndex = 1,
                totalClips = total,
                currentClipPercent = 0,
                stageLabel = "Preparing Media3 pipeline..."
            )

            var successCount = 0
            targetClips.forEachIndexed { idx, clip ->
                val syncedClip = clip.copy(
                    aspectRatio = settings.aspectRatio,
                    captionStyle = settings.captionStyle,
                    captionLanguage = settings.captionLanguage,
                    captionColorHex = settings.captionColorHex,
                    captionPositionY = settings.captionPositionY,
                    platformPreset = settings.platformPreset
                )
                firestoreRepository.updateClipEdits(currentUserId, syncedClip)

                _renderProgress.value = RenderProgress(
                    isRendering = true,
                    currentClipIndex = idx + 1,
                    totalClips = total,
                    currentClipPercent = 0,
                    stageLabel = "Rendering Clip ${idx + 1} of $total (${syncedClip.aspectRatio})..."
                )

                val result = videoProcessor.renderClipToMp4(
                    sourceUriString = playableUri,
                    sourceWidth = _selectedSource.value?.width ?: 1920,
                    sourceHeight = _selectedSource.value?.height ?: 1080,
                    clip = syncedClip,
                    brandName = brandName,
                    onProgress = { pct ->
                        _renderProgress.value = _renderProgress.value.copy(currentClipPercent = pct)
                    }
                )
                result.onSuccess { file ->
                    successCount++
                    firestoreRepository.updateClipExportState(
                        userId = currentUserId,
                        projectId = project.id,
                        clipId = clip.id,
                        localFilePath = file.absolutePath,
                        rendered = true,
                        gallerySaved = clip.gallerySaved
                    )
                }.onFailure { err ->
                    Log.e(TAG, "Clip ${clip.clipIndex} render failed", err)
                }
            }

            firestoreRepository.updateProjectSettings(
                currentUserId,
                updatedProject.copy(status = if (successCount > 0) "COMPLETED" else "FAILED")
            )
            _renderProgress.value = RenderProgress()
            _toastMessage.value = if (successCount > 0) {
                "Rendered $successCount MP4 clips ready for Gallery Save & Download!"
            } else {
                "Rendering failed. Verify the video file format is valid MP4/MOV."
            }
        }
    }

    fun renderSingleClip(project: VideoProject, clip: GeneratedClip) {
        viewModelScope.launch {
            val playableUri = _selectedSource.value?.localVideoUriString
                ?: project.sourceUrl.takeIf { project.sourceType != "YOUTUBE_URL" && it.isNotBlank() }
            if (playableUri.isNullOrBlank()) {
                _toastMessage.value = "Attach a local MP4 video first to render this clip."
                return@launch
            }
            val profile = (userProfileState.value as? UiState.Success)?.data
            val brandName = profile?.brandName ?: "ClipFlow Studio"

            _renderProgress.value = RenderProgress(
                isRendering = true,
                currentClipIndex = clip.clipIndex.toInt(),
                totalClips = 1,
                currentClipPercent = 0,
                stageLabel = "Rendering Clip ${clip.clipIndex}..."
            )
            videoProcessor.renderClipToMp4(
                sourceUriString = playableUri,
                sourceWidth = _selectedSource.value?.width ?: 1920,
                sourceHeight = _selectedSource.value?.height ?: 1080,
                clip = clip,
                brandName = brandName,
                onProgress = { pct ->
                    _renderProgress.value = _renderProgress.value.copy(currentClipPercent = pct)
                }
            ).onSuccess { file ->
                firestoreRepository.updateClipExportState(
                    userId = currentUserId,
                    projectId = project.id,
                    clipId = clip.id,
                    localFilePath = file.absolutePath,
                    rendered = true,
                    gallerySaved = clip.gallerySaved
                )
                _editingClip.value = _editingClip.value?.copy(
                    localFilePath = file.absolutePath,
                    rendered = true
                )
                _toastMessage.value = "Clip ${clip.clipIndex} rendered to MP4!"
            }.onFailure { err ->
                _toastMessage.value = "Render error: ${err.localizedMessage}"
            }
            _renderProgress.value = RenderProgress()
        }
    }

    fun saveClipToPhoneGallery(project: VideoProject, clip: GeneratedClip) {
        viewModelScope.launch {
            val existingFile = clip.localFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
            val fileToSave = if (existingFile != null) {
                existingFile
            } else {
                val playableUri = _selectedSource.value?.localVideoUriString
                    ?: project.sourceUrl.takeIf { project.sourceType != "YOUTUBE_URL" && it.isNotBlank() }
                if (playableUri.isNullOrBlank()) {
                    _toastMessage.value = "Attach a local MP4 video to render and save directly to Phone Gallery."
                    return@launch
                }
                val profile = (userProfileState.value as? UiState.Success)?.data
                _renderProgress.value = RenderProgress(
                    isRendering = true,
                    currentClipIndex = 1,
                    totalClips = 1,
                    currentClipPercent = 0,
                    stageLabel = "Rendering MP4 before saving to Gallery..."
                )
                val rendered = videoProcessor.renderClipToMp4(
                    sourceUriString = playableUri,
                    sourceWidth = _selectedSource.value?.width ?: 1920,
                    sourceHeight = _selectedSource.value?.height ?: 1080,
                    clip = clip,
                    brandName = profile?.brandName ?: "ClipFlow Studio",
                    onProgress = { pct -> _renderProgress.value = _renderProgress.value.copy(currentClipPercent = pct) }
                )
                _renderProgress.value = RenderProgress()
                rendered.getOrElse { err ->
                    _toastMessage.value = "Could not render clip: ${err.localizedMessage}"
                    return@launch
                }
            }

            videoProcessor.saveClipToGallery(fileToSave, clip)
                .onSuccess {
                    firestoreRepository.updateClipExportState(
                        userId = currentUserId,
                        projectId = project.id,
                        clipId = clip.id,
                        localFilePath = fileToSave.absolutePath,
                        rendered = true,
                        gallerySaved = true
                    )
                    _toastMessage.value = "Saved to Phone Gallery: Movies/ClipFlow AI/Generated Clips/${clip.platformPreset}"
                }
                .onFailure { err ->
                    _toastMessage.value = "Gallery save failed: ${err.localizedMessage}"
                }
        }
    }

    fun exportClipsAsZip(clips: List<GeneratedClip>, onZipReady: (File) -> Unit) {
        viewModelScope.launch {
            val files = clips.mapNotNull { c -> c.localFilePath?.let { File(it) } }.filter { it.exists() }
            if (files.isEmpty()) {
                _toastMessage.value = "Render at least one MP4 clip first before exporting ZIP."
                return@launch
            }
            videoProcessor.createZipArchive(files)
                .onSuccess { zip ->
                    _lastZipFile.value = zip
                    onZipReady(zip)
                }
                .onFailure { err ->
                    _toastMessage.value = err.localizedMessage ?: "Failed to create ZIP archive."
                }
        }
    }

    fun regenerateSingleClip(clip: GeneratedClip, targetLanguage: String, hookAngle: String) {
        viewModelScope.launch {
            _regeneratingClipId.value = clip.id
            try {
                val updated = aiService.regenerateSingleClip(clip, targetLanguage, hookAngle)
                firestoreRepository.updateClipEdits(currentUserId, updated)
                if (_editingClip.value?.id == clip.id) {
                    pushEditorState(updated)
                }
                _toastMessage.value = "Regenerated Clip ${clip.clipIndex} with fresh $hookAngle hooks!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _toastMessage.value = "Regeneration failed: ${e.localizedMessage}"
            } finally {
                _regeneratingClipId.value = null
            }
        }
    }

    fun searchClips(query: String, clips: List<GeneratedClip>, transcript: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchBusy.value = true
            try {
                val results = aiService.searchClipsInTranscript(query, clips, transcript)
                _searchResults.value = results.map { ClipSearchMatch(it.first, it.second) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _toastMessage.value = "Search failed: ${e.localizedMessage}"
            } finally {
                _searchBusy.value = false
            }
        }
    }

    // Editor lifecycle
    fun openClipInEditor(clip: GeneratedClip) {
        undoStack.clear()
        redoStack.clear()
        _editingClip.value = clip
        _canUndo.value = false
        _canRedo.value = false
    }

    fun pushEditorState(updated: GeneratedClip) {
        _editingClip.value?.let { prev ->
            if (prev != updated) {
                undoStack.add(prev)
                redoStack.clear()
            }
        }
        _editingClip.value = updated
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undoEdit() {
        val prev = undoStack.removeLastOrNull() ?: return
        _editingClip.value?.let { redoStack.add(it) }
        _editingClip.value = prev
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun redoEdit() {
        val next = redoStack.removeLastOrNull() ?: return
        _editingClip.value?.let { undoStack.add(it) }
        _editingClip.value = next
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun saveEditorChanges() {
        val clip = _editingClip.value ?: return
        viewModelScope.launch {
            firestoreRepository.updateClipEdits(currentUserId, clip)
                .onSuccess { _toastMessage.value = "Clip edits saved!" }
                .onFailure { err -> _toastMessage.value = "Save failed: ${err.localizedMessage}" }
        }
    }

    fun runAiEditAssistant(instruction: String) {
        val clip = _editingClip.value ?: return
        if (instruction.isBlank()) return
        viewModelScope.launch {
            _aiAssistantBusy.value = true
            try {
                val delta = aiService.applyAiEditInstruction(clip, instruction)
                val newStart = (clip.startMs + delta.trimStartDeltaMs).coerceIn(0L, clip.endMs - 2000L)
                val newEnd = (clip.endMs + delta.trimEndDeltaMs).coerceAtLeast(newStart + 2000L)
                val nextClip = clip.copy(
                    hook = delta.hook ?: clip.hook,
                    captionsText = delta.captionsText ?: clip.captionsText,
                    translatedCaptionsText = delta.translatedCaptionsText ?: clip.translatedCaptionsText,
                    captionLanguage = delta.captionLanguage ?: clip.captionLanguage,
                    captionStyle = delta.captionStyle ?: clip.captionStyle,
                    captionColorHex = delta.captionColorHex ?: clip.captionColorHex,
                    captionPositionY = delta.captionPositionY ?: clip.captionPositionY,
                    aspectRatio = delta.aspectRatio ?: clip.aspectRatio,
                    startMs = newStart,
                    endMs = newEnd
                )
                pushEditorState(nextClip)
                firestoreRepository.updateClipEdits(currentUserId, nextClip)
                _toastMessage.value = delta.explanation
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _toastMessage.value = "AI Assistant error: ${e.localizedMessage}"
            } finally {
                _aiAssistantBusy.value = false
            }
        }
    }

    fun deleteClip(projectId: String, clipId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteClip(currentUserId, projectId, clipId)
                .onSuccess { _toastMessage.value = "Clip deleted." }
                .onFailure { err -> _toastMessage.value = err.localizedMessage }
        }
    }

    fun renameProject(projectId: String, newTitle: String, folderName: String) {
        viewModelScope.launch {
            firestoreRepository.updateProjectTitleAndFolder(currentUserId, projectId, newTitle, folderName)
                .onSuccess { _toastMessage.value = "Project updated." }
                .onFailure { err -> _toastMessage.value = err.localizedMessage }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteProject(currentUserId, projectId)
                .onSuccess {
                    if (_activeProjectId.value == projectId) _activeProjectId.value = null
                    _toastMessage.value = "Project deleted."
                }
                .onFailure { err -> _toastMessage.value = err.localizedMessage }
        }
    }

    fun saveBrandKit(
        brandName: String,
        primaryHex: String,
        fontStyle: String,
        introTag: String?,
        outroTag: String?
    ) {
        viewModelScope.launch {
            firestoreRepository.updateBrandKit(
                userId = currentUserId,
                brandName = brandName,
                brandPrimaryHex = primaryHex,
                brandFontStyle = fontStyle,
                brandIntroTag = introTag,
                brandOutroTag = outroTag
            ).onSuccess {
                _toastMessage.value = "Brand Kit saved to cloud profile!"
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage
            }
        }
    }

    fun saveCustomTemplate(name: String) {
        val settings = _generationSettings.value
        viewModelScope.launch {
            firestoreRepository.saveTemplate(
                currentUserId,
                ClipTemplate(
                    userId = currentUserId,
                    name = name,
                    aspectRatio = settings.aspectRatio,
                    captionStyle = settings.captionStyle,
                    captionColorHex = settings.captionColorHex,
                    captionLanguage = settings.captionLanguage,
                    platformPreset = settings.platformPreset
                )
            ).onSuccess {
                _toastMessage.value = "Saved template \"$name\""
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage
            }
        }
    }

    fun applyTemplate(template: ClipTemplate) {
        _generationSettings.update {
            it.copy(
                aspectRatio = template.aspectRatio,
                captionStyle = template.captionStyle,
                captionColorHex = template.captionColorHex,
                captionLanguage = template.captionLanguage,
                platformPreset = template.platformPreset
            )
        }
        _toastMessage.value = "Applied template \"${template.name}\""
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteTemplate(currentUserId, templateId)
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
