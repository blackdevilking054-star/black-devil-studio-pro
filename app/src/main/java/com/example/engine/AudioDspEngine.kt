package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import com.example.model.AudioEqPreset
import com.example.model.AudioExportFormat
import com.example.model.AudioTrackState
import com.example.model.SfxKind
import com.example.model.VoiceChangerPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

object AudioDspEngine {

    const val SAMPLE_RATE = 44100

    /**
     * Synthesizes 8 royalty-free studio SFX directly in 44.1kHz 16-bit PCM.
     */
    fun synthesizeSfxPcm(kind: SfxKind): ShortArray {
        val sr = SAMPLE_RATE.toDouble()
        return when (kind) {
            SfxKind.POP -> {
                val len = (sr * 0.16).toInt()
                ShortArray(len) { i ->
                    val t = i / sr
                    val env = exp(-t * 28.0)
                    val freq = 180.0 + 920.0 * exp(-t * 36.0)
                    (sin(2.0 * PI * freq * t) * env * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            SfxKind.WHOOSH -> {
                val len = (sr * 0.72).toInt()
                val rng = Random(42)
                var lp = 0.0
                var lp2 = 0.0
                ShortArray(len) { i ->
                    val u = i.toDouble() / len
                    val env = sin(PI * u) * sin(PI * u)
                    val cutoff = 0.03 + 0.34 * sin(PI * u)
                    val white = rng.nextDouble(-1.0, 1.0)
                    lp += cutoff * (white - lp)
                    lp2 += (cutoff * 0.65) * (lp - lp2)
                    val sample = (lp - lp2 * 0.5) * env * 2.8
                    (tanh(sample) * 29000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            SfxKind.BASS_DROP -> {
                val len = (sr * 1.15).toInt()
                var phase = 0.0
                ShortArray(len) { i ->
                    val t = i / sr
                    val freq = 34.0 + 130.0 * exp(-t * 4.8)
                    phase += 2.0 * PI * freq / sr
                    val env = exp(-t * 2.1) * min(1.0, t / 0.012)
                    val raw = sin(phase) * 1.65 + 0.25 * sin(phase * 2.0)
                    (tanh(raw) * env * 31000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            SfxKind.BOOM -> {
                val len = (sr * 0.95).toInt()
                val rng = Random(99)
                var lp = 0.0
                var phase = 0.0
                ShortArray(len) { i ->
                    val t = i / sr
                    val freq = 38.0 + 95.0 * exp(-t * 14.0)
                    phase += 2.0 * PI * freq / sr
                    val noise = rng.nextDouble(-1.0, 1.0)
                    lp += 0.08 * (noise - lp)
                    val impact = sin(phase) * exp(-t * 3.8) + lp * 1.4 * exp(-t * 5.2)
                    (tanh(impact * 1.6) * 31500.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            SfxKind.HITMARKER -> {
                val len = (sr * 0.14).toInt()
                val rng = Random(7)
                ShortArray(len) { i ->
                    val t = i / sr
                    val click1 = exp(-t * 95.0) * (sin(2.0 * PI * 3200.0 * t) + rng.nextDouble(-0.5, 0.5))
                    val t2 = (t - 0.032).coerceAtLeast(0.0)
                    val click2 = if (t >= 0.032) exp(-t2 * 110.0) * sin(2.0 * PI * 4400.0 * t2) else 0.0
                    ((click1 * 0.7 + click2 * 0.9) * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            SfxKind.LASER -> {
                val len = (sr * 0.36).toInt()
                var phase = 0.0
                ShortArray(len) { i ->
                    val t = i / sr
                    val freq = 180.0 + 2100.0 * exp(-t * 11.5)
                    phase += 2.0 * PI * freq / sr
                    val saw = ((phase / PI) % 2.0) - 1.0
                    val env = exp(-t * 5.2) * min(1.0, t / 0.005)
                    ((saw * 0.6 + sin(phase) * 0.4) * env * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            SfxKind.DING -> {
                val len = (sr * 0.90).toInt()
                ShortArray(len) { i ->
                    val t = i / sr
                    val env = exp(-t * 4.4) * min(1.0, t / 0.004)
                    val s = sin(2.0 * PI * 1046.5 * t) * 0.6 +
                        sin(2.0 * PI * 2093.0 * t) * 0.28 +
                        sin(2.0 * PI * 3136.0 * t) * 0.12
                    (s * env * 29000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            SfxKind.RISER -> {
                val len = (sr * 1.4).toInt()
                val rng = Random(1337)
                var phase = 0.0
                var lp = 0.0
                ShortArray(len) { i ->
                    val u = i.toDouble() / len
                    val freq = 110.0 + 1450.0 * (u * u)
                    phase += 2.0 * PI * freq / sr
                    val white = rng.nextDouble(-1.0, 1.0)
                    lp += (0.04 + 0.38 * u) * (white - lp)
                    val env = (u * u) * min(1.0, (1.0 - u) * 28.0)
                    val sig = (sin(phase) * 0.6 + lp * 0.8) * env * 1.8
                    (tanh(sig) * 30000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
        }
    }

    /**
     * Generates a clean royalty-free ambient synth loop PCM.
     */
    fun generateStarterBeatPcm(durationSec: Double = 4.0, isVoiceDemo: Boolean = false): ShortArray {
        val len = (SAMPLE_RATE * durationSec).toInt()
        val notes = if (isVoiceDemo) doubleArrayOf(220.0, 261.63, 329.63, 293.66) else doubleArrayOf(130.81, 164.81, 196.0, 246.94)
        return ShortArray(len) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val beatIdx = ((t * 2.0).toInt()) % notes.size
            val beatLocal = (t * 2.0) % 1.0
            val f = notes[beatIdx]
            val env = exp(-beatLocal * 3.5) * min(1.0, beatLocal / 0.02)
            val kick = if (!isVoiceDemo && beatLocal < 0.15) {
                sin(2.0 * PI * (48.0 + 70.0 * exp(-beatLocal * 24.0)) * beatLocal) * exp(-beatLocal * 16.0) * 0.65
            } else 0.0
            val pad = (sin(2.0 * PI * f * t) * 0.45 + sin(2.0 * PI * f * 2.0 * t) * 0.18) * env
            (tanh((pad + kick) * 1.2) * 18500.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    fun splitPcm(pcm: ShortArray, splitPct: Float): Pair<ShortArray, ShortArray> {
        if (pcm.size < 4) return pcm to ShortArray(0)
        val idx = (pcm.size * splitPct.coerceIn(0.1f, 0.9f)).toInt().coerceIn(2, pcm.size - 2)
        return pcm.copyOfRange(0, idx) to pcm.copyOfRange(idx, pcm.size)
    }

    fun mergePcmTracks(first: ShortArray, second: ShortArray): ShortArray {
        if (first.isEmpty()) return second.copyOf()
        if (second.isEmpty()) return first.copyOf()
        val fadeLen = min(880, min(first.size / 4, second.size / 4))
        val total = first.size + second.size - fadeLen
        val out = ShortArray(total)
        val nonOverlapFirst = first.size - fadeLen
        first.copyInto(out, 0, 0, nonOverlapFirst)
        for (i in 0 until fadeLen) {
            val ratio = i.toFloat() / fadeLen.coerceAtLeast(1)
            val s1 = first[nonOverlapFirst + i] * (1f - ratio)
            val s2 = second[i] * ratio
            out[nonOverlapFirst + i] = (s1 + s2).roundToInt().coerceIn(-32767, 32767).toShort()
        }
        second.copyInto(out, first.size, fadeLen, second.size)
        return out
    }

    fun encodePcmToWav(pcm: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val byteLen = pcm.size * 2
        val out = ByteArrayOutputStream(44 + byteLen)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + byteLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(byteLen)
        out.write(header.array())

        val dataBuf = ByteBuffer.allocate(byteLen).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) dataBuf.putShort(s)
        out.write(dataBuf.array())
        return out.toByteArray()
    }

    /**
     * Encodes mono 44.1kHz PCM to AAC-LC ADTS frames without MediaMuxer (zero MPEG4Writer risk).
     * Falls back cleanly to WAV bytes if hardware AAC encoder is unavailable.
     */
    fun encodePcmForFormat(pcm: ShortArray, format: AudioExportFormat): Pair<ByteArray, AudioExportFormat> {
        if (format == AudioExportFormat.WAV) {
            return encodePcmToWav(pcm) to AudioExportFormat.WAV
        }
        val aac = runCatching { encodePcmToAdtsAac(pcm) }.getOrNull()
        return if (aac != null && aac.size > 64) {
            aac to AudioExportFormat.MP3_COMPAT
        } else {
            encodePcmToWav(pcm) to AudioExportFormat.WAV
        }
    }

    private fun encodePcmToAdtsAac(pcm: ShortArray): ByteArray? {
        if (pcm.isEmpty()) return null
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val out = ByteArrayOutputStream()
        return try {
            val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val pcmBytes = ByteArray(pcm.size * 2)
            ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm)

            val info = MediaCodec.BufferInfo()
            var offset = 0
            var inputDone = false
            var outputDone = false
            var idle = 0

            while (!outputDone && idle < 80) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)
                        if (buf != null && offset < pcmBytes.size) {
                            val chunk = min(buf.capacity(), pcmBytes.size - offset)
                            buf.clear()
                            buf.put(pcmBytes, offset, chunk)
                            val ptsUs = ((offset / 2).toLong() * 1_000_000L) / SAMPLE_RATE
                            codec.queueInputBuffer(inIdx, 0, chunk, ptsUs, 0)
                            offset += chunk
                        } else {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000L)
                if (outIdx >= 0) {
                    idle = 0
                    val outBuf = codec.getOutputBuffer(outIdx)
                    val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                    if (outBuf != null && info.size > 0 && !isConfig) {
                        val packetLen = info.size + 7
                        out.write(buildAdtsHeader(packetLen))
                        val aacData = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(aacData)
                        out.write(aacData)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                } else {
                    idle++
                }
            }
            out.toByteArray()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
    }

    private fun buildAdtsHeader(packetLen: Int): ByteArray {
        val profile = 2 // AAC LC
        val freqIdx = 4 // 44100 Hz
        val chanCfg = 1 // Mono
        val h = ByteArray(7)
        h[0] = 0xFF.toByte()
        h[1] = 0xF9.toByte()
        h[2] = (((profile - 1) shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        h[3] = (((chanCfg and 3) shl 6) + (packetLen shr 11)).toByte()
        h[4] = ((packetLen and 0x7FF) shr 3).toByte()
        h[5] = (((packetLen and 7) shl 5) + 0x1F).toByte()
        h[6] = 0xFC.toByte()
        return h
    }

    fun readWavPcm(file: File): ShortArray? {
        if (!file.exists() || file.length() <= 44L) return null
        val bytes = file.readBytes()
        if (bytes.size <= 44) return null
        val count = (bytes.size - 44) / 2
        val buf = ByteBuffer.wrap(bytes, 44, count * 2).order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(count) { buf.short }
    }

    fun writePcmWavFile(file: File, pcm: ShortArray): File {
        FileOutputStream(file).use { it.write(encodePcmToWav(pcm)) }
        return file
    }

    fun computeWaveformBars(pcm: ShortArray, bars: Int = 44): List<Float> {
        if (pcm.isEmpty()) return List(bars) { 0.08f }
        val step = max(1, pcm.size / bars)
        return List(bars) { b ->
            val start = b * step
            val end = min(pcm.size, start + step)
            var peak = 0
            for (i in start until end) {
                val v = abs(pcm[i].toInt())
                if (v > peak) peak = v
            }
            (peak / 32767f).coerceIn(0.06f, 1f)
        }
    }

    suspend fun playPcm(pcm: ShortArray) = withContext(Dispatchers.IO) {
        if (pcm.isEmpty()) return@withContext
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            track.play()
            val chunkShorts = 2048
            var offset = 0
            while (coroutineContext.isActive && offset < pcm.size) {
                val toWrite = min(chunkShorts, pcm.size - offset)
                val written = track.write(pcm, offset, toWrite, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }
            if (coroutineContext.isActive) {
                kotlinx.coroutines.delay(120L)
            }
            runCatching { track.stop() }
        } catch (_: Exception) {
            // AudioTrack fallback on headless/muted emulator
        } finally {
            runCatching { track.release() }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun recordVoiceoverToFile(
        outputFile: File,
        noiseReduction: Boolean,
        shouldContinue: () -> Boolean,
        onLevel: (Float, Int) -> Unit
    ): Pair<ShortArray, Long> = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2
        )

        val chunks = mutableListOf<ShortArray>()
        val readBuf = ShortArray(minBuf / 2)
        val startMs = System.currentTimeMillis()
        var hpPrevIn = 0.0
        var hpPrevOut = 0.0

        try {
            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording()
                while (coroutineContext.isActive && shouldContinue()) {
                    val read = recorder.read(readBuf, 0, readBuf.size)
                    if (read > 0) {
                        val processed = ShortArray(read)
                        var sumSq = 0.0
                        for (i in 0 until read) {
                            var sample = readBuf[i].toDouble()
                            if (noiseReduction) {
                                val hp = 0.985 * (hpPrevOut + sample - hpPrevIn)
                                hpPrevIn = sample
                                hpPrevOut = hp
                                sample = if (abs(hp) < 420.0) hp * 0.12 else hp
                            }
                            val clamped = sample.roundToInt().coerceIn(-32767, 32767).toShort()
                            processed[i] = clamped
                            sumSq += clamped.toDouble() * clamped.toDouble()
                        }
                        chunks.add(processed)
                        val rms = sqrt(sumSq / read) / 14000.0
                        val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt()
                        onLevel(rms.toFloat().coerceIn(0.05f, 1f), elapsedSec)
                    }
                }
                recorder.stop()
            }
        } catch (_: Exception) {
            // Cleanly close when mic hardware unavailable
        } finally {
            runCatching { recorder.release() }
        }

        val totalSamples = chunks.sumOf { it.size }
        val merged = ShortArray(totalSamples)
        var pos = 0
        for (c in chunks) {
            c.copyInto(merged, pos)
            pos += c.size
        }
        val finalPcm = if (merged.isEmpty()) generateStarterBeatPcm(2.5, isVoiceDemo = true) else merged
        writePcmWavFile(outputFile, finalPcm)
        val durMs = ((finalPcm.size.toDouble() / SAMPLE_RATE) * 1000.0).toLong()
        finalPcm to durMs
    }

    suspend fun extractAudioFromMediaUri(
        context: Context,
        uri: Uri,
        outputFile: File,
        maxSeconds: Int = 25
    ): Pair<ShortArray, Long>? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIdx = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIdx < 0 || audioFormat == null) return@withContext null

            extractor.selectTrack(audioTrackIdx)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            val channels = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            val srcRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val maxRawSamples = srcRate * maxSeconds
            val monoSamples = ArrayList<Short>(min(maxRawSamples, 44100 * 10))
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone && monoSamples.size < maxRawSamples) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)
                        val sampleSize = if (buf != null) extractor.readSampleData(buf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000L)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val frameCount = shortBuf.remaining() / channels
                        for (f in 0 until frameCount) {
                            var sum = 0
                            for (c in 0 until channels) sum += shortBuf.get().toInt()
                            monoSamples.add((sum / channels).toShort())
                            if (monoSamples.size >= maxRawSamples) break
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                }
            }

            if (monoSamples.isEmpty()) return@withContext null

            val targetLen = ((monoSamples.size.toDouble() * SAMPLE_RATE) / srcRate).toInt().coerceAtLeast(1)
            val resampled = ShortArray(targetLen) { i ->
                val srcPos = (i.toDouble() * srcRate) / SAMPLE_RATE
                val idx0 = srcPos.toInt().coerceIn(0, monoSamples.lastIndex)
                val idx1 = (idx0 + 1).coerceIn(0, monoSamples.lastIndex)
                val frac = srcPos - idx0
                ((monoSamples[idx0] * (1.0 - frac)) + (monoSamples[idx1] * frac)).roundToInt().toShort()
            }
            writePcmWavFile(outputFile, resampled)
            val durMs = ((resampled.size.toDouble() / SAMPLE_RATE) * 1000.0).toLong()
            resampled to durMs
        } catch (_: Exception) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /**
     * Applies Trim, Pitch & Tempo Speed, Voice Changer, Vocal Clarity, EQ/Echo/Reverb, Fade-In/Out, and Volume.
     */
    fun processTrackSignal(raw: ShortArray, track: AudioTrackState): FloatArray {
        if (raw.isEmpty()) return FloatArray(0)
        val sIdx = (raw.size * track.trimStartPct.coerceIn(0f, 0.95f)).toInt()
        val eIdx = (raw.size * track.trimEndPct.coerceIn(track.trimStartPct + 0.05f, 1f)).toInt()
            .coerceIn(sIdx + 1, raw.size)
        val sliceLen = (eIdx - sIdx).coerceAtLeast(1)

        // Effective playback rate combines pitch semitones & tempo speed
        val pitchFactor = 2.0.pow(track.pitchSemitones.coerceIn(-8f, 8f) / 12.0)
        val rate = (pitchFactor * track.tempoSpeed.coerceIn(0.5f, 2.0f)).coerceIn(0.45, 2.6)
        val outLen = ((sliceLen / rate).roundToInt()).coerceIn(64, SAMPLE_RATE * 35)

        val buf = FloatArray(outLen) { i ->
            val srcF = sIdx + i * rate
            val i0 = srcF.toInt().coerceIn(sIdx, eIdx - 1)
            val i1 = (i0 + 1).coerceIn(sIdx, eIdx - 1)
            val frac = (srcF - i0).toFloat()
            val sample = raw[i0] * (1f - frac) + raw[i1] * frac
            sample / 32767f
        }

        // Voice Changer special FX (Robot ring mod / Radio bandpass)
        when (track.voicePreset) {
            VoiceChangerPreset.ROBOT -> {
                for (i in buf.indices) {
                    val carrier = sin(2.0 * PI * 58.0 * i / SAMPLE_RATE).toFloat()
                    buf[i] = buf[i] * (0.4f + 0.6f * carrier)
                }
            }
            VoiceChangerPreset.RADIO -> {
                var lp = 0f
                var hp = 0f
                var prevIn = 0f
                for (i in buf.indices) {
                    val x = buf[i]
                    hp = 0.92f * (hp + x - prevIn)
                    prevIn = x
                    lp += 0.25f * (hp - lp)
                    buf[i] = tanh((lp * 1.9).toDouble()).toFloat()
                }
            }
            else -> Unit
        }

        // Vocal Clarity Enhancer (high-pass rumble cut + crisp presence lift)
        if (track.vocalClarityEnabled || track.eqPreset == AudioEqPreset.VOCAL_POP) {
            var hp = 0f
            var prevIn = 0f
            var lpMid = 0f
            for (i in buf.indices) {
                val x = buf[i]
                hp = 0.975f * (hp + x - prevIn)
                prevIn = x
                lpMid += 0.14f * (hp - lpMid)
                val presence = hp - lpMid
                buf[i] = (hp + presence * 0.45f).coerceIn(-1.6f, 1.6f)
            }
        }

        // EQ, Echo & Hall Reverb Presets
        when (track.eqPreset) {
            AudioEqPreset.FLAT, AudioEqPreset.VOCAL_POP -> Unit
            AudioEqPreset.BASS_BOOST -> {
                var low = 0f
                for (i in buf.indices) {
                    low += 0.045f * (buf[i] - low)
                    buf[i] = (buf[i] + low * 0.85f).coerceIn(-1.8f, 1.8f)
                }
            }
            AudioEqPreset.ECHO -> {
                val delaySamples = (SAMPLE_RATE * 0.24).toInt()
                val copy = buf.copyOf()
                for (i in delaySamples until buf.size) {
                    buf[i] = (buf[i] + copy[i - delaySamples] * 0.42f +
                        if (i >= delaySamples * 2) copy[i - delaySamples * 2] * 0.20f else 0f)
                }
            }
            AudioEqPreset.HALL_REVERB -> {
                val taps = intArrayOf(1367, 2333, 3821, 5779)
                val gains = floatArrayOf(0.28f, 0.22f, 0.16f, 0.12f)
                val copy = buf.copyOf()
                for (i in buf.indices) {
                    var wet = 0f
                    for (t in taps.indices) {
                        val idx = i - taps[t]
                        if (idx >= 0) wet += copy[idx] * gains[t]
                    }
                    buf[i] = buf[i] * 0.82f + wet
                }
            }
        }

        // Volume (0%..200%) + Fade-In / Fade-Out
        val fadeInSamples = (track.fadeInSec * SAMPLE_RATE).toInt().coerceIn(0, buf.size / 2)
        val fadeOutSamples = (track.fadeOutSec * SAMPLE_RATE).toInt().coerceIn(0, buf.size / 2)
        val vol = track.volume.coerceIn(0f, 2f)
        for (i in buf.indices) {
            var gain = vol
            if (fadeInSamples > 0 && i < fadeInSamples) gain *= (i.toFloat() / fadeInSamples)
            if (fadeOutSamples > 0 && i >= buf.size - fadeOutSamples) gain *= ((buf.size - i).toFloat() / fadeOutSamples)
            buf[i] *= gain
        }
        return buf
    }

    /**
     * Mixes all 3 Tracks with Voice Changer, Pitch/Tempo, EQ/Echo/Reverb, Vocal Clarity, 0-200% Boost, Mute, Trim, Fade-In/Out.
     */
    suspend fun mixThreeTracks(tracks: List<AudioTrackState>): ShortArray = withContext(Dispatchers.Default) {
        val processed = tracks.mapNotNull { track ->
            if (track.muted || track.volume <= 0.005f || track.filePath.isNullOrBlank()) return@mapNotNull null
            val raw = readWavPcm(File(track.filePath)) ?: return@mapNotNull null
            if (raw.isEmpty()) return@mapNotNull null
            processTrackSignal(raw, track)
        }

        if (processed.isEmpty()) return@withContext ShortArray(0)
        val maxLen = processed.maxOf { it.size }
        ShortArray(maxLen) { i ->
            var sum = 0f
            for (buf in processed) {
                if (i < buf.size) sum += buf[i]
            }
            (tanh(sum.toDouble()) * 32000.0).roundToInt().coerceIn(-32767, 32767).toShort()
        }
    }
}
