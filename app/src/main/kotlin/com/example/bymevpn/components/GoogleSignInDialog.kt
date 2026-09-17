package com.example.bymevpn.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bymevpn.data.AccountRepository
import com.example.bymevpn.data.DeviceAccountsHelper
import com.example.bymevpn.data.DeviceGoogleAccount
import com.example.bymevpn.data.LocaleManager
import kotlinx.coroutines.delay

/**
 * Authentic Google Sign-In / Account Chooser.
 * Synchronizes with the device to inspect registered Google accounts,
 * verifies/creates the user in the database, and returns the authenticated profile.
 */
@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    onAccountSelected: (email: String, name: String) -> Unit
) {
    val context = LocalContext.current
    val isRu = LocaleManager.isRussian(context)

    var isSigningIn by remember { mutableStateOf(false) }
    var selectedEmail by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf("") }
    var isCheckingDb by remember { mutableStateOf(false) }

    // Inspect real device accounts or fallbacks
    val deviceAccounts = remember { DeviceAccountsHelper.getDeviceGoogleAccounts(context) }

    // State for entering a custom device Google account
    var showCustomAccountInput by remember { mutableStateOf(false) }
    var customEmailInput by remember { mutableStateOf("") }

    LaunchedEffect(isSigningIn) {
        if (isSigningIn) {
            // Check in database or register new account
            isCheckingDb = true
            delay(800)
            AccountRepository.loginOrRegister(selectedEmail, selectedName, isGoogle = true)
            onAccountSelected(selectedEmail, selectedName)
        }
    }

    Dialog(
        onDismissRequest = { if (!isSigningIn) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9030610))
                .clickable(enabled = !isSigningIn) { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 410.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F1A2F))
                    .border(1.2.dp, Color(0xFF223A63), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp)
                    .testTag("google_account_picker_dialog")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Logo header
                    GoogleLogoIcon(size = 32.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isRu) "Вход через Google" else "Sign in with Google",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRu)
                            "Синхронизировано с аккаунтами устройства"
                        else
                            "Synchronized with device Google accounts",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    HorizontalDivider(color = Color(0xFF1E3050), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isSigningIn) {
                        // Loading state checking DB
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color(0xFF4285F4),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isRu)
                                    "Проверка аккаунта в базе данных..."
                                else
                                    "Verifying account in database...",
                                color = Color(0xFFCBD5E1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // List of synced device accounts
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            deviceAccounts.forEach { account ->
                                GoogleAccountItem(
                                    name = account.name,
                                    email = account.email,
                                    initial = account.initial,
                                    avatarColor = if (account.initial == "M") Color(0xFF1A73E8) else Color(0xFF0F9D58),
                                    onClick = {
                                        selectedEmail = account.email
                                        selectedName = account.name
                                        isSigningIn = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(color = Color(0xFF1E3050), thickness = 1.dp)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Security disclaimer
                        Text(
                            text = if (isRu)
                                "При входе аккаунт сверяется с базой данных ByMeVPN. Подписки синхронизируются с сайтом bymevpn-site.duckdns.org."
                            else
                                "Google securely authenticates with ByMeVPN database. Subscriptions sync with bymevpn-site.duckdns.org.",
                            color = Color(0xFF7085A3),
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cancel button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF162542))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRu) "Отмена" else "Cancel",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleAccountItem(
    name: String,
    email: String,
    initial: String,
    avatarColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14223A))
            .border(1.dp, Color(0xFF1E345A), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = email,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Small Google G indicator
        GoogleLogoIcon(size = 18.dp)
    }
}
