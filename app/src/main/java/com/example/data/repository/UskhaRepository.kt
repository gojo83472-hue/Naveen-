package com.example.data.repository

import com.example.data.database.UskhaDao
import com.example.data.model.UserProfile
import com.example.data.model.Friend
import com.example.data.model.Transaction
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UskhaRepository(private val dao: UskhaDao) {

    val profileFlow: Flow<UserProfile?> = dao.getProfileFlow()
    val friendsFlow: Flow<List<Friend>> = dao.getFriendsFlow()
    val transactionsFlow: Flow<List<Transaction>> = dao.getTransactionsFlow()

    fun getMessagesFlow(friendId: String): Flow<List<ChatMessage>> {
        return dao.getMessagesFlow(friendId)
    }

    suspend fun insertMessage(message: ChatMessage) {
        dao.insertMessage(message)
    }

    suspend fun createInitialDataIfEmpty() {
        val currentProfile = dao.getProfileDirect()
        if (currentProfile == null) {
            dao.insertProfile(UserProfile(name = "Alex Mercer", email = "alex@uskha.com", unixUid = "889301824756", balance = 35.00))
        }

        val currentFriends = dao.getFriendsFlow().first()
        if (currentFriends.isEmpty()) {
            val initialFriends = listOf(
                Friend("880594382012", "Sarah Connor", "Safety Specialist & Fitness Coach", 120.0, 1, true),
                Friend("880193482756", "Anup (Verified Match)", "Clear Voice & Face Verified Specialist", 150.0, 2, true),
                Friend("880918342205", "Marcus Wright", "Tech Innovator & Cybersecurity consultant", 45.0, 3, false),
                Friend("880293847561", "Kyle Reese", "History Buff & Strategic Planner", 90.0, 4, true),
                Friend("880491837562", "John Connor", "Finance Enthusiast & Leader", 310.0, 5, true),
                Friend("880793485721", "Dani Ramos", "Creative UX Specialist & Designer", 85.0, 6, false)
            )
            dao.insertFriends(initialFriends)

            // Insert a few transactions for initial logs
            dao.insertTransaction(Transaction(friendId = "880594382012", friendName = "Sarah Connor", amount = 3.0, isSending = true, note = "Secure Text Session Fee"))
            dao.insertTransaction(Transaction(friendId = "880293847561", friendName = "Kyle Reese", amount = 20.0, isSending = true, note = "Video Match Setup Fee"))
            dao.insertTransaction(Transaction(friendId = "880193482756", friendName = "Anup (Verified Match)", amount = 25.0, isSending = true, note = "Perfect Audio Match Calibration Fee"))
        }
    }

    suspend fun addCoins(coinsAmount: Double) {
        val profile = dao.getProfileDirect() ?: return
        dao.insertProfile(
            profile.copy(
                balance = profile.balance + coinsAmount,
                totalPurchasedCoinsThisWeek = profile.totalPurchasedCoinsThisWeek + coinsAmount
            )
        )
    }

    suspend fun addReferral() {
        val profile = dao.getProfileDirect() ?: return
        dao.insertProfile(
            profile.copy(
                balance = profile.balance + 10.0,
                referralsCount = profile.referralsCount + 1
            )
        )
    }

    suspend fun claimOneTimeOffer() {
        val profile = dao.getProfileDirect() ?: return
        if (!profile.hasUsedOneTimeOffer) {
            dao.insertProfile(
                profile.copy(
                    balance = profile.balance + 50.0,
                    hasUsedOneTimeOffer = true,
                    oneTimeFreeCallsRemaining = profile.oneTimeFreeCallsRemaining + 1,
                    totalPurchasedCoinsThisWeek = profile.totalPurchasedCoinsThisWeek + 50.0
                )
            )
        }
    }

    suspend fun buyVip(vipType: String) {
        val profile = dao.getProfileDirect() ?: return
        val dailyCalls = if (vipType == "weekly") 15 else 60
        val girlsCalls = if (vipType == "weekly") 3 else 4
        dao.insertProfile(
            profile.copy(
                isVip = true,
                vipType = vipType,
                vipDailyCallsRemaining = dailyCalls,
                vipGirlsCallsRemaining = girlsCalls
            )
        )
    }

    suspend fun useVideoCall(isGirlsOnly: Boolean): Boolean {
        val profile = dao.getProfileDirect() ?: return false
        
        if (profile.oneTimeFreeCallsRemaining > 0) {
            dao.insertProfile(
                profile.copy(
                    oneTimeFreeCallsRemaining = profile.oneTimeFreeCallsRemaining - 1
                )
            )
            return true
        }
        
        if (profile.isVip) {
            if (isGirlsOnly) {
                if (profile.vipGirlsCallsRemaining > 0) {
                    dao.insertProfile(profile.copy(vipGirlsCallsRemaining = profile.vipGirlsCallsRemaining - 1))
                    return true
                }
            } else {
                if (profile.vipDailyCallsRemaining > 0) {
                    dao.insertProfile(profile.copy(vipDailyCallsRemaining = profile.vipDailyCallsRemaining - 1))
                    return true
                }
            }
        }
        
        // No VIP usage left, deduct coins
        val cost = if (isGirlsOnly) 35.0 else 20.0
        if (profile.balance >= cost) {
            dao.insertProfile(profile.copy(balance = profile.balance - cost))
            return true
        }
        
        return false
    }

    suspend fun useMessageText(): Boolean {
        val profile = dao.getProfileDirect() ?: return false
        if (profile.isVip) {
            return true // Free chat under VIP
        }
        // Deduct 3 coins for standard chat session
        if (profile.balance >= 3.0) {
            dao.insertProfile(profile.copy(balance = profile.balance - 3.0))
            return true
        }
        return false
    }

    suspend fun sendMoney(friendId: String, friendName: String, amount: Double, note: String): Boolean {
        if (amount <= 0) return false
        val profile = dao.getProfileDirect() ?: return false
        if (profile.balance < amount) return false

        // Deduct balance
        val updatedProfile = profile.copy(balance = profile.balance - amount)
        dao.insertProfile(updatedProfile)

        // Insert Transaction
        val tx = Transaction(
            friendId = friendId,
            friendName = friendName,
            amount = amount,
            isSending = true,
            note = note.ifBlank { "Direct Transfer" }
        )
        dao.insertTransaction(tx)

        // Generate synthetic confirmation answer from friend
        val botMessage = ChatMessage(
            friendId = friendId,
            content = "Received ${amount} USK. Thank you! 🙏",
            isFromUser = false
        )
        dao.insertMessage(botMessage)

        return true
    }

    suspend fun requestMoney(friendId: String, friendName: String, amount: Double, note: String) {
        val tx = Transaction(
            friendId = friendId,
            friendName = friendName,
            amount = amount,
            isSending = false,
            note = note.ifBlank { "Requested funds" },
            status = "Pending"
        )
        dao.insertTransaction(tx)
    }

    suspend fun addNewFriend(name: String, bio: String): Friend {
        val friendId = (880000000000L + (Math.random() * 9999999999L).toLong()).toString()
        val friend = Friend(
            id = friendId,
            name = name,
            bio = bio,
            avatarColorSeed = (name.length * 3) % 6,
            isOnline = true
        )
        dao.insertFriend(friend)
        return friend
    }

    suspend fun updateBankLimit(newLimit: Double) {
        val profile = dao.getProfileDirect() ?: return
        dao.insertProfile(profile.copy(activeBankLimit = newLimit))
    }

    suspend fun switchBank(bankName: String, limit: Double) {
        val profile = dao.getProfileDirect() ?: return
        dao.insertProfile(profile.copy(activeBankName = bankName, activeBankLimit = limit))
    }

    suspend fun completeKyc() {
        val profile = dao.getProfileDirect() ?: return
        dao.insertProfile(profile.copy(kycCompleted = true, activeBankLimit = 5000.00))
    }

    suspend fun registerVideoCall() {
        val profile = dao.getProfileDirect() ?: return
        dao.insertProfile(profile.copy(lastVideoCallTime = System.currentTimeMillis()))
    }

    suspend fun resetVideoCallTime() {
        val profile = dao.getProfileDirect() ?: return
        dao.insertProfile(profile.copy(lastVideoCallTime = 0L))
    }
}
