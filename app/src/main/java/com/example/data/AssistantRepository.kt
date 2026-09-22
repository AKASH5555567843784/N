package com.example.data

import kotlinx.coroutines.flow.Flow

class AssistantRepository(private val dao: AssistantDao) {
    // Memories
    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemories()
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> = dao.getMemoriesByCategory(category)
    suspend fun searchMemories(query: String): List<MemoryEntity> = dao.searchMemories(query)
    suspend fun insertMemory(memory: MemoryEntity) = dao.insertMemory(memory)
    suspend fun deleteMemory(memory: MemoryEntity) = dao.deleteMemory(memory)
    suspend fun clearAllMemories() = dao.clearAllMemories()

    // Planner Items
    val allPlannerItems: Flow<List<PlannerEntity>> = dao.getAllPlannerItems()
    fun getPlannerItemsByType(type: String): Flow<List<PlannerEntity>> = dao.getPlannerItemsByType(type)
    suspend fun insertPlannerItem(item: PlannerEntity) = dao.insertPlannerItem(item)
    suspend fun deletePlannerItem(item: PlannerEntity) = dao.deletePlannerItem(item)
    suspend fun updatePlannerItemStatus(id: Long, isCompleted: Boolean) = dao.updatePlannerItemStatus(id, isCompleted)
    suspend fun clearAllPlannerItems() = dao.clearAllPlannerItems()

    // User History
    val allUserHistory: Flow<List<UserHistoryEntity>> = dao.getAllUserHistory()
    suspend fun insertUserHistory(history: UserHistoryEntity) = dao.insertUserHistory(history)
    suspend fun clearUserHistory() = dao.clearUserHistory()

    // Assistant Preferences
    val allPreferences: Flow<List<PreferenceEntity>> = dao.getAllPreferences()
    suspend fun getPreferenceByKey(key: String): PreferenceEntity? = dao.getPreferenceByKey(key)
    suspend fun insertPreference(preference: PreferenceEntity) = dao.insertPreference(preference)

    // Chat Messages
    val allChatMessages: Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()
    suspend fun insertChatMessage(message: ChatMessageEntity) = dao.insertChatMessage(message)
    suspend fun deleteChatMessageById(id: Long) = dao.deleteChatMessageById(id)
    suspend fun clearAllChatMessages() = dao.clearAllChatMessages()
}
