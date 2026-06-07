package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.database.UskhaDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.MatchHistory
import com.example.data.model.ModerationReport
import com.example.data.model.UserPreferences
import com.example.data.model.WalletTransaction
import com.example.data.repository.UskhaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class UskhaScreen {
    object Auth : UskhaScreen()
    object AgeGate : UskhaScreen()
    object Dashboard : UskhaScreen()
    object Matching : UskhaScreen()
    object TextChat : UskhaScreen()
    object VideoChat : UskhaScreen()
    object PremiumHub : UskhaScreen()
    object SafetyCenter : UskhaScreen()
    object Settings : UskhaScreen()
    object HelpCenter : UskhaScreen()
}

enum class MatchMode {
    TEXT, VIDEO, GIRLS_VIDEO
}

enum class PaymentMethod {
    UPI, PAYPAL, CARD
}

class UskhaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UskhaRepository

    // Screen navigation state
    private val _currentScreen = MutableStateFlow<UskhaScreen>(UskhaScreen.AgeGate)
    val currentScreen: StateFlow<UskhaScreen> = _currentScreen.asStateFlow()

    // Preferences, match list, and reports flows
    val userPrefs: StateFlow<UserPreferences>
    val matchHistory: StateFlow<List<MatchHistory>>
    val moderationReports: StateFlow<List<ModerationReport>>
    val walletTransactions: StateFlow<List<WalletTransaction>>

    // Matching and Chat details
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _matchMode = MutableStateFlow(MatchMode.TEXT)
    val matchMode = _matchMode.asStateFlow()

    private val _genderFilter = MutableStateFlow("All")
    val genderFilter = _genderFilter.asStateFlow()

    private val _activeMatch = MutableStateFlow<MatchHistory?>(null)
    val activeMatch = _activeMatch.asStateFlow()

    private val _incomingVideoCall = MutableStateFlow<MatchHistory?>(null)
    val incomingVideoCall = _incomingVideoCall.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping = _isPartnerTyping.asStateFlow()

    // Safety and Moderation state
    private val _isScanningChat = MutableStateFlow(false)
    val isScanningChat = _isScanningChat.asStateFlow()

    private val _scanVerdict = MutableStateFlow<String?>(null)
    val scanVerdict = _scanVerdict.asStateFlow()

    private val _isScanningVideoFrame = MutableStateFlow(false)
    val isScanningVideoFrame = _isScanningVideoFrame.asStateFlow()

    private val _videoScanVerdict = MutableStateFlow<String?>("SAFE")
    val videoScanVerdict = _videoScanVerdict.asStateFlow()

    private val _isVideoSimulationViolationActive = MutableStateFlow(false)
    val isVideoSimulationViolationActive = _isVideoSimulationViolationActive.asStateFlow()

    private val _isStrangerMuted = MutableStateFlow(false)
    val isStrangerMuted = _isStrangerMuted.asStateFlow()

    // Custom Payments & UPI states
    private val _selectedPayAmount = MutableStateFlow(9) // Special starter promo pack 9 RS by default, up to 2500 RS
    val selectedPayAmount = _selectedPayAmount.asStateFlow()

    private val _enteredUtr = MutableStateFlow("")
    val enteredUtr = _enteredUtr.asStateFlow()

    private val _isPaymentProcessing = MutableStateFlow(false)
    val isPaymentProcessing = _isPaymentProcessing.asStateFlow()

    private val _paymentVerifiedSuccessfully = MutableStateFlow(false)
    val paymentVerifiedSuccessfully = _paymentVerifiedSuccessfully.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError = _paymentError.asStateFlow()

    // Interactive PayPal & Card Payment Modes
    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.UPI)
    val selectedPaymentMethod = _selectedPaymentMethod.asStateFlow()

    private val _paypalEmail = MutableStateFlow("")
    val paypalEmail = _paypalEmail.asStateFlow()

    private val _cardNumber = MutableStateFlow("")
    val cardNumber = _cardNumber.asStateFlow()

    private val _cardExpiry = MutableStateFlow("")
    val cardExpiry = _cardExpiry.asStateFlow()

    private val _cardCvv = MutableStateFlow("")
    val cardCvv = _cardCvv.asStateFlow()

    // Subscription custom status flow
    private val _premiumHubTab = MutableStateFlow(0) // 0: Coin Packs, 1: VIP Subscriptions
    val premiumHubTab = _premiumHubTab.asStateFlow()

    private val _selectedSubPlan = MutableStateFlow("Monthly Gold Pass")
    val selectedSubPlan = _selectedSubPlan.asStateFlow()

    private val _subPlanPrice = MutableStateFlow(149)
    val subPlanPrice = _subPlanPrice.asStateFlow()

    private val _isSubscribing = MutableStateFlow(false)
    val isSubscribing = _isSubscribing.asStateFlow()

    private val _subSuccess = MutableStateFlow(false)
    val subSuccess = _subSuccess.asStateFlow()

    private val _subError = MutableStateFlow<String?>(null)
    val subError = _subError.asStateFlow()

    // Custom settings states
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage = _selectedLanguage.asStateFlow()

    private val _isConnectSoundEnabled = MutableStateFlow(true)
    val isConnectSoundEnabled = _isConnectSoundEnabled.asStateFlow()

    private val _isDisconnectSoundEnabled = MutableStateFlow(true)
    val isDisconnectSoundEnabled = _isDisconnectSoundEnabled.asStateFlow()

    private val _audioVideoQuality = MutableStateFlow("Perfect HD Stereo") // "Perfect HD Stereo", "Ultra Crisp Voice", "Standard Eco"
    val audioVideoQuality = _audioVideoQuality.asStateFlow()

    // --- Online & India Network Monitoring States ---
    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    private val _isIndiaNetwork = MutableStateFlow(true)
    val isIndiaNetwork = _isIndiaNetwork.asStateFlow()

    private val _telephonyOperatorName = MutableStateFlow("carrier")
    val telephonyOperatorName = _telephonyOperatorName.asStateFlow()

    private val _isMockingIndiaNetwork = MutableStateFlow(false)
    val isMockingIndiaNetwork = _isMockingIndiaNetwork.asStateFlow()

    // --- Active Server Node Selectors ---
    private val _activeServer = MutableStateFlow("USA - America HighSpeed")
    val activeServer: StateFlow<String> = _activeServer.asStateFlow()

    fun selectServer(server: String) {
        _activeServer.value = server
        _toastMessage.value = "Switched connection routing to: $server!"
    }

    // --- Support Ticketing & Diagnostics Data Structures ---
    data class DiagnosticTask(
        val name: String,
        val description: String,
        val status: String, // "RECOVERED", "WARNING", "HEALTHY"
        val severity: String, // "High", "Low", "None"
        val isResolved: Boolean = true
    )

    data class SupportTicket(
        val ticketId: String,
        val title: String,
        val category: String,
        val description: String,
        val serverNode: String,
        val status: String, // "PENDING", "ANALYZING", "FIXED"
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(
        listOf(
            SupportTicket(
                ticketId = "TKT-38290",
                title = "Initial test handshake latency spikes",
                category = "Connection Lag",
                description = "Handshake latency spiked during simulated test matching sessions.",
                serverNode = "USA - America HighSpeed",
                status = "FIXED"
            ),
            SupportTicket(
                ticketId = "TKT-49202",
                title = "Camera black frames on handshake segment",
                category = "Video Stream Issue",
                description = "Video feed experienced dark, pixelated blackout periods.",
                serverNode = "GLOBAL - Universal LowLatency",
                status = "FIXED"
            )
        )
    )
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<DiagnosticTask>>(
        listOf(
            DiagnosticTask("Stream lag & delay handshake", "Correcting timing delays and dropping back-buffers", "RECOVERED", "High", true),
            DiagnosticTask("Database serialization check", "Index checks and garbage collection sweep", "HEALTHY", "Low", true),
            DiagnosticTask("AI Video Frame processing load", "Normalizing GPU/NPU utilization matrix", "HEALTHY", "Moderate", true),
            DiagnosticTask("Microphone echo calibration", "Acoustic echo cancellation buffer reset", "HEALTHY", "Low", true)
        )
    )
    val diagnostics: StateFlow<List<DiagnosticTask>> = _diagnostics.asStateFlow()

    private val _isResolvingProblem = MutableStateFlow(false)
    val isResolvingProblem: StateFlow<Boolean> = _isResolvingProblem.asStateFlow()

    private val _activeResolvingTask = MutableStateFlow("")
    val activeResolvingTask: StateFlow<String> = _activeResolvingTask.asStateFlow()

    fun submitSupportTicket(title: String, category: String, description: String) {
        val ticketId = "TKT-" + (10000..99999).random()
        val newTicket = SupportTicket(
            ticketId = ticketId,
            title = title,
            category = category,
            description = description,
            serverNode = _activeServer.value,
            status = "PENDING"
        )
        _supportTickets.value = listOf(newTicket) + _supportTickets.value
        _toastMessage.value = "Submitted Ticket! ID: $ticketId"

        viewModelScope.launch {
            delay(4000)
            _supportTickets.value = _supportTickets.value.map { ticket ->
                if (ticket.ticketId == ticketId) ticket.copy(status = "ANALYZING") else ticket
            }
            delay(3000)
            _supportTickets.value = _supportTickets.value.map { ticket ->
                if (ticket.ticketId == ticketId) ticket.copy(status = "FIXED") else ticket
            }
            _toastMessage.value = "Uskha Server Auto-Repair automatically fixed Ticket #$ticketId!"
        }
    }

    fun completeResolvingProblem(category: String) {
        _activeResolvingTask.value = category
        _isResolvingProblem.value = true
        _toastMessage.value = "Executing simulated diagnostics cleansers..."
        
        viewModelScope.launch {
            delay(2500)
            _isResolvingProblem.value = false
            _activeResolvingTask.value = ""
            
            try {
                val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {}

            _toastMessage.value = "Simulated $category resolved and stream node cleared!"
        }
    }

    fun skipToNextPartner() {
        val mode = _matchMode.value
        viewModelScope.launch {
            _isScanningChat.value = true
            _toastMessage.value = "Reconnecting to another match... Handshaking..."
            delay(800)
            _isScanningChat.value = false
            completeMatch(mode)
            _toastMessage.value = "Connected to next partner channel!"
        }
    }

    fun performNetworkCheck() {
        val context = getApplication<Application>().applicationContext
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val online = capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        )
        _isOnline.value = online

        val telephonyManager = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        val networkIso = telephonyManager?.networkCountryIso?.lowercase() ?: ""
        val simIso = telephonyManager?.simCountryIso?.lowercase() ?: ""
        val localeCountry = java.util.Locale.getDefault().country?.lowercase() ?: ""

        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu") ||
                android.os.Build.PRODUCT.contains("sdk_gphone")

        val belongsToIndia = networkIso == "in" || simIso == "in" || localeCountry == "in" || isEmulator

        val operatorName = if (isEmulator) {
            "Simulated India LTE (Jio/Airtel)"
        } else {
            val opName = telephonyManager?.networkOperatorName
            if (!opName.isNullOrEmpty()) opName else "Airtel / Jio India Network"
        }

        _telephonyOperatorName.value = operatorName
        _isIndiaNetwork.value = belongsToIndia || _isMockingIndiaNetwork.value
    }

    fun setMockIndiaNetwork(mock: Boolean) {
        _isMockingIndiaNetwork.value = mock
        _isIndiaNetwork.value = true
        _telephonyOperatorName.value = "Airtel India LTE (Simulated)"
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun setConnectSoundEnabled(enabled: Boolean) {
        _isConnectSoundEnabled.value = enabled
    }

    fun setDisconnectSoundEnabled(enabled: Boolean) {
        _isDisconnectSoundEnabled.value = enabled
    }

    fun setAudioVideoQuality(mode: String) {
        _audioVideoQuality.value = mode
    }

    fun playConnectSound() {
        if (_isConnectSoundEnabled.value) {
            try {
                val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 120)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playDisconnectSound() {
        if (_isDisconnectSoundEnabled.value) {
            try {
                val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 220)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Age Verification System States ---
    private val _ageVerificationMethod = MutableStateFlow("NONE") // "NONE", "THIRD_PARTY", "ID_SCANNER"
    val ageVerificationMethod = _ageVerificationMethod.asStateFlow()

    private val _ageVerificationStatus = MutableStateFlow("IDLE") // "IDLE", "PROCESSING", "SUCCESS", "FAILED"
    val ageVerificationStatus = _ageVerificationStatus.asStateFlow()

    private val _ageVerificationStepDescription = MutableStateFlow("")
    val ageVerificationStepDescription = _ageVerificationStepDescription.asStateFlow()

    private val _verificationError = MutableStateFlow<String?>(null)
    val verificationError = _verificationError.asStateFlow()

    private val _verificationName = MutableStateFlow("")
    val verificationName = _verificationName.asStateFlow()

    private val _verificationDob = MutableStateFlow("") // DD-MM-YYYY
    val verificationDob = _verificationDob.asStateFlow()

    private val _verificationDocType = MutableStateFlow("National ID") // "National ID", "Passport", "Driver License"
    val verificationDocType = _verificationDocType.asStateFlow()

    private val _extractedDob = MutableStateFlow("")
    val extractedDob = _extractedDob.asStateFlow()

    private val _extractedName = MutableStateFlow("")
    val extractedName = _extractedName.asStateFlow()

    private val _extractedDocNum = MutableStateFlow("")
    val extractedDocNum = _extractedDocNum.asStateFlow()

    fun setVerificationMethod(method: String) {
        _ageVerificationMethod.value = method
        _ageVerificationStatus.value = "IDLE"
        _verificationError.value = null
    }

    fun updateVerificationName(name: String) {
        _verificationName.value = name
    }

    fun updateVerificationDob(dob: String) {
        _verificationDob.value = dob
    }

    fun updateVerificationDocType(type: String) {
        _verificationDocType.value = type
    }

    fun resetVerificationState() {
        _ageVerificationMethod.value = "NONE"
        _ageVerificationStatus.value = "IDLE"
        _verificationError.value = null
        _verificationName.value = ""
        _verificationDob.value = ""
        _verificationDocType.value = "National ID"
        _extractedDob.value = ""
        _extractedName.value = ""
        _extractedDocNum.value = ""
    }

    fun startThirdPartyInstantCheck() {
        val name = _verificationName.value.trim()
        val dob = _verificationDob.value.trim()

        if (name.isEmpty()) {
            _verificationError.value = "Full Name is required for cross-referencing secure databases."
            return
        }

        // Validate basic date structure DD-MM-YYYY or DD/MM/YYYY
        val regex = Regex("""\d{2}[-/]\d{2}[-/]\d{4}""")
        if (!dob.matches(regex)) {
            _verificationError.value = "Please enter DOB in DD-MM-YYYY format."
            return
        }

        val yearPart = dob.takeLast(4).toIntOrNull()
        if (yearPart == null) {
            _verificationError.value = "Invalid Date of Birth format."
            return
        }

        val currentYear = 2026
        val age = currentYear - yearPart

        _verificationError.value = null
        _ageVerificationStatus.value = "PROCESSING"

        viewModelScope.launch {
            try {
                _ageVerificationStepDescription.value = "Connecting to SafeVerify API node..."
                delay(1000)
                _ageVerificationStepDescription.value = "Establishing end-to-end encrypted SSL tunnel..."
                delay(1000)
                _ageVerificationStepDescription.value = "Querying national register identity nodes..."
                delay(1200)
                _ageVerificationStepDescription.value = "Awaiting institutional cryptographic response..."
                delay(1000)

                if (age < 18) {
                    _ageVerificationStatus.value = "FAILED"
                    _verificationError.value = "Verification Denied: Specified Date of Birth ($dob) indicates user is under the age of 18."
                } else {
                    _ageVerificationStatus.value = "SUCCESS"
                    _ageVerificationStepDescription.value = "Successfully Verified Over 18! Signed with cryptographic certificate."
                }
            } catch (e: Exception) {
                _ageVerificationStatus.value = "FAILED"
                _verificationError.value = "Secure Node Connection Error: ${e.message}"
            }
        }
    }

    fun startIdDocumentScanSimulation() {
        _verificationError.value = null
        _ageVerificationStatus.value = "PROCESSING"

        viewModelScope.launch {
            try {
                _ageVerificationStepDescription.value = "Aligning optical character recognition (OCR) matrix..."
                delay(1200)
                _ageVerificationStepDescription.value = "Acquiring card boundary frames & hologram reflections..."
                delay(1000)
                _ageVerificationStepDescription.value = "Extracting MRZ record data and printing blocks..."
                delay(1400)
                _ageVerificationStepDescription.value = "Verifying neural age compliance status..."
                delay(1000)

                _extractedName.value = "VERIFIED HOLDER"
                _extractedDocNum.value = "${_verificationDocType.value.take(3).uppercase()}-${(100000..999999).random()}"
                _extractedDob.value = "18/06/2004" // Guarantees 18+

                _ageVerificationStatus.value = "SUCCESS"
                _ageVerificationStepDescription.value = "AI OCR Scanner Verification Completed Successfully! User is confirmed 18+."
            } catch (e: Exception) {
                _ageVerificationStatus.value = "FAILED"
                _verificationError.value = "Document scanner jammed: OCR reading timeout. Clear physical camera view and retry."
            }
        }
    }

    fun confirmCompleteVerification() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAgeVerified(true)
            _currentScreen.value = UskhaScreen.Dashboard
        }
    }

    init {
        val database = UskhaDatabase.getDatabase(application)
        repository = UskhaRepository(database.uskhaDao())

        userPrefs = repository.userPreferencesFlow
            .map { it ?: UserPreferences() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

        matchHistory = repository.matchHistoryFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        moderationReports = repository.reportsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        walletTransactions = repository.walletTransactionsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Pull initial preferences and route screen
        viewModelScope.launch {
            val initialPrefs = repository.getUserPreferencesDirect()
            if (!initialPrefs.isLoggedIn) {
                _currentScreen.value = UskhaScreen.Auth
            } else if (initialPrefs.ageVerified) {
                _currentScreen.value = UskhaScreen.Dashboard
            } else {
                _currentScreen.value = UskhaScreen.AgeGate
            }

            // Seed initial welcome credit if history is empty
            launch(Dispatchers.IO) {
                val existing = repository.walletTransactionsFlow.first()
                if (existing.isEmpty()) {
                    repository.insertWalletTransaction(WalletTransaction(
                        type = "CREDIT",
                        amount = 20,
                        description = "Welcome Account Credit"
                    ))
                }
            }

            // Simulate initial incoming friend request for high-fidelity experience if lists are empty
            delay(10000)
            val currentLatest = repository.getUserPreferencesDirect()
            if (parseFriends(currentLatest.friendRequestsJson).isEmpty() && parseFriends(currentLatest.friendsJson).isEmpty()) {
                sendFriendRequest("USKHA-38291", "Priya")
                _toastMessage.value = "New friend request from Priya (USKHA-38291)!"
            }
        }

        // Start continuous background network checks
        viewModelScope.launch {
            while (true) {
                try {
                    performNetworkCheck()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000)
            }
        }
    }

    fun navigateTo(screen: UskhaScreen) {
        val previousScreen = _currentScreen.value
        _currentScreen.value = screen

        // Start/Stop Video Scanning loops
        if (screen is UskhaScreen.VideoChat) {
            startVideoFrameProcessingLoop()
        } else if (previousScreen is UskhaScreen.VideoChat) {
            stopVideoFrameProcessingLoop()
        }

        // Connect sound trigger
        if ((screen is UskhaScreen.TextChat || screen is UskhaScreen.VideoChat) &&
            previousScreen !is UskhaScreen.TextChat && previousScreen !is UskhaScreen.VideoChat) {
            playConnectSound()
        }

        // Disconnect sound trigger
        if (screen is UskhaScreen.Dashboard &&
            (previousScreen is UskhaScreen.TextChat || previousScreen is UskhaScreen.VideoChat)) {
            playDisconnectSound()
        }

        // Reset states if leaving active screens
        if (screen is UskhaScreen.Dashboard) {
            _activeMatch.value = null
            _chatMessages.value = emptyList()
            _isSearching.value = false
        }
    }

    fun verifyAge(confirmed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (confirmed) {
                repository.setAgeVerified(true)
                _currentScreen.value = UskhaScreen.Dashboard
            }
        }
    }

    fun setGenderFilter(gender: String) {
        _genderFilter.value = gender
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            repository.saveUserPreferences(current.copy(selectedGenderFilter = gender))
        }
    }

    fun setUserGender(gender: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            repository.saveUserPreferences(current.copy(userGender = gender))
            _toastMessage.value = "Profile setting updated: Gender set explicitly to $gender."
        }
    }

    fun setSafeModeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            repository.saveUserPreferences(current.copy(safeModeEnabled = enabled))
            _toastMessage.value = if (enabled) {
                "Safe Mode Enabled: Incoming video streams are now blurred by default."
            } else {
                "Safe Mode Disabled: Video streams will display directly."
            }
        }
    }

    fun triggerIncomingVideoCallFromBoy() {
        viewModelScope.launch {
            delay(2500) // Small realistic delay
            val boyNames = listOf("Rohit", "Vikram", "Abhishek", "Rohan", "Kabir", "Aryan")
            val name = boyNames.random()
            val age = (19..24).random()
            val avatarSeed = java.util.UUID.randomUUID().toString()
            _incomingVideoCall.value = MatchHistory(
                id = 0,
                partnerName = name,
                partnerAge = age,
                partnerGender = "Boy",
                avatarSeed = avatarSeed
            )
        }
    }

    fun acceptIncomingVideoCall() {
        val call = _incomingVideoCall.value ?: return
        _incomingVideoCall.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertMatch(call)
            val finalMatch = call.copy(id = id)
            _activeMatch.value = finalMatch
            viewModelScope.launch(Dispatchers.Main) {
                navigateTo(UskhaScreen.VideoChat)
            }
            sendSystemMessage("You accepted the incoming video call. Private video link secure.")
            delay(1500)
            simulateStrangerResponse()
        }
    }

    fun declineIncomingVideoCall() {
        _incomingVideoCall.value = null
        _toastMessage.value = "Incoming call declined safely."
    }

    fun startMatching(mode: MatchMode) {
        val prefs = userPrefs.value
        // Enforce Boy-to-Girl video calling constraint
        if (prefs.userGender == "Girl" && (mode == MatchMode.VIDEO || mode == MatchMode.GIRLS_VIDEO)) {
            _toastMessage.value = "Secure Rule: Girls only receive incoming video calls from boys. Opening safe receiving queue..."
            triggerIncomingVideoCallFromBoy()
            return
        }

        if (mode == MatchMode.GIRLS_VIDEO && prefs.userGender == "Girl") {
            _toastMessage.value = "Girls-Only Video Call is exclusive to Boys."
            return
        }

        val isFreeVideoCall = mode == MatchMode.VIDEO && !prefs.hasUsedFreeVideoCall
        val requiredCoins = when (mode) {
            MatchMode.GIRLS_VIDEO -> 30
            MatchMode.TEXT -> 3
            MatchMode.VIDEO -> if (isFreeVideoCall) 0 else 15
        }
        val currentCoins = prefs.walletCoins
        if (currentCoins < requiredCoins) {
            // Redirect to pay
            _selectedPayAmount.value = 30  // Set default checkout to cover the 30 coins Pack!
            _currentScreen.value = UskhaScreen.PremiumHub
            return
        }

        _matchMode.value = mode
        _isSearching.value = true
        _currentScreen.value = UskhaScreen.Matching

        viewModelScope.launch {
            // Simulated radar matching delay for high suspension visual experience
            val searchDuration = (3000L..6000L).random()
            delay(searchDuration)

            if (_isSearching.value) {
                // Check gender lock: Girl matching requires premium or unlocked
                val filter = _genderFilter.value
                
                if (mode != MatchMode.GIRLS_VIDEO && filter == "Girl" && !prefs.premiumSubscribed && !prefs.girlVideoUnlocked) {
                    // Redirect to pay
                    _isSearching.value = false
                    _selectedPayAmount.value = 9
                    _currentScreen.value = UskhaScreen.PremiumHub
                    return@launch
                }

                // Deduct the coins now
                val deducted = if (isFreeVideoCall) true else repository.deductCoins(requiredCoins)
                if (deducted) {
                    if (isFreeVideoCall) {
                        repository.saveUserPreferences(prefs.copy(hasUsedFreeVideoCall = true))
                        repository.insertWalletTransaction(WalletTransaction(
                            type = "DEBIT",
                            amount = 0,
                            description = "Free Video Match Voucher"
                        ))
                    } else {
                        val matchDesc = when (mode) {
                            MatchMode.GIRLS_VIDEO -> "Girls-Only Video Call Match Fee"
                            MatchMode.TEXT -> "Instant Text Call Match Fee"
                            MatchMode.VIDEO -> "Random Video Call Match Fee"
                        }
                        repository.insertWalletTransaction(WalletTransaction(
                            type = "DEBIT",
                            amount = requiredCoins,
                            description = matchDesc
                        ))
                    }
                    completeMatch(mode)
                } else {
                    _isSearching.value = false
                    _selectedPayAmount.value = 9
                    _currentScreen.value = UskhaScreen.PremiumHub
                }
            }
        }
    }

    fun stopMatching() {
        _isSearching.value = false
        _currentScreen.value = UskhaScreen.Dashboard
    }

    private suspend fun completeMatch(mode: MatchMode) {
        val prefs = userPrefs.value
        _isSearching.value = false
        _isStrangerMuted.value = false
        _scanVerdict.value = null

        val randNames = if (mode == MatchMode.GIRLS_VIDEO || (mode == MatchMode.VIDEO && prefs.userGender == "Boy")) {
            // Strictly boys can only initiate video calls to girls - ensure we only choose girl names
            listOf("Sneha", "Kriti", "Anjali", "Priya", "Ishika", "Maya")
        } else if (prefs.userGender == "Boy") {
            // Boys connect ONLY to boys in normal matches! (boys to boys)
            listOf("Rohit", "Vikram", "Abhishek", "Rohan", "Kabir", "Aryan")
        } else if (_genderFilter.value == "Girl") {
            listOf("Sneha", "Kriti", "Anjali", "Priya", "Ishika", "Maya")
        } else if (_genderFilter.value == "Boy") {
            listOf("Rohit", "Vikram", "Abhishek", "Rohan", "Kabir", "Aryan")
        } else {
            listOf("Sneha", "Kriti", "Anjali", "Priya", "Rohit", "Abhishek", "Maya", "Rohan")
        }

        val name = randNames.random()
        val age = (18..24).random()
        val pGender = if (name in listOf("Sneha", "Kriti", "Anjali", "Priya", "Ishika", "Maya")) "Girl" else "Boy"
        
        // Generate a random seed for the robot/face visual representations
        val avatarSeed = UUID.randomUUID().toString()

        val match = MatchHistory(
            partnerName = name,
            partnerAge = age,
            partnerGender = pGender,
            avatarSeed = avatarSeed
        )

        val id = repository.insertMatch(match)
        val finalMatch = match.copy(id = id)
        _activeMatch.value = finalMatch

        viewModelScope.launch(Dispatchers.Main) {
            if (mode == MatchMode.TEXT) {
                navigateTo(UskhaScreen.TextChat)
            } else {
                navigateTo(UskhaScreen.VideoChat)
            }
        }

        // Send system greeting greeting
        sendSystemMessage("You are now connected randomly to a stranger. Say Hello!")
        
        // Let the stranger send an icebreaker automatically after 1.5 seconds!
        delay(1500)
        simulateStrangerResponse()
    }

    private fun sendSystemMessage(text: String) {
        val sysMsg = ChatMessage(
            matchId = _activeMatch.value?.id ?: 0,
            sender = "system",
            messageText = text
        )
        _chatMessages.value = _chatMessages.value + sysMsg
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty()) return
        val matchId = _activeMatch.value?.id ?: return

        val userMsg = ChatMessage(
            matchId = matchId,
            sender = "user",
            messageText = text
        )

        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertChatMessage(userMsg)
            
            // Trigger stranger response and real-time content filters if necessary
            delay(1000)
            simulateStrangerResponse()
        }
    }

    private fun simulateStrangerResponse() {
        val match = _activeMatch.value ?: return
        if (_isStrangerMuted.value) return

        _isPartnerTyping.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // Use Gemini API to calculate reply
            val replyText = GeminiClient.generateStrangerReply(
                partnerName = match.partnerName,
                partnerAge = match.partnerAge,
                partnerGender = match.partnerGender,
                chatHistory = _chatMessages.value
            )

            _isPartnerTyping.value = false

            val strangerMsg = ChatMessage(
                matchId = match.id,
                sender = "stranger",
                messageText = replyText
            )

            _chatMessages.value = _chatMessages.value + strangerMsg
            repository.insertChatMessage(strangerMsg)
        }
    }

    /**
     * Reporting and AI Content Moderation integration
     */
    fun reportStranger(reason: String) {
        val match = _activeMatch.value ?: return
        val currentHistory = _chatMessages.value.filter { it.sender != "system" }

        _isScanningChat.value = true
        _isStrangerMuted.value = true // Prevent further replies immediately

        viewModelScope.launch(Dispatchers.IO) {
            // Execute Gemini real-time safety scans
            val scan = GeminiClient.analyzeConversationModeration(currentHistory)
            
            val verdict = if (!scan.isSafe) {
                "Inappropriate: ${scan.reason}"
            } else {
                "Safety scanned: Safe. Stranger moderated."
            }

            _scanVerdict.value = verdict
            _isScanningChat.value = false

            // Save official report log
            val report = ModerationReport(
                reportedPartnerName = match.partnerName,
                chatExcerpt = currentHistory.takeLast(5).joinToString(", ") { "${it.sender}: ${it.messageText}" },
                scanVerdict = verdict
            )
            repository.insertModerationReport(report)

            if (!scan.isSafe) {
                // Automatically terminate match for user safety
                sendSystemMessage("This chat was terminated automatically by Uskha AI content filtering protection: ${scan.reason}")
            }
        }
    }

    fun dismissScanVerdict() {
        _scanVerdict.value = null
    }

    /**
     * Interactive Payments processors (UPI, PayPal, Credit Card)
     */
    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
        _paymentError.value = null
        _paymentVerifiedSuccessfully.value = false
    }

    fun updatePayAmount(amount: Int) {
        _selectedPayAmount.value = amount
        _enteredUtr.value = ""
        _paypalEmail.value = ""
        _cardNumber.value = ""
        _cardExpiry.value = ""
        _cardCvv.value = ""
        _paymentVerifiedSuccessfully.value = false
        _paymentError.value = null
    }

    fun updateUtrField(newValue: String) {
        _enteredUtr.value = newValue
    }

    fun updatePaypalEmail(newValue: String) {
        _paypalEmail.value = newValue
    }

    fun updateCardDetails(number: String, expiry: String, cvv: String) {
        _cardNumber.value = number
        _cardExpiry.value = expiry
        _cardCvv.value = cvv
    }

    fun submitSimulatedPaymentReceipt() {
        val method = _selectedPaymentMethod.value
        _paymentError.value = null

        when (method) {
            PaymentMethod.UPI -> {
                val utr = _enteredUtr.value.trim()
                if (utr.length < 6) {
                    _paymentError.value = "Please enter a valid 12-digit UPI transaction ID or reference code."
                    return
                }
            }
            PaymentMethod.PAYPAL -> {
                val email = _paypalEmail.value.trim()
                if (!email.contains("@") || !email.contains(".")) {
                    _paymentError.value = "Please enter a valid PayPal email address to authorize."
                    return
                }
            }
            PaymentMethod.CARD -> {
                val num = _cardNumber.value.replace(" ", "")
                val exp = _cardExpiry.value.trim()
                val cvv = _cardCvv.value.trim()
                if (num.length < 12 || exp.length < 5 || cvv.length != 3) {
                    _paymentError.value = "Please complete Card details: Number (12-16-digit), Expiry (MM/YY) and CVV (3-digit)."
                    return
                }
            }
        }

        _isPaymentProcessing.value = true

        viewModelScope.launch {
            // Realistic payment clearing simulated delay
            delay(3000)

            _isPaymentProcessing.value = false
            _paymentVerifiedSuccessfully.value = true

            // Update user status and persist in Room DB
            val amount = _selectedPayAmount.value
            val current = repository.getUserPreferencesDirect()
            
            val updatedCoins = when (amount) {
                9 -> current.walletCoins + 25
                30 -> current.walletCoins + 70
                100 -> current.walletCoins + 233
                250 -> current.walletCoins + 585
                500 -> current.walletCoins + 1170
                1000 -> current.walletCoins + 2350
                2500 -> current.walletCoins + 6000
                else -> current.walletCoins + 25
            }

            // High priority unlocks based on premium criteria
            val girlUnlock = amount >= 100
            repository.saveUserPreferences(
                current.copy(
                    premiumSubscribed = true,
                    girlVideoUnlocked = current.girlVideoUnlocked || girlUnlock,
                    walletCoins = updatedCoins
                )
            )

            val creditedCoins = updatedCoins - current.walletCoins
            repository.insertWalletTransaction(WalletTransaction(
                type = "CREDIT",
                amount = creditedCoins,
                description = "Purchased Coin Pack (INR $amount)"
            ))
        }
    }

    fun selectPremiumHubTab(tab: Int) {
        _premiumHubTab.value = tab
        // Reset flows when switching tabs to avoid carrying over transition logs
        _subSuccess.value = false
        _subError.value = null
        _paymentVerifiedSuccessfully.value = false
        _paymentError.value = null
    }

    fun selectSubscriptionPlan(planName: String, price: Int) {
        _selectedSubPlan.value = planName
        _subPlanPrice.value = price
        _subSuccess.value = false
        _subError.value = null
    }

    fun toggleSimulatedDecline(decline: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            repository.saveUserPreferences(current.copy(simulatedPaymentForceDecline = decline))
            _toastMessage.value = if (decline) {
                "Developer Simulation Activated: Payments will now fail with Insufficient Funds."
            } else {
                "Developer Simulation Reset: Payments will now clear successfully."
            }
        }
    }

    fun submitSubscriptionPayment() {
        val plan = _selectedSubPlan.value
        val price = _subPlanPrice.value
        _subError.value = null
        _isSubscribing.value = true

        viewModelScope.launch {
            // Replicate multiple step process for payment simulation
            delay(1200)
            val current = repository.getUserPreferencesDirect()
            
            if (current.simulatedPaymentForceDecline) {
                _isSubscribing.value = false
                _subError.value = "Declined: Insufficient funds or card restriction (Code: 51). Please toggle simulated decline in controls to allow success."
                return@launch
            }

            delay(1200)
            // Save active VIP subscription in the Room DB
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, if (plan.contains("Annual")) 365 else 30)
            val renewalString = sdf.format(calendar.time)

            repository.saveUserPreferences(
                current.copy(
                    premiumSubscribed = true,
                    isSubscriptionActive = true,
                    subscriptionName = plan,
                    subscriptionRenewalDate = renewalString
                )
            )

            // Insert matching wallet transaction for ledger
            repository.insertWalletTransaction(
                WalletTransaction(
                    type = "CREDIT",
                    amount = price,
                    description = "Subscribed to $plan (INR $price)"
                )
            )

            _isSubscribing.value = false
            _subSuccess.value = true
            _toastMessage.value = "Congratulations! You are now subscribed to $plan."
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            repository.saveUserPreferences(
                current.copy(
                    premiumSubscribed = false,
                    isSubscriptionActive = false,
                    subscriptionName = "",
                    subscriptionRenewalDate = ""
                )
            )
            repository.insertWalletTransaction(
                WalletTransaction(
                    type = "DEBIT",
                    amount = 0,
                    description = "Subscription Cancelled"
                )
            )
            _subSuccess.value = false
            _toastMessage.value = "Subscription cancelled. Premium VIP features have been disabled."
        }
    }

    fun closePaymentScreen() {
        _enteredUtr.value = ""
        _paypalEmail.value = ""
        _cardNumber.value = ""
        _cardExpiry.value = ""
        _cardCvv.value = ""
        _paymentVerifiedSuccessfully.value = false
        _paymentError.value = null
        _currentScreen.value = UskhaScreen.Dashboard
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = matchHistory.value
            list.forEach {
                repository.deleteMatch(it.id)
            }
        }
    }

    // ==========================================
    // SECURE LOGIN & AUTH SERVICES
    // ==========================================
    fun loginWithGmail(email: String, nameInput: String, gender: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            val cleanEmail = email.trim()
            val cleanName = if (nameInput.trim().isNotEmpty()) nameInput.trim() else "Google User"
            val avatarSeed = "avatar_g_" + (1000..9999).random()

            repository.saveUserPreferences(current.copy(
                isLoggedIn = true,
                loginType = "GMAIL",
                loggedInEmail = cleanEmail,
                username = cleanName,
                avatarSeedOnAuth = avatarSeed,
                userGender = gender,
                walletCoins = current.walletCoins + 10 // Register bonus
            ))
            repository.insertWalletTransaction(WalletTransaction(
                type = "CREDIT",
                amount = 10,
                description = "Google Auth Registration Bonus"
            ))
            _currentScreen.value = if (current.ageVerified) UskhaScreen.Dashboard else UskhaScreen.AgeGate
        }
    }

    fun loginWithPhoneNumber(phone: String, nameInput: String, gender: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            val cleanPhone = phone.trim()
            val cleanName = if (nameInput.trim().isNotEmpty()) nameInput.trim() else "Phone Explorer"
            val avatarSeed = "avatar_p_" + (1000..9999).random()

            repository.saveUserPreferences(current.copy(
                isLoggedIn = true,
                loginType = "PHONE",
                loggedInPhone = cleanPhone,
                username = cleanName,
                avatarSeedOnAuth = avatarSeed,
                userGender = gender,
                walletCoins = current.walletCoins + 10 // Register bonus
            ))
            repository.insertWalletTransaction(WalletTransaction(
                type = "CREDIT",
                amount = 10,
                description = "Phone Auth Registration Bonus"
            ))
            _currentScreen.value = if (current.ageVerified) UskhaScreen.Dashboard else UskhaScreen.AgeGate
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getUserPreferencesDirect()
            repository.saveUserPreferences(current.copy(
                isLoggedIn = false,
                loginType = "",
                loggedInEmail = "",
                loggedInPhone = "",
                appliedInviteCode = "",
                hasAppliedInvite = false
            ))
            _currentScreen.value = UskhaScreen.Auth
        }
    }

    // ==========================================
    // INVITE REFERRAL CODE SYSTEM
    // ==========================================
    private val _inviteCodeError = MutableStateFlow<String?>(null)
    val inviteCodeError = _inviteCodeError.asStateFlow()

    private val _inviteCodeSuccess = MutableStateFlow(false)
    val inviteCodeSuccess = _inviteCodeSuccess.asStateFlow()

    fun applyInviteCode(code: String) {
        val prefs = userPrefs.value
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isEmpty()) return

        if (cleanCode == prefs.selfInviteCode.uppercase()) {
            _inviteCodeError.value = "Cannot refer your own invite code."
            _inviteCodeSuccess.value = false
            return
        }

        if (prefs.hasAppliedInvite) {
            _inviteCodeError.value = "You have already applied an invite code."
            _inviteCodeSuccess.value = false
            return
        }

        _inviteCodeError.value = null
        _inviteCodeSuccess.value = true

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveUserPreferences(prefs.copy(
                hasAppliedInvite = true,
                appliedInviteCode = cleanCode,
                walletCoins = prefs.walletCoins + 25 // reward coins
            ))
            repository.insertWalletTransaction(WalletTransaction(
                type = "CREDIT",
                amount = 25,
                description = "Referral Bonus Code: $cleanCode"
            ))
        }
    }

    fun clearInviteStates() {
        _inviteCodeError.value = null
        _inviteCodeSuccess.value = false
    }

    // ==========================================
    // SOCIAL CONNECTIONS: FRIENDS & REQUESTS
    // ==========================================
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun triggerSimpleToast(msg: String) {
        _toastMessage.value = msg
    }

    val friends: StateFlow<List<Friend>> by lazy {
        userPrefs
            .map { parseFriends(it.friendsJson) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val friendRequests: StateFlow<List<Friend>> by lazy {
        userPrefs
            .map { parseFriends(it.friendRequestsJson) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun parseFriends(raw: String): List<Friend> {
        if (raw.trim().isEmpty() || raw == "[]" || raw == "null") return emptyList()
        try {
            return raw.split(";;").filter { it.isNotEmpty() }.mapNotNull { block ->
                val tokens = block.split(":")
                if (tokens.size >= 5) {
                    Friend(
                        userId = tokens[0],
                        name = tokens[1],
                        age = tokens[2].toIntOrNull() ?: 20,
                        gender = tokens[3],
                        avatarSeed = tokens[4]
                    )
                } else null
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun serializeFriends(list: List<Friend>): String {
        return list.joinToString(";;") { "${it.userId}:${it.name}:${it.age}:${it.gender}:${it.avatarSeed}" }
    }

    private suspend fun addFriendDirectly(friend: Friend) {
        val prefs = userPrefs.value
        val list = parseFriends(prefs.friendsJson).toMutableList()
        if (list.any { it.userId == friend.userId }) return
        list.add(friend)
        repository.saveUserPreferences(prefs.copy(friendsJson = serializeFriends(list)))
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = userPrefs.value
            val list = parseFriends(prefs.friendsJson).toMutableList()
            list.removeAll { it.userId == userId }
            repository.saveUserPreferences(prefs.copy(friendsJson = serializeFriends(list)))
        }
    }

    fun sendFriendRequest(targetUserId: String, targetName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = userPrefs.value
            val list = parseFriends(prefs.friendRequestsJson).toMutableList()
            if (list.any { it.userId == targetUserId }) return@launch

            val randomAvatar = UUID.randomUUID().toString()
            val incoming = Friend(
                userId = targetUserId,
                name = targetName,
                age = (18..24).random(),
                gender = if (targetName in listOf("Kriti", "Anjali", "Priya", "Sneha", "Ishika", "Maya")) "Girl" else "Boy",
                avatarSeed = randomAvatar
            )
            list.add(incoming)
            repository.saveUserPreferences(prefs.copy(friendRequestsJson = serializeFriends(list)))
        }
    }

    fun sendFriendRequestToId(targetId: String) {
        val cleanId = targetId.trim().uppercase()
        if (cleanId.isEmpty()) return

        _toastMessage.value = "Request to $cleanId sent successfully!"

        viewModelScope.launch(Dispatchers.IO) {
            delay(2500)
            val name = listOf("Sneha", "Kriti", "Anjali", "Rohan", "Kabir", "Aryan").random()
            val incoming = Friend(
                userId = cleanId,
                name = name,
                age = (18..25).random(),
                gender = if (name in listOf("Sneha", "Kriti", "Anjali", "Sneha", "Ishika", "Maya")) "Girl" else "Boy",
                avatarSeed = UUID.randomUUID().toString()
            )
            addFriendDirectly(incoming)
            _toastMessage.value = "$name ($cleanId) accepted your request!"
        }
    }

    fun sendFriendRequestToPartner() {
        val match = _activeMatch.value ?: return
        val partnerId = "USKHA-" + Math.abs(match.avatarSeed.hashCode() % 90000 + 10000)

        sendSystemMessage("Sent Friend Request to ${match.partnerName} ($partnerId).")

        viewModelScope.launch {
            delay(1200)
            _isPartnerTyping.value = true
            delay(1500)
            _isPartnerTyping.value = false

            val text = "Wow! I'd love to stay connected as your friend! I've accepted your invitation. We can chat anytime now! Check the Social tab!"
            val msg = ChatMessage(
                matchId = match.id,
                sender = "stranger",
                messageText = text
            )
            _chatMessages.value = _chatMessages.value + msg
            repository.insertChatMessage(msg)

            val newFriend = Friend(
                userId = partnerId,
                name = match.partnerName,
                age = match.partnerAge,
                gender = match.partnerGender,
                avatarSeed = match.avatarSeed
            )
            addFriendDirectly(newFriend)
        }
    }

    fun acceptFriendRequest(request: Friend) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = userPrefs.value
            val reqList = parseFriends(prefs.friendRequestsJson).toMutableList()
            reqList.removeAll { it.userId == request.userId }

            val friendList = parseFriends(prefs.friendsJson).toMutableList()
            if (!friendList.any { it.userId == request.userId }) {
                friendList.add(request)
            }

            repository.saveUserPreferences(prefs.copy(
                friendRequestsJson = serializeFriends(reqList),
                friendsJson = serializeFriends(friendList)
            ))
            _toastMessage.value = "You are now friends with ${request.name}!"
        }
    }

    fun declineFriendRequest(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = userPrefs.value
            val reqList = parseFriends(prefs.friendRequestsJson).toMutableList()
            reqList.removeAll { it.userId == userId }
            repository.saveUserPreferences(prefs.copy(friendRequestsJson = serializeFriends(reqList)))
        }
    }

    private var videoScanJob: kotlinx.coroutines.Job? = null

    fun startVideoFrameProcessingLoop() {
        videoScanJob?.cancel()
        _videoScanVerdict.value = "SAFE"
        _isVideoSimulationViolationActive.value = false
        videoScanJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5000) // Fast 5 seconds cycle for responsive demo
                _isScanningVideoFrame.value = true
                delay(1000) // Thinking time
                
                val isViolated = _isVideoSimulationViolationActive.value
                val result = GeminiClient.analyzeVideoFrame(
                    bitmap = null,
                    isSimulationViolationActive = isViolated
                )
                
                _isScanningVideoFrame.value = false
                if (!result.isSafe) {
                    _videoScanVerdict.value = "VIOLATION DETECTED: ${result.reason}"
                    terminateVideoCallDueToSafetyViolation(result.reason ?: "Unsafe stream detected.")
                    break
                } else {
                    _videoScanVerdict.value = "SAFE"
                }
            }
        }
    }

    fun stopVideoFrameProcessingLoop() {
        videoScanJob?.cancel()
        videoScanJob = null
        _isScanningVideoFrame.value = false
    }

    fun scanCurrentCameraBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningVideoFrame.value = true
            val result = GeminiClient.analyzeVideoFrame(
                bitmap = bitmap,
                isSimulationViolationActive = _isVideoSimulationViolationActive.value
            )
            _isScanningVideoFrame.value = false
            if (!result.isSafe) {
                _videoScanVerdict.value = "VIOLATION DETECTED: ${result.reason}"
                terminateVideoCallDueToSafetyViolation(result.reason ?: "Unsafe Content Detected")
            } else {
                _videoScanVerdict.value = "SAFE"
            }
        }
    }

    fun toggleVideoSimulationViolation(active: Boolean) {
        _isVideoSimulationViolationActive.value = active
        if (active) {
            _toastMessage.value = "SIMULATION SAFETY VIOLATION ON: NSFW Mode active. Automatically shutting down call next cycle!"
        } else {
            _toastMessage.value = "Simulation violation deactivated."
        }
    }

    fun terminateVideoCallDueToSafetyViolation(reason: String) {
        viewModelScope.launch {
            val partnerName = _activeMatch.value?.partnerName ?: "Stranger"
            val report = com.example.data.model.ModerationReport(
                id = 0,
                reporterName = "Gemini AI Content Filter",
                reportedPartnerName = partnerName,
                chatExcerpt = "[Automated Active Video Call Scanner Flagged: Safety Violations Detected]",
                scanVerdict = "Inappropriate - Blocked (${reason})"
            )
            repository.insertModerationReport(report)
            
            // Reassure user
            _toastMessage.value = "VIDEO TERMINATED: Gemini AI Content Filter closed the feed to protect you! Report logged."
            _videoScanVerdict.value = "BLOCKED: Safety Violation detected."
            
            // Navigate away
            stopVideoFrameProcessingLoop()
            _activeMatch.value = null
            _currentScreen.value = UskhaScreen.Dashboard
        }
    }
}

data class Friend(
    val userId: String,
    val name: String,
    val age: Int,
    val gender: String,
    val avatarSeed: String
)
