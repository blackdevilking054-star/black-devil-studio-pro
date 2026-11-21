package com.example.ui.workspaces

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ProjectEntity
import com.example.data.WorkspaceType
import com.example.engine.MediaExportStore
import com.example.model.ImageConvertFormat
import com.example.model.UtilitiesState
import com.example.ui.components.StudioActionPill
import com.example.ui.components.StudioChipButton
import com.example.ui.components.StudioSliderRow
import com.example.ui.components.StudioToolTabStrip
import com.example.ui.components.ToolTabItem
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.XonixStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class UtilityToolTab {
    PROJECTS, SHRINK_PHOTOS, MAKE_GIF
}

private enum class GifSliderMode(val label: String) {
    SPEED("Speed"),
    LENGTH("Frames")
}

@Composable
fun UtilitiesVaultWorkspace(
    state: UtilitiesState,
    projects: List<ProjectEntity>,
    viewModel: XonixStudioViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable { mutableStateOf(UtilityToolTab.PROJECTS) }
    var gifSliderMode by rememberSaveable { mutableStateOf(GifSliderMode.SPEED) }
    var snapshotName by remember { mutableStateOf("") }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addBatchImages(uris)
    }

    val gifVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.setGifSourceVideo(uri)
    }

    val tabs = remember {
        listOf(
            ToolTabItem(UtilityToolTab.PROJECTS, "Projects", Icons.Default.FolderSpecial),
            ToolTabItem(UtilityToolTab.SHRINK_PHOTOS, "Shrink", Icons.Default.Compress),
            ToolTabItem(UtilityToolTab.MAKE_GIF, "GIF", Icons.Default.Gif)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                UtilityToolTab.PROJECTS -> {
                    Surface(
                        color = StudioSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Saved Projects", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(
                                "Your edits save automatically so you can pick up right where you left off.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = snapshotName,
                                    onValueChange = { snapshotName = it },
                                    placeholder = { Text("Name this save (optional)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = StudioBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.saveCurrentWorkspaceSnapshot(snapshotName.trim().ifBlank { "My Edit" })
                                        snapshotName = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("vault_save_snapshot_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save Project")
                                }
                            }
                        }
                    }

                    if (projects.isEmpty()) {
                        Surface(
                            color = StudioSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(34.dp))
                                Text("No Saved Projects Yet", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Your Video, Photo, and Music edits will show up here.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    } else {
                        val dateFmt = remember { SimpleDateFormat("MMM dd · HH:mm", Locale.US) }
                        projects.forEach { proj ->
                            val icon = when (proj.workspaceType) {
                                WorkspaceType.VIDEO.name -> Icons.Default.Movie
                                WorkspaceType.PHOTO.name -> Icons.Default.Photo
                                else -> Icons.Default.GraphicEq
                            }
                            Surface(
                                color = StudioSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = ElectricCyanContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(proj.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${proj.resolutionLabel} · ${dateFmt.format(Date(proj.updatedAt))}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                        Text(proj.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(
                                        onClick = { viewModel.resumeVaultProject(proj) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Open ${proj.name}", tint = EmeraldSafe)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteVaultProject(proj.id) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete ${proj.name}", tint = SignalRed)
                                    }
                                }
                            }
                        }
                    }
                }

                UtilityToolTab.SHRINK_PHOTOS -> {
                    Surface(
                        color = StudioSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Shrink & Convert Photos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(
                                "Make photos smaller or switch between PNG, JPG, and WEBP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ImageConvertFormat.entries.forEach { fmt ->
                                    StudioChipButton(
                                        text = fmt.label,
                                        selected = state.outputFormat == fmt,
                                        onClick = { viewModel.setBatchOutputFormat(fmt) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (state.outputFormat != ImageConvertFormat.PNG) {
                                StudioSliderRow(
                                    label = "Quality",
                                    value = state.quality.toFloat(),
                                    valueRange = 20f..100f,
                                    displayValue = "${state.quality}%"
                                ) { viewModel.setBatchQuality(it.roundToInt()) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudioActionPill(
                                    label = "Pick Photos",
                                    icon = Icons.Default.AddPhotoAlternate,
                                    onClick = {
                                        multiPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.batchImages.isNotEmpty()) {
                                    StudioActionPill(
                                        label = "Clear",
                                        icon = Icons.Default.Delete,
                                        accent = SignalRed,
                                        onClick = { viewModel.clearBatchImages() }
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.runBatchImageCompression() },
                                enabled = !state.isCompressing && state.batchImages.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("utility_batch_compress_button")
                            ) {
                                Icon(Icons.Default.Compress, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (state.isCompressing) "Saving..." else "Save ${state.batchImages.size} Photo(s)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    state.batchImages.forEach { item ->
                        Surface(
                            color = StudioSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (item.compressedBytes != null) EmeraldSafe.copy(alpha = 0.6f) else StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val orig = MediaExportStore.formatSize(item.originalBytes)
                                    val after = item.compressedBytes?.let { MediaExportStore.formatSize(it) }
                                    Text(
                                        text = if (after != null) "$orig -> $after" else "Size: $orig",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (after != null) EmeraldSafe else TextSecondary
                                    )
                                }
                                Icon(Icons.Default.Photo, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                UtilityToolTab.MAKE_GIF -> {
                    Surface(
                        color = StudioSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Video to GIF & Photos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Surface(
                                color = StudioSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Video", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text(state.gifSourceName, style = MaterialTheme.typography.titleSmall, color = ElectricCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    StudioActionPill(
                                        label = "Pick Video",
                                        icon = Icons.Default.VideoFile,
                                        onClick = {
                                            gifVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                        }
                                    )
                                }
                            }

                            // ONLY 1 slider at a time
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GifSliderMode.entries.forEach { m ->
                                    StudioChipButton(
                                        text = m.label,
                                        selected = gifSliderMode == m,
                                        onClick = { gifSliderMode = m }
                                    )
                                }
                            }
                            when (gifSliderMode) {
                                GifSliderMode.SPEED -> StudioSliderRow("GIF Speed", state.gifFps.toFloat(), 4f..15f, "${state.gifFps}x") {
                                    viewModel.setGifParams(fps = it.roundToInt())
                                }
                                GifSliderMode.LENGTH -> StudioSliderRow("Frames", state.gifFrameCount.toFloat(), 4f..24f, "${state.gifFrameCount}") {
                                    viewModel.setGifParams(frames = it.roundToInt())
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.convertVideoToGif() },
                                    enabled = !state.isProcessingGif,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("utility_export_gif_button")
                                ) {
                                    Icon(Icons.Default.Gif, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save GIF", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.extractVideoFramesBatch() },
                                    enabled = !state.isProcessingGif,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("utility_extract_frames_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save Photos", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!state.lastUtilityStatus.isNullOrBlank()) {
                                Text(state.lastUtilityStatus, style = MaterialTheme.typography.labelSmall, color = EmeraldSafe)
                            }
                        }
                    }
                }
            }
        }

        // Single horizontal row of big icons at the bottom
        StudioToolTabStrip(
            tabs = tabs,
            selected = activeTab,
            onSelect = { activeTab = it }
        )
    }
}
