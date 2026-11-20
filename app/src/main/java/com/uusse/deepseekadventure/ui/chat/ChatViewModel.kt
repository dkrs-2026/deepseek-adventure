package com.uusse.deepseekadventure.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uusse.deepseekadventure.core.network.StreamEvent
import com.uusse.deepseekadventure.core.tokenizer.TokenEstimator
import com.uusse.deepseekadventure.core.util.Pricing
import com.uusse.deepseekadventure.core.util.TimeUtil
import com.uusse.deepseekadventure.data.AppContainer
import com.uusse.deepseekadventure.domain.model.ChatMessage
import com.uusse.deepseekadventure.domain.model.Conversation
import com.uusse.deepseekadventure.domain.model.Message
import com.uusse.deepseekadventure.domain.model.MessageRole
import com.uusse.deepseekadventure.domain.model.MessageStatus
import com.uusse.deepseekadventure.domain.model.Usage
import com.uusse.deepseekadventure.domain.model.UsageRecord
import com.uusse.deepseekadventure.domain.repository.AppSettings
import com.uusse.deepseekadventure.core.util.PriceTable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val streamingContent: String = "",
    val streamingReasoning: String = "",
    val reasoningExpanded: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val estimatedTokens: Int = 0,
    val useStreaming: Boolean = true,
    val consecutiveFailures: Int = 0,
    val thinkingEnabled: Boolean = false,
    val reasoningEffort: String = "high",
    val model: String = "deepseek-flash",
    val branchLabel: String? = null,
)

