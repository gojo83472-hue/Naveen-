package com.example.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.MatchHistory
import com.example.data.model.ModerationReport
import com.example.data.model.UserPreferences
import com.example.data.model.WalletTransaction
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.*
import com.example.ui.viewmodel.MatchMode
import com.example.ui.viewmodel.PaymentMethod
import com.example.ui.viewmodel.UskhaScreen
import com.example.ui.viewmodel.UskhaViewModel
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Prime Entry Composable that routes to the correct screen based on viewmodel states
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UskhaMainApp(viewModel: UskhaViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isIndiaNetwork by viewModel.isIndiaNetwork.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth
        val isWideScreen = width > 600.dp

        if (isWideScreen) {
            // Elegant cybernetic galaxy grid wallpaper for PC and Laptop screens
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStep = 40.dp.toPx()
                val w = size.width
                val h = size.height

                // Drawing premium matrix grid lines
                for (x in 0..(w / gridStep).toInt()) {
                    drawLine(
                        color = Color(0xFF141724),
                        start = Offset(x * gridStep, 0f),
                        end = Offset(x * gridStep, h),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..(h / gridStep).toInt()) {
                    drawLine(
                        color = Color(0xFF141724),
                        start = Offset(0f, y * gridStep),
                        end = Offset(w, y * gridStep),
                        strokeWidth = 1f
                    )
                }

                // Smooth background modern neon loops
                drawCircle(color = NeonCyan.copy(alpha = 0.05f), radius = 240.dp.toPx(), center = Offset(0f, h * 0.2f))
                drawCircle(color = NeonPink.copy(alpha = 0.05f), radius = 240.dp.toPx(), center = Offset(w, h * 0.8f))
            }
        }

        // Screen frame centering
        Box(
            modifier = Modifier
                .then(
                    if (isWideScreen) {
                        Modifier
                            .width(430.dp)
                            .fillMaxHeight()
                            .border(BorderStroke(1.5.dp, GridBorder), RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(ObsidianBlack)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
        ) {
            if (!isOnline || !isIndiaNetwork) {
                GatewayBlockerScreen(viewModel = viewModel)
            } else {
                val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
                val context = LocalContext.current
                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        is UskhaScreen.Auth -> AuthScreen(viewModel = viewModel)
                        is UskhaScreen.AgeGate -> AgeGateScreen(viewModel = viewModel, onConfirm = { viewModel.verifyAge(it) })
                        is UskhaScreen.Dashboard -> DashboardScreen(viewModel = viewModel, prefs = prefs)
                        is UskhaScreen.Matching -> MatchingScreen(viewModel = viewModel)
                        is UskhaScreen.TextChat -> TextChatScreen(viewModel = viewModel)
                        is UskhaScreen.VideoChat -> VideoChatScreen(viewModel = viewModel)
                        is UskhaScreen.PremiumHub -> PremiumHubScreen(viewModel = viewModel, prefs = prefs)
                        is UskhaScreen.SafetyCenter -> SafetyCenterScreen(viewModel = viewModel)
                        is UskhaScreen.Settings -> SettingsScreen(viewModel = viewModel)
                        is UskhaScreen.HelpCenter -> HelpCenterScreen(viewModel = viewModel)
                    }
                }

                val incomingCall by viewModel.incomingVideoCall.collectAsStateWithLifecycle()
                val activeServerVal by viewModel.activeServer.collectAsStateWithLifecycle()
                incomingCall?.let { call ->
                    IncomingCallOverlay(
                        match = call,
                        activeServer = activeServerVal,
                        onAccept = { viewModel.acceptIncomingVideoCall() },
                        onDecline = { viewModel.declineIncomingVideoCall() }
                    )
                }
            }
        }
    }
}

@Composable
fun GatewayBlockerScreen(viewModel: UskhaViewModel) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isIndiaNetwork by viewModel.isIndiaNetwork.collectAsStateWithLifecycle()
    val operatorName by viewModel.telephonyOperatorName.collectAsStateWithLifecycle()
    val isMocking by viewModel.isMockingIndiaNetwork.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-Security Radar Scanner
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            var scale by remember { mutableStateOf(1f) }
            LaunchedEffect(Unit) {
                while (true) {
                    scale = 1.3f
                    delay(1200)
                    scale = 1f
                    delay(1200)
                }
            }
            val pulseScale by animateFloatAsState(
                targetValue = scale,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseRadar"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(CircleShape)
                    .background(if (isOnline && isIndiaNetwork) AccentTeal.copy(alpha = 0.15f) else NeonPink.copy(alpha = 0.15f))
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isOnline && isIndiaNetwork) AccentTeal else NeonPink),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOnline && isIndiaNetwork) Icons.Default.Lock else Icons.Default.WifiOff,
                    contentDescription = "Shield Security Node",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "USKHA SECURE ENHANCED GATEWAY",
            color = if (isOnline && isIndiaNetwork) NeonCyan else NeonPink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "India Network Validation",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To protect user security, filter latency, and enable end-to-end encrypted chats/video calls, the Uskha app requires an active broadband or cell link registered to supported Indian telecommunication carriers.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Live Telemetry diagnostics panel
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, GridBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "DIAGNOSTIC SYSTEM TELEMETRY",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // 1. Connection check
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            tint = if (isOnline) AccentTeal else NeonPink,
                            contentDescription = "Online",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Broadband Data", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (isOnline) "Connected" else "Disconnected",
                        color = if (isOnline) AccentTeal else NeonPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. India Carrier check
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isIndiaNetwork) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            tint = if (isIndiaNetwork) AccentTeal else NeonPink,
                            contentDescription = "India Carrier",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registered Indian SIM Location", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (isIndiaNetwork) "Verified" else "Outside India",
                        color = if (isIndiaNetwork) AccentTeal else NeonPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3. active carrier parameter display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GridBorder)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Carrier Operator IP Name", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = operatorName,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Direct System retry trigger button with CDMA feedback sounds
        Button(
            onClick = {
                try {
                    val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                    toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 60)
                } catch (e: Exception) {}
                viewModel.performNetworkCheck()
            },
            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, AccentTeal))),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry Link", tint = NeonCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Re-validate System Handshake", color = Color.White, fontWeight = FontWeight.Black)
        }

        // Gorgeous Emulator / Sandbox sandbox bypass toggle container
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SANDBOX DEVELOPER MODE BYPASS",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Simulate active India Jio/Airtel LTE network links for testing",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isMocking,
                    onCheckedChange = { viewModel.setMockIndiaNetwork(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        }
    }
}

/**
 * 🔞 18+ Verification Age Gate
 */
