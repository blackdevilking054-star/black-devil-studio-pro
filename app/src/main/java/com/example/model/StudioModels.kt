package com.example.model

import androidx.annotation.DrawableRes

enum class StudioWorkspace(val label: String, val badge: String) {
    HOME("Home", "Creator Hub"),
    VIDEO("Video", "Video Studio"),
    PHOTO("Photo", "Photo Studio"),
    AUDIO("Music", "Audio & SFX"),
    VAULT("Tools", "Projects & Tools")
}

// ==========================================
// WORKSPACE 1: A-TO-Z VIDEO EDITOR MODELS
// ==========================================

enum class VideoAspectPreset(val label: String, val ratioW: Int, val ratioH: Int, val tag: String) {
    YOUTUBE_16_9("16:9", 16, 9, "YouTube"),
    SHORTS_9_16("9:16", 9, 16, "Shorts/Reels"),
    SQUARE_1_1("1:1", 1, 1, "Square"),
    PORTRAIT_4_5("4:5", 4, 5, "Feed"),
    CINEMA_21_9("21:9", 21, 9, "Cinema")
}

enum class VideoBgFillMode(val label: String) {
    BLUR("Blur"),
    COLOR("Color"),
    IMAGE("Photo")
}

enum class SpeedCurvePreset(val label: String) {
    NORMAL("Normal"),
    MONTAGE("Montage"),
    HERO("Hero"),
    BULLET("Bullet"),
    JUMP_CUT("Jump Cut"),
    FLASH("Flash")
}

enum class BlendModeKind(val label: String) {
    NORMAL("Normal"),
    SCREEN("Screen"),
    MULTIPLY("Multiply"),
    OVERLAY("Overlay"),
    LIGHTEN("Lighten")
}

enum class MaskShapeKind(val label: String) {
    NONE("None"),
    CIRCLE("Circle"),
    RECTANGLE("Rect"),
    LINEAR("Split"),
    STAR("Star"),
    HEART("Heart")
}

enum class TransitionCategory(val label: String) {
    BASIC("Basic"),
    CAMERA("Camera"),
    GLITCH("Glitch"),
    SPIN_ZOOM("Spin/Zoom"),
    SHAKE("Shake"),
    SPLIT("Split"),
    LIGHT("Light Flash")
}

enum class VideoTransition(val label: String, val category: TransitionCategory) {
    // Basic (8)
    NONE("None", TransitionCategory.BASIC),
    CROSSFADE("Crossfade", TransitionCategory.BASIC),
    DIP_TO_BLACK("Dip Black", TransitionCategory.BASIC),
    DIP_TO_WHITE("Dip White", TransitionCategory.BASIC),
    WIPE_LEFT("Wipe Left", TransitionCategory.BASIC),
    WIPE_RIGHT("Wipe Right", TransitionCategory.BASIC),
    WIPE_UP("Wipe Up", TransitionCategory.BASIC),
    WIPE_DOWN("Wipe Down", TransitionCategory.BASIC),

    // Camera (6)
    WHIP_PAN("Whip Pan", TransitionCategory.CAMERA),
    WHIP_UP("Whip Up", TransitionCategory.CAMERA),
    SLIDE_LEFT("Slide Left", TransitionCategory.CAMERA),
    SLIDE_RIGHT("Slide Right", TransitionCategory.CAMERA),
    SLIDE_UP("Slide Up", TransitionCategory.CAMERA),
    SLIDE_DOWN("Slide Down", TransitionCategory.CAMERA),

    // Glitch (6)
    GLITCH_RGB("Glitch RGB", TransitionCategory.GLITCH),
    PIXEL_SPLIT("Pixel Split", TransitionCategory.GLITCH),
    SCANLINE("Scanline", TransitionCategory.GLITCH),
    CHROMATIC("Chromatic", TransitionCategory.GLITCH),
    STROBE("Strobe", TransitionCategory.GLITCH),
    VHS_TRACKING("VHS Glitch", TransitionCategory.GLITCH),

