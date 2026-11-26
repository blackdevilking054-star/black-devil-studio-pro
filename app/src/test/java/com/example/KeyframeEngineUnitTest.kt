package com.example

import com.example.engine.VideoMediaEngine
import com.example.engine.VideoOverlayRenderer
import com.example.model.KeyframeEasing
import com.example.model.VideoClip
import com.example.model.VideoEditorState
import com.example.model.VideoKeyframe
import com.example.model.VideoTextOverlay
import com.example.ui.workspaces.resolveActiveKeyframeTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests verifying the CapCut / VN Keyframe Animation Engine
 * (easing curves, clamping before/after first/last keyframes, segment detection,
 * Text/Sticker overlay resolution, and active target state resolution).
 */
class KeyframeEngineUnitTest {

    @Test
    fun easingCurves_matchExpectedMathematicalBehavior() {
        // Endpoints always map 0 -> 0 and 1 -> 1 across all 4 curves
        KeyframeEasing.entries.forEach { easing ->
            assertEquals(0f, VideoMediaEngine.applyKeyframeEasing(0f, easing), 0.0001f)
            assertEquals(1f, VideoMediaEngine.applyKeyframeEasing(1f, easing), 0.0001f)
        }

        // Linear: f(0.25) = 0.25, f(0.5) = 0.5, f(0.75) = 0.75
        assertEquals(0.25f, VideoMediaEngine.applyKeyframeEasing(0.25f, KeyframeEasing.LINEAR), 0.0001f)
        assertEquals(0.50f, VideoMediaEngine.applyKeyframeEasing(0.50f, KeyframeEasing.LINEAR), 0.0001f)
        assertEquals(0.75f, VideoMediaEngine.applyKeyframeEasing(0.75f, KeyframeEasing.LINEAR), 0.0001f)

        // Ease In (cubic t^3): slow start
        assertEquals(0.015625f, VideoMediaEngine.applyKeyframeEasing(0.25f, KeyframeEasing.EASE_IN), 0.0001f)
        assertEquals(0.125f, VideoMediaEngine.applyKeyframeEasing(0.50f, KeyframeEasing.EASE_IN), 0.0001f)

        // Ease Out (1 - (1-t)^3): fast start, slow settle
        assertEquals(0.578125f, VideoMediaEngine.applyKeyframeEasing(0.25f, KeyframeEasing.EASE_OUT), 0.0001f)
        assertEquals(0.875f, VideoMediaEngine.applyKeyframeEasing(0.50f, KeyframeEasing.EASE_OUT), 0.0001f)

        // Ease In-Out (smoothstep): S-curve symmetric around 0.5
        assertEquals(0.15625f, VideoMediaEngine.applyKeyframeEasing(0.25f, KeyframeEasing.EASE_IN_OUT), 0.0001f)
        assertEquals(0.50f, VideoMediaEngine.applyKeyframeEasing(0.50f, KeyframeEasing.EASE_IN_OUT), 0.0001f)
        assertEquals(0.84375f, VideoMediaEngine.applyKeyframeEasing(0.75f, KeyframeEasing.EASE_IN_OUT), 0.0001f)
    }

    @Test
    fun interpolateKeyframes_handlesUnsortedListHoldBeforeFirstAndHoldAfterLast() {
        val kfs = listOf(
            VideoKeyframe(id = "k2", timeMs = 2000L, xNorm = 0.6f, yNorm = -0.3f, scale = 1.8f, rotationDeg = 120f, opacity = 0.2f),
            VideoKeyframe(id = "k1", timeMs = 500L, xNorm = -0.2f, yNorm = 0.1f, scale = 1.0f, rotationDeg = 0f, opacity = 1.0f, easing = KeyframeEasing.LINEAR)
        )

        // Before first keyframe -> holds k1
        val before = VideoMediaEngine.interpolateKeyframes(kfs, 100L)
        assertEquals(-0.2f, before.xNorm, 0.0001f)
        assertEquals(0.1f, before.yNorm, 0.0001f)
        assertEquals(1.0f, before.scale, 0.0001f)
        assertEquals(0f, before.rotationDeg, 0.0001f)
        assertEquals(1.0f, before.opacity, 0.0001f)

        // Midpoint (1250ms = 50% between 500ms and 2000ms) with LINEAR easing
        val mid = VideoMediaEngine.interpolateKeyframes(kfs, 1250L)
        assertEquals(0.2f, mid.xNorm, 0.0001f)
        assertEquals(-0.1f, mid.yNorm, 0.0001f)
        assertEquals(1.4f, mid.scale, 0.0001f)
        assertEquals(60f, mid.rotationDeg, 0.0001f)
        assertEquals(0.6f, mid.opacity, 0.0001f)

        // After last keyframe -> holds k2
        val after = VideoMediaEngine.interpolateKeyframes(kfs, 3500L)
        assertEquals(0.6f, after.xNorm, 0.0001f)
        assertEquals(-0.3f, after.yNorm, 0.0001f)
        assertEquals(1.8f, after.scale, 0.0001f)
        assertEquals(120f, after.rotationDeg, 0.0001f)
        assertEquals(0.2f, after.opacity, 0.0001f)
    }

