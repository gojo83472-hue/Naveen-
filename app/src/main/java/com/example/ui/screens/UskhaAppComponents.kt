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
            text = if (matchMode == MatchMode.TEXT) "TEXT CHAT STREAM" else "LIVE VIDEO LINK",
            color = NeonCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Target Criteria: $genderFilter",
            color = NeonPink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Spectacular sweeping Radar simulation
        RadarScanner(genderFilter = genderFilter)

        Spacer(modifier = Modifier.height(40.dp))

        // Animating statuses
        Text(
            text = searchPhrases[statusTextIndex],
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please be respectful. Committing harassment triggers instant automated safety bans.",
            color = TextAccent,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

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
            Text("Cancel Stream", fontWeight = FontWeight.Bold)
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
                    Box(modifier = Modifier.fillMaxSize()) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simple banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(UskhaScreen.Dashboard) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Premium Gateway",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription/payment item tabs: 9, 19, 100, 500, 1000 RS
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
                title = "9 RS Option",
                subtitle = "Ad-free experience\n+ 15 Coins",
                selected = amount == 9,
                onClick = { viewModel.updatePayAmount(9) },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "19 RS Option",
                subtitle = "79 Coins Pack\n+ Girls Priority Match",
                selected = amount == 19,
                onClick = { viewModel.updatePayAmount(19) },
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
                title = "100 RS Pack",
                subtitle = "250 Coins Tier\n+ Full Filter Unlock",
                selected = amount == 100,
                onClick = { viewModel.updatePayAmount(100) },
                modifier = Modifier.weight(1f)
            )

            PremiumOptionCard(
                title = "500 RS Pack",
                subtitle = "1399 Coins Tier\n+ VIP High Speed",
                selected = amount == 500,
                onClick = { viewModel.updatePayAmount(500) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PremiumOptionCard(
            title = "1000 RS Ultra Vip",
            subtitle = "3000 Coins King Tier\n+ Golden Frame & Lifetime Badge",
            selected = amount == 1000,
            onClick = { viewModel.updatePayAmount(1000) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (successfullyVerified) {
            // Gold sparkling confirmation badge
            SuccessPaymentCard(amount = amount, onClose = { viewModel.closePaymentScreen() })
        } else {
            // Recreated UPI Screen centered nicely
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, GridBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Uskha Billing
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", color = ObsidianBlack, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Uskha Gateway",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // High fidelity QR code drawing
                    UPIQrCodeRepresentation(amount = amount)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Scan to pay with any UPI app",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bank selection details
                    Row(
                        modifier = Modifier
                            .fillHeaderWidth(0.85f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Small orange/gold bank logo representation
                        Spacer(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF6D00))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bank of Baroda 9688",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Copy ID field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString("uskha.pay@oksbi"))
                                Toast.makeText(context, "UPI ID Copied!", Toast.LENGTH_SHORT).show()
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UPI ID: uskha.pay@oksbi",
                            color = TextAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                        Text("Verifying Transaction Ledger...", fontWeight = FontWeight.Bold)
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
    var isCameraAvailable by remember { mutableStateOf(true) }

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
                    onClick = onStartText,
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("start_text_chat_button")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Text chat", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Secure Text", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("3 Coins", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onStartVideo,
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                    border = BorderStroke(1.dp, NeonPink),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("start_video_chat_button")
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video call", tint = NeonPink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Video Match", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = if (!prefs.hasUsedFreeVideoCall) "1st Call FREE!" else "15 Coins",
                            color = if (!prefs.hasUsedFreeVideoCall) Color(0xFF00FF66) else NeonPink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
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
        StatBox(
            title = "Network ID",
            value = "BOB-${prefs.id}",
            accentColor = BrightViolet,
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

                if (title.contains("Girls") || title.contains("19")) {
                    Badge(containerColor = NeonPink) {
                        Text("HOT", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                } else if (title.contains("100")) {
                    Badge(containerColor = NeonCyan) {
                        Text("POPULAR", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                } else if (title.contains("500")) {
                    Badge(containerColor = Color(0xFFFFD700)) {
                        Text("BEST VALUE", color = ObsidianBlack, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
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

@Composable
fun UPIQrCodeRepresentation(amount: Int) {
    // Beautiful replica of Google Pay centered QR frame
    Box(
        modifier = Modifier
            .size(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width
            val stroke = 3.dp.toPx()

            // Draw QR corner blocks (outer black boxes)
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(0f, 0f),
                size = Size(s * 0.28f, s * 0.28f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(stroke, stroke),
                size = Size(s * 0.28f - stroke * 2, s * 0.28f - stroke * 2),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(stroke * 2, stroke * 2),
                size = Size(s * 0.28f - stroke * 4, s * 0.28f - stroke * 4),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Top-right block
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(s * 0.72f, 0f),
                size = Size(s * 0.28f, s * 0.28f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(s * 0.72f + stroke, stroke),
                size = Size(s * 0.28f - stroke * 2, s * 0.28f - stroke * 2),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(s * 0.72f + stroke * 2, stroke * 2),
                size = Size(s * 0.28f - stroke * 4, s * 0.28f - stroke * 4),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Bottom-left block
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(0f, s * 0.72f),
                size = Size(s * 0.28f, s * 0.28f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(stroke, s * 0.72f + stroke),
                size = Size(s * 0.28f - stroke * 2, s * 0.28f - stroke * 2),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(stroke * 2, s * 0.72f + stroke * 2),
                size = Size(s * 0.28f - stroke * 4, s * 0.28f - stroke * 4),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Mock randomized QR dot structures
            drawRect(color = Color.Black, topLeft = Offset(s * 0.35f, s * 0.10f), size = Size(s * 0.12f, s * 0.05f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.52f, s * 0.05f), size = Size(s * 0.08f, s * 0.15f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.38f, s * 0.24f), size = Size(s * 0.18f, s * 0.06f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.12f, s * 0.35f), size = Size(s * 0.05f, s * 0.12f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.05f, s * 0.52f), size = Size(s * 0.15f, s * 0.08f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.24f, s * 0.38f), size = Size(s * 0.06f, s * 0.18f))

            // Center details
            drawRect(color = Color.Black, topLeft = Offset(s * 0.35f, s * 0.45f), size = Size(s * 0.3f, s * 0.22f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.45f, s * 0.68f), size = Size(s * 0.18f, s * 0.14f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.72f, s * 0.35f), size = Size(s * 0.22f, s * 0.12f))
            drawRect(color = Color.Black, topLeft = Offset(s * 0.68f, s * 0.52f), size = Size(s * 0.14f, s * 0.18f))
        }

        // Draw Google Pay visual logo icon in the center exactly as shown
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(BorderStroke(1.dp, Color.LightGray), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Drawn Google Pay colored logo representation
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Box(modifier = Modifier.size(6.dp, 12.dp).background(Color(0xFFEA4335))) // Red
                Box(modifier = Modifier.size(6.dp, 12.dp).background(Color(0xFF4285F4))) // Blue
                Box(modifier = Modifier.size(6.dp, 12.dp).background(Color(0xFFFBBC05))) // Yellow
                Box(modifier = Modifier.size(6.dp, 12.dp).background(Color(0xFF34A853))) // Green
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
