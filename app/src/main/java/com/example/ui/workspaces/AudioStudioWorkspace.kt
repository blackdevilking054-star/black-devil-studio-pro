package com.example.ui.workspaces

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AudioEqPreset
import com.example.model.AudioExportFormat
import com.example.model.AudioStudioState
import com.example.model.AudioTrackRole
import com.example.model.SfxKind
import com.example.model.VoiceChangerPreset
import com.example.ui.components.StudioActionPill
import com.example.ui.components.StudioChipButton
import com.example.ui.components.StudioSliderRow
import com.example.ui.components.StudioToolTabStrip
import com.example.ui.components.ToolTabItem
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EmeraldSafeContainer
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ProAmber
import com.example.ui.theme.ProAmberContainer
import com.example.ui.theme.SignalRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.XonixStudioViewModel
import java.util.Locale
import kotlin.math.roundToInt

private enum class AudioToolTab {
    MIXER, EFFECTS, VOICE, SOUNDS, SAFE_MUSIC
}

private enum class TrackSliderMode(val label: String) {
    VOLUME("Volume"),
    FADE_IN("Fade In"),
    FADE_OUT("Fade Out"),
    TRIM_START("Start"),
    TRIM_END("End")
}

private enum class AudioFxSliderMode(val label: String) {
    PITCH("Pitch"),
    TEMPO("Tempo")
}