    // Spin / Zoom (6)
    ZOOM_IN("Zoom In", TransitionCategory.SPIN_ZOOM),
    ZOOM_OUT("Zoom Out", TransitionCategory.SPIN_ZOOM),
    SPIN_360("Spin 360", TransitionCategory.SPIN_ZOOM),
    SPIN_LEFT("Spin Left", TransitionCategory.SPIN_ZOOM),
    SWIRL_ZOOM("Swirl Zoom", TransitionCategory.SPIN_ZOOM),
    CRASH_ZOOM("Crash Zoom", TransitionCategory.SPIN_ZOOM),

    // Shake (4)
    SHAKE("Shake", TransitionCategory.SHAKE),
    EARTHQUAKE("Quake", TransitionCategory.SHAKE),
    IMPACT_HIT("Impact Hit", TransitionCategory.SHAKE),
    HANDHELD_JOLT("Jolt", TransitionCategory.SHAKE),

    // Split (4)
    SPLIT("Split Open", TransitionCategory.SPLIT),
    SPLIT_VERTICAL("Split Vert", TransitionCategory.SPLIT),
    CURTAIN_OPEN("Curtain", TransitionCategory.SPLIT),
    SHUTTER_SPLIT("Shutter", TransitionCategory.SPLIT),

    // Light Flash (6)
    FLASH("White Flash", TransitionCategory.LIGHT),
    BLUR("Blur Dissolve", TransitionCategory.LIGHT),
    LIGHT_LEAK("Light Leak", TransitionCategory.LIGHT),
    GLOW_BURN("Glow Burn", TransitionCategory.LIGHT),
    RADIAL_FLASH("Radial Flash", TransitionCategory.LIGHT),
    CRIMSON_FLASH("Crimson Flash", TransitionCategory.LIGHT)
}

enum class FilterCategory(val label: String) {
    CINEMATIC("Cinematic"),
    CYBER("Cyber & Neon"),
    MOODY("Moody Dark"),
    VINTAGE("Vintage Film"),
    VIBRANT("Vivid & Anime"),
    BW("B&W Art")
}

enum class LutPreset(
    val label: String,
    val category: FilterCategory,
    val swatchPrimaryHex: Long,
    val swatchSecondaryHex: Long
) {
    // Cinematic (6)
    ORIGINAL("Original", FilterCategory.CINEMATIC, 0xFF3A3446, 0xFF1D1926),
    CINEMATIC_TEAL("Teal & Orange", FilterCategory.CINEMATIC, 0xFF0099A8, 0xFFE67E22),
    HOLLYWOOD_GOLD("Hollywood", FilterCategory.CINEMATIC, 0xFFD4AF37, 0xFF2B1E12),
    BLOCKBUSTER("Blockbuster", FilterCategory.CINEMATIC, 0xFF1D4E89, 0xFFD95D39),
    ANAMORPHIC("Anamorphic", FilterCategory.CINEMATIC, 0xFF2C5364, 0xFF0F2027),
    INDIE_DRAMA("Indie Film", FilterCategory.CINEMATIC, 0xFF6B705C, 0xFFCB997E),

    // Cyber & Neon (6)
    CYBER_CYAN("Cyberpunk", FilterCategory.CYBER, 0xFF00E5FF, 0xFF9B1FE8),
    VAPOR_NEON("Crimson Neon", FilterCategory.CYBER, 0xFFFF1E42, 0xFF7B1FA2),
    NEON_GLOW("Neon Glow", FilterCategory.CYBER, 0xFFFF2A85, 0xFF00F0FF),
    SYNTHWAVE("Synthwave", FilterCategory.CYBER, 0xFFFF007F, 0xFF240046),
    ELECTRIC_VIOLET("Ultraviolet", FilterCategory.CYBER, 0xFF8A2BE2, 0xFF1E1038),
    MATRIX_GREEN("Cyber Matrix", FilterCategory.CYBER, 0xFF00FF66, 0xFF082214),

    // Moody Dark (5)
    MOODY_DARK("Moody Dark", FilterCategory.MOODY, 0xFF2A2D34, 0xFF0D0F14),
    MIDNIGHT_BLUE("Midnight", FilterCategory.MOODY, 0xFF102A43, 0xFF243B53),
    OBSIDIAN_SHADOW("Obsidian", FilterCategory.MOODY, 0xFF3B1C32, 0xFF0A070C),
    GOTHIC_RED("Blood Moon", FilterCategory.MOODY, 0xFF990024, 0xFF140509),
    STORMY_SLATE("Stormy Slate", FilterCategory.MOODY, 0xFF475569, 0xFF1E293B),

    // Vintage Film (6)
    KODAK_WARM("Kodak Warm", FilterCategory.VINTAGE, 0xFFE09F3E, 0xFF540B0E),
    MATTE_FILM("Matte Film", FilterCategory.VINTAGE, 0xFF8D99AE, 0xFF4A4E69),
    RETRO_VHS("Retro VHS", FilterCategory.VINTAGE, 0xFFFF5E5B, 0xFF2E294E),
    VINTAGE_70S("Vintage 70s", FilterCategory.VINTAGE, 0xFFD97706, 0xFF78350F),
    SEPIA_GOLD("Sepia Gold", FilterCategory.VINTAGE, 0xFFB08968, 0xFF4A321F),
    POLAROID_FADE("Polaroid", FilterCategory.VINTAGE, 0xFFA8DADC, 0xFF457B9D),

    // Vivid & Anime (6)
    HDR_PUNCH("HDR Vivid", FilterCategory.VIBRANT, 0xFFFF4D00, 0xFF00B4D8),
    ANIME_SKY("Anime", FilterCategory.VIBRANT, 0xFF38BDF8, 0xFFF472B6),
    WARM_SUNSET("Warm Sunset", FilterCategory.VIBRANT, 0xFFFF6B35, 0xFFF7C59F),
    COLD_WINTER("Cold Winter", FilterCategory.VIBRANT, 0xFF60A5FA, 0xFF1E3A8A),
    PASTEL_DREAM("Pastel", FilterCategory.VIBRANT, 0xFFFBCFE8, 0xFFA5F3FC),
    TROPICAL_POP("Tropical", FilterCategory.VIBRANT, 0xFF10B981, 0xFFF59E0B),

    // B&W Art (3)
    NOIR_BW("B&W Noir", FilterCategory.BW, 0xFF64748B, 0xFF09090B),
    SILVER_MONO("Silver Mono", FilterCategory.BW, 0xFFCBD5E1, 0xFF334155),
    HIGH_KEY_BW("Bright B&W", FilterCategory.BW, 0xFFF1F5F9, 0xFF64748B)
}