class ChatViewModel(
    private val container: AppContainer,
    private val conversationId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var settings = AppSettings()
    private var conversation: Conversation? = null
    private var currentJob: Job? = null
    private val reasoningBuf = StringBuilder()
    private val contentBuf = StringBuilder()
    private var lastEmit = 0L

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { s ->
                settings = s
                _uiState.update {
                    it.copy(
                        thinkingEnabled = s.thinkingEnabled,
                        reasoningEffort = s.reasoningEffort,
                        model = s.model,
                    )
                }
            }
        }
        viewModelScope.launch {
            conversation = container.conversationRepository.get(conversationId)
            container.conversationRepository.observeMessages(conversationId).collect { list ->
                _uiState.update { it.copy(messages = list) }
            }
        }
        viewModelScope.launch { refreshTokenEstimate() }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
        viewModelScope.launch { refreshTokenEstimate() }
    }

    fun setThinking(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.update { it.copy(thinkingEnabled = enabled) } }
    }

    fun setReasoningEffort(effort: String) {
        viewModelScope.launch { container.settingsRepository.update { it.copy(reasoningEffort = effort) } }
    }

    fun setUseStreaming(enabled: Boolean) {
        _uiState.update { it.copy(useStreaming = enabled) }
    }

    fun toggleReasoning() {
        _uiState.update { it.copy(reasoningExpanded = !it.reasoningExpanded) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissInfo() {
        _uiState.update { it.copy(info = null) }
    }

    fun send() {
        val text = _uiState.value.input.trim()
        if (text.isBlank() || _uiState.value.isGenerating) return
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val conv = conversation
            if (conv == null) {
                _uiState.update { it.copy(error = "请先创建会话") }
                return@launch
            }
            val key = container.settingsRepository.apiKey()
            if (key.isBlank()) {
                _uiState.update { it.copy(error = "请先在设置页输入 API Key") }
                return@launch
            }
            val parentId = conv.activeLeafId
            val path = if (parentId != null) container.conversationRepository.pathTo(parentId) else emptyList()
            val messages = buildMessages(path, text)
            _uiState.update {
                it.copy(info = "本次估算 ${TokenEstimator.estimateMessages(messages.map { m -> m.role to m.content })} tokens")
            }

            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conv.id,
                parentId = parentId,
                role = MessageRole.USER,
                content = text,
                createdAt = System.currentTimeMillis(),
            )
            container.conversationRepository.insertMessage(userMsg)
            conversation = conv.copy(
                rootMessageId = conv.rootMessageId ?: userMsg.id,
                activeLeafId = userMsg.id,
                updatedAt = System.currentTimeMillis(),
            )
            container.conversationRepository.save(conversation!!)
            _uiState.update { it.copy(input = "") }
            runGeneration(messages, userMsg.id)
        }
    }

    fun retry(messageId: String) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val msg = container.conversationRepository.getMessage(messageId) ?: return@launch
            val parentId = msg.parentId ?: return@launch
            val path = container.conversationRepository.pathTo(parentId)
            runGeneration(buildMessages(path), parentId)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val msg = container.conversationRepository.getMessage(messageId) ?: return@launch
            if (msg.role != MessageRole.USER) return@launch
            val conv = conversation ?: return@launch
            val newUser = msg.copy(
                id = UUID.randomUUID().toString(),
                content = newContent,
                parentId = msg.parentId,
                createdAt = System.currentTimeMillis(),
                branchLabel = "编辑",
            )
            container.conversationRepository.insertMessage(newUser)
            conversation = conv.copy(activeLeafId = newUser.id, updatedAt = System.currentTimeMillis())
            container.conversationRepository.save(conversation!!)
            val path = container.conversationRepository.pathTo(newUser.id)
            runGeneration(buildMessages(path), newUser.id)
        }
    }

    fun branchFrom(messageId: String) {
        viewModelScope.launch {
            val msg = container.conversationRepository.getMessage(messageId) ?: return@launch
            val conv = conversation ?: return@launch
            conversation = conv.copy(activeLeafId = messageId, updatedAt = System.currentTimeMillis())
            container.conversationRepository.save(conversation!!)
            _uiState.update {
                it.copy(
                    info = "已从该消息创建分支，后续发送走新路径；原路径仍保留在历史中",
                    branchLabel = msg.branchLabel ?: "分支",
                )
            }
        }
    }

    fun generateSummary() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val conv = conversation ?: return@launch
            val leaf = conv.activeLeafId ?: return@launch
            val path = container.conversationRepository.pathTo(leaf)
            if (path.size < 6) {
                _uiState.update { it.copy(info = "消息较少，暂不需要摘要") }
                return@launch
            }
            val key = container.settingsRepository.apiKey()
            if (key.isBlank()) {
                _uiState.update { it.copy(error = "请先设置 API Key") }
                return@launch
            }
            _uiState.update { it.copy(isGenerating = true, info = "正在生成前情摘要…") }
            val source = path.take(path.size / 2)
                .joinToString("\n") { "${it.role.name}: ${it.content.take(400)}" }
            val summaryMessages = listOf(
                ChatMessage("system", "请把以下对话历史压缩为一段简洁的中文前情摘要，保留角色设定、关键事件和当前剧情状态。"),
                ChatMessage("user", source),
            )
            try {
                val (summary, _) = container.chatRepository.complete(summaryMessages, settings, key)
                val summaryMsg = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conv.id,
                    parentId = null,
                    role = MessageRole.SYSTEM,
                    content = "[前情摘要]\n${summary.trim()}",
                    branchLabel = "摘要",
                    status = MessageStatus.COMPLETE,
                )
                container.conversationRepository.insertMessage(summaryMsg)
                conversation = conv.copy(
                    rootMessageId = summaryMsg.id,
                    activeLeafId = summaryMsg.id,
                    updatedAt = System.currentTimeMillis(),
                )
                container.conversationRepository.save(conversation!!)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        info = "已生成摘要并作为当前上下文起点；原始消息仍保留在历史中",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = "摘要生成失败：${e.message}") }
            }
        }
    }

    private suspend fun buildMessages(path: List<Message>, userInput: String? = null): List<ChatMessage> {
        val worldText = container.worldBookRepository.observeAll().first()
            .filter { it.enabled }
            .joinToString("\n\n") { "【${it.title}】\n${it.content}" }
        val sys = buildString {
            append(settings.systemPrompt.ifBlank { "你是一位专业的文字互动冒险游戏主持人。" })
            if (worldText.isNotBlank()) append("\n\n[世界书/设定集]\n").append(worldText)
        }
        val result = mutableListOf(ChatMessage("system", sys))
        path.filter { it.role != MessageRole.SYSTEM }.forEach {
            result.add(ChatMessage(it.role.name.lowercase(), it.content))
        }
        if (userInput != null) result.add(ChatMessage("user", userInput))
        return result
    }

    private suspend fun refreshTokenEstimate() {
        val conv = conversation ?: return
        val leaf = conv.activeLeafId ?: conv.rootMessageId ?: return
        val path = container.conversationRepository.pathTo(leaf)
        val base = TokenEstimator.estimateMessages(
            path.filter { it.role != MessageRole.SYSTEM }.map { it.role.name.lowercase() to it.content }
        )
        _uiState.update { it.copy(estimatedTokens = base + TokenEstimator.estimate(it.input) + 20) }
    }

    private suspend fun runGeneration(messages: List<ChatMessage>, parentId: String) {
        reasoningBuf.clear()
        contentBuf.clear()
        lastEmit = 0L
        val key = container.settingsRepository.apiKey()
        _uiState.update {
            it.copy(isGenerating = true, streamingContent = "", streamingReasoning = "", error = null)
        }

        val streamUsage = AtomicReference<Usage?>(null)
        var failed = false
        var errorMsg: String? = null
        var finalContent = ""
        var finalReasoning = ""

        if (_uiState.value.useStreaming) {
            container.chatRepository.stream(messages, settings, key) { event ->
                when (event) {
                    is StreamEvent.Delta -> onDelta(event.reasoning, event.content)
                    is StreamEvent.Usage -> streamUsage.set(event.usage)
                    is StreamEvent.Done -> Unit
                    is StreamEvent.Error -> {
                        failed = true
                        errorMsg = event.error.message
                    }
                }
            }
            finalContent = contentBuf.toString()
            finalReasoning = reasoningBuf.toString()
        } else {
            try {
                val (content, usage) = container.chatRepository.complete(messages, settings, key)
                finalContent = content
                streamUsage.set(usage)
            } catch (e: Exception) {
                failed = true
                errorMsg = e.message ?: "生成失败"
            }
        }

        val conv = conversation ?: return
        val assistantId = UUID.randomUUID().toString()
        val status = if (failed) MessageStatus.INTERRUPTED else MessageStatus.COMPLETE

        if (finalContent.isNotBlank() || finalReasoning.isNotBlank() || !failed) {
            val msg = Message(
                id = assistantId,
                conversationId = conv.id,
                parentId = parentId,
                role = MessageRole.ASSISTANT,
                content = finalContent,
                reasoningContent = finalReasoning,
                thinkingEnabled = settings.thinkingEnabled,
                reasoningEffort = settings.reasoningEffort,
                modelId = settings.model,
                status = status,
            )
            container.conversationRepository.insertMessage(msg)
            conversation = conv.copy(activeLeafId = assistantId, updatedAt = System.currentTimeMillis())
            container.conversationRepository.save(conversation!!)
        }

        if (failed) {
            val count = _uiState.value.consecutiveFailures + 1
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    error = errorMsg,
                    consecutiveFailures = count,
                    useStreaming = count < 3,
                    streamingContent = "",
                    streamingReasoning = "",
                    info = if (count >= 3) "已连续失败 3 次，下次将自动使用非流式请求" else null,
                )
            }
            return
        }

        val usage = streamUsage.get() ?: Usage()
        val isPeak = TimeUtil.isPeak(settings.holidaysCsv)
        val table = PriceTable(
            settings.priceFlashHit, settings.priceFlashMiss, settings.priceFlashOut,
            settings.priceProHit, settings.priceProMiss, settings.priceProOut,
        )
        val cost = Pricing.calculateCostMicro(usage, settings.model, isPeak, table)
        container.usageRepository.insert(
            UsageRecord(
                id = UUID.randomUUID().toString(),
                messageId = assistantId,
                conversationId = conv.id,
                timestamp = System.currentTimeMillis(),
                modelId = settings.model,
                isPeak = isPeak,
                promptTokens = usage.promptTokens,
                cacheHitTokens = usage.cacheHitTokens,
                cacheMissTokens = usage.cacheMissTokens,
                completionTokens = usage.completionTokens,
                reasoningTokens = usage.reasoningTokens,
                estimatedCostMicro = cost,
            )
        )
        _uiState.update {
            it.copy(
                isGenerating = false,
                consecutiveFailures = 0,
                streamingContent = "",
                streamingReasoning = "",
                info = "本次用量：输入 ${usage.promptTokens}（命中 ${usage.cacheHitTokens}）/ 输出 ${usage.completionTokens}（思考 ${usage.reasoningTokens}），估算 ${Pricing.formatMicro(cost)}",
            )
        }
    }

    /** 每 ~80ms 批量刷新一次流式文本，避免逐 token 重组 */
    private fun onDelta(reasoning: String, content: String) {
        reasoningBuf.append(reasoning)
        contentBuf.append(content)
        val now = System.currentTimeMillis()
        if (now - lastEmit >= 80) {
            lastEmit = now
            _uiState.update {
                it.copy(
                    streamingContent = contentBuf.toString(),
                    streamingReasoning = reasoningBuf.toString(),
                )
            }
        }
    }
}

class ChatViewModelFactory(
    private val container: AppContainer,
    private val conversationId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(container, conversationId) as T
}
