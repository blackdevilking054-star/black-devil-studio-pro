package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportResult(
    val success: Boolean,
    val displayPath: String,
    val byteCount: Long = 0L,
    val error: String? = null
)

object MediaExportStore {

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun makeFileName(prefix: String, ext: String): String =
        "BlackDevil_${prefix}_${timestamp()}.$ext"

    fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }

    suspend fun saveImageBytes(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): ExportResult = withContext(Dispatchers.IO) {
        saveToMediaStore(
            context = context,
            fileName = fileName,
            mimeType = mimeType,
            relativeDir = "${Environment.DIRECTORY_PICTURES}/BlackDevilStudioPro",
            collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else null,
            fallbackDirType = Environment.DIRECTORY_PICTURES
        ) { out ->
            out.write(bytes)
        }.copy(byteCount = bytes.size.toLong())
    }

    suspend fun saveAudioBytes(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): ExportResult = withContext(Dispatchers.IO) {
        saveToMediaStore(
            context = context,
            fileName = fileName,
            mimeType = mimeType,
            relativeDir = "${Environment.DIRECTORY_MUSIC}/BlackDevilStudioPro",
            collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else null,
            fallbackDirType = Environment.DIRECTORY_MUSIC
        ) { out ->
            out.write(bytes)
        }.copy(byteCount = bytes.size.toLong())
    }

    suspend fun saveVideoFile(
        context: Context,
        sourceFile: File,
        fileName: String
    ): ExportResult = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return@withContext ExportResult(false, "", 0L, "Rendered video buffer was empty")
        }
        val size = sourceFile.length()
        saveToMediaStore(
            context = context,
            fileName = fileName,
            mimeType = "video/mp4",
            relativeDir = "${Environment.DIRECTORY_MOVIES}/BlackDevilStudioPro",
            collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else null,
            fallbackDirType = Environment.DIRECTORY_MOVIES
        ) { out ->
            sourceFile.inputStream().use { input ->
                input.copyTo(out)
            }
        }.copy(byteCount = size)
    }

    private fun saveToMediaStore(
        context: Context,
        fileName: String,
        mimeType: String,
        relativeDir: String,
        collectionUri: Uri?,
        fallbackDirType: String,
        writer: (OutputStream) -> Unit
    ): ExportResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && collectionUri != null) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(collectionUri, values)
                    ?: return ExportResult(false, "", 0L, "Unable to create MediaStore entry")
                resolver.openOutputStream(itemUri)?.use(writer)
                    ?: return ExportResult(false, "", 0L, "Unable to open output stream")
                val done = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(itemUri, done, null, null)
                ExportResult(true, "$relativeDir/$fileName")
            } else {
                val baseDir = File(context.getExternalFilesDir(fallbackDirType), "BlackDevilStudioPro")
                if (!baseDir.exists()) baseDir.mkdirs()
                val outFile = File(baseDir, fileName)
                FileOutputStream(outFile).use(writer)
                ExportResult(true, outFile.absolutePath)
            }
        } catch (e: Exception) {
            ExportResult(false, "", 0L, e.message ?: "Export failed")
        }
    }
}
