package com.example.ui.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.engine.VideoMediaEngine
import com.example.engine.VideoOverlayRenderer
import com.example.model.KeyframeEasing
import com.example.model.VideoEditorState
import com.example.model.VideoKeyframe
import com.example.ui.components.StudioActionPill
import com.example.ui.components.StudioChipButton
import com.example.ui.components.StudioSliderRow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.XonixStudioViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ProKeyframeProperty(val label: String) {
    SCALE("Scale"),
    POS_X("Pos X"),
    POS_Y("Pos Y"),
    ROTATE("Rotate"),
    OPACITY("Opacity")
}

data class ActiveKeyframeTargetInfo(
    val shortKind: String,
    val title: String,
    val isOverlay: Boolean,
    val startMs: Long,
    val durationMs: Long,
    val localMs: Long,
    val keyframes: List<VideoKeyframe>,
    val currentX: Float,
    val currentY: Float,
    val currentScale: Float,
    val currentRotationDeg: Float,
    val currentOpacity: Float,
    val activeKeyframe: VideoKeyframe?,
    val hasPrevKeyframe: Boolean,
    val hasNextKeyframe: Boolean,
    val activeSegment: Pair<VideoKeyframe, VideoKeyframe>?
)

/**
 * Resolves the currently active Keyframe target (Selected Text/Sticker if one is selected,
 * otherwise the active/selected Video clip under the playhead).
 */
fun resolveActiveKeyframeTarget(state: VideoEditorState): ActiveKeyframeTargetInfo? {
    val tol = XonixStudioViewModel.KEYFRAME_MATCH_TOLERANCE_MS
    val selOverlay = state.selectedTextId?.let { id ->
        state.textLayers.firstOrNull { it.id == id }
    }
    if (selOverlay != null) {
        val dur = (selOverlay.endMs - selOverlay.startMs).coerceAtLeast(1L)
        val localMs = (state.playheadMs - selOverlay.startMs).coerceIn(0L, dur)
        val eff = VideoOverlayRenderer.resolveOverlayTransformAt(selOverlay, selOverlay.startMs + localMs)
        val sorted = selOverlay.keyframes.sortedBy { it.timeMs }
        val activeKf = sorted.firstOrNull { abs(it.timeMs - localMs) <= tol }
        val hasPrev = sorted.any { it.timeMs < localMs - tol }
        val hasNext = sorted.any { it.timeMs > localMs + tol }
        val segment = VideoMediaEngine.findActiveKeyframeSegment(sorted, localMs)
        return ActiveKeyframeTargetInfo(
            shortKind = if (selOverlay.isSticker) "Sticker" else "Text",
            title = selOverlay.text.ifBlank { if (selOverlay.isSticker) "Sticker" else "Text" },
            isOverlay = true,
            startMs = selOverlay.startMs,
            durationMs = dur,
            localMs = localMs,
            keyframes = sorted,
            currentX = eff.xNorm,
            currentY = eff.yNorm,
            currentScale = eff.scale,
            currentRotationDeg = eff.rotationDeg,
            currentOpacity = eff.opacity,
            activeKeyframe = activeKf,
            hasPrevKeyframe = hasPrev,
            hasNextKeyframe = hasNext,
            activeSegment = segment
        )
    }

    if (state.clips.isEmpty()) return null
    var acc = 0L
    var targetClip = state.clips.last()
    var targetStart = 0L
    var foundAtPlayhead = false
    for (i in state.clips.indices) {
        val c = state.clips[i]
        val d = c.effectiveDurationMs
        if (state.playheadMs <= acc + d || i == state.clips.lastIndex) {
            targetClip = c
            targetStart = acc
            foundAtPlayhead = true
            break
        }
        acc += d
    }
    if (!foundAtPlayhead) return null
    val dur = targetClip.effectiveDurationMs.coerceAtLeast(1L)
    val localMs = (state.playheadMs - targetStart).coerceIn(0L, dur)
    val sorted = targetClip.keyframes.sortedBy { it.timeMs }
    val hasKfs = sorted.isNotEmpty()
    val interp = if (hasKfs) VideoMediaEngine.interpolateKeyframes(sorted, localMs) else null
    val activeKf = sorted.firstOrNull { abs(it.timeMs - localMs) <= tol }
    val hasPrev = sorted.any { it.timeMs < localMs - tol }
    val hasNext = sorted.any { it.timeMs > localMs + tol }
    val segment = VideoMediaEngine.findActiveKeyframeSegment(sorted, localMs)
    return ActiveKeyframeTargetInfo(
        shortKind = "Video",
        title = targetClip.name,
        isOverlay = false,
        startMs = targetStart,
        durationMs = dur,
        localMs = localMs,
        keyframes = sorted,
        currentX = interp?.xNorm ?: state.panX,
        currentY = interp?.yNorm ?: state.panY,
        currentScale = interp?.scale ?: state.cropZoom,
        currentRotationDeg = interp?.rotationDeg ?: targetClip.rotationDeg,
        currentOpacity = interp?.opacity ?: 1f,
        activeKeyframe = activeKf,
        hasPrevKeyframe = hasPrev,
        hasNextKeyframe = hasNext,
        activeSegment = segment
    )
}

