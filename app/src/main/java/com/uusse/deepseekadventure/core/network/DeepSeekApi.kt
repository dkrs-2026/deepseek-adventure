package com.uusse.deepseekadventure.core.network

import com.uusse.deepseekadventure.domain.model.Usage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val apiJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ---------- 请求 DTO ----------

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean,
    val thinking: ThinkingDto,
    val reasoning_effort: String,
    val stream_options: StreamOptionsDto,
)

@Serializable
data class MessageDto(val role: String, val content: String)

@Serializable
data class ThinkingDto(val type: String)   // enabled / disabled

@Serializable
data class StreamOptionsDto(val include_usage: Boolean = true)

// ---------- 响应 DTO ----------

@Serializable
data class ChatChunk(
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
)

@Serializable
data class ChoiceDto(
    val delta: DeltaDto = DeltaDto(),
    val message: ResponseMessageDto? = null,
)

@Serializable
data class DeltaDto(
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    val role: String? = null,
)

@Serializable
data class ResponseMessageDto(
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

@Serializable
data class UsageDto(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0,
    val prompt_cache_hit_tokens: Int = 0,
    val prompt_cache_miss_tokens: Int = 0,
    val prompt_tokens_details: PromptDetailsDto? = null,
    val completion_tokens_details: CompletionDetailsDto? = null,
)

@Serializable
data class PromptDetailsDto(val cached_tokens: Int = 0)

@Serializable
data class CompletionDetailsDto(val reasoning_tokens: Int = 0)

@Serializable
data class BalanceDto(
    val is_available: Boolean = false,
    val balance_infos: List<BalanceInfoDto> = emptyList(),
)

@Serializable
data class BalanceInfoDto(
    val currency: String = "CNY",
    val total_balance: String = "0",
    val granted_balance: String = "0",
    val topped_up_balance: String = "0",
)

@Serializable
data class ModelsDto(val data: List<ModelDto> = emptyList())

@Serializable
data class ModelDto(val id: String = "")

// ---------- 归一化 ----------

fun UsageDto.toUsage(): Usage = Usage(
    promptTokens = prompt_tokens,
    completionTokens = completion_tokens,
    totalTokens = total_tokens,
    cacheHitTokens = if (prompt_cache_hit_tokens != 0) prompt_cache_hit_tokens
    else (prompt_tokens_details?.cached_tokens ?: 0),
    cacheMissTokens = prompt_cache_miss_tokens,
    reasoningTokens = completion_tokens_details?.reasoning_tokens ?: 0,
)
