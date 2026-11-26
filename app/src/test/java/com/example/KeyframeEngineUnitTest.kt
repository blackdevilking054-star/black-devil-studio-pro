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
}

