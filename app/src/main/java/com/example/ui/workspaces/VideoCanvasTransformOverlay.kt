package com.example.ui.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive White Transform Box (used in both Video Editor & Photo Studio):
 * - Crisp white bounding border that rotates with the layer
 * - Unified 1-finger drag-to-move + 2-finger pinch-resize & rotate + tap / double-tap
 * - 4 generous corner drag handles for smooth 1-finger resizing
 * - Top stem + circular white 1-finger rotation handle with live degree readout
 * - Optional top-right Red "X" delete button (for Video PIP overlays)
 */
@Composable
internal fun BoxScope.InteractiveWhiteTransformBox(
    leftPx: Int,
    topPx: Int,
    boxW: Int,
    boxH: Int,
    rotationDeg: Float,
    showDeleteX: Boolean,
    onDeleteClick: () -> Unit,
    onCornerScaleDelta: (Float) -> Unit,
    onRotateDeltaDeg: (Float) -> Unit,
    minCanvasEdge: Float,
    tagPrefix: String,
    onDragMoveDelta: (dxPx: Float, dyPx: Float) -> Unit = { _, _ -> },
    onPinchRotate: (zoom: Float, rotationDeltaDeg: Float) -> Unit = { _, _ -> },
    onTapBox: () -> Unit = {},
    onDoubleTapBox: (() -> Unit)? = null,
    badgeHint: String? = null,
    allowPinchRotate: Boolean = true
) {
    val density = LocalDensity.current
    val currentOnScale by rememberUpdatedState(onCornerScaleDelta)
    val currentOnRotate by rememberUpdatedState(onRotateDeltaDeg)
    val currentOnDragMove by rememberUpdatedState(onDragMoveDelta)
    val currentOnPinchRotate by rememberUpdatedState(onPinchRotate)
    val currentOnTap by rememberUpdatedState(onTapBox)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTapBox)
    val currentRotDeg by rememberUpdatedState(rotationDeg)
    val currentAllowPinchRotate by rememberUpdatedState(allowPinchRotate)

    val handleTopExtraPx = with(density) { 56.dp.roundToPx() }
    val handlePadPx = with(density) { 22.dp.roundToPx() }

    val totalW = boxW + handlePadPx * 2
    val totalH = boxH + handleTopExtraPx + handlePadPx
    val boxCenterYNorm = (handleTopExtraPx + boxH / 2f) / totalH.coerceAtLeast(1).toFloat()

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(leftPx - handlePadPx, topPx - handleTopExtraPx) }
            .size(
                width = with(density) { totalW.toDp() },
                height = with(density) { totalH.toDp() }
            )
            .graphicsLayer {
                rotationZ = rotationDeg
                transformOrigin = TransformOrigin(0.5f, boxCenterYNorm)
            }
            .zIndex(8f)
            .testTag("${tagPrefix}_transform_box")
    ) {
        // Dedicated Top 1-Finger Circular Rotation Handle + Stem (clearly separated above top edge resize handle)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(11f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .pointerInput(tagPrefix) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            currentOnRotate(dragAmount.x * 0.72f)
                        }
                    }
                    .testTag("${tagPrefix}_rotate_handle"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.2.dp, OledBlack.copy(alpha = 0.75f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate Frame",
                            tint = OledBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.6.dp)
                    .height(24.dp)
                    .background(Color.White)
            )
        }

        // Crisp White Bounding Box + Unified 1-Finger Drag / 2-Finger Pinch-Rotate / Double-Tap + 4 Corner & 4 Side Handles
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
                .size(
                    width = with(density) { boxW.toDp() },
                    height = with(density) { boxH.toDp() }
                )
        ) {
            // Interactive interior: 1-finger move, 2-finger pinch & rotate, single tap, double tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.8.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .pointerInput(tagPrefix) {
                        var lastTapTime = 0L
                        var lastTapPos = Offset.Zero
                        val touchSlop = viewConfiguration.touchSlop
                        val doubleTapMs = viewConfiguration.doubleTapTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            val downPos = down.position
                            var prevSinglePos = downPos
                            var moved = false
                            var multiTouchActive = false
                            var prevCentroid = Offset.Zero
                            var prevSpan = 0f
                            var prevAngle = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                if (pressed.size >= 2) {
                                    val p0 = pressed[0].position
                                    val p1 = pressed[1].position
                                    val centroid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                                    val span = hypot(p1.x - p0.x, p1.y - p0.y).coerceAtLeast(1f)
                                    val angle = atan2(p1.y - p0.y, p1.x - p0.x)
                                    if (!multiTouchActive) {
                                        multiTouchActive = true
                                        moved = true
                                        prevCentroid = centroid
                                        prevSpan = span
                                        prevAngle = angle
                                    } else {
                                        val zoom = (span / prevSpan.coerceAtLeast(1f)).coerceIn(0.6f, 1.6f)
                                        var deltaRad = angle - prevAngle
                                        while (deltaRad > PI) deltaRad -= (2.0 * PI).toFloat()
                                        while (deltaRad < -PI) deltaRad += (2.0 * PI).toFloat()
                                        val rotDeltaDeg = if (currentAllowPinchRotate) {
                                            (deltaRad * 180f / PI.toFloat())
                                        } else {
                                            0f
                                        }
                                        val localDx = centroid.x - prevCentroid.x
                                        val localDy = centroid.y - prevCentroid.y
                                        // Convert local rotated-box delta into parent canvas coordinates
                                        val rad = currentRotDeg * PI.toFloat() / 180f
                                        val cosA = cos(rad)
                                        val sinA = sin(rad)
                                        val worldDx = localDx * cosA - localDy * sinA
                                        val worldDy = localDx * sinA + localDy * cosA
                                        currentOnPinchRotate(zoom, rotDeltaDeg)
                                        if (abs(worldDx) > 0.2f || abs(worldDy) > 0.2f) {
                                            currentOnDragMove(worldDx, worldDy)
                                        }
                                        prevCentroid = centroid
                                        prevSpan = span
                                        prevAngle = angle
                                    }
                                    pressed.forEach { it.consume() }
                                } else if (!multiTouchActive) {
                                    val cur = pressed[0]
                                    val totalDist = hypot(cur.position.x - downPos.x, cur.position.y - downPos.y)
                                    if (!moved && totalDist > touchSlop) {
                                        moved = true
                                        prevSinglePos = cur.position
                                    }
                                    if (moved) {
                                        val localDx = cur.position.x - prevSinglePos.x
                                        val localDy = cur.position.y - prevSinglePos.y
                                        if (abs(localDx) > 0.2f || abs(localDy) > 0.2f) {
                                            val rad = currentRotDeg * PI.toFloat() / 180f
                                            val cosA = cos(rad)
                                            val sinA = sin(rad)
                                            val worldDx = localDx * cosA - localDy * sinA
                                            val worldDy = localDx * sinA + localDy * cosA
                                            currentOnDragMove(worldDx, worldDy)
                                        }
                                        prevSinglePos = cur.position
                                        cur.consume()
                                    }
                                }
                            }

                            if (!moved && !multiTouchActive) {
                                val now = System.currentTimeMillis()
                                val tapDist = hypot(downPos.x - lastTapPos.x, downPos.y - lastTapPos.y)
                                val dbl = currentOnDoubleTap
                                if (dbl != null && (now - lastTapTime) <= doubleTapMs && tapDist <= 90f) {
                                    lastTapTime = 0L
                                    dbl()
                                } else {
                                    lastTapTime = now
                                    lastTapPos = downPos
                                    currentOnTap()
                                }
                            }
                        }
                    }
                    .testTag("${tagPrefix}_box_drag_area")
            )

            // Live angle badge while rotated
            if (abs(rotationDeg) >= 1f) {
                Surface(
                    color = OledBlack.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                ) {
                    Text(
                        text = "${rotationDeg.roundToInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Optional contextual hint (e.g. "Double-tap to edit" on Text layers)
            if (!badgeHint.isNullOrBlank()) {
                Surface(
                    color = OledBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = badgeHint,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Side Edge Resize Handles centered cleanly on exact Left, Right, Top, and Bottom outer edges
            SideResizeHandle(
                alignment = Alignment.CenterStart,
                offsetX = (-14).dp,
                offsetY = 0.dp,
                isHorizontalBar = false,
                signX = -1f,
                signY = 0f,
                edgeLengthPx = boxW.coerceAtLeast(64).toFloat(),
                tag = "${tagPrefix}_side_left",
                onScaleDelta = { currentOnScale(it) }
            )
            SideResizeHandle(
                alignment = Alignment.CenterEnd,
                offsetX = 14.dp,
                offsetY = 0.dp,
                isHorizontalBar = false,
                signX = 1f,
                signY = 0f,
                edgeLengthPx = boxW.coerceAtLeast(64).toFloat(),
                tag = "${tagPrefix}_side_right",
                onScaleDelta = { currentOnScale(it) }
            )
            SideResizeHandle(
                alignment = Alignment.TopCenter,
                offsetX = 0.dp,
                offsetY = (-14).dp,
                isHorizontalBar = true,
                signX = 0f,
                signY = -1f,
                edgeLengthPx = boxH.coerceAtLeast(64).toFloat(),
                tag = "${tagPrefix}_side_top",
                onScaleDelta = { currentOnScale(it) }
            )
            SideResizeHandle(
                alignment = Alignment.BottomCenter,
                offsetX = 0.dp,
                offsetY = 14.dp,
                isHorizontalBar = true,
                signX = 0f,
                signY = 1f,
                edgeLengthPx = boxH.coerceAtLeast(64).toFloat(),
                tag = "${tagPrefix}_side_bottom",
                onScaleDelta = { currentOnScale(it) }
            )

            // Corner 1: Top-Left Drag Resize Handle (centered dead-on exact outer vertex)
            CornerResizeHandle(
                alignment = Alignment.TopStart,
                offsetX = (-17).dp,
                offsetY = (-17).dp,
                signX = -1f,
                signY = -1f,
                minCanvasEdge = minCanvasEdge,
                tag = "${tagPrefix}_corner_tl",
                onScaleDelta = { currentOnScale(it) }
            )

            // Corner 2: Top-Right (Red "X" Delete button for PIP, or White Corner Resize Handle)
            if (showDeleteX) {
                Surface(
                    onClick = onDeleteClick,
                    shape = CircleShape,
                    color = SignalRed,
                    border = BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 14.dp, y = (-14).dp)
                        .size(28.dp)
                        .zIndex(10f)
                        .testTag("pip_overlay_delete_x_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Overlay",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                CornerResizeHandle(
                    alignment = Alignment.TopEnd,
                    offsetX = 17.dp,
                    offsetY = (-17).dp,
                    signX = 1f,
                    signY = -1f,
                    minCanvasEdge = minCanvasEdge,
                    tag = "${tagPrefix}_corner_tr",
                    onScaleDelta = { currentOnScale(it) }
                )
            }

            // Corner 3: Bottom-Left Drag Resize Handle
            CornerResizeHandle(
                alignment = Alignment.BottomStart,
                offsetX = (-17).dp,
                offsetY = 17.dp,
                signX = -1f,
                signY = 1f,
                minCanvasEdge = minCanvasEdge,
                tag = "${tagPrefix}_corner_bl",
                onScaleDelta = { currentOnScale(it) }
            )

            // Corner 4: Bottom-Right Drag Resize Handle
            CornerResizeHandle(
                alignment = Alignment.BottomEnd,
                offsetX = 17.dp,
                offsetY = 17.dp,
                signX = 1f,
                signY = 1f,
                minCanvasEdge = minCanvasEdge,
                tag = "${tagPrefix}_corner_br",
                onScaleDelta = { currentOnScale(it) }
            )
        }
    }
}

@Composable
private fun BoxScope.SideResizeHandle(
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    isHorizontalBar: Boolean,
    signX: Float,
    signY: Float,
    edgeLengthPx: Float,
    tag: String,
    onScaleDelta: (Float) -> Unit
) {
    val currentOnScale by rememberUpdatedState(onScaleDelta)
    val currentEdge by rememberUpdatedState(edgeLengthPx)
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(28.dp)
            .zIndex(9f)
            .pointerInput(tag) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaNorm = (dragAmount.x * signX + dragAmount.y * signY) / (currentEdge.coerceAtLeast(64f) * 0.55f)
                    currentOnScale(deltaNorm)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = if (isHorizontalBar) 20.dp else 6.dp,
                    height = if (isHorizontalBar) 6.dp else 20.dp
                )
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White)
                .border(1.2.dp, OledBlack.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun BoxScope.CornerResizeHandle(
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    signX: Float,
    signY: Float,
    minCanvasEdge: Float,
    tag: String,
    onScaleDelta: (Float) -> Unit
) {
    val currentOnScale by rememberUpdatedState(onScaleDelta)
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(34.dp)
            .zIndex(10f)
            .pointerInput(tag) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaNorm = (dragAmount.x * signX + dragAmount.y * signY) / (minCanvasEdge * 0.72f)
                    currentOnScale(deltaNorm)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.6.dp, OledBlack.copy(alpha = 0.8f), CircleShape)
        )
    }
}

@Composable
internal fun QuickActionChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    testTagName: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.testTag(testTagName)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = if (tint == SignalRed) SignalRed else TextPrimary
            )
        }
    }
}

internal fun normalizeAngleDeg(deg: Float): Float {
    var d = deg % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}
