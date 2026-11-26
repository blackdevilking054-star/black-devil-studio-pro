package com.example

import android.app.Application
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ImageProcessingEngine
import com.example.engine.VideoMediaEngine
import com.example.model.VideoEditorState
import com.example.model.VideoKeyframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoEditorFixesTest {

    @Test
    fun renderPreviewAtPlayhead_emptyClips_rendersPureBlackZeroWhitePixels() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val emptyState = VideoEditorState(
            clips = emptyList(),
            textLayers = emptyList(),
            audioLaneClips = emptyList(),
            playheadMs = 0L
        )
        assertEquals(0L, emptyState.totalDurationMs)

        val bmp = VideoMediaEngine.renderPreviewAtPlayhead(context, emptyState, maxEdge = 160)
        try {
            assertEquals(Color.BLACK, bmp.getPixel(0, 0))
            assertEquals(Color.BLACK, bmp.getPixel(bmp.width / 2, bmp.height / 2))
            assertEquals(Color.BLACK, bmp.getPixel(bmp.width - 1, bmp.height - 1))
        } finally {
            bmp.recycle()
        }
    }

    @Test
    fun clampFrameCenter_keepsSmallerAndLargerFramesBoundedInViewport() {
        // Smaller frame (400px) inside 1000px viewport: center stays in [200, 800]
        assertEquals(200f, ImageProcessingEngine.clampFrameCenter(50f, 400f, 1000f), 0.01f)
        assertEquals(800f, ImageProcessingEngine.clampFrameCenter(980f, 400f, 1000f), 0.01f)
        assertEquals(500f, ImageProcessingEngine.clampFrameCenter(500f, 400f, 1000f), 0.01f)

        // Larger zoomed frame (1600px) inside 1000px viewport: center stays in [200, 800] so edges never leave black gap
        assertEquals(200f, ImageProcessingEngine.clampFrameCenter(-400f, 1600f, 1000f), 0.01f)
        assertEquals(800f, ImageProcessingEngine.clampFrameCenter(1400f, 1600f, 1000f), 0.01f)
    }

    @Test
    fun interpolateKeyframes_smoothlyInterpolatesPositionScaleAndOpacity() {
        val kfs = listOf(
            VideoKeyframe(id = "k1", timeMs = 0L, xNorm = -0.4f, yNorm = 0.0f, scale = 0.8f, rotationDeg = 0f, opacity = 0.2f),
            VideoKeyframe(id = "k2", timeMs = 1000L, xNorm = 0.4f, yNorm = 0.2f, scale = 1.6f, rotationDeg = 90f, opacity = 1.0f)
        )
        val start = VideoMediaEngine.interpolateKeyframes(kfs, 0L)
        val mid = VideoMediaEngine.interpolateKeyframes(kfs, 500L)
        val quarter = VideoMediaEngine.interpolateKeyframes(kfs, 250L)
        val end = VideoMediaEngine.interpolateKeyframes(kfs, 1000L)

        assertEquals(-0.4f, start.xNorm, 0.001f)
        assertEquals(0.2f, start.opacity, 0.001f)
        // Smoothstep at t=0.5 equals 0.5 exactly
        assertEquals(0.0f, mid.xNorm, 0.001f)
        assertEquals(1.2f, mid.scale, 0.001f)
        assertEquals(0.6f, mid.opacity, 0.001f)
        // Smoothstep eases out gently at t=0.25 -> ease = 0.15625 (less than linear 0.25)
        assertTrue(quarter.scale < 1.0f)
        assertEquals(0.4f, end.xNorm, 0.001f)
        assertEquals(1.6f, end.scale, 0.001f)
        assertEquals(1.0f, end.opacity, 0.001f)
    }

    @Test
    fun measureOverlayBounds_dynamicallyHugsScaleAndStickerCrop() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val smallText = com.example.model.VideoTextOverlay(
            id = "t1",
            text = "PRO TITLE",
            fontId = "bold_sans",
            scale = 0.6f
        )
        val largeText = smallText.copy(scale = 1.8f)
        val bSmall = com.example.engine.VideoOverlayRenderer.measureOverlayBounds(context, smallText, 1000f, 600f)
        val bLarge = com.example.engine.VideoOverlayRenderer.measureOverlayBounds(context, largeText, 1000f, 600f)
        assertTrue(bSmall.width < bLarge.width)
        assertTrue(bSmall.height < bLarge.height)
        assertTrue(bSmall.width < 1000f)

        val fullSticker = com.example.model.VideoTextOverlay(
            id = "s1",
            text = "LIVE",
            isSticker = true,
            stickerBadgeId = "badge_live",
            scale = 1.0f
        )
        val croppedSticker = fullSticker.copy(cropLeft = 0.25f, cropRight = 0.75f, cropTop = 0.2f, cropBottom = 0.8f)
        val bFull = com.example.engine.VideoOverlayRenderer.measureOverlayBounds(context, fullSticker, 1000f, 600f)
        val bCrop = com.example.engine.VideoOverlayRenderer.measureOverlayBounds(context, croppedSticker, 1000f, 600f)
        assertEquals(bFull.width * 0.5f, bCrop.width, 2f)
        assertEquals(bFull.height * 0.6f, bCrop.height, 2f)
    }

    @Test
    fun proTextAndStickerAnimations_renderCleanlyAcrossAllModes() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val proFonts = com.example.ui.theme.StudioFonts.proVideoPresets
        assertEquals(5, proFonts.size)
        assertEquals(
            listOf("Bold Sans", "Cinematic Serif", "Neon Mono", "Street Script", "Impact Display"),
            proFonts.map { it.displayName }
        )

        // Render every text animation (including Neon Glitch) + every sticker animation & category without crash
        val layers = com.example.model.TextAnimationStyle.entries.mapIndexed { i, anim ->
            com.example.model.VideoTextOverlay(
                id = "txt_$i",
                text = "ANIM $i",
                fontId = proFonts[i % proFonts.size].id,
                animation = anim,
                animationSpeed = 1.5f,
                rotationDeg = 12f,
                opacity = 0.85f,
                bgEnabled = i % 2 == 0,
                startMs = 0L,
                endMs = 4000L
            )
        } + com.example.model.StickerAnimationStyle.entries.mapIndexed { i, sAnim ->
            val catItem = com.example.engine.VideoStickerCatalog.items[i % com.example.engine.VideoStickerCatalog.items.size]
            com.example.model.VideoTextOverlay(
                id = "stk_$i",
                text = catItem.label,
                isSticker = true,
                stickerBadgeId = catItem.badgeId,
                stickerAnimation = sAnim,
                flipH = i % 2 == 0,
                flipV = i % 3 == 0,
                opacity = 0.9f,
                cropLeft = 0.1f,
                cropTop = 0.1f,
                cropRight = 0.9f,
                cropBottom = 0.9f,
                startMs = 0L,
                endMs = 4000L
            )
        }

        val state = VideoEditorState(
            clips = emptyList(),
            textLayers = layers,
            playheadMs = 450L
        )
        val bmp = VideoMediaEngine.renderPreviewAtPlayhead(context, state, maxEdge = 240)
        try {
            assertEquals(240, maxOf(bmp.width, bmp.height))
            // Top-left corner stays clean pure black (no white flash)
            assertEquals(Color.BLACK, bmp.getPixel(0, 0))
        } finally {
            bmp.recycle()
        }
    }

    @Test
    fun keyframeEasingCurves_andHoldBeforeAfterFirstLast_workAccurately() {
        val baseStart = VideoKeyframe(
            id = "kf_a",
            timeMs = 500L,
            xNorm = 0.0f,
            yNorm = 0.0f,
            scale = 1.0f,
            rotationDeg = 0f,
            opacity = 0.0f
        )
        val baseEnd = VideoKeyframe(
            id = "kf_b",
            timeMs = 1500L,
            xNorm = 0.8f,
            yNorm = -0.4f,
            scale = 2.0f,
            rotationDeg = 90f,
            opacity = 1.0f
        )

        // Hold first keyframe before 500ms, hold last keyframe after 1500ms
        val before = VideoMediaEngine.interpolateKeyframes(listOf(baseStart, baseEnd), 100L)
        assertEquals(0.0f, before.xNorm, 0.001f)
        assertEquals(1.0f, before.scale, 0.001f)
        assertEquals(0.0f, before.opacity, 0.001f)

        val after = VideoMediaEngine.interpolateKeyframes(listOf(baseStart, baseEnd), 2500L)
        assertEquals(0.8f, after.xNorm, 0.001f)
        assertEquals(2.0f, after.scale, 0.001f)
        assertEquals(90f, after.rotationDeg, 0.001f)
        assertEquals(1.0f, after.opacity, 0.001f)

        // Check all 4 easing curves at t = 0.25 (localMs = 750L)
        val lin = VideoMediaEngine.interpolateKeyframes(
            listOf(baseStart.copy(easing = com.example.model.KeyframeEasing.LINEAR), baseEnd),
            750L
        )
        val easeIn = VideoMediaEngine.interpolateKeyframes(
            listOf(baseStart.copy(easing = com.example.model.KeyframeEasing.EASE_IN), baseEnd),
            750L
        )
        val easeOut = VideoMediaEngine.interpolateKeyframes(
            listOf(baseStart.copy(easing = com.example.model.KeyframeEasing.EASE_OUT), baseEnd),
            750L
        )
        val easeInOut = VideoMediaEngine.interpolateKeyframes(
            listOf(baseStart.copy(easing = com.example.model.KeyframeEasing.EASE_IN_OUT), baseEnd),
            750L
        )

        // Linear at t=0.25 -> 1.25 scale
        assertEquals(1.25f, lin.scale, 0.001f)
        // Ease-In starts slow (t^3 = 0.015625) < Ease-In-Out (0.15625) < Linear (0.25) < Ease-Out (0.578125)
        assertTrue(easeIn.scale < easeInOut.scale)
        assertTrue(easeInOut.scale < lin.scale)
        assertTrue(lin.scale < easeOut.scale)

        // Segment detection strictly between keyframes
        val segMid = VideoMediaEngine.findActiveKeyframeSegment(listOf(baseStart, baseEnd), 1000L)
        assertTrue(segMid != null && segMid.first.id == "kf_a" && segMid.second.id == "kf_b")
        assertTrue(VideoMediaEngine.findActiveKeyframeSegment(listOf(baseStart, baseEnd), 200L) == null)
        assertTrue(VideoMediaEngine.findActiveKeyframeSegment(listOf(baseStart, baseEnd), 1800L) == null)
    }

    @Test
    fun textAndStickerKeyframes_interpolateBoundsAndRenderSmoothly() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val animatedSticker = com.example.model.VideoTextOverlay(
            id = "stk_anim",
            text = "LIVE",
            isSticker = true,
            stickerBadgeId = "badge_live",
            startMs = 1000L,
            endMs = 5000L,
            xNorm = 0.2f,
            yNorm = 0.2f,
            scale = 0.8f,
            rotationDeg = 0f,
            opacity = 1f,
            keyframes = listOf(
                VideoKeyframe(
                    id = "sk1",
                    timeMs = 0L,
                    xNorm = 0.2f,
                    yNorm = 0.3f,
                    scale = 0.8f,
                    rotationDeg = 0f,
                    opacity = 0.4f,
                    easing = com.example.model.KeyframeEasing.LINEAR
                ),
                VideoKeyframe(
                    id = "sk2",
                    timeMs = 2000L,
                    xNorm = 0.8f,
                    yNorm = 0.7f,
                    scale = 1.6f,
                    rotationDeg = 180f,
                    opacity = 1.0f,
                    easing = com.example.model.KeyframeEasing.LINEAR
                )
            )
        )

        // Global playhead 2000ms = local 1000ms (exact midpoint between 0ms and 2000ms)
        val resolvedMid = com.example.engine.VideoOverlayRenderer.resolveOverlayTransformAt(animatedSticker, 2000L)
        assertEquals(0.5f, resolvedMid.xNorm, 0.001f)
        assertEquals(0.5f, resolvedMid.yNorm, 0.001f)
        assertEquals(1.2f, resolvedMid.scale, 0.001f)
        assertEquals(90f, resolvedMid.rotationDeg, 0.001f)
        assertEquals(0.7f, resolvedMid.opacity, 0.001f)

        // Bounding box at 1000ms vs 3000ms tracks exact animated center, scale, and rotation
        val bStart = com.example.engine.VideoOverlayRenderer.measureOverlayBounds(context, animatedSticker, 1000f, 500f, playheadMs = 1000L)
        val bEnd = com.example.engine.VideoOverlayRenderer.measureOverlayBounds(context, animatedSticker, 1000f, 500f, playheadMs = 3000L)
        assertEquals(200f, bStart.cx, 1f)
        assertEquals(800f, bEnd.cx, 1f)
        assertTrue(bEnd.width > bStart.width)
        assertEquals(180f, bEnd.rotationDeg, 0.01f)
    }

    @Test
    fun viewModelKeyframeEngine_toggleAutoKeyframeEasingAndJumpNavigation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = com.example.viewmodel.XonixStudioViewModel(app)

        // 1. Select first video clip and verify capture of current transform on ◆+
        val firstClipId = vm.videoState.value.clips.first().id
        vm.selectClip(firstClipId)
        vm.seekPlayhead(0L)
        vm.updateVideoCanvasTransform(zoom = 1.35f, panX = 0.25f, panY = -0.15f, rotationDeg = 15f)
        vm.toggleKeyframeAtPlayhead()

        val afterFirstKf = vm.videoState.value.clips.first().keyframes
        assertEquals(1, afterFirstKf.size)
        assertEquals(0L, afterFirstKf[0].timestampMs)
        assertEquals(1.35f, afterFirstKf[0].scale, 0.001f)
        assertEquals(0.25f, afterFirstKf[0].offsetX, 0.001f)
        assertEquals(-0.15f, afterFirstKf[0].offsetY, 0.001f)
        assertEquals(15f, afterFirstKf[0].rotation, 0.001f)

        // 2. Auto-Keyframe: move playhead to 1500ms and drag/scale on canvas WITHOUT tapping ◆+
        vm.seekPlayhead(1500L)
        vm.adjustVideoCanvasTransform(zoomDelta = 0.35f, panDeltaX = -0.50f, rotationDeltaDeg = 30f)

        val afterAutoKf = vm.videoState.value.clips.first().keyframes
        assertEquals(2, afterAutoKf.size)
        assertEquals(1500L, afterAutoKf[1].timestampMs)
        assertEquals(1.70f, afterAutoKf[1].scale, 0.01f)
        assertEquals(-0.25f, afterAutoKf[1].offsetX, 0.01f)
        assertEquals(45f, afterAutoKf[1].rotation, 0.01f)

        // 3. Move playhead between the 2 keyframes (750ms) and set Easing Curve to EASE_OUT
        vm.seekPlayhead(750L)
        vm.setKeyframeSegmentEasing(com.example.model.KeyframeEasing.EASE_OUT)
        assertEquals(
            com.example.model.KeyframeEasing.EASE_OUT,
            vm.videoState.value.clips.first().keyframes.first().easing
        )

        // 4. Jump <◆ (Previous) -> 0ms, then ◆> (Next) -> 1500ms, then ◆- deletes keyframe at 1500ms
        vm.jumpToPreviousKeyframe()
        assertEquals(0L, vm.videoState.value.playheadMs)
        vm.jumpToNextKeyframe()
        assertEquals(1500L, vm.videoState.value.playheadMs)
        vm.toggleKeyframeAtPlayhead()
        assertEquals(1, vm.videoState.value.clips.first().keyframes.size)
        assertEquals(0L, vm.videoState.value.clips.first().keyframes.first().timestampMs)

        // 5. Text / Sticker layer auto-keyframing
        vm.addVideoText("ANIMATED TITLE")
        val textId = vm.videoState.value.selectedTextId!!
        vm.seekPlayhead(1500L)
        vm.toggleKeyframeAtPlayhead() // KF1 at local 0ms (since startMs = 1500ms)
        vm.seekPlayhead(2500L)
        vm.updateSelectedVideoText { it.copy(xNorm = 0.75f, opacity = 0.3f) } // Auto-KF2 at local 1000ms
        val updatedText = vm.videoState.value.textLayers.first { it.id == textId }
        assertEquals(2, updatedText.keyframes.size)
        assertEquals(0.75f, updatedText.keyframes[1].offsetX, 0.001f)
        assertEquals(0.3f, updatedText.keyframes[1].opacity, 0.001f)
    }
}


