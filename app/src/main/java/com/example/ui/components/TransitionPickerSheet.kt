package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransitionCategory
import com.example.model.VideoTransition
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ProAmber
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

/**
 * CapCut / VN style Visual Animated Transition Picker:
 * - Grouped into 7 category tabs (Basic, Camera, Glitch, Spin/Zoom, Shake, Split, Light Flash)
 * - 40 transitions rendered as live animated mini preview cards
 * - Adjustable Duration slider (0.1s - 3.0s)
 */
@Composable
fun TransitionPickerPanel(
    selectedTransition: VideoTransition,
    durationMs: Long,
    onSelectTransition: (VideoTransition) -> Unit,
    onDurationChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by rememberSaveable(selectedTransition) {
        mutableStateOf(selectedTransition.category)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "transition_card_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "transition_cycle"
    )

    val filteredTransitions = remember(activeCategory) {
        VideoTransition.entries.filter { it.category == activeCategory }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 7 Category Tabs: Basic | Camera | Glitch | Spin/Zoom | Shake | Split | Light Flash
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransitionCategory.entries.forEach { cat ->
                StudioChipButton(
                    text = cat.label,
                    selected = activeCategory == cat,
                    onClick = { activeCategory = cat },
                    modifier = Modifier.testTag("transition_cat_${cat.name.lowercase()}")
                )
            }
        }

        // Visual Animated Preview Cards Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredTransitions.forEach { transition ->
                VisualTransitionCard(
                    transition = transition,
                    selected = transition == selectedTransition,
                    progress = animProgress,
                    onClick = { onSelectTransition(transition) }
                )
            }
        }

        // Duration Slider (0.1s - 3.0s)
        val clampedSec = (durationMs / 1000f).coerceIn(0.1f, 3.0f)
        StudioSliderRow(
            label = "Transition Duration",
            value = clampedSec,
            valueRange = 0.1f..3.0f,
            displayValue = String.format(Locale.US, "%.1fs", clampedSec)
        ) { sec ->
            onDurationChange((sec * 1000f).toLong().coerceIn(100L, 3000L))
        }
    }
}

