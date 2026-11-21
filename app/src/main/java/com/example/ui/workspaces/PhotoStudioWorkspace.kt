package com.example.ui.workspaces

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.BlendModeKind
import com.example.model.CollageLayoutKind
import com.example.model.LayerKind
import com.example.model.PhotoCanvasPreset
import com.example.model.PhotoExportFormat
import com.example.model.PhotoLayer
import com.example.model.PhotoStudioState
import com.example.model.StudioShapeKind
import com.example.model.ToneCurvePreset
import com.example.ui.components.StudioActionPill
import com.example.ui.components.StudioChipButton
import com.example.ui.components.StudioColorSwatchRow
import com.example.ui.components.StudioSliderRow
import com.example.ui.components.StudioToolTabStrip
import com.example.ui.components.ToolTabItem
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProAmber
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioFonts
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.XonixStudioViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class PhotoToolTab {
    CANVAS, LAYERS, CUTOUT, BLUR, ADJUST, TEXT_SHAPES
}

private enum class TextShapesSubMode(val label: String) {
    TEXT("Text"),
    SHAPES("Shapes & Stickers")
}

private enum class PhotoLayerSlider(val label: String) {
    SCALE("Size"),
    ROTATE("Rotate"),
    FADE("Fade"),
    SKEW_X("Skew X"),
    SKEW_Y("Skew Y"),
    DOUBLE_EXP("Double Exp")
}

private enum class PhotoCutoutMode(val label: String) {
    REMOVE_BG("Magic Cut"),
    ERASER("Eraser"),
    RESTORE("Restore"),
    SPOT_HEAL("Spot Heal"),
    OUTLINE("Neon Glow")
}

private enum class PhotoAdjustControl(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    HIGHLIGHTS("Highlights"),
    SHADOWS("Shadows"),
    WHITES("Whites"),
    BLACKS("Blacks"),
    COLOR("Color"),
    VIBRANCE("Vibrance"),
    WARMTH("Warmth"),
    TINT("Tint"),
    HUE("Hue"),
    SHARP("Sharp"),
    CLARITY("Clarity"),
    SOFT("Soft"),
    VIGNETTE("Vignette"),
    HSL_RED("Red Mix"),
    HSL_GREEN("Green Mix"),
    HSL_BLUE("Blue Mix")
}

private enum class PhotoTextControl(val label: String) {
    FONT("Font"),
    COLOR("Color"),
    DEPTH_3D("3D Depth"),
    OUTLINE("Outline"),
    CURVE("Curve"),
    TILT("Tilt")
}

private enum class PhotoCustomDim(val label: String) {
    WIDTH("Width"),
    HEIGHT("Height")
}

