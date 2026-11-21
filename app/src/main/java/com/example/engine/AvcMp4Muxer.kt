package com.example.engine

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * Pure-Kotlin ISO BMFF (ISO/IEC 14496-12 + 14496-15) H.264 (.mp4) Muxer.
 *
 * Replaces platform android.media.MediaMuxer / native MPEG4Writer so video export
 * never hits AOSP MPEG4Writer errors ("Already have codec specific data" or
 * "Stop() called but track is not started or stopped") across Android 7–16 devices
 * and emulators, while producing standard avc1/avcC MP4 files playable everywhere.
 */
class AvcMp4Muxer(
    private val outputFile: File,
    private val width: Int,
    private val height: Int,
    private val fps: Int
) {
    private data class SampleRecord(
        val fileOffset: Long,
        val sizeBytes: Int,
        val isSyncFrame: Boolean
    )

    private val raf = RandomAccessFile(outputFile, "rw")
    private val samples = ArrayList<SampleRecord>(128)
    private var spsNal: ByteArray? = null
    private var ppsNal: ByteArray? = null
    private var mdatHeaderOffset: Long = 0L
    private var currentWriteOffset: Long = 0L
    private var closed = false

    init {
        raf.setLength(0L)
        // Write 24-byte 'ftyp' box: isom / iso2 / avc1 / mp41
        val ftyp = buildBox("ftyp") { out ->
            out.writeBytes("isom")
            out.writeInt(0x00000200)
            out.writeBytes("isom")
            out.writeBytes("iso2")
            out.writeBytes("avc1")
            out.writeBytes("mp41")
        }
        raf.write(ftyp)
        mdatHeaderOffset = ftyp.size.toLong()
        // Reserve 8-byte 'mdat' header (32-bit size + "mdat")
        raf.writeInt(8)
        raf.writeBytes("mdat")
        currentWriteOffset = mdatHeaderOffset + 8L
    }

    /**
     * Registers codec-specific data buffers (e.g., MediaFormat csd-0 / csd-1 or BUFFER_FLAG_CODEC_CONFIG)
     * in either Annex-B format or raw single-NAL format.
     */
    fun addCodecConfig(buffer: ByteBuffer?) {
        if (buffer == null) return
        val dup = buffer.duplicate()
        val bytes = ByteArray(dup.remaining())
        dup.get(bytes)
        addCodecConfigBytes(bytes)
    }

    fun addCodecConfigBytes(raw: ByteArray) {
        if (raw.isEmpty()) return
        val nals = splitAnnexBNals(raw)
        if (nals.isNotEmpty()) {
            for (nal in nals) {
                captureSpsOrPps(nal)
            }
        } else {
            captureSpsOrPps(raw)
        }
    }

    /**
     * Appends one encoded H.264 access unit (Annex-B bitstream) into the mdat box.
     * Automatically extracts any inline SPS/PPS NAL units for the avcC header and
     * writes only VCL video frame slices (NAL types 1..5) with 4-byte AVCC length prefixes.
     */
    fun writeAccessUnit(annexBData: ByteArray, isKeyFrameFlag: Boolean): Boolean {
        if (closed || annexBData.isEmpty()) return false
        val nals = splitAnnexBNals(annexBData)
        val candidateNals = if (nals.isNotEmpty()) nals else listOf(annexBData)

        val vclNals = ArrayList<ByteArray>(2)
        var hasIdr = false
        for (nal in candidateNals) {
            if (nal.isEmpty()) continue
            val nalType = nal[0].toInt() and 0x1F
            when (nalType) {
                7 -> if (spsNal == null) spsNal = nal
                8 -> if (ppsNal == null) ppsNal = nal
                5 -> {
                    hasIdr = true
                    vclNals.add(nal)
                }
                1, 2, 3, 4 -> vclNals.add(nal)
                // Ignore AUD (9), SEI (6), filler (12), etc.
            }
        }

        if (vclNals.isEmpty()) return false

        // Wait until first keyframe if none written yet so players decode cleanly from frame 0
        if (samples.isEmpty() && !hasIdr && !isKeyFrameFlag) {
            return false
        }

        val sampleStart = currentWriteOffset
        var sampleBytes = 0
        for (nal in vclNals) {
            val len = nal.size
            val lenHeader = byteArrayOf(
                ((len ushr 24) and 0xFF).toByte(),
                ((len ushr 16) and 0xFF).toByte(),
                ((len ushr 8) and 0xFF).toByte(),
                (len and 0xFF).toByte()
            )
            raf.write(lenHeader)
            raf.write(nal)
            sampleBytes += 4 + len
        }
        currentWriteOffset += sampleBytes.toLong()
        samples.add(
            SampleRecord(
                fileOffset = sampleStart,
                sizeBytes = sampleBytes,
                isSyncFrame = hasIdr || isKeyFrameFlag
            )
        )
        return true
    }

    val writtenFrameCount: Int
        get() = samples.size

    /**
     * Patches the mdat box size, appends the complete 'moov' atom, and closes the file.
     * Returns true if a valid, playable MP4 was finalized.
     */
    fun finish(): Boolean {
        if (closed) return samples.isNotEmpty()
        closed = true
        return try {
            val sps = spsNal
            val pps = ppsNal
            if (samples.isEmpty() || sps == null || pps == null) {
                raf.close()
                return false
            }

            // Patch mdat size
            val mdatSize = currentWriteOffset - mdatHeaderOffset
            if (mdatSize > Int.MAX_VALUE.toLong()) {
                raf.close()
                return false
            }
            raf.seek(mdatHeaderOffset)
            raf.writeInt(mdatSize.toInt())

            // Append moov box at end of mdat
            raf.seek(currentWriteOffset)
            val moov = buildMoovBox(sps, pps)
            raf.write(moov)
            raf.fd.sync()
            raf.close()
            true
        } catch (_: Exception) {
            runCatching { raf.close() }
            false
        }
    }

    fun abort() {
        if (!closed) {
            closed = true
            runCatching { raf.close() }
        }
    }

    private fun captureSpsOrPps(nal: ByteArray) {
        if (nal.isEmpty()) return
        when (nal[0].toInt() and 0x1F) {
            7 -> if (spsNal == null) spsNal = nal
            8 -> if (ppsNal == null) ppsNal = nal
        }
    }

    private fun buildMoovBox(sps: ByteArray, pps: ByteArray): ByteArray {
        val safeFps = fps.coerceIn(1, 120)
        val timescale = 90_000
        val sampleDelta = (timescale / safeFps).coerceAtLeast(1)
        val totalDuration = (samples.size.toLong() * sampleDelta.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val mvhd = buildFullBox("mvhd", version = 0, flags = 0) { out ->
            out.writeInt(0) // creation_time
            out.writeInt(0) // modification_time
            out.writeInt(timescale)
            out.writeInt(totalDuration)
            out.writeInt(0x00010000) // rate = 1.0
            out.writeShort(0x0100) // volume = 1.0
            out.writeShort(0) // reserved
            out.writeInt(0)
            out.writeInt(0)
            writeUnityMatrix(out)
            repeat(6) { out.writeInt(0) } // pre_defined
            out.writeInt(2) // next_track_ID
        }

        val tkhd = buildFullBox("tkhd", version = 0, flags = 0x000003) { out ->
            out.writeInt(0) // creation_time
            out.writeInt(0) // modification_time
            out.writeInt(1) // track_ID
            out.writeInt(0) // reserved
            out.writeInt(totalDuration)
            out.writeInt(0)
            out.writeInt(0)
            out.writeShort(0) // layer
            out.writeShort(0) // alternate_group
            out.writeShort(0) // volume (video = 0)
            out.writeShort(0) // reserved
            writeUnityMatrix(out)
            out.writeInt(width shl 16) // 16.16 fixed-point width
            out.writeInt(height shl 16) // 16.16 fixed-point height
        }

        val mdhd = buildFullBox("mdhd", version = 0, flags = 0) { out ->
            out.writeInt(0) // creation_time
            out.writeInt(0) // modification_time
            out.writeInt(timescale)
            out.writeInt(totalDuration)
            out.writeShort(0x55C4) // ISO-639-2/T "und"
            out.writeShort(0) // pre_defined
        }

        val hdlr = buildFullBox("hdlr", version = 0, flags = 0) { out ->
            out.writeInt(0) // pre_defined
            out.writeBytes("vide")
            out.writeInt(0)
            out.writeInt(0)
            out.writeInt(0)
            out.writeBytes("VideoHandler")
            out.writeByte(0)
        }

        val vmhd = buildFullBox("vmhd", version = 0, flags = 1) { out ->
            out.writeShort(0) // graphicsmode
            out.writeShort(0) // opcolor R
            out.writeShort(0) // opcolor G
            out.writeShort(0) // opcolor B
        }

        val urlBox = buildFullBox("url ", version = 0, flags = 1) { }
        val dref = buildFullBox("dref", version = 0, flags = 0) { out ->
            out.writeInt(1)
            out.write(urlBox)
        }
        val dinf = buildBox("dinf") { it.write(dref) }

        val avcC = buildBox("avcC") { out ->
            val profileIdc = if (sps.size > 1) sps[1].toInt() and 0xFF else 0x42
            val profileCompat = if (sps.size > 2) sps[2].toInt() and 0xFF else 0xC0
            val levelIdc = if (sps.size > 3) sps[3].toInt() and 0xFF else 0x1F
            out.writeByte(1) // configurationVersion
            out.writeByte(profileIdc)
            out.writeByte(profileCompat)
            out.writeByte(levelIdc)
            out.writeByte(0xFF) // 6 bits reserved (1) + 2 bits lengthSizeMinusOne = 3 (4-byte NAL lengths)
            out.writeByte(0xE1) // 3 bits reserved (1) + 5 bits numOfSequenceParameterSets = 1
            out.writeShort(sps.size)
            out.write(sps)
            out.writeByte(1) // numOfPictureParameterSets = 1
            out.writeShort(pps.size)
            out.write(pps)
        }

        val avc1 = buildBox("avc1") { out ->
            repeat(6) { out.writeByte(0) } // reserved
            out.writeShort(1) // data_reference_index
            out.writeShort(0) // pre_defined
            out.writeShort(0) // reserved
            out.writeInt(0) // pre_defined[0]
            out.writeInt(0) // pre_defined[1]
            out.writeInt(0) // pre_defined[2]
            out.writeShort(width)
            out.writeShort(height)
            out.writeInt(0x00480000) // horizresolution 72 dpi
            out.writeInt(0x00480000) // vertresolution 72 dpi
            out.writeInt(0) // reserved
            out.writeShort(1) // frame_count
            repeat(32) { out.writeByte(0) } // compressorname
            out.writeShort(0x0018) // depth = 24
            out.writeShort(0xFFFF) // pre_defined = -1
            out.write(avcC)
        }

        val stsd = buildFullBox("stsd", version = 0, flags = 0) { out ->
            out.writeInt(1)
            out.write(avc1)
        }

        val stts = buildFullBox("stts", version = 0, flags = 0) { out ->
            out.writeInt(1)
            out.writeInt(samples.size)
            out.writeInt(sampleDelta)
        }

        val syncIndices = samples.mapIndexedNotNull { idx, rec -> if (rec.isSyncFrame) idx + 1 else null }
        val stss = if (syncIndices.isNotEmpty()) {
            buildFullBox("stss", version = 0, flags = 0) { out ->
                out.writeInt(syncIndices.size)
                for (syncIdx in syncIndices) {
                    out.writeInt(syncIdx)
                }
            }
        } else {
            ByteArray(0)
        }

        val stsc = buildFullBox("stsc", version = 0, flags = 0) { out ->
            out.writeInt(1)
            out.writeInt(1) // first_chunk
            out.writeInt(1) // samples_per_chunk
            out.writeInt(1) // sample_description_index
        }

        val stsz = buildFullBox("stsz", version = 0, flags = 0) { out ->
            out.writeInt(0) // variable sample size
            out.writeInt(samples.size)
            for (rec in samples) {
                out.writeInt(rec.sizeBytes)
            }
        }

        val needsCo64 = samples.any { it.fileOffset > Int.MAX_VALUE.toLong() }
        val chunkOffsetBox = if (needsCo64) {
            buildFullBox("co64", version = 0, flags = 0) { out ->
                out.writeInt(samples.size)
                for (rec in samples) {
                    out.writeLong(rec.fileOffset)
                }
            }
        } else {
            buildFullBox("stco", version = 0, flags = 0) { out ->
                out.writeInt(samples.size)
                for (rec in samples) {
                    out.writeInt(rec.fileOffset.toInt())
                }
            }
        }

        val stbl = buildBox("stbl") { out ->
            out.write(stsd)
            out.write(stts)
            if (stss.isNotEmpty()) out.write(stss)
            out.write(stsc)
            out.write(stsz)
            out.write(chunkOffsetBox)
        }

        val minf = buildBox("minf") { out ->
            out.write(vmhd)
            out.write(dinf)
            out.write(stbl)
        }

        val mdia = buildBox("mdia") { out ->
            out.write(mdhd)
            out.write(hdlr)
            out.write(minf)
        }

        val trak = buildBox("trak") { out ->
            out.write(tkhd)
            out.write(mdia)
        }

        return buildBox("moov") { out ->
            out.write(mvhd)
            out.write(trak)
        }
    }

    private fun writeUnityMatrix(out: DataOutputStream) {
        out.writeInt(0x00010000)
        out.writeInt(0)
        out.writeInt(0)
        out.writeInt(0)
        out.writeInt(0x00010000)
        out.writeInt(0)
        out.writeInt(0)
        out.writeInt(0)
        out.writeInt(0x40000000)
    }

    private fun buildBox(type: String, bodyWriter: (DataOutputStream) -> Unit): ByteArray {
        val bodyBaos = ByteArrayOutputStream()
        DataOutputStream(bodyBaos).use { bodyWriter(it) }
        val body = bodyBaos.toByteArray()
        val boxBaos = ByteArrayOutputStream(8 + body.size)
        DataOutputStream(boxBaos).use { out ->
            out.writeInt(8 + body.size)
            out.writeBytes(type)
            out.write(body)
        }
        return boxBaos.toByteArray()
    }

    private fun buildFullBox(
        type: String,
        version: Int,
        flags: Int,
        bodyWriter: (DataOutputStream) -> Unit
    ): ByteArray {
        return buildBox(type) { out ->
            out.writeInt(((version and 0xFF) shl 24) or (flags and 0x00FFFFFF))
            bodyWriter(out)
        }
    }

    companion object {
        /**
         * Splits an Annex-B byte array (with 3-byte 00 00 01 or 4-byte 00 00 00 01 start codes)
         * into individual raw NAL unit payloads without start codes.
         */
        fun splitAnnexBNals(data: ByteArray): List<ByteArray> {
            val starts = ArrayList<Pair<Int, Int>>(4) // (startCodeBegin, payloadBegin)
            var i = 0
            val n = data.size
            while (i + 2 < n) {
                if (data[i].toInt() == 0 && data[i + 1].toInt() == 0) {
                    if (data[i + 2].toInt() == 1) {
                        starts.add(i to (i + 3))
                        i += 3
                        continue
                    } else if (i + 3 < n && data[i + 2].toInt() == 0 && data[i + 3].toInt() == 1) {
                        starts.add(i to (i + 4))
                        i += 4
                        continue
                    }
                }
                i++
            }
            if (starts.isEmpty()) return emptyList()

            val result = ArrayList<ByteArray>(starts.size)
            for (idx in starts.indices) {
                val payloadStart = starts[idx].second
                val payloadEnd = if (idx + 1 < starts.size) starts[idx + 1].first else n
                if (payloadEnd > payloadStart) {
                    result.add(data.copyOfRange(payloadStart, payloadEnd))
                }
            }
            return result
        }
    }
}
