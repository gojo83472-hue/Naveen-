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
    val hasUsedFreeVideoCall: Boolean = false
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