    @Test
    fun overlayAndTargetResolution_supportsVideoTextAndStickerKeyframes() {
        val sticker = VideoTextOverlay(
            id = "stk_1",
            text = "LIVE",
            isSticker = true,
            startMs = 1000L,
            endMs = 5000L,
            xNorm = 0.2f,
            yNorm = 0.2f,
            scale = 1.0f,
            rotationDeg = 0f,
            opacity = 1.0f,
            keyframes = listOf(
                VideoKeyframe("sk1", 0L, 0.2f, 0.2f, 1.0f, 0f, 0.2f, KeyframeEasing.LINEAR),
                VideoKeyframe("sk2", 2000L, 0.8f, 0.6f, 2.0f, 90f, 1.0f, KeyframeEasing.LINEAR)
            )
        )

        val resolved = VideoOverlayRenderer.resolveOverlayTransformAt(sticker, playheadMs = 2000L)
        assertEquals(0.5f, resolved.xNorm, 0.0001f)
        assertEquals(0.4f, resolved.yNorm, 0.0001f)
        assertEquals(1.5f, resolved.scale, 0.0001f)
        assertEquals(45f, resolved.rotationDeg, 0.0001f)
        assertEquals(0.6f, resolved.opacity, 0.0001f)

        val stateWithSticker = VideoEditorState(
            clips = listOf(VideoClip(id = "c1", name = "Clip 1", durationMs = 6000L, trimEndMs = 6000L)),
            textLayers = listOf(sticker),
            selectedTextId = sticker.id,
            playheadMs = 2000L
        )
        val target = resolveActiveKeyframeTarget(stateWithSticker)
        assertNotNull(target)
        assertEquals("Sticker", target!!.shortKind)
        assertTrue(target.isOverlay)
        assertNull(target.activeKeyframe) // 1000ms is strictly between 0ms and 2000ms
        assertTrue(target.hasPrevKeyframe)
        assertTrue(target.hasNextKeyframe)
        assertNotNull(target.activeSegment)
        assertEquals("sk1", target.activeSegment!!.first.id)
        assertEquals("sk2", target.activeSegment!!.second.id)

        // When playhead is right on sk1 (global 1000ms = local 0ms)
        val targetAtStart = resolveActiveKeyframeTarget(stateWithSticker.copy(playheadMs = 1000L))!!
        assertNotNull(targetAtStart.activeKeyframe)
        assertEquals("sk1", targetAtStart.activeKeyframe!!.id)
        assertFalse(targetAtStart.hasPrevKeyframe)
        assertTrue(targetAtStart.hasNextKeyframe)
        assertNull(targetAtStart.activeSegment)
    }

    @Test
    fun otaSmartUpdateGating_blocksSameVersionAndDismissedAutoChecks() {
        assertEquals(5, BuildConfig.VERSION_CODE)
        assertEquals("2.5", BuildConfig.VERSION_NAME)
        assertFalse(com.example.updater.AppUpdateChecker.shouldShowUpdateDialog(5, 5, 0, manualCheck = false))
        assertFalse(com.example.updater.AppUpdateChecker.shouldShowUpdateDialog(5, 5, 0, manualCheck = true))
        assertTrue(com.example.updater.AppUpdateChecker.shouldShowUpdateDialog(6, 5, 0, manualCheck = false))
        assertFalse(com.example.updater.AppUpdateChecker.shouldShowUpdateDialog(6, 5, 6, manualCheck = false))
        assertTrue(com.example.updater.AppUpdateChecker.shouldShowUpdateDialog(6, 5, 6, manualCheck = true))
        assertTrue(com.example.updater.AppUpdateChecker.shouldShowUpdateDialog(7, 5, 6, manualCheck = false))
        assertTrue(
            com.example.updater.AppUpdateChecker.isTrustedGitHubHttpsUrl(
                "https://github.com/blackdevilking054-star/black-devil-studio-pro/releases/latest/download/app-debug.apk"
            )
        )
    }