@Composable
private fun VisualTransitionCard(
    transition: VideoTransition,
    selected: Boolean,
    progress: Float,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ElectricCyanContainer else StudioSurfaceElevated,
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) ElectricCyan else StudioBorder
        ),
        modifier = Modifier
            .width(84.dp)
            .testTag("transition_card_${transition.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(DeepObsidian)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val clipABrush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0099CC), Color(0xFF0B2239))
                    )
                    val clipBBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFFA855F7), Color(0xFFFF1E42))
                    )
                    val t = progress
                    val pulse = 1f - abs(t * 2f - 1f)

                    when (transition) {
                        // BASIC
                        VideoTransition.NONE -> {
                            drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                        }
                        VideoTransition.CROSSFADE -> {
                            drawRect(clipABrush)
                            drawRect(clipBBrush, alpha = t)
                        }
                        VideoTransition.DIP_TO_BLACK -> {
                            drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                            drawRect(Color.Black.copy(alpha = pulse))
                        }
                        VideoTransition.DIP_TO_WHITE -> {
                            drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                            drawRect(Color.White.copy(alpha = pulse))
                        }
                        VideoTransition.WIPE_LEFT -> {
                            drawRect(clipABrush)
                            clipRect(left = w * (1f - t), top = 0f, right = w, bottom = h) {
                                drawRect(clipBBrush)
                            }
                        }
                        VideoTransition.WIPE_RIGHT -> {
                            drawRect(clipABrush)
                            clipRect(left = 0f, top = 0f, right = w * t, bottom = h) {
                                drawRect(clipBBrush)
                            }
                        }
                        VideoTransition.WIPE_UP -> {
                            drawRect(clipABrush)
                            clipRect(left = 0f, top = h * (1f - t), right = w, bottom = h) {
                                drawRect(clipBBrush)
                            }
                        }
                        VideoTransition.WIPE_DOWN -> {
                            drawRect(clipABrush)
                            clipRect(left = 0f, top = 0f, right = w, bottom = h * t) {
                                drawRect(clipBBrush)
                            }
                        }

                        // CAMERA
                        VideoTransition.WHIP_PAN, VideoTransition.SLIDE_LEFT -> {
                            val dx = w * t
                            drawRect(clipABrush, topLeft = Offset(-dx, 0f), size = Size(w, h))
                            drawRect(clipBBrush, topLeft = Offset(w - dx, 0f), size = Size(w, h))
                            if (transition == VideoTransition.WHIP_PAN) {
                                drawLine(
                                    color = Color.White.copy(alpha = pulse * 0.7f),
                                    start = Offset(w * 0.15f, h * 0.35f),
                                    end = Offset(w * 0.85f, h * 0.35f),
                                    strokeWidth = 3f
                                )
                            }
                        }
                        VideoTransition.SLIDE_RIGHT -> {
                            val dx = w * t
                            drawRect(clipABrush, topLeft = Offset(dx, 0f), size = Size(w, h))
                            drawRect(clipBBrush, topLeft = Offset(dx - w, 0f), size = Size(w, h))
                        }
                        VideoTransition.WHIP_UP, VideoTransition.SLIDE_UP -> {
                            val dy = h * t
                            drawRect(clipABrush, topLeft = Offset(0f, -dy), size = Size(w, h))
                            drawRect(clipBBrush, topLeft = Offset(0f, h - dy), size = Size(w, h))
                            if (transition == VideoTransition.WHIP_UP) {
                                drawLine(
                                    color = Color.White.copy(alpha = pulse * 0.7f),
                                    start = Offset(w * 0.5f, h * 0.15f),
                                    end = Offset(w * 0.5f, h * 0.85f),
                                    strokeWidth = 3f
                                )
                            }
                        }
                        VideoTransition.SLIDE_DOWN -> {
                            val dy = h * t
                            drawRect(clipABrush, topLeft = Offset(0f, dy), size = Size(w, h))
                            drawRect(clipBBrush, topLeft = Offset(0f, dy - h), size = Size(w, h))
                        }

                        // GLITCH
                        VideoTransition.GLITCH_RGB,
                        VideoTransition.CHROMATIC,
                        VideoTransition.PIXEL_SPLIT,
                        VideoTransition.SCANLINE,
                        VideoTransition.VHS_TRACKING -> {
                            drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                            val shift = pulse * 7.5f
                            drawRect(
                                SignalRed.copy(alpha = 0.5f * pulse),
                                topLeft = Offset(-shift, h * 0.18f),
                                size = Size(w, h * 0.24f)
                            )
                            drawRect(
                                Color(0xFF00E5FF).copy(alpha = 0.5f * pulse),
                                topLeft = Offset(shift, h * 0.55f),
                                size = Size(w, h * 0.24f)
                            )
                            if (transition == VideoTransition.SCANLINE || transition == VideoTransition.VHS_TRACKING) {
                                val step = if (transition == VideoTransition.VHS_TRACKING) 6 else 8
                                for (y in 3 until h.toInt() step step) {
                                    drawLine(
                                        Color.Black.copy(alpha = 0.45f),
                                        Offset(0f, y.toFloat()),
                                        Offset(w, y.toFloat()),
                                        strokeWidth = 2f
                                    )
                                }
                                if (transition == VideoTransition.VHS_TRACKING) {
                                    drawRect(
                                        Color.White.copy(alpha = 0.35f * pulse),
                                        topLeft = Offset(0f, h * t),
                                        size = Size(w, 4f)
                                    )
                                }
                            }
                        }
                        VideoTransition.STROBE -> {
                            val flashOn = ((t * 10f).toInt() % 2 == 0)
                            if (flashOn) drawRect(Color.White) else drawRect(clipBBrush)
                        }

                        // SPIN / ZOOM
                        VideoTransition.ZOOM_IN -> {
                            scale(1f + t * 0.45f) {
                                drawRect(clipABrush, alpha = 1f - t)
                            }
                            scale(0.75f + t * 0.25f) {
                                drawRect(clipBBrush, alpha = t)
                            }
                        }
                        VideoTransition.ZOOM_OUT -> {
                            scale(1.45f - t * 0.45f) {
                                drawRect(clipBBrush, alpha = t)
                            }
                            scale(1f - t * 0.25f) {
                                drawRect(clipABrush, alpha = 1f - t)
                            }
                        }
                        VideoTransition.SPIN_360 -> {
                            rotate(degrees = t * 180f) {
                                drawRoundRect(
                                    brush = if (t < 0.5f) clipABrush else clipBBrush,
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                            }
                        }
                        VideoTransition.SPIN_LEFT -> {
                            rotate(degrees = -t * 180f) {
                                drawRoundRect(
                                    brush = if (t < 0.5f) clipABrush else clipBBrush,
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                            }
                        }
                        VideoTransition.SWIRL_ZOOM -> {
                            rotate(degrees = pulse * 140f) {
                                scale(1f + pulse * 0.55f) {
                                    drawRoundRect(
                                        brush = if (t < 0.5f) clipABrush else clipBBrush,
                                        cornerRadius = CornerRadius(12f, 12f)
                                    )
                                }
                            }
                        }
                        VideoTransition.CRASH_ZOOM -> {
                            scale(1f + pulse * 0.9f) {
                                drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                            }
                            drawRect(Color.White.copy(alpha = pulse * 0.22f))
                        }

                        // SHAKE
                        VideoTransition.SHAKE -> {
                            val sx = sin(t * 24f) * 6f
                            val sy = sin(t * 19f) * 4f
                            drawRect(
                                brush = if (t < 0.5f) clipABrush else clipBBrush,
                                topLeft = Offset(sx, sy),
                                size = Size(w, h)
                            )
                        }
                        VideoTransition.EARTHQUAKE -> {
                            val sx = sin(t * 44f) * 8f * pulse
                            val sy = sin(t * 36f) * 7f * pulse
                            drawRect(
                                brush = if (t < 0.5f) clipABrush else clipBBrush,
                                topLeft = Offset(sx, sy),
                                size = Size(w, h)
                            )
                        }
                        VideoTransition.IMPACT_HIT -> {
                            val hitScale = 1f + pulse * 0.35f
                            val sy = sin(t * 30f) * 5f * pulse
                            scale(hitScale) {
                                drawRect(
                                    brush = if (t < 0.5f) clipABrush else clipBBrush,
                                    topLeft = Offset(0f, sy),
                                    size = Size(w, h)
                                )
                            }
                            drawRect(SignalRed.copy(alpha = pulse * 0.28f))
                        }
                        VideoTransition.HANDHELD_JOLT -> {
                            val sx = sin(t * 18f) * 5f
                            rotate(degrees = sin(t * 22f) * 6f * pulse) {
                                drawRect(
                                    brush = if (t < 0.5f) clipABrush else clipBBrush,
                                    topLeft = Offset(sx, 0f),
                                    size = Size(w, h)
                                )
                            }
                        }

                        // SPLIT
                        VideoTransition.SPLIT -> {
                            drawRect(clipBBrush)
                            val half = (w * 0.5f) * (1f - t)
                            drawRect(clipABrush, topLeft = Offset.Zero, size = Size(half, h))
                            drawRect(clipABrush, topLeft = Offset(w - half, 0f), size = Size(half, h))
                        }
                        VideoTransition.SPLIT_VERTICAL -> {
                            drawRect(clipBBrush)
                            val halfH = (h * 0.5f) * (1f - t)
                            drawRect(clipABrush, topLeft = Offset.Zero, size = Size(w, halfH))
                            drawRect(clipABrush, topLeft = Offset(0f, h - halfH), size = Size(w, halfH))
                        }
                        VideoTransition.CURTAIN_OPEN -> {
                            drawRect(clipBBrush)
                            val half = (w * 0.5f) * (1f - t)
                            drawRect(clipABrush, topLeft = Offset.Zero, size = Size(half, h))
                            drawRect(clipABrush, topLeft = Offset(w - half, 0f), size = Size(half, h))
                            drawLine(Color.Black, Offset(half, 0f), Offset(half, h), strokeWidth = 2f)
                            drawLine(Color.Black, Offset(w - half, 0f), Offset(w - half, h), strokeWidth = 2f)
                        }
                        VideoTransition.SHUTTER_SPLIT -> {
                            drawRect(clipBBrush)
                            val bandH = h / 4f
                            val closedW = w * (1f - t)
                            for (i in 0 until 4) {
                                val x = if (i % 2 == 0) 0f else w - closedW
                                drawRect(clipABrush, topLeft = Offset(x, i * bandH), size = Size(closedW, bandH))
                            }
                        }

                        // LIGHT FLASH
                        VideoTransition.FLASH, VideoTransition.RADIAL_FLASH -> {
                            drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                            drawCircle(
                                color = Color.White.copy(alpha = pulse * 0.9f),
                                radius = w * (0.25f + pulse * 0.55f),
                                center = Offset(w * 0.5f, h * 0.5f)
                            )
                        }
                        VideoTransition.CRIMSON_FLASH -> {
                            drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                            drawCircle(
                                color = SignalRed.copy(alpha = pulse * 0.92f),
                                radius = w * (0.3f + pulse * 0.6f),
                                center = Offset(w * 0.5f, h * 0.5f)
                            )
                        }
                        VideoTransition.BLUR -> {
                            drawRect(clipABrush, alpha = 1f - t)
                            drawRect(clipBBrush, alpha = t)
                            drawRect(Color.White.copy(alpha = pulse * 0.25f))
                        }
                        VideoTransition.LIGHT_LEAK, VideoTransition.GLOW_BURN -> {
                            drawRect(if (t < 0.5f) clipABrush else clipBBrush)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        ProAmber.copy(alpha = pulse * 0.85f),
                                        NeonViolet.copy(alpha = pulse * 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }
                    }
                }
            }

            Text(
                text = transition.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) ElectricCyan else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
