package com.uusse.deepseekadventure.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rootMessageId: String?,
    val activeLeafId: String?,
    val modelId: String,
    val systemPrompt: String,
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId"), Index("parentId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val parentId: String?,
    val role: String,
    val content: String,
    val reasoningContent: String,
    val thinkingEnabled: Boolean,
    val reasoningEffort: String,
    val modelId: String,
    val status: String,
    val branchLabel: String?,
    val createdAt: Long,
)

@Entity(tableName = "usage_records", indices = [Index("conversationId")])
data class UsageRecordEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val conversationId: String,
    val timestamp: Long,
    val modelId: String,
    val isPeak: Boolean,
    val promptTokens: Int,
    val cacheHitTokens: Int,
    val cacheMissTokens: Int,
    val completionTokens: Int,
    val reasoningTokens: Int,
    val estimatedCostMicro: Long,
)

@Entity(tableName = "world_book")
data class WorldBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val keywords: String,
    val enabled: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "balance_snapshot")
data class BalanceSnapshotEntity(
    @PrimaryKey val singleton: Int = 1,
    val json: String,
)