enum class TextAnimationStyle(val label: String) {
    NONE("None"),
    FADE("Fade"),
    POP("Pop"),
    SLIDE_UP("Slide Up"),
    TYPEWRITER("Type")
}

enum class VideoExportResolution(
    val label: String,
    val shortLabel: String,
    val longEdge: Int,
    val bitrateMbps: Int
) {
    HD_720P("HD 720p", "HD", 1280, 6),
    FHD_1080P("Full HD", "Full HD", 1920, 12),
    UHD_4K("4K Ultra", "4K", 3840, 28)
}

enum class VideoExportFps(val label: String, val fps: Int) {
    FPS_30("Standard", 30),
    FPS_60("Smooth", 60)
}

enum class VideoExportPreset(
    val label: String,
    val subtitle: String,
    val aspect: VideoAspectPreset?,
    val resolution: VideoExportResolution,
    val fps: VideoExportFps
) {
    YOUTUBE_4K_60(
        label = "YouTube 4K 60fps",
        subtitle = "16:9 • 4K Ultra • 60fps",
        aspect = VideoAspectPreset.YOUTUBE_16_9,
        resolution = VideoExportResolution.UHD_4K,
        fps = VideoExportFps.FPS_60
    ),
    REELS_SHORTS_1080_60(
        label = "Reels/Shorts 1080p 60fps",
        subtitle = "9:16 • Full HD • 60fps",
        aspect = VideoAspectPreset.SHORTS_9_16,
        resolution = VideoExportResolution.FHD_1080P,
        fps = VideoExportFps.FPS_60
    ),
    INSTAGRAM_1080(
        label = "Instagram 1080p",
        subtitle = "4:5 • Full HD • 30fps",
        aspect = VideoAspectPreset.PORTRAIT_4_5,
        resolution = VideoExportResolution.FHD_1080P,
        fps = VideoExportFps.FPS_30
    ),
    WHATSAPP_FAST(
        label = "WhatsApp Fast Share",
        subtitle = "HD 720p • Fast Compact • 30fps",
        aspect = null,
        resolution = VideoExportResolution.HD_720P,
        fps = VideoExportFps.FPS_30
    )
}

