package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.LoginHistoryEntity
import com.example.data.UserAccountEntity
import com.example.security.AppSecurityFirewall
import com.example.updater.AppUpdateInfo
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldSafe
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormat = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    user: UserAccountEntity,
    isOwner: Boolean,
    loginHistory: List<LoginHistoryEntity>,
    isCheckingUpdate: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onShareApp: () -> Unit,
    onOpenAdminPin: () -> Unit,
    onLogOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingName by rememberSaveable { mutableStateOf(false) }
    var draftName by rememberSaveable(user.fullName) { mutableStateOf(user.fullName) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    val initial = user.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ElectricCyanContainer)
                        .border(1.5.dp, ElectricCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = ElectricCyan
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (user.isGuest) "Offline Guest Mode" else user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = if (user.isGuest) StudioSurfaceElevated else ElectricCyanContainer,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, if (user.isGuest) StudioBorder else ElectricCyan)
                ) {
                    Text(
                        text = if (user.isGuest) "Guest" else "Member",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (user.isGuest) TextSecondary else ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = StudioBorder.copy(alpha = 0.6f))

            // 1. Edit Profile
            Surface(
                onClick = { editingName = !editingName },
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_edit_button")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Edit Profile", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                        Text(
                            text = if (editingName) "Close" else "Change Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }

                    if (editingName) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draftName,
                                onValueChange = { draftName = it },
                                label = { Text("Display Name") },
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
                                    onUpdateName(draftName)
                                    editingName = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save Name")
                            }
                        }
                    }
                }
            }

            // 2. Login History
            Surface(
                onClick = { showHistory = !showHistory },
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_login_history_button")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Login History", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                        Text(
                            text = "${loginHistory.size} entries",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    if (showHistory) {
                        Spacer(Modifier.height(10.dp))
                        if (loginHistory.isEmpty()) {
                            Text("No sign-in logs recorded yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                loginHistory.take(8).forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DeepObsidian)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = entry.eventType,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = ElectricCyan
                                        )
                                        Text(
                                            text = historyDateFormat.format(Date(entry.loginTimestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Refer & Share App
            Surface(
                onClick = onShareApp,
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_refer_share_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Refer & Share App", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Share Black Devil Studio Pro with friends", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                    Text("Invite", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSafe)
                }
            }

            // 3b. Check for Updates (GitHub Chrome OTA)
            Surface(
                onClick = { if (!isCheckingUpdate) onCheckForUpdates() },
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_check_updates_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = stringResource(R.string.update_check_title),
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.update_check_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Installed v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    if (isCheckingUpdate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ElectricCyan
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.update_checking),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ElectricCyan
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.update_check_action),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricCyan
                        )
                    }
                }
            }

            // About Black Devil Studio Pro
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepObsidian,
                border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_about_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About Black Devil Studio Pro",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} Pro • 4K Ready • Zero Watermarks • Offline Vault",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        color = ElectricCyanContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "BD PRO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 4. OWNER-ONLY ADMIN DASHBOARD BUTTON (COMPLETELY HIDDEN FOR GUEST & REGULAR USERS)
            if (isOwner) {
                Surface(
                    onClick = onOpenAdminPin,
                    shape = RoundedCornerShape(14.dp),
                    color = ProAmberContainer,
                    border = BorderStroke(1.dp, ProAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_owner_admin_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Dashboard",
                                tint = ProAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Admin Dashboard",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ProAmber
                                )
                                Text(
                                    text = "Owner security & user controls",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ProAmber, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 5. Log Out
            OutlinedButton(
                onClick = onLogOut,
                border = BorderStroke(1.dp, SignalRed.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SignalRed.copy(alpha = 0.1f),
                    contentColor = SignalRed
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("profile_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminPinUnlockDialog(
    lockoutRemainingMs: Long,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onSubmitPin: (String) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    val isLockedOut = lockoutRemainingMs > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ProAmber)
                Text("Owner PIN Required", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isLockedOut) {
                        "PIN entry is temporarily locked for ${AppSecurityFirewall.formatLockoutTime(lockoutRemainingMs)} after 3 failed attempts."
                    } else {
                        "Enter your 6-digit Master PIN to open the Owner Admin Dashboard."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLockedOut) SignalRed else TextSecondary
                )

                if (!isLockedOut) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { raw -> pinInput = raw.filter { it.isDigit() }.take(6) },
                        label = { Text("6-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProAmber,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pin_input")
                    )
                }

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = SignalRed
                    )
                }
            }
        },
        confirmButton = {
            if (!isLockedOut) {
                Button(
                    onClick = {
                        onSubmitPin(pinInput)
                        pinInput = ""
                    },
                    enabled = pinInput.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = ProAmber, contentColor = OledBlack),
                    modifier = Modifier.testTag("admin_pin_unlock_button")
                ) {
                    Text("Unlock", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AdminDashboardScreen(
    currentOwnerId: String,
    users: List<UserAccountEntity>,
    loginHistory: List<LoginHistoryEntity>,
    onRemoveUser: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val registeredCount = users.count { !it.isGuest }
    val guestCount = users.count { it.isGuest }

    Surface(
        color = OledBlack,
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Surface(
                color = DeepObsidian,
                border = BorderStroke(0.5.dp, ProAmber.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose, modifier = Modifier.testTag("admin_close_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Owner Admin Dashboard",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ProAmber
                            )
                            Text(
                                text = "Account oversight & login logs",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Surface(
                        color = ProAmberContainer,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ProAmber)
                    ) {
                        Text(
                            text = "VERIFIED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ProAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard("Total Users", users.size.toString(), Icons.Default.People, ElectricCyan, Modifier.weight(1f))
                    AdminStatCard("Registered", registeredCount.toString(), Icons.Default.VerifiedUser, EmeraldSafe, Modifier.weight(1f))
                    AdminStatCard("Guest", guestCount.toString(), Icons.Default.Person, NeonViolet, Modifier.weight(1f))
                }

                // Accounts Management
                Text(
                    text = "Accounts (${users.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                if (users.isEmpty()) {
                    Text("No accounts stored yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    users.forEach { acc ->
                        val isSelf = acc.id == currentOwnerId
                        Surface(
                            color = StudioSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelf) ProAmber.copy(alpha = 0.6f) else StudioBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = acc.fullName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            color = if (acc.isGuest) StudioSurfaceElevated else ElectricCyanContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isSelf) "Owner" else if (acc.isGuest) "Guest" else "Member",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (isSelf) ProAmber else if (acc.isGuest) TextSecondary else ElectricCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (acc.isGuest) "Local Guest Profile" else acc.email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Last active: ${historyDateFormat.format(Date(acc.lastLoginAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan.copy(alpha = 0.8f)
                                    )
                                }

                                if (!isSelf) {
                                    IconButton(
                                        onClick = { onRemoveUser(acc.id) },
                                        modifier = Modifier.testTag("admin_remove_user_${acc.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Account",
                                            tint = SignalRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Full Login History Logs
                Text(
                    text = "Login History Logs (${loginHistory.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Surface(
                    color = StudioSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (loginHistory.isEmpty()) {
                            Text("No login activity recorded yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            loginHistory.take(40).forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DeepObsidian)
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${log.userName} • ${log.eventType}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (log.isGuest) "Guest Session" else log.userEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = historyDateFormat.format(Date(log.loginTimestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    onUpdateNow: () -> Unit
) {
    val notesText = updateInfo.releaseNotes.ifBlank {
        stringResource(R.string.update_dialog_default_notes)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ota_update_dialog"),
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = stringResource(R.string.update_dialog_title),
                tint = ElectricCyan,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.update_dialog_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DeepObsidian,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${updateInfo.versionName} (Build ${updateInfo.versionCode})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = ElectricCyan
                        )
                        Text(
                            text = "Current v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = notesText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("ota_update_now_button")
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_now),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("ota_update_later_button")
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_later),
                    color = TextSecondary
                )
            }
        }
    )
}
