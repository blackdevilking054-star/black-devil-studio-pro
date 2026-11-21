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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import com.example.engine.VideoMediaEngine
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class VideoToolTab {
    TRIM, AUDIO, SPEED, TRANSITIONS, FILTER, ADJUST, ENHANCE, FRAME, OVERLAY, KEYFRAME, TEXT, EXPORT
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
    NONE, VIDEO, PIP
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val canUndoVideo by viewModel.canUndoVideo.collectAsStateWithLifecycle()
    val canRedoVideo by viewModel.canRedoVideo.collectAsStateWithLifecycle()
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

    LaunchedEffect(openEnhanceRequested) {
        if (openEnhanceRequested) {
            activeTab = VideoToolTab.ENHANCE
            isToolDrawerExpanded = true
            onConsumeEnhanceRequest()
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
            ToolTabItem(VideoToolTab.TRIM, "Trim", Icons.Default.ContentCut),
            ToolTabItem(VideoToolTab.AUDIO, "Audio", Icons.Default.MusicNote),
            ToolTabItem(VideoToolTab.SPEED, "Speed", Icons.Default.Speed),
            ToolTabItem(VideoToolTab.TRANSITIONS, "Trans", Icons.Default.Waves),
            ToolTabItem(VideoToolTab.FILTER, "Filter", Icons.Default.FilterVintage),
            ToolTabItem(VideoToolTab.ADJUST, "Adjust", Icons.Default.Tune),
            ToolTabItem(VideoToolTab.ENHANCE, "Enhance", Icons.Default.HighQuality),
            ToolTabItem(VideoToolTab.FRAME, "Transform", Icons.Default.AspectRatio),
            ToolTabItem(VideoToolTab.OVERLAY, "Overlay", Icons.Default.PictureInPictureAlt),
            ToolTabItem(VideoToolTab.KEYFRAME, "Keyframe", Icons.Default.Diamond),
            ToolTabItem(VideoToolTab.TEXT, "Text", Icons.Default.Title),
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
    val selectedClipRotDeg = state.selectedClip?.rotationDeg ?: 0f

    LaunchedEffect(hasActiveUserPip) {
        if (!hasActiveUserPip && canvasSelection == CanvasSelectionTarget.PIP) {
            canvasSelection = CanvasSelectionTarget.NONE
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // =========================================================================
        // 1. INTERACTIVE VIDEO PREVIEW CANVAS (TAP TO SELECT + WHITE RESIZE/ROTATE BOX)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isToolDrawerExpanded) 0.49f else 0.64f)
                .background(OledBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val ratio = (state.aspectPreset.ratioW.toFloat() / state.aspectPreset.ratioH.toFloat())
                    .coerceIn(0.56f, 2.25f)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepObsidian)
                        .pointerInput(hasActiveUserPip) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.coerceAtLeast(1).toFloat()
                                val h = size.height.coerceAtLeast(1).toFloat()
                                val pip = currentState.pip
                                if (hasActiveUserPip) {
                                    val pipW = (w * pip.scale).coerceIn(88f, w)
                                    val pipH = (h * pip.scale).coerceIn(72f, h)
                                    val cx = w * pip.xNorm
                                    val cy = h * pip.yNorm
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
                                canvasSelection = if (currentCanvasSelection == CanvasSelectionTarget.VIDEO) {
                                    CanvasSelectionTarget.NONE
                                } else {
                                    CanvasSelectionTarget.VIDEO
                                }
                            }
                        }
                        .pointerInput(hasActiveUserPip) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val w = size.width.coerceAtLeast(1).toFloat()
                                val h = size.height.coerceAtLeast(1).toFloat()
                                when (currentCanvasSelection) {
                                    CanvasSelectionTarget.PIP -> if (hasActiveUserPip) {
                                        val p = currentState.pip
                                        val nextRot = normalizeAngleDeg(p.rotationDeg + rotation)
                                        viewModel.updatePip {
                                            it.copy(
                                                xNorm = (it.xNorm + pan.x / w).coerceIn(0.08f, 0.92f),
                                                yNorm = (it.yNorm + pan.y / h).coerceIn(0.08f, 0.92f),
                                                scale = (it.scale * zoom).coerceIn(0.15f, 0.95f),
                                                rotationDeg = nextRot
                                            )
                                        }
                                    }
                                    CanvasSelectionTarget.VIDEO -> {
                                        val s = currentState
                                        val baseRot = s.selectedClip?.rotationDeg ?: 0f
                                        val nextRot = if (rotation != 0f) normalizeAngleDeg(baseRot + rotation) else null
                                        viewModel.updateVideoCanvasTransform(
                                            zoom = if (zoom != 1f) (s.cropZoom * zoom).coerceIn(0.4f, 3.0f) else null,
                                            panX = if (pan.x != 0f) (s.panX + pan.x / w * 1.4f).coerceIn(-1f, 1f) else null,
                                            panY = if (pan.y != 0f) (s.panY + pan.y / h * 1.4f).coerceIn(-1f, 1f) else null,
                                            rotationDeg = nextRot
                                        )
                                    }
                                    CanvasSelectionTarget.NONE -> {
                                        // Allow direct 2-finger pinch/rotate even before tapping
                                        if (zoom != 1f || rotation != 0f) {
                                            canvasSelection = CanvasSelectionTarget.VIDEO
                                            val s = currentState
                                            val baseRot = s.selectedClip?.rotationDeg ?: 0f
                                            viewModel.updateVideoCanvasTransform(
                                                zoom = (s.cropZoom * zoom).coerceIn(0.4f, 3.0f),
                                                rotationDeg = normalizeAngleDeg(baseRot + rotation)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null && !previewBitmap.isRecycled) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Video Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    val canvasW = constraints.maxWidth.coerceAtLeast(1)
                    val canvasH = constraints.maxHeight.coerceAtLeast(1)
                    val minCanvasEdge = minOf(canvasW, canvasH).coerceAtLeast(1).toFloat()

                    // VN / YouTube Create Interactive White Bounding Box for Video Frame
                    if (canvasSelection == CanvasSelectionTarget.VIDEO) {
                        val visualScale = (0.76f * state.cropZoom.coerceIn(0.45f, 1.2f)).coerceIn(0.36f, 0.86f)
                        val boxW = (canvasW * visualScale).roundToInt().coerceIn(104, canvasW - 18)
                        val boxH = (canvasH * visualScale).roundToInt().coerceIn(78, canvasH - 42)
                        val centerOffsetX = (state.panX * canvasW * 0.28f).roundToInt()
                        val centerOffsetY = (state.panY * canvasH * 0.28f).roundToInt()
                        val leftPx = ((canvasW - boxW) / 2 + centerOffsetX).coerceIn(8, (canvasW - boxW - 8).coerceAtLeast(8))
                        val topPx = ((canvasH - boxH) / 2 + centerOffsetY).coerceIn(30, (canvasH - boxH - 8).coerceAtLeast(30))

                        InteractiveWhiteTransformBox(
                            leftPx = leftPx,
                            topPx = topPx,
                            boxW = boxW,
                            boxH = boxH,
                            rotationDeg = selectedClipRotDeg,
                            showDeleteX = false,
                            onDeleteClick = {},
                            onCornerScaleDelta = { deltaNorm ->
                                val nextZoom = (currentState.cropZoom + deltaNorm * 1.35f).coerceIn(0.4f, 3.0f)
                                viewModel.updateVideoCanvasTransform(zoom = nextZoom)
                            },
                            onRotateDeltaDeg = { deltaDeg ->
                                val baseRot = currentState.selectedClip?.rotationDeg ?: 0f
                                viewModel.updateVideoCanvasTransform(
                                    rotationDeg = normalizeAngleDeg(baseRot + deltaDeg)
                                )
                            },
                            minCanvasEdge = minCanvasEdge,
                            tagPrefix = "video_canvas"
                        )
                    }

                    // Active User-Added PIP Overlay Bounding Box + Corner Handles + Top Rotate + Red "X" Delete
                    if (hasActiveUserPip) {
                        val boxW = (canvasW * state.pip.scale).roundToInt().coerceIn(88, canvasW - 12)
                        val boxH = (canvasH * state.pip.scale).roundToInt().coerceIn(72, canvasH - 36)
                        val leftPx = ((canvasW * state.pip.xNorm) - boxW / 2f)
                            .roundToInt()
                            .coerceIn(4, (canvasW - boxW - 4).coerceAtLeast(4))
                        val topPx = ((canvasH * state.pip.yNorm) - boxH / 2f)
                            .roundToInt()
                            .coerceIn(28, (canvasH - boxH - 4).coerceAtLeast(28))

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
                                minCanvasEdge = minCanvasEdge,
                                tagPrefix = "pip_canvas"
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

            // Thin YouTube-Style Progress Line right under Preview Stage
            val progressFraction = if (state.totalDurationMs > 0L) {
                (state.playheadMs.toFloat() / state.totalDurationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(StudioBorder.copy(alpha = 0.55f))
                    .testTag("video_youtube_progress_line")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(ElectricCyan)
                )
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
        // 2. FIXED CENTER WHITE PLAYHEAD TIMELINE + CONTEXTUAL QUICK-ACTION BAR + DOCK
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            border = BorderStroke(0.5.dp, StudioBorder.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isToolDrawerExpanded) 0.51f else 0.36f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Contextual Floating Quick-Action Bar (Hidden until a clip is tapped)
                AnimatedVisibility(
                    visible = isClipQuickBarVisible && state.selectedClip != null,
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
                        .then(if (!isToolDrawerExpanded) Modifier.weight(1f) else Modifier)
                        .background(DeepObsidian)
                        .pointerInput(Unit) {
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
                ) {
                    val halfViewportDp = maxWidth / 2

                    // Synchronized Horizontally Scrollable Video Filmstrip + Direct Audio Track
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(timelineScrollState)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Row 1: Video Filmstrip Clips with Compact Mini Seam Transition Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width(halfViewportDp))

                            state.clips.forEachIndexed { idx, clip ->
                                val isSel = clip.id == state.selectedClip?.id
                                val thumbs = clipThumbnails[clip.id].orEmpty()
                                val currentClip by rememberUpdatedState(clip)
                                val currentZoom by rememberUpdatedState(timelineZoom)
                                val clipWidthDp = clipWidthsDp.getOrElse(idx) { 120f }.dp

                                Surface(
                                    onClick = {
                                        if (isSel) {
                                            isClipQuickBarVisible = !isClipQuickBarVisible
                                        } else {
                                            viewModel.selectClip(clip.id)
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
                                                    start = if (isSel) 18.dp else 6.dp,
                                                    end = if (isSel) 18.dp else 6.dp,
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
                                            if (clip.keyframes.isNotEmpty()) {
                                                Icon(
                                                    imageVector = Icons.Default.Diamond,
                                                    contentDescription = "Has Keyframes",
                                                    tint = ProAmber,
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .align(Alignment.TopEnd)
                                                )
                                            }
                                        }

                                        // Direct Touch-and-Drag Left & Right Trim Handles on Selected Clip
                                        if (isSel) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .width(16.dp)
                                                    .fillMaxHeight()
                                                    .background(Color.White)
                                                    .pointerInput(clip.id) {
                                                        detectHorizontalDragGestures { change, dragAmount ->
                                                            change.consume()
                                                            val msPerPx = (1000f / (pxPerSecBase * density.density * currentZoom)) * currentClip.speed
                                                            val deltaMs = (dragAmount * msPerPx).toLong()
                                                            val maxStart = (currentClip.trimEndMs - 300L).coerceAtLeast(0L)
                                                            val nextStart = (currentClip.trimStartMs + deltaMs).coerceIn(0L, maxStart)
                                                            if (nextStart != currentClip.trimStartMs) {
                                                                viewModel.updateSelectedClip { it.copy(trimStartMs = nextStart) }
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
                                                    .width(16.dp)
                                                    .fillMaxHeight()
                                                    .background(Color.White)
                                                    .pointerInput(clip.id) {
                                                        detectHorizontalDragGestures { change, dragAmount ->
                                                            change.consume()
                                                            val msPerPx = (1000f / (pxPerSecBase * density.density * currentZoom)) * currentClip.speed
                                                            val deltaMs = (dragAmount * msPerPx).toLong()
                                                            val minEnd = (currentClip.trimStartMs + 300L).coerceAtMost(currentClip.durationMs)
                                                            val nextEnd = (currentClip.trimEndMs + deltaMs).coerceIn(minEnd, currentClip.durationMs)
                                                            if (nextEnd != currentClip.trimEndMs) {
                                                                viewModel.updateSelectedClip { it.copy(trimEndMs = nextEnd) }
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

                                // Sleek Compact Mini Transition Badge sitting neatly ON the seam between clips
                                if (idx < state.clips.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(48.dp)
                                            .zIndex(5f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            onClick = {
                                                viewModel.selectClip(clip.id)
                                                activeTab = VideoToolTab.TRANSITIONS
                                                isToolDrawerExpanded = true
                                            },
                                            shape = RoundedCornerShape(5.dp),
                                            color = if (clip.transitionAfter != VideoTransition.NONE) Color.White else StudioSurfaceElevated,
                                            border = BorderStroke(
                                                1.dp,
                                                if (clip.transitionAfter != VideoTransition.NONE) ElectricCyan else StudioBorder
                                            ),
                                            modifier = Modifier
                                                .requiredSize(18.dp)
                                                .testTag("timeline_transition_badge_$idx")
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

                            Spacer(Modifier.width(8.dp))

                            // Compact "+ Add" Video Clip Tile right at the end of the filmstrip (VN/CapCut style)
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

                        // Row 2: Direct Audio Track Lane aligned beneath the Video Filmstrip
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width(halfViewportDp))

                            Surface(
                                onClick = {
                                    activeTab = VideoToolTab.AUDIO
                                    isToolDrawerExpanded = true
                                },
                                shape = RoundedCornerShape(7.dp),
                                color = NeonViolet.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonViolet),
                                modifier = Modifier
                                    .height(25.dp)
                                    .testTag("video_add_audio_lane_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = "Add Audio", tint = NeonViolet, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "+ Add Audio",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = TextPrimary
                                    )
                                }
                            }

                            state.audioLaneClips.forEach { aClip ->
                                Spacer(Modifier.width(6.dp))
                                val isSelAudio = aClip.id == state.selectedAudioClip?.id
                                val laneAccent = when {
                                    aClip.muted -> TextSecondary
                                    aClip.isVoiceover -> EmeraldSafe
                                    aClip.isSfx -> ProAmber
                                    else -> NeonViolet
                                }
                                val audioWidthDp = ((aClip.durationMs / 1000f) * 32f * timelineZoom)
                                    .dp
                                    .coerceIn(88.dp, 360.dp)
                                Surface(
                                    onClick = {
                                        viewModel.selectVideoAudioClip(aClip.id)
                                        activeTab = VideoToolTab.AUDIO
                                        isToolDrawerExpanded = true
                                    },
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (isSelAudio) laneAccent.copy(alpha = 0.24f) else StudioSurface,
                                    border = BorderStroke(1.dp, if (isSelAudio) laneAccent else StudioBorder),
                                    modifier = Modifier
                                        .width(audioWidthDp)
                                        .height(25.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (aClip.isVoiceover) Icons.Default.Mic else Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = laneAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = aClip.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(aClip.volume * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = laneAccent
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(halfViewportDp))
                        }
                    }

                    // Single Fixed Vertical White Playhead Line in the Exact Center of the Timeline
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight()
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

                // Collapsible Compact Tool Drawer (tap any tool icon to open, tap again or chevron to collapse)
                AnimatedVisibility(
                    visible = isToolDrawerExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isToolDrawerExpanded) Modifier.weight(1f) else Modifier)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                        VideoToolTab.TRIM -> {
                            val clip = state.selectedClip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill("Split", Icons.Default.ContentCut, { viewModel.splitSelectedClipAtPlayhead() })
                                StudioActionPill("Copy", Icons.Default.ContentCopy, { viewModel.duplicateSelectedClip() })
                                if (clip != null) {
                                    StudioActionPill("Left", Icons.AutoMirrored.Filled.ArrowBack, { viewModel.reorderClip(clip.id, true) })
                                    StudioActionPill("Right", Icons.AutoMirrored.Filled.ArrowForward, { viewModel.reorderClip(clip.id, false) })
                                }
                                StudioActionPill("Delete", Icons.Default.Delete, { viewModel.deleteSelectedClip() }, accent = SignalRed)
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
                                        label = "Trim Start",
                                        value = clip.trimStartMs.toFloat(),
                                        valueRange = 0f..(clip.trimEndMs - 300L).coerceAtLeast(100L).toFloat(),
                                        displayValue = formatTimecode(clip.trimStartMs)
                                    ) { v -> viewModel.updateSelectedClip { it.copy(trimStartMs = v.toLong()) } }

                                    VideoTrimControl.END -> StudioSliderRow(
                                        label = "Trim End",
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
                                        viewModel.updateSelectedClip { it.copy(transitionAfter = tr) }
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
                            Surface(
                                onClick = { viewModel.toggleVideoQualityEnhancer() },
                                shape = RoundedCornerShape(16.dp),
                                color = if (enhanced) ElectricCyanContainer else StudioSurfaceElevated,
                                border = BorderStroke(if (enhanced) 1.5.dp else 1.dp, if (enhanced) ElectricCyan else StudioBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("video_quality_enhancer_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Enhance Video",
                                            tint = if (enhanced) ElectricCyan else TextPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Enhance Quality (HD / 4K Clarity)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (enhanced) ElectricCyan else TextPrimary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Makes your video sharper, cleaner, and clearer",
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
                                VideoFrameControl.ZOOM -> StudioSliderRow("Zoom", state.cropZoom, 0.6f..2.4f, "${(state.cropZoom * 100).roundToInt()}%") { viewModel.updateVideoFraming(zoom = it) }
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
                                    VideoKeyframeControl.OPACITY -> StudioSliderRow("Keyframe Opacity", interp.opacity, 0.1f..1f, "${(interp.opacity * 100).roundToInt()}%") { v ->
                                        viewModel.addOrUpdateKeyframeAtPlayhead(opacity = v)
                                    }
                                }
                            }
                        }

                        VideoToolTab.TEXT -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill(
                                    label = "+ Text",
                                    icon = Icons.Default.Title,
                                    onClick = { viewModel.addVideoText("NEW TITLE", isCaption = false) }
                                )
                                StudioActionPill(
                                    label = "Stickers",
                                    icon = Icons.Default.Photo,
                                    accent = ProAmber,
                                    onClick = { showStickerRow = !showStickerRow }
                                )
                                StudioActionPill(
                                    label = if (state.isListeningCaptions) "Listening..." else "Auto Captions",
                                    icon = Icons.Default.ClosedCaption,
                                    accent = NeonViolet,
                                    onClick = {
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
                                            viewModel.showToast("Caption added — tap to edit")
                                        }
                                    }
                                )
                            }

                            if (showStickerRow) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("🔥", "⚡", "👑", "🎬", "⭐", "❤️", "💯", "🚀").forEach { sym ->
                                        StudioChipButton(
                                            text = sym,
                                            selected = false,
                                            onClick = { viewModel.addVideoSticker(sym) }
                                        )
                                    }
                                }
                            }

                            // Multi-layer selector chips
                            if (state.textLayers.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.textLayers.forEach { layer ->
                                        StudioChipButton(
                                            text = layer.text.take(12),
                                            selected = layer.id == state.selectedTextId,
                                            onClick = { viewModel.selectVideoText(layer.id) }
                                        )
                                    }
                                }
                            }

                            val selText = state.textLayers.firstOrNull { it.id == state.selectedTextId }
                            if (selText != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = selText.text,
                                        onValueChange = { txt -> viewModel.updateSelectedVideoText { it.copy(text = txt) } },
                                        placeholder = { Text("Enter text") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricCyan,
                                            unfocusedBorderColor = StudioBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.deleteSelectedVideoText() }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Text", tint = SignalRed)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VideoTextControl.entries.forEach { ctrl ->
                                        StudioChipButton(
                                            text = ctrl.label,
                                            selected = textControl == ctrl,
                                            onClick = { textControl = ctrl }
                                        )
                                    }
                                }

                                when (textControl) {
                                    VideoTextControl.FONT -> Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StudioFonts.all.forEach { f ->
                                            StudioChipButton(
                                                text = f.displayName,
                                                selected = selText.fontId == f.id,
                                                onClick = { viewModel.updateSelectedVideoText { it.copy(fontId = f.id) } }
                                            )
                                        }
                                    }

                                    VideoTextControl.STYLE -> Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextAnimationStyle.entries.forEach { anim ->
                                            StudioChipButton(
                                                text = anim.label,
                                                selected = selText.animation == anim,
                                                onClick = { viewModel.updateSelectedVideoText { it.copy(animation = anim) } }
                                            )
                                        }
                                    }

                                    VideoTextControl.COLOR -> StudioColorSwatchRow(selText.colorArgb, { c ->
                                        viewModel.updateSelectedVideoText { it.copy(colorArgb = c) }
                                    })

                                    VideoTextControl.BG_STYLE -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Background Box & Shadow", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Switch(
                                                    checked = selText.bgEnabled,
                                                    onCheckedChange = { b -> viewModel.updateSelectedVideoText { it.copy(bgEnabled = b) } },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricCyan)
                                                )
                                                Switch(
                                                    checked = selText.shadowEnabled,
                                                    onCheckedChange = { sh -> viewModel.updateSelectedVideoText { it.copy(shadowEnabled = sh) } },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonViolet)
                                                )
                                            }
                                        }
                                        StudioSliderRow("Outline", selText.strokeWidth, 0f..16f, "${selText.strokeWidth.roundToInt()}") { w ->
                                            viewModel.updateSelectedVideoText { it.copy(strokeWidth = w) }
                                        }
                                    }

                                    VideoTextControl.SIZE -> StudioSliderRow("Text Size", selText.scale, 0.5f..2.2f, "${(selText.scale * 100).roundToInt()}%") { sc ->
                                        viewModel.updateSelectedVideoText { it.copy(scale = sc) }
                                    }

                                    VideoTextControl.POSITION -> StudioSliderRow("Up / Down", selText.yNorm, 0.1f..0.92f, "${(selText.yNorm * 100).roundToInt()}%") { y ->
                                        viewModel.updateSelectedVideoText { it.copy(yNorm = y) }
                                    }
                                }
                            }
                        }

                        VideoToolTab.EXPORT -> {
                            val is4KSelected = state.exportResolution == VideoExportResolution.UHD_4K
                            val is60FpsSelected = state.exportFps == VideoExportFps.FPS_60
                            val needsSafeFallback = (is4KSelected && is60FpsSelected && !encoderCapabilities.supports4K60) ||
                                (is4KSelected && !encoderCapabilities.supports4K30)

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
                // 3. SLEEK SINGLE HORIZONTAL CAPCUT-STYLE BOTTOM ICON DOCK (TOGGLES DRAWER)
                // =========================================================================
                StudioToolTabStrip(
                    tabs = tabs,
                    selected = activeTab,
                    onSelect = { tab ->
                        if (activeTab == tab) {
                            isToolDrawerExpanded = !isToolDrawerExpanded
                        } else {
                            activeTab = tab
                            isToolDrawerExpanded = true
                        }
                    }
                )
            }
        }
    }
}
