package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.Surface
import com.example.R
import com.example.model.HardwareEncoderCapabilities
import com.example.model.SpeedCurvePreset
import com.example.model.VideoClip
import com.example.model.VideoEditorState
import com.example.model.VideoExportFps
import com.example.model.VideoExportResolution
import com.example.model.VideoKeyframe
import com.example.model.VideoTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

data class VideoMetadataInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int
)

data class InterpolatedKeyframe(
    val xNorm: Float = 0f,
    val yNorm: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f
)

object VideoMediaEngine {

    fun queryHardwareEncoderCapabilities(): HardwareEncoderCapabilities {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val avcEncoders = codecList.codecInfos.filter { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
            }
            if (avcEncoders.isEmpty()) {
                return HardwareEncoderCapabilities(encoderName = "Software Fallback", supports4K30 = false, supports4K60 = false, supports1080p60 = true)
            }

            val sorted = avcEncoders.sortedByDescending { info ->
                val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.isHardwareAccelerated
                } else {
                    val n = info.name.lowercase()
                    !n.startsWith("omx.google.") && !n.startsWith("c2.android.")
                }
                if (isHw) 1 else 0
            }

            var bestName = sorted.first().name
            var supports4K30 = false
            var supports4K60 = false
            var supports1080p60 = false
            var maxW = 1920
            var maxH = 1080

