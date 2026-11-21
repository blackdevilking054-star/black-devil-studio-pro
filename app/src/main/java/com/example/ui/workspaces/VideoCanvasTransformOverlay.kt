package com.example.ui.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * VN / YouTube Create style Interactive White Transform Box:
 * - Crisp white bounding border that visually rotates with the layer
 * - 4 corner drag handles for smooth direct-on-canvas scaling
 * - Top stem + circular white rotation handle with live degree readout
 * - Optional top-right Red "X" delete button for PIP overlays
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
    tagPrefix: String
) {
    val density = LocalDensity.current
    val currentOnScale by rememberUpdatedState(onCornerScaleDelta)
    val currentOnRotate by rememberUpdatedState(onRotateDeltaDeg)
    val handleTopExtraPx = with(density) { 36.dp.roundToPx() }
    val handlePadPx = with(density) { 12.dp.roundToPx() }

    val totalW = boxW + handlePadPx * 2
    val totalH = boxH + handleTopExtraPx + handlePadPx

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(leftPx - handlePadPx, topPx - handleTopExtraPx) }
            .size(
                width = with(density) { totalW.toDp() },
                height = with(density) { totalH.toDp() }
            )
            .graphicsLayer { rotationZ = rotationDeg }
            .zIndex(8f)
            .testTag("${tagPrefix}_transform_box")
    ) {
        // Top Rotation Handle (White circular handle + stem rising from top edge center)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.2.dp, OledBlack.copy(alpha = 0.7f)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(26.dp)
                    .pointerInput(tagPrefix) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            currentOnRotate(dragAmount.x * 0.65f)
                        }
                    }
                    .testTag("${tagPrefix}_rotate_handle")
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
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(10.dp)
                    .background(Color.White)
            )
        }

        // Crisp White Bounding Box + 4 Corner Drag Handles
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(
                    width = with(density) { boxW.toDp() },
                    height = with(density) { boxH.toDp() }
                )
        ) {
            // White selection frame border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.6.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            // Live angle badge while rotated
            if (abs(rotationDeg) >= 1f) {
                Surface(
                    color = OledBlack.copy(alpha = 0.78f),
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

            // Corner 1: Top-Left Drag Resize Handle
            CornerResizeHandle(
                alignment = Alignment.TopStart,
                offsetX = (-8).dp,
                offsetY = (-8).dp,
                signX = -1f,
                signY = -1f,
                minCanvasEdge = minCanvasEdge,
                tag = "${tagPrefix}_corner_tl",
                onScaleDelta = { currentOnScale(it) }
            )

            // Corner 2: Top-Right (Red "X" Delete button for PIP, or White Corner Resize Handle for Video)
            if (showDeleteX) {
                Surface(
                    onClick = onDeleteClick,
                    shape = CircleShape,
                    color = SignalRed,
                    border = BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
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
            } else {
                CornerResizeHandle(
                    alignment = Alignment.TopEnd,
                    offsetX = 8.dp,
                    offsetY = (-8).dp,
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
                offsetX = (-8).dp,
                offsetY = 8.dp,
                signX = -1f,
                signY = 1f,
                minCanvasEdge = minCanvasEdge,
                tag = "${tagPrefix}_corner_bl",
                onScaleDelta = { currentOnScale(it) }
            )

            // Corner 4: Bottom-Right Drag Resize Handle
            CornerResizeHandle(
                alignment = Alignment.BottomEnd,
                offsetX = 8.dp,
                offsetY = 8.dp,
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
private fun BoxScope.CornerResizeHandle(
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
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
            .size(28.dp)
            .pointerInput(tag) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaNorm = (dragAmount.x * signX + dragAmount.y * signY) / (minCanvasEdge * 0.9f)
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
                .border(1.5.dp, OledBlack.copy(alpha = 0.75f), CircleShape)
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
