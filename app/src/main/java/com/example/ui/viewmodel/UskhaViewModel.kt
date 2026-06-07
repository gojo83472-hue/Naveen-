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
import com.example.data.repository.UskhaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class UskhaScreen {
    object AgeGate : UskhaScreen()
    object Dashboard : UskhaScreen()
    object Matching : UskhaScreen()
    object TextChat : UskhaScreen()
    object VideoChat : UskhaScreen()
    object PremiumHub : UskhaScreen()
    object SafetyCenter : UskhaScreen()
}

enum class MatchMode {
    TEXT, VIDEO
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

    // Matching and Chat details
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _matchMode = MutableStateFlow(MatchMode.TEXT)
    val matchMode = _matchMode.asStateFlow()

    private val _genderFilter = MutableStateFlow("All")
    val genderFilter = _genderFilter.asStateFlow()

    private val _activeMatch = MutableStateFlow<MatchHistory?>(null)
    val activeMatch = _activeMatch.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping = _isPartnerTyping.asStateFlow()

    // Safety and Moderation state
    private val _isScanningChat = MutableStateFlow(false)
    val isScanningChat = _isScanningChat.asStateFlow()

    private val _scanVerdict = MutableStateFlow<String?>(null)
    val scanVerdict = _scanVerdict.asStateFlow()

    private val _isStrangerMuted = MutableStateFlow(false)
    val isStrangerMuted = _isStrangerMuted.asStateFlow()

    // Custom Payments & UPI states
    private val _selectedPayAmount = MutableStateFlow(9) // 9 RS standard, 19 RS girl video call
    val selectedPayAmount = _selectedPayAmount.asStateFlow()

    private val _enteredUtr = MutableStateFlow("")
    val enteredUtr = _enteredUtr.asStateFlow()

    private val _isPaymentProcessing = MutableStateFlow(false)
    val isPaymentProcessing = _isPaymentProcessing.asStateFlow()

    private val _paymentVerifiedSuccessfully = MutableStateFlow(false)
    val paymentVerifiedSuccessfully = _paymentVerifiedSuccessfully.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError = _paymentError.asStateFlow()

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

        // Pull initial preferences and route screen
        viewModelScope.launch {
            val initialPrefs = repository.getUserPreferencesDirect()
            if (initialPrefs.ageVerified) {
                _currentScreen.value = UskhaScreen.Dashboard
            } else {
                _currentScreen.value = UskhaScreen.AgeGate
            }
        }
    }

    fun navigateTo(screen: UskhaScreen) {
        _currentScreen.value = screen
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

    fun startMatching(mode: MatchMode) {
        val prefs = userPrefs.value
        val isFreeVideoCall = mode == MatchMode.VIDEO && !prefs.hasUsedFreeVideoCall
        val requiredCoins = if (isFreeVideoCall) 0 else (if (mode == MatchMode.TEXT) 3 else 15)
        val currentCoins = prefs.walletCoins
        if (currentCoins < requiredCoins) {
            // Redirect to pay
            _selectedPayAmount.value = if (mode == MatchMode.VIDEO) 19 else 9
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
                
                if (filter == "Girl" && !prefs.premiumSubscribed && !prefs.girlVideoUnlocked) {
                    // Redirect to pay
                    _isSearching.value = false
                    _selectedPayAmount.value = 19
                    _currentScreen.value = UskhaScreen.PremiumHub
                    return@launch
                }

                // Deduct the coins now
                val deducted = if (isFreeVideoCall) true else repository.deductCoins(requiredCoins)
                if (deducted) {
                    if (isFreeVideoCall) {
                        repository.saveUserPreferences(prefs.copy(hasUsedFreeVideoCall = true))
                    }
                    completeMatch(mode)
                } else {
                    _isSearching.value = false
                    _selectedPayAmount.value = if (mode == MatchMode.VIDEO) 19 else 9
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
        _isSearching.value = false
        _isStrangerMuted.value = false
        _scanVerdict.value = null

        val randNames = if (_genderFilter.value == "Girl") {
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

        if (mode == MatchMode.TEXT) {
            _currentScreen.value = UskhaScreen.TextChat
        } else {
            _currentScreen.value = UskhaScreen.VideoChat
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
     * UPI payments processor
     */
    fun updatePayAmount(amount: Int) {
        _selectedPayAmount.value = amount
        _enteredUtr.value = ""
        _paymentVerifiedSuccessfully.value = false
        _paymentError.value = null
    }

    fun updateUtrField(newValue: String) {
        _enteredUtr.value = newValue
    }

    fun submitSimulatedPaymentReceipt() {
        val utr = _enteredUtr.value.trim()
        if (utr.length < 6) {
            _paymentError.value = "Please enter a valid 12-digit UPI transaction ID or reference code."
            return
        }

        _isPaymentProcessing.value = true
        _paymentError.value = null

        viewModelScope.launch {
            // Realistic payment clearing simulated delay
            delay(3000)

            _isPaymentProcessing.value = false
            _paymentVerifiedSuccessfully.value = true

            // Update user status persist in Room DB
            val amount = _selectedPayAmount.value
            val current = repository.getUserPreferencesDirect()
            
            if (amount == 9) {
                repository.saveUserPreferences(current.copy(premiumSubscribed = true, walletCoins = current.walletCoins + 15))
            } else if (amount == 19) {
                repository.saveUserPreferences(current.copy(girlVideoUnlocked = true, walletCoins = current.walletCoins + 79))
            } else if (amount == 100) {
                repository.saveUserPreferences(current.copy(premiumSubscribed = true, girlVideoUnlocked = true, walletCoins = current.walletCoins + 250))
            } else if (amount == 500) {
                repository.saveUserPreferences(current.copy(premiumSubscribed = true, girlVideoUnlocked = true, walletCoins = current.walletCoins + 1399))
            } else if (amount == 1000) {
                repository.saveUserPreferences(current.copy(premiumSubscribed = true, girlVideoUnlocked = true, walletCoins = current.walletCoins + 3000))
            } else {
                repository.saveUserPreferences(current.copy(walletCoins = current.walletCoins + 79))
            }
        }
    }

    fun closePaymentScreen() {
        _enteredUtr.value = ""
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
}