/**
 * Draws glowing crimson/white diamond markers (`◆`) directly on a Video, Text, or Sticker
 * timeline clip at each Keyframe's exact time position, plus a subtle glowing connector line
 * between consecutive Keyframes.
 */
@Composable
fun TimelineKeyframeDiamondMarkers(
    keyframes: List<VideoKeyframe>,
    clipDurationMs: Long,
    clipStartMs: Long,
    playheadMs: Long,
    clipWidthDp: Dp,
    topPaddingDp: Dp = 3.dp,
    tagOwnerId: String,
    onSelectKeyframe: (VideoKeyframe) -> Unit,
    modifier: Modifier = Modifier
) {
    if (keyframes.isEmpty()) return
    val density = LocalDensity.current
    val sorted = keyframes.sortedBy { it.timeMs }
    val safeDur = clipDurationMs.coerceAtLeast(1L).toFloat()
    val clipWpx = with(density) { clipWidthDp.toPx() }.coerceAtLeast(1f)
    val markerSizeDp = 15.dp
    val markerPx = with(density) { markerSizeDp.toPx() }
    val topPx = with(density) { topPaddingDp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        // Glowing crimson animation span line connecting consecutive Keyframes
        if (sorted.size >= 2) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5.5f)
            ) {
                val yCenter = topPx + markerPx / 2f
                val firstFrac = (sorted.first().timeMs.toFloat() / safeDur).coerceIn(0f, 1f)
                val lastFrac = (sorted.last().timeMs.toFloat() / safeDur).coerceIn(0f, 1f)
                val x1 = (clipWpx * firstFrac).coerceIn(markerPx / 2f, (clipWpx - markerPx / 2f).coerceAtLeast(markerPx / 2f))
                val x2 = (clipWpx * lastFrac).coerceIn(x1, (clipWpx - markerPx / 2f).coerceAtLeast(x1))
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            ElectricCyan.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.90f),
                            ElectricCyan.copy(alpha = 0.75f)
                        )
                    ),
                    start = Offset(x1, yCenter),
                    end = Offset(x2, yCenter),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Individual glowing crimson/white diamond markers (`◆`)
        sorted.forEach { kf ->
            val frac = (kf.timeMs.toFloat() / safeDur).coerceIn(0f, 1f)
            val xPx = (clipWpx * frac - markerPx / 2f)
                .roundToInt()
                .coerceIn(0, (clipWpx - markerPx).roundToInt().coerceAtLeast(0))
            val isAtPlayhead = abs((clipStartMs + kf.timeMs) - playheadMs) <= 65L

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(xPx, topPx.roundToInt()) }
                    .size(markerSizeDp)
                    .zIndex(7f)
                    .clickable { onSelectKeyframe(kf) }
                    .testTag("keyframe_marker_${tagOwnerId}_${kf.id}"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.40f
                    val glowR = size.minDimension * 0.56f

                    // Outer crimson glow halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricCyan.copy(alpha = if (isAtPlayhead) 0.95f else 0.65f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = glowR
                        ),
                        radius = glowR,
                        center = Offset(cx, cy)
                    )

                    val diamond = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r, cy)
                        close()
                    }
                    // Active at playhead = glowing white core + crimson rim; resting = vivid crimson core + crisp white rim
                    drawPath(
                        path = diamond,
                        color = if (isAtPlayhead) Color.White else ElectricCyan
                    )
                    drawPath(
                        path = diamond,
                        color = if (isAtPlayhead) ElectricCyan else Color.White,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Compact CapCut / VN Keyframe Control Strip placed above the timeline whenever a Video, Text,
 * or Sticker layer is active:
 * - Target badge + Keyframe count
 * - `<◆` Previous Keyframe jump button
 * - `◆+` / `◆-` Add / Remove Keyframe toggle button
 * - `◆>` Next Keyframe jump button
 * - Inline Easing Curve badge (visible automatically when playhead sits between two Keyframes)
 */
@Composable
fun CompactCapCutKeyframeBar(
    target: ActiveKeyframeTargetInfo,
    onPreviousKeyframe: () -> Unit,
    onToggleKeyframe: () -> Unit,
    onNextKeyframe: () -> Unit,
    onSelectEasing: (KeyframeEasing) -> Unit,
    onOpenKeyframeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnKf = target.activeKeyframe != null
    val activeSeg = target.activeSegment

    Surface(
        color = StudioSurfaceElevated,
        border = BorderStroke(0.5.dp, StudioBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("capcut_keyframe_control_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Target & Keyframe count pill
            Surface(
                onClick = onOpenKeyframeDrawer,
                shape = RoundedCornerShape(8.dp),
                color = OledBlack.copy(alpha = 0.72f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (target.keyframes.isNotEmpty()) ElectricCyan.copy(alpha = 0.8f) else StudioBorder
                ),
                modifier = Modifier.testTag("keyframe_target_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Keyframe Target",
                        tint = if (target.keyframes.isNotEmpty()) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (target.keyframes.isNotEmpty()) {
                            "${target.shortKind} • ${target.keyframes.size}◆"
                        } else {
                            target.shortKind
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // `<◆` Previous Keyframe Jump Button
            KeyframeJumpButton(
                label = "<◆",
                contentDesc = "Previous Keyframe",
                enabled = target.hasPrevKeyframe,
                testTagName = "keyframe_prev_button",
                onClick = onPreviousKeyframe
            )

            // `◆+` / `◆-` Add or Remove Keyframe Toggle Button
            Surface(
                onClick = onToggleKeyframe,
                shape = RoundedCornerShape(9.dp),
                color = if (isOnKf) ElectricCyan else ElectricCyanContainer,
                border = BorderStroke(
                    width = 1.2.dp,
                    color = if (isOnKf) Color.White else ElectricCyan
                ),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("keyframe_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isOnKf) "◆-" else "◆+",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (isOnKf) "Remove" else "Keyframe",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.White
                    )
                }
            }

            // `◆>` Next Keyframe Jump Button
            KeyframeJumpButton(
                label = "◆>",
                contentDesc = "Next Keyframe",
                enabled = target.hasNextKeyframe,
                testTagName = "keyframe_next_button",
                onClick = onNextKeyframe
            )

            // Inline Easing Curve Quick Selector (appears automatically when playhead sits between two Keyframes)
            if (activeSeg != null) {
                val curEasing = activeSeg.first.easing
                Spacer(Modifier.width(2.dp))
                KeyframeEasing.entries.forEach { easing ->
                    val selected = curEasing == easing
                    Surface(
                        onClick = { onSelectEasing(easing) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) ElectricCyan else StudioSurface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) Color.White else StudioBorder
                        ),
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("keyframe_inline_easing_${easing.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = easing.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 9.5.sp
                                ),
                                color = if (selected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyframeJumpButton(
    label: String,
    contentDesc: String,
    enabled: Boolean,
    testTagName: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) StudioSurface else OledBlack.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) Color.White.copy(alpha = 0.85f) else StudioBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .height(28.dp)
            .testTag(testTagName)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = if (enabled) Color.White else TextSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Full CapCut / VN Keyframe Drawer Panel supporting Video, Text, and Sticker layers,
 * `<◆` / `◆+ / ◆-` / `◆>` navigation, Easing Curve selector between keyframes,
 * clickable timestamp chips, and real-time transform sliders.
 */
@Composable
fun ProKeyframeDrawerPanel(
    target: ActiveKeyframeTargetInfo?,
    onPreviousKeyframe: () -> Unit,
    onToggleKeyframe: () -> Unit,
    onNextKeyframe: () -> Unit,
    onSelectEasing: (KeyframeEasing) -> Unit,
    onJumpToTimestamp: (Long) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    onClearAllKeyframes: () -> Unit,
    onUpdateTransform: (xNorm: Float?, yNorm: Float?, scale: Float?, rotationDeg: Float?, opacity: Float?) -> Unit
) {
    if (target == null) {
        Text(
            text = "Select a Video, Text, or Sticker layer on the timeline to animate with Keyframes.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    var activeProp by rememberSaveable { mutableStateOf(ProKeyframeProperty.SCALE) }
    val isOnKf = target.activeKeyframe != null
    val activeSeg = target.activeSegment

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. Target header + Auto-Keyframe status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Active Keyframe Layer",
                    tint = ElectricCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${target.shortKind}: ${target.title}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (target.keyframes.isNotEmpty()) "Auto-KF ON" else "Tap ◆+ to start",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = if (target.keyframes.isNotEmpty()) ElectricCyan else TextSecondary
            )
        }

        // 2. `<◆` | `◆+` / `◆-` | `◆>` | Clear row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyframeJumpButton(
                label = "<◆",
                contentDesc = "Previous Keyframe",
                enabled = target.hasPrevKeyframe,
                testTagName = "drawer_keyframe_prev_button",
                onClick = onPreviousKeyframe
            )
            StudioActionPill(
                label = if (isOnKf) "◆- Remove Keyframe" else "◆+ Add Keyframe",
                icon = Icons.Default.Diamond,
                accent = if (isOnKf) SignalRed else ElectricCyan,
                onClick = onToggleKeyframe,
                modifier = Modifier
                    .weight(1f)
                    .testTag("drawer_keyframe_toggle_button")
            )
            KeyframeJumpButton(
                label = "◆>",
                contentDesc = "Next Keyframe",
                enabled = target.hasNextKeyframe,
                testTagName = "drawer_keyframe_next_button",
                onClick = onNextKeyframe
            )
            if (target.keyframes.isNotEmpty()) {
                Surface(
                    onClick = onClearAllKeyframes,
                    shape = RoundedCornerShape(8.dp),
                    color = SignalRed.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, SignalRed),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("drawer_keyframe_clear_button")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All Keyframes",
                            tint = SignalRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Easing Curve Selector (shown whenever playhead sits between two Keyframes)
        if (activeSeg != null) {
            val curEasing = activeSeg.first.easing
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OledBlack.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keyframe_easing_selector_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Easing Curve",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.2fs → %.2fs",
                                activeSeg.first.timeMs / 1000f,
                                activeSeg.second.timeMs / 1000f
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ElectricCyan
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyframeEasing.entries.forEach { easing ->
                            val selected = curEasing == easing
                            Surface(
                                onClick = { onSelectEasing(easing) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) ElectricCyan else StudioSurfaceElevated,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) Color.White else StudioBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("keyframe_easing_${easing.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    EasingCurveMiniGraph(
                                        easing = easing,
                                        strokeColor = if (selected) Color.White else ElectricCyan,
                                        modifier = Modifier
                                            .width(26.dp)
                                            .height(14.dp)
                                    )
                                    Text(
                                        text = easing.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = if (selected) Color.White else TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Scrollable Keyframe Diamond Chips
        if (target.keyframes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                target.keyframes.forEachIndexed { idx, kf ->
                    val isCurrent = target.activeKeyframe?.id == kf.id
                    Surface(
                        onClick = { onJumpToTimestamp(target.startMs + kf.timeMs) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) ElectricCyan.copy(alpha = 0.28f) else StudioSurfaceElevated,
                        border = BorderStroke(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) Color.White else ElectricCyan.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("drawer_keyframe_chip_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = if (isCurrent) Color.White else ElectricCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "KF%d %.2fs", idx + 1, kf.timeMs / 1000f),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = { onDeleteKeyframe(kf.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Keyframe ${idx + 1}",
                                    tint = SignalRed,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Transform Property Selector & Live Auto-Keyframing Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProKeyframeProperty.entries.forEach { prop ->
                StudioChipButton(
                    text = prop.label,
                    selected = activeProp == prop,
                    onClick = { activeProp = prop }
                )
            }
        }

        when (activeProp) {
            ProKeyframeProperty.SCALE -> StudioSliderRow(
                label = "Keyframe Scale",
                value = target.currentScale,
                valueRange = 0.25f..2.6f,
                displayValue = "${(target.currentScale * 100).roundToInt()}%"
            ) { v -> onUpdateTransform(null, null, v, null, null) }

            ProKeyframeProperty.POS_X -> {
                val range = if (target.isOverlay) 0.04f..0.96f else -0.8f..0.8f
                val displayPct = if (target.isOverlay) {
                    "${((target.currentX - 0.5f) * 200f).roundToInt()}%"
                } else {
                    "${(target.currentX * 100f).roundToInt()}%"
                }
                StudioSliderRow(
                    label = "Move Left / Right",
                    value = target.currentX.coerceIn(range.start, range.endInclusive),
                    valueRange = range,
                    displayValue = displayPct
                ) { v -> onUpdateTransform(v, null, null, null, null) }
            }

            ProKeyframeProperty.POS_Y -> {
                val range = if (target.isOverlay) 0.04f..0.96f else -0.8f..0.8f
                val displayPct = if (target.isOverlay) {
                    "${((target.currentY - 0.5f) * 200f).roundToInt()}%"
                } else {
                    "${(target.currentY * 100f).roundToInt()}%"
                }
                StudioSliderRow(
                    label = "Move Up / Down",
                    value = target.currentY.coerceIn(range.start, range.endInclusive),
                    valueRange = range,
                    displayValue = displayPct
                ) { v -> onUpdateTransform(null, v, null, null, null) }
            }

            ProKeyframeProperty.ROTATE -> StudioSliderRow(
                label = "Keyframe Rotate",
                value = target.currentRotationDeg.coerceIn(-180f, 180f),
                valueRange = -180f..180f,
                displayValue = "${target.currentRotationDeg.roundToInt()}°"
            ) { v -> onUpdateTransform(null, null, null, v, null) }

            ProKeyframeProperty.OPACITY -> StudioSliderRow(
                label = "Keyframe Opacity",
                value = target.currentOpacity.coerceIn(0f, 1f),
                valueRange = 0f..1f,
                displayValue = "${(target.currentOpacity * 100).roundToInt()}%"
            ) { v -> onUpdateTransform(null, null, null, null, v) }
        }
    }
}

@Composable
private fun EasingCurveMiniGraph(
    easing: KeyframeEasing,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val steps = 16
        val path = Path()
        for (i in 0..steps) {
            val t = i.toFloat() / steps.toFloat()
            val yNorm = VideoMediaEngine.applyKeyframeEasing(t, easing)
            val px = t * size.width
            val py = (1f - yNorm) * size.height
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