data class HardwareEncoderCapabilities(
    val encoderName: String = "Hardware AVC",
    val supports4K30: Boolean = false,
    val supports4K60: Boolean = false,
    val supports1080p60: Boolean = true,
    val maxWidth: Int = 1920,
    val maxHeight: Int = 1080
)

data class VideoKeyframe(
    val id: String,
    val timeMs: Long,
    val xNorm: Float = 0f,
    val yNorm: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f
)

data class VideoAudioClip(
    val id: String,
    val name: String,
    val filePath: String? = null,
    val startMs: Long = 0L,
    val durationMs: Long = 4000L,
    val trimStartPct: Float = 0f,
    val trimEndPct: Float = 1f,
    val volume: Float = 1.0f, // 0f..2f (0%..200%)
    val fadeInSec: Float = 0.2f,
    val fadeOutSec: Float = 0.4f,
    val muted: Boolean = false,
    val isVoiceover: Boolean = false,
    val isSfx: Boolean = false
)

data class VideoClip(
    val id: String,
    val name: String,
    val uriString: String? = null,
    @DrawableRes val fallbackDrawable: Int? = null,
    val durationMs: Long = 5000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 5000L,
    val speed: Float = 1f, // 0.1x to 10.0x
    val speedCurve: SpeedCurvePreset = SpeedCurvePreset.NORMAL,
    val reversed: Boolean = false,
    val rotationDeg: Float = 0f, // 90 deg steps or free -180..180
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val mirrorMode: Boolean = false,
    val stabilizeEnabled: Boolean = false,
    val motionBlur: Float = 0f, // 0..1
    val transitionAfter: VideoTransition = VideoTransition.CROSSFADE,
    val transitionDurationMs: Long = 500L,
    val keyframes: List<VideoKeyframe> = emptyList(),
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0
) {
    val effectiveDurationMs: Long
        get() = (((trimEndMs - trimStartMs).coerceAtLeast(300L)) / speed.coerceIn(0.1f, 10f)).toLong().coerceAtLeast(250L)
}

data class VideoColorGrade(
    val brightness: Float = 0f,     // -1f..1f
    val contrast: Float = 1f,       // 0.4f..2.0f
    val saturation: Float = 1f,     // 0f..2.2f
    val exposure: Float = 0f,       // -1f..1f
    val temperature: Float = 0f,    // -1f..1f
    val sharpen: Float = 0f,        // 0f..1f
    val blur: Float = 0f,           // 0f..1f
    val vignette: Float = 0f,       // 0f..1f
    val hslHue: Float = 0f,         // -180f..180f
    val hslSat: Float = 0f,         // -1f..1f
    val hslLight: Float = 0f,       // -0.5f..0.5f
    val lut: LutPreset = LutPreset.ORIGINAL,
    val filterIntensity: Float = 1.0f, // 0f..1f (0%..100%)
    val qualityEnhancerEnabled: Boolean = false
)

data class PipOverlayState(
    val enabled: Boolean = false,
    val uriString: String? = null,
    @DrawableRes val fallbackDrawable: Int? = null,
    val label: String = "Overlay",
    val xNorm: Float = 0.72f,
    val yNorm: Float = 0.26f,
    val scale: Float = 0.36f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val blendMode: BlendModeKind = BlendModeKind.NORMAL,
    val maskShape: MaskShapeKind = MaskShapeKind.NONE,
    val chromaEnabled: Boolean = false,
    val chromaKeyColor: Int = 0xFF00FF00.toInt(),
    val chromaTolerance: Float = 0.36f,
    val spillSuppression: Float = 0.45f
)

data class VideoTextOverlay(
    val id: String,
    val text: String,
    val fontId: String = "anton",
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val strokeArgb: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 6f,
    val shadowEnabled: Boolean = true,
    val bgEnabled: Boolean = false,
    val bgColorArgb: Int = 0xCC120916.toInt(),
    val xNorm: Float = 0.5f,
    val yNorm: Float = 0.80f,
    val scale: Float = 1.0f,
    val startMs: Long = 0L,
    val endMs: Long = 4000L,
    val animation: TextAnimationStyle = TextAnimationStyle.POP,
    val isCaption: Boolean = false,
    val isSticker: Boolean = false
)

