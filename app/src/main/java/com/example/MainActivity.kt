package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.StudioWorkspace
import com.example.ui.auth.WelcomeAuthScreen
import com.example.ui.components.AdminDashboardScreen
import com.example.ui.components.AdminPinUnlockDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.ProfileBottomSheet
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.OledBlack
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XonixStudioProTheme
import com.example.ui.workspaces.AudioStudioWorkspace
import com.example.ui.workspaces.HomeHubScreen
import com.example.ui.workspaces.PhotoStudioWorkspace
import com.example.ui.workspaces.UtilitiesVaultWorkspace
import com.example.ui.workspaces.VideoEditorWorkspace
import com.example.viewmodel.XonixStudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            XonixStudioProTheme {
                XonixStudioApp()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val aggressive = level >= TRIM_MEMORY_BACKGROUND || level == TRIM_MEMORY_RUNNING_CRITICAL
        if (aggressive || level >= TRIM_MEMORY_RUNNING_LOW) {
            com.example.engine.ImageProcessingEngine.trimMemory(aggressive = aggressive)
            com.example.engine.TimelineThumbnailCache.trimMemory(aggressive = aggressive)
            if (aggressive) {
                com.example.engine.VideoMediaEngine.trimMemory()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        com.example.engine.ImageProcessingEngine.trimMemory(aggressive = true)
        com.example.engine.TimelineThumbnailCache.trimMemory(aggressive = true)
        com.example.engine.VideoMediaEngine.trimMemory()
    }
}

@Composable
private fun XonixStudioApp(
    viewModel: XonixStudioViewModel = viewModel()
) {
    val authReady by viewModel.authReady.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()

    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val autoSaveLabel by viewModel.autoSaveLabel.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val canUndoVideo by viewModel.canUndoVideo.collectAsStateWithLifecycle()
    val canRedoVideo by viewModel.canRedoVideo.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = msg,
            duration = SnackbarDuration.Short
        )
        viewModel.clearToast()
    }

    if (!authReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ElectricCyan)
        }
        return
    }

    val activeUser = currentUser
    if (activeUser == null) {
        WelcomeAuthScreen(
            errorMessage = authError,
            onClearError = { viewModel.clearAuthError() },
            onContinueAsGuest = { viewModel.continueAsGuest() },
            onSignIn = { email, password -> viewModel.signIn(email, password) },
            onCreateAccount = { name, email, password -> viewModel.createAccount(name, email, password) }
        )
        availableUpdate?.let { updateInfo ->
            AppUpdateDialog(
                updateInfo = updateInfo,
                onDismiss = { viewModel.dismissUpdateDialog() },
                onUpdateNow = { viewModel.openAvailableUpdateInChrome() }
            )
        }
        return
    }

    val isOwner = viewModel.isCurrentUserOwner()
    val userInitial = activeUser.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"

    // Return to Creator Home Hub when pressing Back from any sub-workspace
    BackHandler(enabled = activeWorkspace != StudioWorkspace.HOME && !isAdminUnlocked) {
        viewModel.selectWorkspace(StudioWorkspace.HOME)
    }

    val navItems = remember {
        listOf(
            Triple(StudioWorkspace.HOME, Icons.Filled.Home, Icons.Outlined.Home),
            Triple(StudioWorkspace.VIDEO, Icons.Filled.Movie, Icons.Outlined.Movie),
            Triple(StudioWorkspace.PHOTO, Icons.Filled.Photo, Icons.Outlined.Photo),
            Triple(StudioWorkspace.AUDIO, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
            Triple(StudioWorkspace.VAULT, Icons.Filled.FolderSpecial, Icons.Outlined.FolderSpecial)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.isCtrlPressed && event.key == Key.S -> {
                            viewModel.saveCurrentWorkspaceSnapshot()
                            true
                        }
                        event.isCtrlPressed && event.key == Key.E -> {
                            if (activeWorkspace == StudioWorkspace.VIDEO) {
                                viewModel.requestOpenVideoExport()
                            } else {
                                viewModel.triggerWorkspaceExport()
                            }
                            true
                        }
                        event.key == Key.Spacebar -> {
                            when (activeWorkspace) {
                                StudioWorkspace.VIDEO -> viewModel.toggleVideoPlayback()
                                StudioWorkspace.AUDIO -> viewModel.toggleAudioMixPlayback()
                                else -> Unit
                            }
                            true
                        }
                        event.key == Key.S && activeWorkspace == StudioWorkspace.VIDEO -> {
                            viewModel.splitSelectedClipAtPlayhead()
                            true
                        }
                        event.key == Key.One -> { viewModel.selectWorkspace(StudioWorkspace.HOME); true }
                        event.key == Key.Two -> { viewModel.selectWorkspace(StudioWorkspace.VIDEO); true }
                        event.key == Key.Three -> { viewModel.selectWorkspace(StudioWorkspace.PHOTO); true }
                        event.key == Key.Four -> { viewModel.selectWorkspace(StudioWorkspace.AUDIO); true }
                        event.key == Key.Five -> { viewModel.selectWorkspace(StudioWorkspace.VAULT); true }
                        event.key == Key.Delete -> {
                            when (activeWorkspace) {
                                StudioWorkspace.VIDEO -> viewModel.deleteSelectedClip()
                                StudioWorkspace.PHOTO -> viewModel.photoState.value.selectedLayerId?.let { viewModel.deletePhotoLayer(it) }
                                else -> Unit
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            val isWideScreen = maxWidth >= 700.dp

            Scaffold(
                containerColor = OledBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    StudioTopBar(
                        workspace = activeWorkspace,
                        autoSaveLabel = autoSaveLabel,
                        exportProgress = exportProgress,
                        userInitial = userInitial,
                        canUndo = canUndoVideo,
                        canRedo = canRedoVideo,
                        onNavigateHome = { viewModel.selectWorkspace(StudioWorkspace.HOME) },
                        onUndo = { viewModel.undoVideoEdit() },
                        onRedo = { viewModel.redoVideoEdit() },
                        onQuickSave = { viewModel.saveCurrentWorkspaceSnapshot() },
                        onShareApp = { viewModel.shareAppWithFriends() },
                        onOpenProfile = { showProfileSheet = true },
                        onExport = {
                            if (activeWorkspace == StudioWorkspace.VIDEO) {
                                viewModel.requestOpenVideoExport()
                            } else {
                                viewModel.triggerWorkspaceExport()
                            }
                        }
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = StudioSurfaceElevated,
                            contentColor = TextPrimary,
                            actionColor = ElectricCyan
                        )
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = DeepObsidian,
                            tonalElevation = 0.dp
                        ) {
                            navItems.forEach { (ws, filled, outlined) ->
                                val selected = activeWorkspace == ws
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectWorkspace(ws) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) filled else outlined,
                                            contentDescription = ws.label
                                        )
                                    },
                                    label = { Text(ws.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = ElectricCyanContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${ws.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = DeepObsidian,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { (ws, filled, outlined) ->
                                val selected = activeWorkspace == ws
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectWorkspace(ws) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) filled else outlined,
                                            contentDescription = ws.label
                                        )
                                    },
                                    label = { Text(ws.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = ElectricCyanContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("rail_tab_${ws.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (activeWorkspace) {
                            StudioWorkspace.HOME -> HomeWorkspaceHost(
                                activeUser = activeUser,
                                viewModel = viewModel
                            )
                            StudioWorkspace.VIDEO -> VideoWorkspaceHost(
                                viewModel = viewModel
                            )
                            StudioWorkspace.PHOTO -> PhotoWorkspaceHost(
                                viewModel = viewModel
                            )
                            StudioWorkspace.AUDIO -> AudioWorkspaceHost(
                                viewModel = viewModel
                            )
                            StudioWorkspace.VAULT -> VaultWorkspaceHost(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        // Profile Bottom Sheet (includes Owner-Only Admin Dashboard button strictly when isOwner == true)
        if (showProfileSheet) {
            val userLoginHistory by viewModel.currentUserLoginHistory.collectAsStateWithLifecycle()
            val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
            ProfileBottomSheet(
                user = activeUser,
                isOwner = isOwner,
                loginHistory = userLoginHistory,
                isCheckingUpdate = isCheckingUpdate,
                onCheckForUpdates = { viewModel.checkForAppUpdate(manual = true) },
                onDismiss = { showProfileSheet = false },
                onUpdateName = { newName -> viewModel.updateProfileName(newName) },
                onShareApp = {
                    showProfileSheet = false
                    viewModel.shareAppWithFriends()
                },
                onOpenAdminPin = {
                    showProfileSheet = false
                    if (isOwner) {
                        viewModel.refreshAdminLockoutStatus()
                        showAdminPinDialog = true
                    }
                },
                onLogOut = {
                    showProfileSheet = false
                    showAdminPinDialog = false
                    viewModel.logOut()
                }
            )
        }

        // Owner-Only 6-Digit Master PIN Unlock Dialog (with 24-hour lockout after 3 failed attempts)
        if (showAdminPinDialog && isOwner) {
            val adminPinMessage by viewModel.adminPinMessage.collectAsStateWithLifecycle()
            val adminLockoutMs by viewModel.adminLockoutMs.collectAsStateWithLifecycle()
            AdminPinUnlockDialog(
                lockoutRemainingMs = adminLockoutMs,
                statusMessage = adminPinMessage,
                onDismiss = { showAdminPinDialog = false },
                onSubmitPin = { pin ->
                    if (viewModel.verifyOwnerAdminPin(pin)) {
                        showAdminPinDialog = false
                    }
                }
            )
        }

        // Owner-Only Admin Dashboard Full-Screen Overlay
        if (isAdminUnlocked && isOwner) {
            val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
            val allLoginHistory by viewModel.allLoginHistory.collectAsStateWithLifecycle()
            AdminDashboardScreen(
                currentOwnerId = activeUser.id,
                users = allUsers,
                loginHistory = allLoginHistory,
                onRemoveUser = { userId -> viewModel.removeAccountAsOwner(userId) },
                onClose = { viewModel.lockAdminDashboard() }
            )
        }

        // GitHub Chrome OTA In-App Update Popup Dialog
        availableUpdate?.let { updateInfo ->
            AppUpdateDialog(
                updateInfo = updateInfo,
                onDismiss = { viewModel.dismissUpdateDialog() },
                onUpdateNow = { viewModel.openAvailableUpdateInChrome() }
            )
        }
    }
}

@Composable
private fun HomeWorkspaceHost(
    activeUser: com.example.data.UserAccountEntity,
    viewModel: XonixStudioViewModel
) {
    val vaultProjects by viewModel.vaultProjects.collectAsStateWithLifecycle()
    HomeHubScreen(
        currentUser = activeUser,
        projects = vaultProjects,
        onStartCreating = { viewModel.selectWorkspace(StudioWorkspace.VIDEO) },
        onPickMediaToStart = { uris -> viewModel.importVideoClipsAndOpenEditor(uris) },
        onOpenWorkspace = { viewModel.selectWorkspace(it) },
        onOpen4KEnhancer = { viewModel.openVideoEnhancerShortcut() },
        onResumeProject = { viewModel.resumeVaultProject(it) },
        onDeleteProject = { viewModel.deleteVaultProject(it) },
        onShareApp = { viewModel.shareAppWithFriends() }
    )
}

@Composable
private fun VideoWorkspaceHost(
    viewModel: XonixStudioViewModel
) {
    val videoState by viewModel.videoState.collectAsStateWithLifecycle()
    val videoPreview by viewModel.videoPreviewBitmap.collectAsStateWithLifecycle()
    val clipThumbnails by viewModel.clipThumbnails.collectAsStateWithLifecycle()
    val encoderCapabilities by viewModel.encoderCapabilities.collectAsStateWithLifecycle()
    val openEnhanceRequested by viewModel.openVideoEnhanceRequested.collectAsStateWithLifecycle()
    val openExportRequested by viewModel.openVideoExportRequested.collectAsStateWithLifecycle()
    VideoEditorWorkspace(
        state = videoState,
        previewBitmap = videoPreview,
        clipThumbnails = clipThumbnails,
        encoderCapabilities = encoderCapabilities,
        viewModel = viewModel,
        openEnhanceRequested = openEnhanceRequested,
        onConsumeEnhanceRequest = { viewModel.consumeVideoEnhanceRequest() },
        openExportRequested = openExportRequested,
        onConsumeExportRequest = { viewModel.consumeVideoExportRequest() }
    )
}

@Composable
private fun PhotoWorkspaceHost(
    viewModel: XonixStudioViewModel
) {
    val photoState by viewModel.photoState.collectAsStateWithLifecycle()
    val photoPreview by viewModel.photoPreviewBitmap.collectAsStateWithLifecycle()
    PhotoStudioWorkspace(
        state = photoState,
        previewBitmap = photoPreview,
        viewModel = viewModel
    )
}

@Composable
private fun AudioWorkspaceHost(
    viewModel: XonixStudioViewModel
) {
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    AudioStudioWorkspace(
        state = audioState,
        viewModel = viewModel
    )
}

@Composable
private fun VaultWorkspaceHost(
    viewModel: XonixStudioViewModel
) {
    val utilitiesState by viewModel.utilitiesState.collectAsStateWithLifecycle()
    val vaultProjects by viewModel.vaultProjects.collectAsStateWithLifecycle()
    UtilitiesVaultWorkspace(
        state = utilitiesState,
        projects = vaultProjects,
        viewModel = viewModel
    )
}
