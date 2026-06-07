package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.MatchHistory
import com.example.data.model.ModerationReport
import com.example.data.model.UserPreferences
import com.example.data.model.WalletTransaction

@Database(
    entities = [
        UserPreferences::class,
        MatchHistory::class,
        ChatMessage::class,
        ModerationReport::class,
        WalletTransaction::class
    ],
    version = 7,
    exportSchema = false
)
abstract class UskhaDatabase : RoomDatabase() {

    abstract fun uskhaDao(): UskhaDao

    companion object {
        @Volatile
        private var INSTANCE: UskhaDatabase? = null

        fun getDatabase(context: Context): UskhaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UskhaDatabase::class.java,
                    "uskha_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
