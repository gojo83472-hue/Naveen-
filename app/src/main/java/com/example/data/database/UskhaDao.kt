package com.example.data.database

import androidx.room.*
import com.example.data.model.ChatMessage
import com.example.data.model.MatchHistory
import com.example.data.model.ModerationReport
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UskhaDao {

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getUserPreferencesFlow(): Flow<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferencesDirect(): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPreferences(prefs: UserPreferences)

    @Query("SELECT * FROM match_history ORDER BY matchTime DESC")
    fun getMatchHistoryFlow(): Flow<List<MatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistory): Long

    @Update
    suspend fun updateMatch(match: MatchHistory)

    @Query("SELECT * FROM chat_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getChatMessagesFlow(matchId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    suspend fun getChatMessagesDirect(matchId: Int): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Query("UPDATE chat_messages SET isFlagged = 1, flagReason = :reason WHERE id = :messageId")
    suspend fun flagChatMessage(messageId: Int, reason: String)

    @Query("SELECT * FROM moderation_reports ORDER BY timestamp DESC")
    fun getModerationReportsFlow(): Flow<List<ModerationReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModerationReport(report: ModerationReport): Long

    @Query("DELETE FROM match_history WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Int)

    @Query("DELETE FROM chat_messages WHERE matchId = :matchId")
    suspend fun deleteChatByMatchId(matchId: Int)
}