data class VideoEditorState(
    val projectId: String = "video_default",
    val projectName: String = "Black Devil Edit",
    val clips: List<VideoClip> = emptyList(),
    val selectedClipId: String? = null,
    val audioLaneClips: List<VideoAudioClip> = emptyList(),
    val selectedAudioClipId: String? = null,
    val isRecordingVideoVoiceover: Boolean = false,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val loopPlayback: Boolean = true,
    val aspectPreset: VideoAspectPreset = VideoAspectPreset.YOUTUBE_16_9,
    val cropZoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val blurredBgFill: Boolean = true,
    val bgFillMode: VideoBgFillMode = VideoBgFillMode.BLUR,
    val bgFillColorArgb: Int = 0xFF14070D.toInt(),
    val bgImageUri: String? = null,
    val colorGrade: VideoColorGrade = VideoColorGrade(),
    val pip: PipOverlayState = PipOverlayState(),
    val textLayers: List<VideoTextOverlay> = emptyList(),
    val selectedTextId: String? = null,
    val isListeningCaptions: Boolean = false,
    val exportResolution: VideoExportResolution = VideoExportResolution.FHD_1080P,
    val exportFps: VideoExportFps = VideoExportFps.FPS_30
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }.coerceAtLeast(1000L)

    val selectedClip: VideoClip?
        get() = clips.firstOrNull { it.id == selectedClipId } ?: clips.firstOrNull()

    val selectedAudioClip: VideoAudioClip?
        get() = audioLaneClips.firstOrNull { it.id == selectedAudioClipId } ?: audioLaneClips.firstOrNull()
}

// ==========================================
// WORKSPACE 2: A-TO-Z PHOTO & THUMBNAIL STUDIO
// ==========================================

enum class PhotoCanvasPreset(val label: String, val width: Int, val height: Int, val tag: String) {
    YOUTUBE_THUMB("YouTube", 1280, 720, "16:9"),
    STORY_9_16("Shorts", 1080, 1920, "9:16"),
    POST_1_1("Square", 1080, 1080, "1:1"),
    PORTRAIT_4_5("Portrait", 1080, 1350, "4:5"),
    CUSTOM("Custom", 1440, 1080, "Custom")
}

enum class CollageLayoutKind(val label: String) {
    SINGLE("Single"),
    SPLIT_2("2-Split"),
    GRID_3("3-Grid"),
    GRID_4("4-Grid")
}

enum class ToneCurvePreset(val label: String) {
    LINEAR("Normal"),
    S_CURVE("S-Curve"),
    MATTE("Matte"),
    PUNCHY("Punchy"),
    HIGH_KEY("Bright")
}

enum class LayerKind {
    IMAGE, SHAPE, TEXT, STICKER
}

enum class StudioShapeKind(val label: String) {
    ARROW("Arrow"),
    CIRCLE("Circle"),
    RECTANGLE("Rect"),
    STAR("Star"),
    BADGE("Badge")
}

data class EraserStrokePoint(
    val xNorm: Float,
    val yNorm: Float,
    val radiusNorm: Float,
    val softness: Float
)

data class SpotHealPoint(
    val xNorm: Float,
    val yNorm: Float,
    val radiusNorm: Float = 0.05f
)

data class PhotoLayer(
    val id: String,
    val name: String,
    val kind: LayerKind,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val isBackground: Boolean = false,
    val xNorm: Float = 0.5f,
    val yNorm: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val skewX: Float = 0f, // -0.45..0.45 perspective skew
    val skewY: Float = 0f,
    val opacity: Float = 1f,
    val blendMode: BlendModeKind = BlendModeKind.NORMAL,
    val doubleExposureAmount: Float = 0f, // 0..1 screen blend mix
    // Image fields
    val imageUri: String? = null,
    @DrawableRes val fallbackDrawable: Int? = null,
    val chromaEnabled: Boolean = false,
    val chromaColor: Int = 0xFF00FF00.toInt(),
    val chromaTolerance: Float = 0.36f,
    val eraserStrokes: List<EraserStrokePoint> = emptyList(),
    val spotHealPoints: List<SpotHealPoint> = emptyList(),
    val outlineGlowEnabled: Boolean = false,
    val outlineGlowColor: Int = 0xFFFF1E42.toInt(),
    val outlineGlowWidth: Float = 12f,
    // Shape fields
    val shapeKind: StudioShapeKind = StudioShapeKind.ARROW,
    val fillColor: Int = 0xFFFF1E42.toInt(),
    val strokeColor: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 6f,
    // Text fields
    val text: String = "BLACK DEVIL",
    val fontId: String = "bangers",
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val textUseGradient: Boolean = true,
    val textGradientEndColor: Int = 0xFFFF1E42.toInt(),
    val textStrokeColor: Int = 0xFF000000.toInt(),
    val textStrokeWidth: Float = 10f,
    val textShadowEnabled: Boolean = true,
    val text3dDepth: Float = 0f, // 0..16px 3D extruded bevel shadow
    val textCurveDeg: Float = 0f,
    // Sticker fields
    val stickerSymbol: String = "LIVE"
)

