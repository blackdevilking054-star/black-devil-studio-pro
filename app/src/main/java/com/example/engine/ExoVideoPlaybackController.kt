package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.example.model.LutPreset
import com.example.model.VideoClip
import com.example.model.VideoEditorState
import com.example.model.VideoTransition
import kotlin.math.abs

/**
 * Low-latency hardware-accelerated Media3 ExoPlayer controller optimized for 2GB/3GB RAM devices.
 * - Uses [DefaultLoadControl] (min=1500ms, max=5000ms, playback=500ms, 12MB byte cap, zero back-buffer).
 * - Uses [DefaultRenderersFactory] prioritizing hardware [android.media.MediaCodec] with async
 *   queueing and automatic decoder fallback so 4K/1080p clips scrub and play smoothly at 60fps.
 * - Switches to [SeekParameters.CLOSEST_SYNC] during timeline scrubbing with non-blocking
 *   coalescing so dragging the playhead never stalls or freezes the UI thread.
 * - Binds directly to a GPU [TextureView] so Compose `graphicsLayer { ... }` transforms
 *   (Keyframes, zoom, pan, rotate, opacity) execute on the GPU at 60fps with zero recomposition.
 */
@OptIn(UnstableApi::class)
class ExoVideoPlaybackController(context: Context) {