            for (info in sorted) {
                val caps = runCatching { info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC) }.getOrNull() ?: continue
                val vCaps = caps.videoCapabilities ?: continue
                val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.isHardwareAccelerated
                } else {
                    val n = info.name.lowercase()
                    !n.startsWith("omx.google.") && !n.startsWith("c2.android.")
                }

                val wUpper = vCaps.supportedWidths.upper
                val hUpper = vCaps.supportedHeights.upper
                if (wUpper >= maxW) {
                    maxW = wUpper
                    maxH = hUpper
                    if (isHw) bestName = info.name
                }
                if (isHw) {
                    if (vCaps.areSizeAndRateSupported(3840, 2160, 30.0)) supports4K30 = true
                    if (vCaps.areSizeAndRateSupported(3840, 2160, 60.0)) supports4K60 = true
                    if (vCaps.areSizeAndRateSupported(1920, 1080, 60.0)) supports1080p60 = true
                }
            }

            HardwareEncoderCapabilities(
                encoderName = bestName.substringAfterLast(".").ifBlank { bestName },
                supports4K30 = supports4K30,
                supports4K60 = supports4K60,
                supports1080p60 = supports1080p60 || sorted.isNotEmpty(),
                maxWidth = maxW,
                maxHeight = maxH
            )
        } catch (_: Exception) {
            HardwareEncoderCapabilities()
        }
    }

    fun probeVideoMetadata(context: Context, uriString: String): VideoMetadataInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uriString))
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 5000L
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val swapped = rot == 90 || rot == 270
            val w = if (swapped) rawH else rawW
            val h = if (swapped) rawW else rawH
            VideoMetadataInfo(dur.coerceAtLeast(500L), w, h)
        } catch (_: Exception) {
            VideoMetadataInfo(5000L, 1280, 720)
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Extracts low-RAM 72x44 filmstrip thumbnails for a clip (safe for 2GB/3GB RAM devices).
     */
    fun extractFilmstripThumbnails(
        context: Context,
        clip: VideoClip,
        count: Int = 5
    ): List<Bitmap> {
        val safeCount = count.coerceIn(2, 6)
        val span = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(300L)
        val thumbs = ArrayList<Bitmap>(safeCount)

        if (!clip.uriString.isNullOrBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(clip.uriString))
                for (i in 0 until safeCount) {
                    val ms = clip.trimStartMs + (span * i) / max(1, safeCount - 1)
                    val frame = retriever.getFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frame != null) {
                        val small = Bitmap.createScaledBitmap(frame, 76, 46, true)
                        if (small != frame) frame.recycle()
                        thumbs.add(small)
                    }
                }
            } catch (_: Exception) {
                // Fall back to sample drawable below
            } finally {
                runCatching { retriever.release() }
            }
        }

        if (thumbs.isEmpty()) {
            val fallback = ImageProcessingEngine.decodeSampledBitmap(
                context = context,
                uriString = null,
                fallbackDrawable = clip.fallbackDrawable ?: R.drawable.img_studio_sample_bg_1795185351810,
                maxDimension = 128
            )
            if (fallback != null) {
                val small = Bitmap.createScaledBitmap(fallback, 76, 46, true)
                repeat(safeCount) { thumbs.add(small) }
            }
        }
        return thumbs
    }

    /**
     * Maps normalized timeline progress through cinematic Speed Curve Ramping presets.
     */
    fun remapProgressWithSpeedCurve(u: Float, curve: SpeedCurvePreset): Float {
        val x = u.coerceIn(0f, 1f)
        return when (curve) {
            SpeedCurvePreset.NORMAL -> x
            SpeedCurvePreset.MONTAGE -> (x.pow(0.72f) * 0.5f + x.pow(1.45f) * 0.5f).coerceIn(0f, 1f)
            SpeedCurvePreset.HERO -> {
                // Fast intro -> dramatic slow-motion center -> fast exit
                val slowCenter = x - 0.14f * sin(2f * PI.toFloat() * x)
                slowCenter.coerceIn(0f, 1f)
            }
            SpeedCurvePreset.BULLET -> (1f - (1f - x).pow(2.2f)).coerceIn(0f, 1f)
            SpeedCurvePreset.JUMP_CUT -> ((x * 5f).toInt() / 5f + (x * 5f % 1f) * 0.45f).coerceIn(0f, 1f)
            SpeedCurvePreset.FLASH -> (x.pow(0.55f)).coerceIn(0f, 1f)
        }
    }

    /**
     * Smoothly interpolates Diamond Keyframe transforms across a clip's local playhead.
     */
    fun interpolateKeyframes(keyframes: List<VideoKeyframe>, localClipMs: Long): InterpolatedKeyframe {
        if (keyframes.isEmpty()) return InterpolatedKeyframe()
        val sorted = keyframes.sortedBy { it.timeMs }
        if (sorted.size == 1 || localClipMs <= sorted.first().timeMs) {
            val k = sorted.first()
            return InterpolatedKeyframe(k.xNorm, k.yNorm, k.scale, k.rotationDeg, k.opacity)
        }
        if (localClipMs >= sorted.last().timeMs) {
            val k = sorted.last()
            return InterpolatedKeyframe(k.xNorm, k.yNorm, k.scale, k.rotationDeg, k.opacity)
        }
        for (i in 0 until sorted.lastIndex) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (localClipMs in a.timeMs..b.timeMs) {
                val span = (b.timeMs - a.timeMs).coerceAtLeast(1L).toFloat()
                val t = ((localClipMs - a.timeMs) / span).coerceIn(0f, 1f)
                return InterpolatedKeyframe(
                    xNorm = a.xNorm + (b.xNorm - a.xNorm) * t,
                    yNorm = a.yNorm + (b.yNorm - a.yNorm) * t,
                    scale = a.scale + (b.scale - a.scale) * t,
                    rotationDeg = a.rotationDeg + (b.rotationDeg - a.rotationDeg) * t,
                    opacity = a.opacity + (b.opacity - a.opacity) * t
                )
            }
        }
        return InterpolatedKeyframe()
    }

    private data class CachedSourceFrame(
        val key: String,
        val bitmap: Bitmap
    )

    private val retrieverLock = Any()
    private val retrieverCache = LinkedHashMap<String, MediaMetadataRetriever>(3, 0.75f, true)
    @Volatile
    private var cachedFrame: CachedSourceFrame? = null

    @Volatile
    var lastRenderedSourceAspect: Float = 16f / 9f
        private set

    private fun getOrOpenRetriever(context: Context, uriString: String): MediaMetadataRetriever? {
        synchronized(retrieverLock) {
            retrieverCache[uriString]?.let { return it }
            return try {
                val r = MediaMetadataRetriever()
                r.setDataSource(context, Uri.parse(uriString))
                if (retrieverCache.size >= 2) {
                    val oldestKey = retrieverCache.keys.firstOrNull()
                    if (oldestKey != null) {
                        runCatching { retrieverCache.remove(oldestKey)?.release() }
                    }
                }
                retrieverCache[uriString] = r
                r
            } catch (_: Exception) {
                null
            }
        }
    }

    fun extractClipFrame(
        context: Context,
        clip: VideoClip,
        localClipMs: Long,
        maxEdge: Int
    ): Bitmap {
        val safeStart = clip.trimStartMs.coerceAtLeast(0L)
        val safeEnd = clip.trimEndMs.coerceAtLeast(safeStart)
        val clampedMs = if (clip.reversed) {
            (safeEnd - localClipMs).coerceIn(safeStart, safeEnd)
        } else {
            (safeStart + localClipMs).coerceIn(safeStart, safeEnd)
        }

        // Quantize preview frame lookup to 65ms buckets so 30fps FX/Transition animations run silky-smooth
        val bucketMs = if (maxEdge <= 800) (clampedMs / 65L) * 65L else clampedMs
        val cacheKey = "${clip.id}|${clip.uriString ?: clip.fallbackDrawable}|$bucketMs|$maxEdge"
        val existing = cachedFrame
        if (existing != null && existing.key == cacheKey && !existing.bitmap.isRecycled) {
            return existing.bitmap
        }

        if (!clip.uriString.isNullOrBlank()) {
            val retriever = getOrOpenRetriever(context, clip.uriString)
            if (retriever != null) {
                try {
                    val frame = synchronized(retrieverLock) {
                        retriever.getFrameAtTime(
                            bucketMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                    }
                    if (frame != null) {
                        val scaled = scaleDownIfNeeded(frame, maxEdge)
                        if (scaled != frame) frame.recycle()
                        if (maxEdge <= 800) {
                            cachedFrame = CachedSourceFrame(cacheKey, scaled)
                        }
                        return scaled
                    }
                } catch (_: Exception) {
                    synchronized(retrieverLock) {
                        runCatching { retrieverCache.remove(clip.uriString)?.release() }
                    }
                }
            }
        }

        val fallback = ImageProcessingEngine.decodeSampledBitmap(
            context = context,
            uriString = null,
            fallbackDrawable = clip.fallbackDrawable ?: R.drawable.img_studio_sample_bg_1795185351810,
            maxDimension = maxEdge
        )
        val result = fallback ?: Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(12, 8, 16))
        }
        if (maxEdge <= 800) {
            cachedFrame = CachedSourceFrame(cacheKey, result)
        }
        return result
    }

    private fun scaleDownIfNeeded(bmp: Bitmap, maxEdge: Int): Bitmap {
        val m = max(bmp.width, bmp.height)
        if (m <= maxEdge) return bmp
        val ratio = maxEdge.toFloat() / m.toFloat()
        return Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * ratio).roundToInt().coerceAtLeast(32),
            (bmp.height * ratio).roundToInt().coerceAtLeast(32),
            true
        )
    }

    suspend fun captureFreezeFrameHd(
        context: Context,
        state: VideoEditorState
    ): ExportResult = withContext(Dispatchers.Default) {
        val rendered = renderPreviewAtPlayhead(context, state, maxEdge = 1920)
        try {
            val baos = ByteArrayOutputStream()
            rendered.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val fileName = MediaExportStore.makeFileName("FreezeFrame", "png")
            MediaExportStore.saveImageBytes(context, fileName, "image/png", baos.toByteArray())
        } finally {
            rendered.recycle()
        }
    }

    private data class TransitionFxValues(
        val alpha: Float = 1f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val scale: Float = 1f,
        val rotation: Float = 0f,
        val glitch: Float = 0f,
        val blur: Float = 0f,
        val wipeProgress: Float = 1f,
        val wipeVertical: Boolean = false,
        val splitProgress: Float = 1f,
        val flashAlpha: Float = 0f,
        val flashColor: Int = Color.WHITE
    )

    private fun isContinuousClipFx(transition: VideoTransition): Boolean {
        return when (transition) {
            VideoTransition.SHAKE,
            VideoTransition.EARTHQUAKE,
            VideoTransition.IMPACT_HIT,
            VideoTransition.HANDHELD_JOLT,
            VideoTransition.GLITCH_RGB,
            VideoTransition.PIXEL_SPLIT,
            VideoTransition.SCANLINE,
            VideoTransition.CHROMATIC,
            VideoTransition.STROBE,
            VideoTransition.VHS_TRACKING -> true
            else -> false
        }
    }

    private fun computeTransitionFx(
        transition: VideoTransition,
        pRaw: Float,
        timeMs: Long,
        continuousMode: Boolean = false
    ): TransitionFxValues {
        if (transition == VideoTransition.NONE) return TransitionFxValues()
        val p = pRaw.coerceIn(0f, 1f)
        // Smooth ease-out curve so live motion feels snappy and cinematic
        val eased = 1f - (1f - p) * (1f - p)
        val inv = if (continuousMode) {
            // Smooth pulsing wave across continuous FX cycles (never flat 0)
            0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin(p * Math.PI)).toFloat()
        } else {
            1f - eased
        }

        var tAlpha = 1f
        var tOffsetX = 0f
        var tOffsetY = 0f
        var tScale = 1f
        var tRotation = 0f
        var tGlitch = 0f
        var tBlur = 0f
        var tWipeProgress = 1f
        var tWipeVertical = false
        var tSplitProgress = 1f
        var flashAlpha = 0f
        var flashColor = Color.WHITE

        when (transition) {
            VideoTransition.NONE -> Unit
            VideoTransition.CROSSFADE -> tAlpha = 0.15f + 0.85f * eased
            VideoTransition.DIP_TO_BLACK -> tAlpha = eased
            VideoTransition.DIP_TO_WHITE -> { flashColor = Color.WHITE; flashAlpha = inv * 0.92f }
            VideoTransition.WIPE_LEFT -> { tWipeProgress = eased.coerceAtLeast(0.03f); tWipeVertical = false }
            VideoTransition.WIPE_RIGHT -> { tWipeProgress = eased.coerceAtLeast(0.03f); tWipeVertical = false; tOffsetX = -inv * 0.25f }
            VideoTransition.WIPE_UP -> { tWipeProgress = eased.coerceAtLeast(0.03f); tWipeVertical = true }
            VideoTransition.WIPE_DOWN -> { tWipeProgress = eased.coerceAtLeast(0.03f); tWipeVertical = true; tOffsetY = -inv * 0.25f }

            VideoTransition.WHIP_PAN -> { tOffsetX = inv * 0.95f; tBlur = inv * 0.85f; tScale = 1f + inv * 0.08f }
            VideoTransition.WHIP_UP -> { tOffsetY = inv * 0.95f; tBlur = inv * 0.85f; tScale = 1f + inv * 0.08f }
            VideoTransition.SLIDE_LEFT -> tOffsetX = inv
            VideoTransition.SLIDE_RIGHT -> tOffsetX = -inv
            VideoTransition.SLIDE_UP -> tOffsetY = inv
            VideoTransition.SLIDE_DOWN -> tOffsetY = -inv

            VideoTransition.GLITCH_RGB -> {
                tGlitch = inv
                tOffsetX = (if ((timeMs / 45L) % 2L == 0L) 0.038f else -0.038f) * inv
            }
            VideoTransition.PIXEL_SPLIT -> {
                tGlitch = inv * 0.9f
                tSplitProgress = if (continuousMode) 0.72f + 0.28f * eased else 0.30f + 0.70f * eased
            }
            VideoTransition.SCANLINE -> { tGlitch = inv * 0.75f; tAlpha = 0.55f + 0.45f * eased }
            VideoTransition.CHROMATIC -> { tGlitch = inv * 1.15f; tScale = 1.0f + inv * 0.14f }
            VideoTransition.STROBE -> {
                val pulse = (timeMs / 60L) % 2L == 0L
                tAlpha = if (pulse) 1f else 0.30f
                flashAlpha = if (pulse) inv * 0.52f else 0f
            }
            VideoTransition.VHS_TRACKING -> {
                tGlitch = inv * 0.95f
                tOffsetY = kotlin.math.sin(p * Math.PI * 10.0).toFloat() * inv * 0.06f
                tBlur = inv * 0.35f
            }

            VideoTransition.ZOOM_IN -> tScale = 1.48f - 0.48f * eased
            VideoTransition.ZOOM_OUT -> tScale = 0.58f + 0.42f * eased
            VideoTransition.SPIN_360 -> { tRotation = inv * 360f; tScale = 0.68f + 0.32f * eased }
            VideoTransition.SPIN_LEFT -> { tRotation = -inv * 360f; tScale = 0.72f + 0.28f * eased }
            VideoTransition.SWIRL_ZOOM -> { tRotation = inv * 180f; tScale = 1.52f - 0.52f * eased; tBlur = inv * 0.45f }
            VideoTransition.CRASH_ZOOM -> { tScale = 1.92f - 0.92f * eased; tBlur = inv * 0.6f }

            VideoTransition.SHAKE -> {
                tOffsetX = kotlin.math.sin(p * Math.PI * 8.0).toFloat() * inv * 0.085f
                tOffsetY = kotlin.math.cos(p * Math.PI * 7.0).toFloat() * inv * 0.065f
                tScale = 1.06f
            }
            VideoTransition.EARTHQUAKE -> {
                tOffsetX = kotlin.math.sin(p * Math.PI * 14.0).toFloat() * inv * 0.13f
                tOffsetY = kotlin.math.cos(p * Math.PI * 11.0).toFloat() * inv * 0.10f
                tScale = 1.10f
            }
            VideoTransition.IMPACT_HIT -> {
                tScale = 1.0f + inv * 0.28f
                tOffsetY = kotlin.math.sin(p * Math.PI * 8.0).toFloat() * inv * 0.09f
                flashAlpha = inv * 0.35f
            }
            VideoTransition.HANDHELD_JOLT -> {
                tRotation = kotlin.math.sin(p * Math.PI * 6.0).toFloat() * inv * 8.5f
                tOffsetX = kotlin.math.cos(p * Math.PI * 7.0).toFloat() * inv * 0.075f
                tScale = 1.07f
            }

            VideoTransition.SPLIT -> tSplitProgress = eased.coerceAtLeast(0.03f)
            VideoTransition.SPLIT_VERTICAL -> { tSplitProgress = eased.coerceAtLeast(0.03f); tOffsetY = inv * 0.14f }
            VideoTransition.CURTAIN_OPEN -> { tSplitProgress = eased.coerceAtLeast(0.03f); tAlpha = 0.4f + 0.6f * eased }
            VideoTransition.SHUTTER_SPLIT -> { tSplitProgress = eased.coerceAtLeast(0.03f); tBlur = inv * 0.4f; flashColor = Color.BLACK; flashAlpha = inv * 0.25f }

            VideoTransition.FLASH -> { flashColor = Color.WHITE; flashAlpha = inv * 0.95f }
            VideoTransition.BLUR -> tBlur = inv * 0.95f
            VideoTransition.LIGHT_LEAK -> { flashColor = Color.rgb(255, 165, 55); flashAlpha = inv * 0.78f }
            VideoTransition.GLOW_BURN -> { flashColor = Color.rgb(255, 30, 66); flashAlpha = inv * 0.72f; tScale = 1.0f + inv * 0.16f }
            VideoTransition.RADIAL_FLASH -> { flashColor = Color.rgb(255, 240, 190); flashAlpha = inv * 0.85f; tBlur = inv * 0.45f }
            VideoTransition.CRIMSON_FLASH -> { flashColor = Color.rgb(255, 30, 66); flashAlpha = inv * 0.92f; tScale = 1.0f + inv * 0.10f }
        }

        return TransitionFxValues(
            alpha = tAlpha,
            offsetX = tOffsetX,
            offsetY = tOffsetY,
            scale = tScale,
            rotation = tRotation,
            glitch = tGlitch,
            blur = tBlur,
            wipeProgress = tWipeProgress,
            wipeVertical = tWipeVertical,
            splitProgress = tSplitProgress,
            flashAlpha = flashAlpha,
            flashColor = flashColor
        )
    }

    fun renderPreviewAtPlayhead(
        context: Context,
        state: VideoEditorState,
        maxEdge: Int
    ): Bitmap {
        val ratio = state.aspectPreset.ratioW.toFloat() / state.aspectPreset.ratioH.toFloat()
        val targetW: Int
        val targetH: Int
        if (ratio >= 1f) {
            targetW = maxEdge
            targetH = (maxEdge / ratio).roundToInt().coerceAtLeast(64)
        } else {
            targetH = maxEdge
            targetW = (maxEdge * ratio).roundToInt().coerceAtLeast(64)
        }

        val clips = state.clips
        if (clips.isEmpty()) {
            return Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.rgb(8, 6, 12))
            }
        }

        var cursor = 0L
        var activeClip = clips.last()
        var localTimelineMs = 0L
        var activeIdx = clips.lastIndex

        for (i in clips.indices) {
            val c = clips[i]
            val dur = c.effectiveDurationMs
            if (state.playheadMs < cursor + dur || i == clips.lastIndex) {
                activeClip = c
                activeIdx = i
                localTimelineMs = (state.playheadMs - cursor).coerceIn(0L, dur)
                break
            }
            cursor += dur
        }

        // Apply Speed Curve Ramping on local timeline progress
        val activeDurMs = activeClip.effectiveDurationMs.coerceAtLeast(1L)
        val normProgress = (localTimelineMs.toFloat() / activeDurMs.toFloat()).coerceIn(0f, 1f)
        val curvedProgress = remapProgressWithSpeedCurve(normProgress, activeClip.speedCurve)
        val clipSpanMs = (activeClip.trimEndMs - activeClip.trimStartMs).coerceAtLeast(300L)
        val sourceLocalMs = (curvedProgress * clipSpanMs).toLong()
        val srcFrame = extractClipFrame(context, activeClip, sourceLocalMs, maxEdge)
        lastRenderedSourceAspect = srcFrame.width.toFloat() / srcFrame.height.toFloat().coerceAtLeast(1f)

        // Interpolate Diamond Keyframes on the active clip
        val kf = interpolateKeyframes(activeClip.keyframes, localTimelineMs)

        // Live FX & Transition Animation (works seamlessly on BOTH multi-clip seams AND single-clip / active-clip FX)
        var fx = TransitionFxValues()
        var prevClipBackdrop: Bitmap? = null

        val prevClip = if (activeIdx > 0) clips[activeIdx - 1] else null
        val incomingDurMs = prevClip?.transitionDurationMs?.coerceIn(100L, 3000L) ?: 500L
        val activeFxDurMs = activeClip.transitionDurationMs.coerceIn(100L, 3000L)
        val remainingMs = (activeDurMs - localTimelineMs).coerceAtLeast(0L)

        if (prevClip != null && prevClip.transitionAfter != VideoTransition.NONE && localTimelineMs < incomingDurMs) {
            // 1. Multi-clip incoming transition right after seam (with outgoing clip frame underneath!)
            val p = (localTimelineMs.toFloat() / incomingDurMs.toFloat()).coerceIn(0f, 1f)
            fx = computeTransitionFx(prevClip.transitionAfter, p, localTimelineMs, continuousMode = false)
            val prevSpan = (prevClip.trimEndMs - prevClip.trimStartMs).coerceAtLeast(300L)
            prevClipBackdrop = extractClipFrame(context, prevClip, prevSpan, maxEdge)
        } else if (activeClip.transitionAfter != VideoTransition.NONE) {
            val activeTr = activeClip.transitionAfter
            if (isContinuousClipFx(activeTr)) {
                // 2. Continuous Camera Shake / Glitch / VHS FX animates live throughout the clip
                val cycleP = ((localTimelineMs % activeFxDurMs).toFloat() / activeFxDurMs.toFloat()).coerceIn(0f, 1f)
                fx = computeTransitionFx(activeTr, cycleP, localTimelineMs, continuousMode = true)
            } else if (activeIdx < clips.lastIndex) {
                // 3. Multi-clip outgoing lead-out transition right before seam into next clip
                val outDurMs = (activeFxDurMs / 2L).coerceIn(120L, 800L)
                if (remainingMs < outDurMs) {
                    val outProgress = (remainingMs.toFloat() / outDurMs.toFloat()).coerceIn(0f, 1f)
                    val leadOut = computeTransitionFx(activeTr, outProgress, localTimelineMs, continuousMode = false)
                    // Reverse directional travel on exit so Whip Up whips cleanly up & out before the next clip enters
                    fx = leadOut.copy(
                        offsetX = -leadOut.offsetX * 0.7f,
                        offsetY = -leadOut.offsetY * 0.7f,
                        rotation = -leadOut.rotation * 0.45f
                    )
                }
            } else {
                // 4. Single-clip or last-clip Motion/Whip/Spin/Zoom/Flash/Wipe FX animates live on intro & outro
                if (localTimelineMs < activeFxDurMs) {
                    val p = (localTimelineMs.toFloat() / activeFxDurMs.toFloat()).coerceIn(0f, 1f)
                    fx = computeTransitionFx(activeTr, p, localTimelineMs, continuousMode = false)
                } else if (activeDurMs > activeFxDurMs * 2L && remainingMs < activeFxDurMs) {
                    val p = (remainingMs.toFloat() / activeFxDurMs.toFloat()).coerceIn(0f, 1f)
                    val outro = computeTransitionFx(activeTr, p, localTimelineMs, continuousMode = false)
                    fx = outro.copy(
                        offsetX = -outro.offsetX,
                        offsetY = -outro.offsetY,
                        rotation = -outro.rotation
                    )
                }
            }
        }

        // Optional User-Added PIP bitmap ONLY (never draw any default stuck overlay)
        var pipBmp: Bitmap? = null
        var shouldRecyclePip = false
        if (state.pip.enabled && !state.pip.uriString.isNullOrBlank()) {
            val rawPip = ImageProcessingEngine.decodeSampledBitmap(
                context = context,
                uriString = state.pip.uriString,
                fallbackDrawable = null,
                maxDimension = (maxEdge * 0.55f).roundToInt().coerceAtLeast(180)
            )
            if (rawPip != null) {
                if (state.pip.chromaEnabled) {
                    pipBmp = ImageProcessingEngine.applyChromaKey(
                        source = rawPip,
                        keyColor = state.pip.chromaKeyColor,
                        tolerance = state.pip.chromaTolerance,
                        spillSuppression = state.pip.spillSuppression
                    )
                    shouldRecyclePip = true
                } else {
                    pipBmp = rawPip
                }
            }
        }

        val rendered = ImageProcessingEngine.renderVideoFrame(
            context = context,
            sourceFrame = srcFrame,
            targetW = targetW,
            targetH = targetH,
            cropZoom = state.cropZoom,
            panX = state.panX,
            panY = state.panY,
            blurredBgFill = state.blurredBgFill,
            bgFillMode = state.bgFillMode,
            bgFillColorArgb = state.bgFillColorArgb,
            bgImageUri = state.bgImageUri,
            clipRotationDeg = activeClip.rotationDeg,
            clipFlipH = activeClip.flipH,
            clipFlipV = activeClip.flipV,
            clipMirrorMode = activeClip.mirrorMode,
            stabilizeEnabled = activeClip.stabilizeEnabled,
            motionBlur = activeClip.motionBlur,
            keyframeX = kf.xNorm,
            keyframeY = kf.yNorm,
            keyframeScale = kf.scale,
            keyframeRot = kf.rotationDeg,
            keyframeOpacity = kf.opacity,
            grade = state.colorGrade,
            pipBitmap = pipBmp,
            pipXNorm = state.pip.xNorm,
            pipYNorm = state.pip.yNorm,
            pipScale = state.pip.scale,
            pipRotationDeg = state.pip.rotationDeg,
            pipOpacity = state.pip.opacity,
            pipBlendMode = state.pip.blendMode,
            pipMaskShape = state.pip.maskShape,
            transitionAlpha = fx.alpha,
            transitionOffsetX = fx.offsetX,
            transitionOffsetY = fx.offsetY,
            transitionScale = fx.scale,
            transitionRotationDeg = fx.rotation,
            transitionGlitchAmount = fx.glitch,
            transitionBlurAmount = fx.blur,
            transitionWipeProgress = fx.wipeProgress,
            transitionWipeVertical = fx.wipeVertical,
            transitionSplitProgress = fx.splitProgress,
            flashOverlayAlpha = fx.flashAlpha,
            flashOverlayColor = fx.flashColor,
            activeTexts = state.textLayers,
            playheadMs = state.playheadMs,
            previousFrame = prevClipBackdrop
        )
        if (shouldRecyclePip) pipBmp?.recycle()
        return rendered
    }

    /**
     * Watermark-free MP4 Video Export using pure-Kotlin AvcMp4Muxer (zero MPEG4Writer issues).
     */
    suspend fun exportVideoToMp4(
        context: Context,
        state: VideoEditorState,
        capabilities: HardwareEncoderCapabilities = queryHardwareEncoderCapabilities(),
        onProgress: (Float) -> Unit
    ): ExportResult = withContext(Dispatchers.Default) {
        val requestedRes = state.exportResolution
        val requestedFps = state.exportFps

        val needsFallbackTo1080p60 = when {
            requestedRes == VideoExportResolution.UHD_4K && requestedFps == VideoExportFps.FPS_60 && !capabilities.supports4K60 -> true
            requestedRes == VideoExportResolution.UHD_4K && !capabilities.supports4K30 -> true
            else -> false
        }

        val effectiveRes = if (needsFallbackTo1080p60) VideoExportResolution.FHD_1080P else requestedRes
        val effectiveFps = if (needsFallbackTo1080p60) 60 else requestedFps.fps
        val effectiveBitrateMbps = when {
            needsFallbackTo1080p60 -> 14
            effectiveRes == VideoExportResolution.UHD_4K && effectiveFps == 60 -> 32
            effectiveFps == 60 -> (effectiveRes.bitrateMbps * 1.4f).roundToInt()
            else -> effectiveRes.bitrateMbps
        }

        val primaryResult = encodeMp4Attempt(
            context = context,
            state = state,
            longEdge = effectiveRes.longEdge,
            fps = effectiveFps,
            bitrateMbps = effectiveBitrateMbps,
            tagLabel = "${effectiveRes.shortLabel.replace(" ", "")}_${effectiveFps}fps",
            onProgress = onProgress
        )

        val finalResult = if (!primaryResult.success && (effectiveRes.longEdge > 1280 || effectiveFps > 30)) {
            encodeMp4Attempt(
                context = context,
                state = state,
                longEdge = VideoExportResolution.HD_720P.longEdge,
                fps = 30,
                bitrateMbps = 6,
                tagLabel = "HD_30fps",
                onProgress = onProgress
            )
        } else {
            primaryResult
        }

        com.example.security.AppSecurityFirewall.cleanupTempExportCache(context)

        if (finalResult.success && needsFallbackTo1080p60) {
            finalResult.copy(displayPath = "${finalResult.displayPath} (Full HD Smooth)")
        } else {
            finalResult
        }
    }

    private data class ValidatedEncoderTarget(
        val codecName: String?,
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrateBps: Int
    )

    private fun resolveSupportedEncoderTarget(
        reqW: Int,
        reqH: Int,
        reqFps: Int,
        reqBitrateBps: Int
    ): ValidatedEncoderTarget {
        val alignedW = ((reqW / 16) * 16).coerceAtLeast(320)
        val alignedH = ((reqH / 16) * 16).coerceAtLeast(240)
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoders = codecList.codecInfos.filter { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
            }.sortedByDescending { info ->
                val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.isHardwareAccelerated
                } else {
                    val n = info.name.lowercase()
                    !n.startsWith("omx.google.") && !n.startsWith("c2.android.")
                }
                if (isHw) 1 else 0
            }

            for (info in encoders) {
                val caps = runCatching { info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC) }.getOrNull() ?: continue
                val vCaps = caps.videoCapabilities ?: continue
                if (!caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)) continue

                val wAlign = vCaps.widthAlignment.coerceAtLeast(2)
                val hAlign = vCaps.heightAlignment.coerceAtLeast(2)
                val maxW = vCaps.supportedWidths.upper
                val maxH = vCaps.supportedHeights.upper
                val minW = vCaps.supportedWidths.lower
                val minH = vCaps.supportedHeights.lower

                var w = alignedW.coerceIn(minW, maxW)
                var h = alignedH.coerceIn(minH, maxH)
                w = (w / wAlign) * wAlign
                h = (h / hAlign) * hAlign
                var fps = reqFps.coerceIn(
                    vCaps.supportedFrameRates.lower.coerceAtLeast(15),
                    vCaps.supportedFrameRates.upper.coerceAtMost(60)
                )
                if (!vCaps.areSizeAndRateSupported(w, h, fps.toDouble())) {
                    fps = 30.coerceIn(vCaps.supportedFrameRates.lower, vCaps.supportedFrameRates.upper)
                }
                if (!vCaps.areSizeAndRateSupported(w, h, fps.toDouble())) {
                    val scale = min(1280f / max(w, 1), 720f / max(h, 1)).coerceAtMost(1f)
                    w = (((w * scale).roundToInt() / wAlign) * wAlign).coerceIn(minW, maxW)
                    h = (((h * scale).roundToInt() / hAlign) * hAlign).coerceIn(minH, maxH)
                }
                val bitrate = reqBitrateBps.coerceIn(vCaps.bitrateRange.lower, vCaps.bitrateRange.upper)
                return ValidatedEncoderTarget(info.name, w, h, fps, bitrate)
            }
            ValidatedEncoderTarget(null, alignedW, alignedH, reqFps.coerceAtMost(30), reqBitrateBps)
        } catch (_: Exception) {
            ValidatedEncoderTarget(null, alignedW, alignedH, reqFps.coerceAtMost(30), reqBitrateBps)
        }
    }

    private suspend fun encodeMp4Attempt(
        context: Context,
        state: VideoEditorState,
        longEdge: Int,
        fps: Int,
        bitrateMbps: Int,
        tagLabel: String,
        onProgress: (Float) -> Unit
    ): ExportResult {
        val ratio = state.aspectPreset.ratioW.toFloat() / state.aspectPreset.ratioH.toFloat()
        val rawW: Int
        val rawH: Int
        if (ratio >= 1f) {
            rawW = longEdge
            rawH = (longEdge / ratio).roundToInt()
        } else {
            rawH = longEdge
            rawW = (longEdge * ratio).roundToInt()
        }

        val target = resolveSupportedEncoderTarget(rawW, rawH, fps, bitrateMbps * 1_000_000)
        val encW = target.width
        val encH = target.height
        val encFps = target.fps
        val totalDurationMs = state.totalDurationMs.coerceIn(1000L, 15000L)
        val totalFrames = ((totalDurationMs / 1000.0) * encFps).roundToInt().coerceIn(24, 300)
        val renderMaxEdge = min(max(encW, encH), 1920)
        val upscalePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)

        val tempFile = File(context.cacheDir, "bd_export_${System.currentTimeMillis()}.mp4")
        var codec: MediaCodec? = null
        var mp4Muxer: AvcMp4Muxer? = null
        var inputSurface: Surface? = null

        return try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, encW, encH).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, target.bitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, encFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val activeCodec = if (target.codecName != null) {
                MediaCodec.createByCodecName(target.codecName)
            } else {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            }
            codec = activeCodec
            activeCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val activeSurface = activeCodec.createInputSurface()
            inputSurface = activeSurface
            activeCodec.start()

            val activeMuxer = AvcMp4Muxer(tempFile, encW, encH, encFps)
            mp4Muxer = activeMuxer
            val bufferInfo = MediaCodec.BufferInfo()

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) runCatching { activeCodec.signalEndOfInputStream() }
                var eosIdleRetries = 0
                while (true) {
                    val outIdx = activeCodec.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                            eosIdleRetries++
                            if (eosIdleRetries > 60) break
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outFmt = activeCodec.outputFormat
                            if (outFmt.containsKey("csd-0")) activeMuxer.addCodecConfig(outFmt.getByteBuffer("csd-0"))
                            if (outFmt.containsKey("csd-1")) activeMuxer.addCodecConfig(outFmt.getByteBuffer("csd-1"))
                        }
                        outIdx >= 0 -> {
                            eosIdleRetries = 0
                            val encodedBuf = activeCodec.getOutputBuffer(outIdx)
                            val isCodecConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                            val isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                            val isEos = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                            if (encodedBuf != null && bufferInfo.size > 0) {
                                val rawBytes = ByteArray(bufferInfo.size)
                                encodedBuf.position(bufferInfo.offset)
                                encodedBuf.get(rawBytes)
                                if (isCodecConfig) activeMuxer.addCodecConfigBytes(rawBytes)
                                else activeMuxer.writeAccessUnit(rawBytes, isKeyFrame)
                            }
                            activeCodec.releaseOutputBuffer(outIdx, false)
                            if (isEos) break
                        }
                    }
                }
            }

            for (frameIdx in 0 until totalFrames) {
                val ms = ((frameIdx.toDouble() / totalFrames) * totalDurationMs).toLong()
                val frameBmp = renderPreviewAtPlayhead(context, state.copy(playheadMs = ms), renderMaxEdge)
                val surfaceCanvas: Canvas = activeSurface.lockCanvas(null)
                try {
                    surfaceCanvas.drawColor(Color.BLACK)
                    surfaceCanvas.drawBitmap(frameBmp, null, Rect(0, 0, encW, encH), upscalePaint)
                } finally {
                    activeSurface.unlockCanvasAndPost(surfaceCanvas)
                    frameBmp.recycle()
                }
                drainEncoder(false)
                onProgress((frameIdx + 1).toFloat() / totalFrames.toFloat())
            }

            drainEncoder(true)
            runCatching { activeCodec.stop() }
            runCatching { activeCodec.release() }
            codec = null
            runCatching { activeSurface.release() }
            inputSurface = null

            val finalized = activeMuxer.finish()
            mp4Muxer = null

            if (!finalized || activeMuxer.writtenFrameCount <= 0 || !tempFile.exists() || tempFile.length() == 0L) {
                return ExportResult(false, "", 0L, "Could not save video on this device. Try HD 720p.")
            }

            val fileName = MediaExportStore.makeFileName("Video_$tagLabel", "mp4")
            MediaExportStore.saveVideoFile(context, tempFile, fileName)
        } catch (_: Exception) {
            ExportResult(false, "", 0L, "Video save paused. Retrying in HD.")
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { inputSurface?.release() }
            runCatching { mp4Muxer?.abort() }
            runCatching { tempFile.delete() }
        }
    }

    suspend fun extractFramesToGallery(
        context: Context,
        videoUri: String?,
        fallbackState: VideoEditorState,
        frameCount: Int
    ): Pair<Int, ExportResult> = withContext(Dispatchers.Default) {
        val count = frameCount.coerceIn(3, 12)
        var saved = 0
        var lastResult = ExportResult(false, "", 0L, "No frames extracted")
        val duration = if (!videoUri.isNullOrBlank()) probeVideoMetadata(context, videoUri).durationMs else fallbackState.totalDurationMs

        for (i in 0 until count) {
            val ms = (duration * i) / max(1, count - 1)
            val bmp = if (!videoUri.isNullOrBlank()) {
                extractClipFrame(context, VideoClip("ext", "Frame", uriString = videoUri, durationMs = duration, trimEndMs = duration), ms, 1280)
            } else {
                renderPreviewAtPlayhead(context, fallbackState.copy(playheadMs = ms), maxEdge = 1280)
            }
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 95, baos)
            bmp.recycle()
            val res = MediaExportStore.saveImageBytes(context, MediaExportStore.makeFileName("Frame_${i + 1}", "png"), "image/png", baos.toByteArray())
            if (res.success) { saved++; lastResult = res }
        }
        saved to lastResult
    }

    suspend fun exportVideoToGif(
        context: Context,
        videoUri: String?,
        fallbackState: VideoEditorState,
        fps: Int,
        frameCount: Int
    ): ExportResult = withContext(Dispatchers.Default) {
        val count = frameCount.coerceIn(4, 24)
        val delayCs = (100 / fps.coerceIn(4, 15)).coerceAtLeast(6)
        val duration = if (!videoUri.isNullOrBlank()) probeVideoMetadata(context, videoUri).durationMs else fallbackState.totalDurationMs

        val gifW = 320
        val gifH = 180
        val baos = ByteArrayOutputStream()
        writeGifHeader(baos, gifW, gifH)

        for (i in 0 until count) {
            val ms = (duration * i) / max(1, count - 1)
            val raw = if (!videoUri.isNullOrBlank()) {
                extractClipFrame(context, VideoClip("gif", "Gif", uriString = videoUri, durationMs = duration, trimEndMs = duration), ms, gifW)
            } else {
                renderPreviewAtPlayhead(context, fallbackState.copy(playheadMs = ms), maxEdge = gifW)
            }
            val frame = Bitmap.createScaledBitmap(raw, gifW, gifH, true)
            if (frame != raw) raw.recycle()
            writeGifFrame(baos, frame, delayCs)
            frame.recycle()
        }
        baos.write(0x3B)
        MediaExportStore.saveImageBytes(context, MediaExportStore.makeFileName("Anim", "gif"), "image/gif", baos.toByteArray())
    }

    private fun writeGifHeader(out: OutputStream, w: Int, h: Int) {
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShortLE(out, w)
        writeShortLE(out, h)
        out.write(0xF7)
        out.write(0)
        out.write(0)
        for (r in 0 until 6) {
            for (g in 0 until 6) {
                for (b in 0 until 6) {
                    out.write(r * 51); out.write(g * 51); out.write(b * 51)
                }
            }
        }
        for (i in 0 until 40) {
            val v = (i * 255) / 39
            out.write(v); out.write(v); out.write(v)
        }
        out.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00))
    }

    private fun writeGifFrame(out: OutputStream, bmp: Bitmap, delayCs: Int) {
        val w = bmp.width
        val h = bmp.height
        val pix = IntArray(w * h)
        bmp.getPixels(pix, 0, w, 0, 0, w, h)
        val indexed = ByteArray(w * h)
        for (i in pix.indices) {
            val p = pix[i]
            val r = (((p ushr 16) and 0xFF) * 5 + 127) / 255
            val g = (((p ushr 8) and 0xFF) * 5 + 127) / 255
            val b = ((p and 0xFF) * 5 + 127) / 255
            indexed[i] = (r * 36 + g * 6 + b).toByte()
        }

        out.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, 0x04))
        writeShortLE(out, delayCs)
        out.write(0x00); out.write(0x00)
        out.write(0x2C)
        writeShortLE(out, 0); writeShortLE(out, 0)
        writeShortLE(out, w); writeShortLE(out, h)
        out.write(0x00)
        out.write(8)

        val clearCode = 256
        val eoiCode = 257
        val block = ByteArray(255)
        var blockLen = 0
        var bitBuf = 0
        var bitCount = 0

        fun emitCode(code: Int, bits: Int) {
            bitBuf = bitBuf or (code shl bitCount)
            bitCount += bits
            while (bitCount >= 8) {
                block[blockLen++] = (bitBuf and 0xFF).toByte()
                bitBuf = bitBuf ushr 8
                bitCount -= 8
                if (blockLen == 255) {
                    out.write(255); out.write(block, 0, 255); blockLen = 0
                }
            }
        }

        emitCode(clearCode, 9)
        var codesSinceClear = 0
        for (b in indexed) {
            emitCode(b.toInt() and 0xFF, 9)
            codesSinceClear++
            if (codesSinceClear >= 250) { emitCode(clearCode, 9); codesSinceClear = 0 }
        }
        emitCode(eoiCode, 9)
        if (bitCount > 0) block[blockLen++] = (bitBuf and 0xFF).toByte()
        if (blockLen > 0) { out.write(blockLen); out.write(block, 0, blockLen) }
        out.write(0x00)
    }

    private fun writeShortLE(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }
}
