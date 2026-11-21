package com.example.ui.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.StickerCropAspect
import com.example.model.VideoTextOverlay
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import kotlin.math.roundToInt

/**
 * Interactive on-canvas Sticker Crop Overlay:
 * - Free, 1:1, 16:9, 9:16 aspect ratio presets
 * - 4 draggable corner handles + draggable interior window
 * - Crisp rule-of-thirds guides + dimmed exterior mask
 * - Apply & Cancel buttons
 */
@Composable
internal fun BoxScope.StickerInteractiveCropOverlay(
    sticker: VideoTextOverlay,
    viewportW: Int,
    viewportH: Int,
    onApplyCrop: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onCancelCrop: () -> Unit
) {
    val density = LocalDensity.current
    var cropLeft by remember(sticker.id) { mutableFloatStateOf(sticker.cropLeft.coerceIn(0f, 0.85f)) }
    var cropTop by remember(sticker.id) { mutableFloatStateOf(sticker.cropTop.coerceIn(0f, 0.85f)) }
    var cropRight by remember(sticker.id) { mutableFloatStateOf(sticker.cropRight.coerceIn(0.15f, 1f)) }
    var cropBottom by remember(sticker.id) { mutableFloatStateOf(sticker.cropBottom.coerceIn(0.15f, 1f)) }
    var selectedAspect by remember(sticker.id) { mutableStateOf(StickerCropAspect.FREE) }

    val frameMarginPx = with(density) { 26.dp.toPx() }
    val availW = (viewportW - frameMarginPx * 2f).coerceAtLeast(80f)
    val availH = (viewportH - frameMarginPx * 2f - with(density) { 64.dp.toPx() }).coerceAtLeast(80f)
    val workLeft = (viewportW - availW) / 2f
    val workTop = with(density) { 38.dp.toPx() }.coerceAtMost((viewportH - availH).coerceAtLeast(0f) / 2f)

    fun applyAspectPreset(aspect: StickerCropAspect) {
        selectedAspect = aspect
        val ratio = aspect.ratio ?: return
        val cx = (cropLeft + cropRight) / 2f
        val cy = (cropTop + cropBottom) / 2f
        // Fit largest box with given ratio inside [0..1] x [0..1]
        val wNorm: Float
        val hNorm: Float
        val containerAspect = availW / availH.coerceAtLeast(1f)
        val normRatio = ratio / containerAspect
        if (normRatio >= 1f) {
            wNorm = 0.86f
            hNorm = (wNorm / normRatio).coerceIn(0.18f, 0.86f)
        } else {
            hNorm = 0.86f
            wNorm = (hNorm * normRatio).coerceIn(0.18f, 0.86f)
        }
        val halfW = wNorm / 2f
        val halfH = hNorm / 2f
        val clampedCx = cx.coerceIn(halfW, 1f - halfW)
        val clampedCy = cy.coerceIn(halfH, 1f - halfH)
        cropLeft = (clampedCx - halfW).coerceIn(0f, 0.85f)
        cropRight = (clampedCx + halfW).coerceIn(0.15f, 1f)
        cropTop = (clampedCy - halfH).coerceIn(0f, 0.85f)
        cropBottom = (clampedCy + halfH).coerceIn(0.15f, 1f)
    }

    val boxLeftPx = workLeft + cropLeft * availW
    val boxTopPx = workTop + cropTop * availH
    val boxRightPx = workLeft + cropRight * availW
    val boxBottomPx = workTop + cropBottom * availH
    val boxWPx = (boxRightPx - boxLeftPx).coerceAtLeast(24f)
    val boxHPx = (boxBottomPx - boxTopPx).coerceAtLeast(24f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(18f)
            .background(Color.Black.copy(alpha = 0.45f))
            .testTag("sticker_crop_overlay")
    ) {
        // Dimmed Exterior + Crisp White Crop Box + Rule of Thirds Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maskColor = Color.Black.copy(alpha = 0.58f)
            // Top mask
            drawRect(maskColor, Offset.Zero, Size(size.width, boxTopPx.coerceAtLeast(0f)))
            // Bottom mask
            drawRect(maskColor, Offset(0f, boxBottomPx), Size(size.width, (size.height - boxBottomPx).coerceAtLeast(0f)))
            // Left mask
            drawRect(maskColor, Offset(0f, boxTopPx), Size(boxLeftPx.coerceAtLeast(0f), boxHPx))
            // Right mask
            drawRect(maskColor, Offset(boxRightPx, boxTopPx), Size((size.width - boxRightPx).coerceAtLeast(0f), boxHPx))

            // Crop Box Border
            drawRect(
                color = Color.White,
                topLeft = Offset(boxLeftPx, boxTopPx),
                size = Size(boxWPx, boxHPx),
                style = Stroke(width = 2.dp.toPx())
            )
            // Rule of thirds lines
            val thirdW = boxWPx / 3f
            val thirdH = boxHPx / 3f
            val gridColor = Color.White.copy(alpha = 0.42f)
            for (i in 1..2) {
                drawLine(
                    gridColor,
                    Offset(boxLeftPx + thirdW * i, boxTopPx),
                    Offset(boxLeftPx + thirdW * i, boxBottomPx),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    gridColor,
                    Offset(boxLeftPx, boxTopPx + thirdH * i),
                    Offset(boxRightPx, boxTopPx + thirdH * i),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Draggable Interior Crop Window
        Box(
            modifier = Modifier
                .offset { IntOffset(boxLeftPx.roundToInt(), boxTopPx.roundToInt()) }
                .size(
                    width = with(density) { boxWPx.toDp() },
                    height = with(density) { boxHPx.toDp() }
                )
                .pointerInput("crop_interior_move") {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        down.consume()
                        var prev = down.position
                        while (true) {
                            val ev = awaitPointerEvent()
                            val cur = ev.changes.firstOrNull { it.pressed } ?: break
                            val dxNorm = (cur.position.x - prev.x) / availW
                            val dyNorm = (cur.position.y - prev.y) / availH
                            val w = cropRight - cropLeft
                            val h = cropBottom - cropTop
                            val nextL = (cropLeft + dxNorm).coerceIn(0f, (1f - w).coerceAtLeast(0f))
                            val nextT = (cropTop + dyNorm).coerceIn(0f, (1f - h).coerceAtLeast(0f))
                            cropLeft = nextL
                            cropRight = nextL + w
                            cropTop = nextT
                            cropBottom = nextT + h
                            prev = cur.position
                            cur.consume()
                        }
                    }
                }
        )

        // 4 Draggable White Corner Handles
        val handleSizePx = with(density) { 28.dp.roundToPx() }
        CropCornerHandle(
            xPx = (boxLeftPx - handleSizePx / 2f).roundToInt(),
            yPx = (boxTopPx - handleSizePx / 2f).roundToInt(),
            tag = "sticker_crop_tl"
        ) { dx, dy ->
            selectedAspect = StickerCropAspect.FREE
            cropLeft = (cropLeft + dx / availW).coerceIn(0f, (cropRight - 0.16f).coerceAtLeast(0f))
            cropTop = (cropTop + dy / availH).coerceIn(0f, (cropBottom - 0.16f).coerceAtLeast(0f))
        }
        CropCornerHandle(
            xPx = (boxRightPx - handleSizePx / 2f).roundToInt(),
            yPx = (boxTopPx - handleSizePx / 2f).roundToInt(),
            tag = "sticker_crop_tr"
        ) { dx, dy ->
            selectedAspect = StickerCropAspect.FREE
            cropRight = (cropRight + dx / availW).coerceIn((cropLeft + 0.16f).coerceAtMost(1f), 1f)
            cropTop = (cropTop + dy / availH).coerceIn(0f, (cropBottom - 0.16f).coerceAtLeast(0f))
        }
        CropCornerHandle(
            xPx = (boxLeftPx - handleSizePx / 2f).roundToInt(),
            yPx = (boxBottomPx - handleSizePx / 2f).roundToInt(),
            tag = "sticker_crop_bl"
        ) { dx, dy ->
            selectedAspect = StickerCropAspect.FREE
            cropLeft = (cropLeft + dx / availW).coerceIn(0f, (cropRight - 0.16f).coerceAtLeast(0f))
            cropBottom = (cropBottom + dy / availH).coerceIn((cropTop + 0.16f).coerceAtMost(1f), 1f)
        }
        CropCornerHandle(
            xPx = (boxRightPx - handleSizePx / 2f).roundToInt(),
            yPx = (boxBottomPx - handleSizePx / 2f).roundToInt(),
            tag = "sticker_crop_br"
        ) { dx, dy ->
            selectedAspect = StickerCropAspect.FREE
            cropRight = (cropRight + dx / availW).coerceIn((cropLeft + 0.16f).coerceAtMost(1f), 1f)
            cropBottom = (cropBottom + dy / availH).coerceIn((cropTop + 0.16f).coerceAtMost(1f), 1f)
        }

        // Top Aspect Ratio Bar (Free, 1:1, 16:9, 9:16)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .zIndex(20f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            StickerCropAspect.entries.forEach { aspect ->
                val active = selectedAspect == aspect
                Surface(
                    onClick = { applyAspectPreset(aspect) },
                    shape = RoundedCornerShape(50),
                    color = if (active) ElectricCyan else StudioSurfaceElevated.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, if (active) Color.White else StudioBorder),
                    modifier = Modifier.testTag("sticker_crop_aspect_${aspect.name.lowercase()}")
                ) {
                    Text(
                        text = aspect.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Bottom Apply & Cancel Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .zIndex(20f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onCancelCrop,
                shape = RoundedCornerShape(50),
                color = StudioSurfaceElevated.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, SignalRed),
                modifier = Modifier.testTag("sticker_crop_cancel_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Crop", tint = SignalRed, modifier = Modifier.size(14.dp))
                    Text("Cancel", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
            }
            Surface(
                onClick = { onApplyCrop(cropLeft, cropTop, cropRight, cropBottom) },
                shape = RoundedCornerShape(50),
                color = EmeraldSafe,
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier.testTag("sticker_crop_apply_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Apply Crop", tint = OledBlack, modifier = Modifier.size(14.dp))
                    Text("Apply", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = OledBlack)
                }
            }
        }
    }
}

@Composable
private fun CropCornerHandle(
    xPx: Int,
    yPx: Int,
    tag: String,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .offset { IntOffset(xPx, yPx) }
            .size(28.dp)
            .zIndex(19f)
            .pointerInput(tag) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                    var prev = down.position
                    while (true) {
                        val ev = awaitPointerEvent()
                        val cur = ev.changes.firstOrNull { it.pressed } ?: break
                        val dx = cur.position.x - prev.x
                        val dy = cur.position.y - prev.y
                        currentOnDrag(dx, dy)
                        prev = cur.position
                        cur.consume()
                    }
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.8.dp, OledBlack),
            modifier = Modifier.size(14.dp)
        ) {}
    }
}
