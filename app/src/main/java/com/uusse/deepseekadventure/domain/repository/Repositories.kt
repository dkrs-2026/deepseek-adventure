package com.uusse.deepseekadventure.domain.repository

import com.uusse.deepseekadventure.core.network.StreamEvent
import com.uusse.deepseekadventure.domain.model.BalanceInfo
import com.uusse.deepseekadventure.domain.model.ChatMessage
import com.uusse.deepseekadventure.domain.model.Conversation
import com.uusse.deepseekadventure.domain.model.Message
import com.uusse.deepseekadventure.domain.model.Usage
import com.uusse.deepseekadventure.domain.model.UsageRecord
import com.uusse.deepseekadventure.domain.model.WorldBookEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** 全部偏好（API Key 单独加密存储，不放这里） */
@Serializable
data class AppSettings(
    val model: String = "deepseek-flash",
    val systemPrompt: String = "你是一位专业的文字互动冒险游戏主持人。请保持剧情连贯，描述生动，并等待玩家输入。",
    val thinkingEnabled: Boolean = false,
    val reasoningEffort: String = "high",
    val themeMode: String = "system",          // system / light / dark
    val contextBudget: Int = 900000,
    val holidaysCsv: String = "",
    val priceFlashHit: Double = 0.02,
    val priceFlashMiss: Double = 1.0,
    val priceFlashOut: Double = 4.0,
    val priceProHit: Double = 0.15,
    val priceProMiss: Double = 4.5,
    val priceProOut: Double = 13.5,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun saveApiKey(key: String)
    fun apiKey(): String
    fun maskedApiKey(): String
    fun hasApiKey(): Boolean
}

interface ConversationRepository {
    fun observeConversations(): Flow<List<Conversation>>
    suspend fun create(title: String, model: String, systemPrompt: String): Conversation
    suspend fun get(id: String): Conversation?
    suspend fun save(conversation: Conversation)
    suspend fun delete(id: String)
    fun observeMessages(conversationId: String): Flow<List<Message>>
    suspend fun getMessage(id: String): Message?
    suspend fun insertMessage(message: Message)
    suspend fun updateMessage(message: Message)
    /** 从 root 到指定叶子的上下文路径 */
    suspend fun pathTo(leafId: String): List<Message>
}

interface ChatRepository {
    suspend fun stream(
        messages: List<ChatMessage>,
        settings: AppSettings,
        apiKey: String,
        onEvent: (StreamEvent) -> Unit,
    )
    suspend fun complete(
        messages: List<ChatMessage>,
        settings: AppSettings,
        apiKey: String,
    ): Pair<String, Usage>
    suspend fun fetchBalance(apiKey: String): BalanceInfo
    suspend fun fetchModels(apiKey: String): List<String>
}

interface UsageRepository {
    suspend fun insert(record: UsageRecord)
    fun observeAll(): Flow<List<UsageRecord>>
}

interface BalanceRepository {
    suspend fun refresh(apiKey: String): BalanceInfo
    fun observe(): Flow<BalanceInfo?>
}

interface WorldBookRepository {
    fun observeAll(): Flow<List<WorldBookEntry>>
    suspend fun insert(entry: WorldBookEntry)
    suspend fun update(entry: WorldBookEntry)
    suspend fun delete(id: String)
}