@Composable
fun PhotoStudioWorkspace(
    state: PhotoStudioState,
    previewBitmap: Bitmap?,
    viewModel: XonixStudioViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable { mutableStateOf(PhotoToolTab.LAYERS) }
    var layerSlider by rememberSaveable { mutableStateOf(PhotoLayerSlider.SCALE) }
    var cutoutMode by rememberSaveable { mutableStateOf(PhotoCutoutMode.REMOVE_BG) }
    var adjustControl by rememberSaveable { mutableStateOf(PhotoAdjustControl.BRIGHTNESS) }
    var textControl by rememberSaveable { mutableStateOf(PhotoTextControl.FONT) }
    var textShapesMode by rememberSaveable { mutableStateOf(TextShapesSubMode.TEXT) }
    var customDim by rememberSaveable { mutableStateOf(PhotoCustomDim.WIDTH) }
    var importAsBackground by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.importPhotoLayer(uri, asBackground = importAsBackground)
    }

    val tabs = remember {
        listOf(
            ToolTabItem(PhotoToolTab.CANVAS, "Canvas", Icons.Default.Crop),
            ToolTabItem(PhotoToolTab.LAYERS, "Layers", Icons.Default.Layers),
            ToolTabItem(PhotoToolTab.CUTOUT, "Retouch", Icons.Default.AutoFixHigh),
            ToolTabItem(PhotoToolTab.BLUR, "DSLR Blur", Icons.Default.BlurOn),
            ToolTabItem(PhotoToolTab.ADJUST, "Adjust", Icons.Default.Tune),
            ToolTabItem(PhotoToolTab.TEXT_SHAPES, "Text/Shapes", Icons.Default.Title)
        )
    }

    val isBrushActive = state.eraserModeActive || state.restoreBrushActive || state.spotHealModeActive
    val brushBorderColor = when {
        state.eraserModeActive -> SignalRed
        state.restoreBrushActive -> EmeraldSafe
        state.spotHealModeActive -> ProAmber
        else -> StudioBorder.copy(alpha = 0.6f)
    }

    var editingTextLayerId by remember { mutableStateOf<String?>(null) }
    var inlineDraftText by remember { mutableStateOf("") }
    var inlineOriginalText by remember { mutableStateOf("") }
    val currentPhotoState by rememberUpdatedState(state)

    fun openInlineTextEditor(layer: PhotoLayer) {
        if (layer.kind != LayerKind.TEXT && layer.kind != LayerKind.STICKER) return
        val initial = if (layer.kind == LayerKind.STICKER) layer.stickerSymbol else layer.text
        editingTextLayerId = layer.id
        inlineDraftText = initial
        inlineOriginalText = initial
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // =========================================================================
        // 1. TOP 52%: INTERACTIVE TOUCH-LAYER PHOTO PREVIEW STAGE
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.52f)
                .background(OledBlack)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (editingTextLayerId != null) {
                        editingTextLayerId = null
                    } else if (!isBrushActive) {
                        viewModel.clearPhotoLayerSelection()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val aspect = (state.canvasWidth.toFloat() / state.canvasHeight.toFloat().coerceAtLeast(1f))
                .coerceIn(0.56f, 1.9f)

            // Unclipped aspect-ratio stage so layer corner & 1-finger rotate handles never clip
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspect, matchHeightConstraintsFirst = true),
                contentAlignment = Alignment.Center
            ) {
                val canvasW = constraints.maxWidth.coerceAtLeast(1)
                val canvasH = constraints.maxHeight.coerceAtLeast(1)
                val minCanvasEdge = min(canvasW, canvasH).coerceAtLeast(1).toFloat()

                // Clipped preview surface with Brush gestures OR Tap/Double-Tap layer selection + 2-finger pinch/rotate
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepObsidian)
                        .border(
                            width = if (isBrushActive) 2.dp else 0.5.dp,
                            color = brushBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .pointerInput(isBrushActive) {
                            if (isBrushActive) {
                                detectDragGestures { change, _ ->
                                    if (size.width <= 0 || size.height <= 0) return@detectDragGestures
                                    change.consume()
                                    val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                    viewModel.handlePhotoBrushTouch(nx, ny)
                                }
                            } else {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        val hit = hitTestPhotoLayer(
                                            layers = currentPhotoState.layers,
                                            tapX = tapOffset.x,
                                            tapY = tapOffset.y,
                                            canvasW = size.width.coerceAtLeast(1),
                                            canvasH = size.height.coerceAtLeast(1)
                                        )
                                        if (hit != null) {
                                            viewModel.selectPhotoLayer(hit.id)
                                        } else {
                                            viewModel.clearPhotoLayerSelection()
                                        }
                                    },
                                    onDoubleTap = { tapOffset ->
                                        val hit = hitTestPhotoLayer(
                                            layers = currentPhotoState.layers,
                                            tapX = tapOffset.x,
                                            tapY = tapOffset.y,
                                            canvasW = size.width.coerceAtLeast(1),
                                            canvasH = size.height.coerceAtLeast(1)
                                        )
                                        if (hit != null) {
                                            viewModel.selectPhotoLayer(hit.id)
                                            if (hit.kind == LayerKind.TEXT || hit.kind == LayerKind.STICKER) {
                                                openInlineTextEditor(hit)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(isBrushActive, "photo_canvas_multitouch") {
                            if (!isBrushActive) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    if (size.width <= 0 || size.height <= 0) return@detectTransformGestures
                                    if (currentPhotoState.selectedLayerId == null) return@detectTransformGestures
                                    viewModel.transformSelectedPhotoLayer(
                                        dxNorm = pan.x / size.width,
                                        dyNorm = pan.y / size.height,
                                        zoomFactor = zoom,
                                        rotationDeltaDeg = rotation
                                    )
                                }
                            }
                        }
                        .testTag("photo_preview_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null && !previewBitmap.isRecycled) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Photo Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isBrushActive) {
                        val hint = when {
                            state.eraserModeActive -> "Eraser On — Rub photo to erase"
                            state.restoreBrushActive -> "Restore Brush — Rub to restore pixels"
                            else -> "Spot Heal — Rub blemishes to blend"
                        }
                        Surface(
                            color = brushBorderColor.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Interactive White Touch Transform Box for Selected Layer (Text, Sticker, Shape, or Image)
                val selLayer = state.selectedLayer?.takeIf { it.visible && !it.locked && !isBrushActive && editingTextLayerId == null }
                if (selLayer != null) {
                    val box = computePhotoLayerBoxPx(selLayer, canvasW, canvasH)
                    val leftPx = (box.cx - box.boxW / 2f)
                        .roundToInt()
                        .coerceIn(-24, (canvasW - 40).coerceAtLeast(0))
                    val topPx = (box.cy - box.boxH / 2f)
                        .roundToInt()
                        .coerceIn(-12, (canvasH - 40).coerceAtLeast(0))
                    val isEditableText = selLayer.kind == LayerKind.TEXT || selLayer.kind == LayerKind.STICKER

                    InteractiveWhiteTransformBox(
                        leftPx = leftPx,
                        topPx = topPx,
                        boxW = box.boxW.roundToInt(),
                        boxH = box.boxH.roundToInt(),
                        rotationDeg = selLayer.rotationDeg,
                        showDeleteX = false,
                        onDeleteClick = {},
                        onDragMoveDelta = { dxPx, dyPx ->
                            viewModel.transformSelectedPhotoLayer(
                                dxNorm = dxPx / canvasW.toFloat(),
                                dyNorm = dyPx / canvasH.toFloat(),
                                zoomFactor = 1f,
                                rotationDeltaDeg = 0f
                            )
                        },
                        onPinchRotate = { zoom, rotDeg ->
                            viewModel.transformSelectedPhotoLayer(
                                dxNorm = 0f,
                                dyNorm = 0f,
                                zoomFactor = zoom,
                                rotationDeltaDeg = rotDeg
                            )
                        },
                        onCornerScaleDelta = { deltaNorm ->
                            viewModel.transformSelectedPhotoLayer(
                                dxNorm = 0f,
                                dyNorm = 0f,
                                zoomFactor = (1f + deltaNorm * 1.15f).coerceIn(0.65f, 1.5f),
                                rotationDeltaDeg = 0f
                            )
                        },
                        onRotateDeltaDeg = { deltaDeg ->
                            viewModel.transformSelectedPhotoLayer(
                                dxNorm = 0f,
                                dyNorm = 0f,
                                zoomFactor = 1f,
                                rotationDeltaDeg = deltaDeg
                            )
                        },
                        onDoubleTapBox = if (isEditableText) {
                            { openInlineTextEditor(selLayer) }
                        } else null,
                        badgeHint = if (isEditableText) "Double-tap to edit" else null,
                        minCanvasEdge = minCanvasEdge,
                        tagPrefix = "photo_layer_${selLayer.id}"
                    )
                }
            }

            // Double-Tap Inline Text Editor Overlay (Live canvas preview as you type)
            val editingLayer = editingTextLayerId?.let { id -> state.layers.firstOrNull { it.id == id } }
            if (editingLayer != null) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(editingLayer.id) {
                    runCatching { focusRequester.requestFocus() }
                }
                Surface(
                    color = DeepObsidian.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, ElectricCyan),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.92f)
                        .zIndex(20f)
                        .testTag("photo_inline_text_editor")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingLayer.kind == LayerKind.STICKER) "Edit Sticker Text" else "Edit Text Layer",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = ElectricCyan
                            )
                            IconButton(
                                onClick = {
                                    viewModel.updatePhotoLayerById(editingLayer.id) { l ->
                                        if (l.kind == LayerKind.STICKER) l.copy(stickerSymbol = inlineOriginalText)
                                        else l.copy(text = inlineOriginalText)
                                    }
                                    editingTextLayerId = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        OutlinedTextField(
                            value = inlineDraftText,
                            onValueChange = { updated ->
                                inlineDraftText = updated
                                viewModel.updatePhotoLayerById(editingLayer.id) { l ->
                                    if (l.kind == LayerKind.STICKER) l.copy(stickerSymbol = updated)
                                    else l.copy(text = updated)
                                }
                            },
                            placeholder = { Text("Type your headline...") },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = StudioBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("photo_inline_text_field")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updatePhotoLayerById(editingLayer.id) { l ->
                                        if (l.kind == LayerKind.STICKER) l.copy(stickerSymbol = inlineOriginalText)
                                        else l.copy(text = inlineOriginalText)
                                    }
                                    editingTextLayerId = null
                                },
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { editingTextLayerId = null },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("photo_inline_text_done")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. FOCUSED TOOL PANEL (1 SLIDER AT A TIME) + BOTTOM ICON ROW
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(0.5.dp, StudioBorder.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.48f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        PhotoToolTab.CANVAS -> {
                            Text("Aspect Ratio & Size", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PhotoCanvasPreset.entries.forEach { preset ->
                                    StudioChipButton(
                                        text = "${preset.label} ${preset.tag}",
                                        selected = state.preset == preset,
                                        onClick = { viewModel.setPhotoCanvasPreset(preset) }
                                    )
                                }
                            }
                            if (state.preset == PhotoCanvasPreset.CUSTOM) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PhotoCustomDim.entries.forEach { dim ->
                                        StudioChipButton(text = dim.label, selected = customDim == dim, onClick = { customDim = dim })
                                    }
                                }
                                when (customDim) {
                                    PhotoCustomDim.WIDTH -> StudioSliderRow("Width", state.customWidth.toFloat(), 640f..2560f, "${state.customWidth}px") {
                                        viewModel.setPhotoCustomSize(it.roundToInt(), state.customHeight)
                                    }
                                    PhotoCustomDim.HEIGHT -> StudioSliderRow("Height", state.customHeight.toFloat(), 640f..2560f, "${state.customHeight}px") {
                                        viewModel.setPhotoCustomSize(state.customWidth, it.roundToInt())
                                    }
                                }
                            }

                            Text("Collage Grid Layout", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CollageLayoutKind.entries.forEach { layout ->
                                    StudioChipButton(
                                        text = layout.label,
                                        selected = state.collageLayout == layout,
                                        onClick = { viewModel.setPhotoCollageLayout(layout) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("YouTube Thumbnail Safe-Zone Grid", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Switch(
                                    checked = state.showSafeZoneGrid,
                                    onCheckedChange = { viewModel.togglePhotoSafeZoneGrid() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricCyan)
                                )
                            }

                            StudioSliderRow(
                                label = "Frame & Border",
                                value = state.borderWidthPx,
                                valueRange = 0f..36f,
                                displayValue = "${state.borderWidthPx.roundToInt()}px"
                            ) { viewModel.setPhotoBorder(widthPx = it) }

                            if (state.borderWidthPx > 0.5f) {
                                StudioColorSwatchRow(state.borderColorArgb, { c -> viewModel.setPhotoBorder(colorArgb = c) })
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PhotoExportFormat.entries.forEach { fmt ->
                                    StudioChipButton(
                                        text = fmt.label,
                                        selected = state.exportFormat == fmt,
                                        onClick = { viewModel.setPhotoExportFormat(fmt) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (state.exportFormat == PhotoExportFormat.JPG_COMPRESSED && state.preset != PhotoCanvasPreset.CUSTOM) {
                                StudioSliderRow("JPG Quality", state.jpgQuality.toFloat(), 50f..100f, "${state.jpgQuality}%") {
                                    viewModel.setPhotoExportFormat(PhotoExportFormat.JPG_COMPRESSED, it.roundToInt())
                                }
                            }
                            Button(
                                onClick = { viewModel.exportPhoto() },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("photo_export_hd_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save ${state.exportFormat.label}", fontWeight = FontWeight.Bold)
                            }
                        }

                        PhotoToolTab.LAYERS -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioActionPill("+ Photo", Icons.Default.AddPhotoAlternate, {
                                    importAsBackground = false
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                })
                                StudioActionPill("Background", Icons.Default.Crop, {
                                    importAsBackground = true
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }, accent = NeonViolet)
                                StudioActionPill("+ Text", Icons.Default.Title, { viewModel.addPhotoTextLayer() })
                                StudioActionPill("+ Sticker", Icons.Default.Category, { viewModel.addPhotoStickerLayer("NEW!") }, accent = ProAmber)
                                if (state.selectedLayer != null) {
                                    StudioActionPill("Flip H", Icons.Default.Flip, {
                                        viewModel.updateSelectedPhotoLayer { it.copy(flipH = !it.flipH) }
                                    })
                                    StudioActionPill("Flip V", Icons.Default.Flip, {
                                        viewModel.updateSelectedPhotoLayer { it.copy(flipV = !it.flipV) }
                                    })
                                }
                            }

                            val sel = state.selectedLayer
                            if (sel != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PhotoLayerSlider.entries.forEach { s ->
                                        StudioChipButton(
                                            text = s.label,
                                            selected = layerSlider == s,
                                            onClick = { layerSlider = s }
                                        )
                                    }
                                }
                                when (layerSlider) {
                                    PhotoLayerSlider.SCALE -> StudioSliderRow("Size", sel.scale, 0.25f..2.6f, "${(sel.scale * 100).roundToInt()}%") { v ->
                                        viewModel.updateSelectedPhotoLayer { it.copy(scale = v) }
                                    }
                                    PhotoLayerSlider.ROTATE -> StudioSliderRow("Rotate", sel.rotationDeg, -180f..180f, "${sel.rotationDeg.roundToInt()}°") { v ->
                                        viewModel.updateSelectedPhotoLayer { it.copy(rotationDeg = v) }
                                    }
                                    PhotoLayerSlider.FADE -> StudioSliderRow("Opacity", sel.opacity, 0.1f..1f, "${(sel.opacity * 100).roundToInt()}%") { v ->
                                        viewModel.updateSelectedPhotoLayer { it.copy(opacity = v) }
                                    }
                                    PhotoLayerSlider.SKEW_X -> StudioSliderRow("Perspective X", sel.skewX, -0.45f..0.45f, "${(sel.skewX * 100).roundToInt()}%") { v ->
                                        viewModel.updateSelectedPhotoLayer { it.copy(skewX = v) }
                                    }
                                    PhotoLayerSlider.SKEW_Y -> StudioSliderRow("Perspective Y", sel.skewY, -0.45f..0.45f, "${(sel.skewY * 100).roundToInt()}%") { v ->
                                        viewModel.updateSelectedPhotoLayer { it.copy(skewY = v) }
                                    }
                                    PhotoLayerSlider.DOUBLE_EXP -> StudioSliderRow("Double Exposure", sel.doubleExposureAmount, 0f..1f, "${(sel.doubleExposureAmount * 100).roundToInt()}%") { v ->
                                        viewModel.updateSelectedPhotoLayer { it.copy(doubleExposureAmount = v) }
                                    }
                                }

                                // Blend Mode chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BlendModeKind.entries.forEach { bm ->
                                        StudioChipButton(
                                            text = bm.label,
                                            selected = sel.blendMode == bm,
                                            onClick = { viewModel.updateSelectedPhotoLayer { it.copy(blendMode = bm) } }
                                        )
                                    }
                                }
                            }

                            // Multi-Layer Manager Cards
                            state.layers.reversed().forEach { layer ->
                                val isSel = layer.id == state.selectedLayerId
                                Surface(
                                    onClick = { viewModel.selectPhotoLayer(layer.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) ElectricCyanContainer else StudioSurfaceElevated,
                                    border = BorderStroke(1.dp, if (isSel) ElectricCyan else StudioBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = layer.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSel) ElectricCyan else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { viewModel.duplicatePhotoLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextPrimary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(onClick = { viewModel.reorderPhotoLayer(layer.id, true) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextPrimary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(onClick = { viewModel.reorderPhotoLayer(layer.id, false) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextPrimary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(onClick = { viewModel.toggleLayerVisibility(layer.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(
                                                imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Show or Hide",
                                                tint = if (layer.visible) ElectricCyan else TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.toggleLayerLock(layer.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(
                                                imageVector = if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Lock",
                                                tint = if (layer.locked) ProAmber else TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deletePhotoLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SignalRed, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }

                        PhotoToolTab.CUTOUT -> {
                            val sel = state.selectedLayer
                            if (sel == null || sel.kind != LayerKind.IMAGE) {
                                Text(
                                    "Pick a photo layer first in Layers to cut out, erase, restore, or spot heal.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PhotoCutoutMode.entries.forEach { m ->
                                        StudioChipButton(
                                            text = m.label,
                                            selected = cutoutMode == m,
                                            onClick = {
                                                cutoutMode = m
                                                viewModel.setPhotoTouchTool(
                                                    eraser = m == PhotoCutoutMode.ERASER,
                                                    restore = m == PhotoCutoutMode.RESTORE,
                                                    spotHeal = m == PhotoCutoutMode.SPOT_HEAL
                                                )
                                            }
                                        )
                                    }
                                }

                                when (cutoutMode) {
                                    PhotoCutoutMode.REMOVE_BG -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Magic Wand Background Remover", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                            Switch(
                                                checked = sel.chromaEnabled,
                                                onCheckedChange = { c -> viewModel.updateSelectedPhotoLayer { it.copy(chromaEnabled = c) } },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSafe)
                                            )
                                        }
                                        if (sel.chromaEnabled) {
                                            StudioColorSwatchRow(sel.chromaColor, { c -> viewModel.updateSelectedPhotoLayer { it.copy(chromaColor = c) } })
                                            StudioSliderRow("Strength", sel.chromaTolerance, 0.08f..0.85f, "${(sel.chromaTolerance * 100).roundToInt()}%") { t ->
                                                viewModel.updateSelectedPhotoLayer { it.copy(chromaTolerance = t) }
                                            }
                                        }
                                    }

                                    PhotoCutoutMode.ERASER -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Freehand Feather Eraser", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (sel.eraserStrokes.isNotEmpty()) {
                                                    StudioActionPill("Clear", Icons.Default.Refresh, { viewModel.clearEraserStrokesOnSelected() }, accent = SignalRed)
                                                }
                                                Switch(
                                                    checked = state.eraserModeActive,
                                                    onCheckedChange = { viewModel.setPhotoTouchTool(eraser = it) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SignalRed)
                                                )
                                            }
                                        }
                                        StudioSliderRow("Brush Size", state.eraserBrushSize, 0.02f..0.18f, "${(state.eraserBrushSize * 100).roundToInt()}") {
                                            viewModel.updateEraserBrush(size = it)
                                        }
                                    }

                                    PhotoCutoutMode.RESTORE -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Restore Brush (Paint back erased areas)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                            Switch(
                                                checked = state.restoreBrushActive,
                                                onCheckedChange = { viewModel.setPhotoTouchTool(restore = it) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSafe)
                                            )
                                        }
                                        StudioSliderRow("Restore Radius", state.eraserBrushSize, 0.02f..0.18f, "${(state.eraserBrushSize * 100).roundToInt()}") {
                                            viewModel.updateEraserBrush(size = it)
                                        }
                                    }

                                    PhotoCutoutMode.SPOT_HEAL -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Spot Heal / Blemish Remover", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (sel.spotHealPoints.isNotEmpty()) {
                                                    StudioActionPill("Reset", Icons.Default.Refresh, { viewModel.clearSpotHealOnSelected() }, accent = ProAmber)
                                                }
                                                Switch(
                                                    checked = state.spotHealModeActive,
                                                    onCheckedChange = { viewModel.setPhotoTouchTool(spotHeal = it) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ProAmber)
                                                )
                                            }
                                        }
                                        StudioSliderRow("Heal Brush Size", state.eraserBrushSize, 0.025f..0.12f, "${(state.eraserBrushSize * 100).roundToInt()}") {
                                            viewModel.updateEraserBrush(size = it)
                                        }
                                    }

                                    PhotoCutoutMode.OUTLINE -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Auto-Outline Neon Glow", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                            Switch(
                                                checked = sel.outlineGlowEnabled,
                                                onCheckedChange = { g -> viewModel.updateSelectedPhotoLayer { it.copy(outlineGlowEnabled = g) } },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricCyan)
                                            )
                                        }
                                        if (sel.outlineGlowEnabled) {
                                            StudioColorSwatchRow(sel.outlineGlowColor, { c -> viewModel.updateSelectedPhotoLayer { it.copy(outlineGlowColor = c) } })
                                            StudioSliderRow("Glow Width", sel.outlineGlowWidth, 4f..26f, "${sel.outlineGlowWidth.roundToInt()}px") { w ->
                                                viewModel.updateSelectedPhotoLayer { it.copy(outlineGlowWidth = w) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PhotoToolTab.BLUR -> {
                            Text(
                                "DSLR Portrait Blur — Keep your subject crisp while softening the background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            StudioSliderRow(
                                label = "Portrait Background Blur",
                                value = state.dslrBackgroundBlur,
                                valueRange = 0f..1f,
                                displayValue = "${(state.dslrBackgroundBlur * 100).roundToInt()}%"
                            ) { viewModel.setDslrBackgroundBlur(it) }
                        }

                        PhotoToolTab.ADJUST -> {
                            val lr = state.lightroom
                            StudioActionPill(
                                label = "AI Magic Auto-Color",
                                icon = Icons.Default.AutoAwesome,
                                accent = ElectricCyan,
                                onClick = { viewModel.autoColorEnhancePhoto() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("photo_ai_magic_auto_color_button")
                            )
                            // Tone Curves row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ToneCurvePreset.entries.forEach { tc ->
                                    StudioChipButton(
                                        text = "Curve: ${tc.label}",
                                        selected = lr.toneCurve == tc,
                                        onClick = { viewModel.updateLightroom { it.copy(toneCurve = tc) } }
                                    )
                                }
                            }

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
                                    PhotoAdjustControl.entries.forEach { ctrl ->
                                        StudioChipButton(
                                            text = ctrl.label,
                                            selected = adjustControl == ctrl,
                                            onClick = { adjustControl = ctrl }
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.resetLightroom() },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Adjust", tint = TextSecondary)
                                }
                            }

                            // ONLY 1 slider open at a time
                            when (adjustControl) {
                                PhotoAdjustControl.BRIGHTNESS -> StudioSliderRow("Brightness", lr.brightness, -0.8f..0.8f, "${(lr.brightness * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(brightness = v) } }
                                PhotoAdjustControl.CONTRAST -> StudioSliderRow("Contrast", lr.contrast, 0.4f..2.0f, "${(lr.contrast * 100).roundToInt()}%") { v -> viewModel.updateLightroom { it.copy(contrast = v) } }
                                PhotoAdjustControl.HIGHLIGHTS -> StudioSliderRow("Highlights", lr.highlights, -1f..1f, "${(lr.highlights * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(highlights = v) } }
                                PhotoAdjustControl.SHADOWS -> StudioSliderRow("Shadows", lr.shadows, -1f..1f, "${(lr.shadows * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(shadows = v) } }
                                PhotoAdjustControl.WHITES -> StudioSliderRow("Whites", lr.whites, -1f..1f, "${(lr.whites * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(whites = v) } }
                                PhotoAdjustControl.BLACKS -> StudioSliderRow("Blacks", lr.blacks, -1f..1f, "${(lr.blacks * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(blacks = v) } }
                                PhotoAdjustControl.COLOR -> StudioSliderRow("Saturation", lr.saturation, 0f..2.2f, "${(lr.saturation * 100).roundToInt()}%") { v -> viewModel.updateLightroom { it.copy(saturation = v) } }
                                PhotoAdjustControl.VIBRANCE -> StudioSliderRow("Vibrance", lr.vibrance, -1f..1f, "${(lr.vibrance * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(vibrance = v) } }
                                PhotoAdjustControl.WARMTH -> StudioSliderRow("Warmth", lr.warmth, -1f..1f, "${(lr.warmth * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(warmth = v) } }
                                PhotoAdjustControl.TINT -> StudioSliderRow("Tint", lr.tint, -1f..1f, "${(lr.tint * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(tint = v) } }
                                PhotoAdjustControl.HUE -> StudioSliderRow("Hue", lr.hueDeg, -180f..180f, "${lr.hueDeg.roundToInt()}°") { v -> viewModel.updateLightroom { it.copy(hueDeg = v) } }
                                PhotoAdjustControl.SHARP -> StudioSliderRow("Sharpness", lr.sharpness, 0f..1f, "${(lr.sharpness * 100).roundToInt()}%") { v -> viewModel.updateLightroom { it.copy(sharpness = v) } }
                                PhotoAdjustControl.CLARITY -> StudioSliderRow("Clarity", lr.clarity, 0f..1f, "${(lr.clarity * 100).roundToInt()}%") { v -> viewModel.updateLightroom { it.copy(clarity = v) } }
                                PhotoAdjustControl.SOFT -> StudioSliderRow("Soften", lr.blur, 0f..1f, "${(lr.blur * 100).roundToInt()}%") { v -> viewModel.updateLightroom { it.copy(blur = v) } }
                                PhotoAdjustControl.VIGNETTE -> StudioSliderRow("Vignette", lr.vignette, 0f..1f, "${(lr.vignette * 100).roundToInt()}%") { v -> viewModel.updateLightroom { it.copy(vignette = v) } }
                                PhotoAdjustControl.HSL_RED -> StudioSliderRow("Red Mix", lr.hslRedShift, -0.5f..0.5f, "${(lr.hslRedShift * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(hslRedShift = v) } }
                                PhotoAdjustControl.HSL_GREEN -> StudioSliderRow("Green Mix", lr.hslGreenShift, -0.5f..0.5f, "${(lr.hslGreenShift * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(hslGreenShift = v) } }
                                PhotoAdjustControl.HSL_BLUE -> StudioSliderRow("Blue Mix", lr.hslBlueShift, -0.5f..0.5f, "${(lr.hslBlueShift * 100).roundToInt()}") { v -> viewModel.updateLightroom { it.copy(hslBlueShift = v) } }
                            }
                        }

                        PhotoToolTab.TEXT_SHAPES -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextShapesSubMode.entries.forEach { sub ->
                                    StudioChipButton(
                                        text = sub.label,
                                        selected = textShapesMode == sub,
                                        onClick = { textShapesMode = sub }
                                    )
                                }
                            }

                            if (textShapesMode == TextShapesSubMode.SHAPES) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioShapeKind.entries.forEach { shape ->
                                        StudioChipButton(
                                            text = "+ ${shape.label}",
                                            selected = state.selectedLayer?.kind == LayerKind.SHAPE && state.selectedLayer?.shapeKind == shape,
                                            onClick = { viewModel.addPhotoShapeLayer(shape) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("LIVE", "NEW!", "4K", "HOT", "PRO", "VIP").forEach { badge ->
                                        StudioChipButton(
                                            text = badge,
                                            selected = false,
                                            onClick = { viewModel.addPhotoStickerLayer(badge) }
                                        )
                                    }
                                }
                                val sel = state.selectedLayer
                                if (sel != null && (sel.kind == LayerKind.SHAPE || sel.kind == LayerKind.STICKER)) {
                                    StudioColorSwatchRow(sel.fillColor, { c -> viewModel.updateSelectedPhotoLayer { it.copy(fillColor = c) } })
                                    StudioSliderRow("Border Size", sel.strokeWidth, 0f..22f, "${sel.strokeWidth.roundToInt()}") { w ->
                                        viewModel.updateSelectedPhotoLayer { it.copy(strokeWidth = w) }
                                    }
                                }
                            } else {
                                val sel = state.selectedLayer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StudioActionPill("+ Add Text", Icons.Default.Title, { viewModel.addPhotoTextLayer("BLACK DEVIL\nPRO!") })
                                    if (sel != null && sel.kind == LayerKind.TEXT) {
                                        OutlinedTextField(
                                            value = sel.text,
                                            onValueChange = { txt -> viewModel.updateSelectedPhotoLayer { it.copy(text = txt, name = txt.lineSequence().firstOrNull()?.take(14) ?: "Text") } },
                                            placeholder = { Text("Type headline") },
                                            maxLines = 2,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ElectricCyan,
                                                unfocusedBorderColor = StudioBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (sel != null && sel.kind == LayerKind.TEXT) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PhotoTextControl.entries.forEach { ctrl ->
                                            StudioChipButton(
                                                text = ctrl.label,
                                                selected = textControl == ctrl,
                                                onClick = { textControl = ctrl }
                                            )
                                        }
                                    }

                                    when (textControl) {
                                        PhotoTextControl.FONT -> Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StudioFonts.all.forEach { font ->
                                                StudioChipButton(
                                                    text = font.displayName,
                                                    selected = sel.fontId == font.id,
                                                    onClick = { viewModel.updateSelectedPhotoLayer { it.copy(fontId = font.id) } }
                                                )
                                            }
                                        }

                                        PhotoTextControl.COLOR -> {
                                            StudioColorSwatchRow(sel.textColor, { c -> viewModel.updateSelectedPhotoLayer { it.copy(textColor = c) } })
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Gradient & Shadow", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Switch(
                                                        checked = sel.textUseGradient,
                                                        onCheckedChange = { g -> viewModel.updateSelectedPhotoLayer { it.copy(textUseGradient = g) } },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricCyan)
                                                    )
                                                    Switch(
                                                        checked = sel.textShadowEnabled,
                                                        onCheckedChange = { sh -> viewModel.updateSelectedPhotoLayer { it.copy(textShadowEnabled = sh) } },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonViolet)
                                                    )
                                                }
                                            }
                                        }

                                        PhotoTextControl.DEPTH_3D -> StudioSliderRow("3D Text Depth", sel.text3dDepth, 0f..16f, "${sel.text3dDepth.roundToInt()}px") { d ->
                                            viewModel.updateSelectedPhotoLayer { it.copy(text3dDepth = d) }
                                        }

                                        PhotoTextControl.OUTLINE -> {
                                            StudioColorSwatchRow(sel.textStrokeColor, { c -> viewModel.updateSelectedPhotoLayer { it.copy(textStrokeColor = c) } })
                                            StudioSliderRow("Outline", sel.textStrokeWidth, 0f..26f, "${sel.textStrokeWidth.roundToInt()}") { w ->
                                                viewModel.updateSelectedPhotoLayer { it.copy(textStrokeWidth = w) }
                                            }
                                        }

                                        PhotoTextControl.CURVE -> StudioSliderRow("Curve", sel.textCurveDeg, -90f..90f, "${sel.textCurveDeg.roundToInt()}°") { deg ->
                                            viewModel.updateSelectedPhotoLayer { it.copy(textCurveDeg = deg) }
                                        }

                                        PhotoTextControl.TILT -> StudioSliderRow("Tilt", sel.rotationDeg, -45f..45f, "${sel.rotationDeg.roundToInt()}°") { rot ->
                                            viewModel.updateSelectedPhotoLayer { it.copy(rotationDeg = rot) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. SINGLE HORIZONTAL ROW OF BIG CAPCUT-STYLE ICONS AT THE BOTTOM
                // =========================================================================
                StudioToolTabStrip(
                    tabs = tabs,
                    selected = activeTab,
                    onSelect = {
                        activeTab = it
                        if (it != PhotoToolTab.CUTOUT) {
                            viewModel.setPhotoTouchTool(eraser = false, restore = false, spotHeal = false)
                        }
                    }
                )
            }
        }
    }
}

private data class PhotoLayerBoxPx(
    val cx: Float,
    val cy: Float,
    val boxW: Float,
    val boxH: Float
)

private fun computePhotoLayerBoxPx(
    layer: PhotoLayer,
    canvasW: Int,
    canvasH: Int
): PhotoLayerBoxPx {
    val w = canvasW.coerceAtLeast(1).toFloat()
    val h = canvasH.coerceAtLeast(1).toFloat()
    val minEdge = min(w, h)
    val cx = layer.xNorm * w
    val cy = layer.yNorm * h

    val (boxW, boxH) = when (layer.kind) {
        LayerKind.TEXT -> {
            val textSizePx = (minEdge * 0.12f * layer.scale).coerceIn(18f, w * 0.45f)
            val lines = layer.text.ifBlank { "BLACK DEVIL" }.split("\n")
            val maxChars = (lines.maxOfOrNull { it.length } ?: 6).coerceAtLeast(3)
            val bw = (maxChars * textSizePx * 0.62f + 26f).coerceIn((w * 0.24f).coerceAtLeast(88f), w * 0.92f)
            val bh = (lines.size * textSizePx * 1.25f + 22f).coerceIn((h * 0.14f).coerceAtLeast(52f), h * 0.72f)
            bw to bh
        }
        LayerKind.STICKER -> {
            val bw = (minEdge * 0.30f * layer.scale).coerceIn(78f, w * 0.85f)
            val bh = (bw * 0.46f).coerceIn(46f, h * 0.55f)
            bw to bh
        }
        LayerKind.SHAPE -> {
            val baseSize = minEdge * 0.24f * layer.scale
            val bw = (baseSize * 1.48f).coerceIn(74f, w * 0.85f)
            val bh = (baseSize * 1.28f).coerceIn(66f, h * 0.85f)
            bw to bh
        }
        LayerKind.IMAGE -> {
            if (layer.isBackground) {
                val sc = layer.scale.coerceIn(0.45f, 1.0f)
                (w * 0.84f * sc).coerceIn(96f, w * 0.90f) to (h * 0.84f * sc).coerceIn(80f, h * 0.90f)
            } else {
                val baseFit = minEdge * 0.66f * layer.scale
                baseFit.coerceIn(84f, w * 0.90f) to baseFit.coerceIn(84f, h * 0.90f)
            }
        }
    }
    return PhotoLayerBoxPx(cx = cx, cy = cy, boxW = boxW, boxH = boxH)
}

private fun hitTestPhotoLayer(
    layers: List<PhotoLayer>,
    tapX: Float,
    tapY: Float,
    canvasW: Int,
    canvasH: Int
): PhotoLayer? {
    // Check topmost foreground layers first so text/stickers/cutouts are effortless to tap
    val foregroundCandidates = layers.asReversed().filter { it.visible && !it.locked && !it.isBackground }
    for (layer in foregroundCandidates) {
        val box = computePhotoLayerBoxPx(layer, canvasW, canvasH)
        val dx = tapX - box.cx
        val dy = tapY - box.cy
        val rad = (-layer.rotationDeg) * PI.toFloat() / 180f
        val cosA = cos(rad)
        val sinA = sin(rad)
        val localX = dx * cosA - dy * sinA
        val localY = dx * sinA + dy * cosA
        val pad = 16f
        if (abs(localX) <= box.boxW / 2f + pad && abs(localY) <= box.boxH / 2f + pad) {
            return layer
        }
    }
    return null
}