data class LightroomGrade(
    val brightness: Float = 0f,     // -1..1
    val contrast: Float = 1f,       // 0.4..2.0
    val highlights: Float = 0f,     // -1..1
    val shadows: Float = 0f,        // -1..1
    val whites: Float = 0f,         // -1..1
    val blacks: Float = 0f,         // -1..1
    val saturation: Float = 1f,     // 0..2.2
    val vibrance: Float = 0f,       // -1..1
    val warmth: Float = 0f,         // -1..1
    val tint: Float = 0f,           // -1..1 green/magenta
    val hueDeg: Float = 0f,         // -180..180
    val sharpness: Float = 0f,      // 0..1
    val clarity: Float = 0f,        // 0..1
    val blur: Float = 0f,           // 0..1
    val vignette: Float = 0.15f,    // 0..1
    val toneCurve: ToneCurvePreset = ToneCurvePreset.LINEAR,
    val hslRedShift: Float = 0f,    // -0.5..0.5
    val hslGreenShift: Float = 0f,  // -0.5..0.5
    val hslBlueShift: Float = 0f    // -0.5..0.5
)

enum class PhotoExportFormat(val label: String, val ext: String, val mime: String) {
    PNG_HD("Lossless PNG", "png", "image/png"),
    JPG_COMPRESSED("High-Quality JPG", "jpg", "image/jpeg")
}

data class PhotoStudioState(
    val projectId: String = "photo_default",
    val projectName: String = "Black Devil Thumbnail",
    val preset: PhotoCanvasPreset = PhotoCanvasPreset.YOUTUBE_THUMB,
    val customWidth: Int = 1440,
    val customHeight: Int = 1080,
    val collageLayout: CollageLayoutKind = CollageLayoutKind.SINGLE,
    val borderWidthPx: Float = 0f,
    val borderColorArgb: Int = 0xFFFF1E42.toInt(),
    val showSafeZoneGrid: Boolean = false,
    val layers: List<PhotoLayer> = emptyList(),
    val selectedLayerId: String? = null,
    val dslrBackgroundBlur: Float = 0.35f,
    val lightroom: LightroomGrade = LightroomGrade(),
    val eraserModeActive: Boolean = false,
    val restoreBrushActive: Boolean = false,
    val spotHealModeActive: Boolean = false,
    val eraserBrushSize: Float = 0.055f,
    val eraserSoftness: Float = 0.45f,
    val exportFormat: PhotoExportFormat = PhotoExportFormat.PNG_HD,
    val jpgQuality: Int = 92
) {
    val canvasWidth: Int
        get() = if (preset == PhotoCanvasPreset.CUSTOM) customWidth.coerceIn(320, 3840) else preset.width
    val canvasHeight: Int
        get() = if (preset == PhotoCanvasPreset.CUSTOM) customHeight.coerceIn(320, 3840) else preset.height
    val selectedLayer: PhotoLayer?
        get() = layers.firstOrNull { it.id == selectedLayerId }
}

// ==========================================
// WORKSPACE 3: A-TO-Z AUDIO & MUSIC STUDIO
// ==========================================

enum class AudioTrackRole(val label: String) {
    ORIGINAL_VIDEO("Dialogue"),
    BACKGROUND_MUSIC("Music"),
    VOICEOVER("Voiceover")
}

