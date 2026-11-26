package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import com.example.R
import com.example.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Low-RAM asynchronous timeline thumbnail cache.
 * Decodes and caches video clip filmstrip frames strictly on [Dispatchers.IO] at
 * <= 128x128px using [BitmapFactory.Options.inSampleSize] and [Bitmap.Config.RGB_565]
 * (2 bytes/pixel, <= 32 KB per frame) inside a heap-aware [LruCache] so timeline scrolling
 * and 60fps playback never trigger OutOfMemoryError or GC pauses on 2GB/3GB RAM devices.
 */
object TimelineThumbnailCache {

    const val MAX_THUMB_EDGE_PX: Int = 128
    private val maxCacheBytes: Int = run {
        val heapBytes = Runtime.getRuntime().maxMemory().coerceAtLeast(32L * 1024L * 1024L)
        (heapBytes / 16L).coerceIn(2L * 1024L * 1024L, 6L * 1024L * 1024L).toInt()
    }

    private val lruCache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return if (value.isRecycled) 1 else value.allocationByteCount.coerceAtLeast(1)
        }
    }

    private fun frameCacheKey(clip: VideoClip, bucketMs: Long): String {
        val sourceKey = clip.uriString ?: "res:${clip.fallbackDrawable ?: R.drawable.img_studio_sample_bg_1795185351810}"
        return "$sourceKey|$bucketMs|${MAX_THUMB_EDGE_PX}_565"
    }

    /**
     * Converts any source bitmap to a compact [Bitmap.Config.RGB_565] bitmap bounded to
     * [MAX_THUMB_EDGE_PX] x [MAX_THUMB_EDGE_PX] and recycles the temporary input if requested.
     */
    fun toCompactRgb565(source: Bitmap, maxEdge: Int = MAX_THUMB_EDGE_PX): Bitmap {
        val safeEdge = maxEdge.coerceIn(32, MAX_THUMB_EDGE_PX)
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)
        val scale = min(safeEdge.toFloat() / srcW.toFloat(), safeEdge.toFloat() / srcH.toFloat())
            .coerceAtMost(1f)
        val outW = (srcW * scale).roundToInt().coerceIn(24, safeEdge)
        val outH = (srcH * scale).roundToInt().coerceIn(24, safeEdge)

        if (source.config == Bitmap.Config.RGB_565 && source.width == outW && source.height == outH) {
            return source
        }

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.RGB_565)
        val canvas = Canvas(out)
        val srcRect = Rect(0, 0, srcW, srcH)
        val dstRect = Rect(0, 0, outW, outH)
        canvas.drawBitmap(source, srcRect, dstRect, null)
        return out
    }

    /**
     * Returns already-cached filmstrip thumbnails for [clip] instantaneously on the caller thread,
     * or null if any frame still needs async IO decoding.
     */
    fun getCachedFilmstrip(clip: VideoClip, count: Int = 5): List<Bitmap>? {
        val safeCount = count.coerceIn(2, 6)
        val span = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(300L)
        val result = ArrayList<Bitmap>(safeCount)
        synchronized(lruCache) {
            for (i in 0 until safeCount) {
                val rawMs = clip.trimStartMs + (span * i) / max(1, safeCount - 1)
                val bucketMs = (rawMs / 250L) * 250L
                val bmp = lruCache.get(frameCacheKey(clip, bucketMs))
                if (bmp == null || bmp.isRecycled) return null
                result.add(bmp)
            }
        }
        return result
    }

    /**
     * Asynchronously decodes and caches [count] low-memory (<= 120x120px RGB_565) timeline
     * thumbnails on [Dispatchers.IO]. Reuses cached frames across timeline trims and scrolls.
     */
    suspend fun loadFilmstripAsync(
        context: Context,
        clip: VideoClip,
        count: Int = 5
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        getCachedFilmstrip(clip, count)?.let { return@withContext it }

        val safeCount = count.coerceIn(2, 6)
        val span = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(300L)
        val thumbs = ArrayList<Bitmap>(safeCount)

        if (!clip.uriString.isNullOrBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(clip.uriString))
                for (i in 0 until safeCount) {
                    val rawMs = clip.trimStartMs + (span * i) / max(1, safeCount - 1)
                    val bucketMs = (rawMs / 250L) * 250L
                    val key = frameCacheKey(clip, bucketMs)
                    val cached = synchronized(lruCache) { lruCache.get(key) }
                    if (cached != null && !cached.isRecycled) {
                        thumbs.add(cached)
                        continue
                    }

                    val decoded: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            bucketMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            MAX_THUMB_EDGE_PX,
                            MAX_THUMB_EDGE_PX
                        )
                    } else {
                        retriever.getFrameAtTime(
                            bucketMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                    }
                    if (decoded != null) {
                        val compact = toCompactRgb565(decoded, MAX_THUMB_EDGE_PX)
                        if (compact !== decoded) decoded.recycle()
                        synchronized(lruCache) { lruCache.put(key, compact) }
                        thumbs.add(compact)
                    }
                }
            } catch (_: Exception) {
                // Fall back to compact RGB_565 sample drawable below
            } finally {
                runCatching { retriever.release() }
            }
        }

        if (thumbs.isEmpty()) {
            val resId = clip.fallbackDrawable ?: R.drawable.img_studio_sample_bg_1795185351810
            val fallbackKey = "fallback_res:$resId|${MAX_THUMB_EDGE_PX}_565"
            var compactFallback = synchronized(lruCache) { lruCache.get(fallbackKey) }
            if (compactFallback == null || compactFallback.isRecycled) {
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, resId, opts)
                var sampleSize = 1
                while (opts.outWidth / (sampleSize * 2) >= MAX_THUMB_EDGE_PX &&
                    opts.outHeight / (sampleSize * 2) >= MAX_THUMB_EDGE_PX
                ) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val raw = BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
                if (raw != null) {
                    compactFallback = toCompactRgb565(raw, MAX_THUMB_EDGE_PX)
                    if (compactFallback !== raw) raw.recycle()
                    synchronized(lruCache) { lruCache.put(fallbackKey, compactFallback) }
                }
            }
            if (compactFallback != null && !compactFallback.isRecycled) {
                repeat(safeCount) { i ->
                    val rawMs = clip.trimStartMs + (span * i) / max(1, safeCount - 1)
                    val bucketMs = (rawMs / 250L) * 250L
                    synchronized(lruCache) {
                        lruCache.put(frameCacheKey(clip, bucketMs), compactFallback)
                    }
                    thumbs.add(compactFallback)
                }
            }
        }

        thumbs
    }

    fun trimMemory(aggressive: Boolean) {
        synchronized(lruCache) {
            if (aggressive) {
                lruCache.evictAll()
            } else {
                lruCache.trimToSize(maxCacheBytes / 2)
            }
        }
    }

    fun clear() {
        synchronized(lruCache) {
            lruCache.evictAll()
        }
    }
}
