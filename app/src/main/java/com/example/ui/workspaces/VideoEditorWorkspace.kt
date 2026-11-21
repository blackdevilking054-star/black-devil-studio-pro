package com.example.ui.workspaces

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.ImageProcessingEngine
import com.example.engine.VideoMediaEngine
import com.example.engine.VideoOverlayRenderer
import com.example.model.BlendModeKind
import com.example.model.FilterCategory
import com.example.model.HardwareEncoderCapabilities
import com.example.model.LutPreset
import com.example.model.MaskShapeKind
import com.example.model.SfxKind
import com.example.model.SpeedCurvePreset
import com.example.model.TextAnimationStyle
import com.example.model.VideoAspectPreset
import com.example.model.VideoBgFillMode
import com.example.model.VideoEditorState
import com.example.model.VideoExportFps
import com.example.model.VideoExportPreset
import com.example.model.VideoExportResolution
import com.example.model.VideoTransition
import com.example.ui.components.StudioActionPill
import com.example.ui.components.StudioChipButton
import com.example.ui.components.StudioColorSwatchRow
import com.example.ui.components.StudioSliderRow
import com.example.ui.components.StudioToolTabStrip
import com.example.ui.components.ToolTabItem
import com.example.ui.components.TransitionPickerPanel
import com.example.ui.components.formatSimpleTime
import com.example.ui.components.formatTimecode
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProAmberContainer
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioFonts
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.XonixStudioViewModel
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class VideoToolTab {
    NONE, TRIM, SPLIT, AUDIO, TEXT, STICKER, FILTER, TRANSITIONS, SPEED, KEYFRAME, DELETE, ADJUST, ENHANCE, FRAME, OVERLAY, EXPORT
}

private enum class VideoTrimControl(val label: String) {
    START("Trim Start"),
    END("Trim End")
}

private enum class VideoAudioSlider(val label: String) {
    VOLUME("Volume"),
    FADE_IN("Fade In"),
    FADE_OUT("Fade Out"),
    TRIM_START("Start"),
    TRIM_END("End")
}

private enum class VideoAdjustControl(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Color"),
    EXPOSURE("Exposure"),
    WARMTH("Warmth"),
    SHARPEN("Sharp"),
    BLUR("Blur"),
    VIGNETTE("Vignette"),
    HSL_HUE("HSL Hue"),
    HSL_SAT("HSL Sat"),
    HSL_LIGHT("HSL Light")
}

private enum class VideoFrameControl(val label: String) {
    ZOOM("Zoom"),
    PAN_X("Left / Right"),
    PAN_Y("Up / Down"),
    ROTATE("Free Rotate")
}

private enum class VideoOverlayControl(val label: String) {
    SIZE("Size"),
    ROTATE("Rotate"),
    OPACITY("Opacity"),
    CLEAN("Green Edge"),
    SPILL("Anti-Spill")
}

private enum class VideoKeyframeControl(val label: String) {
    SCALE("Scale"),
    POS_X("Move X"),
    POS_Y("Move Y"),
    ROTATE("Rotate"),
    OPACITY("Opacity")
}

private enum class VideoTextControl(val label: String) {
    FONT("Font"),
    STYLE("Motion"),
    COLOR("Color"),
    BG_STYLE("Box / Shadow"),
    SIZE("Size"),
    POSITION("Position")
}

private enum class CanvasSelectionTarget {
    NONE, VIDEO, PIP, TEXT, STICKER
}

