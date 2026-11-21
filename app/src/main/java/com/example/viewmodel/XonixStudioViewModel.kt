package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AuthResult
import com.example.data.LocalEncryptedUserRepository
import com.example.data.LoginHistoryEntity
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.data.UserAccountEntity
import com.example.data.UserAuthRepository
import com.example.data.WorkspaceType
import com.example.data.XonixDatabase
import com.example.engine.AudioDspEngine
import com.example.engine.ExportResult
import com.example.engine.ImageProcessingEngine
import com.example.engine.MediaExportStore
import com.example.engine.VideoMediaEngine
import com.example.model.AudioEqPreset
import com.example.model.AudioExportFormat
import com.example.model.AudioStudioState
import com.example.model.AudioTrackRole
import com.example.model.AudioTrackState
import com.example.model.BatchImageEntry
import com.example.model.CollageLayoutKind
import com.example.model.CopyrightCheckItem
import com.example.model.EraserStrokePoint
import com.example.model.HardwareEncoderCapabilities
import com.example.model.ImageConvertFormat
import com.example.model.LayerKind
import com.example.model.LightroomGrade
import com.example.model.LutPreset
import com.example.model.PhotoCanvasPreset
import com.example.model.PhotoExportFormat
import com.example.model.PhotoLayer
import com.example.model.PhotoStudioState
import com.example.model.PipOverlayState
import com.example.model.SfxKind
import com.example.model.SpeedCurvePreset
import com.example.model.SpotHealPoint
import com.example.model.StudioShapeKind
import com.example.model.StudioWorkspace
import com.example.model.TextAnimationStyle
import com.example.model.UtilitiesState
import com.example.model.VideoAspectPreset
import com.example.model.VideoAudioClip
import com.example.model.VideoBgFillMode
import com.example.model.VideoClip
import com.example.model.VideoColorGrade
import com.example.model.VideoEditorState
import com.example.model.VideoExportFps
import com.example.model.VideoExportPreset
import com.example.model.VideoExportResolution
import com.example.model.VideoKeyframe
import com.example.model.VideoTextOverlay
import com.example.model.VideoTransition
import com.example.model.VoiceChangerPreset
import com.example.security.AppSecurityFirewall
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class XonixStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = XonixDatabase.getInstance(appContext)
    private val repository = ProjectRepository(db.projectDao())
    private val userRepository: UserAuthRepository = LocalEncryptedUserRepository(appContext, db.userDao())
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val videoAdapter = moshi.adapter(VideoEditorState::class.java)
    private val photoAdapter = moshi.adapter(PhotoStudioState::class.java)
    private val audioAdapter = moshi.adapter(AudioStudioState::class.java)

    val vaultProjects: StateFlow<List<ProjectEntity>> = repository.projects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Auth, Profile & Owner-Only Admin State
    private val _authReady = MutableStateFlow(false)
    val authReady: StateFlow<Boolean> = _authReady.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccountEntity?>(null)
    val currentUser: StateFlow<UserAccountEntity?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _adminPinMessage = MutableStateFlow<String?>(null)
    val adminPinMessage: StateFlow<String?> = _adminPinMessage.asStateFlow()

    private val _adminLockoutMs = MutableStateFlow(0L)
    val adminLockoutMs: StateFlow<Long> = _adminLockoutMs.asStateFlow()

    val allUsers: StateFlow<List<UserAccountEntity>> = userRepository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLoginHistory: StateFlow<List<LoginHistoryEntity>> = userRepository.allLoginHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentUserLoginHistory: StateFlow<List<LoginHistoryEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else userRepository.observeUserLoginHistory(user.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeWorkspace = MutableStateFlow(StudioWorkspace.HOME)
    val activeWorkspace: StateFlow<StudioWorkspace> = _activeWorkspace.asStateFlow()

    private val _openVideoEnhanceRequested = MutableStateFlow(false)
    val openVideoEnhanceRequested: StateFlow<Boolean> = _openVideoEnhanceRequested.asStateFlow()

    private val _videoState = MutableStateFlow(createInitialVideoState())
    val videoState: StateFlow<VideoEditorState> = _videoState.asStateFlow()

    private val _videoPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val videoPreviewBitmap: StateFlow<Bitmap?> = _videoPreviewBitmap.asStateFlow()

    private val _clipThumbnails = MutableStateFlow<Map<String, List<Bitmap>>>(emptyMap())
    val clipThumbnails: StateFlow<Map<String, List<Bitmap>>> = _clipThumbnails.asStateFlow()

    // Video Editor Undo / Redo History Stack
    private val videoUndoStack = ArrayDeque<VideoEditorState>()
    private val videoRedoStack = ArrayDeque<VideoEditorState>()
    private var lastUndoPushMs = 0L
    private var lastUndoTag: String? = null

    private val _canUndoVideo = MutableStateFlow(false)
    val canUndoVideo: StateFlow<Boolean> = _canUndoVideo.asStateFlow()

    private val _canRedoVideo = MutableStateFlow(false)
    val canRedoVideo: StateFlow<Boolean> = _canRedoVideo.asStateFlow()

    private val _photoState = MutableStateFlow(createInitialPhotoState())
    val photoState: StateFlow<PhotoStudioState> = _photoState.asStateFlow()

    private val _photoPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val photoPreviewBitmap: StateFlow<Bitmap?> = _photoPreviewBitmap.asStateFlow()

    private val _audioState = MutableStateFlow(createInitialAudioState())
    val audioState: StateFlow<AudioStudioState> = _audioState.asStateFlow()

    private val _utilitiesState = MutableStateFlow(UtilitiesState())
    val utilitiesState: StateFlow<UtilitiesState> = _utilitiesState.asStateFlow()

    private val _exportProgress = MutableStateFlow<Float?>(null)
    val exportProgress: StateFlow<Float?> = _exportProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _autoSaveLabel = MutableStateFlow("Saved")
    val autoSaveLabel: StateFlow<String> = _autoSaveLabel.asStateFlow()

    private val _encoderCapabilities = MutableStateFlow(HardwareEncoderCapabilities())
    val encoderCapabilities: StateFlow<HardwareEncoderCapabilities> = _encoderCapabilities.asStateFlow()

    private val _openVideoExportRequested = MutableStateFlow(false)
    val openVideoExportRequested: StateFlow<Boolean> = _openVideoExportRequested.asStateFlow()

    private var videoRenderJob: Job? = null
    private var photoRenderJob: Job? = null
    private val videoRenderDirty = AtomicBoolean(false)
    private val videoRenderImmediate = AtomicBoolean(false)
    private val photoRenderDirty = AtomicBoolean(false)
    private val photoRenderImmediate = AtomicBoolean(false)
    private var playbackJob: Job? = null
    private var audioPlayJob: Job? = null
    private var micRecordJob: Job? = null
    private var videoVoiceRecordJob: Job? = null
    private var autoSaveJob: Job? = null
    private val recordingFlag = AtomicBoolean(false)
    private val videoVoiceRecordingFlag = AtomicBoolean(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val restored = userRepository.restoreSession()
            _currentUser.value = restored
            _authReady.value = true
            _adminLockoutMs.value = AppSecurityFirewall.getPinLockoutRemainingMs(appContext)
        }
        viewModelScope.launch(Dispatchers.Default) {
            _encoderCapabilities.value = VideoMediaEngine.queryHardwareEncoderCapabilities()
        }
        prepareStarterAudioFiles()
        refreshFilmstripThumbnails(_videoState.value.clips)
        scheduleVideoPreviewRender()
        schedulePhotoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    // =========================================================================
    // AUTHENTICATION, PROFILE & OWNER-ONLY ADMIN CONTROLS
    // =========================================================================

    fun clearAuthError() {
        _authError.value = null
    }

    fun continueAsGuest() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = userRepository.continueAsGuest()) {
                is AuthResult.Success -> {
                    _currentUser.value = res.user
                    _authError.value = null
                    _isAdminUnlocked.value = false
                    _activeWorkspace.value = StudioWorkspace.HOME
                    showToast("Welcome! Offline Guest Mode active.")
                }
                is AuthResult.Error -> _authError.value = res.message
            }
        }
    }

    fun createAccount(fullName: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = userRepository.createAccount(fullName, email, password)) {
                is AuthResult.Success -> {
                    _currentUser.value = res.user
                    _authError.value = null
                    _isAdminUnlocked.value = false
                    _activeWorkspace.value = StudioWorkspace.HOME
                    showToast("Welcome, ${res.user.fullName}!")
                }
                is AuthResult.Error -> _authError.value = res.message
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = userRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _currentUser.value = res.user
                    _authError.value = null
                    _isAdminUnlocked.value = false
                    _activeWorkspace.value = StudioWorkspace.HOME
                    showToast("Signed in as ${res.user.fullName}")
                }
                is AuthResult.Error -> _authError.value = res.message
            }
        }
    }

    fun updateProfileName(newName: String) {
        val active = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = userRepository.updateProfileName(active.id, newName)) {
                is AuthResult.Success -> {
                    _currentUser.value = res.user
                    showToast("Profile updated")
                }
                is AuthResult.Error -> showToast(res.message)
            }
        }
    }

    fun logOut() {
        stopVideoPlayback()
        stopAudioMixPlayback()
        stopVoiceoverRecording()
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.logOut()
            _isAdminUnlocked.value = false
            _adminPinMessage.value = null
            _currentUser.value = null
            _activeWorkspace.value = StudioWorkspace.HOME
        }
    }

    fun isCurrentUserOwner(): Boolean {
        val user = _currentUser.value ?: return false
        return AppSecurityFirewall.isOwnerAccount(user.email, user.isGuest)
    }

    fun refreshAdminLockoutStatus() {
        _adminLockoutMs.value = AppSecurityFirewall.getPinLockoutRemainingMs(appContext)
    }

    fun verifyOwnerAdminPin(pin: String): Boolean {
        val user = _currentUser.value ?: return false
        val result = AppSecurityFirewall.verifyOwnerPin(
            context = appContext,
            email = user.email,
            isGuest = user.isGuest,
            pinInput = pin
        )
        _adminPinMessage.value = result.message
        _adminLockoutMs.value = result.remainingLockoutMs
        _isAdminUnlocked.value = result.unlocked
        return result.unlocked
    }

    fun lockAdminDashboard() {
        _isAdminUnlocked.value = false
        _adminPinMessage.value = null
    }

    fun removeAccountAsOwner(userId: String) {
        if (!isCurrentUserOwner() || !_isAdminUnlocked.value) return
        val current = _currentUser.value
        if (current != null && current.id == userId) {
            showToast("Cannot remove the active owner account")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.removeAccount(userId)
            showToast("Account removed")
        }
    }

    fun openVideoEnhancerShortcut() {
        _videoState.update { s ->
            s.copy(colorGrade = s.colorGrade.copy(qualityEnhancerEnabled = true))
        }
        _openVideoEnhanceRequested.value = true
        selectWorkspace(StudioWorkspace.VIDEO)
        scheduleVideoPreviewRender(true)
        showToast("4K Clarity Enhancer ON")
    }

    fun consumeVideoEnhanceRequest() {
        _openVideoEnhanceRequested.value = false
    }

    fun shareAppWithFriends() {
        AppSecurityFirewall.launchReferAndShareIntent(appContext)
    }

    fun selectWorkspace(workspace: StudioWorkspace) {
        if (_activeWorkspace.value != workspace) {
            stopVideoPlayback()
            stopAudioMixPlayback()
            _activeWorkspace.value = workspace
            when (workspace) {
                StudioWorkspace.VIDEO -> if (_videoPreviewBitmap.value == null) scheduleVideoPreviewRender(true)
                StudioWorkspace.PHOTO -> if (_photoPreviewBitmap.value == null) schedulePhotoPreviewRender(true)
                else -> Unit
            }
        }
    }

    fun requestOpenVideoExport() {
        _openVideoExportRequested.value = true
    }

    fun consumeVideoExportRequest() {
        _openVideoExportRequested.value = false
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // =========================================================================
    // WORKSPACE 1: PRO VIDEO EDITOR ACTIONS
    // =========================================================================

    private fun createInitialVideoState(): VideoEditorState {
        val clip1 = VideoClip(
            id = "clip_cyber_intro",
            name = "Neon City Intro",
            fallbackDrawable = R.drawable.img_studio_sample_bg_1795185351810,
            durationMs = 4500L,
            trimStartMs = 0L,
            trimEndMs = 4500L,
            speed = 1f,
            transitionAfter = VideoTransition.CROSSFADE
        )
        val clip2 = VideoClip(
            id = "clip_esports_outro",
            name = "Cyber Gear Reveal",
            fallbackDrawable = R.drawable.img_studio_sample_bg_1795185351810,
            durationMs = 3500L,
            trimStartMs = 0L,
            trimEndMs = 3500L,
            speed = 1.25f,
            transitionAfter = VideoTransition.FLASH
        )
        val titleOverlay = VideoTextOverlay(
            id = "txt_title_1",
            text = "BLACK DEVIL STUDIO PRO",
            fontId = "orbitron",
            colorArgb = 0xFFFF1E42.toInt(),
            strokeArgb = 0xFF000000.toInt(),
            strokeWidth = 7f,
            xNorm = 0.5f,
            yNorm = 0.82f,
            scale = 1.05f,
            startMs = 0L,
            endMs = 5500L,
            animation = TextAnimationStyle.POP
        )
        return VideoEditorState(
            clips = listOf(clip1, clip2),
            selectedClipId = clip1.id,
            aspectPreset = VideoAspectPreset.YOUTUBE_16_9,
            pip = PipOverlayState(
                enabled = false,
                uriString = null,
                fallbackDrawable = null,
                label = "Overlay"
            ),
            textLayers = listOf(titleOverlay),
            selectedTextId = titleOverlay.id
        )
    }

    private fun refreshFilmstripThumbnails(clips: List<VideoClip>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _clipThumbnails.value
            val activeIds = clips.map { it.id }.toSet()
            val updated = existing.filterKeys { it in activeIds }.toMutableMap()
            clips.forEach { clip ->
                if (updated[clip.id].isNullOrEmpty()) {
                    updated[clip.id] = VideoMediaEngine.extractFilmstripThumbnails(appContext, clip, count = 5)
                }
            }
            _clipThumbnails.value = updated
        }
    }

    private fun scheduleVideoPreviewRender(immediate: Boolean = false) {
        if (immediate) videoRenderImmediate.set(true)
        videoRenderDirty.set(true)
        if (videoRenderJob?.isActive == true) return
        videoRenderJob = viewModelScope.launch {
            while (videoRenderDirty.getAndSet(false)) {
                val isFastImmediate = videoRenderImmediate.getAndSet(false)
                if (!isFastImmediate) delay(18L)
                val snapshot = _videoState.value
                val targetEdge = if (snapshot.isPlaying || !isFastImmediate) 600 else 720
                val rendered = withContext(Dispatchers.Default) {
                    VideoMediaEngine.renderPreviewAtPlayhead(appContext, snapshot, maxEdge = targetEdge)
                }
                _videoPreviewBitmap.value = rendered
            }
        }
    }

    fun importVideoClipsAndOpenEditor(uris: List<Uri>) {
        if (uris.isEmpty()) {
            selectWorkspace(StudioWorkspace.VIDEO)
            return
        }
        importVideoClips(uris, replaceStarters = true)
        selectWorkspace(StudioWorkspace.VIDEO)
    }

    fun importVideoClips(uris: List<Uri>, replaceStarters: Boolean = false) {
        val validUris = uris.filter { AppSecurityFirewall.validateMediaUri(appContext, it, "video/") }
        if (validUris.isEmpty()) {
            showToast("Selected file is not a supported video")
            return
        }
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                validUris.map { uri ->
                    val meta = VideoMediaEngine.probeVideoMetadata(appContext, uri.toString())
                    val name = AppSecurityFirewall.sanitizeText(
                        queryFileName(uri) ?: "Clip ${(100..999).random()}",
                        maxLength = 40
                    )
                    VideoClip(
                        id = "clip_${UUID.randomUUID().toString().take(8)}",
                        name = name.substringBeforeLast("."),
                        uriString = uri.toString(),
                        durationMs = meta.durationMs,
                        trimStartMs = 0L,
                        trimEndMs = meta.durationMs,
                        speed = 1f,
                        transitionAfter = VideoTransition.CROSSFADE,
                        sourceWidth = meta.width,
                        sourceHeight = meta.height
                    )
                }
            }
            recordVideoUndoSnapshot()
            _videoState.update { s ->
                val hasOnlyStarters = s.clips.all { it.uriString.isNullOrBlank() }
                val updated = if (replaceStarters && hasOnlyStarters) added else s.clips + added
                val cleanPip = if (s.pip.uriString.isNullOrBlank()) {
                    PipOverlayState(enabled = false, uriString = null, fallbackDrawable = null)
                } else {
                    s.pip
                }
                s.copy(
                    clips = updated,
                    selectedClipId = added.last().id,
                    playheadMs = if (replaceStarters && hasOnlyStarters) 0L else s.playheadMs,
                    pip = cleanPip
                )
            }
            refreshFilmstripThumbnails(_videoState.value.clips)
            showToast("Added ${added.size} clip(s)")
            scheduleVideoPreviewRender(true)
            scheduleAutoSave(WorkspaceType.VIDEO)
        }
    }

    private fun syncVideoHistoryFlags() {
        _canUndoVideo.value = videoUndoStack.isNotEmpty()
        _canRedoVideo.value = videoRedoStack.isNotEmpty()
    }

    private fun recordVideoUndoSnapshot(coalesceTag: String? = null) {
        val now = System.currentTimeMillis()
        if (coalesceTag != null && coalesceTag == lastUndoTag && (now - lastUndoPushMs) < 550L) {
            lastUndoPushMs = now
            return
        }
        val snapshot = _videoState.value.copy(
            isPlaying = false,
            isListeningCaptions = false,
            isRecordingVideoVoiceover = false
        )
        if (videoUndoStack.lastOrNull() == snapshot) return
        if (videoUndoStack.size >= 40) videoUndoStack.removeFirst()
        videoUndoStack.addLast(snapshot)
        videoRedoStack.clear()
        lastUndoPushMs = now
        lastUndoTag = coalesceTag
        syncVideoHistoryFlags()
    }

    fun undoVideoEdit() {
        if (videoUndoStack.isEmpty()) return
        stopVideoPlayback()
        val current = _videoState.value.copy(
            isPlaying = false,
            isListeningCaptions = false,
            isRecordingVideoVoiceover = false
        )
        if (videoRedoStack.size >= 40) videoRedoStack.removeFirst()
        videoRedoStack.addLast(current)
        val previous = videoUndoStack.removeLast()
        lastUndoTag = null
        _videoState.value = previous
        syncVideoHistoryFlags()
        refreshFilmstripThumbnails(previous.clips)
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun redoVideoEdit() {
        if (videoRedoStack.isEmpty()) return
        stopVideoPlayback()
        val current = _videoState.value.copy(
            isPlaying = false,
            isListeningCaptions = false,
            isRecordingVideoVoiceover = false
        )
        if (videoUndoStack.size >= 40) videoUndoStack.removeFirst()
        videoUndoStack.addLast(current)
        val restored = videoRedoStack.removeLast()
        lastUndoTag = null
        _videoState.value = restored
        syncVideoHistoryFlags()
        refreshFilmstripThumbnails(restored.clips)
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun replaceSelectedClipMedia(uri: Uri) {
        if (!AppSecurityFirewall.validateMediaUri(appContext, uri, "video/")) {
            showToast("Selected file is not a supported video")
            return
        }
        val targetId = _videoState.value.selectedClip?.id ?: return
        viewModelScope.launch {
            val (meta, cleanName) = withContext(Dispatchers.IO) {
                val m = VideoMediaEngine.probeVideoMetadata(appContext, uri.toString())
                val n = AppSecurityFirewall.sanitizeText(
                    queryFileName(uri) ?: "Clip ${(100..999).random()}",
                    maxLength = 40
                ).substringBeforeLast(".")
                m to n
            }
            recordVideoUndoSnapshot()
            _videoState.update { s ->
                s.copy(
                    clips = s.clips.map { c ->
                        if (c.id == targetId) {
                            c.copy(
                                name = cleanName,
                                uriString = uri.toString(),
                                fallbackDrawable = null,
                                durationMs = meta.durationMs,
                                trimStartMs = 0L,
                                trimEndMs = meta.durationMs,
                                sourceWidth = meta.width,
                                sourceHeight = meta.height
                            )
                        } else c
                    }
                )
            }
            _clipThumbnails.update { it - targetId }
            refreshFilmstripThumbnails(_videoState.value.clips)
            scheduleVideoPreviewRender(true)
            scheduleAutoSave(WorkspaceType.VIDEO)
            showToast("Clip replaced")
        }
    }

    fun selectClip(id: String) {
        _videoState.update { it.copy(selectedClipId = id, selectedAudioClipId = null) }
    }

    fun clearTimelineSelection() {
        _videoState.update { it.copy(selectedClipId = null, selectedAudioClipId = null) }
    }

    fun reorderClip(id: String, moveLeft: Boolean) {
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val idx = s.clips.indexOfFirst { it.id == id }
            if (idx < 0) return@update s
            val target = if (moveLeft) idx - 1 else idx + 1
            if (target !in s.clips.indices) return@update s
            val mutable = s.clips.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(target, item)
            s.copy(clips = mutable)
        }
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    private fun findClipAtPlayhead(s: VideoEditorState): Triple<Int, VideoClip, Long>? {
        if (s.clips.isEmpty()) return null
        var acc = 0L
        s.clips.forEachIndexed { idx, clip ->
            val dur = clip.effectiveDurationMs
            if (s.playheadMs <= acc + dur || idx == s.clips.lastIndex) {
                val localMs = (s.playheadMs - acc).coerceIn(0L, dur)
                return Triple(idx, clip, localMs)
            }
            acc += dur
        }
        return null
    }

    fun splitSelectedClipAtPlayhead() {
        val snapshot = _videoState.value
        val hit = findClipAtPlayhead(snapshot) ?: return
        val (idx, target, localTimeline) = hit
        val safeStart = target.trimStartMs.coerceAtLeast(0L)
        val safeEnd = target.trimEndMs.coerceAtLeast(safeStart)
        val minSplitSrcMs = safeStart + 250L
        val maxSplitSrcMs = safeEnd - 250L
        if (maxSplitSrcMs < minSplitSrcMs) {
            showToast("Clip is too short to split")
            return
        }
        val rawSplitSrcMs = safeStart + (localTimeline * target.speed).toLong()
        if (rawSplitSrcMs < safeStart + 200L || rawSplitSrcMs > safeEnd - 200L) {
            showToast("Move playhead inside the clip to split")
            return
        }
        val splitSrcMs = rawSplitSrcMs.coerceIn(minSplitSrcMs, maxSplitSrcMs)
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val currentTarget = s.clips.getOrNull(idx) ?: return@update s
            val partAKfs = currentTarget.keyframes.filter { it.timeMs <= localTimeline }
            val partBKfs = currentTarget.keyframes
                .filter { it.timeMs > localTimeline }
                .map { it.copy(timeMs = (it.timeMs - localTimeline).coerceAtLeast(0L)) }
            val partA = currentTarget.copy(trimEndMs = splitSrcMs, keyframes = partAKfs)
            val partB = currentTarget.copy(
                id = "clip_${UUID.randomUUID().toString().take(8)}",
                name = "${currentTarget.name} (B)",
                trimStartMs = splitSrcMs,
                keyframes = partBKfs
            )
            val mutable = s.clips.toMutableList()
            mutable[idx] = partA
            mutable.add(idx + 1, partB)
            s.copy(clips = mutable, selectedClipId = partB.id)
        }
        refreshFilmstripThumbnails(_videoState.value.clips)
        showToast("Clip split at playhead")
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun trimClipLeftToPlayhead() {
        val snapshot = _videoState.value
        val hit = findClipAtPlayhead(snapshot) ?: return
        val (idx, target, localTimeline) = hit
        if (localTimeline < 80L) {
            showToast("Playhead is already at clip start")
            return
        }
        val newStart = (target.trimStartMs + (localTimeline * target.speed).toLong())
            .coerceIn(target.trimStartMs, (target.trimEndMs - 300L).coerceAtLeast(target.trimStartMs))
        if (newStart <= target.trimStartMs) {
            showToast("Clip is already at minimum length")
            return
        }
        recordVideoUndoSnapshot()
        var clipOffset = 0L
        for (i in 0 until idx) clipOffset += snapshot.clips[i].effectiveDurationMs
        _videoState.update { s ->
            val updatedClips = s.clips.mapIndexed { i, c ->
                if (i == idx) {
                    val shiftedKfs = c.keyframes.mapNotNull { kf ->
                        val nextT = kf.timeMs - localTimeline
                        if (nextT >= 0L) kf.copy(timeMs = nextT) else null
                    }
                    c.copy(trimStartMs = newStart, keyframes = shiftedKfs)
                } else c
            }
            s.copy(
                clips = updatedClips,
                selectedClipId = target.id,
                playheadMs = clipOffset
            )
        }
        showToast("Trimmed left to playhead")
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun trimClipRightToPlayhead() {
        val snapshot = _videoState.value
        val hit = findClipAtPlayhead(snapshot) ?: return
        val (idx, target, localTimeline) = hit
        if (localTimeline >= target.effectiveDurationMs - 80L) {
            showToast("Playhead is already at clip end")
            return
        }
        val newEnd = (target.trimStartMs + (localTimeline * target.speed).toLong())
            .coerceIn((target.trimStartMs + 300L).coerceAtMost(target.trimEndMs), target.trimEndMs)
        if (newEnd >= target.trimEndMs) {
            showToast("Clip is already at minimum length")
            return
        }
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val updatedClips = s.clips.mapIndexed { i, c ->
                if (i == idx) {
                    val clampedKfs = c.keyframes.filter { it.timeMs <= localTimeline }
                    c.copy(trimEndMs = newEnd, keyframes = clampedKfs)
                } else c
            }
            s.copy(
                clips = updatedClips,
                selectedClipId = target.id,
                playheadMs = s.playheadMs.coerceAtMost(updatedClips.sumOf { it.effectiveDurationMs })
            )
        }
        showToast("Trimmed right to playhead")
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun duplicateSelectedClip() {
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val target = s.selectedClip ?: return@update s
            val idx = s.clips.indexOfFirst { it.id == target.id }
            val copy = target.copy(
                id = "clip_${UUID.randomUUID().toString().take(8)}",
                name = "${target.name} Copy"
            )
            val mutable = s.clips.toMutableList()
            mutable.add((idx + 1).coerceAtMost(mutable.size), copy)
            s.copy(clips = mutable, selectedClipId = copy.id)
        }
        refreshFilmstripThumbnails(_videoState.value.clips)
        showToast("Clip duplicated")
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun deleteSelectedClip() {
        if (_videoState.value.clips.size <= 1) {
            showToast("At least one clip must remain in timeline")
            return
        }
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val remaining = s.clips.filterNot { it.id == s.selectedClipId }
            s.copy(
                clips = remaining,
                selectedClipId = remaining.firstOrNull()?.id,
                playheadMs = s.playheadMs.coerceAtMost(remaining.sumOf { it.effectiveDurationMs })
            )
        }
        refreshFilmstripThumbnails(_videoState.value.clips)
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun updateSelectedClip(transform: (VideoClip) -> VideoClip) {
        recordVideoUndoSnapshot(coalesceTag = "clip_edit")
        _videoState.update { s ->
            val id = s.selectedClip?.id ?: return@update s
            s.copy(clips = s.clips.map { if (it.id == id) transform(it) else it })
        }
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun rotateSelectedClip90() {
        updateSelectedClip { c ->
            val next = ((c.rotationDeg + 90f) % 360f).let { if (it > 180f) it - 360f else it }
            c.copy(rotationDeg = next)
        }
    }

    fun toggleSelectedClipFlipH() {
        updateSelectedClip { it.copy(flipH = !it.flipH) }
    }

    fun toggleSelectedClipFlipV() {
        updateSelectedClip { it.copy(flipV = !it.flipV) }
    }

    fun toggleSelectedClipMirror() {
        updateSelectedClip { it.copy(mirrorMode = !it.mirrorMode) }
    }

    fun toggleSelectedClipStabilize() {
        updateSelectedClip { it.copy(stabilizeEnabled = !it.stabilizeEnabled) }
    }

    fun setSelectedClipMotionBlur(amount: Float) {
        updateSelectedClip { it.copy(motionBlur = amount.coerceIn(0f, 1f)) }
    }

    fun setSelectedClipSpeedCurve(curve: SpeedCurvePreset) {
        updateSelectedClip { it.copy(speedCurve = curve) }
    }

    fun setVideoBgFillMode(mode: VideoBgFillMode) {
        _videoState.update {
            it.copy(bgFillMode = mode, blurredBgFill = mode == VideoBgFillMode.BLUR)
        }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun setVideoBgFillColor(argb: Int) {
        _videoState.update {
            it.copy(bgFillMode = VideoBgFillMode.COLOR, bgFillColorArgb = argb, blurredBgFill = false)
        }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun setVideoBgImage(uri: Uri) {
        if (!AppSecurityFirewall.validateMediaUri(appContext, uri, "image/")) {
            showToast("Unsupported background image")
            return
        }
        _videoState.update {
            it.copy(bgFillMode = VideoBgFillMode.IMAGE, bgImageUri = uri.toString(), blurredBgFill = false)
        }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
        showToast("Background photo applied")
    }

    // --- Direct Video Audio Track Lane ---

    fun importAudioToVideoLane(uri: Uri) {
        if (!AppSecurityFirewall.validateMediaUri(appContext, uri, null)) {
            showToast("Unsupported audio file")
            return
        }
        viewModelScope.launch {
            _exportProgress.value = 0.35f
            val name = AppSecurityFirewall.sanitizeText(queryFileName(uri) ?: "Music Track", maxLength = 34)
            val dir = File(appContext.cacheDir, "video_audio_lane").apply { mkdirs() }
            val outFile = File(dir, "lane_${System.currentTimeMillis()}.wav")
            val extracted = AudioDspEngine.extractAudioFromMediaUri(appContext, uri, outFile)
            _exportProgress.value = null
            if (extracted != null) {
                val (_, durMs) = extracted
                val clip = VideoAudioClip(
                    id = "vaud_${UUID.randomUUID().toString().take(8)}",
                    name = name.substringBeforeLast("."),
                    filePath = outFile.absolutePath,
                    startMs = _videoState.value.playheadMs,
                    durationMs = durMs
                )
                _videoState.update { s ->
                    s.copy(audioLaneClips = s.audioLaneClips + clip, selectedAudioClipId = clip.id)
                }
                showToast("Music added to timeline")
                scheduleAutoSave(WorkspaceType.VIDEO)
            } else {
                showToast("Could not decode audio from file")
            }
        }
    }

    fun addSfxToVideoLane(kind: SfxKind) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(appContext.cacheDir, "video_audio_lane").apply { mkdirs() }
            val outFile = File(dir, "sfx_${kind.id}_${System.currentTimeMillis()}.wav")
            val pcm = AudioDspEngine.synthesizeSfxPcm(kind)
            AudioDspEngine.writePcmWavFile(outFile, pcm)
            val durMs = (pcm.size * 1000L / AudioDspEngine.SAMPLE_RATE).coerceAtLeast(350L)
            val clip = VideoAudioClip(
                id = "vsfx_${UUID.randomUUID().toString().take(8)}",
                name = "${kind.label} SFX",
                filePath = outFile.absolutePath,
                startMs = _videoState.value.playheadMs,
                durationMs = durMs,
                fadeInSec = 0f,
                fadeOutSec = 0.05f,
                isSfx = true
            )
            _videoState.update { s ->
                s.copy(audioLaneClips = s.audioLaneClips + clip, selectedAudioClipId = clip.id)
            }
            withContext(Dispatchers.Main) {
                showToast("Added ${kind.label} SFX to timeline")
                scheduleAutoSave(WorkspaceType.VIDEO)
            }
            AudioDspEngine.playPcm(pcm)
        }
    }

    fun extractAudioFromSelectedVideoClip() {
        val clip = _videoState.value.selectedClip ?: return
        val uriStr = clip.uriString
        if (uriStr.isNullOrBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val dir = File(appContext.cacheDir, "video_audio_lane").apply { mkdirs() }
                val outFile = File(dir, "extracted_clip_${System.currentTimeMillis()}.wav")
                val pcm = AudioDspEngine.generateStarterBeatPcm(
                    (clip.effectiveDurationMs / 1000.0).coerceIn(1.5, 8.0),
                    isVoiceDemo = true
                )
                AudioDspEngine.writePcmWavFile(outFile, pcm)
                val audioClip = VideoAudioClip(
                    id = "vext_${UUID.randomUUID().toString().take(8)}",
                    name = "${clip.name} Audio",
                    filePath = outFile.absolutePath,
                    startMs = 0L,
                    durationMs = clip.effectiveDurationMs
                )
                _videoState.update { s ->
                    s.copy(audioLaneClips = s.audioLaneClips + audioClip, selectedAudioClipId = audioClip.id)
                }
                withContext(Dispatchers.Main) { showToast("Extracted audio from ${clip.name}") }
            }
        } else {
            importAudioToVideoLane(Uri.parse(uriStr))
        }
    }

    fun startVideoVoiceoverRecording() {
        if (_videoState.value.isRecordingVideoVoiceover) return
        stopVideoPlayback()
        videoVoiceRecordingFlag.set(true)
        _videoState.update { it.copy(isRecordingVideoVoiceover = true) }
        videoVoiceRecordJob?.cancel()
        videoVoiceRecordJob = viewModelScope.launch {
            val dir = File(appContext.cacheDir, "video_audio_lane").apply { mkdirs() }
            val outFile = File(dir, "vvo_${System.currentTimeMillis()}.wav")
            val (_, durMs) = AudioDspEngine.recordVoiceoverToFile(
                outputFile = outFile,
                noiseReduction = true,
                shouldContinue = { videoVoiceRecordingFlag.get() },
                onLevel = { _, _ -> }
            )
            val clip = VideoAudioClip(
                id = "vvo_${UUID.randomUUID().toString().take(8)}",
                name = "Voiceover",
                filePath = outFile.absolutePath,
                startMs = _videoState.value.playheadMs,
                durationMs = durMs,
                volume = 1.25f,
                isVoiceover = true
            )
            _videoState.update { s ->
                s.copy(
                    audioLaneClips = s.audioLaneClips + clip,
                    selectedAudioClipId = clip.id,
                    isRecordingVideoVoiceover = false
                )
            }
            showToast("Voiceover added (${String.format("%.1fs", durMs / 1000f)})")
            scheduleAutoSave(WorkspaceType.VIDEO)
        }
    }

    fun stopVideoVoiceoverRecording() {
        videoVoiceRecordingFlag.set(false)
    }

    fun selectVideoAudioClip(id: String) {
        _videoState.update { it.copy(selectedAudioClipId = id, selectedClipId = null) }
    }

    fun updateSelectedVideoAudioClip(transform: (VideoAudioClip) -> VideoAudioClip) {
        _videoState.update { s ->
            val id = s.selectedAudioClip?.id ?: return@update s
            s.copy(audioLaneClips = s.audioLaneClips.map { if (it.id == id) transform(it) else it })
        }
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun trimVideoAudioClip(
        id: String,
        trimStartPct: Float? = null,
        trimEndPct: Float? = null,
        startMs: Long? = null
    ) {
        recordVideoUndoSnapshot(coalesceTag = "audio_trim_$id")
        _videoState.update { s ->
            val maxStart = s.totalDurationMs.coerceAtLeast(0L)
            s.copy(
                selectedAudioClipId = id,
                selectedClipId = null,
                audioLaneClips = s.audioLaneClips.map { a ->
                    if (a.id != id) a else {
                        val nextStartPct = (trimStartPct ?: a.trimStartPct)
                            .coerceIn(0f, (a.trimEndPct - 0.06f).coerceAtLeast(0f))
                        val nextEndPct = (trimEndPct ?: a.trimEndPct)
                            .coerceIn((nextStartPct + 0.06f).coerceAtMost(1f), 1f)
                        val nextStartMs = (startMs ?: a.startMs).coerceIn(0L, maxStart)
                        a.copy(
                            trimStartPct = nextStartPct,
                            trimEndPct = nextEndPct,
                            startMs = nextStartMs
                        )
                    }
                }
            )
        }
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun splitSelectedVideoAudioClip() {
        _videoState.update { s ->
            val target = s.selectedAudioClip ?: return@update s
            val midPct = ((target.trimStartPct + target.trimEndPct) / 2f).coerceIn(0.1f, 0.9f)
            if (midPct - target.trimStartPct < 0.08f || target.trimEndPct - midPct < 0.08f) return@update s
            val partA = target.copy(trimEndPct = midPct)
            val partB = target.copy(
                id = "vaud_${UUID.randomUUID().toString().take(8)}",
                name = "${target.name} (2)",
                trimStartPct = midPct,
                startMs = target.startMs + (target.durationMs * (midPct - target.trimStartPct)).toLong()
            )
            val idx = s.audioLaneClips.indexOfFirst { it.id == target.id }
            val list = s.audioLaneClips.toMutableList()
            list[idx] = partA
            list.add(idx + 1, partB)
            s.copy(audioLaneClips = list, selectedAudioClipId = partB.id)
        }
        showToast("Audio clip split")
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun moveVideoAudioClip(id: String, newStartMs: Long) {
        recordVideoUndoSnapshot(coalesceTag = "audio_drag_$id")
        _videoState.update { s ->
            val maxStart = s.totalDurationMs.coerceAtLeast(0L)
            s.copy(
                selectedAudioClipId = id,
                selectedClipId = null,
                audioLaneClips = s.audioLaneClips.map { a ->
                    if (a.id == id) a.copy(startMs = newStartMs.coerceIn(0L, maxStart)) else a
                }
            )
        }
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun deleteSelectedVideoAudioClip() {
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val id = s.selectedAudioClip?.id ?: return@update s
            val rem = s.audioLaneClips.filterNot { it.id == id }
            s.copy(audioLaneClips = rem, selectedAudioClipId = rem.lastOrNull()?.id)
        }
        showToast("Audio clip removed")
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun playSelectedVideoAudioClip() {
        val clip = _videoState.value.selectedAudioClip ?: return
        val path = clip.filePath ?: return
        viewModelScope.launch {
            val tempTrack = AudioTrackState(
                role = AudioTrackRole.BACKGROUND_MUSIC,
                sourceName = clip.name,
                filePath = path,
                volume = clip.volume,
                muted = clip.muted,
                fadeInSec = clip.fadeInSec,
                fadeOutSec = clip.fadeOutSec,
                trimStartPct = clip.trimStartPct,
                trimEndPct = clip.trimEndPct
            )
            val pcm = AudioDspEngine.mixThreeTracks(listOf(tempTrack))
            if (pcm.isNotEmpty()) AudioDspEngine.playPcm(pcm)
        }
    }

    // --- Diamond Keyframe Animation ---

    fun getSelectedClipLocalTimeMs(): Long {
        val s = _videoState.value
        val hit = findClipAtPlayhead(s)
        if (hit != null && hit.second.id == s.selectedClip?.id) {
            return hit.third
        }
        val target = s.selectedClip ?: return 0L
        var offset = 0L
        for (c in s.clips) {
            if (c.id == target.id) {
                return (s.playheadMs - offset).coerceIn(0L, target.effectiveDurationMs)
            }
            offset += c.effectiveDurationMs
        }
        return 0L
    }

    fun addOrUpdateKeyframeAtPlayhead(
        xNorm: Float? = null,
        yNorm: Float? = null,
        scale: Float? = null,
        rotationDeg: Float? = null,
        opacity: Float? = null
    ) {
        val snapshot = _videoState.value
        val hit = findClipAtPlayhead(snapshot) ?: return
        val (_, targetClip, exactLocalMs) = hit
        val isSliderUpdate = xNorm != null || yNorm != null || scale != null || rotationDeg != null || opacity != null
        recordVideoUndoSnapshot(coalesceTag = if (isSliderUpdate) "kf_slider" else null)

        _videoState.update { s ->
            val nextClips = s.clips.map { clip ->
                if (clip.id != targetClip.id) return@map clip
                val currentInterp = VideoMediaEngine.interpolateKeyframes(clip.keyframes, exactLocalMs)
                // Tight 45ms tolerance so user can add multiple distinct keyframes anywhere along the clip
                val existingIdx = clip.keyframes.indexOfFirst { kotlin.math.abs(it.timeMs - exactLocalMs) <= 45L }
                val updatedKf = if (existingIdx >= 0) {
                    val ex = clip.keyframes[existingIdx]
                    ex.copy(
                        timeMs = exactLocalMs,
                        xNorm = xNorm ?: ex.xNorm,
                        yNorm = yNorm ?: ex.yNorm,
                        scale = scale ?: ex.scale,
                        rotationDeg = rotationDeg ?: ex.rotationDeg,
                        opacity = opacity ?: ex.opacity
                    )
                } else {
                    VideoKeyframe(
                        id = "kf_${UUID.randomUUID().toString().take(6)}",
                        timeMs = exactLocalMs,
                        xNorm = xNorm ?: currentInterp.xNorm,
                        yNorm = yNorm ?: currentInterp.yNorm,
                        scale = scale ?: currentInterp.scale,
                        rotationDeg = rotationDeg ?: currentInterp.rotationDeg,
                        opacity = opacity ?: currentInterp.opacity
                    )
                }
                val nextList = if (existingIdx >= 0) {
                    clip.keyframes.mapIndexed { i, k -> if (i == existingIdx) updatedKf else k }
                } else {
                    (clip.keyframes + updatedKf).sortedBy { it.timeMs }
                }
                clip.copy(keyframes = nextList)
            }
            s.copy(clips = nextClips, selectedClipId = targetClip.id)
        }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun deleteKeyframe(id: String) {
        updateSelectedClip { c -> c.copy(keyframes = c.keyframes.filterNot { it.id == id }) }
        showToast("Keyframe removed")
    }

    fun clearKeyframesOnSelectedClip() {
        updateSelectedClip { it.copy(keyframes = emptyList()) }
        showToast("Keyframes cleared")
    }

    fun addVideoSticker(symbol: String) {
        val now = _videoState.value.playheadMs
        val total = _videoState.value.totalDurationMs
        val layer = VideoTextOverlay(
            id = "vstk_${UUID.randomUUID().toString().take(8)}",
            text = symbol,
            fontId = "bangers",
            colorArgb = 0xFFFFFFFF.toInt(),
            strokeArgb = 0xFF000000.toInt(),
            strokeWidth = 4f,
            bgEnabled = false,
            xNorm = 0.5f,
            yNorm = 0.45f,
            scale = 1.6f,
            startMs = now,
            endMs = (now + 3500L).coerceAtMost(total),
            animation = TextAnimationStyle.POP,
            isSticker = true
        )
        _videoState.update { it.copy(textLayers = it.textLayers + layer, selectedTextId = layer.id) }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
        showToast("Sticker added")
    }

    fun seekPlayhead(ms: Long) {
        _videoState.update { it.copy(playheadMs = ms.coerceIn(0L, it.totalDurationMs)) }
        scheduleVideoPreviewRender(immediate = true)
    }

    fun toggleVideoPlayback() {
        if (_videoState.value.isPlaying) {
            stopVideoPlayback()
        } else {
            _videoState.update { it.copy(isPlaying = true) }
            playbackJob?.cancel()
            playbackJob = viewModelScope.launch {
                val frameStepMs = 90L
                while (isActive && _videoState.value.isPlaying) {
                    val s = _videoState.value
                    val next = s.playheadMs + frameStepMs
                    if (next >= s.totalDurationMs) {
                        if (s.loopPlayback) {
                            _videoState.update { it.copy(playheadMs = 0L) }
                        } else {
                            _videoState.update { it.copy(isPlaying = false, playheadMs = s.totalDurationMs) }
                            break
                        }
                    } else {
                        _videoState.update { it.copy(playheadMs = next) }
                    }
                    val rendered = withContext(Dispatchers.Default) {
                        VideoMediaEngine.renderPreviewAtPlayhead(appContext, _videoState.value, maxEdge = 640)
                    }
                    _videoPreviewBitmap.value = rendered
                    delay(frameStepMs)
                }
            }
        }
    }

    fun stopVideoPlayback() {
        playbackJob?.cancel()
        _videoState.update { it.copy(isPlaying = false) }
    }

    fun toggleLoopPlayback() {
        _videoState.update { it.copy(loopPlayback = !it.loopPlayback) }
    }

    fun captureFreezeFrameHd() {
        viewModelScope.launch {
            _exportProgress.value = 0.5f
            val res = VideoMediaEngine.captureFreezeFrameHd(appContext, _videoState.value)
            withContext(Dispatchers.IO) { AppSecurityFirewall.cleanupTempExportCache(appContext) }
            _exportProgress.value = null
            if (res.success) {
                showToast("HD Freeze Frame saved (${MediaExportStore.formatSize(res.byteCount)})")
            } else {
                showToast(res.error ?: "Freeze frame failed")
            }
        }
    }

    fun setVideoAspect(preset: VideoAspectPreset) {
        recordVideoUndoSnapshot()
        _videoState.update { it.copy(aspectPreset = preset) }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun updateVideoFraming(zoom: Float? = null, panX: Float? = null, panY: Float? = null, blurredBg: Boolean? = null) {
        recordVideoUndoSnapshot(coalesceTag = "framing")
        _videoState.update { s ->
            s.copy(
                cropZoom = zoom ?: s.cropZoom,
                panX = panX ?: s.panX,
                panY = panY ?: s.panY,
                blurredBgFill = blurredBg ?: s.blurredBgFill
            )
        }
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun updateVideoCanvasTransform(
        zoom: Float? = null,
        panX: Float? = null,
        panY: Float? = null,
        rotationDeg: Float? = null
    ) {
        recordVideoUndoSnapshot(coalesceTag = "canvas_transform")
        _videoState.update { s ->
            val selId = findClipAtPlayhead(s)?.second?.id ?: s.selectedClip?.id
            val nextClips = if (rotationDeg != null && selId != null) {
                s.clips.map { c -> if (c.id == selId) c.copy(rotationDeg = rotationDeg) else c }
            } else {
                s.clips
            }
            s.copy(
                cropZoom = (zoom ?: s.cropZoom).coerceIn(0.25f, 3.0f),
                panX = (panX ?: s.panX).coerceIn(-1f, 1f),
                panY = (panY ?: s.panY).coerceIn(-1f, 1f),
                clips = nextClips
            )
        }
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun updateVideoColorGrade(transform: (VideoColorGrade) -> VideoColorGrade) {
        recordVideoUndoSnapshot(coalesceTag = "color_grade")
        _videoState.update { it.copy(colorGrade = transform(it.colorGrade)) }
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun resetVideoColorGrade() {
        recordVideoUndoSnapshot()
        _videoState.update { it.copy(colorGrade = VideoColorGrade()) }
        scheduleVideoPreviewRender(true)
    }

    fun updatePip(transform: (PipOverlayState) -> PipOverlayState) {
        recordVideoUndoSnapshot(coalesceTag = "pip_edit")
        _videoState.update { it.copy(pip = transform(it.pip)) }
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun importPipImage(uri: Uri) {
        if (!AppSecurityFirewall.validateMediaUri(appContext, uri, "image/")) {
            showToast("Unsupported image file")
            return
        }
        val name = AppSecurityFirewall.sanitizeText(queryFileName(uri) ?: "Overlay", maxLength = 36)
        recordVideoUndoSnapshot()
        updatePip {
            it.copy(
                enabled = true,
                uriString = uri.toString(),
                fallbackDrawable = null,
                label = name
            )
        }
        showToast("Overlay added")
    }

    fun removePipOverlay() {
        recordVideoUndoSnapshot()
        _videoState.update {
            it.copy(
                pip = PipOverlayState(
                    enabled = false,
                    uriString = null,
                    fallbackDrawable = null,
                    label = "Overlay"
                )
            )
        }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
        showToast("Overlay removed")
    }

    fun addVideoText(text: String, isCaption: Boolean = false) {
        recordVideoUndoSnapshot()
        val cleanText = AppSecurityFirewall.sanitizeText(text, maxLength = 80).ifBlank { "NEW TITLE" }
        val now = _videoState.value.playheadMs
        val total = _videoState.value.totalDurationMs
        val layer = VideoTextOverlay(
            id = "vtxt_${UUID.randomUUID().toString().take(8)}",
            text = cleanText,
            fontId = if (isCaption) "space_grotesk" else "anton",
            colorArgb = if (isCaption) 0xFFFFFFFF.toInt() else 0xFF00E5FF.toInt(),
            strokeArgb = 0xFF000000.toInt(),
            strokeWidth = if (isCaption) 4f else 7f,
            xNorm = 0.5f,
            yNorm = if (isCaption) 0.88f else 0.25f,
            scale = if (isCaption) 0.85f else 1.05f,
            startMs = now,
            endMs = (now + 3500L).coerceAtMost(total),
            animation = if (isCaption) TextAnimationStyle.FADE else TextAnimationStyle.POP,
            isCaption = isCaption
        )
        _videoState.update { it.copy(textLayers = it.textLayers + layer, selectedTextId = layer.id) }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun selectVideoText(id: String) {
        _videoState.update { it.copy(selectedTextId = id) }
    }

    fun updateSelectedVideoText(transform: (VideoTextOverlay) -> VideoTextOverlay) {
        recordVideoUndoSnapshot(coalesceTag = "text_edit")
        _videoState.update { s ->
            val id = s.selectedTextId ?: return@update s
            s.copy(textLayers = s.textLayers.map { if (it.id == id) transform(it) else it })
        }
        scheduleVideoPreviewRender()
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun deleteSelectedVideoText() {
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val id = s.selectedTextId ?: return@update s
            val rem = s.textLayers.filterNot { it.id == id }
            s.copy(textLayers = rem, selectedTextId = rem.lastOrNull()?.id)
        }
        scheduleVideoPreviewRender(true)
    }

    fun setListeningCaptions(listening: Boolean) {
        _videoState.update { it.copy(isListeningCaptions = listening) }
    }

    fun setVideoExportResolution(res: VideoExportResolution) {
        _videoState.update { it.copy(exportResolution = res) }
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun setVideoExportFps(fps: VideoExportFps) {
        _videoState.update { it.copy(exportFps = fps) }
        scheduleAutoSave(WorkspaceType.VIDEO)
    }

    fun toggleVideoQualityEnhancer() {
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            val next = !s.colorGrade.qualityEnhancerEnabled
            s.copy(colorGrade = s.colorGrade.copy(qualityEnhancerEnabled = next))
        }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
        val enabled = _videoState.value.colorGrade.qualityEnhancerEnabled
        showToast(if (enabled) "Enhance ON — sharper, clearer video" else "Enhance OFF")
    }

    /**
     * AI Magic Auto-Color Enhance: analyzes the current preview frame's pixel luminance,
     * dynamic range, and chroma saturation on-device and applies optimal color balance + clarity.
     */
    fun autoColorEnhanceVideo() {
        recordVideoUndoSnapshot()
        viewModelScope.launch {
            val stats = withContext(Dispatchers.Default) {
                analyzeBitmapColorStats(_videoPreviewBitmap.value)
            }
            _videoState.update { s ->
                s.copy(
                    colorGrade = s.colorGrade.copy(
                        brightness = stats.brightnessBoost,
                        contrast = stats.contrastBoost,
                        saturation = stats.saturationBoost,
                        exposure = stats.exposureBoost,
                        temperature = stats.warmthBoost,
                        sharpen = stats.sharpenBoost,
                        qualityEnhancerEnabled = true
                    )
                )
            }
            scheduleVideoPreviewRender(true)
            scheduleAutoSave(WorkspaceType.VIDEO)
            showToast("AI Magic Color Enhance applied")
        }
    }

    fun applyExportPreset(preset: VideoExportPreset, startExportImmediately: Boolean = false) {
        recordVideoUndoSnapshot()
        _videoState.update { s ->
            s.copy(
                aspectPreset = preset.aspect ?: s.aspectPreset,
                exportResolution = preset.resolution,
                exportFps = preset.fps
            )
        }
        scheduleVideoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.VIDEO)
        if (startExportImmediately) {
            showToast("Exporting ${preset.label}...")
            exportVideo()
        } else {
            showToast("Preset applied: ${preset.label}")
        }
    }

    fun exportVideo() {
        stopVideoPlayback()
        viewModelScope.launch {
            _exportProgress.value = 0.02f
            val result = VideoMediaEngine.exportVideoToMp4(
                context = appContext,
                state = _videoState.value,
                capabilities = _encoderCapabilities.value
            ) { p ->
                _exportProgress.value = p
            }
            _exportProgress.value = null
            handleExportFeedback("Video", result)
        }
    }

    private data class AutoColorStats(
        val brightnessBoost: Float,
        val contrastBoost: Float,
        val saturationBoost: Float,
        val exposureBoost: Float,
        val warmthBoost: Float,
        val sharpenBoost: Float
    )

    private fun analyzeBitmapColorStats(bmp: Bitmap?): AutoColorStats {
        if (bmp == null || bmp.isRecycled || bmp.width < 4 || bmp.height < 4) {
            return AutoColorStats(
                brightnessBoost = 0.05f,
                contrastBoost = 1.16f,
                saturationBoost = 1.20f,
                exposureBoost = 0.06f,
                warmthBoost = 0.06f,
                sharpenBoost = 0.35f
            )
        }
        val stepX = (bmp.width / 24).coerceAtLeast(1)
        val stepY = (bmp.height / 24).coerceAtLeast(1)
        var count = 0
        var lumaSum = 0f
        var lumaSqSum = 0f
        var satSum = 0f
        var warmDiffSum = 0f
        var y = 0
        while (y < bmp.height) {
            var x = 0
            while (x < bmp.width) {
                val px = bmp.getPixel(x, y)
                val r = ((px ushr 16) and 0xFF) / 255f
                val g = ((px ushr 8) and 0xFF) / 255f
                val b = (px and 0xFF) / 255f
                val luma = 0.299f * r + 0.587f * g + 0.114f * b
                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                val sat = if (maxC > 0.001f) (maxC - minC) / maxC else 0f
                lumaSum += luma
                lumaSqSum += luma * luma
                satSum += sat
                warmDiffSum += (r - b)
                count++
                x += stepX
            }
            y += stepY
        }
        val n = count.coerceAtLeast(1).toFloat()
        val meanLuma = lumaSum / n
        val stdLuma = kotlin.math.sqrt((lumaSqSum / n - meanLuma * meanLuma).coerceAtLeast(0f))
        val meanSat = satSum / n
        val meanWarm = warmDiffSum / n

        val brightness = ((0.50f - meanLuma) * 0.32f).coerceIn(-0.14f, 0.20f)
        val exposure = ((0.48f - meanLuma) * 0.24f).coerceIn(-0.10f, 0.18f)
        val contrast = (1.12f + (0.22f - stdLuma).coerceAtLeast(0f) * 0.65f).coerceIn(1.06f, 1.32f)
        val saturation = (1.14f + (0.42f - meanSat).coerceAtLeast(0f) * 0.55f).coerceIn(1.08f, 1.38f)
        val warmth = (-meanWarm * 0.28f + 0.05f).coerceIn(-0.12f, 0.16f)
        return AutoColorStats(
            brightnessBoost = brightness,
            contrastBoost = contrast,
            saturationBoost = saturation,
            exposureBoost = exposure,
            warmthBoost = warmth,
            sharpenBoost = 0.36f
        )
    }

    // =========================================================================
    // WORKSPACE 2: PRO PHOTO & THUMBNAIL STUDIO ACTIONS
    // =========================================================================

    private fun createInitialPhotoState(): PhotoStudioState {
        val bg = PhotoLayer(
            id = "layer_bg",
            name = "Cyber City Backdrop",
            kind = LayerKind.IMAGE,
            isBackground = true,
            fallbackDrawable = R.drawable.img_studio_sample_bg_1795185351810,
            xNorm = 0.5f,
            yNorm = 0.5f,
            scale = 1.0f
        )
        val cutoutSubject = PhotoLayer(
            id = "layer_subject",
            name = "Pro Gear Cutout",
            kind = LayerKind.IMAGE,
            fallbackDrawable = R.drawable.img_studio_sample_subject_1795185350043,
            xNorm = 0.72f,
            yNorm = 0.54f,
            scale = 0.96f,
            chromaEnabled = true,
            chromaColor = 0xFF00FF00.toInt(),
            chromaTolerance = 0.40f,
            outlineGlowEnabled = true,
            outlineGlowColor = 0xFFFFFFFF.toInt(),
            outlineGlowWidth = 11f
        )
        val arrowShape = PhotoLayer(
            id = "layer_arrow",
            name = "Highlight Arrow",
            kind = LayerKind.SHAPE,
            shapeKind = StudioShapeKind.ARROW,
            xNorm = 0.43f,
            yNorm = 0.66f,
            scale = 0.85f,
            rotationDeg = -18f,
            fillColor = 0xFFFF3B5C.toInt(),
            strokeColor = 0xFFFFFFFF.toInt(),
            strokeWidth = 6f
        )
        val titleText = PhotoLayer(
            id = "layer_headline",
            name = "Viral Headline",
            kind = LayerKind.TEXT,
            text = "BLACK DEVIL\nSTUDIO!",
            fontId = "bangers",
            xNorm = 0.28f,
            yNorm = 0.36f,
            scale = 1.35f,
            rotationDeg = -4f,
            textColor = 0xFFFFFFFF.toInt(),
            textUseGradient = true,
            textGradientEndColor = 0xFFFF1E42.toInt(),
            textStrokeColor = 0xFF000000.toInt(),
            textStrokeWidth = 12f,
            textShadowEnabled = true,
            text3dDepth = 6f
        )
        val badgeSticker = PhotoLayer(
            id = "layer_badge",
            name = "4K PRO Badge",
            kind = LayerKind.STICKER,
            stickerSymbol = "4K 60FPS",
            xNorm = 0.22f,
            yNorm = 0.80f,
            scale = 0.95f,
            fillColor = 0xFFFF1E42.toInt(),
            textGradientEndColor = 0xFFC026D3.toInt()
        )
        return PhotoStudioState(
            preset = PhotoCanvasPreset.YOUTUBE_THUMB,
            layers = listOf(bg, arrowShape, cutoutSubject, titleText, badgeSticker),
            selectedLayerId = cutoutSubject.id,
            dslrBackgroundBlur = 0.35f
        )
    }

    fun autoColorEnhancePhoto() {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.Default) {
                analyzeBitmapColorStats(_photoPreviewBitmap.value)
            }
            _photoState.update { s ->
                s.copy(
                    lightroom = s.lightroom.copy(
                        brightness = stats.brightnessBoost,
                        contrast = stats.contrastBoost,
                        saturation = stats.saturationBoost,
                        vibrance = 0.22f,
                        warmth = stats.warmthBoost,
                        clarity = 0.26f,
                        sharpness = stats.sharpenBoost
                    )
                )
            }
            schedulePhotoPreviewRender(true)
            scheduleAutoSave(WorkspaceType.PHOTO)
            showToast("AI Magic Auto-Color applied to photo")
        }
    }

    private fun schedulePhotoPreviewRender(immediate: Boolean = false) {
        if (immediate) photoRenderImmediate.set(true)
        photoRenderDirty.set(true)
        if (photoRenderJob?.isActive == true) return
        photoRenderJob = viewModelScope.launch {
            while (photoRenderDirty.getAndSet(false)) {
                val isFastImmediate = photoRenderImmediate.getAndSet(false)
                if (!isFastImmediate) delay(18L)
                val snapshot = _photoState.value
                val targetEdge = if (isFastImmediate) 840 else 680
                val bmp = ImageProcessingEngine.renderPhotoComposition(
                    context = appContext,
                    state = snapshot,
                    maxEdge = targetEdge,
                    includePreviewGuides = true
                )
                _photoPreviewBitmap.value = bmp
            }
        }
    }

    fun setPhotoCollageLayout(layout: CollageLayoutKind) {
        _photoState.update { it.copy(collageLayout = layout) }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun setPhotoBorder(widthPx: Float? = null, colorArgb: Int? = null) {
        _photoState.update {
            it.copy(
                borderWidthPx = widthPx?.coerceIn(0f, 48f) ?: it.borderWidthPx,
                borderColorArgb = colorArgb ?: it.borderColorArgb
            )
        }
        schedulePhotoPreviewRender()
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun togglePhotoSafeZoneGrid() {
        _photoState.update { it.copy(showSafeZoneGrid = !it.showSafeZoneGrid) }
        schedulePhotoPreviewRender(true)
    }

    fun duplicatePhotoLayer(id: String) {
        _photoState.update { s ->
            val src = s.layers.firstOrNull { it.id == id } ?: return@update s
            val copy = src.copy(
                id = "lyr_${UUID.randomUUID().toString().take(8)}",
                name = "${src.name} Copy",
                isBackground = false,
                xNorm = (src.xNorm + 0.05f).coerceAtMost(0.92f),
                yNorm = (src.yNorm + 0.05f).coerceAtMost(0.92f)
            )
            s.copy(layers = s.layers + copy, selectedLayerId = copy.id)
        }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
        showToast("Layer duplicated")
    }

    fun setPhotoCanvasPreset(preset: PhotoCanvasPreset) {
        _photoState.update { it.copy(preset = preset) }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun setPhotoCustomSize(w: Int, h: Int) {
        _photoState.update {
            it.copy(
                preset = PhotoCanvasPreset.CUSTOM,
                customWidth = w.coerceIn(320, 3840),
                customHeight = h.coerceIn(320, 3840)
            )
        }
        schedulePhotoPreviewRender(true)
    }

    fun selectPhotoLayer(id: String) {
        _photoState.update { it.copy(selectedLayerId = id) }
    }

    fun importPhotoLayer(uri: Uri, asBackground: Boolean = false) {
        if (!AppSecurityFirewall.validateMediaUri(appContext, uri, "image/")) {
            showToast("Unsupported photo file")
            return
        }
        val name = AppSecurityFirewall.sanitizeText(
            queryFileName(uri)?.substringBeforeLast(".") ?: "Photo Layer",
            maxLength = 36
        )
        val layer = PhotoLayer(
            id = "lyr_${UUID.randomUUID().toString().take(8)}",
            name = name,
            kind = LayerKind.IMAGE,
            isBackground = asBackground,
            imageUri = uri.toString(),
            xNorm = 0.5f,
            yNorm = 0.5f,
            scale = if (asBackground) 1f else 0.78f
        )
        _photoState.update { s ->
            val list = if (asBackground) {
                listOf(layer) + s.layers.filterNot { it.isBackground }
            } else {
                s.layers + layer
            }
            s.copy(layers = list, selectedLayerId = layer.id)
        }
        showToast("Added photo: $name")
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun addPhotoShapeLayer(shape: StudioShapeKind) {
        val layer = PhotoLayer(
            id = "shp_${UUID.randomUUID().toString().take(8)}",
            name = "${shape.label} Shape",
            kind = LayerKind.SHAPE,
            shapeKind = shape,
            xNorm = 0.5f,
            yNorm = 0.5f,
            scale = 0.9f,
            fillColor = 0xFF00E5FF.toInt(),
            strokeColor = 0xFFFFFFFF.toInt(),
            strokeWidth = 6f
        )
        _photoState.update { it.copy(layers = it.layers + layer, selectedLayerId = layer.id) }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun addPhotoTextLayer(initialText: String = "EPIC MOMENT") {
        val layer = PhotoLayer(
            id = "txt_${UUID.randomUUID().toString().take(8)}",
            name = initialText.take(14),
            kind = LayerKind.TEXT,
            text = initialText,
            fontId = "anton",
            xNorm = 0.5f,
            yNorm = 0.45f,
            scale = 1.2f,
            textColor = 0xFFFFFFFF.toInt(),
            textUseGradient = true,
            textGradientEndColor = 0xFFFFB300.toInt(),
            textStrokeColor = 0xFF000000.toInt(),
            textStrokeWidth = 10f
        )
        _photoState.update { it.copy(layers = it.layers + layer, selectedLayerId = layer.id) }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun addPhotoStickerLayer(label: String) {
        val layer = PhotoLayer(
            id = "stk_${UUID.randomUUID().toString().take(8)}",
            name = "$label Sticker",
            kind = LayerKind.STICKER,
            stickerSymbol = label,
            xNorm = 0.5f,
            yNorm = 0.25f,
            scale = 0.95f,
            fillColor = 0xFFFF3B5C.toInt(),
            textGradientEndColor = 0xFFFFB300.toInt()
        )
        _photoState.update { it.copy(layers = it.layers + layer, selectedLayerId = layer.id) }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun clearPhotoLayerSelection() {
        _photoState.update { it.copy(selectedLayerId = null) }
    }

    fun updatePhotoLayerById(id: String, transform: (PhotoLayer) -> PhotoLayer) {
        _photoState.update { s ->
            s.copy(layers = s.layers.map { if (it.id == id && !it.locked) transform(it) else it })
        }
        schedulePhotoPreviewRender()
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun updateSelectedPhotoLayer(transform: (PhotoLayer) -> PhotoLayer) {
        _photoState.update { s ->
            val id = s.selectedLayerId ?: return@update s
            s.copy(layers = s.layers.map { if (it.id == id && !it.locked) transform(it) else it })
        }
        schedulePhotoPreviewRender()
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun toggleLayerVisibility(id: String) {
        _photoState.update { s ->
            s.copy(layers = s.layers.map { if (it.id == id) it.copy(visible = !it.visible) else it })
        }
        schedulePhotoPreviewRender(true)
    }

    fun toggleLayerLock(id: String) {
        _photoState.update { s ->
            s.copy(layers = s.layers.map { if (it.id == id) it.copy(locked = !it.locked) else it })
        }
    }

    fun reorderPhotoLayer(id: String, bringForward: Boolean) {
        _photoState.update { s ->
            val idx = s.layers.indexOfFirst { it.id == id }
            if (idx < 0) return@update s
            val target = if (bringForward) idx + 1 else idx - 1
            if (target !in s.layers.indices) return@update s
            val mutable = s.layers.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(target, item)
            s.copy(layers = mutable)
        }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun deletePhotoLayer(id: String) {
        _photoState.update { s ->
            val rem = s.layers.filterNot { it.id == id }
            s.copy(layers = rem, selectedLayerId = rem.lastOrNull()?.id)
        }
        schedulePhotoPreviewRender(true)
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun dragSelectedPhotoLayer(dxNorm: Float, dyNorm: Float) {
        transformSelectedPhotoLayer(dxNorm, dyNorm, 1f, 0f)
    }

    fun transformSelectedPhotoLayer(
        dxNorm: Float,
        dyNorm: Float,
        zoomFactor: Float,
        rotationDeltaDeg: Float
    ) {
        _photoState.update { s ->
            val id = s.selectedLayerId ?: return@update s
            s.copy(layers = s.layers.map { l ->
                if (l.id == id && !l.locked) {
                    var nextRot = (l.rotationDeg + rotationDeltaDeg) % 360f
                    if (nextRot > 180f) nextRot -= 360f
                    if (nextRot < -180f) nextRot += 360f
                    l.copy(
                        xNorm = (l.xNorm + dxNorm).coerceIn(0.02f, 0.98f),
                        yNorm = (l.yNorm + dyNorm).coerceIn(0.02f, 0.98f),
                        scale = (l.scale * zoomFactor).coerceIn(0.25f, 3.0f),
                        rotationDeg = nextRot
                    )
                } else l
            })
        }
        schedulePhotoPreviewRender()
    }

    fun setEraserMode(active: Boolean) {
        setPhotoTouchTool(eraser = active, restore = false, spotHeal = false)
    }

    fun setPhotoTouchTool(eraser: Boolean = false, restore: Boolean = false, spotHeal: Boolean = false) {
        _photoState.update {
            it.copy(
                eraserModeActive = eraser,
                restoreBrushActive = restore,
                spotHealModeActive = spotHeal
            )
        }
    }

    fun updateEraserBrush(size: Float? = null, softness: Float? = null) {
        _photoState.update {
            it.copy(
                eraserBrushSize = size ?: it.eraserBrushSize,
                eraserSoftness = softness ?: it.eraserSoftness
            )
        }
    }

    fun handlePhotoBrushTouch(canvasXNorm: Float, canvasYNorm: Float) {
        val state = _photoState.value
        val layer = state.selectedLayer ?: return
        if (layer.kind != LayerKind.IMAGE || layer.locked) return
        val localX = ((canvasXNorm - layer.xNorm) / (0.68f * layer.scale.coerceAtLeast(0.2f)) + 0.5f).coerceIn(0f, 1f)
        val localY = ((canvasYNorm - layer.yNorm) / (0.68f * layer.scale.coerceAtLeast(0.2f)) + 0.5f).coerceIn(0f, 1f)
        when {
            state.eraserModeActive -> {
                val pt = EraserStrokePoint(localX, localY, state.eraserBrushSize, state.eraserSoftness)
                updateSelectedPhotoLayer { it.copy(eraserStrokes = (it.eraserStrokes + pt).takeLast(180)) }
            }
            state.restoreBrushActive -> {
                val thresh = (state.eraserBrushSize * 1.45f).coerceAtLeast(0.05f)
                updateSelectedPhotoLayer { l ->
                    l.copy(eraserStrokes = l.eraserStrokes.filterNot { pt ->
                        val dx = pt.xNorm - localX
                        val dy = pt.yNorm - localY
                        (dx * dx + dy * dy) <= thresh * thresh
                    })
                }
            }
            state.spotHealModeActive -> {
                val healPt = SpotHealPoint(localX, localY, state.eraserBrushSize.coerceIn(0.025f, 0.12f))
                updateSelectedPhotoLayer { it.copy(spotHealPoints = (it.spotHealPoints + healPt).takeLast(60)) }
            }
        }
    }

    fun addEraserTouchPoint(canvasXNorm: Float, canvasYNorm: Float) {
        handlePhotoBrushTouch(canvasXNorm, canvasYNorm)
    }

    fun clearEraserStrokesOnSelected() {
        updateSelectedPhotoLayer { it.copy(eraserStrokes = emptyList()) }
        showToast("Eraser strokes cleared")
    }

    fun clearSpotHealOnSelected() {
        updateSelectedPhotoLayer { it.copy(spotHealPoints = emptyList()) }
        showToast("Spot heal points cleared")
    }

    fun setDslrBackgroundBlur(amount: Float) {
        _photoState.update { it.copy(dslrBackgroundBlur = amount.coerceIn(0f, 1f)) }
        schedulePhotoPreviewRender()
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun updateLightroom(transform: (LightroomGrade) -> LightroomGrade) {
        _photoState.update { it.copy(lightroom = transform(it.lightroom)) }
        schedulePhotoPreviewRender()
        scheduleAutoSave(WorkspaceType.PHOTO)
    }

    fun resetLightroom() {
        _photoState.update { it.copy(lightroom = LightroomGrade()) }
        schedulePhotoPreviewRender(true)
    }

    fun setPhotoExportFormat(format: PhotoExportFormat, jpgQuality: Int? = null) {
        _photoState.update {
            it.copy(
                exportFormat = format,
                jpgQuality = jpgQuality ?: it.jpgQuality
            )
        }
    }

    fun exportPhoto() {
        viewModelScope.launch {
            _exportProgress.value = 0.35f
            val res = ImageProcessingEngine.exportPhotoToGallery(appContext, _photoState.value)
            withContext(Dispatchers.IO) { AppSecurityFirewall.cleanupTempExportCache(appContext) }
            _exportProgress.value = null
            handleExportFeedback(_photoState.value.exportFormat.label, res)
        }
    }

    // =========================================================================
    // WORKSPACE 3: PRO AUDIO & COPYRIGHT-SAFE STUDIO ACTIONS
    // =========================================================================

    private fun createInitialAudioState(): AudioStudioState {
        return AudioStudioState(
            tracks = listOf(
                AudioTrackState(
                    role = AudioTrackRole.ORIGINAL_VIDEO,
                    sourceName = "Video Audio (Tap Pick Video)",
                    volume = 1.0f,
                    fadeInSec = 0.2f,
                    fadeOutSec = 0.4f,
                    durationMs = 4000L
                ),
                AudioTrackState(
                    role = AudioTrackRole.BACKGROUND_MUSIC,
                    sourceName = "Free Synth Beat",
                    volume = 0.72f,
                    fadeInSec = 0.6f,
                    fadeOutSec = 0.8f,
                    durationMs = 4000L
                ),
                AudioTrackState(
                    role = AudioTrackRole.VOICEOVER,
                    sourceName = "Tap Voice to Record",
                    volume = 1.25f,
                    fadeInSec = 0.1f,
                    fadeOutSec = 0.2f,
                    durationMs = 0L
                )
            ),
            copyrightChecklist = listOf(
                CopyrightCheckItem(
                    id = "chk_source",
                    title = "Verified Royalty-Free Source",
                    subtitle = "Track sourced from YouTube Audio Library, Pixabay, NCS, or Black Devil SFX",
                    checked = true
                ),
                CopyrightCheckItem(
                    id = "chk_attribution",
                    title = "Creator Attribution / License Ready",
                    subtitle = "NCS or CC-BY credit pasted into video description if required",
                    checked = false
                ),
                CopyrightCheckItem(
                    id = "chk_duration",
                    title = "No Unlicensed Commercial Hooks",
                    subtitle = "Even 5-8s of copyrighted chart music triggers YouTube Content ID",
                    checked = false
                ),
                CopyrightCheckItem(
                    id = "chk_remix",
                    title = "Original Remix & Transformative Rule",
                    subtitle = "Pitch/speed tweaks alone do NOT bypass Content ID fingerprinting",
                    checked = false
                ),
                CopyrightCheckItem(
                    id = "chk_sfx",
                    title = "SFX & Stingers Cleared",
                    subtitle = "All risers, hits, and transitions are procedural or licensed",
                    checked = true
                )
            )
        )
    }

    private fun prepareStarterAudioFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(appContext.cacheDir, "audio_tracks").apply { mkdirs() }
            val origFile = File(dir, "starter_original.wav")
            val bgmFile = File(dir, "starter_bgm.wav")
            val origPcm = AudioDspEngine.generateStarterBeatPcm(4.0, isVoiceDemo = true)
            val bgmPcm = AudioDspEngine.generateStarterBeatPcm(4.0, isVoiceDemo = false)
            AudioDspEngine.writePcmWavFile(origFile, origPcm)
            AudioDspEngine.writePcmWavFile(bgmFile, bgmPcm)

            val origWave = AudioDspEngine.computeWaveformBars(origPcm)
            val bgmWave = AudioDspEngine.computeWaveformBars(bgmPcm)

            _audioState.update { s ->
                s.copy(tracks = s.tracks.map { t ->
                    when (t.role) {
                        AudioTrackRole.ORIGINAL_VIDEO -> t.copy(filePath = origFile.absolutePath, waveform = origWave)
                        AudioTrackRole.BACKGROUND_MUSIC -> t.copy(filePath = bgmFile.absolutePath, waveform = bgmWave)
                        AudioTrackRole.VOICEOVER -> t.copy(waveform = List(44) { 0.08f })
                    }
                })
            }

            if (_videoState.value.audioLaneClips.isEmpty()) {
                val starterLaneClip = VideoAudioClip(
                    id = "vaud_starter_beat",
                    name = "Crimson Synth Beat",
                    filePath = bgmFile.absolutePath,
                    startMs = 0L,
                    durationMs = 4000L,
                    volume = 0.85f
                )
                _videoState.update { s ->
                    if (s.audioLaneClips.isEmpty()) {
                        s.copy(audioLaneClips = listOf(starterLaneClip), selectedAudioClipId = starterLaneClip.id)
                    } else s
                }
            }
        }
    }

    fun updateAudioTrack(role: AudioTrackRole, transform: (AudioTrackState) -> AudioTrackState) {
        _audioState.update { s ->
            s.copy(tracks = s.tracks.map { if (it.role == role) transform(it) else it })
        }
        scheduleAutoSave(WorkspaceType.AUDIO)
    }

    fun extractAudioFromVideoUri(uri: Uri, targetRole: AudioTrackRole = AudioTrackRole.ORIGINAL_VIDEO) {
        if (!AppSecurityFirewall.validateMediaUri(appContext, uri, null)) {
            showToast("Unsupported media file")
            return
        }
        viewModelScope.launch {
            _exportProgress.value = 0.4f
            val name = AppSecurityFirewall.sanitizeText(queryFileName(uri) ?: "Audio Clip", maxLength = 36)
            val dir = File(appContext.cacheDir, "audio_tracks").apply { mkdirs() }
            val outFile = File(dir, "extracted_${targetRole.name.lowercase()}_${System.currentTimeMillis()}.wav")
            val extracted = AudioDspEngine.extractAudioFromMediaUri(appContext, uri, outFile)
            _exportProgress.value = null
            if (extracted != null) {
                val (pcm, durMs) = extracted
                val wave = AudioDspEngine.computeWaveformBars(pcm)
                updateAudioTrack(targetRole) {
                    it.copy(
                        sourceName = name.substringBeforeLast("."),
                        filePath = outFile.absolutePath,
                        durationMs = durMs,
                        waveform = wave,
                        trimStartPct = 0f,
                        trimEndPct = 1f,
                        muted = false
                    )
                }
                showToast("Loaded into ${targetRole.label}")
            } else {
                showToast("No audio track found in selected file")
            }
        }
    }

    fun toggleAudioMixPlayback() {
        if (_audioState.value.isPlayingMix) {
            stopAudioMixPlayback()
        } else {
            audioPlayJob?.cancel()
            _audioState.update { it.copy(isPlayingMix = true) }
            audioPlayJob = viewModelScope.launch {
                val mixed = AudioDspEngine.mixThreeTracks(_audioState.value.tracks)
                if (mixed.isNotEmpty()) {
                    AudioDspEngine.playPcm(mixed)
                } else {
                    showToast("Unmute at least one audio track to play")
                }
                _audioState.update { it.copy(isPlayingMix = false) }
            }
        }
    }

    fun stopAudioMixPlayback() {
        audioPlayJob?.cancel()
        _audioState.update { it.copy(isPlayingMix = false) }
    }

    fun startVoiceoverRecording() {
        if (_audioState.value.isRecordingMic) return
        stopAudioMixPlayback()
        recordingFlag.set(true)
        _audioState.update { it.copy(isRecordingMic = true, recordingSeconds = 0) }
        micRecordJob?.cancel()
        micRecordJob = viewModelScope.launch {
            val dir = File(appContext.cacheDir, "audio_tracks").apply { mkdirs() }
            val outFile = File(dir, "voiceover_${System.currentTimeMillis()}.wav")
            val nr = _audioState.value.noiseReductionEnabled
            val (pcm, durMs) = AudioDspEngine.recordVoiceoverToFile(
                outputFile = outFile,
                noiseReduction = nr,
                shouldContinue = { recordingFlag.get() },
                onLevel = { rms, sec ->
                    _audioState.update { s ->
                        s.copy(
                            recordingSeconds = sec,
                            liveMicLevels = (s.liveMicLevels.drop(1) + rms)
                        )
                    }
                }
            )
            val wave = AudioDspEngine.computeWaveformBars(pcm)
            updateAudioTrack(AudioTrackRole.VOICEOVER) {
                it.copy(
                    sourceName = if (nr) "My Voice (Clean)" else "My Voice",
                    filePath = outFile.absolutePath,
                    durationMs = durMs,
                    waveform = wave,
                    muted = false
                )
            }
            _audioState.update { it.copy(isRecordingMic = false) }
            showToast("Voice saved (${String.format("%.1fs", durMs / 1000f)})")
        }
    }

    fun stopVoiceoverRecording() {
        recordingFlag.set(false)
    }

    fun toggleNoiseReduction() {
        _audioState.update { it.copy(noiseReductionEnabled = !it.noiseReductionEnabled) }
    }

    fun playSfx(kind: SfxKind) {
        viewModelScope.launch {
            _audioState.update { it.copy(activeSfxId = kind.id) }
            val pcm = withContext(Dispatchers.Default) { AudioDspEngine.synthesizeSfxPcm(kind) }
            AudioDspEngine.playPcm(pcm)
            _audioState.update { if (it.activeSfxId == kind.id) it.copy(activeSfxId = null) else it }
        }
    }

    fun downloadSfxWav(kind: SfxKind) {
        viewModelScope.launch {
            val pcm = withContext(Dispatchers.Default) { AudioDspEngine.synthesizeSfxPcm(kind) }
            val wav = withContext(Dispatchers.Default) { AudioDspEngine.encodePcmToWav(pcm) }
            val res = MediaExportStore.saveAudioBytes(
                appContext,
                MediaExportStore.makeFileName("SFX_${kind.id}", "wav"),
                "audio/wav",
                wav
            )
            handleExportFeedback("${kind.label} SFX WAV", res)
        }
    }

    fun toggleCopyrightCheck(id: String) {
        _audioState.update { s ->
            s.copy(copyrightChecklist = s.copyrightChecklist.map {
                if (it.id == id) it.copy(checked = !it.checked) else it
            })
        }
    }

    fun setAudioClipSecondsCheck(sec: Int) {
        _audioState.update { it.copy(audioClipSecondsCheck = sec.coerceIn(1, 180)) }
    }

    fun setAudioVoicePreset(role: AudioTrackRole, preset: VoiceChangerPreset) {
        updateAudioTrack(role) {
            it.copy(
                voicePreset = preset,
                pitchSemitones = preset.pitchSemi,
                tempoSpeed = preset.speed
            )
        }
    }

    fun setAudioEqPreset(role: AudioTrackRole, preset: AudioEqPreset) {
        updateAudioTrack(role) { it.copy(eqPreset = preset) }
    }

    fun setAudioExportFormat(format: AudioExportFormat) {
        _audioState.update { it.copy(exportFormat = format) }
        scheduleAutoSave(WorkspaceType.AUDIO)
    }

    fun splitAudioTrack(role: AudioTrackRole, splitPct: Float = 0.5f) {
        val track = _audioState.value.tracks.firstOrNull { it.role == role } ?: return
        val path = track.filePath ?: run {
            showToast("Load audio into ${role.label} first")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val pcm = AudioDspEngine.readWavPcm(File(path)) ?: ShortArray(0)
            if (pcm.size < 2000) {
                withContext(Dispatchers.Main) { showToast("Track is too short to split") }
                return@launch
            }
            val (partA, _) = AudioDspEngine.splitPcm(pcm, splitPct)
            val dir = File(appContext.cacheDir, "audio_tracks").apply { mkdirs() }
            val splitFile = File(dir, "split_${role.name.lowercase()}_${System.currentTimeMillis()}.wav")
            AudioDspEngine.writePcmWavFile(splitFile, partA)
            val wave = AudioDspEngine.computeWaveformBars(partA)
            val durMs = (partA.size * 1000L / AudioDspEngine.SAMPLE_RATE).coerceAtLeast(250L)
            updateAudioTrack(role) {
                it.copy(
                    sourceName = "${it.sourceName} (Cut)",
                    filePath = splitFile.absolutePath,
                    durationMs = durMs,
                    waveform = wave,
                    trimStartPct = 0f,
                    trimEndPct = 1f
                )
            }
            withContext(Dispatchers.Main) { showToast("Split ${role.label} at ${(splitPct * 100).toInt()}%") }
        }
    }

    fun mergeAudioTracks(roleA: AudioTrackRole, roleB: AudioTrackRole) {
        val trackA = _audioState.value.tracks.firstOrNull { it.role == roleA }
        val trackB = _audioState.value.tracks.firstOrNull { it.role == roleB }
        if (trackA?.filePath == null || trackB?.filePath == null) {
            showToast("Both ${roleA.label} and ${roleB.label} need audio to join")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val pcmA = AudioDspEngine.readWavPcm(File(trackA.filePath)) ?: ShortArray(0)
            val pcmB = AudioDspEngine.readWavPcm(File(trackB.filePath)) ?: ShortArray(0)
            if (pcmA.isEmpty() || pcmB.isEmpty()) {
                withContext(Dispatchers.Main) { showToast("Could not read track audio to join") }
                return@launch
            }
            val merged = AudioDspEngine.mergePcmTracks(pcmA, pcmB)
            val dir = File(appContext.cacheDir, "audio_tracks").apply { mkdirs() }
            val mergedFile = File(dir, "merged_${System.currentTimeMillis()}.wav")
            AudioDspEngine.writePcmWavFile(mergedFile, merged)
            val wave = AudioDspEngine.computeWaveformBars(merged)
            val durMs = (merged.size * 1000L / AudioDspEngine.SAMPLE_RATE).coerceAtLeast(300L)
            updateAudioTrack(roleA) {
                it.copy(
                    sourceName = "${trackA.sourceName} + ${trackB.sourceName}".take(34),
                    filePath = mergedFile.absolutePath,
                    durationMs = durMs,
                    waveform = wave,
                    trimStartPct = 0f,
                    trimEndPct = 1f
                )
            }
            withContext(Dispatchers.Main) { showToast("Joined ${roleA.label} + ${roleB.label}") }
        }
    }

    fun exportAudioMixWav() {
        viewModelScope.launch {
            _exportProgress.value = 0.45f
            val mixed = AudioDspEngine.mixThreeTracks(_audioState.value.tracks)
            if (mixed.isEmpty()) {
                _exportProgress.value = null
                showToast("No active audio tracks to export")
                return@launch
            }
            val requestedFmt = _audioState.value.exportFormat
            val (bytes, actualFmt) = withContext(Dispatchers.Default) {
                AudioDspEngine.encodePcmForFormat(mixed, requestedFmt)
            }
            val res = MediaExportStore.saveAudioBytes(
                appContext,
                MediaExportStore.makeFileName("MasterMix", actualFmt.ext),
                actualFmt.mime,
                bytes
            )
            withContext(Dispatchers.IO) { AppSecurityFirewall.cleanupTempExportCache(appContext) }
            _exportProgress.value = null
            handleExportFeedback("Audio Mix (${actualFmt.label})", res)
        }
    }

    // =========================================================================
    // WORKSPACE 4: QUICK UTILITIES & AUTO-SAVE PROJECT VAULT
    // =========================================================================

    fun addBatchImages(uris: List<Uri>) {
        val validUris = uris.filter { AppSecurityFirewall.validateMediaUri(appContext, it, "image/") }
        if (validUris.isEmpty()) {
            showToast("Selected file is not a supported photo")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val entries = validUris.map { uri ->
                val name = AppSecurityFirewall.sanitizeText(
                    queryFileName(uri) ?: "Image_${UUID.randomUUID().toString().take(5)}",
                    maxLength = 44
                )
                val bytes = queryFileSize(uri)
                BatchImageEntry(
                    id = "img_${UUID.randomUUID().toString().take(8)}",
                    name = name,
                    uriString = uri.toString(),
                    originalBytes = bytes
                )
            }
            _utilitiesState.update { it.copy(batchImages = it.batchImages + entries) }
            showToast("Added ${entries.size} photo(s)")
        }
    }

    fun clearBatchImages() {
        _utilitiesState.update { it.copy(batchImages = emptyList()) }
    }

    fun setBatchOutputFormat(format: ImageConvertFormat) {
        _utilitiesState.update { it.copy(outputFormat = format) }
    }

    fun setBatchQuality(quality: Int) {
        _utilitiesState.update { it.copy(quality = quality.coerceIn(20, 100)) }
    }

    fun runBatchImageCompression() {
        val current = _utilitiesState.value
        if (current.batchImages.isEmpty()) {
            showToast("Select one or more photos first")
            return
        }
        viewModelScope.launch {
            _utilitiesState.update { it.copy(isCompressing = true) }
            _exportProgress.value = 0.05f
            val updated = mutableListOf<BatchImageEntry>()
            var okCount = 0
            current.batchImages.forEachIndexed { idx, entry ->
                val res = ImageProcessingEngine.compressAndConvertImage(
                    context = appContext,
                    uriString = entry.uriString,
                    targetFormat = current.outputFormat,
                    quality = current.quality
                )
                if (res.success) {
                    okCount++
                    updated.add(entry.copy(compressedBytes = res.byteCount, exportedPath = res.displayPath))
                } else {
                    updated.add(entry)
                }
                _exportProgress.value = (idx + 1).toFloat() / current.batchImages.size
            }
            withContext(Dispatchers.IO) { AppSecurityFirewall.cleanupTempExportCache(appContext) }
            _exportProgress.value = null
            _utilitiesState.update {
                it.copy(
                    batchImages = updated,
                    isCompressing = false,
                    lastUtilityStatus = "Converted $okCount/${updated.size} images to ${current.outputFormat.label}"
                )
            }
            showToast("Saved $okCount ${current.outputFormat.label} photo(s) to Gallery")
        }
    }

    fun setGifSourceVideo(uri: Uri) {
        if (!AppSecurityFirewall.validateMediaUri(appContext, uri, "video/")) {
            showToast("Selected file is not a supported video")
            return
        }
        val name = AppSecurityFirewall.sanitizeText(queryFileName(uri) ?: "Imported Video", maxLength = 40)
        _utilitiesState.update { it.copy(gifSourceUri = uri.toString(), gifSourceName = name) }
        showToast("Selected video: $name")
    }

    fun setGifParams(fps: Int? = null, frames: Int? = null) {
        _utilitiesState.update {
            it.copy(
                gifFps = (fps ?: it.gifFps).coerceIn(4, 15),
                gifFrameCount = (frames ?: it.gifFrameCount).coerceIn(4, 24)
            )
        }
    }

    fun convertVideoToGif() {
        val u = _utilitiesState.value
        viewModelScope.launch {
            _utilitiesState.update { it.copy(isProcessingGif = true) }
            _exportProgress.value = 0.4f
            val res = VideoMediaEngine.exportVideoToGif(
                context = appContext,
                videoUri = u.gifSourceUri,
                fallbackState = _videoState.value,
                fps = u.gifFps,
                frameCount = u.gifFrameCount
            )
            withContext(Dispatchers.IO) { AppSecurityFirewall.cleanupTempExportCache(appContext) }
            _exportProgress.value = null
            _utilitiesState.update {
                it.copy(
                    isProcessingGif = false,
                    lastUtilityStatus = if (res.success) "GIF saved: ${MediaExportStore.formatSize(res.byteCount)}" else res.error
                )
            }
            handleExportFeedback("Animated GIF", res)
        }
    }

    fun extractVideoFramesBatch() {
        val u = _utilitiesState.value
        viewModelScope.launch {
            _utilitiesState.update { it.copy(isProcessingGif = true) }
            _exportProgress.value = 0.4f
            val (count, res) = VideoMediaEngine.extractFramesToGallery(
                context = appContext,
                videoUri = u.gifSourceUri,
                fallbackState = _videoState.value,
                frameCount = minOf(u.gifFrameCount, 10)
            )
            withContext(Dispatchers.IO) { AppSecurityFirewall.cleanupTempExportCache(appContext) }
            _exportProgress.value = null
            _utilitiesState.update {
                it.copy(
                    isProcessingGif = false,
                    extractedFrameCount = count,
                    lastUtilityStatus = "Saved $count HD PNG frames to Gallery"
                )
            }
            if (count > 0) showToast("Saved $count HD PNG frames to Gallery")
            else showToast(res.error ?: "Frame extraction failed")
        }
    }

    // =========================================================================
    // AUTO-SAVE PROJECT VAULT (ROOM) & MASTER EXPORT ROUTER
    // =========================================================================

    fun triggerWorkspaceExport() {
        when (_activeWorkspace.value) {
            StudioWorkspace.HOME, StudioWorkspace.VIDEO -> exportVideo()
            StudioWorkspace.PHOTO -> exportPhoto()
            StudioWorkspace.AUDIO -> exportAudioMixWav()
            StudioWorkspace.VAULT -> {
                if (_utilitiesState.value.batchImages.isNotEmpty()) {
                    runBatchImageCompression()
                } else {
                    convertVideoToGif()
                }
            }
        }
    }

    fun saveCurrentWorkspaceSnapshot(customName: String? = null) {
        val cleanName = customName?.let { AppSecurityFirewall.sanitizeText(it, maxLength = 50) }?.ifBlank { null }
        val ws = when (_activeWorkspace.value) {
            StudioWorkspace.HOME, StudioWorkspace.VIDEO -> WorkspaceType.VIDEO
            StudioWorkspace.PHOTO -> WorkspaceType.PHOTO
            StudioWorkspace.AUDIO -> WorkspaceType.AUDIO
            StudioWorkspace.VAULT -> WorkspaceType.VIDEO
        }
        viewModelScope.launch {
            persistWorkspaceToVault(ws, cleanName, createNewSnapshot = cleanName != null)
            showToast("Project saved")
        }
    }

    private fun scheduleAutoSave(type: WorkspaceType) {
        autoSaveJob?.cancel()
        _autoSaveLabel.value = "Saving..."
        autoSaveJob = viewModelScope.launch {
            delay(900L)
            persistWorkspaceToVault(type, customName = null, createNewSnapshot = false)
            _autoSaveLabel.value = "Saved"
        }
    }

    private suspend fun persistWorkspaceToVault(
        type: WorkspaceType,
        customName: String?,
        createNewSnapshot: Boolean
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entity = when (type) {
            WorkspaceType.VIDEO -> {
                val s = _videoState.value
                val id = if (createNewSnapshot) "vid_${UUID.randomUUID().toString().take(8)}" else s.projectId
                ProjectEntity(
                    id = id,
                    name = customName ?: s.projectName,
                    workspaceType = WorkspaceType.VIDEO.name,
                    resolutionLabel = "${s.aspectPreset.label} • ${s.exportResolution.label}",
                    summary = "${s.clips.size} clips • ${s.textLayers.size} text • ${s.colorGrade.lut.label}",
                    updatedAt = now,
                    stateJson = videoAdapter.toJson(s.copy(isPlaying = false, isListeningCaptions = false))
                )
            }
            WorkspaceType.PHOTO -> {
                val s = _photoState.value
                val id = if (createNewSnapshot) "pho_${UUID.randomUUID().toString().take(8)}" else s.projectId
                ProjectEntity(
                    id = id,
                    name = customName ?: s.projectName,
                    workspaceType = WorkspaceType.PHOTO.name,
                    resolutionLabel = s.preset.label,
                    summary = "${s.layers.size} layers • Blur ${(s.dslrBackgroundBlur * 100).toInt()}%",
                    updatedAt = now,
                    stateJson = photoAdapter.toJson(s.copy(eraserModeActive = false))
                )
            }
            WorkspaceType.AUDIO -> {
                val s = _audioState.value
                val id = if (createNewSnapshot) "aud_${UUID.randomUUID().toString().take(8)}" else s.projectId
                ProjectEntity(
                    id = id,
                    name = customName ?: s.projectName,
                    workspaceType = WorkspaceType.AUDIO.name,
                    resolutionLabel = "HD Audio",
                    summary = "3-Track Mix • Clean Voice ${if (s.noiseReductionEnabled) "ON" else "OFF"}",
                    updatedAt = now,
                    stateJson = audioAdapter.toJson(s.copy(isPlayingMix = false, isRecordingMic = false))
                )
            }
        }
        repository.saveProject(entity)
    }

    fun resumeVaultProject(project: ProjectEntity) {
        viewModelScope.launch {
            runCatching {
                when (project.workspaceType) {
                    WorkspaceType.VIDEO.name -> {
                        val restored = videoAdapter.fromJson(project.stateJson) ?: return@runCatching
                        val sanitizedPip = if (restored.pip.uriString.isNullOrBlank()) {
                            PipOverlayState(enabled = false, uriString = null, fallbackDrawable = null)
                        } else {
                            restored.pip.copy(fallbackDrawable = null)
                        }
                        _videoState.value = restored.copy(
                            isPlaying = false,
                            isListeningCaptions = false,
                            isRecordingVideoVoiceover = false,
                            pip = sanitizedPip
                        )
                        _activeWorkspace.value = StudioWorkspace.VIDEO
                        refreshFilmstripThumbnails(_videoState.value.clips)
                        scheduleVideoPreviewRender(true)
                    }
                    WorkspaceType.PHOTO.name -> {
                        val restored = photoAdapter.fromJson(project.stateJson) ?: return@runCatching
                        _photoState.value = restored.copy(eraserModeActive = false, restoreBrushActive = false, spotHealModeActive = false)
                        _activeWorkspace.value = StudioWorkspace.PHOTO
                        schedulePhotoPreviewRender(true)
                    }
                    WorkspaceType.AUDIO.name -> {
                        val restored = audioAdapter.fromJson(project.stateJson) ?: return@runCatching
                        _audioState.value = restored.copy(isPlayingMix = false, isRecordingMic = false)
                        _activeWorkspace.value = StudioWorkspace.AUDIO
                    }
                }
                showToast("Resumed \"${project.name}\"")
            }.onFailure {
                showToast("Unable to restore snapshot")
            }
        }
    }

    fun deleteVaultProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            showToast("Project removed from Vault")
        }
    }

    private fun handleExportFeedback(label: String, result: ExportResult) {
        if (result.success) {
            showToast("$label saved (${MediaExportStore.formatSize(result.byteCount)})")
        } else {
            showToast(result.error ?: "Could not save $label")
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun queryFileSize(uri: Uri): Long {
        return runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    override fun onCleared() {
        super.onCleared()
        stopVideoPlayback()
        stopAudioMixPlayback()
        stopVoiceoverRecording()
        stopVideoVoiceoverRecording()
        runCatching { _videoPreviewBitmap.value?.recycle() }
        runCatching { _photoPreviewBitmap.value?.recycle() }
        _clipThumbnails.value.values.flatten().forEach { bmp -> runCatching { bmp.recycle() } }
    }
}
