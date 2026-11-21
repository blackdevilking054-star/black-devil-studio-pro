package com.example.ui.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.StickerCatalogItem
import com.example.engine.VideoStickerCatalog
import com.example.model.StickerAnimationStyle
import com.example.model.StickerPackCategory
import com.example.model.TextAnimationStyle
import com.example.model.VideoTextOverlay
import com.example.ui.components.StudioActionPill
import com.example.ui.components.StudioChipButton
import com.example.ui.components.StudioColorSwatchRow
import com.example.ui.components.StudioSliderRow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ProAmber
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioFonts
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

private enum class ProTextSection(val label: String) {
    FONT("Font"),
    COLOR_STYLE("Color & Style"),
    ANIMATE("Animations"),
    TRANSFORM("Transform")
}

private enum class ProStickerSection(val label: String) {
    TRANSFORM("Edit & Crop"),
    ANIMATE("Animations"),
    PICKER("Sticker Pack")
}

@Composable
internal fun ProTextEditingToolbar(
    textLayers: List<VideoTextOverlay>,
    selectedTextId: String?,
    isListeningCaptions: Boolean,
    onAddText: () -> Unit,
    onOpenStickersTab: () -> Unit,
    onLaunchAutoCaptions: () -> Unit,
    onSelectText: (String) -> Unit,
    onUpdateSelectedText: ((VideoTextOverlay) -> VideoTextOverlay) -> Unit,
    onDuplicateText: (String) -> Unit,
    onDeleteText: (String) -> Unit
) {
    var activeSection by rememberSaveable { mutableStateOf(ProTextSection.FONT) }
    val onlyTextLayers = textLayers.filter { !it.isSticker }
    val selText = onlyTextLayers.firstOrNull { it.id == selectedTextId }
        ?: onlyTextLayers.firstOrNull()

    // Quick creation strip
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StudioActionPill(
            label = "+ Add Text",
            icon = Icons.Default.Title,
            accent = ElectricCyan,
            onClick = onAddText,
            modifier = Modifier.testTag("pro_text_add_button")
        )
        StudioActionPill(
            label = "Stickers",
            icon = Icons.Default.Photo,
            accent = ProAmber,
            onClick = onOpenStickersTab,
            modifier = Modifier.testTag("pro_text_open_stickers_button")
        )
        StudioActionPill(
            label = if (isListeningCaptions) "Listening..." else "Auto Captions",
            icon = Icons.Default.ClosedCaption,
            accent = NeonViolet,
            onClick = onLaunchAutoCaptions,
            modifier = Modifier.testTag("pro_text_auto_captions_button")
        )
    }

    // Multi-text layer chips
    if (onlyTextLayers.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            onlyTextLayers.forEach { layer ->
                StudioChipButton(
                    text = layer.text.take(14).ifBlank { "Text" },
                    selected = layer.id == selText?.id,
                    onClick = { onSelectText(layer.id) }
                )
            }
        }
    }

    if (selText == null) {
        Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Tap + Add Text above to create a styled title, neon label, or animated caption.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(12.dp)
            )
        }
        return
    }

    // Inline Text Field + Duplicate + Delete
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selText.text,
            onValueChange = { txt -> onUpdateSelectedText { it.copy(text = txt) } },
            placeholder = { Text("Enter text", color = TextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = StudioBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("pro_text_inline_input")
        )
        IconButton(
            onClick = { onDuplicateText(selText.id) },
            modifier = Modifier.testTag("pro_text_duplicate_icon")
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Duplicate Text",
                tint = ElectricCyan
            )
        }
        IconButton(
            onClick = { onDeleteText(selText.id) },
            modifier = Modifier.testTag("video_text_delete_button")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Text",
                tint = SignalRed
            )
        }
    }

    // Dedicated Scrollable Pro Text Toolbar Sub-Tabs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProTextSection.entries.forEach { sec ->
            StudioChipButton(
                text = sec.label,
                selected = activeSection == sec,
                onClick = { activeSection = sec },
                modifier = Modifier.testTag("pro_text_tab_${sec.name.lowercase()}")
            )
        }
    }

    when (activeSection) {
        ProTextSection.FONT -> {
            Text(
                text = "Featured Pro Fonts",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudioFonts.proVideoPresets.forEach { f ->
                    val selected = selText.fontId == f.id
                    Surface(
                        onClick = { onUpdateSelectedText { it.copy(fontId = f.id) } },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) ElectricCyanContainer else StudioSurfaceElevated,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) ElectricCyan else StudioBorder
                        ),
                        modifier = Modifier.testTag("pro_font_${f.id}")
                    ) {
                        Text(
                            text = f.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = f.fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = if (selected) ElectricCyan else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }
            Text(
                text = "More Studio Fonts",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudioFonts.all.forEach { f ->
                    StudioChipButton(
                        text = f.displayName,
                        selected = selText.fontId == f.id,
                        onClick = { onUpdateSelectedText { it.copy(fontId = f.id) } }
                    )
                }
            }
        }

        ProTextSection.COLOR_STYLE -> {
            Text(
                text = "Text Fill Color",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            StudioColorSwatchRow(
                selectedArgb = selText.colorArgb,
                onSelect = { c -> onUpdateSelectedText { it.copy(colorArgb = c) } }
            )

            StudioSliderRow(
                label = "Outline / Stroke Width",
                value = selText.strokeWidth,
                valueRange = 0f..18f,
                displayValue = "${selText.strokeWidth.roundToInt()}px"
            ) { w ->
                onUpdateSelectedText { it.copy(strokeWidth = w) }
            }

            Text(
                text = "Outline Color",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            StudioColorSwatchRow(
                selectedArgb = selText.strokeArgb,
                onSelect = { c -> onUpdateSelectedText { it.copy(strokeArgb = c) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Drop Shadow", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Switch(
                        checked = selText.shadowEnabled,
                        onCheckedChange = { sh -> onUpdateSelectedText { it.copy(shadowEnabled = sh) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonViolet),
                        modifier = Modifier.testTag("pro_text_shadow_switch")
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Label Pill", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Switch(
                        checked = selText.bgEnabled,
                        onCheckedChange = { b -> onUpdateSelectedText { it.copy(bgEnabled = b) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricCyan),
                        modifier = Modifier.testTag("pro_text_bg_pill_switch")
                    )
                }
            }

            if (selText.bgEnabled) {
                Text(
                    text = "Label Pill Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                StudioColorSwatchRow(
                    selectedArgb = selText.bgColorArgb,
                    onSelect = { c -> onUpdateSelectedText { it.copy(bgColorArgb = c) } }
                )
            }
        }

        ProTextSection.ANIMATE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextAnimationStyle.entries.forEach { anim ->
                    StudioChipButton(
                        text = anim.label,
                        selected = selText.animation == anim,
                        onClick = { onUpdateSelectedText { it.copy(animation = anim) } },
                        modifier = Modifier.testTag("pro_text_anim_${anim.name.lowercase()}")
                    )
                }
            }
            StudioSliderRow(
                label = "Animation Speed",
                value = selText.animationSpeed,
                valueRange = 0.3f..2.5f,
                displayValue = String.format(Locale.US, "%.1fx", selText.animationSpeed)
            ) { spd ->
                onUpdateSelectedText { it.copy(animationSpeed = spd) }
            }
        }

        ProTextSection.TRANSFORM -> {
            StudioSliderRow(
                label = "Zoom / Scale",
                value = selText.scale,
                valueRange = 0.3f..3.0f,
                displayValue = "${(selText.scale * 100).roundToInt()}%"
            ) { sc ->
                onUpdateSelectedText { it.copy(scale = sc) }
            }
            StudioSliderRow(
                label = "Rotate",
                value = selText.rotationDeg,
                valueRange = -180f..180f,
                displayValue = "${selText.rotationDeg.roundToInt()}°"
            ) { rot ->
                onUpdateSelectedText { it.copy(rotationDeg = rot) }
            }
            StudioSliderRow(
                label = "Opacity",
                value = selText.opacity,
                valueRange = 0.1f..1f,
                displayValue = "${(selText.opacity * 100).roundToInt()}%"
            ) { op ->
                onUpdateSelectedText { it.copy(opacity = op) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudioActionPill(
                    label = "Duplicate",
                    icon = Icons.Default.ContentCopy,
                    accent = ElectricCyan,
                    onClick = { onDuplicateText(selText.id) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pro_text_duplicate_button")
                )
                StudioActionPill(
                    label = "Delete",
                    icon = Icons.Default.Delete,
                    accent = SignalRed,
                    onClick = { onDeleteText(selText.id) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pro_text_cancel_delete_button")
                )
            }
        }
    }
}

@Composable
internal fun ProStickerEditingToolbar(
    stickerLayers: List<VideoTextOverlay>,
    selectedTextId: String?,
    isCropping: Boolean,
    onAddCatalogSticker: (StickerCatalogItem) -> Unit,
    onImportCustomImageSticker: () -> Unit,
    onSelectSticker: (String) -> Unit,
    onUpdateSelectedSticker: ((VideoTextOverlay) -> VideoTextOverlay) -> Unit,
    onStartCropMode: () -> Unit,
    onDuplicateSticker: (String) -> Unit,
    onDeleteSticker: (String) -> Unit
) {
    val selSticker = stickerLayers.firstOrNull { it.id == selectedTextId }
    var activeSection by rememberSaveable(selSticker?.id) {
        mutableStateOf(if (selSticker != null) ProStickerSection.TRANSFORM else ProStickerSection.PICKER)
    }
    var activeCategory by rememberSaveable { mutableStateOf(StickerPackCategory.BADGES) }

    // Top row: Sub-tabs + Custom Photo Sticker Import
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioActionPill(
            label = "+ Import Image",
            icon = Icons.Default.AddPhotoAlternate,
            accent = EmeraldSafe,
            onClick = onImportCustomImageSticker,
            modifier = Modifier.testTag("pro_sticker_import_photo_button")
        )
        if (selSticker != null) {
            ProStickerSection.entries.forEach { sec ->
                StudioChipButton(
                    text = sec.label,
                    selected = activeSection == sec,
                    onClick = { activeSection = sec },
                    modifier = Modifier.testTag("pro_sticker_tab_${sec.name.lowercase()}")
                )
            }
        }
    }

    // Active sticker selector strip if multiple stickers exist
    if (stickerLayers.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stickerLayers.forEach { stk ->
                StudioChipButton(
                    text = stk.text.take(12).ifBlank { "Sticker" },
                    selected = stk.id == selSticker?.id,
                    onClick = { onSelectSticker(stk.id) }
                )
            }
        }
    }

    val effectiveSection = if (selSticker == null) ProStickerSection.PICKER else activeSection

    when (effectiveSection) {
        ProStickerSection.PICKER -> {
            // 7 Sticker Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StickerPackCategory.entries.forEach { cat ->
                    StudioChipButton(
                        text = cat.label,
                        selected = activeCategory == cat,
                        onClick = { activeCategory = cat },
                        modifier = Modifier.testTag("sticker_cat_${cat.name.lowercase()}")
                    )
                }
            }

            // Sticker Items Horizontal Shelf
            val items = VideoStickerCatalog.byCategory(activeCategory)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    val swatchColor = Color(item.primaryColorArgb)
                    Surface(
                        onClick = { onAddCatalogSticker(item) },
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (item.badgeId != null) swatchColor.copy(alpha = 0.7f) else StudioBorder
                        ),
                        modifier = Modifier.testTag("sticker_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.previewGlyph,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (item.badgeId == null) 22.sp else 13.sp
                                ),
                                color = if (item.badgeId != null) swatchColor else TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        ProStickerSection.TRANSFORM -> {
            if (selSticker != null) {
                // Quick Action Row: Crop, Flip H, Flip V, Duplicate, Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioActionPill(
                        label = if (isCropping) "Cropping..." else "Crop",
                        icon = Icons.Default.Crop,
                        accent = if (isCropping) EmeraldSafe else ProAmber,
                        onClick = onStartCropMode,
                        modifier = Modifier.testTag("pro_sticker_crop_button")
                    )
                    StudioActionPill(
                        label = "Flip H",
                        icon = Icons.Default.Flip,
                        accent = if (selSticker.flipH) EmeraldSafe else ElectricCyan,
                        onClick = { onUpdateSelectedSticker { it.copy(flipH = !it.flipH) } },
                        modifier = Modifier.testTag("pro_sticker_flip_h_button")
                    )
                    StudioActionPill(
                        label = "Flip V",
                        icon = Icons.Default.Flip,
                        accent = if (selSticker.flipV) EmeraldSafe else ElectricCyan,
                        onClick = { onUpdateSelectedSticker { it.copy(flipV = !it.flipV) } },
                        modifier = Modifier.testTag("pro_sticker_flip_v_button")
                    )
                    StudioActionPill(
                        label = "Duplicate",
                        icon = Icons.Default.ContentCopy,
                        accent = NeonViolet,
                        onClick = { onDuplicateSticker(selSticker.id) },
                        modifier = Modifier.testTag("pro_sticker_duplicate_button")
                    )
                    StudioActionPill(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        accent = SignalRed,
                        onClick = { onDeleteSticker(selSticker.id) },
                        modifier = Modifier.testTag("pro_sticker_delete_button")
                    )
                }

                StudioSliderRow(
                    label = "Zoom / Scale",
                    value = selSticker.scale,
                    valueRange = 0.25f..3.0f,
                    displayValue = "${(selSticker.scale * 100).roundToInt()}%"
                ) { sc ->
                    onUpdateSelectedSticker { it.copy(scale = sc) }
                }
                StudioSliderRow(
                    label = "Rotate",
                    value = selSticker.rotationDeg,
                    valueRange = -180f..180f,
                    displayValue = "${selSticker.rotationDeg.roundToInt()}°"
                ) { rot ->
                    onUpdateSelectedSticker { it.copy(rotationDeg = rot) }
                }
                StudioSliderRow(
                    label = "Opacity",
                    value = selSticker.opacity,
                    valueRange = 0.1f..1f,
                    displayValue = "${(selSticker.opacity * 100).roundToInt()}%"
                ) { op ->
                    onUpdateSelectedSticker { it.copy(opacity = op) }
                }
            }
        }

        ProStickerSection.ANIMATE -> {
            if (selSticker != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StickerAnimationStyle.entries.forEach { anim ->
                        StudioChipButton(
                            text = anim.label,
                            selected = selSticker.stickerAnimation == anim,
                            onClick = { onUpdateSelectedSticker { it.copy(stickerAnimation = anim) } },
                            modifier = Modifier.testTag("pro_sticker_anim_${anim.name.lowercase()}")
                        )
                    }
                }
                StudioSliderRow(
                    label = "Animation Speed",
                    value = selSticker.animationSpeed,
                    valueRange = 0.3f..2.5f,
                    displayValue = String.format(Locale.US, "%.1fx", selSticker.animationSpeed)
                ) { spd ->
                    onUpdateSelectedSticker { it.copy(animationSpeed = spd) }
                }
            }
        }
    }
}
