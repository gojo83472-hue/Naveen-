package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.UserProfile
import com.example.data.model.Friend
import com.example.data.model.Transaction
import com.example.data.model.ChatMessage

@Database(
    entities = [UserProfile::class, Friend::class, Transaction::class, ChatMessage::class],
    version = 4,
    exportSchema = false
)
abstract class UskhaDatabase : RoomDatabase() {
    abstract fun uskhaDao(): UskhaDao
}