@Composable
fun VideoEditorWorkspace(
    state: VideoEditorState,
    previewBitmap: Bitmap?,
    clipThumbnails: Map<String, List<Bitmap>> = emptyMap(),
    encoderCapabilities: HardwareEncoderCapabilities,
    viewModel: XonixStudioViewModel,
    openEnhanceRequested: Boolean = false,
    onConsumeEnhanceRequest: () -> Unit = {},
    openExportRequested: Boolean = false,
    onConsumeExportRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val canUndoVideo by viewModel.canUndoVideo.collectAsStateWithLifecycle()
    val canRedoVideo by viewModel.canRedoVideo.collectAsStateWithLifecycle()
    val liveSourceAspect by viewModel.videoSourceAspect.collectAsStateWithLifecycle()
    var activeTab by rememberSaveable { mutableStateOf(VideoToolTab.TRIM) }
    var isToolDrawerExpanded by rememberSaveable { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var timelineZoom by rememberSaveable { mutableFloatStateOf(1.0f) }
    var canvasSelection by rememberSaveable { mutableStateOf(CanvasSelectionTarget.NONE) }
    var isClipQuickBarVisible by rememberSaveable { mutableStateOf(false) }
    var activeFilterCategory by rememberSaveable { mutableStateOf(state.colorGrade.lut.category) }
    var trimControl by rememberSaveable { mutableStateOf(VideoTrimControl.START) }
    var audioSlider by rememberSaveable { mutableStateOf(VideoAudioSlider.VOLUME) }
    var adjustControl by rememberSaveable { mutableStateOf(VideoAdjustControl.BRIGHTNESS) }
    var frameControl by rememberSaveable { mutableStateOf(VideoFrameControl.ZOOM) }
    var overlayControl by rememberSaveable { mutableStateOf(VideoOverlayControl.SIZE) }
    var keyframeControl by rememberSaveable { mutableStateOf(VideoKeyframeControl.SCALE) }
    var textControl by rememberSaveable { mutableStateOf(VideoTextControl.FONT) }
    var showStickerRow by rememberSaveable { mutableStateOf(false) }
    var isStickerCropping by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openEnhanceRequested) {
        if (openEnhanceRequested) {
            activeTab = VideoToolTab.ENHANCE
            isToolDrawerExpanded = true
            onConsumeEnhanceRequest()
        }
    }

    LaunchedEffect(openExportRequested) {
        if (openExportRequested) {
            activeTab = VideoToolTab.EXPORT
            isToolDrawerExpanded = true
            onConsumeExportRequest()
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importVideoClips(uris)
    }

    val replaceClipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.replaceSelectedClipMedia(uri)
    }

    val pipImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.importPipImage(uri)
    }

    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.setVideoBgImage(uri)
    }

    val stickerImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomImageSticker(uri)
            canvasSelection = CanvasSelectionTarget.STICKER
            activeTab = VideoToolTab.STICKER
            isToolDrawerExpanded = true
        }
    }

    val audioLanePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importAudioToVideoLane(uri)
    }

    val videoMicPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVideoVoiceoverRecording()
        } else {
            viewModel.showToast("Allow microphone access to record voiceover")
        }
    }

    val speechCaptionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setListeningCaptions(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.addVideoText(spoken, isCaption = true)
                viewModel.showToast("Caption added")
            }
        }
    }

    val tabs = remember {
        listOf(
            ToolTabItem(VideoToolTab.TRIM, "Trim", Icons.Default.SwapHoriz),
            ToolTabItem(VideoToolTab.SPLIT, "Split", Icons.Default.ContentCut),
            ToolTabItem(VideoToolTab.AUDIO, "Audio", Icons.Default.MusicNote),
            ToolTabItem(VideoToolTab.TEXT, "Text", Icons.Default.Title),
            ToolTabItem(VideoToolTab.STICKER, "Sticker", Icons.Default.Photo),
            ToolTabItem(VideoToolTab.FILTER, "Filter", Icons.Default.FilterVintage),
            ToolTabItem(VideoToolTab.TRANSITIONS, "FX", Icons.Default.AutoAwesome),
            ToolTabItem(VideoToolTab.SPEED, "Speed", Icons.Default.Speed),
            ToolTabItem(VideoToolTab.KEYFRAME, "Keyframe", Icons.Default.Diamond),
            ToolTabItem(VideoToolTab.DELETE, "Delete", Icons.Default.Delete),
            ToolTabItem(VideoToolTab.ADJUST, "Adjust", Icons.Default.Tune),
            ToolTabItem(VideoToolTab.ENHANCE, "AI Magic", Icons.Default.AutoAwesome),
            ToolTabItem(VideoToolTab.FRAME, "Transform", Icons.Default.AspectRatio),
            ToolTabItem(VideoToolTab.OVERLAY, "Overlay", Icons.Default.PictureInPictureAlt),
            ToolTabItem(VideoToolTab.EXPORT, "Export", Icons.Default.Download)
        )
    }

    val hasActiveUserPip = state.pip.enabled && !state.pip.uriString.isNullOrBlank()

    // =========================================================================
    // FULLSCREEN THEATER PREVIEW MODE (VN / InShot Instant Expand)
    // =========================================================================
    if (isFullscreen) {
        BackHandler { isFullscreen = false }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(OledBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null && !previewBitmap.isRecycled) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Fullscreen Video Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top Exit Fullscreen Pill
            Surface(
                onClick = { isFullscreen = false },
                shape = RoundedCornerShape(50),
                color = StudioSurfaceElevated.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .testTag("video_fullscreen_exit")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Exit",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }

            // Bottom Translucent Playback Controls
            Surface(
                color = StudioSurface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Slider(
                        value = state.playheadMs.toFloat(),
                        onValueChange = { viewModel.seekPlayhead(it.toLong()) },
                        valueRange = 0f..state.totalDurationMs.toFloat().coerceAtLeast(1000f),
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = StudioBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatSimpleTime(state.playheadMs)} / ${formatSimpleTime(state.totalDurationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Surface(
                            onClick = { viewModel.toggleVideoPlayback() },
                            shape = CircleShape,
                            color = ElectricCyan,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.toggleLoopPlayback() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "Loop Video",
                                tint = if (state.loopPlayback) ElectricCyan else TextSecondary
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val currentState by rememberUpdatedState(state)
    val currentCanvasSelection by rememberUpdatedState(canvasSelection)
    val activePlayheadInfo = remember(state.clips, state.playheadMs) {
        var acc = 0L
        var found: Pair<com.example.model.VideoClip, Long>? = null
        for (i in state.clips.indices) {
            val clip = state.clips[i]
            val dur = clip.effectiveDurationMs
            if (state.playheadMs <= acc + dur || i == state.clips.lastIndex) {
                found = clip to (state.playheadMs - acc).coerceIn(0L, dur)
                break
            }
            acc += dur
        }
        found
    }
    val activePlayheadClip = activePlayheadInfo?.first
    val activePlayheadLocalMs = activePlayheadInfo?.second ?: 0L
    val activeKeyframeInterp = remember(activePlayheadClip?.keyframes, activePlayheadLocalMs) {
        val kfs = activePlayheadClip?.keyframes.orEmpty()
        VideoMediaEngine.interpolateKeyframes(kfs, activePlayheadLocalMs)
    }
    val selectedClipRotDeg = (activePlayheadClip?.rotationDeg ?: state.selectedClip?.rotationDeg ?: 0f) + activeKeyframeInterp.rotationDeg

    val selectedOverlayLayer = remember(state.textLayers, state.selectedTextId) {
        state.textLayers.firstOrNull { it.id == state.selectedTextId }
    }

    val clearSelectionAndCloseDrawer: () -> Unit = {
        viewModel.clearTimelineSelection()
        isClipQuickBarVisible = false
        isToolDrawerExpanded = false
        isStickerCropping = false
        canvasSelection = CanvasSelectionTarget.NONE
    }

    LaunchedEffect(hasActiveUserPip) {
        if (!hasActiveUserPip && canvasSelection == CanvasSelectionTarget.PIP) {
            canvasSelection = CanvasSelectionTarget.NONE
        }
    }

    LaunchedEffect(selectedOverlayLayer?.id, selectedOverlayLayer?.isSticker) {
        if (selectedOverlayLayer == null) {
            isStickerCropping = false
            if (canvasSelection == CanvasSelectionTarget.TEXT || canvasSelection == CanvasSelectionTarget.STICKER) {
                canvasSelection = CanvasSelectionTarget.NONE
            }
        } else if (!selectedOverlayLayer.isSticker) {
            isStickerCropping = false
        }
    }

    LaunchedEffect(state.clips.isEmpty()) {
        if (state.clips.isEmpty()) {
            if (canvasSelection == CanvasSelectionTarget.VIDEO) {
                canvasSelection = CanvasSelectionTarget.NONE
            }
            isClipQuickBarVisible = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // =========================================================================
        // 1. UPPER INTERACTIVE VIDEO PREVIEW CANVAS + PLAYBAR (Expands smoothly to fill middle gap when drawer is closed)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isToolDrawerExpanded) 0.45f else 0.62f)
                .background(OledBlack)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        canvasSelection = CanvasSelectionTarget.NONE
                        isToolDrawerExpanded = false
                    }
                    .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                val targetRatio = state.aspectPreset.ratioW.toFloat() / state.aspectPreset.ratioH.toFloat()
                val availW = maxWidth
                val availH = maxHeight
                val stageW = if (availH > 0.dp && (availW / availH) > targetRatio) {
                    availH * targetRatio
                } else {
                    availW
                }
                val stageH = if (availH > 0.dp && (availW / availH) > targetRatio) {
                    availH
                } else {
                    availW / targetRatio
                }

                // Exact aspect-ratio stage (matches previewBitmap dimensions 1:1 across 16:9, 9:16, 1:1, 4:5, 21:9)
                // Clipped cleanly to viewport bounds so resize box & handles never overflow the player container
                Box(
                    modifier = Modifier
                        .size(width = stageW, height = stageH)
                        .clip(RoundedCornerShape(12.dp))
                        .clipToBounds()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val canvasW = with(density) { stageW.roundToPx() }.coerceAtLeast(1)
                    val canvasH = with(density) { stageH.roundToPx() }.coerceAtLeast(1)
                    val minCanvasEdge = minOf(canvasW, canvasH).coerceAtLeast(1).toFloat()

                    // Clipped rounded pure-black video preview surface with top-to-bottom hit-testing (Stickers -> Text -> PIP -> Video)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(hasActiveUserPip, state.clips.isNotEmpty(), state.textLayers.size) {
                                detectTapGestures { tapOffset ->
                                    if (isStickerCropping) return@detectTapGestures
                                    val w = size.width.coerceAtLeast(1).toFloat()
                                    val h = size.height.coerceAtLeast(1).toFloat()
                                    val curState = currentState

                                    // 1. Hit-test active Stickers & Text overlays first (topmost layers)
                                    val visibleOverlays = curState.textLayers
                                        .filter { curState.playheadMs in it.startMs..it.endMs }
                                        .asReversed()
                                    for (layer in visibleOverlays) {
                                        val b = VideoOverlayRenderer.measureOverlayBounds(context, layer, w, h)
                                        val pad = 16f
                                        val hit = tapOffset.x in (b.cx - b.width / 2f - pad)..(b.cx + b.width / 2f + pad) &&
                                            tapOffset.y in (b.cy - b.height / 2f - pad)..(b.cy + b.height / 2f + pad)
                                        if (hit) {
                                            viewModel.selectVideoText(layer.id)
                                            isClipQuickBarVisible = false
                                            if (layer.isSticker) {
                                                canvasSelection = CanvasSelectionTarget.STICKER
                                                activeTab = VideoToolTab.STICKER
                                            } else {
                                                canvasSelection = CanvasSelectionTarget.TEXT
                                                activeTab = VideoToolTab.TEXT
                                            }
                                            isToolDrawerExpanded = true
                                            return@detectTapGestures
                                        }
                                    }

                                    // 2. Hit-test user PIP overlay
                                    val pip = curState.pip
                                    if (hasActiveUserPip) {
                                        val pipW = (w * pip.scale).coerceIn(88f, w)
                                        val pipH = (h * pip.scale).coerceIn(72f, h)
                                        val cx = ImageProcessingEngine.clampFrameCenter(w * pip.xNorm, pipW, w)
                                        val cy = ImageProcessingEngine.clampFrameCenter(h * pip.yNorm, pipH, h)
                                        val hitPip = tapOffset.x in (cx - pipW / 2f)..(cx + pipW / 2f) &&
                                            tapOffset.y in (cy - pipH / 2f)..(cy + pipH / 2f)
                                        if (hitPip) {
                                            canvasSelection = if (currentCanvasSelection == CanvasSelectionTarget.PIP) {
                                                CanvasSelectionTarget.NONE
                                            } else {
                                                CanvasSelectionTarget.PIP
                                            }
                                            return@detectTapGestures
                                        }
                                    }

                                    // 3. Fallback to Video Frame selection
                                    if (curState.clips.isEmpty()) {
                                        canvasSelection = CanvasSelectionTarget.NONE
                                        return@detectTapGestures
                                    }
                                    canvasSelection = if (currentCanvasSelection == CanvasSelectionTarget.VIDEO) {
                                        CanvasSelectionTarget.NONE
                                    } else {
                                        CanvasSelectionTarget.VIDEO
                                    }
                                }
                            }
                            .pointerInput("video_canvas_multitouch") {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    if (isStickerCropping) return@detectTransformGestures
                                    if (zoom == 1f && rotation == 0f && currentCanvasSelection == CanvasSelectionTarget.NONE) {
                                        return@detectTransformGestures
                                    }
                                    val w = size.width.coerceAtLeast(1).toFloat()
                                    val h = size.height.coerceAtLeast(1).toFloat()
                                    if (currentCanvasSelection == CanvasSelectionTarget.NONE && currentState.clips.isNotEmpty()) {
                                        canvasSelection = CanvasSelectionTarget.VIDEO
                                    }
                                    when (currentCanvasSelection) {
                                        CanvasSelectionTarget.TEXT,
                                        CanvasSelectionTarget.STICKER -> {
                                            viewModel.updateSelectedVideoText { layer ->
                                                layer.copy(
                                                    xNorm = (layer.xNorm + pan.x / w).coerceIn(0.04f, 0.96f),
                                                    yNorm = (layer.yNorm + pan.y / h).coerceIn(0.04f, 0.96f),
                                                    scale = (layer.scale * zoom).coerceIn(0.25f, 3.0f),
                                                    rotationDeg = normalizeAngleDeg(layer.rotationDeg + rotation)
                                                )
                                            }
                                        }
                                        CanvasSelectionTarget.PIP -> if (hasActiveUserPip) {
                                            val p = currentState.pip
                                            viewModel.updatePip {
                                                it.copy(
                                                    xNorm = (it.xNorm + pan.x / w).coerceIn(0.08f, 0.92f),
                                                    yNorm = (it.yNorm + pan.y / h).coerceIn(0.08f, 0.92f),
                                                    scale = (it.scale * zoom).coerceIn(0.15f, 0.95f),
                                                    rotationDeg = normalizeAngleDeg(p.rotationDeg + rotation)
                                                )
                                            }
                                        }
                                        CanvasSelectionTarget.VIDEO -> if (currentState.clips.isNotEmpty()) {
                                            // Pure pinch-scale & pan on Video Frame — NO accidental rotation
                                            val s = currentState
                                            viewModel.updateVideoCanvasTransform(
                                                zoom = if (zoom != 1f) (s.cropZoom * zoom).coerceIn(0.25f, 3.0f) else null,
                                                panX = if (pan.x != 0f) (s.panX + pan.x / (w * 0.4f)).coerceIn(-1f, 1f) else null,
                                                panY = if (pan.y != 0f) (s.panY + pan.y / (h * 0.4f)).coerceIn(-1f, 1f) else null,
                                                rotationDeg = null
                                            )
                                        }
                                        CanvasSelectionTarget.NONE -> Unit
                                    }
                                }
                            }
                            .testTag("video_preview_canvas_touch_surface"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null && !previewBitmap.isRecycled) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Video Preview",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 1. Tightly Fitted White Bounding Box for Video Frame (hugs exact zoomed-in / zoomed-out frame!)
                    if (!isStickerCropping && canvasSelection == CanvasSelectionTarget.VIDEO && activePlayheadClip != null) {
                        val srcAspect = if (activePlayheadClip.sourceWidth > 0 && activePlayheadClip.sourceHeight > 0) {
                            activePlayheadClip.sourceWidth.toFloat() / activePlayheadClip.sourceHeight.toFloat()
                        } else {
                            liveSourceAspect
                        }
                        val safeSrcAspect = srcAspect.coerceIn(0.25f, 4.0f)
                        val fitW: Float
                        val fitH: Float
                        if (canvasW.toFloat() / canvasH.toFloat() > safeSrcAspect) {
                            fitH = canvasH.toFloat()
                            fitW = fitH * safeSrcAspect
                        } else {
                            fitW = canvasW.toFloat()
                            fitH = fitW / safeSrcAspect
                        }
                        val stabilizeBoost = if (activePlayheadClip.stabilizeEnabled) 1.09f else 1f
                        val effZoom = state.cropZoom.coerceIn(0.25f, 3.0f) *
                            activeKeyframeInterp.scale.coerceIn(0.2f, 3.0f) *
                            stabilizeBoost
                        val drawW = fitW * effZoom
                        val drawH = fitH * effZoom
                        val clampedCx = ImageProcessingEngine.clampFrameCenter(
                            canvasW / 2f + (state.panX + activeKeyframeInterp.xNorm) * canvasW * 0.4f,
                            drawW,
                            canvasW.toFloat()
                        )
                        val clampedCy = ImageProcessingEngine.clampFrameCenter(
                            canvasH / 2f + (state.panY + activeKeyframeInterp.yNorm) * canvasH * 0.4f,
                            drawH,
                            canvasH.toFloat()
                        )
                        val visLeft = (clampedCx - drawW / 2f).coerceIn(0f, canvasW.toFloat())
                        val visRight = (clampedCx + drawW / 2f).coerceIn(visLeft, canvasW.toFloat())
                        val visTop = (clampedCy - drawH / 2f).coerceIn(0f, canvasH.toFloat())
                        val visBottom = (clampedCy + drawH / 2f).coerceIn(visTop, canvasH.toFloat())
                        val boxW = (visRight - visLeft).roundToInt().coerceIn(36, canvasW)
                        val boxH = (visBottom - visTop).roundToInt().coerceIn(36, canvasH)
                        val leftPx = visLeft.roundToInt().coerceIn(0, (canvasW - boxW).coerceAtLeast(0))
                        val topPx = visTop.roundToInt().coerceIn(0, (canvasH - boxH).coerceAtLeast(0))

                        InteractiveWhiteTransformBox(
                            leftPx = leftPx,
                            topPx = topPx,
                            boxW = boxW,
                            boxH = boxH,
                            rotationDeg = selectedClipRotDeg,
                            showDeleteX = false,
                            onDeleteClick = {},
                            onDragMoveDelta = { dxPx, dyPx ->
                                val s = currentState
                                viewModel.updateVideoCanvasTransform(
                                    panX = (s.panX + dxPx / (canvasW.toFloat() * 0.4f)).coerceIn(-1f, 1f),
                                    panY = (s.panY + dyPx / (canvasH.toFloat() * 0.4f)).coerceIn(-1f, 1f)
                                )
                            },
                            onPinchRotate = { zoom, _ ->
                                val s = currentState
                                if (zoom != 1f) {
                                    viewModel.updateVideoCanvasTransform(
                                        zoom = (s.cropZoom * zoom).coerceIn(0.25f, 3.0f)
                                    )
                                }
                            },
                            onCornerScaleDelta = { deltaNorm ->
                                val nextZoom = (currentState.cropZoom + deltaNorm * 1.1f).coerceIn(0.25f, 3.0f)
                                viewModel.updateVideoCanvasTransform(zoom = nextZoom)
                            },
                            onRotateDeltaDeg = { deltaDeg ->
                                val baseRot = activePlayheadClip.rotationDeg
                                viewModel.updateVideoCanvasTransform(
                                    rotationDeg = normalizeAngleDeg(baseRot + deltaDeg)
                                )
                            },
                            onTapBox = {
                                canvasSelection = CanvasSelectionTarget.NONE
                            },
                            minCanvasEdge = minCanvasEdge,
                            tagPrefix = "video_canvas",
                            allowPinchRotate = false,
                            viewportW = canvasW,
                            viewportH = canvasH
                        )
                    }

                    // 2. Tightly Fitted White Bounding Box for Selected Text or Sticker Layer (hugs exact scaled & cropped bounds!)
                    if (
                        !isStickerCropping &&
                        selectedOverlayLayer != null &&
                        state.playheadMs in selectedOverlayLayer.startMs..selectedOverlayLayer.endMs &&
                        (canvasSelection == CanvasSelectionTarget.TEXT || canvasSelection == CanvasSelectionTarget.STICKER)
                    ) {
                        val b = VideoOverlayRenderer.measureOverlayBounds(
                            context = context,
                            layer = selectedOverlayLayer,
                            targetW = canvasW.toFloat(),
                            targetH = canvasH.toFloat()
                        )
                        val boxW = b.width.roundToInt().coerceIn(36, canvasW)
                        val boxH = b.height.roundToInt().coerceIn(36, canvasH)
                        val leftPx = (b.cx - boxW / 2f).roundToInt().coerceIn(0, (canvasW - boxW).coerceAtLeast(0))
                        val topPx = (b.cy - boxH / 2f).roundToInt().coerceIn(0, (canvasH - boxH).coerceAtLeast(0))

                        InteractiveWhiteTransformBox(
                            leftPx = leftPx,
                            topPx = topPx,
                            boxW = boxW,
                            boxH = boxH,
                            rotationDeg = selectedOverlayLayer.rotationDeg,
                            showDeleteX = true,
                            onDeleteClick = {
                                val delId = selectedOverlayLayer.id
                                canvasSelection = CanvasSelectionTarget.NONE
                                viewModel.deleteVideoText(delId)
                            },
                            onDragMoveDelta = { dxPx, dyPx ->
                                viewModel.updateSelectedVideoText { layer ->
                                    layer.copy(
                                        xNorm = (layer.xNorm + dxPx / canvasW.toFloat()).coerceIn(0.04f, 0.96f),
                                        yNorm = (layer.yNorm + dyPx / canvasH.toFloat()).coerceIn(0.04f, 0.96f)
                                    )
                                }
                            },
                            onPinchRotate = { zoom, rotDeg ->
                                viewModel.updateSelectedVideoText { layer ->
                                    layer.copy(
                                        scale = (layer.scale * zoom).coerceIn(0.25f, 3.0f),
                                        rotationDeg = normalizeAngleDeg(layer.rotationDeg + rotDeg)
                                    )
                                }
                            },
                            onCornerScaleDelta = { deltaNorm ->
                                viewModel.updateSelectedVideoText { layer ->
                                    layer.copy(scale = (layer.scale + deltaNorm * 1.15f).coerceIn(0.25f, 3.0f))
                                }
                            },
                            onRotateDeltaDeg = { deltaDeg ->
                                viewModel.updateSelectedVideoText { layer ->
                                    layer.copy(rotationDeg = normalizeAngleDeg(layer.rotationDeg + deltaDeg))
                                }
                            },
                            onTapBox = {
                                activeTab = if (selectedOverlayLayer.isSticker) VideoToolTab.STICKER else VideoToolTab.TEXT
                                isToolDrawerExpanded = true
                            },
                            onDoubleTapBox = {
                                if (selectedOverlayLayer.isSticker) {
                                    isStickerCropping = true
                                } else {
                                    activeTab = VideoToolTab.TEXT
                                    isToolDrawerExpanded = true
                                }
                            },
                            badgeHint = if (selectedOverlayLayer.isSticker) "Double-tap to crop" else "Double-tap to edit",
                            minCanvasEdge = minCanvasEdge,
                            tagPrefix = if (selectedOverlayLayer.isSticker) "sticker_canvas" else "text_canvas",
                            allowPinchRotate = true,
                            viewportW = canvasW,
                            viewportH = canvasH
                        )
                    }

                    // 3. Interactive On-Canvas Sticker Crop Mode (Free / 1:1 / 16:9 / 9:16 + Apply & Cancel)
                    if (isStickerCropping && selectedOverlayLayer != null && selectedOverlayLayer.isSticker) {
                        StickerInteractiveCropOverlay(
                            sticker = selectedOverlayLayer,
                            viewportW = canvasW,
                            viewportH = canvasH,
                            onApplyCrop = { l, t, r, b ->
                                viewModel.updateStickerCrop(selectedOverlayLayer.id, l, t, r, b)
                                isStickerCropping = false
                            },
                            onCancelCrop = {
                                isStickerCropping = false
                            }
                        )
                    }

                    // 4. Active User-Added PIP Overlay Bounding Box + Corner Handles + Top Rotate + Red "X" Delete
                    if (!isStickerCropping && hasActiveUserPip) {
                        val boxW = (canvasW * state.pip.scale).roundToInt().coerceIn(64, canvasW)
                        val boxH = (canvasH * state.pip.scale).roundToInt().coerceIn(56, canvasH)
                        val pipCx = ImageProcessingEngine.clampFrameCenter(canvasW * state.pip.xNorm, boxW.toFloat(), canvasW.toFloat())
                        val pipCy = ImageProcessingEngine.clampFrameCenter(canvasH * state.pip.yNorm, boxH.toFloat(), canvasH.toFloat())
                        val leftPx = (pipCx - boxW / 2f).roundToInt().coerceIn(0, (canvasW - boxW).coerceAtLeast(0))
                        val topPx = (pipCy - boxH / 2f).roundToInt().coerceIn(0, (canvasH - boxH).coerceAtLeast(0))

                        if (canvasSelection == CanvasSelectionTarget.PIP) {
                            InteractiveWhiteTransformBox(
                                leftPx = leftPx,
                                topPx = topPx,
                                boxW = boxW,
                                boxH = boxH,
                                rotationDeg = state.pip.rotationDeg,
                                showDeleteX = true,
                                onDeleteClick = {
                                    canvasSelection = CanvasSelectionTarget.NONE
                                    viewModel.removePipOverlay()
                                },
                                onDragMoveDelta = { dxPx, dyPx ->
                                    viewModel.updatePip { p ->
                                        p.copy(
                                            xNorm = (p.xNorm + dxPx / canvasW.toFloat()).coerceIn(0.08f, 0.92f),
                                            yNorm = (p.yNorm + dyPx / canvasH.toFloat()).coerceIn(0.08f, 0.92f)
                                        )
                                    }
                                },
                                onPinchRotate = { zoom, rotDeg ->
                                    viewModel.updatePip { p ->
                                        p.copy(
                                            scale = (p.scale * zoom).coerceIn(0.15f, 0.95f),
                                            rotationDeg = normalizeAngleDeg(p.rotationDeg + rotDeg)
                                        )
                                    }
                                },
                                onCornerScaleDelta = { deltaNorm ->
                                    viewModel.updatePip { p ->
                                        p.copy(scale = (p.scale + deltaNorm * 0.85f).coerceIn(0.15f, 0.95f))
                                    }
                                },
                                onRotateDeltaDeg = { deltaDeg ->
                                    viewModel.updatePip { p ->
                                        p.copy(rotationDeg = normalizeAngleDeg(p.rotationDeg + deltaDeg))
                                    }
                                },
                                onTapBox = {
                                    canvasSelection = CanvasSelectionTarget.NONE
                                },
                                minCanvasEdge = minCanvasEdge,
                                tagPrefix = "pip_canvas",
                                viewportW = canvasW,
                                viewportH = canvasH
                            )
                        } else {
                            // Unselected active PIP still shows clean subtle frame + 1-tap Red "X" corner button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset { IntOffset(leftPx, topPx) }
                                    .size(
                                        width = with(density) { boxW.toDp() },
                                        height = with(density) { boxH.toDp() }
                                    )
                                    .graphicsLayer { rotationZ = state.pip.rotationDeg }
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Surface(
                                    onClick = { viewModel.removePipOverlay() },
                                    shape = CircleShape,
                                    color = SignalRed,
                                    border = BorderStroke(1.5.dp, Color.White),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(26.dp)
                                        .testTag("pip_overlay_delete_x_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Overlay",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Thin Draggable YouTube-Style Seekbar Slider synced with Center White Playhead & Live Timecode
            val latestTotalDurationMs by rememberUpdatedState(state.totalDurationMs)
            var isScrubbingSeekbar by remember { mutableStateOf(false) }
            val progressFraction = if (state.totalDurationMs > 0L) {
                (state.playheadMs.toFloat() / state.totalDurationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(horizontal = 12.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            isScrubbingSeekbar = true
                            val w = size.width.coerceAtLeast(1).toFloat()
                            val startFrac = (down.position.x / w).coerceIn(0f, 1f)
                            val dur0 = latestTotalDurationMs
                            if (dur0 > 0L) {
                                viewModel.seekPlayhead((startFrac * dur0).toLong())
                            }
                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                    if (change != null && change.pressed) {
                                        change.consume()
                                        val frac = (change.position.x / w).coerceIn(0f, 1f)
                                        val dur = latestTotalDurationMs
                                        if (dur > 0L) {
                                            viewModel.seekPlayhead((frac * dur).toLong())
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            } finally {
                                isScrubbingSeekbar = false
                            }
                        }
                    }
                    .testTag("video_youtube_progress_line"),
                contentAlignment = Alignment.CenterStart
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackH = 3.dp.toPx()
                    val cy = size.height / 2f
                    val thumbRadius = if (isScrubbingSeekbar) 6.5.dp.toPx() else 5.dp.toPx()
                    val activeW = (size.width * progressFraction).coerceIn(0f, size.width)
                    // Inactive thin track
                    drawRoundRect(
                        color = StudioBorder.copy(alpha = 0.65f),
                        topLeft = Offset(0f, cy - trackH / 2f),
                        size = Size(size.width, trackH),
                        cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                    )
                    // Active cyan progress track
                    if (activeW > 0f) {
                        drawRoundRect(
                            color = ElectricCyan,
                            topLeft = Offset(0f, cy - trackH / 2f),
                            size = Size(activeW, trackH),
                            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                        )
                    }
                    // Crisp YouTube-style scrub thumb
                    val thumbX = activeW.coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius))
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius,
                        center = Offset(thumbX, cy)
                    )
                    drawCircle(
                        color = ElectricCyan,
                        radius = (thumbRadius - 1.5.dp.toPx()).coerceAtLeast(1.5f),
                        center = Offset(thumbX, cy)
                    )
                }
            }

            // Sleek VN / YouTube Create Playbar Row: Aspect + Live Time (0.66 / 3.00) | Play/Pause | Undo (↩), Redo (↪), Loop, Freeze, Fullscreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = {
                            val presets = VideoAspectPreset.entries
                            val nextIdx = (presets.indexOf(state.aspectPreset) + 1) % presets.size
                            viewModel.setVideoAspect(presets[nextIdx])
                        },
                        shape = RoundedCornerShape(50),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.testTag("video_aspect_ratio_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = ElectricCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = state.aspectPreset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }

                    Text(
                        text = String.format(
                            Locale.US,
                            "%.2f / %.2f",
                            state.playheadMs / 1000f,
                            state.totalDurationMs / 1000f
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.testTag("video_live_timecode")
                    )
                }

                Surface(
                    onClick = { viewModel.toggleVideoPlayback() },
                    shape = CircleShape,
                    color = ElectricCyan,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("video_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.undoVideoEdit() },
                        enabled = canUndoVideo,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("video_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndoVideo) Color.White else TextSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redoVideoEdit() },
                        enabled = canRedoVideo,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("video_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedoVideo) Color.White else TextSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            activeTab = VideoToolTab.ENHANCE
                            isToolDrawerExpanded = true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("video_ai_magic_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Magic Quick Enhance",
                            tint = if (state.colorGrade.qualityEnhancerEnabled || (isToolDrawerExpanded && activeTab == VideoToolTab.ENHANCE)) ElectricCyan else ProAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleLoopPlayback() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = "Loop Video",
                            tint = if (state.loopPlayback) ElectricCyan else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.captureFreezeFrameHd() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("video_freeze_frame_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Freeze Frame HD",
                            tint = TextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("video_fullscreen_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen Preview",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 2. LOWER TIMELINE + COLLAPSIBLE DRAWER + PINNED BOTTOM TOOL BAR
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            border = BorderStroke(0.5.dp, StudioBorder.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isToolDrawerExpanded) 0.55f else 0.38f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Contextual Floating Quick-Action Bar (Hidden until a clip is tapped)
                AnimatedVisibility(
                    visible = isClipQuickBarVisible && state.selectedClipId != null && state.selectedClip != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        color = StudioSurfaceElevated,
                        border = BorderStroke(0.5.dp, StudioBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clip_quick_action_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuickActionChip(
                                label = "Replace",
                                icon = Icons.Default.SwapHoriz,
                                tint = TextPrimary,
                                testTagName = "quick_action_replace",
                                onClick = {
                                    replaceClipPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                            )
                            QuickActionChip(
                                label = "Keyframe ◆",
                                icon = Icons.Default.Diamond,
                                tint = ProAmber,
                                testTagName = "quick_action_keyframe",
                                onClick = {
                                    viewModel.addOrUpdateKeyframeAtPlayhead()
                                    viewModel.showToast("Keyframe set")
                                }
                            )
                            QuickActionChip(
                                label = "Curve",
                                icon = Icons.Default.Speed,
                                tint = ElectricCyan,
                                testTagName = "quick_action_curve",
                                onClick = {
                                    activeTab = VideoToolTab.SPEED
                                    isToolDrawerExpanded = true
                                }
                            )
                            QuickActionChip(
                                label = "Duplicate",
                                icon = Icons.Default.ContentCopy,
                                tint = TextPrimary,
                                testTagName = "quick_action_duplicate",
                                onClick = { viewModel.duplicateSelectedClip() }
                            )
                            QuickActionChip(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                tint = SignalRed,
                                testTagName = "quick_action_delete",
                                onClick = {
                                    viewModel.deleteSelectedClip()
                                    isClipQuickBarVisible = false
                                }
                            )
                            IconButton(
                                onClick = { isClipQuickBarVisible = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide Clip Actions",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Fixed-Center-Playhead Scrollable Timeline Container (Pinch-to-Zoom + Direct Drag Trim)
                val timelineScrollState = rememberScrollState()
                val pxPerSecBase = 46f
                val clipWidthsDp = remember(state.clips, timelineZoom) {
                    state.clips.map { clip ->
                        ((clip.effectiveDurationMs / 1000f) * pxPerSecBase * timelineZoom).coerceIn(82f, 520f)
                    }
                }
                val clipWidthsPx = remember(clipWidthsDp, density) {
                    clipWidthsDp.map { with(density) { it.dp.toPx() } }
                }

                // Sync 1: User drags/scrolls timeline -> updates playheadMs directly under fixed center white line
                LaunchedEffect(timelineScrollState.value, timelineScrollState.isScrollInProgress) {
                    if (timelineScrollState.isScrollInProgress && state.clips.isNotEmpty()) {
                        val scrollPx = timelineScrollState.value.toFloat()
                        var accPx = 0f
                        var accMs = 0L
                        var targetMs = state.totalDurationMs
                        for (i in state.clips.indices) {
                            val wPx = clipWidthsPx.getOrElse(i) { 100f }.coerceAtLeast(1f)
                            val durMs = state.clips[i].effectiveDurationMs
                            if (scrollPx <= accPx + wPx) {
                                val frac = ((scrollPx - accPx) / wPx).coerceIn(0f, 1f)
                                targetMs = (accMs + (durMs * frac).toLong()).coerceIn(0L, state.totalDurationMs)
                                break
                            }
                            accPx += wPx
                            accMs += durMs
                        }
                        if (abs(targetMs - state.playheadMs) >= 15L) {
                            viewModel.seekPlayhead(targetMs)
                        }
                    }
                }

                // Sync 2: Playback or external seek -> glides timeline smoothly past fixed center white line
                LaunchedEffect(state.playheadMs, state.isPlaying, timelineZoom, clipWidthsPx) {
                    if (!timelineScrollState.isScrollInProgress && state.clips.isNotEmpty()) {
                        val targetMs = state.playheadMs
                        var accMs = 0L
                        var accPx = 0f
                        var targetPx = clipWidthsPx.sum()
                        for (i in state.clips.indices) {
                            val durMs = state.clips[i].effectiveDurationMs.coerceAtLeast(1L)
                            val wPx = clipWidthsPx.getOrElse(i) { 100f }
                            if (targetMs <= accMs + durMs) {
                                val frac = ((targetMs - accMs).toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
                                targetPx = accPx + wPx * frac
                                break
                            }
                            accMs += durMs
                            accPx += wPx
                        }
                        val desired = targetPx.roundToInt().coerceIn(0, timelineScrollState.maxValue)
                        if (abs(timelineScrollState.value - desired) > 3) {
                            timelineScrollState.scrollTo(desired)
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isToolDrawerExpanded) 0.40f else 1f)
                        .background(DeepObsidian)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { clearSelectionAndCloseDrawer() }
                        .pointerInput("timeline_pinch_zoom") {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2) {
                                        val zoomFactor = event.calculateZoom()
                                        if (zoomFactor != 1f) {
                                            timelineZoom = (timelineZoom * zoomFactor).coerceIn(0.55f, 3.2f)
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .testTag("timeline_empty_space"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val halfViewportDp = maxWidth / 2
                    val totalClipsWidthDp = clipWidthsDp.sum().dp.coerceAtLeast(180.dp)

                    // Precompute clip start timestamps (ms) and seam X positions (dp)
                    val clipStartMsList = remember(state.clips) {
                        var acc = 0L
                        state.clips.map { c ->
                            val start = acc
                            acc += c.effectiveDurationMs
                            start
                        }
                    }

                    // Synchronized Horizontally Scrollable Video Filmstrip + Full-Width Draggable Audio Waveform Bars
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { clearSelectionAndCloseDrawer() },
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(timelineScrollState)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { clearSelectionAndCloseDrawer() }
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        // Row 1: Video Filmstrip Clips + Unclipped Sibling Seam Transition Joints
                        Box(
                            modifier = Modifier.height(50.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(Modifier.width(halfViewportDp))

                                if (state.clips.isEmpty()) {
                                    Surface(
                                        onClick = {
                                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = StudioSurface,
                                        border = BorderStroke(1.dp, StudioBorder),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("empty_video_timeline_placeholder")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Video Clip",
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Tap + to add video clip",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                state.clips.forEachIndexed { idx, clip ->
                                    val isSel = state.selectedClipId != null && clip.id == state.selectedClipId
                                    val thumbs = clipThumbnails[clip.id].orEmpty()
                                    val currentClip by rememberUpdatedState(clip)
                                    val currentZoom by rememberUpdatedState(timelineZoom)
                                    val clipWidthDp = clipWidthsDp.getOrElse(idx) { 120f }.dp
                                    val clipStartMs = clipStartMsList.getOrElse(idx) { 0L }

                                    Surface(
                                        onClick = {
                                            if (isSel) {
                                                isClipQuickBarVisible = !isClipQuickBarVisible
                                            } else {
                                                viewModel.selectClip(clip.id)
                                                canvasSelection = CanvasSelectionTarget.VIDEO
                                                isClipQuickBarVisible = true
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = StudioSurface,
                                        border = BorderStroke(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) Color.White else StudioBorder
                                        ),
                                        modifier = Modifier
                                            .width(clipWidthDp)
                                            .height(48.dp)
                                            .then(
                                                if (isSel && state.clips.size > 1) {
                                                    Modifier.pointerInput("video_body_reorder_${clip.id}") {
                                                        val swapThresholdPx = with(density) { 52.dp.toPx() }
                                                        var accumDx = 0f
                                                        detectHorizontalDragGestures(
                                                            onDragStart = { accumDx = 0f },
                                                            onDragEnd = { accumDx = 0f },
                                                            onDragCancel = { accumDx = 0f }
                                                        ) { change, dragAmount ->
                                                            change.consume()
                                                            accumDx += dragAmount
                                                            if (accumDx > swapThresholdPx) {
                                                                viewModel.reorderClip(currentClip.id, moveLeft = false)
                                                                accumDx = 0f
                                                            } else if (accumDx < -swapThresholdPx) {
                                                                viewModel.reorderClip(currentClip.id, moveLeft = true)
                                                                accumDx = 0f
                                                            }
                                                        }
                                                    }
                                                } else Modifier
                                            )
                                            .testTag("filmstrip_clip_$idx")
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            if (thumbs.isNotEmpty()) {
                                                Row(modifier = Modifier.fillMaxSize()) {
                                                    thumbs.forEach { bmp ->
                                                        if (!bmp.isRecycled) {
                                                            Image(
                                                                bitmap = bmp.asImageBitmap(),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxHeight()
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                OledBlack.copy(alpha = 0.2f),
                                                                OledBlack.copy(alpha = 0.78f)
                                                            )
                                                        )
                                                    )
                                                    .padding(
                                                        start = if (isSel) 20.dp else 6.dp,
                                                        end = if (isSel) 20.dp else 6.dp,
                                                        top = 3.dp,
                                                        bottom = 3.dp
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomStart),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${idx + 1}. ${clip.name}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = if (isSel) Color.White else TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = String.format(Locale.US, "%.1fs", clip.effectiveDurationMs / 1000f),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        color = if (isSel) ElectricCyan else TextSecondary
                                                    )
                                                }
                                            }

                                            // Direct 1-tap Red Trash Delete badge on Selected Video Clip
                                            if (isSel) {
                                                Surface(
                                                    onClick = {
                                                        viewModel.deleteVideoClipById(clip.id)
                                                        isClipQuickBarVisible = false
                                                    },
                                                    shape = CircleShape,
                                                    color = SignalRed,
                                                    border = BorderStroke(1.dp, Color.White),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(end = 21.dp, top = 2.dp)
                                                        .size(18.dp)
                                                        .zIndex(7f)
                                                        .testTag("timeline_video_delete_${clip.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete ${clip.name}",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Exact-Timestamp Diamond Keyframe Markers aligned dead-center with Playhead
                                            if (clip.keyframes.isNotEmpty()) {
                                                val clipWpx = with(density) { clipWidthDp.toPx() }.coerceAtLeast(1f)
                                                val markerPx = with(density) { 14.dp.toPx() }
                                                clip.keyframes.forEach { kf ->
                                                    val frac = (kf.timeMs.toFloat() / clip.effectiveDurationMs.coerceAtLeast(1L).toFloat())
                                                        .coerceIn(0f, 1f)
                                                    val xPx = (clipWpx * frac - markerPx / 2f)
                                                        .roundToInt()
                                                        .coerceIn(0, (clipWpx - markerPx).roundToInt().coerceAtLeast(0))
                                                    val nearPlayhead = abs((clipStartMs + kf.timeMs) - state.playheadMs) <= 95L
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .offset { IntOffset(xPx, with(density) { 4.dp.roundToPx() }) }
                                                            .size(14.dp)
                                                            .zIndex(6f)
                                                            .clickable {
                                                                viewModel.selectClip(clip.id)
                                                                viewModel.seekPlayhead(clipStartMs + kf.timeMs)
                                                            }
                                                            .testTag("keyframe_marker_${clip.id}_${kf.id}"),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Diamond,
                                                            contentDescription = "Keyframe",
                                                            tint = if (nearPlayhead) Color.White else ProAmber,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Immediate-Claim Left & Right Trim Handles (never stolen by horizontalScroll)
                                            if (isSel) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .width(18.dp)
                                                        .fillMaxHeight()
                                                        .background(Color.White)
                                                        .zIndex(9f)
                                                        .pointerInput("video_trim_l_${clip.id}") {
                                                            awaitEachGesture {
                                                                val down = awaitFirstDown(requireUnconsumed = true)
                                                                down.consume()
                                                                var prevX = down.position.x
                                                                while (true) {
                                                                    val ev = awaitPointerEvent()
                                                                    val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                    val dragAmount = cur.position.x - prevX
                                                                    prevX = cur.position.x
                                                                    if (abs(dragAmount) > 0.25f) {
                                                                        val msPerPx = (1000f / (pxPerSecBase * density.density * currentZoom)) * currentClip.speed
                                                                        val deltaMs = (dragAmount * msPerPx).toLong()
                                                                        val nextStart = (currentClip.trimStartMs + deltaMs).coerceAtLeast(0L)
                                                                        viewModel.trimVideoClipByHandle(currentClip.id, newTrimStartMs = nextStart)
                                                                    }
                                                                    cur.consume()
                                                                }
                                                            }
                                                        }
                                                        .testTag("trim_handle_left"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height(16.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(OledBlack)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd)
                                                        .width(18.dp)
                                                        .fillMaxHeight()
                                                        .background(Color.White)
                                                        .zIndex(9f)
                                                        .pointerInput("video_trim_r_${clip.id}") {
                                                            awaitEachGesture {
                                                                val down = awaitFirstDown(requireUnconsumed = true)
                                                                down.consume()
                                                                var prevX = down.position.x
                                                                while (true) {
                                                                    val ev = awaitPointerEvent()
                                                                    val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                    val dragAmount = cur.position.x - prevX
                                                                    prevX = cur.position.x
                                                                    if (abs(dragAmount) > 0.25f) {
                                                                        val msPerPx = (1000f / (pxPerSecBase * density.density * currentZoom)) * currentClip.speed
                                                                        val deltaMs = (dragAmount * msPerPx).toLong()
                                                                        val nextEnd = (currentClip.trimEndMs + deltaMs).coerceAtLeast(300L)
                                                                        viewModel.trimVideoClipByHandle(currentClip.id, newTrimEndMs = nextEnd)
                                                                    }
                                                                    cur.consume()
                                                                }
                                                            }
                                                        }
                                                        .testTag("trim_handle_right"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height(16.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(OledBlack)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Compact "+ Add" Video Clip Tile right at the end of the filmstrip
                                Surface(
                                    onClick = {
                                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricCyanContainer,
                                    border = BorderStroke(1.dp, ElectricCyan),
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 48.dp)
                                        .testTag("video_import_clip_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Video Clip",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(halfViewportDp))
                            }

                            // Sibling Overlay Layer: Full 34dp Hit Targets Centered Over Every Clip Seam
                            var cumulativeSeamDp = 0f
                            for (idx in 0 until state.clips.lastIndex) {
                                cumulativeSeamDp += clipWidthsDp.getOrElse(idx) { 120f }
                                val clip = state.clips[idx]
                                val seamX = halfViewportDp + cumulativeSeamDp.dp
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = seamX - 17.dp)
                                        .size(width = 34.dp, height = 48.dp)
                                        .zIndex(7f)
                                        .clickable {
                                            viewModel.selectClip(clip.id)
                                            activeTab = VideoToolTab.TRANSITIONS
                                            isToolDrawerExpanded = true
                                        }
                                        .testTag("timeline_transition_badge_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = if (clip.transitionAfter != VideoTransition.NONE) Color.White else StudioSurfaceElevated,
                                        border = BorderStroke(
                                            1.dp,
                                            if (clip.transitionAfter != VideoTransition.NONE) ElectricCyan else StudioBorder
                                        ),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Waves,
                                                contentDescription = "Edit Transition",
                                                tint = if (clip.transitionAfter != VideoTransition.NONE) ElectricCyan else TextSecondary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Row 2: Full-Width Draggable & Trimmable Audio / Voiceover Waveform Track Bars
                        if (state.audioLaneClips.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(halfViewportDp))
                                Surface(
                                    onClick = {
                                        activeTab = VideoToolTab.AUDIO
                                        isToolDrawerExpanded = true
                                    },
                                    shape = RoundedCornerShape(7.dp),
                                    color = NeonViolet.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .width(totalClipsWidthDp)
                                        .height(30.dp)
                                        .testTag("video_add_audio_lane_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = "Add Audio",
                                            tint = NeonViolet,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "+ Add Music, Voiceover or SFX",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp
                                            ),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.width(halfViewportDp))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.audioLaneClips.forEach { aClip ->
                                    val isSelAudio = state.selectedAudioClipId != null && aClip.id == state.selectedAudioClipId
                                    val laneAccent = when {
                                        aClip.muted -> TextSecondary
                                        aClip.isVoiceover -> EmeraldSafe
                                        aClip.isSfx -> ProAmber
                                        else -> NeonViolet
                                    }
                                    val currentAudioClip by rememberUpdatedState(aClip)
                                    val currentZoom by rememberUpdatedState(timelineZoom)
                                    val effectiveAudioMs = (aClip.durationMs * (aClip.trimEndPct - aClip.trimStartPct))
                                        .toLong()
                                        .coerceAtLeast(350L)
                                    val xOffsetDp = ((aClip.startMs / 1000f) * pxPerSecBase * timelineZoom).dp
                                    val barWidthDp = ((effectiveAudioMs / 1000f) * pxPerSecBase * timelineZoom)
                                        .dp
                                        .coerceIn(64.dp, maxOf(totalClipsWidthDp, 560.dp))
                                    val laneTotalWidthDp = maxOf(totalClipsWidthDp, xOffsetDp + barWidthDp + 16.dp)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(Modifier.width(halfViewportDp))
                                        Box(
                                            modifier = Modifier
                                                .width(laneTotalWidthDp)
                                                .height(36.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    viewModel.selectVideoAudioClip(aClip.id)
                                                    canvasSelection = CanvasSelectionTarget.NONE
                                                    isClipQuickBarVisible = false
                                                    activeTab = VideoToolTab.AUDIO
                                                    isToolDrawerExpanded = true
                                                },
                                                shape = RoundedCornerShape(7.dp),
                                                color = laneAccent.copy(alpha = if (isSelAudio) 0.30f else 0.18f),
                                                border = BorderStroke(
                                                    width = if (isSelAudio) 2.dp else 1.dp,
                                                    color = if (isSelAudio) Color.White else laneAccent.copy(alpha = 0.85f)
                                                ),
                                                modifier = Modifier
                                                    .offset(x = xOffsetDp)
                                                    .width(barWidthDp)
                                                    .height(36.dp)
                                                    .then(
                                                        if (isSelAudio) {
                                                            Modifier.pointerInput("audio_body_drag_${aClip.id}") {
                                                                awaitEachGesture {
                                                                    val down = awaitFirstDown(requireUnconsumed = true)
                                                                    var prevX = down.position.x
                                                                    var dragging = false
                                                                    while (true) {
                                                                        val ev = awaitPointerEvent()
                                                                        val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                        val dx = cur.position.x - prevX
                                                                        if (!dragging && abs(cur.position.x - down.position.x) > viewConfiguration.touchSlop) {
                                                                            dragging = true
                                                                        }
                                                                        if (dragging && abs(dx) > 0.25f) {
                                                                            val msPerPx = 1000f / (pxPerSecBase * density.density * currentZoom)
                                                                            val deltaMs = (dx * msPerPx).toLong()
                                                                            val nextStart = (currentAudioClip.startMs + deltaMs).coerceAtLeast(0L)
                                                                            viewModel.moveVideoAudioClip(currentAudioClip.id, nextStart)
                                                                            cur.consume()
                                                                        }
                                                                        prevX = cur.position.x
                                                                    }
                                                                }
                                                            }
                                                        } else Modifier
                                                    )
                                                    .testTag("audio_waveform_bar_${aClip.id}")
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    val waveformBars = remember(aClip.id) {
                                                        val seed = aClip.id.hashCode().toFloat()
                                                        FloatArray(96) { i ->
                                                            (
                                                                abs(sin(i * 0.55f + seed)) * 0.65f +
                                                                    abs(sin(i * 1.37f + seed * 0.5f)) * 0.35f
                                                                ).coerceIn(0.16f, 1f)
                                                        }
                                                    }
                                                    Canvas(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(
                                                                horizontal = if (isSelAudio) 18.dp else 4.dp,
                                                                vertical = 4.dp
                                                            )
                                                    ) {
                                                        val barW = 2.5.dp.toPx()
                                                        val gap = 2.dp.toPx()
                                                        val step = barW + gap
                                                        val count = (size.width / step).toInt().coerceAtLeast(1)
                                                        val midY = size.height / 2f
                                                        val barColor = laneAccent.copy(alpha = 0.62f)
                                                        val corner = CornerRadius(2f, 2f)
                                                        for (i in 0 until count) {
                                                            val amp = if (aClip.muted) 0.12f else waveformBars[i % waveformBars.size]
                                                            val barH = size.height * amp
                                                            drawRoundRect(
                                                                color = barColor,
                                                                topLeft = Offset(i * step, midY - barH / 2f),
                                                                size = Size(barW, barH),
                                                                cornerRadius = corner
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = if (isSelAudio) 20.dp else 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (aClip.isVoiceover) Icons.Default.Mic else Icons.Default.MusicNote,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = aClip.name,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            ),
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = "${(aClip.volume * 100).roundToInt()}%",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            ),
                                                            color = Color.White.copy(alpha = 0.9f)
                                                        )
                                                    }

                                                    // Immediate-Claim Left & Right Drag Trim Handles on Audio Bar
                                                    if (isSelAudio) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.CenterStart)
                                                                .width(18.dp)
                                                                .fillMaxHeight()
                                                                .background(Color.White)
                                                                .zIndex(9f)
                                                                .pointerInput("audio_trim_l_${aClip.id}") {
                                                                    awaitEachGesture {
                                                                        val down = awaitFirstDown(requireUnconsumed = true)
                                                                        down.consume()
                                                                        var prevX = down.position.x
                                                                        while (true) {
                                                                            val ev = awaitPointerEvent()
                                                                            val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                            val dragAmount = cur.position.x - prevX
                                                                            prevX = cur.position.x
                                                                            if (abs(dragAmount) > 0.25f) {
                                                                                val msPerPx = 1000f / (pxPerSecBase * density.density * currentZoom)
                                                                                val deltaMs = dragAmount * msPerPx
                                                                                val durMs = currentAudioClip.durationMs.coerceAtLeast(500L).toFloat()
                                                                                val deltaPct = deltaMs / durMs
                                                                                val nextStartPct = (currentAudioClip.trimStartPct + deltaPct)
                                                                                    .coerceIn(0f, (currentAudioClip.trimEndPct - 0.06f).coerceAtLeast(0f))
                                                                                val appliedDeltaMs = ((nextStartPct - currentAudioClip.trimStartPct) * durMs).toLong()
                                                                                val nextStartMs = (currentAudioClip.startMs + appliedDeltaMs).coerceAtLeast(0L)
                                                                                viewModel.trimVideoAudioClip(
                                                                                    id = currentAudioClip.id,
                                                                                    trimStartPct = nextStartPct,
                                                                                    startMs = nextStartMs
                                                                                )
                                                                            }
                                                                            cur.consume()
                                                                        }
                                                                    }
                                                                }
                                                                .testTag("audio_trim_handle_left_${aClip.id}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(2.5.dp)
                                                                    .height(14.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(OledBlack)
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.CenterEnd)
                                                                .width(18.dp)
                                                                .fillMaxHeight()
                                                                .background(Color.White)
                                                                .zIndex(9f)
                                                                .pointerInput("audio_trim_r_${aClip.id}") {
                                                                    awaitEachGesture {
                                                                        val down = awaitFirstDown(requireUnconsumed = true)
                                                                        down.consume()
                                                                        var prevX = down.position.x
                                                                        while (true) {
                                                                            val ev = awaitPointerEvent()
                                                                            val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                            val dragAmount = cur.position.x - prevX
                                                                            prevX = cur.position.x
                                                                            if (abs(dragAmount) > 0.25f) {
                                                                                val msPerPx = 1000f / (pxPerSecBase * density.density * currentZoom)
                                                                                val deltaMs = dragAmount * msPerPx
                                                                                val durMs = currentAudioClip.durationMs.coerceAtLeast(500L).toFloat()
                                                                                val rawNextPct = currentAudioClip.trimEndPct + (deltaMs / durMs)
                                                                                if (rawNextPct > 1f) {
                                                                                    val extendedDur = (currentAudioClip.durationMs + deltaMs.toLong()).coerceIn(500L, 60_000L)
                                                                                    viewModel.trimVideoAudioClip(
                                                                                        id = currentAudioClip.id,
                                                                                        trimEndPct = 1f,
                                                                                        durationMs = extendedDur
                                                                                    )
                                                                                } else {
                                                                                    val nextEndPct = rawNextPct.coerceIn(
                                                                                        (currentAudioClip.trimStartPct + 0.06f).coerceAtMost(1f),
                                                                                        1f
                                                                                    )
                                                                                    viewModel.trimVideoAudioClip(
                                                                                        id = currentAudioClip.id,
                                                                                        trimEndPct = nextEndPct
                                                                                    )
                                                                                }
                                                                            }
                                                                            cur.consume()
                                                                        }
                                                                    }
                                                                }
                                                                .testTag("audio_trim_handle_right_${aClip.id}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(2.5.dp)
                                                                    .height(14.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(OledBlack)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        // Direct 1-tap Red Trash icon next to every Audio Track (e.g., "Crimson Synth Beat")
                                        Surface(
                                            onClick = { viewModel.deleteVideoAudioClip(aClip.id) },
                                            shape = RoundedCornerShape(7.dp),
                                            color = SignalRed.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, SignalRed.copy(alpha = 0.75f)),
                                            modifier = Modifier
                                                .size(width = 34.dp, height = 36.dp)
                                                .testTag("timeline_audio_delete_${aClip.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete ${aClip.name}",
                                                    tint = SignalRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            onClick = {
                                                activeTab = VideoToolTab.AUDIO
                                                isToolDrawerExpanded = true
                                            },
                                            shape = RoundedCornerShape(7.dp),
                                            color = NeonViolet.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, NeonViolet),
                                            modifier = Modifier
                                                .size(width = 34.dp, height = 36.dp)
                                                .testTag("video_add_audio_lane_button")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Audio",
                                                    tint = NeonViolet,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(halfViewportDp))
                                    }
                                }
                            }
                        }

                        // Row 3: Draggable & Trimmable Text and Sticker Timeline Tracks + 1-Tap Red Trash Delete
                        if (state.textLayers.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                state.textLayers.forEach { tLayer ->
                                    val isSelTxt = state.selectedTextId == tLayer.id
                                    val currentLayer by rememberUpdatedState(tLayer)
                                    val currentZoom by rememberUpdatedState(timelineZoom)
                                    val layerAccent = if (tLayer.isSticker) ProAmber else ElectricCyan
                                    val txtOffsetDp = ((tLayer.startMs / 1000f) * pxPerSecBase * timelineZoom).dp
                                    val txtDurMs = (tLayer.endMs - tLayer.startMs).coerceAtLeast(300L)
                                    val txtWidthDp = ((txtDurMs / 1000f) * pxPerSecBase * timelineZoom)
                                        .dp
                                        .coerceIn(78.dp, maxOf(totalClipsWidthDp, 560.dp))
                                    val laneTotalW = maxOf(totalClipsWidthDp, txtOffsetDp + txtWidthDp + 16.dp)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(Modifier.width(halfViewportDp))
                                        Box(
                                            modifier = Modifier
                                                .width(laneTotalW)
                                                .height(34.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    viewModel.selectVideoText(tLayer.id)
                                                    isClipQuickBarVisible = false
                                                    isStickerCropping = false
                                                    canvasSelection = if (tLayer.isSticker) {
                                                        CanvasSelectionTarget.STICKER
                                                    } else {
                                                        CanvasSelectionTarget.TEXT
                                                    }
                                                    activeTab = if (tLayer.isSticker) VideoToolTab.STICKER else VideoToolTab.TEXT
                                                    isToolDrawerExpanded = true
                                                },
                                                shape = RoundedCornerShape(7.dp),
                                                color = layerAccent.copy(alpha = if (isSelTxt) 0.28f else 0.15f),
                                                border = BorderStroke(
                                                    width = if (isSelTxt) 2.dp else 1.dp,
                                                    color = if (isSelTxt) Color.White else layerAccent.copy(alpha = 0.75f)
                                                ),
                                                modifier = Modifier
                                                    .offset(x = txtOffsetDp)
                                                    .width(txtWidthDp)
                                                    .height(34.dp)
                                                    .then(
                                                        if (isSelTxt) {
                                                            Modifier.pointerInput("overlay_body_drag_${tLayer.id}") {
                                                                awaitEachGesture {
                                                                    val down = awaitFirstDown(requireUnconsumed = true)
                                                                    var prevX = down.position.x
                                                                    var dragging = false
                                                                    while (true) {
                                                                        val ev = awaitPointerEvent()
                                                                        val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                        val dx = cur.position.x - prevX
                                                                        if (!dragging && abs(cur.position.x - down.position.x) > viewConfiguration.touchSlop) {
                                                                            dragging = true
                                                                        }
                                                                        if (dragging && abs(dx) > 0.25f) {
                                                                            val msPerPx = 1000f / (pxPerSecBase * density.density * currentZoom)
                                                                            val deltaMs = (dx * msPerPx).toLong()
                                                                            val nextStart = (currentLayer.startMs + deltaMs).coerceAtLeast(0L)
                                                                            viewModel.moveVideoTextOverlay(currentLayer.id, nextStart)
                                                                            cur.consume()
                                                                        }
                                                                        prevX = cur.position.x
                                                                    }
                                                                }
                                                            }
                                                        } else Modifier
                                                    )
                                                    .testTag("timeline_text_bar_${tLayer.id}")
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = if (isSelTxt) 20.dp else 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (tLayer.isSticker) Icons.Default.Photo else Icons.Default.Title,
                                                            contentDescription = null,
                                                            tint = layerAccent,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = tLayer.text,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            ),
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = String.format(Locale.US, "%.1fs", txtDurMs / 1000f),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                            color = Color.White.copy(alpha = 0.85f)
                                                        )
                                                    }

                                                    // Immediate-Claim Left & Right White Trim Handles for Text & Stickers
                                                    if (isSelTxt) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.CenterStart)
                                                                .width(18.dp)
                                                                .fillMaxHeight()
                                                                .background(Color.White)
                                                                .zIndex(9f)
                                                                .pointerInput("overlay_trim_l_${tLayer.id}") {
                                                                    awaitEachGesture {
                                                                        val down = awaitFirstDown(requireUnconsumed = true)
                                                                        down.consume()
                                                                        var prevX = down.position.x
                                                                        while (true) {
                                                                            val ev = awaitPointerEvent()
                                                                            val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                            val dx = cur.position.x - prevX
                                                                            prevX = cur.position.x
                                                                            if (abs(dx) > 0.25f) {
                                                                                val msPerPx = 1000f / (pxPerSecBase * density.density * currentZoom)
                                                                                val deltaMs = (dx * msPerPx).toLong()
                                                                                val nextStart = (currentLayer.startMs + deltaMs).coerceAtLeast(0L)
                                                                                viewModel.trimVideoTextOverlay(currentLayer.id, newStartMs = nextStart)
                                                                            }
                                                                            cur.consume()
                                                                        }
                                                                    }
                                                                }
                                                                .testTag("overlay_trim_handle_left_${tLayer.id}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(2.5.dp)
                                                                    .height(14.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(OledBlack)
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.CenterEnd)
                                                                .width(18.dp)
                                                                .fillMaxHeight()
                                                                .background(Color.White)
                                                                .zIndex(9f)
                                                                .pointerInput("overlay_trim_r_${tLayer.id}") {
                                                                    awaitEachGesture {
                                                                        val down = awaitFirstDown(requireUnconsumed = true)
                                                                        down.consume()
                                                                        var prevX = down.position.x
                                                                        while (true) {
                                                                            val ev = awaitPointerEvent()
                                                                            val cur = ev.changes.firstOrNull { it.pressed } ?: break
                                                                            val dx = cur.position.x - prevX
                                                                            prevX = cur.position.x
                                                                            if (abs(dx) > 0.25f) {
                                                                                val msPerPx = 1000f / (pxPerSecBase * density.density * currentZoom)
                                                                                val deltaMs = (dx * msPerPx).toLong()
                                                                                val nextEnd = (currentLayer.endMs + deltaMs).coerceAtLeast(300L)
                                                                                viewModel.trimVideoTextOverlay(currentLayer.id, newEndMs = nextEnd)
                                                                            }
                                                                            cur.consume()
                                                                        }
                                                                    }
                                                                }
                                                                .testTag("overlay_trim_handle_right_${tLayer.id}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(2.5.dp)
                                                                    .height(14.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(OledBlack)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            onClick = {
                                                if (state.selectedTextId == tLayer.id) {
                                                    canvasSelection = CanvasSelectionTarget.NONE
                                                    isStickerCropping = false
                                                }
                                                viewModel.deleteVideoText(tLayer.id)
                                            },
                                            shape = RoundedCornerShape(7.dp),
                                            color = SignalRed.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, SignalRed.copy(alpha = 0.75f)),
                                            modifier = Modifier
                                                .size(width = 34.dp, height = 34.dp)
                                                .testTag("timeline_text_delete_${tLayer.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete ${tLayer.text}",
                                                    tint = SignalRed,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(halfViewportDp))
                                    }
                                }
                            }
                        }
                        }
                    }

                    // Single Fixed Vertical White Playhead Line in the Exact Center of the Timeline
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .matchParentSize()
                            .zIndex(10f)
                            .testTag("fixed_center_playhead"),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }

                    // Subtle Zoom Reset Badge floating at top-right only while zoomed
                    if (abs(timelineZoom - 1.0f) > 0.05f) {
                        Surface(
                            onClick = { timelineZoom = 1.0f },
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricCyanContainer.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, ElectricCyan),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .zIndex(11f)
                                .testTag("timeline_zoom_reset_badge")
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fx ↺", timelineZoom),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Collapsible Compact Tool Drawer (sits cleanly between middle timeline & pinned bottom tool bar)
                if (isToolDrawerExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.60f)
                            .background(StudioSurface)
                    ) {
                        // Slim Collapsible Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tabs.firstOrNull { it.id == activeTab }?.label ?: "Tools",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ElectricCyan
                            )
                            Surface(
                                onClick = { isToolDrawerExpanded = false },
                                shape = RoundedCornerShape(50),
                                color = StudioSurface,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.testTag("video_drawer_collapse_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Hide",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = TextSecondary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse Tool Drawer",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Scrollable Single-Focus Control Tray
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (activeTab) {
                        VideoToolTab.NONE, VideoToolTab.SPLIT, VideoToolTab.DELETE -> Unit
                        VideoToolTab.TRIM -> {
                            val clip = state.selectedClip
                            // Primary Trim & Split Toolbar: always-visible Cut, Split, Trim Left, Trim Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill(
                                    label = "Cut",
                                    icon = Icons.Default.Delete,
                                    accent = SignalRed,
                                    onClick = { viewModel.deleteSelectedClip() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("trim_action_cut")
                                )
                                StudioActionPill(
                                    label = "Split",
                                    icon = Icons.Default.ContentCut,
                                    onClick = { viewModel.splitSelectedClipAtPlayhead() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("trim_action_split")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill(
                                    label = "Trim Left",
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    onClick = { viewModel.trimClipLeftToPlayhead() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("trim_action_trim_left")
                                )
                                StudioActionPill(
                                    label = "Trim Right",
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    onClick = { viewModel.trimClipRightToPlayhead() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("trim_action_trim_right")
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill("Copy", Icons.Default.ContentCopy, { viewModel.duplicateSelectedClip() })
                                if (clip != null) {
                                    StudioActionPill("Move ◀", Icons.AutoMirrored.Filled.ArrowBack, { viewModel.reorderClip(clip.id, true) })
                                    StudioActionPill("Move ▶", Icons.AutoMirrored.Filled.ArrowForward, { viewModel.reorderClip(clip.id, false) })
                                }
                            }

                            if (clip != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    VideoTrimControl.entries.forEach { mode ->
                                        StudioChipButton(
                                            text = mode.label,
                                            selected = trimControl == mode,
                                            onClick = { trimControl = mode }
                                        )
                                    }
                                }
                                when (trimControl) {
                                    VideoTrimControl.START -> StudioSliderRow(
                                        label = "Trim Left",
                                        value = clip.trimStartMs.toFloat(),
                                        valueRange = 0f..(clip.trimEndMs - 300L).coerceAtLeast(100L).toFloat(),
                                        displayValue = formatTimecode(clip.trimStartMs)
                                    ) { v -> viewModel.updateSelectedClip { it.copy(trimStartMs = v.toLong()) } }

                                    VideoTrimControl.END -> StudioSliderRow(
                                        label = "Trim Right",
                                        value = clip.trimEndMs.toFloat(),
                                        valueRange = (clip.trimStartMs + 300L).toFloat()..clip.durationMs.coerceAtLeast(clip.trimStartMs + 400L).toFloat(),
                                        displayValue = formatTimecode(clip.trimEndMs)
                                    ) { v -> viewModel.updateSelectedClip { it.copy(trimEndMs = v.toLong()) } }
                                }
                            }
                        }

                        VideoToolTab.AUDIO -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill(
                                    label = "Import Music",
                                    icon = Icons.Default.MusicNote,
                                    onClick = { audioLanePicker.launch(arrayOf("audio/*")) }
                                )
                                StudioActionPill(
                                    label = if (state.isRecordingVideoVoiceover) "Stop Voice" else "Record Voice",
                                    icon = if (state.isRecordingVideoVoiceover) Icons.Default.Stop else Icons.Default.Mic,
                                    accent = if (state.isRecordingVideoVoiceover) SignalRed else EmeraldSafe,
                                    onClick = {
                                        if (state.isRecordingVideoVoiceover) {
                                            viewModel.stopVideoVoiceoverRecording()
                                        } else {
                                            val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (ok) viewModel.startVideoVoiceoverRecording()
                                            else videoMicPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                )
                                StudioActionPill(
                                    label = "Extract Video",
                                    icon = Icons.Default.VideoFile,
                                    accent = NeonViolet,
                                    onClick = { viewModel.extractAudioFromSelectedVideoClip() }
                                )
                            }

                            // Built-in SFX quick add row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SfxKind.entries.forEach { sfx ->
                                    StudioChipButton(
                                        text = "+ ${sfx.label}",
                                        selected = false,
                                        onClick = { viewModel.addSfxToVideoLane(sfx) }
                                    )
                                }
                            }

                            val selAudio = state.selectedAudioClip
                            if (selAudio != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioActionPill("Preview", Icons.Default.PlayArrow, { viewModel.playSelectedVideoAudioClip() })
                                    StudioActionPill("Split", Icons.Default.ContentCut, { viewModel.splitSelectedVideoAudioClip() })
                                    StudioActionPill(
                                        label = if (selAudio.muted) "Unmute" else "Mute",
                                        icon = if (selAudio.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        accent = if (selAudio.muted) SignalRed else ElectricCyan,
                                        onClick = { viewModel.updateSelectedVideoAudioClip { it.copy(muted = !it.muted) } }
                                    )
                                    StudioActionPill("Delete", Icons.Default.Delete, { viewModel.deleteSelectedVideoAudioClip() }, accent = SignalRed)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VideoAudioSlider.entries.forEach { m ->
                                        StudioChipButton(text = m.label, selected = audioSlider == m, onClick = { audioSlider = m })
                                    }
                                }

                                when (audioSlider) {
                                    VideoAudioSlider.VOLUME -> StudioSliderRow("Volume (0-200%)", selAudio.volume, 0f..2f, "${(selAudio.volume * 100).roundToInt()}%") { v ->
                                        viewModel.updateSelectedVideoAudioClip { it.copy(volume = v) }
                                    }
                                    VideoAudioSlider.FADE_IN -> StudioSliderRow("Fade In", selAudio.fadeInSec, 0f..3f, String.format(Locale.US, "%.1fs", selAudio.fadeInSec)) { f ->
                                        viewModel.updateSelectedVideoAudioClip { it.copy(fadeInSec = f) }
                                    }
                                    VideoAudioSlider.FADE_OUT -> StudioSliderRow("Fade Out", selAudio.fadeOutSec, 0f..3f, String.format(Locale.US, "%.1fs", selAudio.fadeOutSec)) { f ->
                                        viewModel.updateSelectedVideoAudioClip { it.copy(fadeOutSec = f) }
                                    }
                                    VideoAudioSlider.TRIM_START -> StudioSliderRow("Trim Start", selAudio.trimStartPct, 0f..0.85f, "${(selAudio.trimStartPct * 100).roundToInt()}%") { s ->
                                        viewModel.updateSelectedVideoAudioClip { it.copy(trimStartPct = s.coerceAtMost(it.trimEndPct - 0.1f)) }
                                    }
                                    VideoAudioSlider.TRIM_END -> StudioSliderRow("Trim End", selAudio.trimEndPct, 0.15f..1f, "${(selAudio.trimEndPct * 100).roundToInt()}%") { e ->
                                        viewModel.updateSelectedVideoAudioClip { it.copy(trimEndPct = e.coerceAtLeast(it.trimStartPct + 0.1f)) }
                                    }
                                }
                            }
                        }

                        VideoToolTab.SPEED -> {
                            val clip = state.selectedClip
                            if (clip != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(0.1f, 0.5f, 1.0f, 1.5f, 2.0f, 5.0f, 10.0f).forEach { spd ->
                                        StudioChipButton(
                                            text = "${spd}x",
                                            selected = abs(clip.speed - spd) < 0.05f,
                                            onClick = { viewModel.updateSelectedClip { it.copy(speed = spd) } }
                                        )
                                    }
                                }
                                StudioSliderRow(
                                    label = "Normal Speed (0.1x–10x)",
                                    value = clip.speed,
                                    valueRange = 0.1f..10.0f,
                                    displayValue = String.format(Locale.US, "%.1fx", clip.speed)
                                ) { s -> viewModel.updateSelectedClip { it.copy(speed = s) } }

                                Text("Speed Curve Ramping", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SpeedCurvePreset.entries.forEach { curve ->
                                        StudioChipButton(
                                            text = curve.label,
                                            selected = clip.speedCurve == curve,
                                            onClick = { viewModel.setSelectedClipSpeedCurve(curve) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reverse Playback", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Switch(
                                        checked = clip.reversed,
                                        onCheckedChange = { r -> viewModel.updateSelectedClip { it.copy(reversed = r) } },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricCyan)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Video Stabilization", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Switch(
                                        checked = clip.stabilizeEnabled,
                                        onCheckedChange = { viewModel.toggleSelectedClipStabilize() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSafe)
                                    )
                                }

                                StudioSliderRow(
                                    label = "Motion Blur",
                                    value = clip.motionBlur,
                                    valueRange = 0f..1f,
                                    displayValue = "${(clip.motionBlur * 100).roundToInt()}%"
                                ) { viewModel.setSelectedClipMotionBlur(it) }
                            }
                        }

                        VideoToolTab.TRANSITIONS -> {
                            val clip = state.selectedClip
                            if (clip != null) {
                                TransitionPickerPanel(
                                    selectedTransition = clip.transitionAfter,
                                    durationMs = clip.transitionDurationMs,
                                    onSelectTransition = { tr ->
                                        viewModel.selectClipTransition(tr)
                                    },
                                    onDurationChange = { durMs ->
                                        viewModel.updateSelectedClip { it.copy(transitionDurationMs = durMs) }
                                    }
                                )
                            }
                        }

                        VideoToolTab.FILTER -> {
                            val g = state.colorGrade
                            val categoryFilters = remember(activeFilterCategory) {
                                LutPreset.entries.filter { it.category == activeFilterCategory }
                            }

                            // 6 Filter Category Tabs (Cinematic, Cyber & Neon, Moody Dark, Vintage Film, Vivid & Anime, B&W Art)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterCategory.entries.forEach { cat ->
                                    StudioChipButton(
                                        text = cat.label,
                                        selected = activeFilterCategory == cat,
                                        onClick = { activeFilterCategory = cat },
                                        modifier = Modifier.testTag("filter_cat_${cat.name.lowercase()}")
                                    )
                                }
                                if (g.lut != LutPreset.ORIGINAL) {
                                    StudioChipButton(
                                        text = "Reset",
                                        selected = false,
                                        onClick = {
                                            activeFilterCategory = FilterCategory.CINEMATIC
                                            viewModel.updateVideoColorGrade {
                                                it.copy(lut = LutPreset.ORIGINAL, filterIntensity = 1.0f)
                                            }
                                        }
                                    )
                                }
                            }

                            // Visual Swatch Cards for 32 Categorized Video Filters
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryFilters.forEach { lut ->
                                    val isSelected = g.lut == lut
                                    Surface(
                                        onClick = {
                                            viewModel.updateVideoColorGrade { it.copy(lut = lut) }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) ElectricCyanContainer else StudioSurfaceElevated,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.8.dp else 1.dp,
                                            color = if (isSelected) ElectricCyan else StudioBorder
                                        ),
                                        modifier = Modifier
                                            .width(78.dp)
                                            .testTag("filter_card_${lut.name.lowercase()}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(5.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(lut.swatchPrimaryHex),
                                                                Color(lut.swatchSecondaryHex)
                                                            )
                                                        )
                                                    )
                                            )
                                            Text(
                                                text = lut.label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) ElectricCyan else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Filter Intensity Slider (0% - 100%)
                            StudioSliderRow(
                                label = "Filter Intensity",
                                value = g.filterIntensity,
                                valueRange = 0f..1f,
                                displayValue = "${(g.filterIntensity * 100).roundToInt()}%",
                                modifier = Modifier.testTag("filter_intensity_slider")
                            ) { intensity ->
                                viewModel.updateVideoColorGrade { it.copy(filterIntensity = intensity) }
                            }
                        }

                        VideoToolTab.ADJUST -> {
                            val g = state.colorGrade
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VideoAdjustControl.entries.forEach { ctrl ->
                                        StudioChipButton(
                                            text = ctrl.label,
                                            selected = adjustControl == ctrl,
                                            onClick = { adjustControl = ctrl }
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.resetVideoColorGrade() },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Adjust", tint = TextSecondary)
                                }
                            }

                            when (adjustControl) {
                                VideoAdjustControl.BRIGHTNESS -> StudioSliderRow("Brightness", g.brightness, -0.8f..0.8f, "${(g.brightness * 100).roundToInt()}") { v -> viewModel.updateVideoColorGrade { it.copy(brightness = v) } }
                                VideoAdjustControl.CONTRAST -> StudioSliderRow("Contrast", g.contrast, 0.4f..1.9f, "${(g.contrast * 100).roundToInt()}%") { v -> viewModel.updateVideoColorGrade { it.copy(contrast = v) } }
                                VideoAdjustControl.SATURATION -> StudioSliderRow("Color", g.saturation, 0f..2.2f, "${(g.saturation * 100).roundToInt()}%") { v -> viewModel.updateVideoColorGrade { it.copy(saturation = v) } }
                                VideoAdjustControl.EXPOSURE -> StudioSliderRow("Exposure", g.exposure, -0.8f..0.8f, "${(g.exposure * 100).roundToInt()}") { v -> viewModel.updateVideoColorGrade { it.copy(exposure = v) } }
                                VideoAdjustControl.WARMTH -> StudioSliderRow("Warmth", g.temperature, -1f..1f, "${(g.temperature * 100).roundToInt()}") { v -> viewModel.updateVideoColorGrade { it.copy(temperature = v) } }
                                VideoAdjustControl.SHARPEN -> StudioSliderRow("Sharp", g.sharpen, 0f..1f, "${(g.sharpen * 100).roundToInt()}%") { v -> viewModel.updateVideoColorGrade { it.copy(sharpen = v) } }
                                VideoAdjustControl.BLUR -> StudioSliderRow("Blur", g.blur, 0f..1f, "${(g.blur * 100).roundToInt()}%") { v -> viewModel.updateVideoColorGrade { it.copy(blur = v) } }
                                VideoAdjustControl.VIGNETTE -> StudioSliderRow("Vignette", g.vignette, 0f..1f, "${(g.vignette * 100).roundToInt()}%") { v -> viewModel.updateVideoColorGrade { it.copy(vignette = v) } }
                                VideoAdjustControl.HSL_HUE -> StudioSliderRow("HSL Hue", g.hslHue, -180f..180f, "${g.hslHue.roundToInt()}°") { v -> viewModel.updateVideoColorGrade { it.copy(hslHue = v) } }
                                VideoAdjustControl.HSL_SAT -> StudioSliderRow("HSL Saturation", g.hslSat, -1f..1f, "${(g.hslSat * 100).roundToInt()}") { v -> viewModel.updateVideoColorGrade { it.copy(hslSat = v) } }
                                VideoAdjustControl.HSL_LIGHT -> StudioSliderRow("HSL Lightness", g.hslLight, -0.5f..0.5f, "${(g.hslLight * 100).roundToInt()}") { v -> viewModel.updateVideoColorGrade { it.copy(hslLight = v) } }
                            }
                        }

                        VideoToolTab.ENHANCE -> {
                            val enhanced = state.colorGrade.qualityEnhancerEnabled
                            Button(
                                onClick = { viewModel.autoColorEnhanceVideo() },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("ai_magic_quick_enhance_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("AI Magic Quick Enhance", fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill(
                                    label = "Auto-Color",
                                    icon = Icons.Default.Tune,
                                    accent = ProAmber,
                                    onClick = { viewModel.autoColorEnhanceVideo() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_magic_auto_color_button")
                                )
                                StudioActionPill(
                                    label = if (state.isListeningCaptions) "Listening..." else "Auto-Captions",
                                    icon = Icons.Default.ClosedCaption,
                                    accent = NeonViolet,
                                    onClick = {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to generate Auto-Captions")
                                        }
                                        runCatching {
                                            viewModel.setListeningCaptions(true)
                                            speechCaptionLauncher.launch(intent)
                                        }.onFailure {
                                            viewModel.setListeningCaptions(false)
                                            viewModel.addVideoText("Tap to edit caption", isCaption = true)
                                            viewModel.showToast("Caption added — tap to edit")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_magic_auto_captions_button")
                                )
                            }

                            Surface(
                                onClick = { viewModel.toggleVideoQualityEnhancer() },
                                shape = RoundedCornerShape(14.dp),
                                color = if (enhanced) ElectricCyanContainer else StudioSurfaceElevated,
                                border = BorderStroke(if (enhanced) 1.5.dp else 1.dp, if (enhanced) ElectricCyan else StudioBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("video_quality_enhancer_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = "Enhance Video",
                                            tint = if (enhanced) ElectricCyan else TextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "HD / 4K Clarity Boost",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (enhanced) ElectricCyan else TextPrimary
                                            )
                                            Text(
                                                text = "Crisper detail and cleaner contrast",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = enhanced,
                                        onCheckedChange = { viewModel.toggleVideoQualityEnhancer() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricCyan)
                                    )
                                }
                            }
                        }

                        VideoToolTab.FRAME -> {
                            val clip = state.selectedClip
                            // Aspect Ratios
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VideoAspectPreset.entries.forEach { preset ->
                                    StudioChipButton(
                                        text = "${preset.label} ${preset.tag}",
                                        selected = state.aspectPreset == preset,
                                        onClick = { viewModel.setVideoAspect(preset) }
                                    )
                                }
                            }

                            // Rotate 90°, Flip H, Flip V, Mirror
                            if (clip != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioActionPill("Rotate 90°", Icons.AutoMirrored.Filled.RotateRight, { viewModel.rotateSelectedClip90() })
                                    StudioActionPill("Flip H", Icons.Default.Flip, { viewModel.toggleSelectedClipFlipH() }, accent = if (clip.flipH) EmeraldSafe else ElectricCyan)
                                    StudioActionPill("Flip V", Icons.Default.Flip, { viewModel.toggleSelectedClipFlipV() }, accent = if (clip.flipV) EmeraldSafe else ElectricCyan)
                                    StudioActionPill("Mirror", Icons.Default.AspectRatio, { viewModel.toggleSelectedClipMirror() }, accent = if (clip.mirrorMode) ProAmber else NeonViolet)
                                }
                            }

                            // Background Fill: Blur / Color / Photo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VideoBgFillMode.entries.forEach { mode ->
                                    StudioChipButton(
                                        text = "BG: ${mode.label}",
                                        selected = state.bgFillMode == mode,
                                        onClick = {
                                            if (mode == VideoBgFillMode.IMAGE) {
                                                bgImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            } else {
                                                viewModel.setVideoBgFillMode(mode)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (state.bgFillMode == VideoBgFillMode.COLOR) {
                                StudioColorSwatchRow(state.bgFillColorArgb, { c -> viewModel.setVideoBgFillColor(c) })
                            }

                            // Zoom / Pan / Free Rotate single slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VideoFrameControl.entries.forEach { ctrl ->
                                    StudioChipButton(text = ctrl.label, selected = frameControl == ctrl, onClick = { frameControl = ctrl })
                                }
                            }
                            when (frameControl) {
                                VideoFrameControl.ZOOM -> StudioSliderRow("Zoom", state.cropZoom, 0.25f..3.0f, "${(state.cropZoom * 100).roundToInt()}%") { viewModel.updateVideoFraming(zoom = it) }
                                VideoFrameControl.PAN_X -> StudioSliderRow("Left / Right", state.panX, -1f..1f, "${(state.panX * 100).roundToInt()}%") { viewModel.updateVideoFraming(panX = it) }
                                VideoFrameControl.PAN_Y -> StudioSliderRow("Up / Down", state.panY, -1f..1f, "${(state.panY * 100).roundToInt()}%") { viewModel.updateVideoFraming(panY = it) }
                                VideoFrameControl.ROTATE -> if (clip != null) {
                                    StudioSliderRow("Free Rotate", clip.rotationDeg, -180f..180f, "${clip.rotationDeg.roundToInt()}°") { deg ->
                                        viewModel.updateSelectedClip { it.copy(rotationDeg = deg) }
                                    }
                                }
                            }
                        }

                        VideoToolTab.OVERLAY -> {
                            val pip = state.pip
                            if (!hasActiveUserPip) {
                                // Clean empty state: overlays are strictly user-added
                                Surface(
                                    color = StudioSurfaceElevated,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, StudioBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "No Overlay Added",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Add a photo, logo, or green-screen sticker on top of your video.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        StudioActionPill(
                                            label = "+ Add Overlay",
                                            icon = Icons.Default.Add,
                                            accent = ElectricCyan,
                                            onClick = {
                                                pipImagePicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.testTag("video_add_overlay_button")
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StudioActionPill(
                                        label = "Replace Photo",
                                        icon = Icons.Default.Layers,
                                        onClick = {
                                            pipImagePicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.testTag("video_add_overlay_button")
                                    )
                                    StudioActionPill(
                                        label = "Remove",
                                        icon = Icons.Default.Close,
                                        accent = SignalRed,
                                        onClick = { viewModel.removePipOverlay() },
                                        modifier = Modifier.testTag("video_remove_overlay_button")
                                    )
                                }

                                // Blend Modes
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BlendModeKind.entries.forEach { bm ->
                                        StudioChipButton(
                                            text = bm.label,
                                            selected = pip.blendMode == bm,
                                            onClick = { viewModel.updatePip { it.copy(blendMode = bm) } }
                                        )
                                    }
                                }

                                // Shape Masking
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MaskShapeKind.entries.forEach { mk ->
                                        StudioChipButton(
                                            text = "Mask: ${mk.label}",
                                            selected = pip.maskShape == mk,
                                            onClick = { viewModel.updatePip { it.copy(maskShape = mk) } }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Green Screen Cutout", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Switch(
                                        checked = pip.chromaEnabled,
                                        onCheckedChange = { ch -> viewModel.updatePip { it.copy(chromaEnabled = ch) } },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSafe)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VideoOverlayControl.entries.forEach { ctrl ->
                                        if ((ctrl == VideoOverlayControl.CLEAN || ctrl == VideoOverlayControl.SPILL) && !pip.chromaEnabled) return@forEach
                                        StudioChipButton(
                                            text = ctrl.label,
                                            selected = overlayControl == ctrl,
                                            onClick = { overlayControl = ctrl }
                                        )
                                    }
                                }

                                when (overlayControl) {
                                    VideoOverlayControl.SIZE -> StudioSliderRow("Size", pip.scale, 0.15f..0.95f, "${(pip.scale * 100).roundToInt()}%") { s ->
                                        viewModel.updatePip { it.copy(scale = s) }
                                    }
                                    VideoOverlayControl.ROTATE -> StudioSliderRow("Rotate", pip.rotationDeg, -180f..180f, "${pip.rotationDeg.roundToInt()}°") { r ->
                                        viewModel.updatePip { it.copy(rotationDeg = r) }
                                    }
                                    VideoOverlayControl.OPACITY -> StudioSliderRow("Opacity", pip.opacity, 0.1f..1f, "${(pip.opacity * 100).roundToInt()}%") { o ->
                                        viewModel.updatePip { it.copy(opacity = o) }
                                    }
                                    VideoOverlayControl.CLEAN -> if (pip.chromaEnabled) {
                                        StudioColorSwatchRow(pip.chromaKeyColor, { c -> viewModel.updatePip { it.copy(chromaKeyColor = c) } })
                                        StudioSliderRow("Green Edge", pip.chromaTolerance, 0.08f..0.85f, "${(pip.chromaTolerance * 100).roundToInt()}%") { t ->
                                            viewModel.updatePip { it.copy(chromaTolerance = t) }
                                        }
                                    }
                                    VideoOverlayControl.SPILL -> if (pip.chromaEnabled) {
                                        StudioSliderRow("Spill Suppression", pip.spillSuppression, 0f..1f, "${(pip.spillSuppression * 100).roundToInt()}%") { sp ->
                                            viewModel.updatePip { it.copy(spillSuppression = sp) }
                                        }
                                    }
                                }
                            }
                        }

                        VideoToolTab.KEYFRAME -> {
                            val clip = state.selectedClip
                            if (clip != null) {
                                val localMs = viewModel.getSelectedClipLocalTimeMs()
                                val interp = VideoMediaEngine.interpolateKeyframes(clip.keyframes, localMs)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioActionPill(
                                        label = "+ Add Keyframe ◆",
                                        icon = Icons.Default.Diamond,
                                        accent = ProAmber,
                                        onClick = {
                                            viewModel.addOrUpdateKeyframeAtPlayhead()
                                            viewModel.showToast("Keyframe set at ${formatSimpleTime(localMs)}")
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (clip.keyframes.isNotEmpty()) {
                                        StudioActionPill(
                                            label = "Clear",
                                            icon = Icons.Default.Delete,
                                            accent = SignalRed,
                                            onClick = { viewModel.clearKeyframesOnSelectedClip() }
                                        )
                                    }
                                }

                                if (clip.keyframes.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        clip.keyframes.forEachIndexed { idx, kf ->
                                            Surface(
                                                onClick = {
                                                    var clipOffset = 0L
                                                    for (c in state.clips) {
                                                        if (c.id == clip.id) break
                                                        clipOffset += c.effectiveDurationMs
                                                    }
                                                    viewModel.seekPlayhead(clipOffset + kf.timeMs)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = ProAmberContainer,
                                                border = BorderStroke(1.dp, ProAmber)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.Diamond, contentDescription = null, tint = ProAmber, modifier = Modifier.size(12.dp))
                                                    Text(
                                                        text = "KF${idx + 1} ${String.format(Locale.US, "%.1fs", kf.timeMs / 1000f)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextPrimary
                                                    )
                                                    IconButton(
                                                        onClick = { viewModel.deleteKeyframe(kf.id) },
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Keyframe", tint = SignalRed, modifier = Modifier.size(13.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VideoKeyframeControl.entries.forEach { ctrl ->
                                        StudioChipButton(text = ctrl.label, selected = keyframeControl == ctrl, onClick = { keyframeControl = ctrl })
                                    }
                                }

                                when (keyframeControl) {
                                    VideoKeyframeControl.SCALE -> StudioSliderRow("Keyframe Scale", interp.scale, 0.4f..2.2f, "${(interp.scale * 100).roundToInt()}%") { v ->
                                        viewModel.addOrUpdateKeyframeAtPlayhead(scale = v)
                                    }
                                    VideoKeyframeControl.POS_X -> StudioSliderRow("Move Left / Right", interp.xNorm, -0.6f..0.6f, "${(interp.xNorm * 100).roundToInt()}%") { v ->
                                        viewModel.addOrUpdateKeyframeAtPlayhead(xNorm = v)
                                    }
                                    VideoKeyframeControl.POS_Y -> StudioSliderRow("Move Up / Down", interp.yNorm, -0.6f..0.6f, "${(interp.yNorm * 100).roundToInt()}%") { v ->
                                        viewModel.addOrUpdateKeyframeAtPlayhead(yNorm = v)
                                    }
                                    VideoKeyframeControl.ROTATE -> StudioSliderRow("Keyframe Rotate", interp.rotationDeg, -180f..180f, "${interp.rotationDeg.roundToInt()}°") { v ->
                                        viewModel.addOrUpdateKeyframeAtPlayhead(rotationDeg = v)
                                    }
                                    VideoKeyframeControl.OPACITY -> StudioSliderRow("Keyframe Opacity", interp.opacity, 0f..1f, "${(interp.opacity * 100).roundToInt()}%") { v ->
                                        viewModel.addOrUpdateKeyframeAtPlayhead(opacity = v)
                                    }
                                }
                            }
                        }

                        VideoToolTab.TEXT -> {
                            ProTextEditingToolbar(
                                textLayers = state.textLayers,
                                selectedTextId = state.selectedTextId,
                                isListeningCaptions = state.isListeningCaptions,
                                onAddText = {
                                    viewModel.addVideoText("NEW TITLE", isCaption = false)
                                    canvasSelection = CanvasSelectionTarget.TEXT
                                },
                                onOpenStickersTab = {
                                    activeTab = VideoToolTab.STICKER
                                    isToolDrawerExpanded = true
                                },
                                onLaunchAutoCaptions = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to add captions")
                                    }
                                    runCatching {
                                        viewModel.setListeningCaptions(true)
                                        speechCaptionLauncher.launch(intent)
                                    }.onFailure {
                                        viewModel.setListeningCaptions(false)
                                        viewModel.addVideoText("Tap to edit caption", isCaption = true)
                                        canvasSelection = CanvasSelectionTarget.TEXT
                                        viewModel.showToast("Caption added — tap to edit")
                                    }
                                },
                                onSelectText = { id ->
                                    viewModel.selectVideoText(id)
                                    canvasSelection = CanvasSelectionTarget.TEXT
                                },
                                onUpdateSelectedText = { transform ->
                                    viewModel.updateSelectedVideoText(transform)
                                },
                                onDuplicateText = { id ->
                                    viewModel.duplicateVideoText(id)
                                    canvasSelection = CanvasSelectionTarget.TEXT
                                },
                                onDeleteText = { id ->
                                    canvasSelection = CanvasSelectionTarget.NONE
                                    viewModel.deleteVideoText(id)
                                }
                            )
                        }

                        VideoToolTab.STICKER -> {
                            ProStickerEditingToolbar(
                                stickerLayers = state.textLayers.filter { it.isSticker },
                                selectedTextId = state.selectedTextId,
                                isCropping = isStickerCropping,
                                onAddCatalogSticker = { item ->
                                    viewModel.addCatalogSticker(item)
                                    canvasSelection = CanvasSelectionTarget.STICKER
                                    isStickerCropping = false
                                },
                                onImportCustomImageSticker = {
                                    stickerImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onSelectSticker = { id ->
                                    viewModel.selectVideoText(id)
                                    canvasSelection = CanvasSelectionTarget.STICKER
                                    isStickerCropping = false
                                },
                                onUpdateSelectedSticker = { transform ->
                                    viewModel.updateSelectedVideoText(transform)
                                },
                                onStartCropMode = {
                                    if (selectedOverlayLayer?.isSticker == true) {
                                        canvasSelection = CanvasSelectionTarget.STICKER
                                        isStickerCropping = !isStickerCropping
                                    }
                                },
                                onDuplicateSticker = { id ->
                                    viewModel.duplicateVideoText(id)
                                    canvasSelection = CanvasSelectionTarget.STICKER
                                    isStickerCropping = false
                                },
                                onDeleteSticker = { id ->
                                    isStickerCropping = false
                                    canvasSelection = CanvasSelectionTarget.NONE
                                    viewModel.deleteVideoText(id)
                                }
                            )
                        }

                        VideoToolTab.EXPORT -> {
                            val is4KSelected = state.exportResolution == VideoExportResolution.UHD_4K
                            val is60FpsSelected = state.exportFps == VideoExportFps.FPS_60
                            val needsSafeFallback = (is4KSelected && is60FpsSelected && !encoderCapabilities.supports4K60) ||
                                (is4KSelected && !encoderCapabilities.supports4K30)

                            Text("1-Tap Export Presets", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VideoExportPreset.entries.chunked(2).forEach { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPresets.forEach { preset ->
                                            val isMatched = state.exportResolution == preset.resolution &&
                                                state.exportFps == preset.fps &&
                                                (preset.aspect == null || state.aspectPreset == preset.aspect)
                                            Surface(
                                                onClick = { viewModel.applyExportPreset(preset) },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isMatched) ElectricCyanContainer else StudioSurfaceElevated,
                                                border = BorderStroke(
                                                    width = if (isMatched) 1.5.dp else 1.dp,
                                                    color = if (isMatched) ElectricCyan else StudioBorder
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("export_preset_${preset.name.lowercase()}")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                                                ) {
                                                    Text(
                                                        text = preset.label,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isMatched) ElectricCyan else TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = preset.subtitle,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text("Quality (Zero Watermark)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VideoExportResolution.entries.forEach { res ->
                                    StudioChipButton(
                                        text = res.label,
                                        selected = state.exportResolution == res,
                                        onClick = { viewModel.setVideoExportResolution(res) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_res_${res.name.lowercase()}")
                                    )
                                }
                            }

                            Text("Motion", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VideoExportFps.entries.forEach { fpsOption ->
                                    StudioChipButton(
                                        text = "${fpsOption.label} (${fpsOption.fps})",
                                        selected = state.exportFps == fpsOption,
                                        onClick = { viewModel.setVideoExportFps(fpsOption) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_fps_${fpsOption.fps}")
                                    )
                                }
                            }

                            if (needsSafeFallback) {
                                Surface(
                                    color = ProAmberContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ProAmber),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_hardware_fallback_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Quality Notice",
                                            tint = ProAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "4K isn't supported on this phone — we'll save in Full HD Smooth so it stays fast and crisp.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ProAmber
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.exportVideo() },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("video_render_mp4_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (needsSafeFallback) {
                                        "Save Full HD Smooth Video"
                                    } else {
                                        "Save ${state.exportResolution.shortLabel} ${state.exportFps.label} Video"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        }
                        }
                    }
                }

                // =========================================================================
                // 3. SCROLLABLE BOTTOM TOOL BAR PINNED DIRECTLY ABOVE NAVIGATION BAR
                // =========================================================================
                StudioToolTabStrip(
                    tabs = tabs,
                    selected = if (isToolDrawerExpanded) activeTab else VideoToolTab.NONE,
                    onSelect = { tab ->
                        when (tab) {
                            VideoToolTab.SPLIT -> {
                                viewModel.splitSelectedClipAtPlayhead()
                                activeTab = VideoToolTab.TRIM
                                isToolDrawerExpanded = true
                            }
                            VideoToolTab.DELETE -> {
                                when {
                                    canvasSelection == CanvasSelectionTarget.PIP && hasActiveUserPip -> {
                                        canvasSelection = CanvasSelectionTarget.NONE
                                        viewModel.removePipOverlay()
                                    }
                                    state.selectedTextId != null -> {
                                        val delId = state.selectedTextId
                                        isStickerCropping = false
                                        if (canvasSelection == CanvasSelectionTarget.TEXT || canvasSelection == CanvasSelectionTarget.STICKER) {
                                            canvasSelection = CanvasSelectionTarget.NONE
                                        }
                                        viewModel.deleteVideoText(delId)
                                    }
                                    state.selectedAudioClipId != null -> {
                                        viewModel.deleteVideoAudioClip(state.selectedAudioClipId)
                                    }
                                    state.selectedClipId != null -> {
                                        viewModel.deleteVideoClipById(state.selectedClipId)
                                    }
                                    state.clips.isNotEmpty() -> {
                                        viewModel.deleteSelectedClip()
                                    }
                                    state.audioLaneClips.isNotEmpty() -> {
                                        viewModel.deleteSelectedVideoAudioClip()
                                    }
                                    state.textLayers.isNotEmpty() -> {
                                        viewModel.deleteSelectedVideoText()
                                    }
                                }
                                isClipQuickBarVisible = false
                            }
                            VideoToolTab.NONE -> Unit
                            else -> {
                                if (activeTab == tab && isToolDrawerExpanded) {
                                    isToolDrawerExpanded = false
                                } else {
                                    activeTab = tab
                                    isToolDrawerExpanded = true
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
