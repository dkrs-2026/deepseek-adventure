package com.uusse.deepseekadventure.data.repository

import com.uusse.deepseekadventure.core.network.ChatRequest
import com.uusse.deepseekadventure.core.network.MessageDto
import com.uusse.deepseekadventure.core.network.SseClient
import com.uusse.deepseekadventure.core.network.StreamEvent
import com.uusse.deepseekadventure.core.network.StreamOptionsDto
import com.uusse.deepseekadventure.core.network.ThinkingDto
import com.uusse.deepseekadventure.domain.model.BalanceInfo
import com.uusse.deepseekadventure.domain.model.ChatMessage
import com.uusse.deepseekadventure.domain.model.Usage
import com.uusse.deepseekadventure.domain.repository.AppSettings
import com.uusse.deepseekadventure.domain.repository.ChatRepository

class ChatRepositoryImpl(private val sse: SseClient) : ChatRepository {

    override suspend fun stream(
        messages: List<ChatMessage>,
        settings: AppSettings,
        apiKey: String,
        onEvent: (StreamEvent) -> Unit,
    ) {
        sse.stream(buildRequest(messages, settings, stream = true), apiKey, onEvent)
    }

    override suspend fun complete(
        messages: List<ChatMessage>,
        settings: AppSettings,
        apiKey: String,
    ): Pair<String, Usage> = sse.complete(buildRequest(messages, settings, stream = false), apiKey)

    override suspend fun fetchBalance(apiKey: String): BalanceInfo = sse.fetchBalance(apiKey)

    override suspend fun fetchModels(apiKey: String): List<String> = sse.fetchModels(apiKey)

    private fun buildRequest(
        messages: List<ChatMessage>,
        settings: AppSettings,
        stream: Boolean,
    ) = ChatRequest(
        model = settings.model,
        messages = messages.map { MessageDto(it.role, it.content) },
        stream = stream,
        thinking = ThinkingDto(if (settings.thinkingEnabled) "enabled" else "disabled"),
        reasoning_effort = settings.reasoningEffort,
        stream_options = StreamOptionsDto(include_usage = true),
    )
}
