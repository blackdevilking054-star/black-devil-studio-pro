package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.StickerAnimationStyle
import com.example.model.StickerPackCategory
import com.example.model.TextAnimationStyle
import com.example.model.VideoTextOverlay
import com.example.ui.theme.StudioFonts
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class StickerCatalogItem(
    val id: String,
    val label: String,
    val previewGlyph: String,
    val category: StickerPackCategory,
    val badgeId: String?,
    val primaryColorArgb: Int = 0xFFFF1E42.toInt(),
    val secondaryColorArgb: Int = 0xFF00E5FF.toInt()
)

data class OverlayVisualBounds(
    val cx: Float,
    val cy: Float,
    val width: Float,
    val height: Float,
    val rotationDeg: Float
)

object VideoStickerCatalog {
    val items: List<StickerCatalogItem> = listOf(
        // Neon Badges
        StickerCatalogItem("nb_live", "LIVE", "LIVE", StickerPackCategory.BADGES, "badge_live", 0xFFFF1E42.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("nb_new", "NEW", "NEW", StickerPackCategory.BADGES, "badge_new", 0xFF00E5FF.toInt(), 0xFF101018.toInt()),
        StickerCatalogItem("nb_pro", "PRO", "PRO", StickerPackCategory.BADGES, "badge_pro", 0xFFFFB800.toInt(), 0xFF181004.toInt()),
        StickerCatalogItem("nb_4k", "4K HD", "4K HD", StickerPackCategory.BADGES, "badge_4k", 0xFF9B51E0.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("nb_vip", "VIP", "VIP", StickerPackCategory.BADGES, "badge_vip", 0xFFFF2E63.toInt(), 0xFFFFD700.toInt()),
        StickerCatalogItem("nb_hot", "HOT", "HOT", StickerPackCategory.BADGES, "badge_hot", 0xFFFF5500.toInt(), 0xFFFFFFFF.toInt()),

        // Arrows
        StickerCatalogItem("ar_right", "Right Arrow", "➜", StickerPackCategory.ARROWS, "arrow_right", 0xFFFF1E42.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("ar_left", "Left Arrow", "⬅", StickerPackCategory.ARROWS, "arrow_left", 0xFF00E5FF.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("ar_up", "Up Arrow", "⬆", StickerPackCategory.ARROWS, "arrow_up", 0xFF10B981.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("ar_down", "Down Arrow", "⬇", StickerPackCategory.ARROWS, "arrow_down", 0xFFFFB800.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("ar_curve", "Pointer", "↪", StickerPackCategory.ARROWS, "arrow_curved", 0xFFFF2E63.toInt(), 0xFFFFFFFF.toInt()),

        // Fire
        StickerCatalogItem("fi_flame", "Fire", "🔥", StickerPackCategory.FIRE, null),
        StickerCatalogItem("fi_boom", "Explosion", "💥", StickerPackCategory.FIRE, null),
        StickerCatalogItem("fi_bolt", "Lightning", "⚡", StickerPackCategory.FIRE, null),
        StickerCatalogItem("fi_comet", "Comet", "☄️", StickerPackCategory.FIRE, null),
        StickerCatalogItem("fi_badge", "ON FIRE", "🔥 HOT", StickerPackCategory.FIRE, "badge_fire", 0xFFFF4500.toInt(), 0xFFFFD700.toInt()),

        // Stars
        StickerCatalogItem("st_star", "Gold Star", "⭐", StickerPackCategory.STARS, null),
        StickerCatalogItem("st_spark", "Sparkles", "✨", StickerPackCategory.STARS, null),
        StickerCatalogItem("st_glow", "Glow Star", "🌟", StickerPackCategory.STARS, null),
        StickerCatalogItem("st_dizzy", "Orbit Star", "💫", StickerPackCategory.STARS, null),
        StickerCatalogItem("st_top", "TOP RATED", "★ TOP", StickerPackCategory.STARS, "badge_star", 0xFFFFB800.toInt(), 0xFFFFFFFF.toInt()),

        // Subscribe / Like
        StickerCatalogItem("so_sub", "SUBSCRIBE", "▶ SUB", StickerPackCategory.SOCIAL, "social_sub", 0xFFFF0033.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("so_like", "LIKE", "👍 LIKE", StickerPackCategory.SOCIAL, "social_like", 0xFF2563EB.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("so_follow", "FOLLOW +", "+ FOLLOW", StickerPackCategory.SOCIAL, "social_follow", 0xFFE11D48.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("so_bell", "NOTIFY", "🔔 BELL", StickerPackCategory.SOCIAL, "social_bell", 0xFFF59E0B.toInt(), 0xFF140D04.toInt()),
        StickerCatalogItem("so_share", "SHARE", "↗ SHARE", StickerPackCategory.SOCIAL, "social_share", 0xFF10B981.toInt(), 0xFFFFFFFF.toInt()),

        // Emojis
        StickerCatalogItem("em_cool", "Cool", "😎", StickerPackCategory.EMOJIS, null),
        StickerCatalogItem("em_crown", "Crown", "👑", StickerPackCategory.EMOJIS, null),
        StickerCatalogItem("em_clap", "Cinema", "🎬", StickerPackCategory.EMOJIS, null),
        StickerCatalogItem("em_100", "100", "💯", StickerPackCategory.EMOJIS, null),
        StickerCatalogItem("em_rocket", "Rocket", "🚀", StickerPackCategory.EMOJIS, null),
        StickerCatalogItem("em_heart", "Heart", "❤️", StickerPackCategory.EMOJIS, null),
        StickerCatalogItem("em_target", "Target", "🎯", StickerPackCategory.EMOJIS, null),
        StickerCatalogItem("em_music", "Music", "🎵", StickerPackCategory.EMOJIS, null),

        // Frames
        StickerCatalogItem("fr_neon", "Neon Frame", "▭ NEON", StickerPackCategory.FRAMES, "frame_neon", 0xFF00E5FF.toInt(), 0xFFFF1E42.toInt()),
        StickerCatalogItem("fr_rec", "REC Frame", "● REC", StickerPackCategory.FRAMES, "frame_rec", 0xFFFF1E42.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("fr_crimson", "Crimson Box", "▣ RED", StickerPackCategory.FRAMES, "frame_crimson", 0xFFFF1E42.toInt(), 0xFFFFB800.toInt()),
        StickerCatalogItem("fr_hud", "HUD Corners", "⌜ ⌝", StickerPackCategory.FRAMES, "frame_corners", 0xFF00E5FF.toInt(), 0xFFFFFFFF.toInt()),
        StickerCatalogItem("fr_gold", "Gold Frame", "❖ GOLD", StickerPackCategory.FRAMES, "frame_gold", 0xFFFFD700.toInt(), 0xFFFF8C00.toInt())
    )

    fun byCategory(category: StickerPackCategory): List<StickerCatalogItem> =
        items.filter { it.category == category }

    fun byBadgeId(badgeId: String?): StickerCatalogItem? =
        if (badgeId.isNullOrBlank()) null else items.firstOrNull { it.badgeId == badgeId }
}

/**
 * Shared renderer & bounding-box measurer for Pro Text and Pro Sticker overlays on video frames.
 * Ensures the interactive white resize/rotate bounding box hugs the exact rendered pixel bounds.
 */
object VideoOverlayRenderer {

    private val stickerAspectCache = LinkedHashMap<String, Float>(8, 0.75f, true)

    private fun resolveStickerNaturalAspect(context: Context, layer: VideoTextOverlay): Float {
        if (!layer.stickerImageUri.isNullOrBlank()) {
            synchronized(stickerAspectCache) {
                stickerAspectCache[layer.stickerImageUri]?.let { return it }
            }
            val bmp = ImageProcessingEngine.decodeSampledBitmap(context, layer.stickerImageUri, null, 256)
            if (bmp != null && !bmp.isRecycled && bmp.height > 0) {
                val aspect = (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.3f, 3.5f)
                synchronized(stickerAspectCache) {
                    if (stickerAspectCache.size >= 16) {
                        stickerAspectCache.keys.firstOrNull()?.let { stickerAspectCache.remove(it) }
                    }
                    stickerAspectCache[layer.stickerImageUri] = aspect
                }
                return aspect
            }
            return 1f
        }
        val badgeId = layer.stickerBadgeId
        return when {
            badgeId == null -> 1f
            badgeId.startsWith("badge_") -> 2.15f
            badgeId.startsWith("social_") -> 2.55f
            badgeId.startsWith("arrow_") -> 1.45f
            badgeId.startsWith("frame_") -> 1.55f
            else -> 1f
        }
    }

    /**
     * Resolves a Text or Sticker overlay's active transform at [playheadMs] using its Keyframe list
     * when present, or returns [layer] unchanged when it has no Keyframes.
     */
    fun resolveOverlayTransformAt(
        layer: VideoTextOverlay,
        playheadMs: Long?
    ): VideoTextOverlay {
        if (playheadMs == null || layer.keyframes.isEmpty()) return layer
        val dur = (layer.endMs - layer.startMs).coerceAtLeast(1L)
        val localMs = (playheadMs - layer.startMs).coerceIn(0L, dur)
        val kf = VideoMediaEngine.interpolateKeyframes(layer.keyframes, localMs)
        return layer.copy(
            xNorm = kf.xNorm.coerceIn(0.04f, 0.96f),
            yNorm = kf.yNorm.coerceIn(0.04f, 0.96f),
            scale = kf.scale.coerceIn(0.25f, 3.2f),
            rotationDeg = kf.rotationDeg,
            opacity = kf.opacity.coerceIn(0f, 1f)
        )
    }

    /**
     * Exact visual bounding box in viewport coordinates [0..targetW] x [0..targetH].
     * Used BOTH for hit-testing taps on the preview canvas AND sizing the tightly-fitted white transform box.
     */
    fun measureOverlayBounds(
        context: Context,
        layer: VideoTextOverlay,
        targetW: Float,
        targetH: Float,
        playheadMs: Long? = null
    ): OverlayVisualBounds {
        val eff = resolveOverlayTransformAt(layer, playheadMs)
        val minEdge = min(targetW, targetH).coerceAtLeast(1f)
        val cx = (eff.xNorm.coerceIn(0.04f, 0.96f)) * targetW
        val cy = (eff.yNorm.coerceIn(0.04f, 0.96f)) * targetH

        if (eff.isSticker) {
            val cropWFrac = (eff.cropRight - eff.cropLeft).coerceIn(0.15f, 1f)
            val cropHFrac = (eff.cropBottom - eff.cropTop).coerceIn(0.15f, 1f)
            val naturalAspect = resolveStickerNaturalAspect(context, eff)
            val baseHeight = when {
                !eff.stickerImageUri.isNullOrBlank() -> minEdge * 0.28f
                eff.stickerBadgeId?.startsWith("frame_") == true -> minEdge * 0.34f
                eff.stickerBadgeId?.startsWith("badge_") == true ||
                    eff.stickerBadgeId?.startsWith("social_") == true -> minEdge * 0.145f
                eff.stickerBadgeId?.startsWith("arrow_") == true -> minEdge * 0.19f
                else -> minEdge * 0.18f
            }
            val fullH = (baseHeight * eff.scale.coerceIn(0.25f, 3.2f)).coerceIn(28f, targetH * 0.92f)
            val fullW = (fullH * naturalAspect).coerceIn(28f, targetW * 0.92f)
            val croppedW = (fullW * cropWFrac).coerceIn(28f, targetW * 0.94f)
            val croppedH = (fullH * cropHFrac).coerceIn(28f, targetH * 0.94f)
            return OverlayVisualBounds(cx, cy, croppedW, croppedH, eff.rotationDeg)
        } else {
            val tf = StudioFonts.byId(eff.fontId).loadTypeface(context)
            val textSizePx = (minEdge * 0.075f * eff.scale.coerceIn(0.3f, 3.0f)).coerceIn(14f, targetW * 0.36f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = tf
                textSize = textSizePx
            }
            val sample = eff.text.ifBlank { "TEXT" }
            val textW = max(paint.measureText(sample), sample.length * textSizePx * 0.48f)
            val fm = paint.fontMetrics
            val textH = (fm.descent - fm.ascent).coerceAtLeast(textSizePx)
            val padX = if (eff.bgEnabled || eff.isCaption) textSizePx * 0.85f else textSizePx * 0.42f
            val padY = if (eff.bgEnabled || eff.isCaption) textSizePx * 0.48f else textSizePx * 0.32f
            val boxW = (textW + padX).coerceIn(48f, targetW * 0.96f)
            val boxH = (textH + padY).coerceIn(32f, targetH * 0.85f)
            return OverlayVisualBounds(cx, cy, boxW, boxH, eff.rotationDeg)
        }
    }

    fun drawOverlays(
        context: Context,
        canvas: Canvas,
        activeTexts: List<VideoTextOverlay>,
        playheadMs: Long,
        targetW: Int,
        targetH: Int
    ) {
        for (layer in activeTexts) {
            if (playheadMs !in layer.startMs..layer.endMs) continue
            val effective = resolveOverlayTransformAt(layer, playheadMs)
            if (effective.isSticker) {
                drawStickerOverlay(context, canvas, effective, playheadMs, targetW.toFloat(), targetH.toFloat())
            } else {
                drawTextOverlay(context, canvas, effective, playheadMs, targetW.toFloat(), targetH.toFloat())
            }
        }
    }

    private fun drawTextOverlay(
        context: Context,
        canvas: Canvas,
        txt: VideoTextOverlay,
        playheadMs: Long,
        targetW: Float,
        targetH: Float
    ) {
        val speed = txt.animationSpeed.coerceIn(0.3f, 2.5f)
        val elapsed = (playheadMs - txt.startMs).coerceAtLeast(0L)
        val remaining = (txt.endMs - playheadMs).coerceAtLeast(0L)
        val durWindow = (420f / speed).coerceAtLeast(120f)
        val progress = (elapsed / durWindow).coerceIn(0f, 1f)
        val outProgress = (remaining / durWindow).coerceIn(0f, 1f)

        var animAlpha = 1f
        var yShift = 0f
        var xShift = 0f
        var animScale = 1f
        var glitchActive = false
        var glitchStrength = 0f
        var shownText = txt.text

        when (txt.animation) {
            TextAnimationStyle.NONE -> Unit
            TextAnimationStyle.FADE -> {
                animAlpha = min(progress, outProgress)
            }
            TextAnimationStyle.POP -> {
                animScale = if (progress < 0.7f) {
                    val t = progress / 0.7f
                    0.45f + 0.65f * sin(t * PI / 2.0).toFloat()
                } else {
                    val t = (progress - 0.7f) / 0.3f
                    1.10f - 0.10f * t
                }
                animAlpha = (progress * 1.8f).coerceIn(0f, 1f)
            }
            TextAnimationStyle.SLIDE_UP -> {
                val ease = 1f - (1f - progress) * (1f - progress) * (1f - progress)
                animAlpha = progress
                yShift = (1f - ease) * targetH * 0.09f
            }
            TextAnimationStyle.TYPEWRITER -> {
                val typeWindow = (1200f / speed).coerceAtLeast(250f)
                val chars = (txt.text.length * ((elapsed / typeWindow).coerceIn(0.08f, 1f))).roundToInt()
                shownText = txt.text.take(chars.coerceAtLeast(1))
            }
            TextAnimationStyle.NEON_GLITCH -> {
                glitchActive = true
                val phase = (elapsed * 0.014f * speed)
                val burst = abs(sin(phase * 1.9f))
                glitchStrength = (0.35f + 0.65f * burst).coerceIn(0f, 1f)
                xShift = sin(phase * 7.3f) * targetW * 0.008f * glitchStrength
                yShift = cos(phase * 5.1f) * targetH * 0.003f * glitchStrength
                animAlpha = (0.82f + 0.18f * abs(cos(phase * 4.2f))).coerceIn(0.6f, 1f)
            }
        }

        val finalAlpha = (animAlpha * txt.opacity.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        if (finalAlpha <= 0.01f) return

        val minEdge = min(targetW, targetH).coerceAtLeast(1f)
        val tf = StudioFonts.byId(txt.fontId).loadTypeface(context)
        val textSizePx = (minEdge * 0.075f * txt.scale.coerceIn(0.3f, 3.0f) * animScale)
            .coerceIn(14f, targetW * 0.36f)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = textSizePx
            color = txt.colorArgb
            textAlign = Paint.Align.CENTER
            alpha = (finalAlpha * 255f).roundToInt().coerceIn(0, 255)
            if (txt.shadowEnabled) {
                setShadowLayer(
                    textSizePx * 0.16f,
                    0f,
                    textSizePx * 0.07f,
                    Color.argb((finalAlpha * 220f).roundToInt(), 0, 0, 0)
                )
            }
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = textSizePx
            style = Paint.Style.STROKE
            color = txt.strokeArgb
            strokeWidth = (txt.strokeWidth * (targetW / 1080f)).coerceAtLeast(1.5f)
            strokeJoin = Paint.Join.ROUND
            textAlign = Paint.Align.CENTER
            alpha = (finalAlpha * 255f).roundToInt().coerceIn(0, 255)
        }

        val cx = txt.xNorm.coerceIn(0.04f, 0.96f) * targetW + xShift
        val cy = txt.yNorm.coerceIn(0.04f, 0.96f) * targetH + yShift
        val fm = fillPaint.fontMetrics
        val baselineOffset = -(fm.ascent + fm.descent) / 2f

        canvas.save()
        canvas.translate(cx, cy)
        if (abs(txt.rotationDeg) > 0.1f) canvas.rotate(txt.rotationDeg)

        // Optional Background Label Pill
        if (txt.isCaption || txt.bgEnabled) {
            val textW = fillPaint.measureText(shownText.ifBlank { " " })
            val textH = (fm.descent - fm.ascent).coerceAtLeast(textSizePx)
            val padX = textSizePx * 0.44f
            val padY = textSizePx * 0.24f
            val pillRect = RectF(
                -(textW / 2f + padX),
                -(textH / 2f + padY),
                textW / 2f + padX,
                textH / 2f + padY
            )
            val baseC = if (txt.bgEnabled) txt.bgColorArgb else Color.argb(200, 12, 7, 16)
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(
                    (finalAlpha * Color.alpha(baseC)).roundToInt().coerceIn(0, 255),
                    Color.red(baseC),
                    Color.green(baseC),
                    Color.blue(baseC)
                )
            }
            val radius = min(pillRect.height() * 0.42f, 24f)
            canvas.drawRoundRect(pillRect, radius, radius, pillPaint)
        }

        // Neon Glitch Chromatic RGB Offset Pass
        if (glitchActive) {
            val splitPx = textSizePx * 0.08f * glitchStrength
            val redPaint = Paint(fillPaint).apply {
                color = Color.rgb(255, 30, 66)
                alpha = (finalAlpha * 180f).roundToInt().coerceIn(0, 255)
            }
            val cyanPaint = Paint(fillPaint).apply {
                color = Color.rgb(0, 229, 255)
                alpha = (finalAlpha * 180f).roundToInt().coerceIn(0, 255)
            }
            canvas.drawText(shownText, -splitPx, baselineOffset - splitPx * 0.25f, redPaint)
            canvas.drawText(shownText, splitPx, baselineOffset + splitPx * 0.25f, cyanPaint)
        }

        if (txt.strokeWidth > 0.5f) {
            canvas.drawText(shownText, 0f, baselineOffset, strokePaint)
        }
        canvas.drawText(shownText, 0f, baselineOffset, fillPaint)
        canvas.restore()
    }

    private fun drawStickerOverlay(
        context: Context,
        canvas: Canvas,
        stk: VideoTextOverlay,
        playheadMs: Long,
        targetW: Float,
        targetH: Float
    ) {
        val speed = stk.animationSpeed.coerceIn(0.3f, 2.5f)
        val elapsed = (playheadMs - stk.startMs).coerceAtLeast(0L)
        val tSec = (elapsed / 1000f) * speed

        var animScale = 1f
        var animYShift = 0f
        var animRotDeg = 0f
        var animAlpha = 1f

        when (stk.stickerAnimation) {
            StickerAnimationStyle.NONE -> Unit
            StickerAnimationStyle.BOUNCE -> {
                val bounce = abs(sin(tSec * PI * 2.2)).toFloat()
                animYShift = -bounce * targetH * 0.055f
                animScale = 1f + (0.08f * (1f - bounce))
            }
            StickerAnimationStyle.PULSE -> {
                animScale = 1f + 0.14f * sin(tSec * PI * 2.4).toFloat()
            }
            StickerAnimationStyle.SPIN -> {
                animRotDeg = (tSec * 140f) % 360f
            }
            StickerAnimationStyle.FLOAT -> {
                animYShift = sin(tSec * PI * 1.5).toFloat() * targetH * 0.036f
            }
            StickerAnimationStyle.POP -> {
                val p = (tSec / 0.36f).coerceIn(0f, 1f)
                animScale = if (p < 0.7f) {
                    0.35f + 0.80f * sin((p / 0.7f) * PI / 2.0).toFloat()
                } else {
                    1.15f - 0.15f * ((p - 0.7f) / 0.3f)
                }
                animAlpha = (p * 2f).coerceIn(0f, 1f)
            }
        }

        val finalAlpha = (animAlpha * stk.opacity.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        if (finalAlpha <= 0.01f) return

        val bounds = measureOverlayBounds(context, stk, targetW, targetH, playheadMs = null)
        val drawW = (bounds.width * animScale).coerceAtLeast(16f)
        val drawH = (bounds.height * animScale).coerceAtLeast(16f)
        val cx = bounds.cx
        val cy = bounds.cy + animYShift

        val cropL = stk.cropLeft.coerceIn(0f, 0.85f)
        val cropT = stk.cropTop.coerceIn(0f, 0.85f)
        val cropR = stk.cropRight.coerceIn((cropL + 0.15f).coerceAtMost(1f), 1f)
        val cropB = stk.cropBottom.coerceIn((cropT + 0.15f).coerceAtMost(1f), 1f)
        val cropWFrac = (cropR - cropL).coerceAtLeast(0.15f)
        val cropHFrac = (cropB - cropT).coerceAtLeast(0.15f)

        canvas.save()
        canvas.translate(cx, cy)
        val totalRot = stk.rotationDeg + animRotDeg
        if (abs(totalRot) > 0.1f) canvas.rotate(totalRot)
        if (stk.flipH || stk.flipV) {
            canvas.scale(if (stk.flipH) -1f else 1f, if (stk.flipV) -1f else 1f)
        }

        val destRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)

        if (!stk.stickerImageUri.isNullOrBlank()) {
            val maxDim = max(drawW, drawH).roundToInt().coerceIn(128, 768)
            val bmp = ImageProcessingEngine.decodeSampledBitmap(context, stk.stickerImageUri, null, maxDim)
            if (bmp != null && !bmp.isRecycled) {
                val srcRect = Rect(
                    (bmp.width * cropL).roundToInt().coerceIn(0, bmp.width - 1),
                    (bmp.height * cropT).roundToInt().coerceIn(0, bmp.height - 1),
                    (bmp.width * cropR).roundToInt().coerceIn(1, bmp.width),
                    (bmp.height * cropB).roundToInt().coerceIn(1, bmp.height)
                )
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (finalAlpha * 255f).roundToInt().coerceIn(0, 255)
                }
                canvas.drawBitmap(bmp, srcRect, destRect, paint)
            }
        } else {
            // Clip to the cropped bounds and translate virtual uncropped sticker so crop region is dead-center
            canvas.save()
            canvas.clipRect(destRect)
            val fullW = drawW / cropWFrac
            val fullH = drawH / cropHFrac
            val cropCenterX = (cropL + cropR) / 2f
            val cropCenterY = (cropT + cropB) / 2f
            canvas.translate((0.5f - cropCenterX) * fullW, (0.5f - cropCenterY) * fullH)
            val fullRect = RectF(-fullW / 2f, -fullH / 2f, fullW / 2f, fullH / 2f)
            val alphaInt = (finalAlpha * 255f).roundToInt().coerceIn(0, 255)

            if (!stk.stickerBadgeId.isNullOrBlank()) {
                drawProceduralSticker(canvas, stk.stickerBadgeId, stk.text, fullRect, alphaInt)
            } else {
                // Crisp Emoji / Symbol Sticker
                val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = min(fullW, fullH) * 0.78f
                    textAlign = Paint.Align.CENTER
                    alpha = alphaInt
                }
                val fm = emojiPaint.fontMetrics
                val baseY = -(fm.ascent + fm.descent) / 2f
                canvas.drawText(stk.text, 0f, baseY, emojiPaint)
            }
            canvas.restore()
        }

        canvas.restore()
    }

    private fun drawProceduralSticker(
        canvas: Canvas,
        badgeId: String,
        label: String,
        rect: RectF,
        alphaInt: Int
    ) {
        val item = VideoStickerCatalog.byBadgeId(badgeId)
        val primary = item?.primaryColorArgb ?: 0xFFFF1E42.toInt()
        val secondary = item?.secondaryColorArgb ?: 0xFFFFFFFF.toInt()
        val pColor = Color.argb(alphaInt, Color.red(primary), Color.green(primary), Color.blue(primary))
        val sColor = Color.argb(alphaInt, Color.red(secondary), Color.green(secondary), Color.blue(secondary))

        when {
            badgeId.startsWith("badge_") || badgeId.startsWith("social_") -> {
                val radius = rect.height() * 0.45f
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pColor
                    setShadowLayer(rect.height() * 0.22f, 0f, 0f, pColor)
                }
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (rect.height() * 0.07f).coerceAtLeast(2f)
                    color = sColor
                }
                val innerRect = RectF(
                    rect.left + borderPaint.strokeWidth,
                    rect.top + borderPaint.strokeWidth,
                    rect.right - borderPaint.strokeWidth,
                    rect.bottom - borderPaint.strokeWidth
                )
                canvas.drawRoundRect(innerRect, radius, radius, bgPaint)
                canvas.drawRoundRect(innerRect, radius, radius, borderPaint)

                val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.DEFAULT_BOLD
                    color = sColor
                    textAlign = Paint.Align.CENTER
                    textSize = (rect.height() * 0.48f).coerceAtLeast(12f)
                }
                val displayTxt = item?.previewGlyph ?: label
                val fm = txtPaint.fontMetrics
                canvas.drawText(displayTxt, 0f, -(fm.ascent + fm.descent) / 2f, txtPaint)
            }

            badgeId.startsWith("arrow_") -> {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pColor
                    style = Paint.Style.FILL
                    setShadowLayer(rect.height() * 0.14f, 0f, 0f, pColor)
                }
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = sColor
                    style = Paint.Style.STROKE
                    strokeWidth = (min(rect.width(), rect.height()) * 0.06f).coerceAtLeast(2f)
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.save()
                when (badgeId) {
                    "arrow_left" -> canvas.rotate(180f)
                    "arrow_up" -> canvas.rotate(-90f)
                    "arrow_down" -> canvas.rotate(90f)
                    "arrow_curved" -> canvas.rotate(-25f)
                }
                val w = rect.width() * 0.86f
                val h = rect.height() * 0.72f
                val path = Path().apply {
                    moveTo(-w / 2f, -h * 0.22f)
                    lineTo(w * 0.08f, -h * 0.22f)
                    lineTo(w * 0.08f, -h * 0.5f)
                    lineTo(w / 2f, 0f)
                    lineTo(w * 0.08f, h * 0.5f)
                    lineTo(w * 0.08f, h * 0.22f)
                    lineTo(-w / 2f, h * 0.22f)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
                canvas.restore()
            }

            badgeId.startsWith("frame_") -> {
                val strokeW = (min(rect.width(), rect.height()) * 0.055f).coerceAtLeast(3f)
                val inset = strokeW
                val fRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pColor
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    setShadowLayer(strokeW * 2.2f, 0f, 0f, pColor)
                }
                if (badgeId == "frame_corners") {
                    val seg = min(fRect.width(), fRect.height()) * 0.28f
                    // 4 HUD Corner Brackets
                    canvas.drawLine(fRect.left, fRect.top, fRect.left + seg, fRect.top, framePaint)
                    canvas.drawLine(fRect.left, fRect.top, fRect.left, fRect.top + seg, framePaint)
                    canvas.drawLine(fRect.right, fRect.top, fRect.right - seg, fRect.top, framePaint)
                    canvas.drawLine(fRect.right, fRect.top, fRect.right, fRect.top + seg, framePaint)
                    canvas.drawLine(fRect.left, fRect.bottom, fRect.left + seg, fRect.bottom, framePaint)
                    canvas.drawLine(fRect.left, fRect.bottom, fRect.left, fRect.bottom - seg, framePaint)
                    canvas.drawLine(fRect.right, fRect.bottom, fRect.right - seg, fRect.bottom, framePaint)
                    canvas.drawLine(fRect.right, fRect.bottom, fRect.right, fRect.bottom - seg, framePaint)
                } else {
                    canvas.drawRoundRect(fRect, strokeW * 2f, strokeW * 2f, framePaint)
                    if (badgeId == "frame_rec") {
                        val dotR = (fRect.height() * 0.075f).coerceAtLeast(4f)
                        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pColor }
                        canvas.drawCircle(fRect.left + dotR * 2.4f, fRect.top + dotR * 2.4f, dotR, dotPaint)
                        val recPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            typeface = Typeface.DEFAULT_BOLD
                            color = sColor
                            textSize = dotR * 2.3f
                        }
                        canvas.drawText("REC", fRect.left + dotR * 4.2f, fRect.top + dotR * 3.1f, recPaint)
                    }
                }
            }
        }
    }
}
