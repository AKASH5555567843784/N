package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MemoryEntity::class, PlannerEntity::class, UserHistoryEntity::class, PreferenceEntity::class, ChatMessageEntity::class], version = 3, exportSchema = false)
abstract class AssistantDatabase : RoomDatabase() {
    abstract fun assistantDao(): AssistantDao

    companion object {
        @Volatile
        private var INSTANCE: AssistantDatabase? = null

        fun getDatabase(context: Context): AssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AssistantDatabase::class.java,
                    "ninety_five_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