@Composable
fun AudioStudioWorkspace(
    state: AudioStudioState,
    viewModel: XonixStudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by rememberSaveable { mutableStateOf(AudioToolTab.MIXER) }
    var selectedRole by rememberSaveable { mutableStateOf(AudioTrackRole.ORIGINAL_VIDEO) }
    var sliderMode by rememberSaveable { mutableStateOf(TrackSliderMode.VOLUME) }
    var fxSliderMode by rememberSaveable { mutableStateOf(AudioFxSliderMode.PITCH) }

    val extractFromVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.extractAudioFromVideoUri(uri, AudioTrackRole.ORIGINAL_VIDEO)
    }

    val importMusicPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.extractAudioFromVideoUri(uri, AudioTrackRole.BACKGROUND_MUSIC)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceoverRecording()
        } else {
            viewModel.showToast("Allow microphone access to record your voice")
        }
    }

    val tabs = remember {
        listOf(
            ToolTabItem(AudioToolTab.MIXER, "Mixer", Icons.Default.GraphicEq),
            ToolTabItem(AudioToolTab.EFFECTS, "FX & EQ", Icons.Default.Equalizer),
            ToolTabItem(AudioToolTab.VOICE, "Voice", Icons.Default.Mic),
            ToolTabItem(AudioToolTab.SOUNDS, "SFX Pad", Icons.Default.MusicNote),
            ToolTabItem(AudioToolTab.SAFE_MUSIC, "Safe Check", Icons.Default.Security)
        )
    }

    val activeTrack = state.tracks.firstOrNull { it.role == selectedRole } ?: state.tracks.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // =========================================================================
        // 1. TOP CLEAN AUDIO STAGE WITH PLAY / PAUSE + LIVE WAVEFORM
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.36f)
                .background(OledBlack)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = DeepObsidian,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, StudioBorder.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isRecordingMic) "Recording Voice..." else (activeTrack?.role?.label ?: "Music Mix"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (state.isRecordingMic) SignalRed else ElectricCyan
                            )
                            Text(
                                text = activeTrack?.let { t ->
                                    val fxTag = buildString {
                                        if (t.voicePreset != VoiceChangerPreset.NORMAL) append(" • ${t.voicePreset.label}")
                                        if (t.eqPreset != AudioEqPreset.FLAT) append(" • ${t.eqPreset.label}")
                                    }
                                    "${t.sourceName}$fxTag"
                                } ?: "Ready to play",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            onClick = { viewModel.toggleAudioMixPlayback() },
                            shape = CircleShape,
                            color = if (state.isPlayingMix) SignalRed else ElectricCyan,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("audio_master_play_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPlayingMix) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlayingMix) "Stop" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    WaveformBarsCanvas(
                        bars = if (state.isRecordingMic) state.liveMicLevels else (activeTrack?.waveform?.ifEmpty { List(36) { 0.1f } } ?: List(36) { 0.1f }),
                        barColor = when {
                            state.isRecordingMic -> SignalRed
                            activeTrack?.muted == true -> TextSecondary.copy(alpha = 0.3f)
                            selectedRole == AudioTrackRole.BACKGROUND_MUSIC -> NeonViolet
                            selectedRole == AudioTrackRole.VOICEOVER -> EmeraldSafe
                            else -> ElectricCyan
                        },
                        trimStartPct = if (state.isRecordingMic) 0f else (activeTrack?.trimStartPct ?: 0f),
                        trimEndPct = if (state.isRecordingMic) 1f else (activeTrack?.trimEndPct ?: 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                    )
                }
            }
        }

        // =========================================================================
        // 2. FOCUSED CONTROL PANEL (1 SLIDER AT A TIME) + BOTTOM ICON ROW
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(0.5.dp, StudioBorder.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.64f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        AudioToolTab.MIXER -> {
                            // 3-Track Selector Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AudioTrackRole.entries.forEach { role ->
                                    StudioChipButton(
                                        text = role.label,
                                        selected = selectedRole == role,
                                        onClick = { selectedRole = role },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (activeTrack != null) {
                                // Track Action Row (Import, Split, Merge, Mute)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StudioActionPill(
                                        label = when (activeTrack.role) {
                                            AudioTrackRole.ORIGINAL_VIDEO -> "Extract Video"
                                            AudioTrackRole.BACKGROUND_MUSIC -> "Import Audio"
                                            AudioTrackRole.VOICEOVER -> "Record Voice"
                                        },
                                        icon = when (activeTrack.role) {
                                            AudioTrackRole.ORIGINAL_VIDEO -> Icons.Default.VideoFile
                                            AudioTrackRole.BACKGROUND_MUSIC -> Icons.Default.MusicNote
                                            AudioTrackRole.VOICEOVER -> Icons.Default.Mic
                                        },
                                        onClick = {
                                            when (activeTrack.role) {
                                                AudioTrackRole.ORIGINAL_VIDEO -> extractFromVideoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                                AudioTrackRole.BACKGROUND_MUSIC -> importMusicPicker.launch(arrayOf("audio/*"))
                                                AudioTrackRole.VOICEOVER -> activeTab = AudioToolTab.VOICE
                                            }
                                        }
                                    )
                                    StudioActionPill(
                                        label = "Split",
                                        icon = Icons.Default.ContentCut,
                                        onClick = { viewModel.splitAudioTrack(activeTrack.role, 0.5f) }
                                    )
                                    val otherRole = if (activeTrack.role == AudioTrackRole.ORIGINAL_VIDEO) {
                                        AudioTrackRole.BACKGROUND_MUSIC
                                    } else {
                                        AudioTrackRole.ORIGINAL_VIDEO
                                    }
                                    StudioActionPill(
                                        label = "Join",
                                        icon = Icons.AutoMirrored.Filled.CallMerge,
                                        accent = NeonViolet,
                                        onClick = { viewModel.mergeAudioTracks(activeTrack.role, otherRole) }
                                    )
                                    Surface(
                                        onClick = { viewModel.updateAudioTrack(activeTrack.role) { it.copy(muted = !it.muted) } },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (activeTrack.muted) SignalRed.copy(alpha = 0.2f) else StudioSurfaceElevated,
                                        border = BorderStroke(1.dp, if (activeTrack.muted) SignalRed else StudioBorder),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (activeTrack.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Mute",
                                                tint = if (activeTrack.muted) SignalRed else ElectricCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (activeTrack.muted) "Muted" else "On",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }

                                // 1 Focused Slider at a Time
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TrackSliderMode.entries.forEach { m ->
                                        StudioChipButton(
                                            text = m.label,
                                            selected = sliderMode == m,
                                            onClick = { sliderMode = m }
                                        )
                                    }
                                }

                                when (sliderMode) {
                                    TrackSliderMode.VOLUME -> StudioSliderRow(
                                        label = "Volume Boost",
                                        value = activeTrack.volume,
                                        valueRange = 0f..2.0f,
                                        displayValue = "${(activeTrack.volume * 100).roundToInt()}%"
                                    ) { v -> viewModel.updateAudioTrack(activeTrack.role) { it.copy(volume = v) } }

                                    TrackSliderMode.FADE_IN -> StudioSliderRow(
                                        label = "Fade In",
                                        value = activeTrack.fadeInSec,
                                        valueRange = 0f..3f,
                                        displayValue = String.format(Locale.US, "%.1fs", activeTrack.fadeInSec)
                                    ) { f -> viewModel.updateAudioTrack(activeTrack.role) { it.copy(fadeInSec = f) } }

                                    TrackSliderMode.FADE_OUT -> StudioSliderRow(
                                        label = "Fade Out",
                                        value = activeTrack.fadeOutSec,
                                        valueRange = 0f..3f,
                                        displayValue = String.format(Locale.US, "%.1fs", activeTrack.fadeOutSec)
                                    ) { f -> viewModel.updateAudioTrack(activeTrack.role) { it.copy(fadeOutSec = f) } }

                                    TrackSliderMode.TRIM_START -> StudioSliderRow(
                                        label = "Trim Start",
                                        value = activeTrack.trimStartPct,
                                        valueRange = 0f..0.85f,
                                        displayValue = "${(activeTrack.trimStartPct * 100).roundToInt()}%"
                                    ) { s ->
                                        viewModel.updateAudioTrack(activeTrack.role) {
                                            it.copy(trimStartPct = s.coerceAtMost(it.trimEndPct - 0.1f))
                                        }
                                    }

                                    TrackSliderMode.TRIM_END -> StudioSliderRow(
                                        label = "Trim End",
                                        value = activeTrack.trimEndPct,
                                        valueRange = 0.15f..1f,
                                        displayValue = "${(activeTrack.trimEndPct * 100).roundToInt()}%"
                                    ) { e ->
                                        viewModel.updateAudioTrack(activeTrack.role) {
                                            it.copy(trimEndPct = e.coerceAtLeast(it.trimStartPct + 0.1f))
                                        }
                                    }
                                }
                            }

                            // Export Format Selector (WAV / MP3-AAC)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AudioExportFormat.entries.forEach { fmt ->
                                    StudioChipButton(
                                        text = fmt.label,
                                        selected = state.exportFormat == fmt,
                                        onClick = { viewModel.setAudioExportFormat(fmt) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.exportAudioMixWav() },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("audio_export_wav_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save ${state.exportFormat.label}", fontWeight = FontWeight.Bold)
                            }
                        }

                        AudioToolTab.EFFECTS -> {
                            // Track selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AudioTrackRole.entries.forEach { role ->
                                    StudioChipButton(
                                        text = role.label,
                                        selected = selectedRole == role,
                                        onClick = { selectedRole = role },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (activeTrack != null) {
                                Text("Voice Changer", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VoiceChangerPreset.entries.forEach { preset ->
                                        StudioChipButton(
                                            text = preset.label,
                                            selected = activeTrack.voicePreset == preset,
                                            onClick = { viewModel.setAudioVoicePreset(activeTrack.role, preset) }
                                        )
                                    }
                                }

                                Text("Echo, Reverb & EQ", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AudioEqPreset.entries.forEach { eq ->
                                        StudioChipButton(
                                            text = eq.label,
                                            selected = activeTrack.eqPreset == eq,
                                            onClick = { viewModel.setAudioEqPreset(activeTrack.role, eq) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Vocal Clarity Enhancer", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Switch(
                                        checked = activeTrack.vocalClarityEnabled,
                                        onCheckedChange = { en -> viewModel.updateAudioTrack(activeTrack.role) { it.copy(vocalClarityEnabled = en) } },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSafe)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AudioFxSliderMode.entries.forEach { m ->
                                        StudioChipButton(
                                            text = m.label,
                                            selected = fxSliderMode == m,
                                            onClick = { fxSliderMode = m }
                                        )
                                    }
                                }

                                when (fxSliderMode) {
                                    AudioFxSliderMode.PITCH -> StudioSliderRow(
                                        label = "Pitch Shift",
                                        value = activeTrack.pitchSemitones,
                                        valueRange = -8f..8f,
                                        displayValue = "${if (activeTrack.pitchSemitones >= 0) "+" else ""}${activeTrack.pitchSemitones.roundToInt()} st"
                                    ) { p -> viewModel.updateAudioTrack(activeTrack.role) { it.copy(pitchSemitones = p) } }

                                    AudioFxSliderMode.TEMPO -> StudioSliderRow(
                                        label = "Tempo Speed",
                                        value = activeTrack.tempoSpeed,
                                        valueRange = 0.5f..2.0f,
                                        displayValue = String.format(Locale.US, "%.2fx", activeTrack.tempoSpeed)
                                    ) { sp -> viewModel.updateAudioTrack(activeTrack.role) { it.copy(tempoSpeed = sp) } }
                                }
                            }
                        }

                        AudioToolTab.VOICE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Noise Reduction & Clean Voice", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("Reduces background noise automatically", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Switch(
                                    checked = state.noiseReductionEnabled,
                                    onCheckedChange = { viewModel.toggleNoiseReduction() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSafe)
                                )
                            }

                            Text(
                                text = String.format(Locale.US, "%02d:%02d", state.recordingSeconds / 60, state.recordingSeconds % 60),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (state.isRecordingMic) SignalRed else ElectricCyan,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Button(
                                onClick = {
                                    if (state.isRecordingMic) {
                                        viewModel.stopVoiceoverRecording()
                                    } else {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            viewModel.startVoiceoverRecording()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isRecordingMic) SignalRed else ElectricCyan,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("audio_record_mic_button")
                            ) {
                                Icon(
                                    imageVector = if (state.isRecordingMic) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (state.isRecordingMic) "Stop Recording" else "Record Studio Voice",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AudioToolTab.SOUNDS -> {
                            Text("8 Royalty-Free Sound Effects — Tap to play or save", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            SfxKind.entries.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { sfx ->
                                        val isPlaying = state.activeSfxId == sfx.id
                                        val accent = Color(sfx.accentHex)
                                        Surface(
                                            onClick = { viewModel.playSfx(sfx) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isPlaying) accent.copy(alpha = 0.22f) else StudioSurfaceElevated,
                                            border = BorderStroke(if (isPlaying) 1.5.dp else 1.dp, if (isPlaying) accent else StudioBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(72.dp)
                                                .testTag("sfx_pad_${sfx.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(sfx.label, style = MaterialTheme.typography.titleSmall, color = if (isPlaying) accent else TextPrimary, maxLines = 1)
                                                    Text(sfx.tag, style = MaterialTheme.typography.labelSmall, color = accent)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.downloadSfxWav(sfx) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = "Save ${sfx.label}", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        AudioToolTab.SAFE_MUSIC -> {
                            Text("Free Copyright-Safe Music", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            val libraries = listOf(
                                Triple("YouTube Audio Library", "Free music safe for YouTube", "https://studio.youtube.com/channel/UC/music"),
                                Triple("Pixabay Music", "Free music for videos & shorts", "https://pixabay.com/music/"),
                                Triple("NCS Music", "Free upbeat creator music", "https://ncs.io/music")
                            )
                            libraries.forEach { (title, sub, url) ->
                                Surface(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }.onFailure {
                                            viewModel.showToast("Could not open $title")
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = StudioSurfaceElevated,
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
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open $title", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            val checkedCount = state.copyrightChecklist.count { it.checked }
                            val scorePct = (checkedCount * 100) / state.copyrightChecklist.size.coerceAtLeast(1)
                            val isSafe = scorePct >= 80
                            Surface(
                                color = if (isSafe) EmeraldSafeContainer else ProAmberContainer,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isSafe) EmeraldSafe else ProAmber),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Copyright Safety", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = if (isSafe) "Safe ($scorePct%)" else "Check ($scorePct%)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSafe) EmeraldSafe else ProAmber
                                        )
                                    }
                                    StudioSliderRow(
                                        label = "Music Clip Length",
                                        value = state.audioClipSecondsCheck.toFloat(),
                                        valueRange = 1f..60f,
                                        displayValue = "${state.audioClipSecondsCheck}s"
                                    ) { viewModel.setAudioClipSecondsCheck(it.roundToInt()) }
                                    Text(
                                        text = if (state.audioClipSecondsCheck >= 5) {
                                            "Tip: Use free libraries above — regular songs over 5s often get claimed."
                                        } else {
                                            "Tip: Always double-check that your music is copyright-free."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }

                            state.copyrightChecklist.forEach { item ->
                                Surface(
                                    onClick = { viewModel.toggleCopyrightCheck(item.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = StudioSurfaceElevated,
                                    border = BorderStroke(1.dp, if (item.checked) EmeraldSafe.copy(alpha = 0.6f) else StudioBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (item.checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = item.title,
                                            tint = if (item.checked) EmeraldSafe else TextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. SINGLE HORIZONTAL ROW OF BIG ICONS AT THE BOTTOM
                // =========================================================================
                StudioToolTabStrip(
                    tabs = tabs,
                    selected = activeTab,
                    onSelect = { activeTab = it }
                )
            }
        }
    }
}

@Composable
private fun WaveformBarsCanvas(
    bars: List<Float>,
    barColor: Color,
    trimStartPct: Float = 0f,
    trimEndPct: Float = 1f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = bars.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val barW = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
        val midY = size.height / 2f
        bars.forEachIndexed { idx, amp ->
            val pct = idx.toFloat() / count
            val inTrim = pct in trimStartPct..trimEndPct
            val h = (amp.coerceIn(0.08f, 1f) * size.height * 0.88f).coerceAtLeast(5f)
            drawRoundRect(
                color = if (inTrim) barColor else barColor.copy(alpha = 0.2f),
                topLeft = Offset(idx * (barW + gap), midY - h / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}
