package com.example.data.repository

import com.example.data.database.UskhaDao
import com.example.data.model.ChatMessage
import com.example.data.model.MatchHistory
import com.example.data.model.ModerationReport
import com.example.data.model.UserPreferences
import com.example.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow

class UskhaRepository(private val dao: UskhaDao) {

    val userPreferencesFlow: Flow<UserPreferences?> = dao.getUserPreferencesFlow()
    val matchHistoryFlow: Flow<List<MatchHistory>> = dao.getMatchHistoryFlow()
    val reportsFlow: Flow<List<ModerationReport>> = dao.getModerationReportsFlow()
    val walletTransactionsFlow: Flow<List<WalletTransaction>> = dao.getWalletTransactionsFlow()

    fun getChatMessagesFlow(matchId: Int): Flow<List<ChatMessage>> = dao.getChatMessagesFlow(matchId)

    suspend fun getUserPreferencesDirect(): UserPreferences {
        var prefs = dao.getUserPreferencesDirect()
        if (prefs == null) {
            prefs = UserPreferences()
        }
        var updated = false
        var nextPrefs = prefs
        if (nextPrefs.selfUserId.isEmpty()) {
            val randomId = "USKHA-" + (10000..99999).random()
            nextPrefs = nextPrefs.copy(selfUserId = randomId)
            updated = true
        }
        if (nextPrefs.selfInviteCode.isEmpty()) {
            val randomInviteCode = "USK-" + ('A'..'Z').shuffled().take(5).joinToString("")
            nextPrefs = nextPrefs.copy(selfInviteCode = randomInviteCode)
            updated = true
        }
        if (updated || prefs == null) {
            dao.saveUserPreferences(nextPrefs)
        }
        return nextPrefs
    }

    suspend fun saveUserPreferences(preferences: UserPreferences) {
        dao.saveUserPreferences(preferences)
    }

    suspend fun setAgeVerified(isVerified: Boolean) {
        val current = getUserPreferencesDirect()
        dao.saveUserPreferences(current.copy(ageVerified = isVerified))
    }

    suspend fun updateSubscription(subscribed: Boolean) {
        val current = getUserPreferencesDirect()
        dao.saveUserPreferences(current.copy(premiumSubscribed = subscribed))
    }

    suspend fun unlockGirlVideo(unlocked: Boolean) {
        val current = getUserPreferencesDirect()
        dao.saveUserPreferences(current.copy(girlVideoUnlocked = unlocked))
    }

    suspend fun addCoins(amount: Int) {
        val current = getUserPreferencesDirect()
        dao.saveUserPreferences(current.copy(walletCoins = current.walletCoins + amount))
    }

    suspend fun deductCoins(amount: Int): Boolean {
        val current = getUserPreferencesDirect()
        if (current.walletCoins >= amount) {
            dao.saveUserPreferences(current.copy(walletCoins = current.walletCoins - amount))
            return true
        }
        return false
    }

    // Matches
    suspend fun insertMatch(match: MatchHistory): Int {
        return dao.insertMatch(match).toInt()
    }

    suspend fun updateMatch(match: MatchHistory) {
        dao.updateMatch(match)
    }

    suspend fun deleteMatch(matchId: Int) {
        dao.deleteMatchById(matchId)
        dao.deleteChatByMatchId(matchId)
    }

    // Chats
    suspend fun insertChatMessage(message: ChatMessage): Int {
        return dao.insertChatMessage(message).toInt()
    }

    suspend fun flagChatMessage(messageId: Int, reason: String) {
        dao.flagChatMessage(messageId, reason)
    }

    // Moderation reports
    suspend fun insertModerationReport(report: ModerationReport) {
        dao.insertModerationReport(report)
        // Increment reported count in preferences
        val prefs = getUserPreferencesDirect()
        dao.saveUserPreferences(prefs.copy(completedReportsCount = prefs.completedReportsCount + 1))
    }

    suspend fun insertWalletTransaction(transaction: WalletTransaction) {
        dao.insertWalletTransaction(transaction)
    }
}
