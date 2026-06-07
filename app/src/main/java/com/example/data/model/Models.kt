package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1,
    val ageVerified: Boolean = false,
    val premiumSubscribed: Boolean = false,
    val girlVideoUnlocked: Boolean = false,
    val completedReportsCount: Int = 0,
    val walletCoins: Int = 20, // Give some default coins
    val selectedGenderFilter: String = "All", // "All", "Girl", "Boy"
    val hasUsedFreeVideoCall: Boolean = false,
    val userGender: String = "Boy", // "Boy" or "Girl", defaults to "Boy"

    // Safety, Credentials & Secure Registration Logins
    val isLoggedIn: Boolean = false,
    val loginType: String = "", // "GMAIL" or "PHONE"
    val loggedInEmail: String = "",
    val loggedInPhone: String = "",
    val username: String = "Uskha Explorer",
    val avatarSeedOnAuth: String = "auth_seed",

    // Unique Identification System (Different for every user)
    val selfUserId: String = "", // Generates as e.g. "USKHA-74921"
    val selfInviteCode: String = "", // Generates as e.g. "USK-GHYX"
    val appliedInviteCode: String = "",
    val hasAppliedInvite: Boolean = false,

    // Social / Friends Network Matrix using JSON lists
    val friendsJson: String = "[]",
    val friendRequestsJson: String = "[]",

    // Safe mode: blurs incoming video streams by default
    val safeModeEnabled: Boolean = false,

    // Subscription status and simulated payment properties
    val subscriptionName: String = "",
    val subscriptionRenewalDate: String = "",
    val isSubscriptionActive: Boolean = false,
    val simulatedPaymentForceDecline: Boolean = false
)

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partnerName: String,
    val partnerAge: Int,
    val partnerGender: String, // "Girl", "Boy"
    val avatarSeed: String,
    val matchTime: Long = System.currentTimeMillis(),
    val isReported: Boolean = false
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val sender: String, // "user" or "stranger"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFlagged: Boolean = false,
    val flagReason: String? = null
)

@Entity(tableName = "moderation_reports")
data class ModerationReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reporterName: String = "You",
    val reportedPartnerName: String,
    val chatExcerpt: String,
    val scanVerdict: String, // "Safe", "Inappropriate - Blocked", "Under AI Investigation"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CREDIT", "DEBIT"
    val amount: Int,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