    @Test
    fun exoPlaybackOptimization_configuresLowLatencyAndHardwareSurfaceRules() {
        assertEquals(128, com.example.engine.TimelineThumbnailCache.MAX_THUMB_EDGE_PX)
        assertEquals(1500, com.example.engine.ExoVideoPlaybackController.MIN_BUFFER_MS)
        assertEquals(5000, com.example.engine.ExoVideoPlaybackController.MAX_BUFFER_MS)
        assertEquals(500, com.example.engine.ExoVideoPlaybackController.BUFFER_FOR_PLAYBACK_MS)
        assertTrue(com.example.engine.ExoVideoPlaybackController.TARGET_BUFFER_BYTES <= 16 * 1024 * 1024)

        // Strict bitmap downsampling & preview ceiling (<= 1080px for preview, <= 128px for thumbnails)
        assertEquals(1080, com.example.engine.ImageProcessingEngine.MAX_PREVIEW_DIMENSION_PX)
        assertEquals(4, com.example.engine.ImageProcessingEngine.computeInSampleSize(4320, 2160, 1080))
        assertEquals(2, com.example.engine.ImageProcessingEngine.computeInSampleSize(2560, 1440, 1080))
        assertEquals(16, com.example.engine.ImageProcessingEngine.computeInSampleSize(2048, 2048, 128))
        assertEquals(1080, com.example.engine.ImageProcessingEngine.quantizeDecodeDimension(4096, isExport = false))
        assertEquals(256, com.example.engine.ImageProcessingEngine.quantizeDecodeDimension(240, isExport = false))
        assertEquals(128, com.example.engine.ImageProcessingEngine.quantizeDecodeDimension(120, isExport = false))

        val realClip = VideoClip(
            id = "real_1",
            name = "Camera Clip",
            uriString = "content://media/external/video/media/42",
            durationMs = 8000L,
            trimEndMs = 8000L
        )
        val starterClip = VideoClip(
            id = "starter_1",
            name = "Starter",
            uriString = null,
            durationMs = 4000L,
            trimEndMs = 4000L
        )

        val cleanState = VideoEditorState(clips = listOf(realClip))
        assertTrue(com.example.engine.ExoVideoPlaybackController.canUseHardwareExoSurface(cleanState, realClip))
        assertFalse(com.example.engine.ExoVideoPlaybackController.canUseHardwareExoSurface(cleanState, starterClip))
        assertFalse(
            com.example.engine.ExoVideoPlaybackController.canUseHardwareExoSurface(
                cleanState,
                realClip.copy(reversed = true)
            )
        )
        assertFalse(
            com.example.engine.ExoVideoPlaybackController.canUseHardwareExoSurface(
                cleanState.copy(colorGrade = cleanState.colorGrade.copy(lut = com.example.model.LutPreset.CINEMATIC_TEAL)),
                realClip
            )
        )
    }

    @Test
    fun gpuKeyframePreviewAndFullRangeInterpolation_workAcrossStarterAndFilteredClips() {
        val k1 = VideoKeyframe(
            id = "vf1",
            timeMs = 0L,
            xNorm = -0.5f,
            yNorm = -0.4f,
            scale = 1.0f,
            rotationDeg = 0f,
            opacity = 1f,
            easing = KeyframeEasing.LINEAR
        )
        val k2 = VideoKeyframe(
            id = "vf2",
            timeMs = 2000L,
            xNorm = 0.5f,
            yNorm = 0.4f,
            scale = 3.6f,
            rotationDeg = 180f,
            opacity = 0.5f,
            easing = KeyframeEasing.LINEAR
        )
        val starterAnimated = VideoClip(
            id = "starter_kf",
            name = "Starter",
            uriString = null,
            durationMs = 4000L,
            trimEndMs = 4000L,
            keyframes = listOf(k1, k2)
        )
        val baseState = VideoEditorState(
            clips = listOf(starterAnimated),
            selectedClipId = starterAnimated.id,
            playheadMs = 1000L
        )

        // Default VideoEditorState (starter clips & LUT-graded clips) qualifies for 60fps GPU graphicsLayer keyframe transforms
        assertTrue(VideoMediaEngine.canUseGpuPreviewTransform(baseState, starterAnimated))
        assertTrue(
            VideoMediaEngine.canUseGpuPreviewTransform(
                baseState.copy(colorGrade = baseState.colorGrade.copy(lut = com.example.model.LutPreset.CINEMATIC_TEAL)),
                starterAnimated
            )
        )
        // Active user PIP overlay cleanly falls back to software compositor
        assertFalse(
            VideoMediaEngine.canUseGpuPreviewTransform(
                baseState.copy(
                    pip = baseState.pip.copy(
                        enabled = true,
                        uriString = "content://media/external/images/media/7"
                    )
                ),
                starterAnimated
            )
        )

        // Midpoint at 1000ms smoothly interpolates offsetX, offsetY, scale (up to 4.0x), rotation, opacity
        val midTarget = resolveActiveKeyframeTarget(baseState)!!
        assertEquals("Video", midTarget.shortKind)
        assertEquals(0.0f, midTarget.currentX, 0.0001f)
        assertEquals(0.0f, midTarget.currentY, 0.0001f)
        assertEquals(2.3f, midTarget.currentScale, 0.0001f)
        assertEquals(90f, midTarget.currentRotationDeg, 0.0001f)
        assertEquals(0.75f, midTarget.currentOpacity, 0.0001f)
        assertNull(midTarget.activeKeyframe)
        assertTrue(midTarget.hasPrevKeyframe)
        assertTrue(midTarget.hasNextKeyframe)

        // Snapping onto k2 flips activeKeyframe non-null (for ◆- delete) and disables next jump
        val endTarget = resolveActiveKeyframeTarget(baseState.copy(playheadMs = 2000L))!!
        assertEquals("vf2", endTarget.activeKeyframe?.id)
        assertTrue(endTarget.hasPrevKeyframe)
        assertFalse(endTarget.hasNextKeyframe)
    }
}



