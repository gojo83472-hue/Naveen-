package com.example.data.database

import androidx.room.*
import com.example.data.model.UserProfile
import com.example.data.model.Friend
import com.example.data.model.Transaction
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface UskhaDao {

    // Profile
    @Query("SELECT * FROM profiles WHERE id = 'current_user' LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM profiles WHERE id = 'current_user' LIMIT 1")
    suspend fun getProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    // Friends
    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun getFriendsFlow(): Flow<List<Friend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<Friend>)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getTransactionsFlow(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // Chat Message
    @Query("SELECT * FROM messages WHERE friendId = :friendId ORDER BY timestamp ASC")
    fun getMessagesFlow(friendId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
}
