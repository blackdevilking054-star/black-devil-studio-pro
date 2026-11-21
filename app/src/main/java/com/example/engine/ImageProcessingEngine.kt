package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.LruCache
import com.example.model.BlendModeKind
import com.example.model.CollageLayoutKind
import com.example.model.EraserStrokePoint
import com.example.model.ImageConvertFormat
import com.example.model.LayerKind
import com.example.model.LightroomGrade
import com.example.model.LutPreset
import com.example.model.MaskShapeKind
import com.example.model.PhotoExportFormat
import com.example.model.PhotoLayer
import com.example.model.PhotoStudioState
import com.example.model.SpotHealPoint
import com.example.model.StudioShapeKind
import com.example.model.ToneCurvePreset
import com.example.model.VideoBgFillMode
import com.example.model.VideoColorGrade
import com.example.ui.theme.StudioFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object ImageProcessingEngine {

    // Bounded 16MB LRU cache of decoded source bitmaps for smooth 2GB/3GB RAM operation
    private val sourceBitmapCache = object : LruCache<String, Bitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun decodeSampledBitmap(
        context: Context,
        uriString: String?,
        fallbackDrawable: Int?,
        maxDimension: Int
    ): Bitmap? {
        val cacheKey = "${uriString ?: "res_$fallbackDrawable"}_$maxDimension"
        sourceBitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val decoded = try {
            if (!uriString.isNullOrBlank()) {
                val uri = Uri.parse(uriString)
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                val sampleSize = computeInSampleSize(opts.outWidth, opts.outHeight, maxDimension)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            } else if (fallbackDrawable != null) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(context.resources, fallbackDrawable, opts)
                val sampleSize = computeInSampleSize(opts.outWidth, opts.outHeight, maxDimension)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeResource(context.resources, fallbackDrawable, decodeOpts)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
        if (decoded != null && !decoded.isRecycled) {
            sourceBitmapCache.put(cacheKey, decoded)
        }
        return decoded
    }

    private fun computeInSampleSize(rawW: Int, rawH: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val maxRaw = max(rawW, rawH)
        while (maxRaw / (inSampleSize * 2) >= maxDim) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    fun toPorterDuffMode(mode: BlendModeKind): PorterDuff.Mode? = when (mode) {
        BlendModeKind.NORMAL -> null
        BlendModeKind.SCREEN -> PorterDuff.Mode.SCREEN
        BlendModeKind.MULTIPLY -> PorterDuff.Mode.MULTIPLY
        BlendModeKind.OVERLAY -> PorterDuff.Mode.OVERLAY
        BlendModeKind.LIGHTEN -> PorterDuff.Mode.LIGHTEN
    }

    fun buildMaskPath(shape: MaskShapeKind, rect: RectF): Path? {
        if (shape == MaskShapeKind.NONE) return null
        val path = Path()
        val cx = rect.centerX()
        val cy = rect.centerY()
        val rx = rect.width() / 2f
        val ry = rect.height() / 2f
        val rad = min(rx, ry)
        when (shape) {
            MaskShapeKind.NONE -> return null
            MaskShapeKind.CIRCLE -> path.addCircle(cx, cy, rad, Path.Direction.CW)
            MaskShapeKind.RECTANGLE -> path.addRoundRect(rect, rad * 0.25f, rad * 0.25f, Path.Direction.CW)
            MaskShapeKind.LINEAR -> path.addRect(rect.left, rect.top, cx + rx * 0.35f, rect.bottom, Path.Direction.CW)
            MaskShapeKind.STAR -> {
                val outerR = rad * 0.96f
                val innerR = outerR * 0.44f
                for (i in 0 until 10) {
                    val angle = -PI / 2.0 + i * PI / 5.0
                    val r = if (i % 2 == 0) outerR else innerR
                    val px = cx + (cos(angle) * r).toFloat()
                    val py = cy + (sin(angle) * r).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            MaskShapeKind.HEART -> {
                val w = rad * 1.9f
                val h = rad * 1.85f
                val top = cy - h * 0.42f
                path.moveTo(cx, top + h * 0.25f)
                path.cubicTo(cx - w * 0.55f, top - h * 0.22f, cx - w * 0.62f, top + h * 0.48f, cx, top + h * 0.92f)
                path.cubicTo(cx + w * 0.62f, top + h * 0.48f, cx + w * 0.55f, top - h * 0.22f, cx, top + h * 0.25f)
                path.close()
            }
        }
        return path
    }

    fun fastBoxBlur(source: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(0, 28)
        if (r <= 0) return source.copy(Bitmap.Config.ARGB_8888, true)

        val scaleDown = if (r > 10 && max(source.width, source.height) > 480) 2 else 1
        val work = if (scaleDown > 1) {
            Bitmap.createScaledBitmap(
                source,
                (source.width / scaleDown).coerceAtLeast(2),
                (source.height / scaleDown).coerceAtLeast(2),
                true
            )
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = work.width
        val h = work.height
        val pix = IntArray(w * h)
        work.getPixels(pix, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        val effR = (r / scaleDown).coerceAtLeast(1)
        val div = effR * 2 + 1

        for (y in 0 until h) {
            var rSum = 0; var gSum = 0; var bSum = 0; var aSum = 0
            val rowOff = y * w
            for (i in -effR..effR) {
                val p = pix[rowOff + i.coerceIn(0, w - 1)]
                aSum += (p ushr 24) and 0xFF
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (x in 0 until w) {
                tmp[rowOff + x] = ((aSum / div) shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                val outP = pix[rowOff + (x - effR).coerceIn(0, w - 1)]
                val inP = pix[rowOff + (x + effR + 1).coerceIn(0, w - 1)]
                aSum += ((inP ushr 24) and 0xFF) - ((outP ushr 24) and 0xFF)
                rSum += ((inP ushr 16) and 0xFF) - ((outP ushr 16) and 0xFF)
                gSum += ((inP ushr 8) and 0xFF) - ((outP ushr 8) and 0xFF)
                bSum += (inP and 0xFF) - (outP and 0xFF)
            }
        }

        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0; var aSum = 0
            for (i in -effR..effR) {
                val p = tmp[i.coerceIn(0, h - 1) * w + x]
                aSum += (p ushr 24) and 0xFF
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (y in 0 until h) {
                pix[y * w + x] = ((aSum / div) shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                val outP = tmp[(y - effR).coerceIn(0, h - 1) * w + x]
                val inP = tmp[(y + effR + 1).coerceIn(0, h - 1) * w + x]
                aSum += ((inP ushr 24) and 0xFF) - ((outP ushr 24) and 0xFF)
                rSum += ((inP ushr 16) and 0xFF) - ((outP ushr 16) and 0xFF)
                gSum += ((inP ushr 8) and 0xFF) - ((outP ushr 8) and 0xFF)
                bSum += (inP and 0xFF) - (outP and 0xFF)
            }
        }

        work.setPixels(pix, 0, w, 0, 0, w, h)
        return if (scaleDown > 1) {
            val rescaled = Bitmap.createScaledBitmap(work, source.width, source.height, true)
            work.recycle()
            rescaled
        } else {
            work
        }
    }

    /**
     * Magic Wand / Chroma Key cutout with smooth edge feathering and configurable Spill Suppression.
     */
    fun applyChromaKey(
        source: Bitmap,
        keyColor: Int,
        tolerance: Float,
        spillSuppression: Float = 0.45f
    ): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)

        val kr = Color.red(keyColor)
        val kg = Color.green(keyColor)
        val kb = Color.blue(keyColor)
        val isGreenKey = kg > kr + 40 && kg > kb + 40
        val maxDist = (tolerance.coerceIn(0.05f, 0.95f) * 320f)
        val featherZone = maxDist * 0.28f
        val spillStrength = spillSuppression.coerceIn(0f, 1f)

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            if (a == 0) continue
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF

            val dr = r - kr
            val dg = g - kg
            val db = b - kb
            val dist = sqrt((dr * dr + dg * dg + db * db).toFloat())

            if (dist < maxDist) {
                if (dist < maxDist - featherZone) {
                    pixels[i] = 0
                } else {
                    val alphaRatio = ((dist - (maxDist - featherZone)) / featherZone).coerceIn(0f, 1f)
                    val newA = (a * alphaRatio).roundToInt()
                    val newG = if (isGreenKey && spillStrength > 0.05f) {
                        val targetG = min(g, ((r + b) / 2 + 10).coerceAtMost(255))
                        (g + (targetG - g) * spillStrength).roundToInt().coerceIn(0, 255)
                    } else g
                    pixels[i] = (newA shl 24) or (r shl 16) or (newG shl 8) or b
                }
            } else if (isGreenKey && spillStrength > 0.05f && g > max(r, b) + 24) {
                val spill = ((g - max(r, b)) * (0.65f * spillStrength)).roundToInt()
                pixels[i] = (a shl 24) or (r shl 16) or ((g - spill).coerceAtLeast(0) shl 8) or b
            }
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    fun applyEraserStrokes(source: Bitmap, strokes: List<EraserStrokePoint>): Bitmap {
        if (strokes.isEmpty()) return source
        val mutable = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        val minDim = min(mutable.width, mutable.height).toFloat()
        for (pt in strokes) {
            val cx = pt.xNorm * mutable.width
            val cy = pt.yNorm * mutable.height
            val rad = (pt.radiusNorm * minDim).coerceAtLeast(4f)
            if (pt.softness > 0.08f) {
                val innerStop = (1f - pt.softness.coerceIn(0.1f, 0.95f)).coerceIn(0.05f, 0.9f)
                paint.shader = RadialGradient(
                    cx, cy, rad,
                    intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                    floatArrayOf(0f, innerStop, 1f),
                    Shader.TileMode.CLAMP
                )
            } else {
                paint.shader = null
                paint.color = Color.BLACK
            }
            canvas.drawCircle(cx, cy, rad, paint)
        }
        return mutable
    }

    /**
     * Spot Heal / Blemish Remover: blends soft local blurred patches over tapped blemish spots.
     */
    fun applySpotHeal(source: Bitmap, points: List<SpotHealPoint>): Bitmap {
        if (points.isEmpty()) return source
        val mutable = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val blurred = fastBoxBlur(mutable, 12)
        val canvas = Canvas(mutable)
        val minDim = min(mutable.width, mutable.height).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        for (pt in points) {
            val cx = pt.xNorm * mutable.width
            val cy = pt.yNorm * mutable.height
            val rad = (pt.radiusNorm * minDim).coerceAtLeast(6f)
            canvas.save()
            val clip = Path().apply { addCircle(cx, cy, rad, Path.Direction.CW) }
            canvas.clipPath(clip)
            canvas.drawBitmap(blurred, 0f, 0f, paint)
            canvas.restore()
        }
        blurred.recycle()
        return mutable
    }

    fun applyAutoOutlineGlow(
        cutout: Bitmap,
        outlineColor: Int,
        outlineWidthPx: Float
    ): Bitmap {
        val pad = outlineWidthPx.roundToInt().coerceIn(4, 32)
        val w = cutout.width + pad * 2
        val h = cutout.height + pad * 2
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val alphaBitmap = cutout.extractAlpha()
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = outlineColor }

        val steps = 16
        for (i in 0 until steps) {
            val angle = (2.0 * PI * i) / steps
            val dx = (cos(angle) * pad * 0.72f).toFloat()
            val dy = (sin(angle) * pad * 0.72f).toFloat()
            canvas.drawBitmap(alphaBitmap, pad + dx, pad + dy, glowPaint)
        }
        glowPaint.alpha = 95
        for (i in 0 until 12) {
            val angle = (2.0 * PI * i) / 12
            val dx = (cos(angle) * pad).toFloat()
            val dy = (sin(angle) * pad).toFloat()
            canvas.drawBitmap(alphaBitmap, pad + dx, pad + dy, glowPaint)
        }
        alphaBitmap.recycle()
        canvas.drawBitmap(cutout, pad.toFloat(), pad.toFloat(), null)
        return result
    }

    fun buildColorMatrix(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        exposure: Float,
        warmth: Float,
        hueDeg: Float = 0f,
        tint: Float = 0f,
        lut: LutPreset = LutPreset.ORIGINAL,
        lutIntensity: Float = 1.0f
    ): ColorMatrix {
        val cm = ColorMatrix()

        val expScale = 1f + (exposure * 0.55f)
        val c = contrast * expScale
        val t = (1f - c) * 128f + (brightness * 95f)
        cm.postConcat(
            ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        cm.postConcat(ColorMatrix().apply { setSaturation(saturation.coerceAtLeast(0f)) })

        if (abs(warmth) > 0.01f || abs(tint) > 0.01f) {
            val rBoost = 1f + warmth * 0.24f + tint * 0.08f
            val gBoost = 1f - tint * 0.22f
            val bBoost = 1f - warmth * 0.24f + tint * 0.08f
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        rBoost, 0f, 0f, 0f, warmth * 12f,
                        0f, gBoost, 0f, 0f, -tint * 10f,
                        0f, 0f, bBoost, 0f, -warmth * 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        if (abs(hueDeg) > 0.5f) {
            val rad = hueDeg * (PI / 180.0)
            val cosV = cos(rad).toFloat()
            val sinV = sin(rad).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        lumR + cosV * (1 - lumR) + sinV * (-lumR), lumG + cosV * (-lumG) + sinV * (-lumG), lumB + cosV * (-lumB) + sinV * (1 - lumB), 0f, 0f,
                        lumR + cosV * (-lumR) + sinV * 0.143f, lumG + cosV * (1 - lumG) + sinV * 0.140f, lumB + cosV * (-lumB) + sinV * (-0.283f), 0f, 0f,
                        lumR + cosV * (-lumR) + sinV * (-(1 - lumR)), lumG + cosV * (-lumG) + sinV * lumG, lumB + cosV * (1 - lumB) + sinV * lumB, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        val clampedIntensity = lutIntensity.coerceIn(0f, 1f)
        if (lut != LutPreset.ORIGINAL && clampedIntensity > 0.01f) {
            val lutCm = buildLutPresetMatrix(lut)
            if (clampedIntensity >= 0.99f) {
                cm.postConcat(lutCm)
            } else {
                val identity = ColorMatrix().array
                val target = lutCm.array
                val blended = FloatArray(20) { i ->
                    identity[i] + (target[i] - identity[i]) * clampedIntensity
                }
                cm.postConcat(ColorMatrix(blended))
            }
        }
        return cm
    }

    private fun buildLutPresetMatrix(lut: LutPreset): ColorMatrix {
        val m = ColorMatrix()
        when (lut) {
            LutPreset.ORIGINAL -> Unit
            LutPreset.CINEMATIC_TEAL -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    1.15f, 0.04f, -0.06f, 0f, 6f,
                    0.02f, 1.02f, 0.04f, 0f, -2f,
                    -0.08f, 0.08f, 1.18f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.HOLLYWOOD_GOLD -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    1.20f, 0.05f, -0.04f, 0f, 8f,
                    0.03f, 1.08f, -0.02f, 0f, 2f,
                    -0.04f, 0f, 0.88f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.BLOCKBUSTER -> {
                m.postConcat(ColorMatrix().apply { setSaturation(1.18f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.18f, 0f, -0.05f, 0f, -6f,
                    0f, 1.08f, 0.02f, 0f, -8f,
                    -0.05f, 0.06f, 1.24f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.ANAMORPHIC -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    0.96f, 0.02f, 0.04f, 0f, -6f,
                    0.02f, 1.06f, 0.06f, 0f, 0f,
                    0.02f, 0.08f, 1.22f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.INDIE_DRAMA -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0.84f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.05f, 0.03f, 0f, 0f, 10f,
                    0.02f, 1.02f, 0.02f, 0f, 8f,
                    0f, 0.02f, 0.92f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.CYBER_CYAN -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    0.92f, 0f, 0.12f, 0f, -4f,
                    0f, 1.14f, 0.14f, 0f, 8f,
                    0.06f, 0.14f, 1.28f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.VAPOR_NEON -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    1.32f, 0.04f, 0.08f, 0f, 12f,
                    0f, 0.84f, 0.04f, 0f, -8f,
                    0.08f, 0.04f, 1.14f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.NEON_GLOW -> {
                m.postConcat(ColorMatrix().apply { setSaturation(1.42f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.22f, 0f, 0.10f, 0f, 6f,
                    0f, 1.04f, 0.08f, 0f, 2f,
                    0.10f, 0.06f, 1.24f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.SYNTHWAVE -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    1.28f, 0f, 0.16f, 0f, 10f,
                    0f, 0.78f, 0.08f, 0f, -12f,
                    0.14f, 0.04f, 1.32f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.ELECTRIC_VIOLET -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0.18f, 0f, 6f,
                    0f, 0.86f, 0.10f, 0f, -6f,
                    0.16f, 0.06f, 1.36f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.MATRIX_GREEN -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    0.76f, 0.08f, 0f, 0f, -14f,
                    0.04f, 1.28f, 0.04f, 0f, 8f,
                    0f, 0.06f, 0.78f, 0f, -14f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.MOODY_DARK -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0.76f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.16f, 0f, 0f, 0f, -26f,
                    0f, 1.14f, 0f, 0f, -24f,
                    0f, 0f, 1.20f, 0f, -18f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.MIDNIGHT_BLUE -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    0.82f, 0f, 0.04f, 0f, -18f,
                    0f, 0.92f, 0.06f, 0f, -12f,
                    0.04f, 0.10f, 1.24f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.OBSIDIAN_SHADOW -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0.68f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.28f, 0f, 0.04f, 0f, -34f,
                    0f, 1.22f, 0f, 0f, -34f,
                    0.04f, 0f, 1.26f, 0f, -30f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.GOTHIC_RED -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0.75f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.34f, 0f, 0f, 0f, -8f,
                    0f, 0.80f, 0f, 0f, -20f,
                    0f, 0f, 0.84f, 0f, -18f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.STORMY_SLATE -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0.62f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    0.96f, 0.02f, 0.04f, 0f, -8f,
                    0.02f, 1.00f, 0.04f, 0f, -6f,
                    0.04f, 0.06f, 1.12f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.KODAK_WARM -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    1.18f, 0.06f, 0f, 0f, 12f,
                    0.02f, 1.05f, 0f, 0f, 4f,
                    0f, 0f, 0.86f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.MATTE_FILM -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    0.92f, 0.03f, 0.02f, 0f, 22f,
                    0.01f, 0.94f, 0.02f, 0f, 18f,
                    0.02f, 0.02f, 0.90f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.RETRO_VHS -> {
                m.postConcat(ColorMatrix().apply { setSaturation(1.12f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.14f, 0.06f, 0.04f, 0f, 14f,
                    0.02f, 0.94f, 0.06f, 0f, 8f,
                    0.08f, 0.02f, 1.08f, 0f, 16f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.VINTAGE_70S -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    1.16f, 0.12f, 0f, 0f, 18f,
                    0.06f, 1.02f, 0f, 0f, 10f,
                    0f, 0.04f, 0.78f, 0f, -4f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.SEPIA_GOLD -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0.25f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.18f, 0.14f, 0.04f, 0f, 10f,
                    0.06f, 1.02f, 0.02f, 0f, 2f,
                    0.02f, 0.02f, 0.78f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.POLAROID_FADE -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    0.96f, 0.04f, 0.04f, 0f, 20f,
                    0.02f, 1.04f, 0.04f, 0f, 16f,
                    0.04f, 0.08f, 1.06f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.HDR_PUNCH -> {
                m.postConcat(ColorMatrix().apply { setSaturation(1.38f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.18f, 0f, 0f, 0f, -18f,
                    0f, 1.18f, 0f, 0f, -18f,
                    0f, 0f, 1.18f, 0f, -18f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.ANIME_SKY -> {
                m.postConcat(ColorMatrix().apply { setSaturation(1.35f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.08f, 0.02f, 0.04f, 0f, 10f,
                    0.02f, 1.12f, 0.04f, 0f, 8f,
                    0.02f, 0.06f, 1.24f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.WARM_SUNSET -> {
                m.postConcat(ColorMatrix().apply { setSaturation(1.22f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.26f, 0.06f, 0f, 0f, 16f,
                    0.04f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.82f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.COLD_WINTER -> m.postConcat(
                ColorMatrix(floatArrayOf(
                    0.88f, 0f, 0.06f, 0f, -4f,
                    0f, 1.02f, 0.08f, 0f, 4f,
                    0.04f, 0.10f, 1.26f, 0f, 16f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
            LutPreset.PASTEL_DREAM -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0.92f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    0.96f, 0.04f, 0.06f, 0f, 28f,
                    0.04f, 0.96f, 0.04f, 0f, 24f,
                    0.06f, 0.04f, 1.02f, 0f, 26f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.TROPICAL_POP -> {
                m.postConcat(ColorMatrix().apply { setSaturation(1.36f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.12f, 0.04f, 0f, 0f, 6f,
                    0.02f, 1.16f, 0.04f, 0f, 6f,
                    0f, 0.04f, 1.08f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.NOIR_BW -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.34f, 0f, 0f, 0f, -36f,
                    0f, 1.34f, 0f, 0f, -36f,
                    0f, 0f, 1.34f, 0f, -36f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.SILVER_MONO -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0f, 0f, -6f,
                    0f, 1.12f, 0f, 0f, -6f,
                    0f, 0f, 1.16f, 0f, -2f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            LutPreset.HIGH_KEY_BW -> {
                m.postConcat(ColorMatrix().apply { setSaturation(0f) })
                m.postConcat(ColorMatrix(floatArrayOf(
                    1.10f, 0f, 0f, 0f, 26f,
                    0f, 1.10f, 0f, 0f, 26f,
                    0f, 0f, 1.10f, 0f, 26f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
        }
        return m
    }

    /**
     * Full per-pixel Lightroom Tone Engine: Highlights, Shadows, Whites, Blacks, Vibrance, Clarity, Sharpness, Tone Curve & HSL Mixer.
     */
    fun applyFineLightroomPass(
        bitmap: Bitmap,
        highlights: Float,
        shadows: Float,
        vibrance: Float,
        sharpness: Float,
        whites: Float = 0f,
        blacks: Float = 0f,
        clarity: Float = 0f,
        toneCurve: ToneCurvePreset = ToneCurvePreset.LINEAR,
        hslRed: Float = 0f,
        hslGreen: Float = 0f,
        hslBlue: Float = 0f
    ): Bitmap {
        val needsTone = abs(highlights) > 0.02f || abs(shadows) > 0.02f ||
            abs(whites) > 0.02f || abs(blacks) > 0.02f || abs(vibrance) > 0.02f ||
            toneCurve != ToneCurvePreset.LINEAR ||
            abs(hslRed) > 0.02f || abs(hslGreen) > 0.02f || abs(hslBlue) > 0.02f
        val effectiveSharp = (sharpness + clarity * 0.65f).coerceIn(0f, 1.4f)
        val needsSharpen = effectiveSharp > 0.05f
        if (!needsTone && !needsSharpen) return bitmap

        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pix = IntArray(w * h)
        out.getPixels(pix, 0, w, 0, 0, w, h)

        if (needsTone) {
            for (i in pix.indices) {
                val p = pix[i]
                val a = (p ushr 24) and 0xFF
                var r = ((p ushr 16) and 0xFF) / 255f
                var g = ((p ushr 8) and 0xFF) / 255f
                var b = (p and 0xFF) / 255f

                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (abs(shadows) > 0.02f) {
                    val m = (1f - lum) * (1f - lum)
                    val d = shadows * 0.42f * m
                    r += d; g += d; b += d
                }
                if (abs(highlights) > 0.02f) {
                    val m = lum * lum
                    val d = highlights * 0.42f * m
                    r += d; g += d; b += d
                }
                if (abs(blacks) > 0.02f) {
                    val m = (1f - lum).let { it * it * it }
                    val d = blacks * 0.36f * m
                    r += d; g += d; b += d
                }
                if (abs(whites) > 0.02f) {
                    val m = lum * lum * lum
                    val d = whites * 0.36f * m
                    r += d; g += d; b += d
                }
                if (abs(vibrance) > 0.02f) {
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    val sat = maxC - minC
                    val boost = vibrance * (1f - sat) * 0.7f
                    val avg = (r + g + b) / 3f
                    r += (r - avg) * boost
                    g += (g - avg) * boost
                    b += (b - avg) * boost
                }
                if (abs(hslRed) > 0.02f && r > g && r > b) {
                    r += hslRed * 0.32f
                }
                if (abs(hslGreen) > 0.02f && g > r && g > b) {
                    g += hslGreen * 0.32f
                }
                if (abs(hslBlue) > 0.02f && b > r && b > g) {
                    b += hslBlue * 0.32f
                }
                if (toneCurve != ToneCurvePreset.LINEAR) {
                    r = applyToneCurveChannel(r, toneCurve)
                    g = applyToneCurveChannel(g, toneCurve)
                    b = applyToneCurveChannel(b, toneCurve)
                }

                val ri = (r.coerceIn(0f, 1f) * 255f).roundToInt()
                val gi = (g.coerceIn(0f, 1f) * 255f).roundToInt()
                val bi = (b.coerceIn(0f, 1f) * 255f).roundToInt()
                pix[i] = (a shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }

        if (needsSharpen && w > 3 && h > 3) {
            val srcCopy = pix.copyOf()
            val amount = effectiveSharp * 0.82f
            for (y in 1 until h - 1) {
                val row = y * w
                for (x in 1 until w - 1) {
                    val c = srcCopy[row + x]
                    val u = srcCopy[row - w + x]
                    val d = srcCopy[row + w + x]
                    val l = srcCopy[row + x - 1]
                    val r = srcCopy[row + x + 1]

                    val a = (c ushr 24) and 0xFF
                    val cr = (c ushr 16) and 0xFF
                    val cg = (c ushr 8) and 0xFF
                    val cb = c and 0xFF

                    val avgR = (((u ushr 16) and 0xFF) + ((d ushr 16) and 0xFF) + ((l ushr 16) and 0xFF) + ((r ushr 16) and 0xFF)) / 4
                    val avgG = (((u ushr 8) and 0xFF) + ((d ushr 8) and 0xFF) + ((l ushr 8) and 0xFF) + ((r ushr 8) and 0xFF)) / 4
                    val avgB = ((u and 0xFF) + (d and 0xFF) + (l and 0xFF) + (r and 0xFF)) / 4

                    val nr = (cr + (cr - avgR) * amount).roundToInt().coerceIn(0, 255)
                    val ng = (cg + (cg - avgG) * amount).roundToInt().coerceIn(0, 255)
                    val nb = (cb + (cb - avgB) * amount).roundToInt().coerceIn(0, 255)
                    pix[row + x] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
                }
            }
        }

        out.setPixels(pix, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyToneCurveChannel(v: Float, curve: ToneCurvePreset): Float {
        val x = v.coerceIn(0f, 1f)
        return when (curve) {
            ToneCurvePreset.LINEAR -> x
            ToneCurvePreset.S_CURVE -> {
                val c = x - 0.5f
                (x + c * (1f - 4f * c * c) * 0.45f).coerceIn(0f, 1f)
            }
            ToneCurvePreset.MATTE -> (0.09f + x * 0.84f).coerceIn(0f, 1f)
            ToneCurvePreset.PUNCHY -> {
                val c = x - 0.5f
                (x + c * (1f - 3.6f * c * c) * 0.65f).coerceIn(0f, 1f)
            }
            ToneCurvePreset.HIGH_KEY -> (x + (1f - x) * 0.16f).coerceIn(0f, 1f)
        }
    }

    fun drawVignette(canvas: Canvas, width: Float, height: Float, strength: Float) {
        if (strength <= 0.01f) return
        val rad = max(width, height) * 0.72f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f,
                height / 2f,
                rad,
                intArrayOf(Color.TRANSPARENT, Color.argb((strength.coerceIn(0f, 1f) * 210f).roundToInt(), 0, 0, 0)),
                floatArrayOf(0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    /**
     * Renders the full Photo & Thumbnail Studio composition with Collage Grids, Perspective Skew,
     * Blend Modes, Double Exposure, Spot Heal, Borders, Full Pro Color Grade, and Optional Safe-Zone Grid.
     */
    suspend fun renderPhotoComposition(
        context: Context,
        state: PhotoStudioState,
        maxEdge: Int,
        includePreviewGuides: Boolean = false
    ): Bitmap = withContext(Dispatchers.Default) {
        val targetW: Int
        val targetH: Int
        val aspect = state.canvasWidth.toFloat() / state.canvasHeight.toFloat().coerceAtLeast(1f)
        if (aspect >= 1f) {
            targetW = min(state.canvasWidth, maxEdge)
            targetH = (targetW / aspect).roundToInt().coerceAtLeast(64)
        } else {
            targetH = min(state.canvasHeight, maxEdge)
            targetW = (targetH * aspect).roundToInt().coerceAtLeast(64)
        }

        val canvasBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.rgb(9, 7, 13))

        val scaleFactor = targetW.toFloat() / state.canvasWidth.toFloat()

        // Optional Multi-Photo Collage Grid for Image Layers
        val imageLayers = state.layers.filter { it.visible && it.kind == LayerKind.IMAGE }
        if (state.collageLayout != CollageLayoutKind.SINGLE && imageLayers.isNotEmpty()) {
            drawCollageGrid(context, canvas, imageLayers, state.collageLayout, targetW, targetH)
        }

        for (layer in state.layers) {
            if (!layer.visible) continue
            if (state.collageLayout != CollageLayoutKind.SINGLE && layer.kind == LayerKind.IMAGE) {
                continue // Already placed cleanly in collage slots
            }
            when (layer.kind) {
                LayerKind.IMAGE -> {
                    val decoded = decodeSampledBitmap(
                        context = context,
                        uriString = layer.imageUri,
                        fallbackDrawable = layer.fallbackDrawable,
                        maxDimension = max(targetW, targetH)
                    ) ?: continue

                    var processed = decoded
                    if (layer.isBackground && state.dslrBackgroundBlur > 0.03f) {
                        val blurred = fastBoxBlur(processed, (state.dslrBackgroundBlur * 24f).roundToInt())
                        if (processed != decoded) processed.recycle()
                        processed = blurred
                    }
                    if (layer.chromaEnabled) {
                        val keyed = applyChromaKey(processed, layer.chromaColor, layer.chromaTolerance)
                        if (processed != decoded) processed.recycle()
                        processed = keyed
                    }
                    if (layer.eraserStrokes.isNotEmpty()) {
                        val erased = applyEraserStrokes(processed, layer.eraserStrokes)
                        if (processed != decoded && erased != processed) processed.recycle()
                        processed = erased
                    }
                    if (layer.spotHealPoints.isNotEmpty()) {
                        val healed = applySpotHeal(processed, layer.spotHealPoints)
                        if (processed != decoded && healed != processed) processed.recycle()
                        processed = healed
                    }
                    if (layer.outlineGlowEnabled) {
                        val outlined = applyAutoOutlineGlow(
                            cutout = processed,
                            outlineColor = layer.outlineGlowColor,
                            outlineWidthPx = (layer.outlineGlowWidth * scaleFactor).coerceAtLeast(3f)
                        )
                        if (processed != decoded) processed.recycle()
                        processed = outlined
                    }

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).roundToInt()
                        toPorterDuffMode(layer.blendMode)?.let { xfermode = PorterDuffXfermode(it) }
                    }

                    canvas.save()
                    val cx = layer.xNorm * targetW
                    val cy = layer.yNorm * targetH
                    canvas.translate(cx, cy)
                    canvas.rotate(layer.rotationDeg)
                    if (abs(layer.skewX) > 0.005f || abs(layer.skewY) > 0.005f) {
                        canvas.skew(layer.skewX.coerceIn(-0.45f, 0.45f), layer.skewY.coerceIn(-0.45f, 0.45f))
                    }
                    if (layer.flipH || layer.flipV) {
                        canvas.scale(if (layer.flipH) -1f else 1f, if (layer.flipV) -1f else 1f)
                    }

                    val baseFit = if (layer.isBackground) {
                        max(targetW.toFloat() / processed.width, targetH.toFloat() / processed.height)
                    } else {
                        min(targetW.toFloat() / processed.width, targetH.toFloat() / processed.height) * 0.68f
                    }
                    val drawW = processed.width * baseFit * layer.scale
                    val drawH = processed.height * baseFit * layer.scale
                    val dstRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
                    canvas.drawBitmap(processed, null, dstRect, paint)

                    // Double Exposure Screen Overlay pass
                    if (layer.doubleExposureAmount > 0.03f) {
                        val dblPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (layer.doubleExposureAmount.coerceIn(0f, 1f) * 175f).roundToInt()
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                        }
                        canvas.save()
                        canvas.scale(1.06f, 1.06f)
                        canvas.translate(drawW * 0.035f, -drawH * 0.02f)
                        canvas.drawBitmap(processed, null, dstRect, dblPaint)
                        canvas.restore()
                    }
                    canvas.restore()

                    if (processed != decoded) processed.recycle()
                }

                LayerKind.SHAPE -> drawShapeLayer(canvas, layer, targetW, targetH, scaleFactor)
                LayerKind.TEXT -> drawTextLayer(context, canvas, layer, targetW, targetH, scaleFactor)
                LayerKind.STICKER -> drawStickerLayer(context, canvas, layer, targetW, targetH, scaleFactor)
            }
        }

        // Apply Pro Color Engine
        val lr = state.lightroom
        val cm = buildColorMatrix(
            brightness = lr.brightness,
            contrast = lr.contrast,
            saturation = lr.saturation,
            exposure = 0f,
            warmth = lr.warmth,
            hueDeg = lr.hueDeg,
            tint = lr.tint
        )
        val gradedBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val gradeCanvas = Canvas(gradedBitmap)
        val cmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        gradeCanvas.drawBitmap(canvasBitmap, 0f, 0f, cmPaint)
        canvasBitmap.recycle()

        var finalBmp = applyFineLightroomPass(
            bitmap = gradedBitmap,
            highlights = lr.highlights,
            shadows = lr.shadows,
            vibrance = lr.vibrance,
            sharpness = lr.sharpness,
            whites = lr.whites,
            blacks = lr.blacks,
            clarity = lr.clarity,
            toneCurve = lr.toneCurve,
            hslRed = lr.hslRedShift,
            hslGreen = lr.hslGreenShift,
            hslBlue = lr.hslBlueShift
        )
        if (finalBmp != gradedBitmap) gradedBitmap.recycle()

        if (lr.blur > 0.03f) {
            val blurred = fastBoxBlur(finalBmp, (lr.blur * 18f).roundToInt())
            finalBmp.recycle()
            finalBmp = blurred
        }

        val finalCanvas = Canvas(finalBmp)
        if (lr.vignette > 0.01f) {
            drawVignette(finalCanvas, targetW.toFloat(), targetH.toFloat(), lr.vignette)
        }

        // Frame & Border
        if (state.borderWidthPx > 0.5f) {
            val bw = (state.borderWidthPx * scaleFactor).coerceAtLeast(3f)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = bw * 2f
                color = state.borderColorArgb
            }
            finalCanvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), borderPaint)
        }

        // Optional YouTube Thumbnail Safe-Zone Grid (Preview only — never exported!)
        if (includePreviewGuides && state.showSafeZoneGrid) {
            drawSafeZoneGuides(finalCanvas, targetW.toFloat(), targetH.toFloat())
        }

        finalBmp
    }

    private fun drawCollageGrid(
        context: Context,
        canvas: Canvas,
        images: List<PhotoLayer>,
        layout: CollageLayoutKind,
        w: Int,
        h: Int
    ) {
        val gap = (min(w, h) * 0.014f).coerceAtLeast(4f)
        val rects = when (layout) {
            CollageLayoutKind.SINGLE -> listOf(RectF(0f, 0f, w.toFloat(), h.toFloat()))
            CollageLayoutKind.SPLIT_2 -> listOf(
                RectF(gap, gap, w / 2f - gap / 2f, h - gap),
                RectF(w / 2f + gap / 2f, gap, w - gap, h - gap)
            )
            CollageLayoutKind.GRID_3 -> listOf(
                RectF(gap, gap, w * 0.56f - gap / 2f, h - gap),
                RectF(w * 0.56f + gap / 2f, gap, w - gap, h / 2f - gap / 2f),
                RectF(w * 0.56f + gap / 2f, h / 2f + gap / 2f, w - gap, h - gap)
            )
            CollageLayoutKind.GRID_4 -> listOf(
                RectF(gap, gap, w / 2f - gap / 2f, h / 2f - gap / 2f),
                RectF(w / 2f + gap / 2f, gap, w - gap, h / 2f - gap / 2f),
                RectF(gap, h / 2f + gap / 2f, w / 2f - gap / 2f, h - gap),
                RectF(w / 2f + gap / 2f, h / 2f + gap / 2f, w - gap, h - gap)
            )
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val radius = gap * 1.4f
        rects.forEachIndexed { idx, slot ->
            val layer = images[idx % images.size]
            val bmp = decodeSampledBitmap(context, layer.imageUri, layer.fallbackDrawable, max(w, h)) ?: return@forEachIndexed
            canvas.save()
            val clipPath = Path().apply { addRoundRect(slot, radius, radius, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            val fillScale = max(slot.width() / bmp.width, slot.height() / bmp.height)
            val dw = bmp.width * fillScale
            val dh = bmp.height * fillScale
            canvas.drawBitmap(
                bmp,
                null,
                RectF(slot.centerX() - dw / 2f, slot.centerY() - dh / 2f, slot.centerX() + dw / 2f, slot.centerY() + dh / 2f),
                paint
            )
            canvas.restore()
        }
    }

    private fun drawSafeZoneGuides(canvas: Canvas, w: Float, h: Float) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(135, 255, 255, 255)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        // Rule of thirds
        canvas.drawLine(w / 3f, 0f, w / 3f, h, gridPaint)
        canvas.drawLine(w * 2f / 3f, 0f, w * 2f / 3f, h, gridPaint)
        canvas.drawLine(0f, h / 3f, w, h / 3f, gridPaint)
        canvas.drawLine(0f, h * 2f / 3f, w, h * 2f / 3f, gridPaint)

        // Safe margin inset (6%)
        val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 30, 66)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(w * 0.05f, h * 0.05f, w * 0.95f, h * 0.95f, marginPaint)

        // YouTube bottom-right duration badge safe zone
        val badgeRect = RectF(w * 0.78f, h * 0.82f, w * 0.96f, h * 0.95f)
        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(170, 255, 30, 66) }
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgeFill)
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (h * 0.045f).coerceAtLeast(14f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("10:00", badgeRect.centerX(), badgeRect.centerY() + txtPaint.textSize * 0.35f, txtPaint)
    }

    private fun drawShapeLayer(
        canvas: Canvas,
        layer: PhotoLayer,
        w: Int,
        h: Int,
        scaleFactor: Float
    ) {
        val baseSize = min(w, h) * 0.24f * layer.scale
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = layer.fillColor
            alpha = (layer.opacity * 255f).roundToInt()
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = layer.strokeColor
            strokeWidth = (layer.strokeWidth * scaleFactor).coerceAtLeast(2f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            alpha = (layer.opacity * 255f).roundToInt()
        }

        canvas.save()
        canvas.translate(layer.xNorm * w, layer.yNorm * h)
        canvas.rotate(layer.rotationDeg)
        if (layer.flipH || layer.flipV) canvas.scale(if (layer.flipH) -1f else 1f, if (layer.flipV) -1f else 1f)

        when (layer.shapeKind) {
            StudioShapeKind.CIRCLE -> {
                canvas.drawCircle(0f, 0f, baseSize * 0.55f, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawCircle(0f, 0f, baseSize * 0.55f, strokePaint)
            }
            StudioShapeKind.RECTANGLE -> {
                val rect = RectF(-baseSize * 0.7f, -baseSize * 0.42f, baseSize * 0.7f, baseSize * 0.42f)
                val r = baseSize * 0.14f
                canvas.drawRoundRect(rect, r, r, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            StudioShapeKind.ARROW -> {
                val path = Path().apply {
                    moveTo(-baseSize * 0.78f, -baseSize * 0.2f)
                    lineTo(baseSize * 0.12f, -baseSize * 0.2f)
                    lineTo(baseSize * 0.12f, -baseSize * 0.52f)
                    lineTo(baseSize * 0.82f, 0f)
                    lineTo(baseSize * 0.12f, baseSize * 0.52f)
                    lineTo(baseSize * 0.12f, baseSize * 0.2f)
                    lineTo(-baseSize * 0.78f, baseSize * 0.2f)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawPath(path, strokePaint)
            }
            StudioShapeKind.STAR -> {
                val path = Path()
                val outerR = baseSize * 0.62f
                val innerR = outerR * 0.44f
                for (i in 0 until 10) {
                    val angle = -PI / 2.0 + i * PI / 5.0
                    val rad = if (i % 2 == 0) outerR else innerR
                    val px = (cos(angle) * rad).toFloat()
                    val py = (sin(angle) * rad).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawPath(path, strokePaint)
            }
            StudioShapeKind.BADGE -> {
                val path = Path()
                val pts = 16
                val outerR = baseSize * 0.64f
                val innerR = outerR * 0.80f
                for (i in 0 until pts * 2) {
                    val angle = i * PI / pts
                    val rad = if (i % 2 == 0) outerR else innerR
                    val px = (cos(angle) * rad).toFloat()
                    val py = (sin(angle) * rad).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawPath(path, strokePaint)
            }
        }
        canvas.restore()
    }

    private fun drawTextLayer(
        context: Context,
        canvas: Canvas,
        layer: PhotoLayer,
        w: Int,
        h: Int,
        scaleFactor: Float
    ) {
        val typeface = StudioFonts.byId(layer.fontId).loadTypeface(context)
        val textSizePx = (min(w, h) * 0.12f * layer.scale).coerceIn(18f, w * 0.45f)
        val lines = layer.text.ifBlank { "BLACK DEVIL" }.split("\n")

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            alpha = (layer.opacity * 255f).roundToInt()
            if (layer.textUseGradient) {
                shader = LinearGradient(
                    0f, -textSizePx * 0.8f, 0f, textSizePx * 0.35f,
                    layer.textColor, layer.textGradientEndColor, Shader.TileMode.CLAMP
                )
            } else {
                color = layer.textColor
            }
            if (layer.textShadowEnabled) {
                setShadowLayer(textSizePx * 0.16f, 0f, textSizePx * 0.09f, Color.argb(215, 0, 0, 0))
            }
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            color = layer.textStrokeColor
            strokeWidth = (layer.textStrokeWidth * scaleFactor * layer.scale).coerceAtLeast(1.5f)
            strokeJoin = Paint.Join.ROUND
            alpha = (layer.opacity * 255f).roundToInt()
        }

        val extrudePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            color = Color.argb((layer.opacity * 235f).roundToInt(), 30, 8, 16)
        }

        canvas.save()
        canvas.translate(layer.xNorm * w, layer.yNorm * h)
        canvas.rotate(layer.rotationDeg)
        if (abs(layer.skewX) > 0.005f || abs(layer.skewY) > 0.005f) {
            canvas.skew(layer.skewX.coerceIn(-0.45f, 0.45f), layer.skewY.coerceIn(-0.45f, 0.45f))
        }

        val lineHeight = textSizePx * 1.12f
        val startY = -((lines.size - 1) * lineHeight) / 2f + textSizePx * 0.34f
        val depthSteps = (layer.text3dDepth * scaleFactor).roundToInt().coerceIn(0, 14)

        if (abs(layer.textCurveDeg) > 4f && lines.size == 1) {
            val single = lines.first()
            val arcRad = max(w * 0.34f, single.length * textSizePx * 0.42f)
            val path = Path()
            val oval = RectF(-arcRad, -arcRad, arcRad, arcRad)
            val sweep = layer.textCurveDeg.coerceIn(-120f, 120f)
            if (sweep > 0f) {
                path.addArc(oval, -90f - sweep / 2f, sweep)
                canvas.translate(0f, arcRad * 0.9f)
            } else {
                path.addArc(oval, 90f - sweep / 2f, sweep)
                canvas.translate(0f, -arcRad * 0.9f)
            }
            for (d in depthSteps downTo 1) {
                canvas.drawTextOnPath(single, path, d.toFloat(), d.toFloat(), extrudePaint)
            }
            if (layer.textStrokeWidth > 0.5f) canvas.drawTextOnPath(single, path, 0f, 0f, strokePaint)
            canvas.drawTextOnPath(single, path, 0f, 0f, fillPaint)
        } else {
            lines.forEachIndexed { idx, line ->
                val yPos = startY + idx * lineHeight
                for (d in depthSteps downTo 1) {
                    canvas.drawText(line, d.toFloat(), yPos + d.toFloat(), extrudePaint)
                }
                if (layer.textStrokeWidth > 0.5f) canvas.drawText(line, 0f, yPos, strokePaint)
                canvas.drawText(line, 0f, yPos, fillPaint)
            }
        }
        canvas.restore()
    }

    private fun drawStickerLayer(
        context: Context,
        canvas: Canvas,
        layer: PhotoLayer,
        w: Int,
        h: Int,
        scaleFactor: Float
    ) {
        val pillW = min(w, h) * 0.28f * layer.scale
        val pillH = pillW * 0.42f
        val rect = RectF(-pillW / 2f, -pillH / 2f, pillW / 2f, pillH / 2f)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                -pillW / 2f, 0f, pillW / 2f, 0f,
                layer.fillColor, layer.textGradientEndColor, Shader.TileMode.CLAMP
            )
            alpha = (layer.opacity * 255f).roundToInt()
            setShadowLayer(16f * scaleFactor, 0f, 6f * scaleFactor, Color.argb(180, 0, 0, 0))
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = (4f * scaleFactor).coerceAtLeast(2f)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = StudioFonts.byId("russo_one").loadTypeface(context)
            color = Color.WHITE
            textSize = pillH * 0.52f
            textAlign = Paint.Align.CENTER
        }

        canvas.save()
        canvas.translate(layer.xNorm * w, layer.yNorm * h)
        canvas.rotate(layer.rotationDeg)
        canvas.drawRoundRect(rect, pillH * 0.35f, pillH * 0.35f, bgPaint)
        canvas.drawRoundRect(rect, pillH * 0.35f, pillH * 0.35f, borderPaint)
        canvas.drawText(layer.stickerSymbol, 0f, pillH * 0.19f, textPaint)
        canvas.restore()
    }

    suspend fun exportPhotoToGallery(
        context: Context,
        state: PhotoStudioState
    ): ExportResult = withContext(Dispatchers.Default) {
        val bmp = renderPhotoComposition(
            context = context,
            state = state,
            maxEdge = max(state.canvasWidth, state.canvasHeight),
            includePreviewGuides = false
        )
        try {
            val out = ByteArrayOutputStream()
            val format = when (state.exportFormat) {
                PhotoExportFormat.PNG_HD -> Bitmap.CompressFormat.PNG
                PhotoExportFormat.JPG_COMPRESSED -> Bitmap.CompressFormat.JPEG
            }
            val quality = if (state.exportFormat == PhotoExportFormat.PNG_HD) 100 else state.jpgQuality.coerceIn(40, 100)
            bmp.compress(format, quality, out)
            val bytes = out.toByteArray()
            val fileName = MediaExportStore.makeFileName("Thumb", state.exportFormat.ext)
            MediaExportStore.saveImageBytes(context, fileName, state.exportFormat.mime, bytes)
        } finally {
            bmp.recycle()
        }
    }

    suspend fun compressAndConvertImage(
        context: Context,
        uriString: String,
        targetFormat: ImageConvertFormat,
        quality: Int
    ): ExportResult = withContext(Dispatchers.IO) {
        val bmp = decodeSampledBitmap(context, uriString, null, 2048)
            ?: return@withContext ExportResult(false, "", 0L, "Unable to decode source image")
        try {
            val baos = ByteArrayOutputStream()
            val compressFmt = when (targetFormat) {
                ImageConvertFormat.PNG -> Bitmap.CompressFormat.PNG
                ImageConvertFormat.JPG -> Bitmap.CompressFormat.JPEG
                ImageConvertFormat.WEBP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
            }
            bmp.compress(compressFmt, quality.coerceIn(20, 100), baos)
            val bytes = baos.toByteArray()
            val fileName = MediaExportStore.makeFileName("Batch", targetFormat.ext)
            MediaExportStore.saveImageBytes(context, fileName, targetFormat.mime, bytes)
        } catch (e: Exception) {
            ExportResult(false, "", 0L, e.message ?: "Compression failed")
        }
    }

    /**
     * Renders a complete A-to-Z Video Frame with BG Fill (Blur/Color/Photo), Rotate/Flip/Mirror,
     * Stabilization, Motion Blur, Keyframes, HSL Grading, Quality Enhancer, PIP Blend Modes + Shape Masking,
     * Visual Transitions, Text Background Pills, Stickers, and Auto-Captions.
     */
    /**
     * Clamps a frame center coordinate along one axis so:
     * - When the scaled frame is smaller than the viewport, it stays completely inside [drawDim/2, viewportDim - drawDim/2].
     * - When the scaled frame is larger than the viewport, it covers the viewport without leaving awkward off-center gaps.
     */
    fun clampFrameCenter(rawCenter: Float, drawDim: Float, viewportDim: Float): Float {
        if (!rawCenter.isFinite() || !drawDim.isFinite() || !viewportDim.isFinite() || viewportDim <= 0f) {
            return viewportDim / 2f
        }
        val half = drawDim / 2f
        return if (drawDim <= viewportDim) {
            rawCenter.coerceIn(half, (viewportDim - half).coerceAtLeast(half))
        } else {
            rawCenter.coerceIn((viewportDim - half).coerceAtMost(half), half)
        }
    }

    fun renderVideoFrame(
        context: Context,
        sourceFrame: Bitmap?,
        targetW: Int,
        targetH: Int,
        cropZoom: Float,
        panX: Float,
        panY: Float,
        blurredBgFill: Boolean,
        bgFillMode: VideoBgFillMode = if (blurredBgFill) VideoBgFillMode.BLUR else VideoBgFillMode.COLOR,
        bgFillColorArgb: Int = Color.BLACK,
        bgImageUri: String? = null,
        clipRotationDeg: Float = 0f,
        clipFlipH: Boolean = false,
        clipFlipV: Boolean = false,
        clipMirrorMode: Boolean = false,
        stabilizeEnabled: Boolean = false,
        motionBlur: Float = 0f,
        keyframeX: Float = 0f,
        keyframeY: Float = 0f,
        keyframeScale: Float = 1f,
        keyframeRot: Float = 0f,
        keyframeOpacity: Float = 1f,
        grade: VideoColorGrade,
        pipBitmap: Bitmap?,
        pipXNorm: Float,
        pipYNorm: Float,
        pipScale: Float,
        pipRotationDeg: Float = 0f,
        pipOpacity: Float = 1f,
        pipBlendMode: BlendModeKind = BlendModeKind.NORMAL,
        pipMaskShape: MaskShapeKind = MaskShapeKind.NONE,
        transitionAlpha: Float = 1f,
        transitionOffsetX: Float = 0f,
        transitionOffsetY: Float = 0f,
        transitionScale: Float = 1f,
        transitionRotationDeg: Float = 0f,
        transitionGlitchAmount: Float = 0f,
        transitionBlurAmount: Float = 0f,
        transitionWipeProgress: Float = 1f,
        transitionWipeVertical: Boolean = false,
        transitionSplitProgress: Float = 1f,
        flashOverlayAlpha: Float = 0f,
        flashOverlayColor: Int = Color.WHITE,
        activeTexts: List<com.example.model.VideoTextOverlay> = emptyList(),
        playheadMs: Long = 0L,
        previousFrame: Bitmap? = null
    ): Bitmap {
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        output.eraseColor(Color.BLACK)
        val canvas = Canvas(output)
        // Always start from clean pure black (#000000) so empty gaps / low opacity NEVER flash white
        canvas.drawColor(Color.BLACK)

        val combinedAlpha = (transitionAlpha * keyframeOpacity).coerceIn(0f, 1f)

        if (sourceFrame != null && !sourceFrame.isRecycled) {
            // 1. Background Fill (Blur / Solid Color / Custom Image) scaled smoothly by clip opacity
            when (bgFillMode) {
                VideoBgFillMode.BLUR -> {
                    if (blurredBgFill && combinedAlpha > 0.01f) {
                        val blurSmall = Bitmap.createScaledBitmap(sourceFrame, 120, 68, true)
                        val blurred = fastBoxBlur(blurSmall, 10)
                        blurSmall.recycle()
                        val bgScale = max(targetW.toFloat() / blurred.width, targetH.toFloat() / blurred.height)
                        val bw = blurred.width * bgScale
                        val bh = blurred.height * bgScale
                        val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (195f * combinedAlpha).roundToInt().coerceIn(0, 255)
                        }
                        canvas.drawBitmap(
                            blurred,
                            null,
                            RectF((targetW - bw) / 2f, (targetH - bh) / 2f, (targetW + bw) / 2f, (targetH + bh) / 2f),
                            bgPaint
                        )
                        blurred.recycle()
                    }
                }
                VideoBgFillMode.COLOR -> {
                    if (bgFillColorArgb != Color.BLACK && combinedAlpha > 0.01f) {
                        val bgPaint = Paint().apply {
                            color = bgFillColorArgb
                            alpha = (Color.alpha(bgFillColorArgb) * combinedAlpha).roundToInt().coerceIn(0, 255)
                        }
                        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), bgPaint)
                    }
                }
                VideoBgFillMode.IMAGE -> {
                    val bgBmp = decodeSampledBitmap(context, bgImageUri, null, max(targetW, targetH))
                    if (bgBmp != null && combinedAlpha > 0.01f) {
                        val bgScale = max(targetW.toFloat() / bgBmp.width, targetH.toFloat() / bgBmp.height)
                        val bw = bgBmp.width * bgScale
                        val bh = bgBmp.height * bgScale
                        val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (255f * combinedAlpha).roundToInt().coerceIn(0, 255)
                        }
                        canvas.drawBitmap(
                            bgBmp,
                            null,
                            RectF((targetW - bw) / 2f, (targetH - bh) / 2f, (targetW + bw) / 2f, (targetH + bh) / 2f),
                            bgPaint
                        )
                    }
                }
            }

            // 1b. Optional Outgoing Clip Backdrop during Multi-Clip Transitions
            if (previousFrame != null && !previousFrame.isRecycled) {
                val prevFit = min(targetW.toFloat() / previousFrame.width, targetH.toFloat() / previousFrame.height)
                val prevScale = prevFit * cropZoom.coerceIn(0.25f, 3.0f)
                val prevW = previousFrame.width * prevScale
                val prevH = previousFrame.height * prevScale
                val prevCx = clampFrameCenter(targetW / 2f + panX * targetW * 0.4f, prevW, targetW.toFloat())
                val prevCy = clampFrameCenter(targetH / 2f + panY * targetH * 0.4f, prevH, targetH.toFloat())
                canvas.drawBitmap(
                    previousFrame,
                    null,
                    RectF(prevCx - prevW / 2f, prevCy - prevH / 2f, prevCx + prevW / 2f, prevCy + prevH / 2f),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
            }

            // 2. Main Framed Video with HSL + ColorMatrix + LUT + Keyframes + Clip Transforms
            if (combinedAlpha > 0.005f) {
                val cm = buildColorMatrix(
                    brightness = (grade.brightness + grade.hslLight).coerceIn(-1f, 1f),
                    contrast = grade.contrast,
                    saturation = (grade.saturation + grade.hslSat).coerceIn(0f, 2.5f),
                    exposure = grade.exposure,
                    warmth = grade.temperature,
                    hueDeg = grade.hslHue,
                    lut = grade.lut,
                    lutIntensity = grade.filterIntensity
                )
                val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(cm)
                    alpha = (combinedAlpha * 255f).roundToInt().coerceIn(0, 255)
                }

                val stabilizeBoost = if (stabilizeEnabled) 1.09f else 1f
                val baseFit = min(targetW.toFloat() / sourceFrame.width, targetH.toFloat() / sourceFrame.height)
                val baseScale = baseFit * cropZoom.coerceIn(0.25f, 3.0f) * keyframeScale.coerceIn(0.2f, 3.0f) * stabilizeBoost
                val baseDrawW = sourceFrame.width * baseScale
                val baseDrawH = sourceFrame.height * baseScale
                val clampedBaseCx = clampFrameCenter(
                    targetW / 2f + (panX + keyframeX) * targetW * 0.4f,
                    baseDrawW,
                    targetW.toFloat()
                )
                val clampedBaseCy = clampFrameCenter(
                    targetH / 2f + (panY + keyframeY) * targetH * 0.4f,
                    baseDrawH,
                    targetH.toFloat()
                )

                val drawW = baseDrawW * transitionScale
                val drawH = baseDrawH * transitionScale
                val cx = clampedBaseCx + transitionOffsetX * targetW
                val cy = clampedBaseCy + transitionOffsetY * targetH

                val combinedBlur = max(grade.blur, transitionBlurAmount)
                var processedSource = if (combinedBlur > 0.04f) {
                    fastBoxBlur(sourceFrame, (combinedBlur * 14f).roundToInt())
                } else {
                    sourceFrame
                }
        if (grade.sharpen > 0.04f) {
            val sharpened = applyFineLightroomPass(
                bitmap = processedSource,
                highlights = 0f,
                shadows = 0f,
                vibrance = 0f,
                sharpness = grade.sharpen
            )
            if (processedSource != sourceFrame) processedSource.recycle()
            processedSource = sharpened
        }
        if (grade.qualityEnhancerEnabled) {
            val enhanced = applyQualityEnhancerPass(processedSource)
            if (processedSource != sourceFrame) processedSource.recycle()
            processedSource = enhanced
        }

        canvas.save()
        if (transitionWipeProgress < 0.99f) {
            val p = transitionWipeProgress.coerceIn(0.02f, 1f)
            if (transitionWipeVertical) {
                canvas.clipRect(0f, targetH * (1f - p), targetW.toFloat(), targetH.toFloat())
            } else {
                canvas.clipRect(0f, 0f, targetW * p, targetH.toFloat())
            }
        } else if (transitionSplitProgress < 0.99f) {
            val halfW = (targetW / 2f) * transitionSplitProgress.coerceIn(0.02f, 1f)
            canvas.clipRect(targetW / 2f - halfW, 0f, targetW / 2f + halfW, targetH.toFloat())
        }

        canvas.translate(cx, cy)
        val totalRot = clipRotationDeg + transitionRotationDeg + keyframeRot
        if (abs(totalRot) > 0.1f) canvas.rotate(totalRot)
        if (clipFlipH || clipFlipV) {
            canvas.scale(if (clipFlipH) -1f else 1f, if (clipFlipV) -1f else 1f)
        }

        val drawRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
        if (clipMirrorMode) {
            // Left half drawn normally, right half mirrored symmetrically
            canvas.save()
            canvas.clipRect(-drawW / 2f, -drawH / 2f, 0f, drawH / 2f)
            canvas.drawBitmap(processedSource, null, drawRect, mainPaint)
            canvas.restore()

            canvas.save()
            canvas.clipRect(0f, -drawH / 2f, drawW / 2f, drawH / 2f)
            canvas.scale(-1f, 1f)
            canvas.drawBitmap(processedSource, null, drawRect, mainPaint)
            canvas.restore()
        } else {
            canvas.drawBitmap(processedSource, null, drawRect, mainPaint)
        }

        // Motion Blur directional ghosting pass
        if (motionBlur > 0.04f) {
            val shift = targetW * 0.024f * motionBlur.coerceIn(0f, 1f)
            val ghostPaint = Paint(mainPaint).apply { alpha = (combinedAlpha * motionBlur * 95f).roundToInt() }
            canvas.drawBitmap(processedSource, null, RectF(drawRect.left - shift, drawRect.top, drawRect.right - shift, drawRect.bottom), ghostPaint)
            canvas.drawBitmap(processedSource, null, RectF(drawRect.left + shift, drawRect.top, drawRect.right + shift, drawRect.bottom), ghostPaint)
        }

        // RGB Glitch chromatic split during Glitch transitions
        if (transitionGlitchAmount > 0.05f) {
            val shift = targetW * 0.022f * transitionGlitchAmount
            val rPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (transitionGlitchAmount * 115f).roundToInt()
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, 35f,
                            0f, 0.2f, 0f, 0f, 0f,
                            0f, 0f, 0.2f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            val cPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (transitionGlitchAmount * 115f).roundToInt()
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            0.2f, 0f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f, 25f,
                            0f, 0f, 1f, 0f, 35f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            canvas.drawBitmap(processedSource, null, RectF(-drawW / 2f - shift, -drawH / 2f, drawW / 2f - shift, drawH / 2f), rPaint)
            canvas.drawBitmap(processedSource, null, RectF(-drawW / 2f + shift, -drawH / 2f, drawW / 2f + shift, drawH / 2f), cPaint)
        }
                canvas.restore()

                if (processedSource != sourceFrame) processedSource.recycle()
            }
        }

        // 3. PIP Overlay with Blend Mode, Shape Masking, Opacity & Rotation (clamped cleanly inside viewport)
        if (pipBitmap != null && !pipBitmap.isRecycled) {
            val pipW = (targetW * pipScale.coerceIn(0.14f, 0.9f)).coerceAtMost(targetW.toFloat())
            val pipH = (pipW * (pipBitmap.height.toFloat() / pipBitmap.width.toFloat().coerceAtLeast(1f)))
                .coerceAtMost(targetH.toFloat())
            val px = clampFrameCenter(pipXNorm * targetW, pipW, targetW.toFloat())
            val py = clampFrameCenter(pipYNorm * targetH, pipH, targetH.toFloat())
            val pipRect = RectF(-pipW / 2f, -pipH / 2f, pipW / 2f, pipH / 2f)
            val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (pipOpacity.coerceIn(0.05f, 1f) * 255f).roundToInt()
                toPorterDuffMode(pipBlendMode)?.let { xfermode = PorterDuffXfermode(it) }
            }

            canvas.save()
            canvas.translate(px, py)
            if (abs(pipRotationDeg) > 0.1f) canvas.rotate(pipRotationDeg)
            buildMaskPath(pipMaskShape, pipRect)?.let { canvas.clipPath(it) }
            canvas.drawBitmap(pipBitmap, null, pipRect, pipPaint)
            canvas.restore()
        }

        // 4. Vignette & Light / Flash Transition
        if (grade.vignette > 0.01f) {
            drawVignette(canvas, targetW.toFloat(), targetH.toFloat(), grade.vignette)
        }
        if (flashOverlayAlpha > 0.01f) {
            val a = (flashOverlayAlpha.coerceIn(0f, 1f) * 235f).roundToInt()
            canvas.drawColor(Color.argb(a, Color.red(flashOverlayColor), Color.green(flashOverlayColor), Color.blue(flashOverlayColor)))
        }

        // 5. Pro Animated Text, Background Pills, Vector/Emoji/Photo Stickers & Captions
        VideoOverlayRenderer.drawOverlays(
            context = context,
            canvas = canvas,
            activeTexts = activeTexts,
            playheadMs = playheadMs,
            targetW = targetW,
            targetH = targetH
        )

        return output
    }

    /**
     * One-Tap "Video Quality Enhancer (HD/4K Clarity)"
     */
    fun applyQualityEnhancerPass(source: Bitmap): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        if (w < 3 || h < 3) return out

        val src = IntArray(w * h)
        out.getPixels(src, 0, w, 0, 0, w, h)
        val dst = src.copyOf()

        val noiseThreshold = 14
        val unsharpAmount = 0.78f

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val c = src[row + x]
                val u = src[row - w + x]
                val d = src[row + w + x]
                val l = src[row + x - 1]
                val r = src[row + x + 1]

                val a = (c ushr 24) and 0xFF
                val cr = (c ushr 16) and 0xFF
                val cg = (c ushr 8) and 0xFF
                val cb = c and 0xFF

                val avgR = (((u ushr 16) and 0xFF) + ((d ushr 16) and 0xFF) + ((l ushr 16) and 0xFF) + ((r ushr 16) and 0xFF)) shr 2
                val avgG = (((u ushr 8) and 0xFF) + ((d ushr 8) and 0xFF) + ((l ushr 8) and 0xFF) + ((r ushr 8) and 0xFF)) shr 2
                val avgB = ((u and 0xFF) + (d and 0xFF) + (l and 0xFF) + (r and 0xFF)) shr 2

                val diffR = cr - avgR
                val diffG = cg - avgG
                val diffB = cb - avgB
                val maxGrad = max(abs(diffR), max(abs(diffG), abs(diffB)))

                var nr: Float
                var ng: Float
                var nb: Float

                if (maxGrad < noiseThreshold) {
                    nr = cr * 0.65f + avgR * 0.35f
                    ng = cg * 0.65f + avgG * 0.35f
                    nb = cb * 0.65f + avgB * 0.35f
                } else {
                    nr = cr + (diffR * unsharpAmount).coerceIn(-38f, 38f)
                    ng = cg + (diffG * unsharpAmount).coerceIn(-38f, 38f)
                    nb = cb + (diffB * unsharpAmount).coerceIn(-38f, 38f)
                }

                nr = microContrastCurve(nr / 255f) * 255f
                ng = microContrastCurve(ng / 255f) * 255f
                nb = microContrastCurve(nb / 255f) * 255f

                dst[row + x] = (a shl 24) or
                    (nr.roundToInt().coerceIn(0, 255) shl 16) or
                    (ng.roundToInt().coerceIn(0, 255) shl 8) or
                    nb.roundToInt().coerceIn(0, 255)
            }
        }
        out.setPixels(dst, 0, w, 0, 0, w, h)
        return out
    }

    private fun microContrastCurve(v: Float): Float {
        val x = v.coerceIn(0f, 1f)
        val centered = x - 0.5f
        return (x + centered * (1f - 4f * centered * centered) * 0.28f).coerceIn(0f, 1f)
    }
}