enum class VoiceChangerPreset(val label: String, val pitchSemi: Float, val speed: Float) {
    NORMAL("Normal", 0f, 1.0f),
    DEEP("Deep Boss", -5f, 0.96f),
    CHIPMUNK("High Pitch", 6f, 1.12f),
    ROBOT("Cyber Robot", -1f, 1.0f),
    RADIO("Studio Radio", 0f, 1.0f)
}

enum class AudioEqPreset(val label: String) {
    FLAT("Flat"),
    BASS_BOOST("Bass Boost"),
    VOCAL_POP("Vocal Clarity"),
    ECHO("Echo"),
    HALL_REVERB("Hall Reverb")
}

enum class AudioExportFormat(val label: String, val ext: String, val mime: String) {
    WAV("WAV Studio", "wav", "audio/wav"),
    MP3_COMPAT("MP3 / AAC", "m4a", "audio/mp4")
}

data class AudioTrackState(
    val role: AudioTrackRole,
    val sourceName: String,
    val filePath: String? = null,
    val volume: Float = 1.0f, // 0f..2f (0%..200%)
    val muted: Boolean = false,
    val fadeInSec: Float = 0.4f,
    val fadeOutSec: Float = 0.6f,
    val trimStartPct: Float = 0f,
    val trimEndPct: Float = 1f,
    val pitchSemitones: Float = 0f, // -8..+8
    val tempoSpeed: Float = 1.0f,   // 0.5..2.0
    val voicePreset: VoiceChangerPreset = VoiceChangerPreset.NORMAL,
    val eqPreset: AudioEqPreset = AudioEqPreset.FLAT,
    val vocalClarityEnabled: Boolean = false,
    val durationMs: Long = 0L,
    val waveform: List<Float> = emptyList()
)

enum class SfxKind(val id: String, val label: String, val tag: String, val accentHex: Long) {
    POP("pop", "Pop", "UI / Cut", 0xFFFF1E42),
    WHOOSH("whoosh", "Whoosh", "Transition", 0xFFC026D3),
    BASS_DROP("bass_drop", "Bass Drop", "Hype 808", 0xFFFF3B5C),
    BOOM("boom", "Boom", "Cinematic", 0xFFFFB300),
    HITMARKER("hitmarker", "Hitmarker", "Gaming", 0xFF10E88A),
    LASER("laser", "Laser", "Sci-Fi FX", 0xFF00E5FF),
    DING("ding", "Ding", "Highlight", 0xFFFDE047),
    RISER("riser", "Riser", "Build Up", 0xFFFF5E7E)
}

data class CopyrightCheckItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val checked: Boolean = false
)

data class AudioStudioState(
    val projectId: String = "audio_default",
    val projectName: String = "Black Devil Mix",
    val tracks: List<AudioTrackState> = emptyList(),
    val isPlayingMix: Boolean = false,
    val isRecordingMic: Boolean = false,
    val noiseReductionEnabled: Boolean = true,
    val recordingSeconds: Int = 0,
    val liveMicLevels: List<Float> = List(36) { 0.06f },
    val activeSfxId: String? = null,
    val copyrightChecklist: List<CopyrightCheckItem> = emptyList(),
    val audioClipSecondsCheck: Int = 12,
    val exportFormat: AudioExportFormat = AudioExportFormat.WAV
)

// ==========================================
// WORKSPACE 4: QUICK UTILITIES & VAULT
// ==========================================

enum class ImageConvertFormat(val label: String, val ext: String, val mime: String) {
    PNG("PNG", "png", "image/png"),
    JPG("JPG", "jpg", "image/jpeg"),
    WEBP("WEBP", "webp", "image/webp")
}

data class BatchImageEntry(
    val id: String,
    val name: String,
    val uriString: String,
    val originalBytes: Long,
    val compressedBytes: Long? = null,
    val exportedPath: String? = null
)

data class UtilitiesState(
    val batchImages: List<BatchImageEntry> = emptyList(),
    val outputFormat: ImageConvertFormat = ImageConvertFormat.WEBP,
    val quality: Int = 82,
    val isCompressing: Boolean = false,
    val gifSourceUri: String? = null,
    val gifSourceName: String = "Active Video Timeline",
    val gifFps: Int = 8,
    val gifFrameCount: Int = 12,
    val extractedFrameCount: Int = 0,
    val isProcessingGif: Boolean = false,
    val lastUtilityStatus: String? = null
)
