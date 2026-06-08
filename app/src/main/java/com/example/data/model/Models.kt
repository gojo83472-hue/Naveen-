package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "profiles")
@Serializable
data class UserProfile(
    @PrimaryKey val id: String = "current_user",
    val name: String = "Alex Mercer",
    val email: String = "alex@uskha.com",
    val unixUid: String = "880193485721",
    val balance: Double = 35.00, // Starting free coins for 35 coins
    val currency: String = "USK",
    val activeBankName: String = "Uskha Federal Bank",
    val activeBankLimit: Double = 300.00, // Small default limit to trigger "exceeded bank limit" naturally, user can increase this!
    val kycCompleted: Boolean = false,
    val lastVideoCallTime: Long = 0L,
    val isVip: Boolean = false,
    val vipType: String = "", // "weekly" or "monthly"
    val vipDailyCallsRemaining: Int = 0,
    val vipGirlsCallsRemaining: Int = 0,
    val referralsCount: Int = 0,
    val totalPurchasedCoinsThisWeek: Double = 0.0,
    val hasUsedOneTimeOffer: Boolean = false,
    val oneTimeFreeCallsRemaining: Int = 0
)

@Entity(tableName = "friends")
@Serializable
data class Friend(
    @PrimaryKey val id: String,
    val name: String,
    val bio: String,
    val initialBalance: Double = 0.0,
    val avatarColorSeed: Int = 0,
    val isOnline: Boolean = false
)

@Entity(tableName = "transactions")
@Serializable
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: String,
    val friendName: String,
    val amount: Double,
    val isSending: Boolean, // true if user is sending, false if receiving
    val date: Long = System.currentTimeMillis(),
    val note: String = "Direct transfer",
    val status: String = "Completed" // "Completed", "Pending", "Failed"
)

@Entity(tableName = "messages")
@Serializable
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
