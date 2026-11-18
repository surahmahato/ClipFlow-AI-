package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.model.ClipTemplate
import com.example.data.model.GeneratedClip
import com.example.data.model.UserProfile
import com.example.data.model.VideoProject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(userId: String, displayName: String, email: String?): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "User ID mismatch" }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = displayName.ifBlank { "Creator" }.take(80),
                    email = email?.take(120),
                    planTier = "FREE",
                    clipsGeneratedCount = 0L,
                    brandName = "ClipFlow Studio",
                    brandPrimaryHex = "#F4D96B",
                    brandFontStyle = "Space Grotesk"
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBrandKit(
        userId: String,
        brandName: String,
        brandPrimaryHex: String,
        brandFontStyle: String,
        brandIntroTag: String?,
        brandOutroTag: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val updates = mutableMapOf<String, Any>(
                "brandName" to brandName.trim().ifBlank { "ClipFlow Studio" }.take(60),
                "brandPrimaryHex" to brandPrimaryHex.trim().ifBlank { "#F4D96B" }.take(9),
                "brandFontStyle" to brandFontStyle.trim().ifBlank { "Space Grotesk" }.take(40),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!brandIntroTag.isNullOrBlank()) {
                updates["brandIntroTag"] = brandIntroTag.trim().take(80)
            }
            if (!brandOutroTag.isNullOrBlank()) {
                updates["brandOutroTag"] = brandOutroTag.trim().take(80)
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementClipsGenerated(userId: String, delta: Long): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            if (delta <= 0L) return Result.success(Unit)
            db.collection("users").document(uid).update(
                mapOf(
                    "clipsGeneratedCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeProjects(userId: String): Flow<List<VideoProject>> = flow {
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users").document(userId).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VideoProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProjectWithClips(
        project: VideoProject,
        clips: List<GeneratedClip>
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/unauthenticated/projects")
            return Result.failure(e)
        }
        val projectRef = db.collection("users").document(uid).collection("projects").document()
        val projectId = projectRef.id
        return try {
            val projectToWrite = project.copy(userId = uid)
            projectRef.set(projectToWrite.toCreateMap()).await()

            val batch = db.batch()
            clips.forEach { clip ->
                val clipRef = projectRef.collection("clips").document()
                val clipPayload = clip.copy(userId = uid, projectId = projectId).toCreateMap()
                batch.set(clipRef, clipPayload)
            }
            batch.commit().await()
            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, projectRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectTitleAndFolder(
        userId: String,
        projectId: String,
        title: String,
        folderName: String
    ): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(projectId.isNotBlank())
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(
                    mapOf(
                        "title" to title.trim().ifBlank { "Untitled Project" }.take(140),
                        "folderName" to folderName.trim().ifBlank { "All Projects" }.take(60),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectSettings(
        userId: String,
        project: VideoProject
    ): Result<Unit> {
        val path = "users/$userId/projects/${project.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(project.id.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "status" to project.status,
                "aspectRatio" to project.aspectRatio,
                "captionStyle" to project.captionStyle,
                "captionLanguage" to project.captionLanguage,
                "clipCountRequested" to project.clipCountRequested.coerceIn(1L, 25L),
                "durationPreset" to project.durationPreset,
                "intensityPreset" to project.intensityPreset,
                "voiceEnhanceEnabled" to project.voiceEnhanceEnabled,
                "noiseReductionEnabled" to project.noiseReductionEnabled,
                "musicEnabled" to project.musicEnabled,
                "sfxEnabled" to project.sfxEnabled,
                "silenceRemovalEnabled" to project.silenceRemovalEnabled,
                "faceTrackingEnabled" to project.faceTrackingEnabled,
                "speakerLayout" to project.speakerLayout,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!project.copilotInstruction.isNullOrBlank()) {
                updates["copilotInstruction"] = project.copilotInstruction.take(500)
            }
            db.collection("users").document(uid).collection("projects").document(project.id)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(projectId.isNotBlank())
            val projRef = db.collection("users").document(uid).collection("projects").document(projectId)
            val clipsSnap = projRef.collection("clips").get().await()
            val batch = db.batch()
            clipsSnap.documents.forEach { batch.delete(it.reference) }
            batch.delete(projRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeClips(userId: String, projectId: String): Flow<List<GeneratedClip>> = flow {
        val path = "users/$userId/projects/$projectId/clips"
        emitAll(
            db.collection("users").document(userId)
                .collection("projects").document(projectId)
                .collection("clips")
                .orderBy("clipIndex", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GeneratedClip::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateClipEdits(userId: String, clip: GeneratedClip): Result<Unit> {
        val path = "users/$userId/projects/${clip.projectId}/clips/${clip.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(clip.projectId.isNotBlank() && clip.id.isNotBlank())
            val safeStart = clip.startMs.coerceAtLeast(0L)
            val safeEnd = clip.endMs.coerceAtLeast(safeStart + 1000L).coerceAtMost(86_400_000L)
            val updates = mutableMapOf<String, Any>(
                "title" to clip.title.take(140).ifBlank { "Viral Clip" },
                "hook" to clip.hook.take(240).ifBlank { "Watch this..." },
                "hookType" to clip.hookType.take(50).ifBlank { "Curiosity" },
                "hookOptions" to clip.hookOptions.take(10).map { it.take(200) },
                "startMs" to safeStart,
                "endMs" to safeEnd,
                "viralScore" to clip.viralScore.coerceIn(0L, 100L),
                "scoreReason" to clip.scoreReason.take(600).ifBlank { "Engaging highlight." },
                "captionsText" to clip.captionsText.take(4000),
                "captionLanguage" to clip.captionLanguage.take(40).ifBlank { "English" },
                "captionStyle" to clip.captionStyle.take(40).ifBlank { "Bold Highlight" },
                "captionColorHex" to clip.captionColorHex.take(9).ifBlank { "#F4D96B" },
                "captionPositionY" to clip.captionPositionY.coerceIn(0.05, 0.95),
                "aspectRatio" to (clip.aspectRatio.takeIf { it in listOf("9:16", "16:9", "1:1", "4:5") } ?: "9:16"),
                "faceFocusX" to clip.faceFocusX.coerceIn(0.0, 1.0),
                "platformPreset" to clip.platformPreset.take(40).ifBlank { "YouTube Shorts" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            clip.translatedCaptionsText?.take(4000)?.takeIf { it.isNotBlank() }?.let {
                updates["translatedCaptionsText"] = it
            }
            clip.youtubeTitle?.take(150)?.takeIf { it.isNotBlank() }?.let { updates["youtubeTitle"] = it }
            clip.instagramCaption?.take(1000)?.takeIf { it.isNotBlank() }?.let { updates["instagramCaption"] = it }
            clip.tiktokDescription?.take(1000)?.takeIf { it.isNotBlank() }?.let { updates["tiktokDescription"] = it }
            clip.hashtags?.take(300)?.takeIf { it.isNotBlank() }?.let { updates["hashtags"] = it }

            db.collection("users").document(uid)
                .collection("projects").document(clip.projectId)
                .collection("clips").document(clip.id)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateClipExportState(
        userId: String,
        projectId: String,
        clipId: String,
        localFilePath: String?,
        rendered: Boolean,
        gallerySaved: Boolean
    ): Result<Unit> {
        val path = "users/$userId/projects/$projectId/clips/$clipId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val updates = mutableMapOf<String, Any>(
                "rendered" to rendered,
                "gallerySaved" to gallerySaved,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!localFilePath.isNullOrBlank()) {
                updates["localFilePath"] = localFilePath.take(500)
            }
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("clips").document(clipId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteClip(userId: String, projectId: String, clipId: String): Result<Unit> {
        val path = "users/$userId/projects/$projectId/clips/$clipId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("clips").document(clipId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeTemplates(userId: String): Flow<List<ClipTemplate>> = flow {
        val path = "users/$userId/templates"
        emitAll(
            db.collection("users").document(userId).collection("templates")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ClipTemplate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveTemplate(userId: String, template: ClipTemplate): Result<Unit> {
        val path = "users/$userId/templates"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("templates").document()
            docRef.set(template.copy(userId = uid).toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTemplate(userId: String, templateId: String): Result<Unit> {
        val path = "users/$userId/templates/$templateId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("templates").document(templateId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
