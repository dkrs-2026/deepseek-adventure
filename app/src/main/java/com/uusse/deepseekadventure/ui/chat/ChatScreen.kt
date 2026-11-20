package com.uusse.deepseekadventure.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uusse.deepseekadventure.domain.model.Message
import com.uusse.deepseekadventure.domain.model.MessageRole
import com.uusse.deepseekadventure.domain.model.MessageStatus

@Composable
fun ChatScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isGenerating) {
        val target = if (state.isGenerating) state.messages.size else state.messages.size - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("冒险 · ${state.model}${state.branchLabel?.let { " · $it" } ?: ""}")
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = { TextButton(onClick = { vm.generateSummary() }) { Text("摘要") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            state.error?.let { err ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("⚠️ $err", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.dismissError() }) { Text("关闭") }
                }
            }
            state.info?.let { info ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("ℹ️ $info", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.dismissInfo() }) { Text("关闭") }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                items(state.messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onToggleReasoning = { vm.toggleReasoning() },
                        onRetry = { vm.retry(it) },
                        onBranch = { vm.branchFrom(it) },
                        onEdit = { id, text -> vm.editMessage(id, text) },
                    )
                }
                if (state.isGenerating) {
                    item {
                        MessageBubble(
                            message = Message(
                                id = "__streaming__",
                                conversationId = "",
                                parentId = null,
                                role = MessageRole.ASSISTANT,
                                content = "",
                                status = MessageStatus.STREAMING,
                            ),
                            isStreaming = true,
                            streamingContent = state.streamingContent,
                            streamingReasoning = state.streamingReasoning,
                            reasoningExpanded = state.reasoningExpanded,
                            onToggleReasoning = { vm.toggleReasoning() },
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                FilterChip(
                    selected = state.thinkingEnabled,
                    onClick = { vm.setThinking(!state.thinkingEnabled) },
                    label = { Text("思考") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.useStreaming,
                    onClick = { vm.setUseStreaming(!state.useStreaming) },
                    label = { Text("流式") },
                )
                Spacer(Modifier.width(8.dp))
                Text("≈${state.estimatedTokens} tokens", style = MaterialTheme.typography.bodySmall)
            }

            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = { vm.onInputChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入剧情…") },
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { vm.send() },
                    enabled = !state.isGenerating && state.input.isNotBlank(),
                ) { Text("发送") }
            }
        }
    }
}
