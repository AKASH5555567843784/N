package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    // --- Memory Queries ---
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()

    // --- Planner Queries ---
    @Query("SELECT * FROM planner_items ORDER BY timestamp DESC")
    fun getAllPlannerItems(): Flow<List<PlannerEntity>>

    @Query("SELECT * FROM planner_items WHERE type = :type ORDER BY timestamp DESC")
    fun getPlannerItemsByType(type: String): Flow<List<PlannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannerItem(item: PlannerEntity)

    @Delete
    suspend fun deletePlannerItem(item: PlannerEntity)

    @Query("UPDATE planner_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updatePlannerItemStatus(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM planner_items")
    suspend fun clearAllPlannerItems()

    // --- User History Queries ---
    @Query("SELECT * FROM user_history ORDER BY timestamp DESC")
    fun getAllUserHistory(): Flow<List<UserHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserHistory(history: UserHistoryEntity)

    @Query("DELETE FROM user_history")
    suspend fun clearUserHistory()

    // --- Assistant Preference Queries ---
    @Query("SELECT * FROM assistant_preferences")
    fun getAllPreferences(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM assistant_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceByKey(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceEntity)

    // --- Chat Messages Queries ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteChatMessageById(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()
}
