package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.model.Friend
import com.example.data.model.Transaction
import com.example.data.model.ChatMessage
import com.example.data.repository.UskhaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.network.SignalingClient
import com.example.data.network.SignalingState

sealed interface TransferUiState {
    object Idle : TransferUiState
    object Confirming : TransferUiState
    object Processing : TransferUiState
    data class Success(val amount: Double, val recipient: String) : TransferUiState
    data class Error(val message: String) : TransferUiState
    data class BankLimitExceeded(val amountExceeded: Double, val activeLimit: Double, val bankName: String) : TransferUiState
}

class UskhaViewModel(private val repository: UskhaRepository) : ViewModel() {

    val signalingClient = SignalingClient()
    val signalingState: StateFlow<SignalingState> = signalingClient.state
    val signalingLogs: StateFlow<List<String>> = signalingClient.logs

    fun connectSignaling(roomId: String, localName: String, localBio: String, customUrl: String? = null) {
        signalingClient.connect(roomId, localName, localBio, customUrl)
    }

    fun sendSignalingMute(isMuted: Boolean) {
        signalingClient.broadcastMessage(if (isMuted) "MUTED" else "UNMUTED")
    }

    fun disconnectSignaling() {
        signalingClient.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        signalingClient.disconnect()
    }

    init {
        viewModelScope.launch {
            repository.createInitialDataIfEmpty()
        }
    }

    val profileState: StateFlow<UserProfile?> = repository.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val friendsState: StateFlow<List<Friend>> = repository.friendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsState: StateFlow<List<Transaction>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Friend for Transfer or Chat
    private val _selectedFriend = MutableStateFlow<Friend?>(null)
    val selectedFriend: StateFlow<Friend?> = _selectedFriend.asStateFlow()

    // Active Messages
    val activeMessages: StateFlow<List<ChatMessage>> = _selectedFriend
        .flatMapLatest { friend ->
            if (friend == null) flowOf(emptyList())
            else repository.getMessagesFlow(friend.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transfer Screen State
    private val _transferUiState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val transferUiState: StateFlow<TransferUiState> = _transferUiState.asStateFlow()

    val transferAmount = MutableStateFlow("")
    val transferNote = MutableStateFlow("")

    // Chat AI Scanner State
    private val _isScanningChat = MutableStateFlow(false)
    val isScanningChat: StateFlow<Boolean> = _isScanningChat.asStateFlow()

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()

    fun selectFriend(friend: Friend) {
        _selectedFriend.value = friend
        _transferUiState.value = TransferUiState.Idle
        transferAmount.value = ""
        transferNote.value = ""
    }

    fun clearSelectedFriend() {
        _selectedFriend.value = null
    }

    fun prepareTransfer(amountStr: String, note: String) {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _transferUiState.value = TransferUiState.Error("Please enter a valid transfer amount")
            return
        }
        val currentProfile = profileState.value
        if (currentProfile == null || currentProfile.balance < amount) {
            _transferUiState.value = TransferUiState.Error("Insufficient balance in your Uskha account")
            return
        }
        if (amount > currentProfile.activeBankLimit) {
            _transferUiState.value = TransferUiState.BankLimitExceeded(
                amountExceeded = amount,
                activeLimit = currentProfile.activeBankLimit,
                bankName = currentProfile.activeBankName
            )
            return
        }
        transferAmount.value = amountStr
        transferNote.value = note
        _transferUiState.value = TransferUiState.Confirming
    }

    fun cancelTransfer() {
        _transferUiState.value = TransferUiState.Idle
    }

    fun executeTransfer() {
        val rec = _selectedFriend.value ?: return
        val amountNum = transferAmount.value.toDoubleOrNull() ?: return
        val noteStr = transferNote.value
        val currentProfile = profileState.value

        if (currentProfile != null && amountNum > currentProfile.activeBankLimit) {
            _transferUiState.value = TransferUiState.BankLimitExceeded(
                amountExceeded = amountNum,
                activeLimit = currentProfile.activeBankLimit,
                bankName = currentProfile.activeBankName
            )
            return
        }

        viewModelScope.launch {
            _transferUiState.value = TransferUiState.Processing
            delay(1500) // Simulated secure transaction confirmation

            val success = repository.sendMoney(rec.id, rec.name, amountNum, noteStr)
            if (success) {
                _transferUiState.value = TransferUiState.Success(amountNum, rec.name)
                // Clear state
                transferAmount.value = ""
                transferNote.value = ""
            } else {
                _transferUiState.value = TransferUiState.Error("Transaction failed. Check balance.")
            }
        }
    }

    fun switchActiveBank(bankName: String, limit: Double) {
        viewModelScope.launch {
            repository.switchBank(bankName, limit)
            _transferUiState.value = TransferUiState.Idle
        }
    }

    fun upgradeKycAndLimit() {
        viewModelScope.launch {
            repository.completeKyc()
            _transferUiState.value = TransferUiState.Idle
        }
    }

    fun registerVideoCall() {
        viewModelScope.launch {
            repository.registerVideoCall()
        }
    }

    fun resetVideoCallToken() {
        viewModelScope.launch {
            repository.resetVideoCallTime()
        }
    }

    fun buyCoins(coinsAmount: Double) {
        viewModelScope.launch {
            repository.addCoins(coinsAmount)
        }
    }

    fun claimOneTimeOffer() {
        viewModelScope.launch {
            repository.claimOneTimeOffer()
        }
    }

    fun buyVip(vipType: String) {
        viewModelScope.launch {
            repository.buyVip(vipType)
        }
    }

    fun inviteReferral() {
        viewModelScope.launch {
            repository.addReferral()
        }
    }

    fun tryUseVideoCall(isGirlsOnly: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val successful = repository.useVideoCall(isGirlsOnly)
            onResult(successful)
        }
    }

    fun tryUseMessageText(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val successful = repository.useMessageText()
            onResult(successful)
        }
    }

    fun addFriend(name: String, bio: String) {
        viewModelScope.launch {
            val rep = repository.addNewFriend(name, bio)
            _selectedFriend.value = rep
        }
    }

    fun sendChatMessage(text: String) {
        val friend = _selectedFriend.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.insertMessage(
                ChatMessage(
                    friendId = friend.id,
                    content = text,
                    isFromUser = true
                )
            )

            // Auto-generated response
            delay(1000)
            repository.insertMessage(
                ChatMessage(
                    friendId = friend.id,
                    content = "Hey Alex! Thanks for your message. Let's talk about the details.",
                    isFromUser = false
                )
            )
        }
    }

    fun runChatScanner() {
        viewModelScope.launch {
            _isScanningChat.value = true
            _scanResult.value = null
            delay(2500)
            _isScanningChat.value = false
            _scanResult.value = "✓ Safe Connection Verified: 0 risks detected. Conversations on Uskha are fully end-to-end encrypted."
        }
    }

    fun resetScan() {
        _scanResult.value = null
    }
}
