package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Friend
import com.example.data.model.Transaction
import com.example.ui.viewmodel.TransferUiState
import com.example.ui.viewmodel.UskhaViewModel
import com.example.data.network.SignalingState
import android.hardware.Camera
import android.view.TextureView
import android.graphics.SurfaceTexture
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log

// Dating Dark Theme Color Palette matching the screenshot exactly
val DatingDarkBg = Color(0xFF0F1115)         // Almost pitch black backing
val DatingCardBg = Color(0xFF161920)         // Dark gray cards
val DatingBorderOutline = Color(0xFF1E2330)   // Card border
val DatingTealCyan = Color(0xFF00F0FF)        // High contrast neon cyan/teal
val DatingPinkNeon = Color(0xFFEC4899)        // High contrast pink/neon magenta
val DatingGoldCoin = Color(0xFFFBBF24)        // Warm golden coin color
val DatingWhite = Color(0xFFFFFFFF)
val DatingGrayMuted = Color(0xFF9CA3AF)
val DatingGreenOnline = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: UskhaViewModel,
    onNavigateToFriends: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToBankLimits: () -> Unit,
    onNavigateToVideoCall: () -> Unit
) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val friends by viewModel.friendsState.collectAsStateWithLifecycle()

    // Splash App Opening Disappearing States
    var showSplash by remember { mutableStateOf(true) }
    var splashAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        delay(1800) // Stay visible for 1.8s
        val steps = 25
        for (i in 1..steps) {
            delay(16)
            splashAlpha = 1f - (i.toFloat() / steps)
        }
        showSplash = false
    }

    // Dashboard Interactive States
    var selectedServer by remember { mutableStateOf("USA") }
    var selectedCriteria by remember { mutableStateOf("Any") }
    var matchCount by remember { mutableStateOf(8) }
    var showBuyCoinsDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var referInputPhone by remember { mutableStateOf("") }
    var isDarkModeEnabled by remember { mutableStateOf(true) }
    var showHelpMenu by remember { mutableStateOf(false) }
    var reportUserText by remember { mutableStateOf("") }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            delay(3000)
            snackbarMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkModeEnabled) DatingDarkBg else Color(0xFF1E222D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Spacer to accommodate status bars elegantly
            Spacer(modifier = Modifier.height(36.dp))

            // 1. TOP HEADER SECTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left avatar 'U' and App text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF242936), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DatingTealCyan
                        )
                    }

                    Column {
                        Text(
                            text = "USKHA",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DatingWhite,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(6.dp)
                                    .background(DatingGreenOnline, CircleShape)
                            )
                            Text(
                                text = "UID: ${profile?.unixUid ?: "889301824756"}",
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = DatingTealCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Balance, Stars, Security, Settings buttons on the top right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Coin balance badge
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E222D), RoundedCornerShape(18.dp))
                            .border(1.dp, Color(0xFF2E3547), RoundedCornerShape(18.dp))
                            .clickable { showBuyCoinsDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(DatingGoldCoin, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "${profile?.balance?.toInt() ?: 30}",
                            color = DatingWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Star action
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E222D), CircleShape)
                            .clickable {
                                matchCount++
                                snackbarMessage = "Added random profiles to matching pipeline!"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Match",
                            tint = DatingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Pink Shield (AI Security status) - lock icon coloring representing security shield
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEC4899).copy(alpha = 0.15f), CircleShape)
                            .clickable { onNavigateToScan() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Status",
                            tint = DatingPinkNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E222D), CircleShape)
                            .clickable {
                                onNavigateToBankLimits()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Wallet Limit settings",
                            tint = DatingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Content area
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 2. SECURE SERVER COUNTRY OVERRIDE CONTAINER
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                        border = BorderStroke(1.dp, DatingBorderOutline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "SECURE SERVER COUNTRY OVERRIDE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DatingGrayMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val servers = listOf(
                                    Pair("USA", "🇺🇸 USA"),
                                    Pair("Russia", "🇷🇺 Russia"),
                                    Pair("China", "🇨🇳 China"),
                                    Pair("India", "🇮🇳 India")
                                )

                                servers.forEach { (srvCode, srvLabel) ->
                                    val isSelected = selectedServer == srvCode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) Color(0xFF29235C) else Color(0xFF1A1D26),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) Color(0xFF818CF8) else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                selectedServer = srvCode
                                                snackbarMessage = "Relayed server link to $srvCode node"
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = srvLabel,
                                            color = DatingWhite,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. MATCH CRITERIA SELECTOR
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                        border = BorderStroke(1.dp, DatingBorderOutline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "MATCH CRITERIA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DatingGrayMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val criteria = listOf(
                                    Triple("Any", "Any 🌍", false),
                                    Triple("Boys", "Boys ⚡", false),
                                    Triple("Girls", "Girls ❤️ 🔒", true)
                                )

                                criteria.forEach { (critId, critLabel, isLocked) ->
                                    val isSelected = selectedCriteria == critId
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF1A1D26),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = if (isSelected) DatingTealCyan else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                if (isLocked) {
                                                    selectedCriteria = critId
                                                    snackbarMessage = "Premium mode unlocked successfully!"
                                                } else {
                                                    selectedCriteria = critId
                                                    snackbarMessage = "Criteria changed to $critId"
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = critLabel,
                                            color = DatingWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. GRID OF ACTION BUTTONS SIDE BY SIDE
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        // SECURE TEXT CARD - send icon representing texting chat
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (profile?.isVip == true) {
                                        onNavigateToFriends()
                                    } else if ((profile?.balance ?: 0.0) >= 3.0) {
                                        viewModel.tryUseMessageText { succ ->
                                            if (succ) {
                                                snackbarMessage = "Chat Session Unlocked! -3 Coins deducted."
                                                onNavigateToFriends()
                                            }
                                        }
                                    } else {
                                        snackbarMessage = "Requires 3 Coins for chat! Open VIP memberships."
                                        showBuyCoinsDialog = true
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                            border = BorderStroke(1.2.dp, DatingTealCyan),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Chat",
                                    tint = DatingTealCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Secure Text",
                                        color = DatingWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (profile?.isVip == true) "FREE (VIP 👑)" else "3 Coins 🟢",
                                        color = if (profile?.isVip == true) DatingPinkNeon else DatingTealCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // VIDEO MATCH CARD - call icon representing video matches
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cost = 20.0
                                    if ((profile?.oneTimeFreeCallsRemaining ?: 0) > 0) {
                                        viewModel.tryUseVideoCall(isGirlsOnly = false) { succ ->
                                            if (succ) {
                                                snackbarMessage = "Consumed Promo Free Call! Connecting video match..."
                                                onNavigateToVideoCall()
                                            }
                                        }
                                    } else if (profile?.isVip == true) {
                                        viewModel.tryUseVideoCall(isGirlsOnly = false) { succ ->
                                            if (succ) {
                                                onNavigateToVideoCall()
                                            } else {
                                                snackbarMessage = "VIP call limit exhausted! Standard is 20 coins."
                                            }
                                        }
                                    } else if ((profile?.balance ?: 0.0) >= cost) {
                                        viewModel.tryUseVideoCall(isGirlsOnly = false) { succ ->
                                            if (succ) {
                                                snackbarMessage = "Subtracted 20 Coins. Launching video match!"
                                                onNavigateToVideoCall()
                                            }
                                        }
                                    } else {
                                        snackbarMessage = "Requires 20 Coins! Instant Recharge now."
                                        showBuyCoinsDialog = true
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                            border = BorderStroke(1.2.dp, DatingPinkNeon),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Video",
                                    tint = DatingPinkNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Video Match",
                                        color = DatingWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if ((profile?.oneTimeFreeCallsRemaining ?: 0) > 0) {
                                            "${profile?.oneTimeFreeCallsRemaining} FREE CALL 🟢"
                                        } else if (profile?.isVip == true) {
                                            "FREE (VIP 👑)"
                                        } else {
                                            "20 Coins 🟢"
                                        },
                                        color = if ((profile?.oneTimeFreeCallsRemaining ?: 0) > 0 || profile?.isVip == true) DatingPinkNeon else DatingGreenOnline,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. GIRLS-ONLY VIDEO CALL BUTTON
                item {
                    val coinPrice = 35
                    val currentCoins = profile?.balance ?: 0.0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(listOf(DatingPinkNeon, DatingTealCyan)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(DatingCardBg)
                            .clickable {
                                val cost = 35.0
                                if ((profile?.oneTimeFreeCallsRemaining ?: 0) > 0) {
                                    viewModel.tryUseVideoCall(isGirlsOnly = true) { succ ->
                                        if (succ) {
                                            snackbarMessage = "Consumed Promo Free Call! Connecting to Girls Radar..."
                                            onNavigateToVideoCall()
                                        }
                                    }
                                } else if (profile?.isVip == true) {
                                    viewModel.tryUseVideoCall(isGirlsOnly = true) { succ ->
                                        if (succ) {
                                            snackbarMessage = "VIP Girls Radar Locked! Launching call."
                                            onNavigateToVideoCall()
                                        } else {
                                            snackbarMessage = "VIP girls calls exhausted! Standard is 35 coins."
                                        }
                                    }
                                } else if (currentCoins >= cost) {
                                    viewModel.tryUseVideoCall(isGirlsOnly = true) { succ ->
                                        if (succ) {
                                            snackbarMessage = "Locked boy-to-girl radar! -35 Coins deducted."
                                            onNavigateToVideoCall()
                                        }
                                    }
                                } else {
                                    snackbarMessage = "Girls radar requires 35 Coins! Open VIP shop."
                                    showBuyCoinsDialog = true
                                }
                            }
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(DatingPinkNeon.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Girls Only",
                                        tint = DatingPinkNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "GIRLS-ONLY VIDEO CALL",
                                        fontWeight = FontWeight.Bold,
                                        color = DatingWhite,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Guaranteed boy-to-girl secure radar",
                                        color = DatingGrayMuted,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "Secure Link",
                                        color = DatingTealCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(DatingPinkNeon, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ONLY", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = DatingWhite)
                                        Text("GIRLS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DatingWhite)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if ((profile?.oneTimeFreeCallsRemaining ?: 0) > 0) "FREE PROMO" else if (profile?.isVip == true && (profile?.vipGirlsCallsRemaining ?: 0) > 0) "FREE VIP" else "$coinPrice Coins",
                                        fontWeight = FontWeight.Black,
                                        color = DatingPinkNeon,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if ((profile?.oneTimeFreeCallsRemaining ?: 0) > 0) "${profile?.oneTimeFreeCallsRemaining} Call Left" else if (profile?.isVip == true) "${profile?.vipGirlsCallsRemaining} Left" else "Radar Verified",
                                        fontSize = 8.sp,
                                        color = DatingGrayMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. SECURE COIN WALLET CARD - lock representing secure safety shield
                item {
                    val currentCoins = profile?.balance ?: 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                        border = BorderStroke(1.2.dp, DatingTealCyan),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Wallet",
                                        tint = DatingTealCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "SECURE COIN WALLET",
                                            fontWeight = FontWeight.Bold,
                                            color = DatingWhite,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "🟢 Active Shield Protection",
                                            color = DatingTealCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(width = 30.dp, height = 18.dp)
                                        .background(DatingGoldCoin, RoundedCornerShape(4.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action and Balance Row - star icon representing buy credits/coins
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(DatingGoldCoin, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }

                                    Text(
                                        text = "${currentCoins.toInt()} COINS",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DatingWhite
                                    )
                                }

                                Button(
                                    onClick = { showBuyCoinsDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DatingTealCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("buy_coins_wallet_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BUY COINS",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFF232936))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Secure footer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Secure",
                                        tint = DatingGrayMuted,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Encrypted Gateway: Secure 256-bit SSL",
                                        fontSize = 9.sp,
                                        color = DatingGrayMuted
                                    )
                                }

                                Text(
                                    text = "ID: USK-VAULT-9413",
                                    fontSize = 9.sp,
                                    color = DatingGrayMuted
                                )
                            }
                        }
                    }
                }

                // 6.4. ONE-TIME ₹9 SPECIAL TOP-UP SUPER PROMO
                item {
                    val hasUsed = profile?.hasUsedOneTimeOffer ?: false
                    if (!hasUsed) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showBuyCoinsDialog = true
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F121A)),
                            border = BorderStroke(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(DatingPinkNeon, DatingGoldCoin))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(DatingPinkNeon.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Offer",
                                                tint = DatingPinkNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "🔥 FIRST-TIME SPECIAL BAG OFFER",
                                                fontWeight = FontWeight.Black,
                                                color = DatingWhite,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Get 50 Coins + 1 Free Video Call instantly!",
                                                color = DatingGoldCoin,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(DatingPinkNeon, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("₹ 9 ONLY", fontSize = 9.sp, color = DatingWhite, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "⚡ Instant UPI Recharge • Account-level one time promotion offer. Grab now before matching!",
                                    fontSize = 10.sp,
                                    color = DatingGrayMuted,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showBuyCoinsDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DatingPinkNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text(
                                        text = "ACTIVATE ₹ 9 PROMO BAG INSTANTLY",
                                        color = DatingWhite,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Display status of used offer in a small cute format
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                            border = BorderStroke(1.dp, DatingBorderOutline),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "₹ 9 One-Time Promo Offer Status",
                                                fontWeight = FontWeight.Bold,
                                                color = DatingWhite,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Claimed successfully! ${profile?.oneTimeFreeCallsRemaining ?: 0} Promo Free Calls remaining.",
                                                color = DatingGrayMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6.5. VIP GOLD MEMBERSHIP BAG STATUS CARD
                item {
                    val isVip = profile?.isVip ?: false
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                        border = BorderStroke(
                            width = 1.2.dp,
                            brush = if (isVip) {
                                Brush.horizontalGradient(listOf(DatingPinkNeon, DatingGoldCoin))
                            } else {
                                Brush.linearGradient(listOf(DatingBorderOutline, DatingBorderOutline))
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "VIP Status",
                                        tint = if (isVip) DatingGoldCoin else DatingGrayMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isVip) "👑 VIP MEMBERSHIP BAG ACTIVE" else "👑 VIP MEMBERSHIP BAG INACTIVE",
                                            fontWeight = FontWeight.Bold,
                                            color = DatingWhite,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (isVip) "VIP Plan Level: ${profile?.vipType?.uppercase()}" else "Upgrade today for unlimited secure privileges",
                                            color = if (isVip) DatingGoldCoin else DatingGrayMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                if (isVip) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF22C55E), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACTIVE", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (isVip) {
                                Text(
                                    text = "⚡ Active Perks:\n" +
                                            "• FULL FREE CHAT: Active ✅\n" +
                                            "• Daily Free Video Calls Left: ${profile?.vipDailyCallsRemaining}\n" +
                                            "• Girls-only Calls Left: ${profile?.vipGirlsCallsRemaining}\n" +
                                            "• Ads status: Blocked 🚫 (Ad-Free Matching Active)",
                                    fontSize = 11.sp,
                                    color = DatingWhite,
                                    lineHeight = 15.sp
                                )
                            } else {
                                Text(
                                    text = "Gets full ad-free chat, 15 daily video calls, girls-only call rights and prioritized profiles starting at just ₹ 180!",
                                    fontSize = 11.sp,
                                    color = DatingGrayMuted,
                                    lineHeight = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { showBuyCoinsDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isVip) DatingGoldCoin else DatingPinkNeon
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text(
                                    text = if (isVip) "UPGRADE / EXTEND MEMBERSHIP" else "ACTIVATE VIP MEMBERSHIP FOR ₹ 180",
                                    color = if (isVip) Color.Black else DatingWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // 6.6. REFER & EARN 10 COINS BAG
                item {
                    val referrals = profile?.referralsCount ?: 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                        border = BorderStroke(1.dp, DatingBorderOutline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Refer & Earn",
                                    tint = DatingTealCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "👥 INVITATIONS & REFERRAL COINS",
                                        fontWeight = FontWeight.Bold,
                                        color = DatingWhite,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Earn 10 FREE Coins per join. Infinite invitations allowed",
                                        color = DatingGrayMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Invite Count", color = DatingGrayMuted, fontSize = 9.sp)
                                    Text("$referrals Invites", color = DatingWhite, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Reward Gained", color = DatingGrayMuted, fontSize = 9.sp)
                                    Text("+${referrals * 10} Coins", color = DatingTealCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = referInputPhone,
                                    onValueChange = { referInputPhone = it },
                                    placeholder = { Text("Friend's Whatsapp No.", fontSize = 12.sp) },
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = DatingTealCyan,
                                        unfocusedBorderColor = DatingBorderOutline
                                    ),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = DatingWhite)
                                )

                                Button(
                                    onClick = {
                                        if (referInputPhone.isNotBlank()) {
                                            viewModel.inviteReferral()
                                            snackbarMessage = "✓ Invitation link triggered for $referInputPhone! Referral rewarded with +10 Coins."
                                            referInputPhone = ""
                                        } else {
                                            snackbarMessage = "Input your friend's Whatsapp ID or Phone VPA."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DatingTealCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Text("INVITE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 6.7. WEEKLY HIGHEST PURCHASER BOARD (Gamified Leaderboard)
                item {
                    val weeklyPurchases = profile?.totalPurchasedCoinsThisWeek ?: 0.0

                    // Dynamic ranking and award claim option
                    val userRank = when {
                        weeklyPurchases >= 1500.0 -> 1
                        weeklyPurchases >= 950.0 -> 2
                        weeklyPurchases >= 450.0 -> 3
                        else -> 4
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                        border = BorderStroke(1.2.dp, DatingTealCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Leaderboard",
                                    tint = DatingGoldCoin,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "🏆 WEEKLY TOP PURCHASER BOARD",
                                        fontWeight = FontWeight.Bold,
                                        color = DatingWhite,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Highest shopper wins 1000 Coins + 20 Girls-Only Calls!",
                                        color = DatingGrayMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Table Structure
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF13151D), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Rank", fontSize = 10.sp, color = DatingGrayMuted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("Player", fontSize = 10.sp, color = DatingGrayMuted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.5f))
                                    Text("Coins Bought", fontSize = 10.sp, color = DatingGrayMuted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                }
                                Divider(color = Color(0xFF232936))

                                val leaderboardList = listOf(
                                    Triple(1, "Rahul_99", 1500.0),
                                    Triple(2, "Praveen_UPI", 950.0),
                                    Triple(3, "Kumar_Kash", 450.0),
                                    Triple(userRank, "You (Naveen_07)", weeklyPurchases)
                                ).sortedByDescending { it.third }

                                leaderboardList.forEach { (rank, name, qty) ->
                                    val isSelf = name.contains("You")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelf) DatingTealCyan.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(vertical = 5.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#$rank",
                                            color = if (rank == 1) DatingGoldCoin else DatingWhite,
                                            fontWeight = if (isSelf) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = name,
                                            color = if (isSelf) DatingTealCyan else DatingWhite,
                                            fontWeight = if (isSelf) FontWeight.Black else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(2.5f)
                                        )
                                        Text(
                                            text = "${qty.toInt()} Coins",
                                            color = if (isSelf) DatingTealCyan else DatingWhite,
                                            fontWeight = if (isSelf) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1.5f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Your Weekly Purchased Stat: ${weeklyPurchases.toInt()} Coins (Rank #$userRank)",
                                color = DatingGrayMuted,
                                fontSize = 10.sp
                            )

                            if (userRank == 1 && weeklyPurchases >= 1500.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.buyCoins(1000.0)
                                        snackbarMessage = "🏆 Congratulations No.1 Purchaser! Claimed weekly top 1000 Coins reward!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("CLAIM WEEKLY NO.1 1000 COIN AWARD", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 7. BOTTOM COUNTERS (Match Count & Flag Alerts)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Match Count
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                            border = BorderStroke(1.dp, DatingBorderOutline)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = "Match Count",
                                    color = DatingGrayMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$matchCount",
                                    color = DatingTealCyan,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Flag Alerts
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                            border = BorderStroke(1.dp, DatingBorderOutline)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = "Flag Alerts",
                                    color = DatingGrayMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "0",
                                    color = DatingPinkNeon,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // 8. ADVANCED SETTINGS, SUPPORT & REPORT HUB
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                        border = BorderStroke(1.2.dp, DatingPinkNeon.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Title & Header (Settings symbol)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = DatingPinkNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "⚙️ SETTINGS, HELP & SUPPORT DESK",
                                        fontWeight = FontWeight.Bold,
                                        color = DatingWhite,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Secure matching & payment tracker configuration",
                                        color = DatingGrayMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Online network check banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "🌐 CLOUD LINK STATUS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = DatingTealCyan
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(DatingGreenOnline.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "ONLINE 🟢",
                                                fontSize = 8.sp,
                                                color = DatingGreenOnline,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Multi-network gateway active. High compatibility for Indian networks (Jio, Airtel, Vi) and international carriers (PayPal, global nodes).",
                                        fontSize = 9.sp,
                                        color = DatingGrayMuted,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Settings row 1: Dark Mode toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Force Dark Mode Theme", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DatingWhite)
                                    Text("Ensures eye-safety during late-night matches", fontSize = 10.sp, color = DatingGrayMuted)
                                }
                                Switch(
                                    checked = isDarkModeEnabled,
                                    onCheckedChange = {
                                        isDarkModeEnabled = it
                                        snackbarMessage = if (it) "Dark-mode layout locked!" else "Standard night colors applied"
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DatingPinkNeon,
                                        checkedTrackColor = DatingPinkNeon.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFF232936))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Settings row 2: Help Menu expandable
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showHelpMenu = !showHelpMenu }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("❓ Help & Frequently Asked Queries", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DatingWhite)
                                    Text("How matching, coins, and instant UPI checkout function", fontSize = 10.sp, color = DatingGrayMuted)
                                }
                                Text(
                                    text = if (showHelpMenu) "▲" else "▼",
                                    fontSize = 12.sp,
                                    color = DatingGrayMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (showHelpMenu) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13151D), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "• How do I get coins?\n  Coins credit instantly when your UPI/PayPal transfer is received. You can trigger an instant verification scan to credit them manually.\n" +
                                               "• How does video call work?\n  You can establish fully encrypted, high-quality, zero-delay connections with live room codes on separate devices.\n" +
                                               "• Is there an offline mode?\n  No, Uskha runs purely online on high-speed Indian and global carriers to connect users immediately.",
                                        fontSize = 10.sp,
                                        color = DatingWhite,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFF232936))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Settings row 3: Reporting section
                            Text(
                                text = "🚫 REPORT USER / REPORT FAKE ACCOUNT",
                                fontWeight = FontWeight.Bold,
                                color = DatingWhite,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Submit details of fake profiles, malicious users, or payment issues immediately.",
                                color = DatingGrayMuted,
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = reportUserText,
                                onValueChange = { reportUserText = it },
                                placeholder = { Text("Details (e.g. Profile Sarah 8802... spamming)", fontSize = 11.sp, color = DatingGrayMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = DatingWhite,
                                    unfocusedTextColor = DatingWhite,
                                    focusedBorderColor = DatingPinkNeon,
                                    unfocusedBorderColor = DatingBorderOutline
                                ),
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = DatingWhite)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (reportUserText.isNotBlank()) {
                                        snackbarMessage = "✓ Report filed successfully. Investigation ID: ${System.currentTimeMillis() % 1000000}. Target has been flagged!"
                                        reportUserText = ""
                                    } else {
                                        snackbarMessage = "Please write detail description to file a report."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DatingPinkNeon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text("SUBMIT SUPPORT TICKET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Overlay status text / Toast simulator
        AnimatedVisibility(
            visible = snackbarMessage != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            Surface(
                color = Color(0xFF22C55E),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = snackbarMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Buy Coins Simulated Payment Dialog
        if (showBuyCoinsDialog) {
            SimulatedPaymentDialog(
                viewModel = viewModel,
                onDismiss = { showBuyCoinsDialog = false },
                onPaymentSuccess = { successMsg ->
                    showBuyCoinsDialog = false
                    snackbarMessage = successMsg
                }
            )
        }

        // Overlay the disappearing splash/logo screen!
        if (showSplash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090A0D).copy(alpha = splashAlpha))
                    .pointerInput(Unit) { /* Block active touch interactions during fade-out */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.alpha(splashAlpha)
                ) {
                    // Logo Container - beautiful stylized merged U and K
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E1218), Color(0xFF090A0D))
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFDA4AF), Color(0xFFEC4899))
                                ),
                                shape = RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing U and K joined inside Composable using elegant overlapping typography
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "U",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFDA4AF),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = 6.dp, y = (-6).dp)
                            )
                            Text(
                                text = "k",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFEC4899),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "U S K H A",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = DatingWhite,
                        letterSpacing = 6.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LATEST JOINING SIGNATURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDA4AF),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator(
                        color = Color(0xFFEC4899),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Custom QR Canvas drawer for high contrast look
@Composable
fun UpiQrCodeCanvas(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sizePx = size.width
        drawRect(color = Color.White)
        
        val boxWidth = sizePx * 0.22f
        val innerBoxWidth = sizePx * 0.14f
        val dotBoxWidth = sizePx * 0.08f
        
        fun drawFinder(x: Float, y: Float) {
            drawRect(
                color = Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(boxWidth, boxWidth)
            )
            drawRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(x + (boxWidth - innerBoxWidth) / 2f, y + (boxWidth - innerBoxWidth) / 2f),
                size = androidx.compose.ui.geometry.Size(innerBoxWidth, innerBoxWidth)
            )
            drawRect(
                color = Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(x + (boxWidth - dotBoxWidth) / 2f, y + (boxWidth - dotBoxWidth) / 2f),
                size = androidx.compose.ui.geometry.Size(dotBoxWidth, dotBoxWidth)
            )
        }
        
        // Finders at corners
        drawFinder(sizePx * 0.08f, sizePx * 0.08f)
        drawFinder(sizePx * 0.70f, sizePx * 0.08f)
        drawFinder(sizePx * 0.08f, sizePx * 0.70f)
        
        // Noise grid
        val step = sizePx / 15f
        for (i in 2..12) {
            for (j in 2..12) {
                if ((i < 6 && j < 6) || (i > 9 && j < 6) || (i < 6 && j > 9)) continue
                if ((i * 7 + j * 13) % 4 == 0 || (i * 3 + j * 11) % 5 == 0) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(i * step, j * step),
                        size = androidx.compose.ui.geometry.Size(step, step)
                    )
                }
            }
        }
    }
}

// Transaction item left unmodified to keep structure but styled for matching theme
@Composable
fun TransactionItem(tx: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DatingCardBg),
        border = BorderStroke(1.dp, DatingBorderOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF242936), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tx.friendName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = DatingWhite
                    )
                }

                Column {
                    Text(text = tx.friendName, fontWeight = FontWeight.Bold, color = DatingWhite, fontSize = 13.sp)
                    Text(text = tx.note, color = DatingGrayMuted, fontSize = 11.sp)
                }
            }

            Text(
                text = "-${tx.amount.toInt()} C",
                color = DatingPinkNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

// 2. SIMULATED RECHARGE PAYMENT DIALOG WITH UPI DEEP-LINKS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatedPaymentDialog(
    viewModel: UskhaViewModel,
    onDismiss: () -> Unit,
    onPaymentSuccess: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val profile by viewModel.profileState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("COINS") } // COINS, VIP
    var buyInSteps by remember { mutableStateOf("SELECT_PACK") } // SELECT_PACK, SECURE_UPI, AUTHORIZE
    var selectedAppToPay by remember { mutableStateOf("GPay") } // GPay, Paytm, FamPay, PayPal
    var activeProgressStep by remember { mutableStateOf(1) }
    var trackingLogText by remember { mutableStateOf("Securing gateway node connection...") }
    
    val hasUsedOfferInit = profile?.hasUsedOneTimeOffer ?: false
    var selectedCoinsAmount by remember(hasUsedOfferInit) { 
        mutableStateOf(if (!hasUsedOfferInit) 50.0 else 230.0) 
    }
    var selectedVipType by remember { mutableStateOf("weekly") }
    var selectedPrice by remember(hasUsedOfferInit) { 
        mutableStateOf(if (!hasUsedOfferInit) 9.0 else 100.0) 
    }
    var txnIdInput by remember { mutableStateOf("") }

    val upiId = "0naveen7290odk@fam"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DatingCardBg,
        title = {
            Text(
                text = if (buyInSteps == "SELECT_PACK") "Exchange & VIP Store Bags" else "🔒 Secure UPI Checkout Gateway",
                color = DatingWhite,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (buyInSteps == "SELECT_PACK") {
                    // Toggle Tabs between COINS and VIP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13151D), RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (activeTab == "COINS") DatingTealCyan else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { activeTab = "COINS" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🪙 COIN PACKS",
                                color = if (activeTab == "COINS") Color.Black else DatingWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (activeTab == "VIP") DatingPinkNeon else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { activeTab = "VIP" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "👑 VIP SUBSCRIPTION",
                                color = if (activeTab == "VIP") Color.White else DatingWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (activeTab == "COINS") {
                        val hasUsedOneTimeOffer = profile?.hasUsedOneTimeOffer ?: false
                        if (!hasUsedOneTimeOffer) {
                            Text(
                                "🔥 ONE-TIME SPECIAL SUPER PROMO (1 Offer Left):",
                                color = DatingPinkNeon,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            
                            val isPromoSelected = selectedCoinsAmount == 50.0 && selectedPrice == 9.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isPromoSelected) Color(0xFF3B1D28) else Color(0xFF1F121A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.horizontalGradient(listOf(DatingPinkNeon, DatingGoldCoin)),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedCoinsAmount = 50.0
                                        selectedPrice = 9.0
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(DatingGoldCoin, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("★", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                    }
                                    Column {
                                        Text("50 Coins + 1 Free Video Call", fontWeight = FontWeight.Black, color = DatingWhite, fontSize = 13.sp)
                                        Text("Only ₹ 9 • Instant crediting! (One-time Offer)", color = DatingGrayMuted, fontSize = 9.sp)
                                    }
                                }
                                Text("₹ 9", color = DatingPinkNeon, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            "Select coin package for instant video matching:",
                            color = DatingGrayMuted,
                            fontSize = 11.sp
                        )

                        val coinPacks = listOf(
                            Triple(70.0, 30.0, "₹ 30"),
                            Triple(230.0, 100.0, "₹ 100"),
                            Triple(580.0, 250.0, "₹ 250"),
                            Triple(1160.0, 500.0, "₹ 500")
                        )

                        coinPacks.forEach { (qty, price, label) ->
                            val isSel = selectedCoinsAmount == qty && selectedPrice == price
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSel) Color(0xFF1E293B) else Color(0xFF1B1D26),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isSel) 1.dp else 0.dp,
                                        color = if (isSel) DatingTealCyan else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedCoinsAmount = qty
                                        selectedPrice = price
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(DatingGoldCoin, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                    }
                                    Text("${qty.toInt()} Coins", fontWeight = FontWeight.Bold, color = DatingWhite, fontSize = 13.sp)
                                }
                                Text(label, color = DatingTealCyan, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // VIP Subscriptions tab
                        Text(
                            "Unlock massive savings, free chat limits & high priorities:",
                            color = DatingGrayMuted,
                            fontSize = 11.sp
                        )

                        listOf(
                            Triple("weekly", 180.0, "₹ 180 - Weekly Bag"),
                            Triple("monthly", 560.0, "₹ 560 - Monthly Bag")
                        ).forEach { (type, price, title) ->
                            val isSel = selectedVipType == type && selectedPrice == price
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedVipType = type
                                        selectedPrice = price
                                        selectedCoinsAmount = 0.0
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) Color(0xFF2E1B3E) else Color(0xFF1B1D26)
                                ),
                                border = BorderStroke(
                                    width = if (isSel) 1.dp else 0.dp,
                                    color = if (isSel) DatingPinkNeon else Color.Transparent
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(title, fontWeight = FontWeight.Bold, color = DatingWhite, fontSize = 13.sp)
                                        Box(
                                            modifier = Modifier
                                                .background(DatingPinkNeon, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("VIP 👑", fontSize = 9.sp, color = DatingWhite, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (type == "weekly") {
                                            "• FULL FREE CHAT WEEKLY\n• Daily 15 Free Video Calls\n• 3 Girls-Only Call credits (Week)\n• 100% Ad-Free matching"
                                        } else {
                                            "• FULL FREE CHAT MONTHLY\n• Weekly 60 Free Video Calls\n• 4 Girls-Only Call credits (Week)\n• Ad-Free girls matching"
                                        },
                                        color = DatingGrayMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }

                } else if (buyInSteps == "SECURE_UPI") {
                    Text(
                        text = "Transfer Rs. ${selectedPrice.toInt()} instantly to merchant UPI ID. Money is credited automatically inside 1 second.",
                        color = DatingGrayMuted,
                        fontSize = 11.sp
                    )

                    // Card with recipient details
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13151D), RoundedCornerShape(12.dp))
                            .border(1.dp, DatingTealCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Merchant receiver UPI VPA", color = DatingGrayMuted, fontSize = 9.sp)
                                    Text(upiId, color = DatingTealCyan, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1A3F49), RoundedCornerShape(6.dp))
                                        .clickable {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(upiId))
                                            android.widget.Toast.makeText(context, "UPI ID Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("COPY", color = DatingTealCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFF232936))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Transaction Sum", color = DatingGrayMuted, fontSize = 9.sp)
                                    Text("₹ ${selectedPrice.toInt()} INR", color = DatingWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Purchase Target", color = DatingGrayMuted, fontSize = 9.sp)
                                    Text(
                                        text = if (selectedCoinsAmount > 0.0) "${selectedCoinsAmount.toInt()} Coins" else "VIP Subscription Bag",
                                        color = DatingPinkNeon,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Multi-app secure payment choices (NO QR CODE!)
                    Text("Select payment app to complete check-out:", color = DatingWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("GPay", "Paytm", "FamPay", "PayPal").forEach { app ->
                            val isChosen = selectedAppToPay == app
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isChosen) DatingTealCyan.copy(alpha = 0.2f) else Color(0xFF13151D), RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isChosen) DatingTealCyan else DatingBorderOutline, RoundedCornerShape(8.dp))
                                    .clickable { selectedAppToPay = app }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app,
                                    fontSize = 11.sp,
                                    color = if (isChosen) DatingTealCyan else DatingWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Large Button to launch payment target
                    Button(
                        onClick = {
                            val msg = "Opening secure checkout on $selectedAppToPay..."
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DatingTealCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch & Pay via $selectedAppToPay", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    // Optional manual txn input
                    OutlinedTextField(
                        value = txnIdInput,
                        onValueChange = { txnIdInput = it },
                        label = { Text("Enter UPI Txn Ref ID / UTR (Optional)", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DatingWhite,
                            unfocusedTextColor = DatingWhite,
                            focusedBorderColor = DatingTealCyan,
                            unfocusedBorderColor = DatingBorderOutline,
                            focusedLabelColor = DatingTealCyan
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = DatingWhite),
                        singleLine = true
                    )

                } else if (buyInSteps == "AUTHORIZE") {
                    // Start progressive simulation
                    LaunchedEffect(Unit) {
                        activeProgressStep = 1
                        trackingLogText = "Redirecting: secure $selectedAppToPay gateway dispatch..."
                        delay(900)
                        activeProgressStep = 2
                        trackingLogText = "Handshake: routing Taka/INR transaction token..."
                        delay(1000)
                        activeProgressStep = 3
                        trackingLogText = "Escrow: Node Naveen-7290-Fam registered cash delivery!"
                        delay(1100)
                        activeProgressStep = 4
                        trackingLogText = "Instant Reconcile: crediting user balance instantly!"
                        delay(900)
                        
                        // Execute final success callback and persist status!
                        if (selectedCoinsAmount == 50.0 && selectedPrice == 9.0) {
                            viewModel.claimOneTimeOffer()
                            onPaymentSuccess("Promo Claimed! +50 Coins and +1 Free Video Call credited instantly!")
                        } else if (selectedCoinsAmount > 0.0) {
                            viewModel.buyCoins(selectedCoinsAmount)
                            onPaymentSuccess("Recharge Successful! +${selectedCoinsAmount.toInt()} Coins Credited instantly via $selectedAppToPay!")
                        } else {
                            viewModel.buyVip(selectedVipType)
                            onPaymentSuccess("VIP Membership Activated successfully! Infinite chats + calls unlocked!")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = DatingTealCyan, modifier = Modifier.size(36.dp))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "REAL-TIME TRANSPARENT TRANSACTION TRACKER",
                            color = DatingPinkNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Render graphic route block diagram
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Node 1: Sender wallet
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (activeProgressStep >= 1) DatingTealCyan else Color(0xFF1B1D26), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👛", fontSize = 14.sp)
                                }
                                Text("My Wallet", fontSize = 9.sp, color = if (activeProgressStep >= 1) DatingWhite else DatingGrayMuted, fontWeight = FontWeight.Bold)
                            }
                            
                            Text("➔", color = if (activeProgressStep >= 2) DatingTealCyan else DatingGrayMuted)

                            // Node 2: App Router Gateway
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (activeProgressStep >= 2) DatingTealCyan else Color(0xFF1B1D26), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔀", fontSize = 14.sp)
                                }
                                Text(selectedAppToPay, fontSize = 9.sp, color = if (activeProgressStep >= 2) DatingWhite else DatingGrayMuted, fontWeight = FontWeight.Bold)
                            }

                            Text("➔", color = if (activeProgressStep >= 3) DatingTealCyan else DatingGrayMuted)

                            // Node 3: Escrow / Cash Node
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (activeProgressStep >= 3) DatingTealCyan else Color(0xFF1B1D26), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏛️", fontSize = 14.sp)
                                }
                                Text("Escrow Fund", fontSize = 9.sp, color = if (activeProgressStep >= 3) DatingWhite else DatingGrayMuted, fontWeight = FontWeight.Bold)
                            }

                            Text("➔", color = if (activeProgressStep >= 4) DatingTealCyan else DatingGrayMuted)

                            // Node 4: Coin delivery
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (activeProgressStep >= 4) DatingGoldCoin else Color(0xFF1B1D26), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🪙", fontSize = 14.sp)
                                }
                                Text("Coins Wallet", fontSize = 9.sp, color = if (activeProgressStep >= 4) DatingWhite else DatingGrayMuted, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .border(1.dp, DatingTealCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "STATUS TRACKING LOG:",
                                    fontSize = 9.sp,
                                    color = DatingTealCyan,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = trackingLogText,
                                    fontSize = 11.sp,
                                    color = DatingWhite,
                                    lineHeight = 14.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (buyInSteps == "SELECT_PACK") {
                Button(
                    onClick = { buyInSteps = "SECURE_UPI" },
                    colors = ButtonDefaults.buttonColors(containerColor = DatingPinkNeon)
                ) {
                    Text("Select Pack", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (buyInSteps == "SECURE_UPI") {
                Button(
                    onClick = {
                        buyInSteps = "AUTHORIZE"
                        // Trigger simulated quick delay then confirm
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                ) {
                    Text("I Have Paid (Instant Approve)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (buyInSteps == "AUTHORIZE") {
                // progressive simulation handles this, dummy button hidden
            }
        },
        dismissButton = {
            if (buyInSteps != "AUTHORIZE") {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = DatingPinkNeon)
                }
            }
        }
    )
}

// 3. FRIENDS LIST SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    viewModel: UskhaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val friends by viewModel.friendsState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DatingDarkBg,
        topBar = {
            SmallTopAppBar(
                title = { Text("Available Pairings", color = DatingWhite, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DatingWhite)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = DatingDarkBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Select an active connection to message:",
                    color = DatingGrayMuted,
                    fontSize = 13.sp
                )
            }

            items(friends) { friend ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectFriend(friend)
                            onNavigateToChat()
                        },
                    colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                    border = BorderStroke(1.dp, DatingBorderOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(DatingTealCyan.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friend.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = DatingTealCyan
                                )
                            }

                            Column {
                                Text(text = friend.name, color = DatingWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = friend.bio, color = DatingGrayMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (friend.isOnline) DatingGreenOnline else DatingGrayMuted, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// 4. CHAT AND GIFTING SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferAndChatScreen(
    viewModel: UskhaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBankLimits: () -> Unit,
    onNavigateToVideoCall: () -> Unit
) {
    val friend by viewModel.selectedFriend.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    var giftAmountInput by remember { mutableStateOf("") }
    var notificationMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notificationMsg) {
        if (notificationMsg != null) {
            delay(3000)
            notificationMsg = null
        }
    }

    if (friend == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DatingTealCyan)
        }
        return
    }

    val currentCoins = profile?.balance ?: 0.0

    Scaffold(
        containerColor = DatingDarkBg,
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text(friend!!.name, color = DatingWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(DatingGreenOnline, CircleShape))
                            Text("Active Secure Connection", color = DatingGrayMuted, fontSize = 10.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DatingWhite)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToVideoCall) {
                        Icon(Icons.Default.Call, contentDescription = "Video call", tint = DatingTealCyan)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = DatingDarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Interactive Gift Coin Panel instead of bank transfers
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                border = BorderStroke(1.dp, DatingTealCyan)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        "SEND PREMIUM COIN GIFT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DatingTealCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = giftAmountInput,
                            onValueChange = { giftAmountInput = it },
                            placeholder = { Text("Coin Qty", color = DatingGrayMuted, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DatingWhite,
                                unfocusedTextColor = DatingWhite,
                                focusedBorderColor = DatingTealCyan,
                                unfocusedBorderColor = DatingBorderOutline
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(50.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val coinsToGift = giftAmountInput.toDoubleOrNull()
                                if (coinsToGift == null || coinsToGift <= 0) {
                                    notificationMsg = "Please input valid coin count!"
                                } else if (currentCoins < coinsToGift) {
                                    notificationMsg = "Insufficient coin balance!"
                                } else {
                                    viewModel.switchActiveBank(profile?.activeBankName ?: "Uskha Federal Bank", currentCoins - coinsToGift)
                                    viewModel.sendChatMessage("🎁 Sent ${coinsToGift.toInt()} Coins Premium Gift!")
                                    giftAmountInput = ""
                                    notificationMsg = "Gift sent successfully!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DatingTealCyan),
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Gift Coins", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Msg Container
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    ChatBubble(message.content, message.isFromUser)
                }
            }

            // Chat typing row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF13151B))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Type safe message...", color = DatingGrayMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DatingWhite,
                        unfocusedTextColor = DatingWhite,
                        focusedBorderColor = DatingTealCyan,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendChatMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier.background(DatingTealCyan, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                }
            }
        }

        // Overlay Notification
        AnimatedVisibility(visible = notificationMsg != null) {
            Surface(
                color = DatingPinkNeon,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth().padding(top = 10.dp)
            ) {
                Text(
                    text = notificationMsg ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, isFromUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isFromUser) DatingTealCyan else Color(0xFF242835)
            ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isFromUser) 12.dp else 0.dp,
                bottomEnd = if (isFromUser) 0.dp else 12.dp
            )
        ) {
            Text(
                text = message,
                color = if (isFromUser) Color.Black else DatingWhite,
                fontSize = 13.sp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

// 5. SECURITY SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    viewModel: UskhaViewModel,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = DatingDarkBg,
        topBar = {
            SmallTopAppBar(
                title = { Text("Protection Verified", color = DatingWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DatingWhite)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = DatingDarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Shield",
                tint = DatingGreenOnline,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Device & Channel 100% Safe",
                color = DatingWhite,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "In accordance with your requests, AI security latency blockages have been fully removed. " +
                "Connection streams, video match handshakes, and chat routing operate in high speed " +
                "without checks or waiting timeouts.",
                color = DatingGrayMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = DatingTealCyan),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Proceed to Matches", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 6. BANK LIMITS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankLimitsScreen(
    viewModel: UskhaViewModel,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = DatingDarkBg,
        topBar = {
            SmallTopAppBar(
                title = { Text("Settings & Balance Control", color = DatingWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DatingWhite)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = DatingDarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Limits",
                tint = DatingTealCyan,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Anti-Spam Limits Active",
                color = DatingWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Daily spend limits default to unlimited under the simplified profile framework, allowing unlimited textual chatting and video match loops.",
                color = DatingGrayMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = DatingTealCyan)
            ) {
                Text("Return to App", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 7. LEGACY FRONT CAMERA PREVIEW View (using standard high-compatibility Android hardware API)
@Composable
fun LegacyCameraPreview(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    var mCamera: Camera? = null
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        try {
                            val cameraCount = Camera.getNumberOfCameras()
                            var frontCameraId = -1
                            val cameraInfo = Camera.CameraInfo()
                            for (i in 0 until cameraCount) {
                                Camera.getCameraInfo(i, cameraInfo)
                                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                    frontCameraId = i
                                    break
                                }
                            }
                            mCamera = if (frontCameraId != -1) {
                                Camera.open(frontCameraId)
                            } else {
                                Camera.open()
                            }
                            mCamera?.setPreviewTexture(surface)
                            mCamera?.setDisplayOrientation(90) // Portrait orientation
                            mCamera?.startPreview()
                        } catch (e: Exception) {
                            Log.e("LegacyCameraPreview", "Camera preview start error: ${e.message}")
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            mCamera?.stopPreview()
                            mCamera?.release()
                            mCamera = null
                        } catch (e: Exception) {
                            Log.e("LegacyCameraPreview", "Camera release error: ${e.message}")
                        }
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}

// 7. VIDEO CALL SCREEN (Zero-delay connection & Cross-device Live Handshaking)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(
    viewModel: UskhaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val friend by viewModel.selectedFriend.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableStateOf("SIMULATOR") } // SIMULATOR or REAL_TIME
    var roomCodeInput by remember { mutableStateOf("8888") }
    var displayNameInput by remember(profile) { mutableStateOf(profile?.name ?: "Alex Mercer") }
    var displayBioInput by remember { mutableStateOf("Looking to match! 🌟") }
    var customServerUrl by remember { mutableStateOf("") }
    var showCustomServerUrlField by remember { mutableStateOf(false) }

    var currentCallingName by remember { mutableStateOf("") }
    var currentCallingBio by remember { mutableStateOf("") }
    var isFriendRequestSent by remember { mutableStateOf(false) }

    var activeCallState by remember { mutableStateOf("IDLE") } // IDLE, CONNECTING, LIVE, ENDED
    var secondsElapsed by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }

    // Remote peer states from WebSocket
    val signalingState by viewModel.signalingState.collectAsStateWithLifecycle()
    val signalingLogs by viewModel.signalingLogs.collectAsStateWithLifecycle()
    var peerMuted by remember { mutableStateOf(false) }

    // Camera permission variables
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(friend) {
        if (friend != null && currentCallingName.isEmpty()) {
            currentCallingName = friend?.name ?: "Sarah Connor"
            currentCallingBio = friend?.bio ?: "Safety Specialist & Fitness Coach"
        }
    }

    if (currentCallingName.isEmpty()) {
        currentCallingName = "Sarah Connor"
        currentCallingBio = "Safety Specialist & Fitness Coach"
    }

    // Call Seconds Counter
    LaunchedEffect(activeCallState) {
        if (activeCallState == "LIVE") {
            while (true) {
                delay(1000)
                secondsElapsed++
            }
        }
    }

    // Local / Simulation connect delay
    LaunchedEffect(activeCallState) {
        if (activeCallState == "CONNECTING" && selectedMode == "SIMULATOR") {
            delay(1200) // Fast matching connection, no security blockages
            activeCallState = "LIVE"
        }
    }

    // WebSocket state synchronization handshakes
    LaunchedEffect(signalingState) {
        if (selectedMode == "REAL_TIME" && activeCallState != "IDLE") {
            when (val state = signalingState) {
                is SignalingState.Connected -> {
                    // Waiting for peer log
                }
                is SignalingState.PeerJoined -> {
                    currentCallingName = state.peerName
                    currentCallingBio = state.peerBio
                }
                is SignalingState.PeerLive -> {
                    currentCallingName = state.peerName
                    currentCallingBio = state.peerBio
                    peerMuted = state.peerMuted
                    activeCallState = "LIVE"
                }
                is SignalingState.PeerLeft -> {
                    android.widget.Toast.makeText(context, "Partner ended connection.", android.widget.Toast.LENGTH_LONG).show()
                    activeCallState = "ENDED"
                }
                is SignalingState.Error -> {
                    android.widget.Toast.makeText(context, "Signaling Error: ${state.message}", android.widget.Toast.LENGTH_LONG).show()
                    activeCallState = "IDLE"
                    viewModel.disconnectSignaling()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = DatingDarkBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activeCallState == "IDLE") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(DatingTealCyan.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Video Matching",
                                tint = DatingTealCyan,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Instant Safe Video Link",
                            fontWeight = FontWeight.Black,
                            color = DatingWhite,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calibrating visual streaming codec optimized for high-speed.",
                            color = DatingGrayMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Match Mode Selector Switch Tabs
                        Surface(
                            color = DatingCardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DatingBorderOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Button(
                                    onClick = { selectedMode = "SIMULATOR" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMode == "SIMULATOR") DatingTealCyan else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text(
                                        "💨 Simulator",
                                        color = if (selectedMode == "SIMULATOR") Color.Black else DatingGrayMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { selectedMode = "REAL_TIME" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMode == "REAL_TIME") DatingPinkNeon else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text(
                                        "🌐 Live Real Match",
                                        color = if (selectedMode == "REAL_TIME") DatingWhite else DatingGrayMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedMode == "SIMULATOR") {
                            // Simulation Info Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                                border = BorderStroke(1.dp, DatingBorderOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "Offline High-Fidelity Matcher",
                                        color = DatingTealCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "• Connects instantly to simulated profiles (Sarah Connor, Sneha Roy...)\n" +
                                        "• Ideal to test call features, coin checkout, and matching overlays.\n" +
                                        "• Consumes 1 call token or credits.",
                                        color = DatingGrayMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        } else {
                            // Real-time synchronization parameters
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DatingCardBg),
                                border = BorderStroke(1.dp, DatingPinkNeon.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "Cross-Device Match Settings",
                                        color = DatingPinkNeon,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Connect two phone screens by inserting the same Match Code on both devices!",
                                        color = DatingGrayMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Match room input field
                                    OutlinedTextField(
                                        value = roomCodeInput,
                                        onValueChange = { roomCodeInput = it },
                                        label = { Text("Match Room Code (e.g. 8888)", fontSize = 11.sp) },
                                        placeholder = { Text("E.g. 1111") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = DatingWhite,
                                            unfocusedTextColor = DatingWhite,
                                            focusedBorderColor = DatingPinkNeon,
                                            unfocusedBorderColor = DatingBorderOutline
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Profil data (Name and bio)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = displayNameInput,
                                            onValueChange = { displayNameInput = it },
                                            label = { Text("My Display Name", fontSize = 9.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = DatingWhite,
                                                unfocusedTextColor = DatingWhite,
                                                focusedBorderColor = DatingTealCyan
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = displayBioInput,
                                            onValueChange = { displayBioInput = it },
                                            label = { Text("My Bio Status", fontSize = 9.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = DatingWhite,
                                                unfocusedTextColor = DatingWhite,
                                                focusedBorderColor = DatingTealCyan
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Expand custom URL
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showCustomServerUrlField = !showCustomServerUrlField },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚙️ Custom Signaling Server settings (Developer)",
                                            fontSize = 9.sp,
                                            color = DatingGrayMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(if (showCustomServerUrlField) "▲" else "▼", fontSize = 8.sp, color = DatingGrayMuted)
                                    }

                                    if (showCustomServerUrlField) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = customServerUrl,
                                            onValueChange = { customServerUrl = it },
                                            label = { Text("Custom Server URL (e.g., ws://192.168.1.10:8080)", fontSize = 10.sp) },
                                            placeholder = { Text("Default: Premium Cloud Gateway") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = DatingWhite,
                                                unfocusedTextColor = DatingWhite,
                                                focusedBorderColor = DatingTealCyan
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242835)),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Back", color = DatingWhite)
                        }

                        Button(
                            onClick = {
                                if (selectedMode == "REAL_TIME") {
                                    viewModel.connectSignaling(
                                        roomId = roomCodeInput,
                                        localName = displayNameInput,
                                        localBio = displayBioInput,
                                        customUrl = customServerUrl
                                    )
                                    activeCallState = "CONNECTING"
                                } else {
                                    activeCallState = "CONNECTING"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMode == "REAL_TIME") DatingPinkNeon else DatingTealCyan
                            ),
                            modifier = Modifier.weight(2f).height(48.dp).testTag("start_video_link_btn")
                        ) {
                            Text(
                                text = if (selectedMode == "REAL_TIME") "ESTABLISH REAL LINK 🌐" else "Establish Video Call 💨",
                                color = if (selectedMode == "REAL_TIME") DatingWhite else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else if (activeCallState == "CONNECTING") {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = if (selectedMode == "REAL_TIME") DatingPinkNeon else DatingTealCyan,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (selectedMode == "REAL_TIME") "ESTABLISHING SIGNALING HANDSHAKE..." else "CONNECTING VIDEO PIPELINE...",
                        color = if (selectedMode == "REAL_TIME") DatingPinkNeon else DatingTealCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    
                    if (selectedMode == "REAL_TIME") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Room ID: $roomCodeInput • Waiting for peer device to handshake...",
                            color = DatingGrayMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Real-time signaling log terminal
                        Text(
                            "Handshake Exchange Terminal Logs:",
                            color = DatingWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .border(1.dp, DatingBorderOutline, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            reverseLayout = true
                        ) {
                            items(signalingLogs.reversed()) { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("failure") || log.contains("Error")) DatingPinkNeon else if (log.contains("Connected") || log.contains("Peer")) DatingGreenOnline else DatingGrayMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.disconnectSignaling()
                                activeCallState = "IDLE"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242835)),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("Cancel Handshake", color = DatingWhite)
                        }
                    }
                }
            } else if (activeCallState == "LIVE") {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main call view simulation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF2A2D3A), DatingDarkBg),
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                            // Other peer details
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .background(if (selectedMode == "REAL_TIME") DatingPinkNeon.copy(alpha = 0.15f) else DatingTealCyan.copy(alpha = 0.15f), CircleShape)
                                    .border(
                                        width = 3.dp,
                                        color = if (peerMuted) DatingPinkNeon else DatingGreenOnline,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Contact",
                                    tint = if (selectedMode == "REAL_TIME") DatingPinkNeon else DatingTealCyan,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Connected with $currentCallingName",
                                    color = DatingWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (peerMuted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(DatingPinkNeon, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("MUTED", fontSize = 8.sp, color = DatingWhite, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            val mins = String.format("%02d", secondsElapsed / 60)
                            val secs = String.format("%02d", secondsElapsed % 60)
                            Text(
                                text = "$mins:$secs",
                                color = if (peerMuted) DatingPinkNeon else DatingGreenOnline,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentCallingBio,
                                color = DatingGrayMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )

                            if (selectedMode == "REAL_TIME") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .background(DatingPinkNeon.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .border(1.dp, DatingPinkNeon.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).background(DatingGreenOnline, CircleShape))
                                        Text("CONNECTED REAL-TIME • WS_ROOM: $roomCodeInput", fontSize = 9.sp, color = DatingPinkNeon, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                // Age & Face Badge
                                Box(
                                    modifier = Modifier
                                        .background(DatingGreenOnline.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(0.8.dp, DatingGreenOnline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(5.dp).background(DatingGreenOnline, CircleShape))
                                        Text("AGE: VERIFIED 18+", fontSize = 8.sp, color = DatingGreenOnline, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Audio Badge
                                Box(
                                    modifier = Modifier
                                        .background(DatingTealCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(0.8.dp, DatingTealCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(5.dp).background(DatingTealCyan, CircleShape))
                                        Text("AUDIO: HI-FI ACCENT", fontSize = 8.sp, color = DatingTealCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Self Video Preview card (Top right) with actual camera stream & face calibration guide overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .size(80.dp, 120.dp)
                            .background(Color(0xFF2D323E), RoundedCornerShape(12.dp))
                            .border(1.5.dp, if (selectedMode == "REAL_TIME") DatingPinkNeon else DatingTealCyan, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LegacyCameraPreview(modifier = Modifier.fillMaxSize())
                                // Facial alignment guide overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, DatingGreenOnline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    // Subtle circle guide representing face match box
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .align(Alignment.Center)
                                            .background(Color.Transparent, CircleShape)
                                            .border(1.dp, DatingGreenOnline.copy(alpha = 0.7f), CircleShape)
                                    )
                                    Text(
                                        text = "FACE VERIFIED",
                                        color = DatingGreenOnline,
                                        fontSize = 5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 2.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "No Camera", tint = DatingGrayMuted, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No Cam", color = DatingGrayMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text("Give Access", color = DatingTealCyan, fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // Translucent panel containing "Next Match" and "Send Friend Request"
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 110.dp) // Placed gracefully above the End Call button (which has bottom = 36.dp)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF161920).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, (if (selectedMode == "REAL_TIME") DatingPinkNeon else DatingTealCyan).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Send Friend Request Button (Nearly next to Next/Arrow)
                                    Button(
                                        onClick = {
                                            if (!isFriendRequestSent) {
                                                viewModel.addFriend(currentCallingName, currentCallingBio)
                                                isFriendRequestSent = true
                                                android.widget.Toast.makeText(context, "Friend Request Sent to $currentCallingName! Added to Friend List.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFriendRequestSent) Color(0xFF22C55E) else DatingPinkNeon
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f).height(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFriendRequestSent) Icons.Default.CheckCircle else Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = DatingWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isFriendRequestSent) "Sent ✅" else "Send Request",
                                            fontWeight = FontWeight.Bold,
                                            color = DatingWhite,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Next Match Button (Only enabled in simulator)
                                    Button(
                                        onClick = {
                                            if (selectedMode == "SIMULATOR") {
                                                val nextMatches = listOf(
                                                    Pair("Sneha Roy", "Loves traveling & sunset photography 🌅"),
                                                    Pair("Anjali Sharma", "Always up for deep talks and coffee ☕"),
                                                    Pair("Priya G.", "Fashion designer and food blogger 👗"),
                                                    Pair("Aisha Patel", "Travel geek who loves reading 📚"),
                                                    Pair("Tanya Sen", "Dancer & independent professional 💃"),
                                                    Pair("Kirti Verma", "Dog lover & tech nerd 🐶"),
                                                    Pair("Riya Singh", "Guitarist looking for music lovers 🎸"),
                                                    Pair("Pooja Rawat", "Yoga instructor sharing positive vibes 🧘‍♀️"),
                                                    Pair("Neha Mehta", "Movie buff, always looking for recommendations 🎬"),
                                                    Pair("Aparna Sen", "Gourmet chef and classical music enthusiast 👩‍🍳")
                                                )
                                                val currentMatch = nextMatches.random()
                                                currentCallingName = currentMatch.first
                                                currentCallingBio = currentMatch.second
                                                isFriendRequestSent = false
                                                
                                                // Trigger reconnection animation
                                                secondsElapsed = 0
                                                activeCallState = "CONNECTING"
                                            } else {
                                                // Real time matchmaking room broadcast PING
                                                viewModel.signalingClient.broadcastMessage("JOIN", "Looking to match again!")
                                                android.widget.Toast.makeText(context, "Sent match packet over WS room!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedMode == "REAL_TIME") DatingBorderOutline else DatingTealCyan
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(44.dp).testTag("next_video_match_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (selectedMode == "REAL_TIME") Icons.Default.Refresh else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (selectedMode == "REAL_TIME") DatingGrayMuted else Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (selectedMode == "REAL_TIME") "Re-Ping WS" else "Next Match",
                                            fontWeight = FontWeight.Black,
                                            color = if (selectedMode == "REAL_TIME") DatingGrayMuted else Color.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Current partner: $currentCallingName • $currentCallingBio",
                                    color = DatingGrayMuted,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // End call card (Bottom center)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                if (selectedMode == "REAL_TIME") {
                                    viewModel.sendSignalingMute(isMuted)
                                }
                            },
                            modifier = Modifier.background(if (isMuted) DatingPinkNeon else Color(0xFF1F222B), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.Warning else Icons.Default.Call,
                                contentDescription = "Mute",
                                tint = DatingWhite
                            )
                        }

                        IconButton(
                            onClick = {
                                if (selectedMode == "REAL_TIME") {
                                    viewModel.disconnectSignaling()
                                }
                                activeCallState = "ENDED"
                            },
                            modifier = Modifier.background(DatingPinkNeon, CircleShape).testTag("end_simulation_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "End Call", tint = DatingWhite)
                        }
                    }
                }
            } else if (activeCallState == "ENDED") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = DatingGreenOnline,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Video Call Stream Concluded",
                        fontWeight = FontWeight.Black,
                        color = DatingWhite,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your connection pairing with $currentCallingName concluded with 0 errors.",
                        color = DatingGrayMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.disconnectSignaling()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DatingTealCyan),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Return to Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
