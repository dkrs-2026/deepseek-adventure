package com.uusse.deepseekadventure.domain.model

import kotlinx.serialization.Serializable

/** 消息角色 */
enum class MessageRole { SYSTEM, USER, ASSISTANT }

/** 消息状态：完整 / 生成中 / 中断 / 错误 */
enum class MessageStatus { COMPLETE, STREAMING, INTERRUPTED, ERROR }

/**
 * 会话。通过 rootMessageId 与 activeLeafId 在消息树上定位当前路径。
 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rootMessageId: String? = null,
    val activeLeafId: String? = null,
    val modelId: String = "deepseek-flash",
    val systemPrompt: String = "",
)

/**
 * 消息节点。parentId 形成树结构，支持编辑/重试/分支而不覆盖历史。
 */
data class Message(
    val id: String,
    val conversationId: String,
    val parentId: String?,
    val role: MessageRole,
    val content: String,
    val reasoningContent: String = "",
    val thinkingEnabled: Boolean = false,
    val reasoningEffort: String = "high",
    val modelId: String = "deepseek-flash",
    val status: MessageStatus = MessageStatus.COMPLETE,
    val branchLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 本地用量记录（金额单位：微元，避免浮点误差） */
data class UsageRecord(
    val id: String,
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

/** 余额快照（来自官方 GET /user/balance） */
@Serializable
data class BalanceInfo(
    val isAvailable: Boolean,
    val currency: String,
    val totalBalance: String,
    val grantedBalance: String,
    val toppedUpBalance: String,
    val fetchedAt: Long = System.currentTimeMillis(),
)

/** 世界书 / 设定集条目 */
data class WorldBookEntry(
    val id: String,
    val title: String,
    val content: String,
    val keywords: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** API 返回的 token 用量（已归一化） */
data class Usage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val cacheHitTokens: Int = 0,
    val cacheMissTokens: Int = 0,
    val reasoningTokens: Int = 0,
)

/** 发给模型的单条消息（role 用字符串，便于直接序列化） */
data class ChatMessage(val role: String, val content: String)
