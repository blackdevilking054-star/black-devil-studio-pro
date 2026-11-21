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

@RunWith(RobolectricTestRunner::class)
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
}

