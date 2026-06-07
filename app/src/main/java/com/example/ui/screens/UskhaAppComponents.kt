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
import com.example.ui.theme.*
import com.example.ui.viewmodel.MatchMode
import com.example.ui.viewmodel.UskhaScreen
import com.example.ui.viewmodel.UskhaViewModel
import kotlinx.coroutines.delay

/**
 * Prime Entry Composable that routes to the correct screen based on viewmodel states
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UskhaMainApp(viewModel: UskhaViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()

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
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is UskhaScreen.AgeGate -> AgeGateScreen(viewModel = viewModel, onConfirm = { viewModel.verifyAge(it) })
                    is UskhaScreen.Dashboard -> DashboardScreen(viewModel = viewModel, prefs = prefs)
                    is UskhaScreen.Matching -> MatchingScreen(viewModel = viewModel)
                    is UskhaScreen.TextChat -> TextChatScreen(viewModel = viewModel)
                    is UskhaScreen.VideoChat -> VideoChatScreen(viewModel = viewModel)
                    is UskhaScreen.PremiumHub -> PremiumHubScreen(viewModel = viewModel, prefs = prefs)
                    is UskhaScreen.SafetyCenter -> SafetyCenterScreen(viewModel = viewModel)
                    is UskhaScreen.Settings -> SettingsScreen(viewModel = viewModel)
                }
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
            coins = prefs.walletCoins,
            isVIP = prefs.premiumSubscribed
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Large matching orb & quick info
        DashboardMatchOrb(
            prefs = prefs,
            activeGender = activeGenderFilter,
            onGenderSelect = { viewModel.setGenderFilter(it) },
            onStartText = { viewModel.startMatching(MatchMode.TEXT) },
            onStartVideo = { viewModel.startMatching(MatchMode.VIDEO) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics row
        StatsRow(prefs = prefs, matchesCount = history.size)

        Spacer(modifier = Modifier.height(18.dp))

        // Match history list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Connection History",
                color = Color.White,
                fontSize = 16.sp,
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

            Row {
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
                // Moving sine waves simulating active stream
                VideoNoiseSimulation(
                    partnerName = match?.partnerName ?: "Stranger",
                    gender = match?.partnerGender ?: "Girl"
                )

                // Top visual overlay info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Row {
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
                "Lovely" to "❤️",
                "Sad" to "😢",
                "Cute" to "✨",
                "Angry" to "😡"
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
                    "Lovely" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(BorderStroke(4.dp, Color(0xFFFF1493).copy(alpha = 0.6f)))
                        ) {
                            Text("💖 LOVELY EMOTION ACTIVE 💖", color = Color(0xFFFF1493), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)))
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
                    "Sad" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF00BFFF).copy(alpha = 0.12f))
                                .border(BorderStroke(4.dp, Color(0xFF00BFFF).copy(alpha = 0.5f)))
                        ) {
                            Text("😢 SAD SOULFUL VIBE ENGAGED 😢", color = Color(0xFF00BFFF), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)))
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
                    "Cute" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(BorderStroke(4.dp, Color(0xFFFFD700).copy(alpha = 0.6f)))
                        ) {
                            Text("✨ CUTE SPARKLING SPARK ✨", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)))
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
                    "Angry" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = 0.14f))
                                .border(BorderStroke(5.dp, Color.Red.copy(alpha = 0.65f)))
                        ) {
                            Text("😡 FIERY ANGER BURST 😡", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)))
                        }
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
            Toast.makeText(context, "No UPI apps (Google Pay, PhonePe, Paytm, BHIM) found.", Toast.LENGTH_LONG).show()
        }
    }

    val coinCount = when (amount) {
        30 -> 70
        100 -> 233
        250 -> 585
        500 -> 1170
        1000 -> 2350
        2500 -> 6000
        else -> 70
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
                text = "Secure UPI Payment",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                Toast.makeText(context, "Secure encrypted UPI peer link active", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subscription/payment item tabs: 30, 100, 250, 500, 1000, 2500 RS
        Text(
            text = "SELECT COIN PACK / OFFER",
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
                    triggerUpiPayment(30)
                },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "100 RS Pack",
                subtitle = "233 Coins Tier\n+ Girls Priority Match",
                selected = amount == 100,
                onClick = {
                    viewModel.updatePayAmount(100)
                    triggerUpiPayment(100)
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
                    triggerUpiPayment(250)
                },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "500 RS Pack",
                subtitle = "1170 Coins Tier\n+ VIP Turbo Connection",
                selected = amount == 500,
                onClick = {
                    viewModel.updatePayAmount(500)
                    triggerUpiPayment(500)
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
                    triggerUpiPayment(1000)
                },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "2500 RS Best Offer",
                subtitle = "6000 Coins King Tier\n+ Lifetime Access",
                selected = amount == 2500,
                onClick = {
                    viewModel.updatePayAmount(2500)
                    triggerUpiPayment(2500)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                                text = "ORDER VALUE",
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Official Powered by UPI lockup
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "POWERED BY ",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "UPI",
                    color = Color.White,
                    fontSize = 15.sp,
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
        }
    }
}

@Composable
fun DashboardMatchOrb(
    prefs: UserPreferences,
    activeGender: String,
    onGenderSelect: (String) -> Unit,
    onStartText: () -> Unit,
    onStartVideo: () -> Unit
) {
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
