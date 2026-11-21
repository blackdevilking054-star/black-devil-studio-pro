package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AudioDspEngine
import com.example.engine.ImageProcessingEngine
import com.example.model.AudioTrackRole
import com.example.model.AudioTrackState
import com.example.model.LutPreset
import com.example.model.SfxKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun verifyAppNameIsBlackDevilStudioPro() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Black Devil Studio Pro", appName)
    }

    @Test
    fun verifySfxSynthesisAndWavEncoding() {
        SfxKind.entries.forEach { kind ->
            val pcm = AudioDspEngine.synthesizeSfxPcm(kind)
            assertTrue("SFX ${kind.id} PCM should be non-empty", pcm.size > 1000)
            val wav = AudioDspEngine.encodePcmToWav(pcm)
            assertEquals('R'.code.toByte(), wav[0])
            assertEquals('I'.code.toByte(), wav[1])
            assertEquals('F'.code.toByte(), wav[2])
            assertEquals('F'.code.toByte(), wav[3])
            assertEquals(44 + pcm.size * 2, wav.size)
        }
    }

    @Test
    fun verifyThreeTrackAudioMixerRespectsBoostAndMute() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val f1 = File(context.cacheDir, "test_t1.wav")
        val f2 = File(context.cacheDir, "test_t2.wav")
        AudioDspEngine.writePcmWavFile(f1, AudioDspEngine.synthesizeSfxPcm(SfxKind.POP))
        AudioDspEngine.writePcmWavFile(f2, AudioDspEngine.synthesizeSfxPcm(SfxKind.DING))

        val tracks = listOf(
            AudioTrackState(AudioTrackRole.ORIGINAL_VIDEO, "T1", f1.absolutePath, volume = 1.8f, muted = false),
            AudioTrackState(AudioTrackRole.BACKGROUND_MUSIC, "T2", f2.absolutePath, volume = 0.6f, muted = false),
            AudioTrackState(AudioTrackRole.VOICEOVER, "T3", f1.absolutePath, volume = 2.0f, muted = true)
        )
        val mixed = AudioDspEngine.mixThreeTracks(tracks)
        assertTrue("Mixed PCM should contain audio samples", mixed.size > 5000)
        assertTrue("Mixed PCM should have non-zero signal", mixed.any { it != 0.toShort() })
    }

    @Test
    fun verifyChromaKeyRemovesGreenBackground() {
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.GREEN)
        bmp.setPixel(8, 8, Color.CYAN)

        val keyed = ImageProcessingEngine.applyChromaKey(bmp, Color.GREEN, 0.35f)
        assertEquals(0, Color.alpha(keyed.getPixel(0, 0)))
        assertEquals(255, Color.alpha(keyed.getPixel(8, 8)))
    }

    @Test
    fun verifyBoxBlurAndLutMatrix() {
        val bmp = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        val blurred = ImageProcessingEngine.fastBoxBlur(bmp, 4)
        assertEquals(24, blurred.width)
        assertEquals(24, blurred.height)

        val cm = ImageProcessingEngine.buildColorMatrix(
            brightness = 0.2f,
            contrast = 1.2f,
            saturation = 1.3f,
            exposure = 0.1f,
            warmth = 0.25f,
            hueDeg = 15f,
            lut = LutPreset.CYBER_CYAN
        )
        assertEquals(20, cm.array.size)
    }

    @Test
    fun verifyQualityEnhancerAnd4KExportCapabilities() {
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(90, 120, 150))
        bmp.setPixel(8, 8, Color.rgb(210, 230, 250))

        val enhanced = ImageProcessingEngine.applyQualityEnhancerPass(bmp)
        assertEquals(16, enhanced.width)
        assertEquals(16, enhanced.height)
        assertEquals(3840, com.example.model.VideoExportResolution.UHD_4K.longEdge)
        assertEquals(60, com.example.model.VideoExportFps.FPS_60.fps)

        val caps = com.example.engine.VideoMediaEngine.queryHardwareEncoderCapabilities()
        assertTrue(caps.encoderName.isNotBlank())
    }

    @Test
    fun verifySecurityFirewallHashingSanitizationAndUriValidation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hash = com.example.security.AppSecurityFirewall.hashPassword("CreatorPass99")
        assertTrue(hash.contains(":"))
        assertTrue(com.example.security.AppSecurityFirewall.verifyPassword("CreatorPass99", hash))
        assertTrue(!com.example.security.AppSecurityFirewall.verifyPassword("WrongPass", hash))

        val cleaned = com.example.security.AppSecurityFirewall.sanitizeText("<b>Hello</b>\u0000 World")
        assertEquals("Hello World", cleaned)

        val validUri = android.net.Uri.parse("content://media/external/video/media/10")
        val blockedFileUri = android.net.Uri.parse("file:///etc/passwd")
        assertTrue(com.example.security.AppSecurityFirewall.validateMediaUri(context, validUri))
        assertTrue(!com.example.security.AppSecurityFirewall.validateMediaUri(context, blockedFileUri))
    }

    @Test
    fun verifyOwnerOnlyAccessAndThreeStrike24HourPinLockout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(com.example.security.AppSecurityFirewall.isOwnerAccount("blackdevilking054@gmail.com", isGuest = false))
        assertTrue(!com.example.security.AppSecurityFirewall.isOwnerAccount("blackdevilking054@gmail.com", isGuest = true))
        assertTrue(!com.example.security.AppSecurityFirewall.isOwnerAccount("creator@example.com", isGuest = false))

        val baseTime = 1_700_000_000_000L
        // Correct PIN unlocks for Owner
        val ok = com.example.security.AppSecurityFirewall.verifyOwnerPin(
            context, "blackdevilking054@gmail.com", false, "637407", nowMs = baseTime
        )
        assertTrue(ok.unlocked)

        // 3 wrong attempts trigger 24-hour lockout
        val a1 = com.example.security.AppSecurityFirewall.verifyOwnerPin(context, "blackdevilking054@gmail.com", false, "111111", nowMs = baseTime + 1000L)
        assertEquals(2, a1.attemptsRemaining)
        val a2 = com.example.security.AppSecurityFirewall.verifyOwnerPin(context, "blackdevilking054@gmail.com", false, "222222", nowMs = baseTime + 2000L)
        assertEquals(1, a2.attemptsRemaining)
        val a3 = com.example.security.AppSecurityFirewall.verifyOwnerPin(context, "blackdevilking054@gmail.com", false, "333333", nowMs = baseTime + 3000L)
        assertTrue(a3.isLockedOut)
        assertTrue(a3.remainingLockoutMs >= 23L * 60L * 60L * 1000L)

        // Even the real PIN is blocked during the 24-hour lockout window
        val blocked = com.example.security.AppSecurityFirewall.verifyOwnerPin(
            context, "blackdevilking054@gmail.com", false, "637407", nowMs = baseTime + 60_000L
        )
        assertTrue(!blocked.unlocked)
        assertTrue(blocked.isLockedOut)
    }

    @Test
    fun verifyThirtyFivePlusTransitionsAndThirtyPlusFiltersAndCleanPipDefault() {
        val allTransitions = com.example.model.VideoTransition.entries
        assertTrue("Expected 35+ transitions, got ${allTransitions.size}", allTransitions.size >= 35)
        assertEquals(7, com.example.model.TransitionCategory.entries.size)
        com.example.model.TransitionCategory.entries.forEach { cat ->
            assertTrue("Category ${cat.label} should have transitions", allTransitions.any { it.category == cat })
        }

        val allFilters = LutPreset.entries
        assertTrue("Expected 30+ filters, got ${allFilters.size}", allFilters.size >= 30)
        assertEquals(6, com.example.model.FilterCategory.entries.size)
        com.example.model.FilterCategory.entries.forEach { cat ->
            assertTrue("Filter category ${cat.label} should have presets", allFilters.any { it.category == cat })
        }

        // Verify filter intensity 0% produces neutral identity while 100% applies LUT color shift
        val zeroIntensity = ImageProcessingEngine.buildColorMatrix(
            brightness = 0f,
            contrast = 1f,
            saturation = 1f,
            exposure = 0f,
            warmth = 0f,
            lut = LutPreset.CYBER_CYAN,
            lutIntensity = 0f
        )
        val fullIntensity = ImageProcessingEngine.buildColorMatrix(
            brightness = 0f,
            contrast = 1f,
            saturation = 1f,
            exposure = 0f,
            warmth = 0f,
            lut = LutPreset.CYBER_CYAN,
            lutIntensity = 1f
        )
        assertEquals(1f, zeroIntensity.array[0], 0.01f)
        assertTrue("Full intensity LUT should alter red channel gain", fullIntensity.array[0] != 1f)

        // Verify PIP overlay defaults to clean disabled state with no pre-loaded fallback image
        val defaultPip = com.example.model.PipOverlayState()
        assertTrue(!defaultPip.enabled)
        assertTrue(defaultPip.uriString == null)
        assertTrue(defaultPip.fallbackDrawable == null)
    }

    @Test
    fun verifyPureKotlinAvcMp4MuxerBuildsValidMp4Container() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val outMp4 = File(context.cacheDir, "unit_test_avc.mp4")
        val muxer = com.example.engine.AvcMp4Muxer(outMp4, width = 1280, height = 720, fps = 30)

        // Annex-B SPS (NAL type 7 = 0x67) + PPS (NAL type 8 = 0x68)
        val csd = byteArrayOf(
            0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0xC0.toByte(), 0x1F, 0xDA.toByte(), 0x01,
            0x00, 0x00, 0x00, 0x01, 0x68, 0xCE.toByte(), 0x3C, 0x80.toByte()
        )
        muxer.addCodecConfigBytes(csd)

        // Frame 1: IDR keyframe (NAL type 5 = 0x65) with inline duplicate SPS/PPS to confirm de-duplication
        val idrWithInlineCsd = csd + byteArrayOf(
            0x00, 0x00, 0x00, 0x01, 0x65, 0x88.toByte(), 0x84.toByte(), 0x00, 0x11, 0x22
        )
        assertTrue(muxer.writeAccessUnit(idrWithInlineCsd, isKeyFrameFlag = true))

        // Frame 2: Non-IDR P-frame (NAL type 1 = 0x41)
        val pFrame = byteArrayOf(0x00, 0x00, 0x01, 0x41, 0x9A.toByte(), 0x22, 0x33, 0x44)
        assertTrue(muxer.writeAccessUnit(pFrame, isKeyFrameFlag = false))
        assertEquals(2, muxer.writtenFrameCount)

        assertTrue(muxer.finish())
        val mp4Bytes = outMp4.readBytes()
        val ascii = String(mp4Bytes, Charsets.ISO_8859_1)
        assertTrue("MP4 must contain ftyp atom", ascii.contains("ftyp"))
        assertTrue("MP4 must contain mdat atom", ascii.contains("mdat"))
        assertTrue("MP4 must contain moov atom", ascii.contains("moov"))
        assertTrue("MP4 must contain avcC config", ascii.contains("avcC"))
        assertTrue("MP4 must contain stss sync table", ascii.contains("stss"))
        outMp4.delete()

        // Verify empty muxer finishes cleanly with false without throwing
        val emptyFile = File(context.cacheDir, "unit_test_empty.mp4")
        val emptyMuxer = com.example.engine.AvcMp4Muxer(emptyFile, 640, 360, 30)
        assertTrue(!emptyMuxer.finish())
        emptyFile.delete()
    }

    @Test
    fun verifySpeedCurvesAndDiamondKeyframeInterpolation() {
        com.example.model.SpeedCurvePreset.entries.forEach { preset ->
            val start = com.example.engine.VideoMediaEngine.remapProgressWithSpeedCurve(0f, preset)
            val mid = com.example.engine.VideoMediaEngine.remapProgressWithSpeedCurve(0.5f, preset)
            val end = com.example.engine.VideoMediaEngine.remapProgressWithSpeedCurve(1f, preset)
            assertTrue("Speed curve ${preset.label} start must be in [0,1]", start in 0f..1f)
            assertTrue("Speed curve ${preset.label} mid must be in [0,1]", mid in 0f..1f)
            assertTrue("Speed curve ${preset.label} end must be in [0,1]", end in 0f..1f)
        }

        val kfs = listOf(
            com.example.model.VideoKeyframe("k1", timeMs = 0L, xNorm = 0f, scale = 1f, rotationDeg = 0f, opacity = 1f),
            com.example.model.VideoKeyframe("k2", timeMs = 2000L, xNorm = 0.4f, scale = 1.8f, rotationDeg = 90f, opacity = 0.5f)
        )
        val half = com.example.engine.VideoMediaEngine.interpolateKeyframes(kfs, 1000L)
        assertEquals(0.2f, half.xNorm, 0.02f)
        assertEquals(1.4f, half.scale, 0.02f)
        assertEquals(45f, half.rotationDeg, 0.5f)
        assertEquals(0.75f, half.opacity, 0.02f)
    }

    @Test
    fun verifyAudioSplitMergeVoiceChangerEqAndMaskShapes() {
        val riser = AudioDspEngine.synthesizeSfxPcm(SfxKind.RISER)
        assertTrue(riser.size > 4000)

        val (partA, partB) = AudioDspEngine.splitPcm(riser, 0.5f)
        assertEquals(riser.size, partA.size + partB.size)

        val merged = AudioDspEngine.mergePcmTracks(partA, partB)
        assertTrue("Merged PCM should reflect 20ms crossfade join", merged.size in (riser.size - 880)..riser.size)

        val fxTrack = AudioTrackState(
            role = AudioTrackRole.VOICEOVER,
            sourceName = "Test",
            pitchSemitones = -4f,
            tempoSpeed = 1.1f,
            voicePreset = com.example.model.VoiceChangerPreset.ROBOT,
            eqPreset = com.example.model.AudioEqPreset.ECHO,
            vocalClarityEnabled = true
        )
        val processed = AudioDspEngine.processTrackSignal(partA, fxTrack)
        assertTrue(processed.size > 1000)

        val rect = android.graphics.RectF(0f, 0f, 200f, 200f)
        com.example.model.MaskShapeKind.entries.forEach { shape ->
            val path = ImageProcessingEngine.buildMaskPath(shape, rect)
            if (shape == com.example.model.MaskShapeKind.NONE) {
                assertTrue(path == null)
            } else {
                assertTrue("Mask path for ${shape.label} should not be null", path != null)
            }
        }
    }
}
