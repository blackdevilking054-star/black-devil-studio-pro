package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R

val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val InterFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

data class StudioFontOption(
    val id: String,
    val displayName: String,
    val category: String,
    @FontRes val fontResId: Int,
    val fontFamily: FontFamily
) {
    fun loadTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, fontResId) ?: Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }
}

object StudioFonts {
    val all: List<StudioFontOption> = listOf(
        StudioFontOption("bangers", "Bangers", "Gaming", R.font.bangers, FontFamily(Font(R.font.bangers))),
        StudioFontOption("anton", "Anton", "Thumbnail", R.font.anton, FontFamily(Font(R.font.anton))),
        StudioFontOption("bebas_neue", "Bebas Neue", "Vlog", R.font.bebas_neue, FontFamily(Font(R.font.bebas_neue))),
        StudioFontOption("black_ops_one", "Black Ops", "Esports", R.font.black_ops_one, FontFamily(Font(R.font.black_ops_one))),
        StudioFontOption("orbitron", "Orbitron", "Sci-Fi", R.font.orbitron, FontFamily(Font(R.font.orbitron))),
        StudioFontOption("russo_one", "Russo One", "Gaming", R.font.russo_one, FontFamily(Font(R.font.russo_one))),
        StudioFontOption("press_start", "8-Bit Retro", "Arcade", R.font.press_start_2p, FontFamily(Font(R.font.press_start_2p))),
        StudioFontOption("permanent_marker", "Marker", "Vlog", R.font.permanent_marker, FontFamily(Font(R.font.permanent_marker))),
        StudioFontOption("righteous", "Righteous", "Neon", R.font.righteous, FontFamily(Font(R.font.righteous))),
        StudioFontOption("oswald", "Oswald", "Bold", R.font.oswald, FontFamily(Font(R.font.oswald))),
        StudioFontOption("montserrat", "Montserrat", "Modern", R.font.montserrat, FontFamily(Font(R.font.montserrat))),
        StudioFontOption("cinzel", "Cinzel", "Cinema", R.font.cinzel, FontFamily(Font(R.font.cinzel))),
        StudioFontOption("space_grotesk", "Space", "Tech", R.font.space_grotesk, SpaceGroteskFamily),
        StudioFontOption("jetbrains_mono", "Code Mono", "HUD", R.font.jetbrains_mono, JetBrainsMonoFamily)
    )

    fun byId(id: String): StudioFontOption = all.firstOrNull { it.id == id } ?: all.first()
}

val XonixTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.3.sp
    )
)