    companion object {
        const val MIN_BUFFER_MS = 1_500
        const val MAX_BUFFER_MS = 5_000
        const val BUFFER_FOR_PLAYBACK_MS = 500
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_000
        const val TARGET_BUFFER_BYTES = 12 * 1024 * 1024 // 12 MB strict RAM cap for 2GB/3GB phones
        private const val SCRUB_SEEK_COALESCE_MS = 16L
        private const val SCRUB_DELTA_THRESHOLD_MS = 24L

        /**
         * Creates a low-latency, low-RAM [DefaultLoadControl] tuned for instant video editor
         * scrubbing and stutter-free local playback on 2GB/3GB RAM Android phones.
         */
        fun buildLowLatencyLoadControl(): DefaultLoadControl {
            return DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setTargetBufferBytes(TARGET_BUFFER_BYTES)
                .setBackBuffer(0, false)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }

        /**
         * Creates a hardware-accelerated [DefaultRenderersFactory] prioritizing platform
         * [android.media.MediaCodec] decoders with async queueing enabled for smooth 60fps
         * 1080p/4K scrubbing and playback.
         */
        fun buildHardwareMediaCodecRenderersFactory(context: Context): DefaultRenderersFactory {
            return DefaultRenderersFactory(context.applicationContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                .setEnableDecoderFallback(true)
                .forceEnableMediaCodecAsynchronousQueueing()
        }

        /**
         * Returns true when the active clip can be rendered directly via the hardware-accelerated
         * ExoPlayer GPU [TextureView] (zero per-frame CPU bitmap decoding).
         * Falls back seamlessly to the software compositor when pixel-level color grading,
         * LUTs, PIP chroma-key, glitch shaders, or reversed playback are active.
         */
        fun canUseHardwareExoSurface(state: VideoEditorState, activeClip: VideoClip?): Boolean {
            if (activeClip == null || activeClip.uriString.isNullOrBlank()) return false
            if (activeClip.reversed || activeClip.mirrorMode || activeClip.motionBlur > 0.02f) return false
            if (state.pip.enabled && !state.pip.uriString.isNullOrBlank()) return false
            val g = state.colorGrade
            val hasPixelGrade = g.lut != LutPreset.ORIGINAL ||
                abs(g.brightness) > 0.01f ||
                abs(g.contrast - 1f) > 0.01f ||
                abs(g.saturation - 1f) > 0.01f ||
                abs(g.exposure) > 0.01f ||
                abs(g.temperature) > 0.01f ||
                g.sharpen > 0.01f ||
                g.blur > 0.01f ||
                g.vignette > 0.01f ||
                abs(g.hslHue) > 0.5f ||
                abs(g.hslSat) > 0.01f ||
                abs(g.hslLight) > 0.01f ||
                g.qualityEnhancerEnabled
            if (hasPixelGrade) return false
            val tr = activeClip.transitionAfter
            val hasShaderFx = tr == VideoTransition.GLITCH_RGB ||
                tr == VideoTransition.PIXEL_SPLIT ||
                tr == VideoTransition.SCANLINE ||
                tr == VideoTransition.CHROMATIC ||
                tr == VideoTransition.VHS_TRACKING ||
                tr == VideoTransition.BLUR
            return !hasShaderFx
        }
    }

    val player: ExoPlayer = ExoPlayer.Builder(
        context.applicationContext,
        buildHardwareMediaCodecRenderersFactory(context)
    )
        .setLoadControl(buildLowLatencyLoadControl())
        .build()
        .apply {
            setSeekParameters(SeekParameters.CLOSEST_SYNC)
            repeatMode = Player.REPEAT_MODE_OFF
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            volume = 1f
        }

    private var loadedUri: String? = null
    private var boundTextureView: TextureView? = null
    private var lastScrubDispatchMs: Long = 0L
    private var lastScrubTargetMs: Long = -1L
    private var isReleased: Boolean = false

    fun attachTextureView(textureView: TextureView?) {
        if (isReleased) return
        if (boundTextureView === textureView) return
        boundTextureView?.let { old -> runCatching { player.clearVideoTextureView(old) } }
        boundTextureView = textureView
        if (textureView != null) {
            runCatching { player.setVideoTextureView(textureView) }
        }
    }

    fun ensureClipLoaded(uriString: String?) {
        if (isReleased) return
        if (uriString.isNullOrBlank()) {
            if (loadedUri != null) {
                loadedUri = null
                player.pause()
                player.clearMediaItems()
            }
            return
        }
        if (loadedUri != uriString) {
            loadedUri = uriString
            lastScrubTargetMs = -1L
            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            player.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
            player.prepare()
        }
    }

    /**
     * Non-blocking scrub seek using [SeekParameters.CLOSEST_SYNC].
     * Coalesces high-frequency touch deltas so dragging never stalls the UI thread.
     */
    fun seekScrub(sourceMs: Long, force: Boolean = false) {
        if (isReleased || loadedUri == null) return
        val clamped = sourceMs.coerceAtLeast(0L)
        val now = SystemClock.elapsedRealtime()
        if (!force &&
            abs(clamped - lastScrubTargetMs) < SCRUB_DELTA_THRESHOLD_MS &&
            (now - lastScrubDispatchMs) < SCRUB_SEEK_COALESCE_MS
        ) {
            return
        }
        lastScrubDispatchMs = now
        lastScrubTargetMs = clamped
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        player.seekTo(clamped)
    }

    fun syncPlaybackState(
        playing: Boolean,
        speed: Float,
        desiredSourceMs: Long
    ) {
        if (isReleased || loadedUri == null) return
        val safeSpeed = speed.coerceIn(0.1f, 10f)
        if (abs(player.playbackParameters.speed - safeSpeed) > 0.01f) {
            player.playbackParameters = PlaybackParameters(safeSpeed)
        }
        if (playing) {
            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            val drift = abs(player.currentPosition - desiredSourceMs)
            if (!player.isPlaying && drift > 45L) {
                player.seekTo(desiredSourceMs.coerceAtLeast(0L))
            } else if (drift > 220L) {
                player.seekTo(desiredSourceMs.coerceAtLeast(0L))
            }
            if (!player.playWhenReady) {
                player.playWhenReady = true
            }
        } else {
            if (player.playWhenReady) {
                player.playWhenReady = false
            }
            val drift = abs(player.currentPosition - desiredSourceMs)
            if (drift > 28L) {
                seekScrub(desiredSourceMs, force = true)
            }
        }
    }

    fun pause() {
        if (isReleased) return
        if (player.playWhenReady) {
            player.playWhenReady = false
        }
    }

    fun currentSourcePositionMs(): Long {
        if (isReleased || loadedUri == null) return 0L
        return player.currentPosition.coerceAtLeast(0L)
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        boundTextureView?.let { old -> runCatching { player.clearVideoTextureView(old) } }
        boundTextureView = null
        runCatching { player.release() }
    }
}