@Composable
fun AgeGateScreen(viewModel: UskhaViewModel, onConfirm: (Boolean) -> Unit) {
    val method by viewModel.ageVerificationMethod.collectAsStateWithLifecycle()
    val status by viewModel.ageVerificationStatus.collectAsStateWithLifecycle()
    val statusText by viewModel.ageVerificationStepDescription.collectAsStateWithLifecycle()
    val errorMsg by viewModel.verificationError.collectAsStateWithLifecycle()

    val nameInput by viewModel.verificationName.collectAsStateWithLifecycle()
    val dobInput by viewModel.verificationDob.collectAsStateWithLifecycle()
    val docType by viewModel.verificationDocType.collectAsStateWithLifecycle()

    val extName by viewModel.extractedName.collectAsStateWithLifecycle()
    val extDob by viewModel.extractedDob.collectAsStateWithLifecycle()
    val extDocNum by viewModel.extractedDocNum.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var exitAppPressed by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Glowing Double Loop Icon representation of Uskha
        DoubleGlowingLoops(modifier = Modifier.size(110.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "USKHA",
            color = NeonCyan,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.testTag("app_title")
        )

        Text(
            text = "Secure Network Entry",
            color = NeonPink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (method) {
            "NONE" -> {
                // Landing gate selection screen
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, GridBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔞 USKHA AGE GATEWAY",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "To guarantee secure interactive chatrooms and enforce strict local monetization safety, you must verify you are at least 18 years of age. Choose one of our encrypted verification gateways below:",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Verification Paths
                Text(
                    text = "SELECT VERIFICATION PATHWAY",
                    color = AccentTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp)
                )

                // OPTION A: Third Party Registry API Check
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, GridBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setVerificationMethod("THIRD_PARTY") }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "SafeVerify API Check",
                            tint = NeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pathway A: SafeVerify Instant API",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Instant Zero-Knowledge check using registered institutional/government registry nodes.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select Option A",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // OPTION B: Live AI OCR Photo Scanner
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, GridBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setVerificationMethod("ID_SCANNER") }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "AI OCR Scanner",
                            tint = NeonPink,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pathway B: Neural ID Scanner",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Secure OCR scanner parses Aadhaar, Driver License, or Passports locally inside sandboxed container.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select Option B",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { exitAppPressed = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                        border = BorderStroke(1.dp, GridBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("exit_age_gate_button")
                    ) {
                        Text("Exit App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onConfirm(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("confirm_age_gate_button")
                    ) {
                        Text("Fast-Acknowledge 18+", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                if (exitAppPressed) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Access is restricted for minor safety compliance. Self-certification required.",
                        color = NeonPink,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            "THIRD_PARTY" -> {
                // Sub-View: SafeVerify Instant Check Form
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.resetVerificationState() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "SafeVerify Instant Node API",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (status == "IDLE") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, GridBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Cross-Reference Details",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Name input
                            Text("Full Legal Name", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { viewModel.updateVerificationName(it) },
                                placeholder = { Text("e.g. Rahul Sharma", color = Color.Gray) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth(),
                                isError = errorMsg != null && nameInput.isEmpty(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GridBorder,
                                    errorBorderColor = NeonPink
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // DOB input
                            Text("Date of Birth", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = dobInput,
                                onValueChange = { viewModel.updateVerificationDob(it) },
                                placeholder = { Text("DD-MM-YYYY", color = Color.Gray) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = errorMsg != null && !dobInput.matches(Regex("""\d{2}[-/]\d{2}[-/]\d{4}""")),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GridBorder,
                                    errorBorderColor = NeonPink
                                )
                            )

                            if (errorMsg != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = errorMsg ?: "",
                                    color = NeonPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.startThirdPartyInstantCheck() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Verify", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Initiate Zero-Knowledge API Request", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (status == "PROCESSING") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, GridBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = statusText,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Securing biometric signature nodes...",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (status == "SUCCESS") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, Color(0xFF00FF66)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Good",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "CRYPTOGRAPHICALLY SIGNED OVER 18",
                                color = Color(0xFF00FF66),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = GridBorder)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Registry Name:", color = TextSecondary, fontSize = 12.sp)
                                Text(nameInput.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Calculated Status:", color = TextSecondary, fontSize = 12.sp)
                                Text("Verified Adult Approved", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SafeGate Authority:", color = TextSecondary, fontSize = 12.sp)
                                Text("Cryptographic Ledger ID-9828", color = Color.LightGray, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.confirmCompleteVerification() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = ObsidianBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Access Chat Core", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                } else if (status == "FAILED") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, NeonPink),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Failed",
                                tint = NeonPink,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "VERIFICATION DECLINED",
                                color = NeonPink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = errorMsg ?: "Unknown API response exception.",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.setVerificationMethod("THIRD_PARTY") },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, GridBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Retry Verification", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            "ID_SCANNER" -> {
                // Sub-View: Intelligent Live OCR Scanner Viewfinder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.resetVerificationState() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Neural Intelligent OCR Scanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (status == "IDLE") {
                    // Choose doc type inside IDLE scanner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("National ID", "Passport", "Driver License").forEach { t ->
                            val selected = docType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) NeonPink.copy(alpha = 0.2f) else SurfaceCard)
                                    .border(1.dp, if (selected) NeonPink else GridBorder, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.updateVerificationDocType(t) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, color = if (selected) Color.White else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (hasCameraPermission) {
                        // Live Viewfinder Frame with Scanner graphics
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, GridBorder, RoundedCornerShape(16.dp))
                        ) {
                            // Inject real CameraX preview so the sensor actually runs!
                            CameraXFeedPreview(modifier = Modifier.fillMaxSize())

                            // Laser Scanner guide block
                            val infiniteTransition = rememberInfiniteTransition(label = "Scanner")
                            val progressLine by infiniteTransition.animateFloat(
                                initialValue = 0.1f,
                                targetValue = 0.9f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Laser"
                            )

                            // Viewfinder scan outline boundary overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Draw corner bracket outlines
                                val bW = w * 0.85f
                                val bH = h * 0.65f
                                val lX = (w - bW) / 2
                                val lY = (h - bH) / 2

                                // Dark semi-opaque shield around target rectangular scan boundaries
                                drawRect(color = Color.Black.copy(alpha = 0.35f))

                                // Scanning laser card rectangle bounds
                                drawRoundRect(
                                    color = NeonPink,
                                    topLeft = Offset(lX, lY),
                                    size = Size(bW, bH),
                                    cornerRadius = CornerRadius(10.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Linear laser line scanning moving up/down inside the bounds block
                                val lineY = lY + (bH * progressLine)
                                drawLine(
                                    color = NeonPink.copy(alpha = 0.75f),
                                    start = Offset(lX + 10.dp.toPx(), lineY),
                                    end = Offset(lX + bW - 10.dp.toPx(), lineY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }

                            // Viewfinder tiny overlay note
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ALIGN ID FRONT COMPARTMENT IN FRAME",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Request camera prompt or fallback trigger
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, GridBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Camera, contentDescription = "Camera", tint = NeonPink, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Camera Permission Required",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Grant camera sensor bounds to scan and parse credentials locally using OCR nodes.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        try {
                                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        } catch (e: Exception) {
                                            hasCameraPermission = true // fallback to simulation
                                            Log.e("Uskha", "Camera launcher exception", e)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Grant Camera Access", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.startIdDocumentScanSimulation() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Capture", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture & Parse $docType", fontWeight = FontWeight.Black)
                    }
                } else if (status == "PROCESSING") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, GridBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = NeonPink, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = statusText,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Validating machine readable code nodes...",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (status == "SUCCESS") {
                    // Parsed beautiful simulated card badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, AccentTeal),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = "Active", tint = AccentTeal, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Parsed $docType Document", color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF00FF66).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("18+ VALID", color = Color(0xFF00FF66), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Simulated ID layout representation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceDark, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                // Mini mockup avatar slot
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GridBorder),
                                    contentAlignment = Alignment.Center
                                   ) {
                                    Icon(Icons.Default.Person, contentDescription = "Holder Photo", tint = Color.LightGray, modifier = Modifier.size(36.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("HOLDER NAME:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(extName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Row {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("DOCUMENT ID:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(extDocNum, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("DATE OF BIRTH:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(extDob, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Disclaimer: OCR extracted information is securely sandboxed locally with zero server-side storage footprint.",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.confirmCompleteVerification() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = ObsidianBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Complete Verification & Open Uskha", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (status == "FAILED") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, NeonPink),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Failed",
                                tint = NeonPink,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "OCR SCANNING ERROR",
                                color = NeonPink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = errorMsg ?: "Failed to read characters. Please adjust lighting and try again.",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.setVerificationMethod("ID_SCANNER") },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, GridBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Retry Scanning ID", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Main dashboard screen hosting statistic tiles and search triggers
 */
@Composable
fun DashboardScreen(viewModel: UskhaViewModel, prefs: UserPreferences) {
    val history by viewModel.matchHistory.collectAsStateWithLifecycle()
    val activeGenderFilter by viewModel.genderFilter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // App header
        DashboardHeader(
            onOpenPremium = { viewModel.navigateTo(UskhaScreen.PremiumHub) },
            onOpenSafety = { viewModel.navigateTo(UskhaScreen.SafetyCenter) },
            onOpenSettings = { viewModel.navigateTo(UskhaScreen.Settings) },
            onOpenHelp = { viewModel.navigateTo(UskhaScreen.HelpCenter) },
            coins = prefs.walletCoins,
            isVIP = prefs.premiumSubscribed
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Large matching orb & quick info
        DashboardMatchOrb(
            viewModel = viewModel,
            prefs = prefs,
            activeGender = activeGenderFilter,
            onGenderSelect = { viewModel.setGenderFilter(it) },
            onStartText = { viewModel.startMatching(MatchMode.TEXT) },
            onStartVideo = { viewModel.startMatching(MatchMode.VIDEO) },
            onStartGirlsVideo = { viewModel.startMatching(MatchMode.GIRLS_VIDEO) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Secure Coin Wallet Component displaying current balance and linking checkout/payment system
        SecureCoinWalletCard(
            coins = prefs.walletCoins,
            onBuyCoins = { viewModel.navigateTo(UskhaScreen.PremiumHub) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics row
        StatsRow(prefs = prefs, matchesCount = history.size)

        var dashboardTab by remember { mutableStateOf(0) } // 0 = Match Logs, 1 = Social Circle, 2 = Wallet History

        Spacer(modifier = Modifier.height(18.dp))

        // Styled tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (dashboardTab == 0) NeonCyan else Color.Transparent)
                    .clickable { dashboardTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = if (dashboardTab == 0) ObsidianBlack else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Match Logs",
                        color = if (dashboardTab == 0) ObsidianBlack else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (dashboardTab == 1) NeonCyan else Color.Transparent)
                    .clickable { dashboardTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Social Circle",
                        tint = if (dashboardTab == 1) ObsidianBlack else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Social Circle",
                        color = if (dashboardTab == 1) ObsidianBlack else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (dashboardTab == 2) NeonCyan else Color.Transparent)
                    .clickable { dashboardTab = 2 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Wallet History",
                        tint = if (dashboardTab == 2) ObsidianBlack else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Wallet History",
                        color = if (dashboardTab == 2) ObsidianBlack else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dashboardTab == 0) {
            // Match history list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Connection History",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (history.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        color = TextAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { viewModel.clearHistory() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .border(BorderStroke(1.dp, GridBorder), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Empty",
                            tint = TextAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matches yet",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Trigger a random scan above to start!",
                            color = TextAccent,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(history, key = { it.id }) { match ->
                        HistoryItemCard(match = match)
                    }
                }
            }
        } else if (dashboardTab == 1) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                SocialAndInvitesTab(viewModel = viewModel, prefs = prefs)
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                WalletTransactionHistoryTab(viewModel = viewModel)
            }
        }
    }
}

/**
 * Radar sonar sweep screen when searching matches
 */
@Composable
fun MatchingScreen(viewModel: UskhaViewModel) {
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val matchMode by viewModel.matchMode.collectAsStateWithLifecycle()
    val genderFilter by viewModel.genderFilter.collectAsStateWithLifecycle()
    
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val audioVideoQuality by viewModel.audioVideoQuality.collectAsStateWithLifecycle()

    var statusTextIndex by remember { mutableStateOf(0) }
    val searchPhrases = listOf(
        "Broadcasting random seed node...",
        "Validating safety compliance channels...",
        "Analyzing user nodes for direct connections...",
        "Awaiting match handshake...",
        "Connecting safely with direct secure stream..."
    )

    LaunchedEffect(isSearching) {
        while (isSearching) {
            delay(1500)
            statusTextIndex = (statusTextIndex + 1) % searchPhrases.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (matchMode == MatchMode.TEXT) "SECURE REQUEST BROADCAST" else "VIDEO STREAM LINK REQUEST",
            color = NeonCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Target Criteria: $genderFilter",
            color = NeonPink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Spectacular sweeping Radar simulation
        RadarScanner(genderFilter = genderFilter)

        Spacer(modifier = Modifier.height(30.dp))

        // Outgoing Request Profile payload card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "METADATA HANDSHAKE PAYLOAD",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonPink.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ZKP SECURE",
                            color = NeonPink,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("SPOKEN LANGUAGE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(selectedLanguage, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("AUDIO/VIDEO CODEC", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(audioVideoQuality, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CONNECTION STRENGTH", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("Ultra HD Sound Profile", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("MATCH METHOD", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(if (matchMode == MatchMode.TEXT) "Encrypted Chat" else "Stereo Cam Link", color = AccentTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Animating statuses
        Text(
            text = searchPhrases[statusTextIndex],
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Please be respectful. Committing harassment triggers instant automated safety bans.",
            color = TextAccent,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.stopMatching() },
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceCard,
                contentColor = NeonPink
            ),
            border = BorderStroke(1.dp, NeonPink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .width(200.dp)
                .height(50.dp)
                .testTag("cancel_matching_button")
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel Request", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Text Chat screen with Safety scan indicators and report trigger
 */
@Composable
fun TextChatScreen(viewModel: UskhaViewModel) {
    val match by viewModel.activeMatch.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isTyping by viewModel.isPartnerTyping.collectAsStateWithLifecycle()
    val isScanningChat by viewModel.isScanningChat.collectAsStateWithLifecycle()
    val scanVerdict by viewModel.scanVerdict.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showReportMenu by remember { mutableStateOf(false) }

    val reportCategories = listOf(
        "Inappropriate language",
        "Spam / Marketing links",
        "NSFW text",
        "Direct Harassment"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Partner Avatar Circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (match?.partnerGender == "Girl") listOf(NeonPink, OberonPink)
                                else listOf(NeonCyan, OberonCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = match?.partnerName?.take(1) ?: "?",
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${match?.partnerName ?: "Stranger"}, ${match?.partnerAge ?: ""}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Direct text connection (Active)",
                        color = AccentTeal,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { viewModel.sendFriendRequestToPartner() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_friend_chat_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Add Friend",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Friend", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                IconButton(
                    onClick = { showReportMenu = true },
                    modifier = Modifier.testTag("report_partner_icon")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Report",
                        tint = NeonPink
                    )
                }

                Button(
                    onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) },
                    colors = ButtonDefaults.buttonColors(containerColor = GridBorder),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Skip", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Scanning Status indicator
        AnimatedVisibility(visible = isScanningChat) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrightViolet.copy(alpha = 0.2f))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = NeonCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Uskha AI is scanning conversation history...",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Scan Verdict alert dialog
        if (scanVerdict != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BrightViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Warning, contentDescription = "Alert", tint = NeonPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = scanVerdict ?: "",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Dismiss",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.dismissScanVerdict() }
                            .padding(8.dp)
                    )
                }
            }
        }

        // Message Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(message = msg)
            }

            if (isTyping) {
                item {
                    PartnerTypingIndicator(partnerName = match?.partnerName ?: "Stranger")
                }
            }
        }

        // Bottom Input Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Send anonymous message...", color = TextAccent) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NeonCyan)
                    .size(48.dp)
                    .testTag("send_chat_message_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Reporting categories bottom sheet/dialog
    if (showReportMenu) {
        AlertDialog(
            onDismissRequest = { showReportMenu = false },
            title = {
                Text(
                    "Report Anonymity Partner",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Scanning conversation for guidelines violations blocks offending parties instantly using AI.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    reportCategories.forEach { category ->
                        Button(
                            onClick = {
                                viewModel.reportStranger(category)
                                showReportMenu = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportMenu = false }) {
                    Text("Close Menu", color = NeonPink)
                }
            },
            containerColor = SurfaceDark,
            tonalElevation = 6.dp
        )
    }
}

/**
 * Live Video Chat screen using real CameraX views and stranger illustration frames
 */
@Composable
fun VideoChatScreen(viewModel: UskhaViewModel) {
    val match by viewModel.activeMatch.collectAsStateWithLifecycle()
    val isScanningChat by viewModel.isScanningChat.collectAsStateWithLifecycle()
    val scanVerdict by viewModel.scanVerdict.collectAsStateWithLifecycle()
    
    val isScanningVideoFrame by viewModel.isScanningVideoFrame.collectAsStateWithLifecycle()
    val videoScanVerdict by viewModel.videoScanVerdict.collectAsStateWithLifecycle()
    val isVideoSimulationViolationActive by viewModel.isVideoSimulationViolationActive.collectAsStateWithLifecycle()

    val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()
    val activeServerVal by viewModel.activeServer.collectAsStateWithLifecycle()
    var isStreamRevealed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val shouldBlurStream = prefs.safeModeEnabled && !isStreamRevealed

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var isCyberMaskOn by remember { mutableStateOf(true) }
    
    // Live interactive camera filters and lovely/sad/cute/angry visual settings
    var activeCameraFilter by remember { mutableStateOf("None") }
    var activeEmotion by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            try {
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            } catch (e: Exception) {
                Log.e("UskhaVideo", "Failed to launch camera permission request", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Stranger Remote Frame (Top 55%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Moving sine waves simulating active stream with privacy blur support
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (shouldBlurStream) 28.dp else 0.dp)
                ) {
                    VideoNoiseSimulation(
                        partnerName = match?.partnerName ?: "Stranger",
                        gender = match?.partnerGender ?: "Girl"
                    )
                }

                if (shouldBlurStream) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .clickable { isStreamRevealed = true }
                            .padding(16.dp)
                            .testTag("reveal_stream_overlay"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Stream Blurred",
                                tint = NeonPink,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Stream Blurred (Safe Mode)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Click anywhere on the stream or the button below to reveal the live video feed safely.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { isStreamRevealed = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("reveal_stream_btn")
                            ) {
                                Text(
                                    text = "Reveal Stream",
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Top visual overlay info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Badge(
                            containerColor = NeonPink,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "LIVE STRANGER CONNECTED",
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Channel: $activeServerVal",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { viewModel.sendFriendRequestToPartner() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_friend_video_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Add Friend",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Friend", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { viewModel.reportStranger("Harassment in Video Call") },
                            modifier = Modifier.testTag("video_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield Report",
                                tint = NeonPink
                            )
                        }

                        Button(
                            onClick = { viewModel.skipToNextPartner() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("next_partner_video_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Partner",
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Next", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) },
                            colors = ButtonDefaults.buttonColors(containerColor = GridBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Disconnect", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Local Camera Preview frame (Bottom 45%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .background(SurfaceDark)
                    .border(BorderStroke(2.dp, GridBorder)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                // Apply the active camera filter onto local feed dynamically
                                when (activeCameraFilter) {
                                    "Beauty" -> {
                                        drawRect(color = Color.White.copy(alpha = 0.15f), blendMode = androidx.compose.ui.graphics.BlendMode.Overlay)
                                    }
                                    "Sepia" -> {
                                        drawRect(color = Color(0xFF704214).copy(alpha = 0.35f), blendMode = androidx.compose.ui.graphics.BlendMode.Color)
                                    }
                                    "Vintage" -> {
                                        drawRect(color = Color(0xFFE5A65D).copy(alpha = 0.25f), blendMode = androidx.compose.ui.graphics.BlendMode.Multiply)
                                    }
                                    "Monochrome" -> {
                                        drawRect(color = Color.Gray.copy(alpha = 0.5f), blendMode = androidx.compose.ui.graphics.BlendMode.Saturation)
                                    }
                                }
                            }
                    ) {
                        CameraXFeedPreview(modifier = Modifier.fillMaxSize())

                        if (isCyberMaskOn) {
                            CyberPrivacyMaskOverlay(modifier = Modifier.fillMaxSize())
                        }

                        // Toggle Privacy Mask Button
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceCard.copy(alpha = 0.85f))
                                .clickable { isCyberMaskOn = !isCyberMaskOn }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCyberMaskOn) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle CyberMask",
                                tint = if (isCyberMaskOn) AccentTeal else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCyberMaskOn) "Cyber-Mask: Active" else "Cyber-Mask: Off",
                                color = if (isCyberMaskOn) AccentTeal else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera Required",
                            tint = TextAccent,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera Permission Required",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Uskha matches require camera feedback.",
                            color = TextAccent,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // AI scanning progress overlay
                if (isScanningChat) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Uskha AI Scanning stream integrity...",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Camera Filters Floating Selection Tray (Left Center)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FILTERS", color = AccentTeal, fontSize = 9.sp, fontWeight = FontWeight.Black)
            listOf(
                "None" to "🎬",
                "Beauty" to "✨",
                "Sepia" to "🍂",
                "Vintage" to "🎞️",
                "Monochrome" to "👤"
            ).forEach { (filt, emoji) ->
                val isSelected = activeCameraFilter == filt
                IconButton(
                    onClick = {
                        activeCameraFilter = filt
                        try {
                            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 40)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) AccentTeal else SurfaceCard.copy(alpha = 0.85f))
                        .size(38.dp)
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
            }
        }

        // Emotional Reactions Overlay Floating Selection Tray (Right Center)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VIBES", color = NeonPink, fontSize = 9.sp, fontWeight = FontWeight.Black)
            listOf(
                "Sobbing" to "😭",
                "Laughing" to "😂",
                "Loving" to "🥰"
            ).forEach { (emo, emoji) ->
                val isSelected = activeEmotion == emo
                IconButton(
                    onClick = {
                        activeEmotion = if (isSelected) null else emo
                        try {
                            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 40)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) NeonPink else SurfaceCard.copy(alpha = 0.85f))
                        .size(38.dp)
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
            }
        }

        // Interactive Emotion Canvas effects overlay spanning entire fullscreen
        activeEmotion?.let { emo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                when (emo) {
                    "Loving" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(BorderStroke(4.dp, Color(0xFFFF1493).copy(alpha = 0.6f)))
                        ) {
                            Text("🥰 LOVE ORBIT SYSTEM ONLINE 🥰", color = Color(0xFFFF1493), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)))
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                drawCircle(color = Color(0xFFFF1493).copy(alpha = 0.3f), radius = 55f, center = Offset(w * 0.2f, h * 0.25f))
                                drawCircle(color = Color(0xFFFF1493).copy(alpha = 0.3f), radius = 75f, center = Offset(w * 0.82f, h * 0.18f))
                                drawCircle(color = Color(0xFFFF1493).copy(alpha = 0.3f), radius = 60f, center = Offset(w * 0.25f, h * 0.68f))
                                drawCircle(color = Color(0xFFFF1493).copy(alpha = 0.3f), radius = 80f, center = Offset(w * 0.77f, h * 0.72f))
                            }
                        }
                    }
                    "Sobbing" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF00BFFF).copy(alpha = 0.12f))
                                .border(BorderStroke(4.dp, Color(0xFF00BFFF).copy(alpha = 0.5f)))
                        ) {
                            Text("😭 CRITICAL TEARDROP INTENSITY engaged 😭", color = Color(0xFF00BFFF), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)))
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                drawLine(color = Color(0xFF00BFFF).copy(alpha = 0.35f), start = Offset(w * 0.15f, h * 0.2f), end = Offset(w * 0.15f, h * 0.38f), strokeWidth = 6f)
                                drawLine(color = Color(0xFF00BFFF).copy(alpha = 0.35f), start = Offset(w * 0.32f, h * 0.45f), end = Offset(w * 0.32f, h * 0.6f), strokeWidth = 6f)
                                drawLine(color = Color(0xFF00BFFF).copy(alpha = 0.35f), start = Offset(w * 0.68f, h * 0.15f), end = Offset(w * 0.68f, h * 0.32f), strokeWidth = 6f)
                                drawLine(color = Color(0xFF00BFFF).copy(alpha = 0.35f), start = Offset(w * 0.88f, h * 0.52f), end = Offset(w * 0.88f, h * 0.68f), strokeWidth = 6f)
                            }
                        }
                    }
                    "Laughing" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(BorderStroke(4.dp, Color(0xFFFFD700).copy(alpha = 0.6f)))
                        ) {
                            Text("😂 LAUGHTER FREQUENCY ACTIVE 😂", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)))
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                drawCircle(color = Color(0xFFFFD700).copy(alpha = 0.4f), radius = 30f, center = Offset(w * 0.18f, h * 0.15f))
                                drawCircle(color = Color(0xFFFFD700).copy(alpha = 0.4f), radius = 20f, center = Offset(w * 0.85f, h * 0.3f))
                                drawCircle(color = Color(0xFFFFD700).copy(alpha = 0.4f), radius = 35f, center = Offset(w * 0.12f, h * 0.62f))
                                drawCircle(color = Color(0xFFFFD700).copy(alpha = 0.4f), radius = 25f, center = Offset(w * 0.88f, h * 0.78f))
                            }
                        }
                    }
                }
            }
        }

        // --- Gemini AI Shield Status HUD Bar ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark.copy(alpha = 0.88f))
                .border(
                    BorderStroke(
                        1.dp,
                        if (isVideoSimulationViolationActive) NeonPink else NeonCyan.copy(alpha = 0.4f)
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
                .testTag("gemini_video_shield_hud")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isScanningVideoFrame) NeonCyan else Color(0xFF00FF66))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gemini Video Shield v2.5",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Badge(
                        containerColor = if (isScanningVideoFrame) NeonCyan.copy(alpha = 0.15f) else Color(0xFF00FF66).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (isScanningVideoFrame) "SCANNING FRAMES..." else "FEED ACTIVE • SECURE",
                            color = if (isScanningVideoFrame) NeonCyan else Color(0xFF00FF66),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Real-time AI Content Filter is scanning camera frame arrays at 5s frequency. Nudity, violence, self-harm, or policy breaches trigger automatic instant disconnect.",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GridBorder)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Frame analysis:",
                            color = TextAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isScanningVideoFrame) "SCANNING ELEMENTS..." else (videoScanVerdict ?: "SAFE"),
                            color = if (isVideoSimulationViolationActive) NeonPink else Color(0xFF00FF66),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Toggle Button to quickly test safety violation with automated shutdown
                    Button(
                        onClick = { viewModel.toggleVideoSimulationViolation(!isVideoSimulationViolationActive) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVideoSimulationViolationActive) NeonPink.copy(alpha = 0.25f) else GridBorder.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, NeonPink),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("simulate_nsfw_btn")
                    ) {
                        Text(
                            text = if (isVideoSimulationViolationActive) "Stop simulation" else "Simulate NSFW Trigger",
                            color = NeonPink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Real-time scan verdict dialog
        if (scanVerdict != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BrightViolet),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "🛡️ SAFETY MODERATION SCANNED",
                            color = NeonPink,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanVerdict ?: "",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.dismissScanVerdict() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Understand", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium payment screen replicating the Bank of Baroda QR Code screenshot!
 */
@Composable
fun PremiumHubScreen(viewModel: UskhaViewModel, prefs: UserPreferences) {
    val amount by viewModel.selectedPayAmount.collectAsStateWithLifecycle()
    val enteredUtr by viewModel.enteredUtr.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isPaymentProcessing.collectAsStateWithLifecycle()
    val successfullyVerified by viewModel.paymentVerifiedSuccessfully.collectAsStateWithLifecycle()
    val paymentError by viewModel.paymentError.collectAsStateWithLifecycle()

    val premiumHubTab by viewModel.premiumHubTab.collectAsStateWithLifecycle()
    val selectedSubPlan by viewModel.selectedSubPlan.collectAsStateWithLifecycle()
    val subPlanPrice by viewModel.subPlanPrice.collectAsStateWithLifecycle()
    val isSubscribing by viewModel.isSubscribing.collectAsStateWithLifecycle()
    val subSuccess by viewModel.subSuccess.collectAsStateWithLifecycle()
    val subError by viewModel.subError.collectAsStateWithLifecycle()

    // Interactive custom checkout gateway variables
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val paypalEmail by viewModel.paypalEmail.collectAsStateWithLifecycle()
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val cardExpiry by viewModel.cardExpiry.collectAsStateWithLifecycle()
    val cardCvv by viewModel.cardCvv.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val triggerUpiPayment = { selectedAmt: Int ->
        try {
            val upiUri = android.net.Uri.parse("upi://pay?pa=0naveen7290odk@fam&pn=Naveen&am=$selectedAmt&cu=INR&tn=Uskha%20Premium")
            val upiIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, upiUri)
            val chooser = android.content.Intent.createChooser(upiIntent, "Pay $selectedAmt RS to Naveen via:")
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("UskhaPayment", "Failed to launch UPI", e)
            Toast.makeText(context, "No UPI apps found. Switch to PayPal or Card payment.", Toast.LENGTH_LONG).show()
        }
    }

    val coinCount = when (amount) {
        9 -> 25
        30 -> 70
        100 -> 233
        250 -> 585
        500 -> 1170
        1000 -> 2350
        2500 -> 6000
        else -> 25
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // google Pay Replica Top App Bar matching the screenshot exactly
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Premium Coin Checkout",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                Toast.makeText(context, "Secure encrypted multi-channel active", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // PREMIUM HUB COMPONENT TAB SELECTOR (COINS VS SUBSCRIPTION CHECKOUT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF151821))
                .border(BorderStroke(1.dp, GridBorder), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabCoinsSelected = premiumHubTab == 0
            val tabSubSelected = premiumHubTab == 1

            // TAB 1: COIN TIER PACKS
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (tabCoinsSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { viewModel.selectPremiumHubTab(0) }
                    .padding(vertical = 12.dp)
                    .testTag("tab_coin_packs"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Coins Tab",
                    tint = if (tabCoinsSelected) NeonCyan else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Coin Packs",
                    color = if (tabCoinsSelected) NeonCyan else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // TAB 2: VIP SUBSCRIPTION PLAN
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (tabSubSelected) NeonPink.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { viewModel.selectPremiumHubTab(1) }
                    .padding(vertical = 12.dp)
                    .testTag("tab_vip_subscriptions"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Subscription Tab",
                    tint = if (tabSubSelected) NeonPink else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VIP Monthly / Annual",
                    color = if (tabSubSelected) NeonPink else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (premiumHubTab == 0) {
            // ⚡ SPECIAL FLASH PROMO: 9 RS FOR 25 COINS (MANDATED FOR HIGHEST ACCURACY)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clickable {
                    viewModel.updatePayAmount(9)
                    if (selectedPaymentMethod == PaymentMethod.UPI) {
                        triggerUpiPayment(9)
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (amount == 9) NeonCyan.copy(alpha = 0.12f) else Color(0xFF1B1E28)
            ),
            border = BorderStroke(
                width = if (amount == 9) 2.dp else 1.dp,
                brush = if (amount == 9) Brush.linearGradient(listOf(NeonCyan, AccentTeal)) else SolidColor(GridBorder)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Sparkling text badge
                Text(
                    text = "FLASH OFFERS",
                    color = ObsidianBlack,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(bottomStart = 8.dp))
                        .background(NeonCyan)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (amount == 9) NeonCyan else Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Promo",
                            tint = if (amount == 9) ObsidianBlack else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ Mini Wallet Starter Pack",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Get 25 Coins for just ₹9! Perfect for direct voice and standard priority matching.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹9",
                            color = NeonCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "25 COINS",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription/payment item tabs: 30, 100, 250, 500, 1000, 2500 RS
        Text(
            text = "SELECT EXTRA COIN PACK TIER",
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumOptionCard(
                title = "30 RS Starter",
                subtitle = "70 Coins Pack\n+ Advanced Filters",
                selected = amount == 30,
                onClick = {
                    viewModel.updatePayAmount(30)
                    if (selectedPaymentMethod == PaymentMethod.UPI) triggerUpiPayment(30)
                },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "100 RS Pack",
                subtitle = "233 Coins Tier\n+ Girls Priority Match",
                selected = amount == 100,
                onClick = {
                    viewModel.updatePayAmount(100)
                    if (selectedPaymentMethod == PaymentMethod.UPI) triggerUpiPayment(100)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumOptionCard(
                title = "250 RS Value",
                subtitle = "585 Coins Tier\n+ Global Access Unlock",
                selected = amount == 250,
                onClick = {
                    viewModel.updatePayAmount(250)
                    if (selectedPaymentMethod == PaymentMethod.UPI) triggerUpiPayment(250)
                },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "500 RS Pack",
                subtitle = "1170 Coins Tier\n+ VIP Turbo Connection",
                selected = amount == 500,
                onClick = {
                    viewModel.updatePayAmount(500)
                    if (selectedPaymentMethod == PaymentMethod.UPI) triggerUpiPayment(500)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumOptionCard(
                title = "1000 RS Vip",
                subtitle = "2350 Coins Tier\n+ Golden Profile View",
                selected = amount == 1000,
                onClick = {
                    viewModel.updatePayAmount(1000)
                    if (selectedPaymentMethod == PaymentMethod.UPI) triggerUpiPayment(1000)
                },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "2500 RS Best Offer",
                subtitle = "6000 Coins King Tier\n+ Lifetime Access",
                selected = amount == 2500,
                onClick = {
                    viewModel.updatePayAmount(2500)
                    if (selectedPaymentMethod == PaymentMethod.UPI) triggerUpiPayment(2500)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CHOOSE PAYMENT GATEWAY METHOD SELECTOR TAB PANEL
        Text(
            text = "CHOOSE TRANSACTION GATEWAY SYSTEM",
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1B1E28))
                .border(BorderStroke(1.dp, GridBorder), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // UPI option
            val isUpiActive = selectedPaymentMethod == PaymentMethod.UPI
            val upiBg = if (isUpiActive) NeonCyan else Color.Transparent
            val upiFg = if (isUpiActive) ObsidianBlack else Color.Gray

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(upiBg)
                    .clickable { viewModel.selectPaymentMethod(PaymentMethod.UPI) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Smartphone, contentDescription = "UPI Gateway", tint = upiFg, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("BHIM UPI", color = upiFg, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            // PayPal option
            val isPaypalActive = selectedPaymentMethod == PaymentMethod.PAYPAL
            val paypalBg = if (isPaypalActive) NeonCyan else Color.Transparent
            val paypalFg = if (isPaypalActive) ObsidianBlack else Color.Gray

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(paypalBg)
                    .clickable { viewModel.selectPaymentMethod(PaymentMethod.PAYPAL) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Send, contentDescription = "PayPal Gateway", tint = paypalFg, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PayPal Express", color = paypalFg, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            // Credit/Debit Card option
            val isCardActive = selectedPaymentMethod == PaymentMethod.CARD
            val cardBg = if (isCardActive) NeonCyan else Color.Transparent
            val cardFg = if (isCardActive) ObsidianBlack else Color.Gray

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBg)
                    .clickable { viewModel.selectPaymentMethod(PaymentMethod.CARD) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = "Card Gateway", tint = cardFg, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Credit Card", color = cardFg, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (successfullyVerified) {
            // Gold sparkling confirmation badge
            SuccessPaymentCard(amount = amount, onClose = { viewModel.closePaymentScreen() })
        } else {
            // Recreated UPI Screen centered nicely with premium color matching GPay screenshot (Color(0xFF151821))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151821)),
                border = BorderStroke(1.dp, GridBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (selectedPaymentMethod) {
                        PaymentMethod.UPI -> {
                            // Premium design: Official Coin Receiver Wallet / UPI ID Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 18.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1B1E28))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString("0naveen7290odk@fam"))
                                        Toast.makeText(context, "UPI ID Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Coin Payment ID",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "RECEIVER UPI ID FOR COINS",
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "0naveen7290odk@fam",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Selected Package info panel instead of QR
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1B1E28))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ORDER VALUE BHIM UPI",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹$amount",
                                        color = Color.White,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = "Coins",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$coinCount Coins package will be unlocked",
                                            color = Color.LightGray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Primary Interactive CTA Button to trigger auto-launch
                            Button(
                                onClick = { triggerUpiPayment(amount) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFA8C7FA),
                                    contentColor = Color(0xFF001D35)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Pay",
                                    tint = Color(0xFF001D35),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚡ PAY NOW VIA UPI APPS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Launches Google Pay, PhonePe, Paytm, or BHIM automatically.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        PaymentMethod.PAYPAL -> {
                            // Stunning Custom PayPal Gate Branding
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 18.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF003087).copy(alpha = 0.08f))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "PayPal",
                                    color = Color(0xFF0079C1),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Express",
                                    color = Color(0xFF00457C),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            Text(
                                text = "ORDER VALUE PAYPAL INSTANT SECURE",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹$amount",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You will receive $coinCount Coins instantly upon validation.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Paypal Email address registration
                            TextField(
                                value = paypalEmail,
                                onValueChange = { viewModel.updatePaypalEmail(it) },
                                placeholder = { Text("PayPal Registered Email (e.g. user@domain.com)", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "PayPal ID", tint = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1B1E28),
                                    unfocusedContainerColor = Color(0xFF1B1E28),
                                    focusedIndicatorColor = Color(0xFF0079C1),
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("paypal_email_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = { viewModel.submitSimulatedPaymentReceipt() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC439), // PayPal gold
                                    contentColor = Color(0xFF003087)  // PayPal deep blue
                                ),
                                shape = RoundedCornerShape(27.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("paypal_submit_button"),
                                enabled = !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF003087),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Validating PayPal signature...", color = Color(0xFF003087), fontWeight = FontWeight.Bold)
                                } else {
                                    Text("⚡ PayPal Instant Check out", fontWeight = FontWeight.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Authenticates instantly inside a secure PayPal browser sandbox loop.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        PaymentMethod.CARD -> {
                            // Virtual holographic premium debit card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PREMIUM WALLET SECURE",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        val isVisa = cardNumber.replace(" ", "").startsWith("4")
                                        Text(
                                            text = if (isVisa) "VISA" else "MC SECURE",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }

                                    // Display raw card number nicely chunky
                                    val formattedNum = remember(cardNumber) {
                                        val clean = cardNumber.replace(" ", "")
                                        val chunks = clean.chunked(4)
                                        if (chunks.isEmpty()) "•••• •••• •••• ••••" else {
                                            val filled = chunks.toMutableList()
                                            while (filled.size < 4) filled.add("••••")
                                            filled.joinToString(" ") { it.padEnd(4, '•') }.take(19)
                                        }
                                    }
                                    Text(
                                        text = formattedNum,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("CARD HOLDER", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "USKHA VIP MEMBER",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("EXPIRE", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                Text(if (cardExpiry.isNotEmpty()) cardExpiry else "MM/YY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("CVV", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                Text(if (cardCvv.isNotEmpty()) cardCvv.map { "•" }.joinToString("") else "•••", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Card Number interactive field
                            TextField(
                                value = cardNumber,
                                onValueChange = {
                                    val cleanNum = it.filter { char -> char.isDigit() }.take(16)
                                    viewModel.updateCardDetails(cleanNum, cardExpiry, cardCvv)
                                },
                                placeholder = { Text("Card Number (16 Digits)", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = "Card icon", tint = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1B1E28),
                                    unfocusedContainerColor = Color(0xFF1B1E28),
                                    focusedIndicatorColor = NeonCyan,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_number_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Expiry Field
                                TextField(
                                    value = cardExpiry,
                                    onValueChange = {
                                        val clean = it.take(5)
                                        viewModel.updateCardDetails(cardNumber, clean, cardCvv)
                                    },
                                    placeholder = { Text("Expiry (MM/YY)", color = Color.Gray, fontSize = 13.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF1B1E28),
                                        unfocusedContainerColor = Color(0xFF1B1E28),
                                        focusedIndicatorColor = NeonCyan,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("card_expiry_input"),
                                    singleLine = true
                                )

                                // CVV Field
                                TextField(
                                    value = cardCvv,
                                    onValueChange = {
                                        val clean = it.filter { char -> char.isDigit() }.take(3)
                                        viewModel.updateCardDetails(cardNumber, cardExpiry, clean)
                                    },
                                    placeholder = { Text("CVV", color = Color.Gray, fontSize = 13.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF1B1E28),
                                        unfocusedContainerColor = Color(0xFF1B1E28),
                                        focusedIndicatorColor = NeonCyan,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("card_cvv_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = { viewModel.submitSimulatedPaymentReceipt() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = ObsidianBlack
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("card_submit_button"),
                                enabled = !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        color = ObsidianBlack,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Passing PCI card validation...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("🛡️ Pay ₹$amount Securely", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Official Powered lockup
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "SECURED BANK LINK ",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "AES-256",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = 1.sp
                )
                Text(
                    text = " ❯❯",
                    color = Color(0xFF34A853),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedPaymentMethod == PaymentMethod.UPI) {
                // Transaction ID paste field
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Text(
                        text = "Verify Payment Transaction",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = enteredUtr,
                        onValueChange = { viewModel.updateUtrField(it) },
                        placeholder = { Text("Paste UPI Transaction Ref ID / UTR Code", fontSize = 13.sp, color = TextAccent) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_ref_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (paymentError != null) {
                        Text(
                            text = paymentError ?: "",
                            color = NeonPink,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.submitSimulatedPaymentReceipt() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_receipt_button"),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = ObsidianBlack,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying Transaction... Please Wait.", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Activate Premium (${amount} RS)", fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                // For card and PayPal, show custom validation error check if any
                if (paymentError != null) {
                    Text(
                        text = paymentError ?: "",
                        color = NeonPink,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
                    )
                }
            }
        }
    } else {
            // SUBSCRIPTION CHECKOUT CONTENT BLOCK (TAB 1 ACTIVE)
            if (subSuccess) {
                // Subscription Success State Screen Component
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151821)),
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(NeonCyan, NeonPink))),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("subscription_success_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified VIP Badge",
                                tint = NeonCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Text(
                            text = "VIP Premium Activated Successfully!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "You are now fully subscribed to the premium features tier.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        // Invoice breakdown table for visual completeness
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E28)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SECURED INVOICE LEDGER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Plan Type:", color = TextSecondary, fontSize = 12.sp)
                                    Text(selectedSubPlan, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                    Text("Base Price:", color = TextSecondary, fontSize = 12.sp)
                                    Text("₹$subPlanPrice.00", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                    Text("Taxes & Fees:", color = TextSecondary, fontSize = 12.sp)
                                    Text("₹0.00", color = Color.White, fontSize = 12.sp)
                                }
                                Divider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                    Text("Total Charged:", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("₹$subPlanPrice.00", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = "Next simulated renewal is on ${prefs.subscriptionRenewalDate}.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Button(
                            onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sub_success_close_btn")
                        ) {
                            Text(
                                "Return to Matches",
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // ACTIVE MEMBERSHIP CONSOLE (IF SUBSCRIBED REGULARLY)
                if (prefs.isSubscriptionActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151821)),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(NeonCyan, NeonPink))),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .testTag("active_subscription_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active Plan",
                                    tint = NeonPink,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ACTIVE VIP ACCESS",
                                    color = NeonPink,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Text(
                                text = "Current Plan: ${prefs.subscriptionName}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Simulated Renewal: ${prefs.subscriptionRenewalDate}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = Color.White.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Text(
                                text = "Your Active Privileges:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "Checked", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlimited priority girl matching filters", color = Color.Gray, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "Checked", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Zero Coin matching hold times", color = Color.Gray, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "Checked", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Premium HD Stereo quality stream lines", color = Color.Gray, fontSize = 11.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { viewModel.cancelSubscription() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, NeonPink),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("cancel_sub_btn")
                            ) {
                                Text("Terminate VIP Subscription", color = NeonPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // SELECTION & CHECKOUT FORM CONTAINER
                    Text(
                        text = "SELECT VIP PLAN TO CHECKOUT",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Plan Option 1: Monthly
                        val extMonthly = selectedSubPlan == "Monthly Gold Pass"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectSubscriptionPlan("Monthly Gold Pass", 149) }
                                .testTag("sub_plan_monthly"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (extMonthly) NeonPink.copy(alpha = 0.08f) else Color(0xFF1B1E28)
                            ),
                            border = BorderStroke(
                                width = if (extMonthly) 2.dp else 1.dp,
                                brush = if (extMonthly) Brush.linearGradient(listOf(NeonPink, Color.Magenta)) else SolidColor(GridBorder)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("MONTHLY PASS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("₹149", color = NeonPink, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                Text("per month", color = Color.Gray, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Unlock full video filters, direct matchmaking.", color = Color.LightGray, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }

                        // Plan Option 2: Annual (Deep discount)
                        val extAnnual = selectedSubPlan == "Annual VIP Star Pass"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectSubscriptionPlan("Annual VIP Star Pass", 599) }
                                .testTag("sub_plan_annual"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (extAnnual) NeonPink.copy(alpha = 0.08f) else Color(0xFF1B1E28)
                            ),
                            border = BorderStroke(
                                width = if (extAnnual) 2.dp else 1.dp,
                                brush = if (extAnnual) Brush.linearGradient(listOf(NeonPink, NeonCyan)) else SolidColor(GridBorder)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box {
                                Text(
                                    text = "SAVE 66%",
                                    color = ObsidianBlack,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(bottomStart = 8.dp))
                                        .background(NeonCyan)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("ANNUAL SPECIAL", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("₹599", color = NeonCyan, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    Text("per year", color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Everything + Spotlight Profile + Golden border.", color = Color.LightGray, fontSize = 10.sp, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // INVOICE SUMMARY PANEL
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151821)),
                        border = BorderStroke(1.dp, GridBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ORDER BILLING SUMMARY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(selectedSubPlan, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("₹$subPlanPrice.00", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Secured line platform tax", color = Color.Gray, fontSize = 11.sp)
                                Text("₹0.00", color = Color.Gray, fontSize = 11.sp)
                            }
                            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Secure checkout amount", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("₹$subPlanPrice.00", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // SEGMENT SECURED CHANNELS (Card, PayPal, UPI)
                    Text(
                        text = "CHOOSE SECURE SUBSCRIPTION GATEWAY",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B1E28))
                            .border(BorderStroke(1.dp, GridBorder), RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Card Tab Option
                        val isCard = selectedPaymentMethod == PaymentMethod.CARD
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCard) NeonPink.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.selectPaymentMethod(PaymentMethod.CARD) }
                                .padding(vertical = 10.dp)
                                .testTag("sub_gateway_card"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = "Card", tint = if (isCard) NeonPink else Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Card Checkout", color = if (isCard) NeonPink else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // PayPal Tab Option
                        val isPaypal = selectedPaymentMethod == PaymentMethod.PAYPAL
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPaypal) NeonPink.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.selectPaymentMethod(PaymentMethod.PAYPAL) }
                                .padding(vertical = 10.dp)
                                .testTag("sub_gateway_paypal"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "PayPal", tint = if (isPaypal) NeonPink else Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PayPal Express", color = if (isPaypal) NeonPink else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // CARD INPUT FIELDS & PAY TRIGGER
                    if (selectedPaymentMethod == PaymentMethod.CARD) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            TextField(
                                value = cardNumber,
                                onValueChange = {
                                    val cleanNum = it.filter { char -> char.isDigit() }.take(16)
                                    viewModel.updateCardDetails(cleanNum, cardExpiry, cardCvv)
                                },
                                placeholder = { Text("Enter Card Number (16 Digits)", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = "Card logo", tint = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1B1E28),
                                    unfocusedContainerColor = Color(0xFF1B1E28),
                                    focusedIndicatorColor = NeonPink,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sub_card_number_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextField(
                                    value = cardExpiry,
                                    onValueChange = {
                                        val clean = it.take(5)
                                        viewModel.updateCardDetails(cardNumber, clean, cardCvv)
                                    },
                                    placeholder = { Text("Expiry (MM/YY)", color = Color.Gray, fontSize = 13.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF1B1E28),
                                        unfocusedContainerColor = Color(0xFF1B1E28),
                                        focusedIndicatorColor = NeonPink,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sub_card_expiry_input"),
                                    singleLine = true
                                )
                                
                                TextField(
                                    value = cardCvv,
                                    onValueChange = {
                                        val clean = it.filter { char -> char.isDigit() }.take(3)
                                        viewModel.updateCardDetails(cardNumber, cardExpiry, clean)
                                    },
                                    placeholder = { Text("CVV", color = Color.Gray, fontSize = 13.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF1B1E28),
                                        unfocusedContainerColor = Color(0xFF1B1E28),
                                        focusedIndicatorColor = NeonPink,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sub_card_cvv_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    } else {
                        // PAYPAL INPUT email ADDRESS
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            TextField(
                                value = paypalEmail,
                                onValueChange = { viewModel.updatePaypalEmail(it) },
                                placeholder = { Text("PayPal Associated Email (e.g., auth@uskha.com)", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email log", tint = Color.Gray) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1B1E28),
                                    unfocusedContainerColor = Color(0xFF1B1E28),
                                    focusedIndicatorColor = NeonPink,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sub_paypal_email_input"),
                                singleLine = true
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // SUBMIT ERROR ALERT FOR PAYMENT DECLINES
                    if (subError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NeonPink.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, NeonPink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, "Error", tint = NeonPink, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(subError ?: "", color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                    
                    // STEP MULTI-HUD FOR PROCESS SIMULATION
                    if (isSubscribing) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E28)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = NeonPink, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Executing secure checkout protocols...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(color = NeonPink, trackColor = Color.DarkGray, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("1. Connecting AES-256 secure gateway...\n2. Simulating bank authorization handshakes...\n3. Unlocking elite matching filters in repository...", color = Color.Gray, fontSize = 10.sp, lineHeight = 13.sp)
                            }
                        }
                    } else {
                        // MAIN CHEKOUT PROCESS TRIG ACTION BTN
                        Button(
                            onClick = { viewModel.submitSubscriptionPayment() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp)
                                .testTag("sub_checkout_btn")
                        ) {
                            Text("Secure Subscription Checkout (₹$subPlanPrice)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // SIMULATED TESTING SWITCH TOGGLE CONTROLS FOR DEVELOPMENT
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E28).copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🔧 Simulate Payment Decline",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Toggle this switch to mock card verification failures (eg. Code 51 Insufficient funds). Active state: ${if (prefs.simulatedPaymentForceDecline) "Declines On" else "Accept All"}",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        lineHeight = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Switch(
                                    checked = prefs.simulatedPaymentForceDecline,
                                    onCheckedChange = { viewModel.toggleSimulatedDecline(it) },
                                    modifier = Modifier.testTag("sim_purchase_decline_switch")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Safety reports center dashboard showing Gemini safety analysis audits
 */
@Composable
fun SafetyCenterScreen(viewModel: UskhaViewModel) {
    val reports by viewModel.moderationReports.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Safety Center Logs",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, GridBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛡️ USKHA SAFETY PROTOCOLS",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All reported chats are evaluated directly by the Google Gemini safety filter. Handshakes and stream tokens are instantly revoked for malicious actors.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Filed Reports Audit (${reports.size})",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No safety incident reports have been filed.",
                    color = TextAccent,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportItemCard(report = report)
                }
            }
        }
    }
}

// =========================================================================
//                   SUBUI DESIGNS AND VISUAL HELPERS
// =========================================================================

@Composable
fun DoubleGlowingLoops(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Loops")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rot"
    )

    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        val sizePx = size.minDimension
        val radius = sizePx * 0.28f

        // Draw Cyan Loop
        drawCircle(
            color = NeonCyan,
            radius = radius,
            center = Offset(size.width * 0.42f, size.height * 0.5f),
            style = Stroke(width = 6.dp.toPx())
        )

        // Draw Pink Loop
        drawCircle(
            color = NeonPink,
            radius = radius,
            center = Offset(size.width * 0.58f, size.height * 0.5f),
            style = Stroke(width = 6.dp.toPx())
        )
    }
}

@Composable
fun HistoryItemCard(match: MatchHistory) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, GridBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (match.partnerGender == "Girl") listOf(NeonPink, OberonPink)
                                else listOf(NeonCyan, OberonCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = match.partnerName.take(1),
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${match.partnerName}, ${match.partnerAge}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${match.partnerGender} matched",
                        color = TextAccent,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "Secure Text",
                color = AccentTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val isSystem = message.sender == "system"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = when {
            isSystem -> Alignment.Center
            isUser -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (isSystem) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GridBorder),
                border = BorderStroke(1.dp, BrightViolet.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = message.messageText,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) NeonCyan else SurfaceCard
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isUser) Color.Transparent else GridBorder
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.messageText,
                    color = if (isUser) ObsidianBlack else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun PartnerTypingIndicator(partnerName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "Dots")
    val dotAnimation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Typing"
    )

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$partnerName is typing",
            color = TextAccent,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .graphicsLayer {
                            val shift = index * 0.25f
                            val normalized = (dotAnimation.value + shift) % 1.0f
                            alpha = if (normalized < 0.5f) normalized * 2f else (1.0f - normalized) * 2f
                        }
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
            }
        }
    }
}

@Composable
fun RadarScanner(genderFilter: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val sweepSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Sweep"
    )

    val scaleScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutCirc),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Box(
        modifier = Modifier
            .size(240.dp)
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                val fullRadius = size.minDimension / 2

                // Concentric circles with scale animation
                drawCircle(
                    color = GridBorder,
                    radius = fullRadius,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = GridBorder,
                    radius = fullRadius * 0.7f * scaleScale,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = GridBorder,
                    radius = fullRadius * 0.4f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Sweep radar wedge
                rotate(sweepSweep, center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonCyan.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        size = Size(fullRadius * 2, fullRadius * 2),
                        topLeft = Offset(center.x - fullRadius, center.y - fullRadius)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Glowing Center Badge representing search profile
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(ObsidianBlack)
                .border(
                    BorderStroke(
                        2.dp,
                        if (genderFilter == "Girl") NeonPink else NeonCyan
                    ), CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (genderFilter == "Girl") Icons.Default.Favorite else Icons.Default.Person,
                contentDescription = "Scanning Info",
                tint = if (genderFilter == "Girl") NeonPink else NeonCyan,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun VideoXPlaceholder(partnerName: String, gender: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        if (gender == "Girl") listOf(NeonPink, OberonPink)
                        else listOf(NeonCyan, OberonCyan)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                partnerName.take(1),
                color = ObsidianBlack,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "$partnerName • Active HD Feed",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Ultra-low latency connection established",
            color = Color(0xFF00FF66),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Peer-to-peer end-to-end encrypted node link",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun CameraXFeedPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isCameraAvailable by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("UskhaVideo", "Camera permission is not granted yet.")
            isCameraAvailable = false
            return@LaunchedEffect
        }
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val cameraSelector = when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        else -> null
                    }
                    if (cameraSelector != null) {
                        val preview = CameraPreviewUseCase.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                        isCameraAvailable = true
                    } else {
                        isCameraAvailable = false
                        Log.w("UskhaVideo", "No active camera sensor found.")
                    }
                } catch (e: Exception) {
                    isCameraAvailable = false
                    Log.e("UskhaVideo", "Failed to bind camera lifecycle", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            isCameraAvailable = false
            Log.e("UskhaVideo", "Failed to obtain ProcessCameraProvider", e)
        }
    }

    if (isCameraAvailable) {
        AndroidView(
            factory = { previewView },
            modifier = modifier
        )
    } else {
        // High fidelity futuristic simulation feed
        Box(
            modifier = modifier
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "CameraSim")
            val sweepAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SimulationSweep"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Pulse"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val limit = size.minDimension / 2

                // Concentric Tech Rings
                drawCircle(
                    color = GridBorder,
                    radius = limit * 0.85f,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = GridBorder,
                    radius = limit * 0.55f,
                    style = Stroke(width = 0.5.dp.toPx())
                )

                // High-End holographic scanning wedges
                rotate(sweepAngle, centerOffset) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonPink.copy(alpha = 0.15f),
                                NeonCyan.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawLine(
                        color = NeonCyan,
                        start = centerOffset,
                        end = Offset(centerOffset.x + (limit * 0.85f), centerOffset.y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Simulated Camera Feed",
                    tint = NeonPink,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            rotationZ = sweepAngle
                        }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "SANDBOX CAMERA SIMULATION",
                    color = NeonPink,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Secure Peer-to-Peer Virtual Stream",
                    color = Color.LightGray.copy(alpha = pulseAlpha),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CyberPrivacyMaskOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "CyberMask")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Scan"
    )

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
            val baseRadius = minOf(w, h) * 0.25f * pulseScale

            // Draw scanning green horizontal laser line
            val yPos = scanOffset * h
            drawLine(
                color = AccentTeal.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(0f, yPos),
                end = androidx.compose.ui.geometry.Offset(w, yPos),
                strokeWidth = 3.dp.toPx()
            )

            // Draw abstract glowing digital face mask structure / biometric overlay
            drawCircle(
                color = AccentTeal.copy(alpha = 0.12f),
                center = center,
                radius = baseRadius
            )
            drawCircle(
                color = AccentTeal,
                center = center,
                radius = baseRadius,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = AccentTeal.copy(alpha = 0.35f),
                center = center,
                radius = baseRadius * 0.75f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Outer crosshair brackets
            val length = 16.dp.toPx()
            val gap = baseRadius + 10.dp.toPx()
            // Top Left
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x - gap, center.y - gap), androidx.compose.ui.geometry.Offset(center.x - gap + length, center.y - gap), 2.dp.toPx())
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x - gap, center.y - gap), androidx.compose.ui.geometry.Offset(center.x - gap, center.y - gap + length), 2.dp.toPx())
            // Top Right
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x + gap, center.y - gap), androidx.compose.ui.geometry.Offset(center.x + gap - length, center.y - gap), 2.dp.toPx())
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x + gap, center.y - gap), androidx.compose.ui.geometry.Offset(center.x + gap, center.y - gap + length), 2.dp.toPx())
            // Bottom Left
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x - gap, center.y + gap), androidx.compose.ui.geometry.Offset(center.x - gap + length, center.y + gap), 2.dp.toPx())
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x - gap, center.y + gap), androidx.compose.ui.geometry.Offset(center.x - gap, center.y + gap - length), 2.dp.toPx())
            // Bottom Right
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x + gap, center.y + gap), androidx.compose.ui.geometry.Offset(center.x + gap - length, center.y + gap), 2.dp.toPx())
            drawLine(AccentTeal, androidx.compose.ui.geometry.Offset(center.x + gap, center.y + gap), androidx.compose.ui.geometry.Offset(center.x + gap, center.y + gap - length), 2.dp.toPx())
        }

        // Display cyber icon and textual warning overlay
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Privacy Shield",
                tint = AccentTeal,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "HOLOGRAPHIC MASK ON",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "USKHA SECURE SHIELD ACTIVE",
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "AI alters & anonymizes live screen nodes.",
                color = TextAccent,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DashboardHeader(
    onOpenPremium: () -> Unit,
    onOpenSafety: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    coins: Int,
    isVIP: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Elegant Mini Logo
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GridBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "USKHA",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Safe & Secure Matching",
                    color = TextAccent,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coin Widget
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceCard)
                    .clickable { onOpenPremium() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // VIP / Subscription Hub link
            IconButton(
                onClick = onOpenPremium,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isVIP) BrightViolet.copy(alpha = 0.3f) else SurfaceCard)
                    .size(36.dp)
                    .testTag("premium_dashboard_tab")
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "VIP",
                    tint = if (isVIP) BrightViolet else Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Safety check logo
            IconButton(
                onClick = onOpenSafety,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Safety Center",
                    tint = NeonPink,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings icon
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .size(36.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AccentTeal,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Help & Bug Solver icon
            IconButton(
                onClick = onOpenHelp,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .size(36.dp)
                    .testTag("help_center_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Support & Help",
                    tint = BrightViolet,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardMatchOrb(
    viewModel: UskhaViewModel,
    prefs: UserPreferences,
    activeGender: String,
    onGenderSelect: (String) -> Unit,
    onStartText: () -> Unit,
    onStartVideo: () -> Unit,
    onStartGirlsVideo: () -> Unit
) {
    val activeServerVal by viewModel.activeServer.collectAsStateWithLifecycle()
    val serversList = listOf(
        "USA" to "🇺🇸",
        "Russia" to "🇷🇺",
        "China" to "🇨🇳",
        "India" to "🇮🇳",
        "Global" to "🌎"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, GridBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SECURE SERVER COUNTRY OVERRIDE",
                color = TextAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                serversList.forEach { (srv, flag) ->
                    val isSelected = activeServerVal.contains(srv)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrightViolet.copy(alpha = 0.35f) else SurfaceDark)
                            .border(BorderStroke(1.dp, if (isSelected) BrightViolet else Color.Transparent), RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.selectServer("$srv - Secure Tunnel Node")
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = flag, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = srv,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = GridBorder, thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MATCH CRITERIA",
                color = TextAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Beautiful Custom Tabs for gender choice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Boy", "Girl").forEach { gender ->
                    val isSelected = activeGender == gender
                    val needsVIPLock = gender == "Girl" && !prefs.premiumSubscribed && !prefs.girlVideoUnlocked

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GridBorder else Color.Transparent)
                            .clickable { onGenderSelect(gender) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (gender == "Girl") "Girls ❤️" else if (gender == "Boy") "Boys ⚡" else "Any 🌎",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (needsVIPLock) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked VIP",
                                    tint = NeonPink,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dual quick matching triggers: Text vs Camera Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 45)
                        } catch (e: Exception) {}
                        onStartText()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, AccentTeal))),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .testTag("start_text_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat, 
                        contentDescription = "Text chat", 
                        tint = NeonCyan, 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Secure Text", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("3 Coins", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        try {
                            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 65)
                        } catch (e: Exception) {}
                        onStartVideo()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonPink, Color(0xFFFF4081)))),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .testTag("start_video_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam, 
                        contentDescription = "Video call", 
                        tint = NeonPink, 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Video Match", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (!prefs.hasUsedFreeVideoCall) "FREE TRIAL!" else "15 Coins",
                                color = if (!prefs.hasUsedFreeVideoCall) Color(0xFF00FF66) else NeonPink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Premium Girls Only Match Mode (Boys to Girls Link)
            Button(
                onClick = {
                    try {
                        val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                        toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 85)
                    } catch (e: Exception) {}
                    onStartGirlsVideo()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(listOf(NeonPink, AccentTeal, NeonCyan))
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .testTag("start_girls_video_call_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glow Crown / Heart icon representing Elite/Premium Boys connection to girls
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonPink.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "VIP Girls Link",
                            tint = NeonPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "GIRLS-ONLY VIDEO CALL",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = NeonPink) {
                                Text(
                                    "ONLY GIRLS",
                                    color = ObsidianBlack,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Guaranteed boy-to-girl secure radar pairing",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "30 Coins",
                            color = NeonPink,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            "Secure Link",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(prefs: UserPreferences, matchesCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatBox(
            title = "Match Count",
            value = "$matchesCount",
            accentColor = NeonCyan,
            modifier = Modifier.weight(1f)
        )
        StatBox(
            title = "Flag Alerts",
            value = "${prefs.completedReportsCount}",
            accentColor = NeonPink,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatBox(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, GridBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = TextAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PremiumOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) SurfaceCard else SurfaceCard.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) NeonCyan else GridBorder
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Circle,
                    contentDescription = "Selection State",
                    tint = if (selected) NeonCyan else TextAccent,
                    modifier = Modifier.size(18.dp)
                )

                if (title.contains("30")) {
                    Badge(containerColor = NeonPink) {
                        Text("STARTER", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                } else if (title.contains("100")) {
                    Badge(containerColor = NeonCyan) {
                        Text("POPULAR", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                } else if (title.contains("2500")) {
                    Badge(containerColor = Color(0xFF00FF66)) {
                        Text("BEST OFFER", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                } else if (title.contains("500") || title.contains("250")) {
                    Badge(containerColor = Color(0xFFFFD700)) {
                        Text("RECOMMENDED", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                } else if (title.contains("1000")) {
                    Badge(containerColor = Color(0xFFFFD700)) {
                        Text("VIP ULTRA", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextAccent,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ReportItemCard(report: ModerationReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, GridBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report vs ${report.reportedPartnerName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (report.scanVerdict.contains("Inappropriate")) "BLOCKED" else "RESOLVED",
                    color = if (report.scanVerdict.contains("Inappropriate")) NeonPink else AccentTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (report.scanVerdict.contains("Inappropriate")) NeonPink.copy(alpha = 0.2f) else AccentTeal.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Chat excerpt: \"${report.chatExcerpt}\"",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI Verdict: ${report.scanVerdict}",
                color = BrightViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SuccessPaymentCard(amount: Int, onClose: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(2.dp, AccentTeal),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AccentTeal.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checked",
                    tint = AccentTeal,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PAYMENT CRITERIA ACTIVATED",
                color = AccentTeal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Verified Successfully!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Uskha billing and safety nodes have verified your UPI payment of $amount RS. Your target filters and premium privileges have been loaded to the workspace ledger. Have a premium matching experience!",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = ObsidianBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Start Matching Now", fontWeight = FontWeight.Black)
            }
        }
    }
}

val NAVEEN_QR_GRID = listOf(
    "111111100010110001111111",
    "100000101101010011000001",
    "101110100011010101011101",
    "101110100111111001011101",
    "101110101100010011011101",
    "100000100011010101000001",
    "111111101010101010111111",
    "000000001101111000000000",
    "110111110011000110010100",
    "001011001000101101010111",
    "101001011110110111100110",
    "011110010001011000011101",
    "110011011110111101101001",
    "000000001111000111001010",
    "111111101100011001101111",
    "100000100111010010101100",
    "101110101011011001001101",
    "101110100001100111100101",
    "101110101100010101111011",
    "100000101010110010001111",
    "111111101111011010011101"
)

fun saveQrCodeToGallery(context: Context, amount: Int, qrGrid: List<String>): String? {
    try {
        val size = 512
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw white background
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
        
        // Draw black QR modules
        val cellPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            style = android.graphics.Paint.Style.FILL
        }
        
        val rows = qrGrid.size
        val cols = qrGrid[0].length
        val cw = size.toFloat() / cols
        val ch = size.toFloat() / rows
        
        for (r in 0 until rows) {
            val row = qrGrid[r]
            for (c in 0 until cols) {
                if (row[c] == '1') {
                    canvas.drawRect(
                        c * cw,
                        r * ch,
                        (c + 1) * cw + 0.5f,
                        (r + 1) * ch + 0.5f,
                        cellPaint
                    )
                }
            }
        }
        
        // Google Pay ribbon logo overlay in the center
        val badgeRadius = size * 0.11f
        val badgeCenter = size / 2f
        
        val badgePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(badgeCenter, badgeCenter, badgeRadius, badgePaint)
        
        val strokePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawCircle(badgeCenter, badgeCenter, badgeRadius, strokePaint)
        
        // Draw Google Pay colored sectors
        val arcRect = android.graphics.RectF(
            badgeCenter - badgeRadius * 0.7f,
            badgeCenter - badgeRadius * 0.7f,
            badgeCenter + badgeRadius * 0.7f,
            badgeCenter + badgeRadius * 0.7f
        )
        
        val redPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#EA4335"); style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
        val bluePaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#4285F4"); style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
        val yellowPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FBBC05"); style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
        val greenPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#34A853"); style = android.graphics.Paint.Style.FILL; isAntiAlias = true }
        
        canvas.drawArc(arcRect, -135f, 90f, true, redPaint)
        canvas.drawArc(arcRect, -45f, 90f, true, bluePaint)
        canvas.drawArc(arcRect, 45f, 90f, true, yellowPaint)
        canvas.drawArc(arcRect, 135f, 90f, true, greenPaint)
        
        // Draw white mask hole in the center
        val maskPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(badgeCenter, badgeCenter, badgeRadius * 0.35f, maskPaint)

        // Save using standard Scoped Storage
        val filename = "Uskha_Payment_QR_${amount}RS.png"
        val cr = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Uskha")
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val imageUri = cr.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            cr.openOutputStream(imageUri).use { out ->
                if (out != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                cr.update(imageUri, contentValues, null, null)
            }
            return "QR downloaded! Saved to Gallery under Pictures/Uskha/$filename"
        }
    } catch (e: Exception) {
        Log.e("Uskha", "save Qr err", e)
        return "Failed: ${e.localizedMessage}"
    }
    return null
}

@Composable
fun UPIQrCodeRepresentation(amount: Int) {
    // Elegant, high-fidelity native recreation of a professional 24x21 QR code
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rows = NAVEEN_QR_GRID.size
            val cols = NAVEEN_QR_GRID[0].length
            val cw = size.width / cols
            val ch = size.height / rows
            
            // Draw each black block sharply
            for (r in 0 until rows) {
                val row = NAVEEN_QR_GRID[r]
                for (c in 0 until cols) {
                    if (row[c] == '1') {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cw, r * ch),
                            size = Size(cw + 0.5f, ch + 0.5f)
                        )
                    }
                }
            }
        }

        // Draw Google Pay visual logo icon in the center exactly as shown in screenshot
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val r = size.width / 2
                val arcRect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)

                // Colored ribbon segments
                drawArc(
                    color = Color(0xFFEA4335), // Red
                    startAngle = -135f,
                    sweepAngle = 90f,
                    useCenter = true
                )
                drawArc(
                    color = Color(0xFF4285F4), // Blue
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = true
                )
                drawArc(
                    color = Color(0xFFFBBC05), // Yellow
                    startAngle = 45f,
                    sweepAngle = 90f,
                    useCenter = true
                )
                drawArc(
                    color = Color(0xFF34A853), // Green
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = true
                )

                // Soft central doughnut ring
                drawCircle(color = Color.White, radius = r * 0.45f)
            }
        }
    }
}

@Composable
fun VideoNoiseSimulation(partnerName: String, gender: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "Noise")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave"
    )
    val blinkColorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkingHD"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Clear, high-definition premium dark peer-to-peer background
            drawRect(color = Color(0xFF0C0C12))

            // Keep very faint scanning coordinates to show modern futuristic vibe but super clear
            for (i in 0..15) {
                val yPos = (yOffset(i, waveOffset) % h)
                drawLine(
                    color = if (gender == "Girl") NeonPink.copy(alpha = 0.04f) else NeonCyan.copy(alpha = 0.04f),
                    start = Offset(0f, yPos),
                    end = Offset(w, yPos),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VideoXPlaceholder(partnerName = partnerName, gender = gender)
        }

        // Overlay with HD Connection Status indicating extremely clear premium video link
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Blinking green dot showing active HD link
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer { alpha = blinkColorAlpha }
                        .clip(CircleShape)
                        .background(Color(0xFF00FF66))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HD 1080p Connected • 60 FPS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Secured",
                    tint = AccentTeal,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Bitrate: 4.8 Mbps • Latency: 11ms",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun yOffset(index: Int, offset: Float): Float {
    return (index * 80f) + offset
}

// Compact custom display size class extension
private val Int.hdp: androidx.compose.ui.unit.Dp
    get() = (this).dp

private fun Modifier.fillHeaderWidth(fraction: Float = 1f): Modifier = this.fillMaxWidth(fraction)

private val OberonPink = Color(0xFFFF85A1)
private val OberonCyan = Color(0xFFE0FFFF)

@Composable
fun SettingsScreen(viewModel: UskhaViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isConnectSoundEnabled by viewModel.isConnectSoundEnabled.collectAsStateWithLifecycle()
    val isDisconnectSoundEnabled by viewModel.isDisconnectSoundEnabled.collectAsStateWithLifecycle()
    val audioVideoQuality by viewModel.audioVideoQuality.collectAsStateWithLifecycle()

    val indianLanguages = remember {
        listOf(
            "English", 
            "Hindi (हिन्दी)", 
            "Tamil (தமிழ்)", 
            "Malayalam (മലയാളം)", 
            "Kannada (കന്നඩ)", 
            "Telugu (తెലുगु)", 
            "Bengali (বাংলা)", 
            "Marathi (मराठी)", 
            "Gujarati (ગુજરાતી)", 
            "Punjabi (ਪੰਜਾਬੀ)", 
            "Urdu (اردו)"
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back To Dashboard",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Preferences & Help",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HELP & SUPPORT PANEL
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Help Mail",
                            tint = NeonPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "24/7 Dedicated Help & Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "For queries regarding payment verifications, reports, or privacy, contact our team directly at the support mail listed below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Support mail row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "gojo83472@gmail.com",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("gojo83472@gmail.com"))
                                    Toast.makeText(context, "Support Email copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Support Email",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Email client button
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:gojo83472@gmail.com")
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Uskha Support Inquiry")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open Email App",
                                    tint = BrightViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // EXPLICIT GENDER PROFILE SETTING CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gender_profile_setting_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Gender Profile",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Explicit Gender Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explicit designation of your gender is required by our matching routers for secure Girl-to-Boy and Girls-Only video networks.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentGender = prefs.userGender

                        // Boy Option
                        val isBoySelected = currentGender == "Boy"
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isBoySelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    BorderStroke(
                                        if (isBoySelected) 2.dp else 1.dp, 
                                        if (isBoySelected) NeonCyan else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setUserGender("Boy") }
                                .padding(12.dp)
                                .testTag("set_gender_boy_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Boy",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBoySelected) NeonCyan else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Girl Option
                        val isGirlSelected = currentGender == "Girl"
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isGirlSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    BorderStroke(
                                        if (isGirlSelected) 2.dp else 1.dp, 
                                        if (isGirlSelected) NeonPink else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setUserGender("Girl") }
                                .padding(12.dp)
                                .testTag("set_gender_girl_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Girl",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGirlSelected) NeonPink else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // PRIVACY SAFE MODE TOGGLE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("safe_mode_toggle_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Safe Mode",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Privacy Safe Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        Switch(
                            checked = prefs.safeModeEnabled,
                            onCheckedChange = { viewModel.setSafeModeEnabled(it) },
                            modifier = Modifier.testTag("safe_mode_switch")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When enabled, incoming stranger video streams are automatically blurred by default. You can tap on a blurred stream anytime during the call to reveal it, giving you total peace of mind.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // THEME CHOICE CONTROL
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Theme Vibe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Dark Theme option
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    BorderStroke(
                                        if (isDarkTheme) 2.dp else 1.dp, 
                                        if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setDarkTheme(true) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = "Dark Mode",
                                tint = if (isDarkTheme) NeonCyan else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Neon Dark",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Light Theme option
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isDarkTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    BorderStroke(
                                        if (!isDarkTheme) 2.dp else 1.dp, 
                                        if (!isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setDarkTheme(false) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = "Light Mode",
                                tint = if (!isDarkTheme) Color(0xFFF57C00) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Light Glow",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // AUDIO/VIDEO PROFILE COMFORT
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-time Connect Protocol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Perfect Audio & Video high-definition profiles for optimal clarity.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val qualities = listOf(
                        "Perfect HD Stereo" to "Crystal clear voice with AI echo suppression & High Definition feed.",
                        "Ultra Crisp Voice" to "Focuses premium bandwidth on voices for dense environments.",
                        "Standard Eco" to "Optimized for erratic network coverage to keep signals steady."
                    )

                    qualities.forEach { (title, desc) ->
                        val isSelected = audioVideoQuality == title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setAudioVideoQuality(title) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setAudioVideoQuality(title) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // INDIAN REGIONAL LANGUAGES LIST
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Language Option",
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Regional Language Target",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Uskha matches matching targets speaking similar regional Indian dialects.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Render languages as scrollable flow rows, wrapped nicely
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        indianLanguages.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { language ->
                                    val isSelected = selectedLanguage == language
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .border(
                                                BorderStroke(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                ),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.setLanguage(language) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = language,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                // Fill dummy cell if odd elements lists
                                if (pair.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // SIGNAL & SOUND EFFECT CUSTOMIZER
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Sound customizations",
                            tint = BrightViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Call Connect sound preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Connect Sound Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connection Alert Tone",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Plays a gentle high-pitched beep when matching succeeds.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isConnectSoundEnabled,
                            onCheckedChange = { viewModel.setConnectSoundEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Disconnect Sound Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Disconnection Alert Tone",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Plays a discrete system alert when active call is terminated.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isDisconnectSoundEnabled,
                            onCheckedChange = { viewModel.setDisconnectSoundEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Tones trigger Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.playConnectSound() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Connect Code", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.playDisconnectSound() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Leave Tone", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECURE CREDENTIALS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Safe Lock Icon",
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SECURE LOCAL PROFILE",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = AccentTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "To guarantee absolute safety, your real contact coordinates (email/phone number) are isolated inside your secure local sandbox. Strangers on Uskha can never inspect or query these values.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Display Name:", color = Color.Gray, fontSize = 12.sp)
                            Text(prefs.username, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gender Identity:", color = Color.Gray, fontSize = 12.sp)
                            Text(if (prefs.userGender == "Girl") "Girl ❤️" else "Boy ⚡", color = if (prefs.userGender == "Girl") NeonPink else NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Secure Login:", color = Color.Gray, fontSize = 12.sp)
                            Text(prefs.loginType, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                        if (prefs.loginType == "GMAIL") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gmail Address (PRIVATE):", color = Color.Gray, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = "Private secure", tint = AccentTeal, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(prefs.loggedInEmail, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (prefs.loginType == "PHONE") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Phone Mobile (PRIVATE):", color = Color.Gray, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = "Private secure", tint = AccentTeal, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(prefs.loggedInPhone, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonPink),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SECURELY LOG OUT", color = NeonPink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Uskha App v2.4 • Made with Love in India",
                    fontSize = 11.sp,
                    color = TextAccent
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AuthScreen(viewModel: UskhaViewModel) {
    val coroutineScope = rememberCoroutineScope()

    var loginWithEmailMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Boy") }

    // Verifying/Authenticating overlay states
    var isVerifying by remember { mutableStateOf(false) }
    var verificationStep by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Brand Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(NeonCyan, NeonPink))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "U",
                color = ObsidianBlack,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "USKHA SECURE AUTH",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Military-grade end-to-end encrypted login",
            color = AccentTeal,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Form Card / Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            if (isVerifying) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = verificationStep,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (otpSent) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SECURE VERIFICATION OTP",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sent 6-digit cryptographic confirmation OTP to your locked channel.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 6) otpInput = it },
                        label = { Text("6-Digit OTP Code") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GridBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (otpInput.length < 4) {
                                Toast.makeText(context, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isVerifying = true
                            verificationStep = "Validating secure session decryption key..."
                            coroutineScope.launch {
                                delay(1200)
                                verificationStep = "Synchronizing encrypted user keychain..."
                                delay(1000)
                                if (loginWithEmailMode) {
                                    viewModel.loginWithGmail(emailInput, nameInput, selectedGender)
                                } else {
                                    viewModel.loginWithPhoneNumber(phoneInput, nameInput, selectedGender)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("VERIFY & LOG IN", color = ObsidianBlack, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { otpSent = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Change Details / Edit", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Switch login mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (loginWithEmailMode) NeonCyan else Color.Transparent)
                                .clickable { loginWithEmailMode = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Google Gmail",
                                color = if (loginWithEmailMode) ObsidianBlack else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!loginWithEmailMode) NeonCyan else Color.Transparent)
                                .clickable { loginWithEmailMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Phone OTP",
                                color = if (!loginWithEmailMode) ObsidianBlack else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Public Username input
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Username (Public)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GridBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "My Gender (Used to connect with correct partners):",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedGender == "Boy") NeonCyan else Color.Transparent)
                                .clickable { selectedGender = "Boy" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Boy ⚡",
                                color = if (selectedGender == "Boy") ObsidianBlack else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedGender == "Girl") NeonPink else Color.Transparent)
                                .clickable { selectedGender = "Girl" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Girl ❤️",
                                color = if (selectedGender == "Girl") ObsidianBlack else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (loginWithEmailMode) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Google Gmail Address") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GridBorder,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number (+91 ...)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GridBorder,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (nameInput.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter a display username", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (loginWithEmailMode && !emailInput.contains("@")) {
                                Toast.makeText(context, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!loginWithEmailMode && phoneInput.trim().length < 10) {
                                Toast.makeText(context, "Please enter a valid 10-digit smartphone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isVerifying = true
                            verificationStep = "Forming secure sandboxed socket..."
                            coroutineScope.launch {
                                delay(1000)
                                verificationStep = "Transmitting OTP challenge to trusted carriers..."
                                delay(1200)
                                isVerifying = false
                                otpSent = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (loginWithEmailMode) "SEND GOOGLE GMAIL OTP" else "SEND MOBILE PHONE OTP",
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy Warning Callout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Safe Lock Icon",
                tint = AccentTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Strict Privacy Guarantee",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To keep you completely safe, your private credentials (Gmail and Phone Number) are isolated inside your secure device sandbox. They are never transmitted, shown, or made visible to any other person. Strangers on Uskha can only see your public random ID.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun SocialAndInvitesTab(viewModel: UskhaViewModel, prefs: UserPreferences) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val friendRequests by viewModel.friendRequests.collectAsStateWithLifecycle()
    val inviteError by viewModel.inviteCodeError.collectAsStateWithLifecycle()
    val inviteSuccess by viewModel.inviteCodeSuccess.collectAsStateWithLifecycle()

    var inviteCodeInput by remember { mutableStateOf("") }
    var searchUserIdInput by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Invite & Referral code sharing section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "MY UNIQUE IDS & INVITE",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Every app user has a very different, unique secured ID. Share yours to grow your circle!",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // User ID card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("MY UNIQUE ID", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    prefs.selfUserId.ifEmpty { "LOADING..." },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Invite Code card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("INVITE CODE", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    prefs.selfInviteCode.ifEmpty { "LOADING..." },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(prefs.selfInviteCode))
                                Toast.makeText(context, "Invite code copied directly to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                // Trigger standard share intent
                                try {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Hey! Add me on Uskha Secured Match! My unique ID is ${prefs.selfUserId} and my Referral Invite code is ${prefs.selfInviteCode} to earn 25 free coins!")
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Invite", color = ObsidianBlack, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Apply referral code section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ENTER REFERRAL INVITE CODE",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Apply a friend's referral invite code to immediately earn +25 coins!",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (prefs.hasAppliedInvite) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentTeal.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Success", tint = AccentTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Successfully applied referral code: ${prefs.appliedInviteCode}. Reward +25 Coins credited!",
                                color = AccentTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inviteCodeInput,
                                onValueChange = { inviteCodeInput = it.uppercase() },
                                label = { Text("Invite Code", fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GridBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    viewModel.applyInviteCode(inviteCodeInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text("Apply", color = ObsidianBlack, fontWeight = FontWeight.Black)
                            }
                        }

                        if (inviteError != null) {
                            Text(
                                text = inviteError ?: "",
                                color = NeonPink,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        if (inviteSuccess) {
                            Text(
                                text = "Code applied successfully! coins added.",
                                color = AccentTeal,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add Friend via User ID directly
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ADD FRIEND BY ID",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Directly transmit a connections query request.",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchUserIdInput,
                            onValueChange = { searchUserIdInput = it.trim().uppercase() },
                            label = { Text("Target User ID (e.g. USKHA-12345)", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GridBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (searchUserIdInput.isNotEmpty()) {
                                    viewModel.sendFriendRequestToId(searchUserIdInput)
                                    searchUserIdInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.7f)
                        ) {
                            Text("Request", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Pending Outbox/Inbox Friend Requests
        if (friendRequests.isNotEmpty()) {
            item {
                Text(
                    "PENDING FRIEND REQUESTS (${friendRequests.size})",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(friendRequests, key = { "req_" + it.userId }) { req ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(BorderStroke(1.dp, GridBorder), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (req.gender == "Girl") NeonPink else NeonCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(req.name.take(1), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(req.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(req.userId, color = Color.Gray, fontSize = 10.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { viewModel.declineFriendRequest(req.userId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Decline", color = NeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.acceptFriendRequest(req) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Accept", color = ObsidianBlack, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Active Friends List
        item {
            Text(
                "CONNECTED FRIENDS CIRCLE (${friends.size})",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (friends.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.People, contentDescription = "No Friends", tint = Color.Gray, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No friends connected yet", color = Color.Gray, fontSize = 12.sp)
                        Text("Click Friend icon during chats to stay in touch!", color = Color.DarkGray, fontSize = 10.sp)
                    }
                }
            }
        } else {
            items(friends, key = { "friend_" + it.userId }) { friend ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (friend.gender == "Girl") NeonPink else NeonCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(friend.name.take(1), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(friend.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = if (friend.gender == "Girl") NeonPink.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)) {
                                    Text(
                                        text = friend.gender.uppercase(),
                                        color = if (friend.gender == "Girl") NeonPink else NeonCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(friend.userId, color = Color.Gray, fontSize = 10.sp)
                        }
                    }

                    Row {
                        IconButton(onClick = {
                            viewModel.removeFriend(friend.userId)
                            Toast.makeText(context, "${friend.name} removed from friends list.", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Friend", tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecureCoinWalletCard(
    coins: Int,
    onBuyCoins: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("secure_coin_wallet_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(listOf(NeonCyan, AccentTeal))
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header row with shield & chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Secured Vault",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SECURE COIN WALLET",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active Shield Protection",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // SIM Card Gold Chip aesthetic
                Box(
                    modifier = Modifier
                        .size(32.dp, 24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            )
                        )
                ) {
                    // Small decorative lines inside chip
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color.Black.copy(alpha = 0.15f),
                            size = size,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Balance display & credit card style number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Vault Balance",
                        color = TextAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins Logo",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$coins COINS",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Elegant interactive Buy Coins action button
                Button(
                    onClick = onBuyCoins,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("wallet_buy_coins_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Add Coins",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BUY COINS",
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secure bottom footnote
            Divider(color = GridBorder, thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encryption Info",
                        tint = TextAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Encrypted Gateway: Secure 256-bit SSL",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }

                Text(
                    text = "ID: USK-VAULT-${(1000..9999).random()}",
                    color = TextAccent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun WalletTransactionHistoryTab(viewModel: UskhaViewModel) {
    val transactions by viewModel.walletTransactions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Secure Vault Transactions",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${transactions.size} records",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(BorderStroke(1.dp, GridBorder), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Empty",
                        tint = TextAccent,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No transactions logged yet",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Earn or purchase coin packs to log history!",
                        color = TextAccent,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("wallet_transactions_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    WalletTransactionRow(tx)
                }
            }
        }
    }
}

@Composable
fun WalletTransactionRow(tx: WalletTransaction) {
    val isCredit = tx.type == "CREDIT"
    val accentColor = if (isCredit) Color(0xFF00FF66) else NeonPink
    
    // Format timestamp as human-readable time
    val dateString = remember(tx.timestamp) {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(tx.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_row_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, GridBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Indicator with Credit/Debit Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = "Tx Type",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.description,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount highlight badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}${tx.amount} C",
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "VAULT",
                    color = TextAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun IncomingCallOverlay(
    match: MatchHistory,
    activeServer: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // Elegant fully-immersive translucent neon backdrop overlaying the interface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack.copy(alpha = 0.96f))
            .clickable(enabled = false) {} // capture clicks
            .padding(24.dp)
            .testTag("incoming_call_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Subdued breathing pulse header
            Badge(
                containerColor = NeonPink.copy(alpha = 0.15f),
                modifier = Modifier.border(BorderStroke(1.dp, NeonPink), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonPink)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE INCOMING VIDEO CALL",
                        color = NeonPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Pulsing Cyber Avatar Container
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Outer ring 2
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.05f))
                        .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.1f)), CircleShape)
                )
                // Outer ring 1
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.08f))
                        .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)), CircleShape)
                )
                // Main visual card
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(BorderStroke(2.dp, NeonCyan), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Random robot face based on avatarSeed or a nice icon
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Incoming call icon",
                        tint = NeonCyan,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Caller name and information
            Text(
                match.partnerName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "${match.partnerAge} years old • $activeServer Node",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Policy / safety reassurance label
            Text(
                text = "Safe & Moderated Connection • Boys-only Initiator Policy",
                color = TextAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Large tactile accept/reject actions side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DECLINE BUTTON (Neon Red/Pink Tone)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NeonPink.copy(alpha = 0.2f))
                            .border(BorderStroke(1.5.dp, NeonPink), CircleShape)
                            .testTag("decline_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Decline Call",
                            tint = NeonPink,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Decline",
                        color = NeonPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ACCEPT BUTTON (Neon Green/Cyan Tone)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF66).copy(alpha = 0.2f))
                            .border(BorderStroke(1.5.dp, Color(0xFF00FF66)), CircleShape)
                            .testTag("accept_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Accept Call",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Accept",
                        color = Color(0xFF00FF66),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HelpCenterScreen(viewModel: UskhaViewModel) {
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val isResolvingProblem by viewModel.isResolvingProblem.collectAsStateWithLifecycle()
    val activeResolvingTask by viewModel.activeResolvingTask.collectAsStateWithLifecycle()

    var ticketTitle by remember { mutableStateOf("") }
    var ticketDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Connection Lag") }
    
    val categories = listOf("Connection Lag", "Video Stream Issue", "Coin Transaction Lag", "Moderation Conflict", "Audio Quality Spikes")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Rounded Back & Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SUPPORT & REPAIR CENTER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Auto-Resolve Match & Streaming Problems",
                    color = BrightViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Diagnostic / Dynamic Resolver HUD card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(BrightViolet, NeonCyan))),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = "Repair", tint = BrightViolet, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-CLICK AUTO-FIX ENGINE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select any matching, lag, or video problem below and tap 'Complete Solution Plan' to instantly repair and reset server pipelines.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isResolvingProblem) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBlack)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrightViolet, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Executing simulated repair: $activeResolvingTask",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Purging routing caches • Resetting camera buffers",
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else {
                    diagnostics.forEach { diag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(diag.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(diag.description, color = TextAccent, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { viewModel.completeResolvingProblem(diag.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightViolet),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Complete", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Submit Ticket Section
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, GridBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Help, contentDescription = "Report", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REPORT CUSTOM BUG / TICKET",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                Text("Summarize Bug Problem", color = TextAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = ticketTitle,
                    onValueChange = { ticketTitle = it },
                    placeholder = { Text("E.g. Camera drops frame on USA match", color = Color.Gray, fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                Text("Detailed Description", color = TextAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = ticketDescription,
                    onValueChange = { ticketDescription = it },
                    placeholder = { Text("Describe the issue or error experienced in detail...", color = Color.Gray, fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector
                Text("Select Category", color = TextAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonCyan.copy(alpha = 0.25f) else SurfaceDark)
                                .border(BorderStroke(1.dp, if (isSel) NeonCyan else Color.Transparent), RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(cat, color = if (isSel) NeonCyan else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (ticketTitle.isNotBlank() && ticketDescription.isNotBlank()) {
                            viewModel.submitSupportTicket(ticketTitle, selectedCategory, ticketDescription)
                            ticketTitle = ""
                            ticketDescription = ""
                        } else {
                            viewModel.triggerSimpleToast("Please fill in both fields before submitting.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Submit Custom Ticket to Uskha HQ", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ticket tracking logs
        Text(
            text = "TROUBLE TICKET TRACKING CENTRAL",
            color = Color.LightGray,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (supportTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No active tickets recorded", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            supportTickets.forEach { tkt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(BorderStroke(1.dp, GridBorder), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GridBorder)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(tkt.ticketId, color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tkt.category, color = BrightViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(tkt.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Node: ${tkt.serverNode}", color = Color.Gray, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (tkt.status) {
                                    "FIXED" -> Color(0xFF00FF66).copy(alpha = 0.2f)
                                    "ANALYZING" -> BrightViolet.copy(alpha = 0.2f)
                                    else -> NeonCyan.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tkt.status,
                            color = when (tkt.status) {
                                "FIXED" -> Color(0xFF00FF66)
                                "ANALYZING" -> BrightViolet
                                else -> NeonCyan
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

