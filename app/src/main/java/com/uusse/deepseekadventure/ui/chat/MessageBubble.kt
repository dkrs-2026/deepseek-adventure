package com.uusse.deepseekadventure.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uusse.deepseekadventure.domain.model.Message
import com.uusse.deepseekadventure.domain.model.MessageRole
import com.uusse.deepseekadventure.domain.model.MessageStatus
import com.uusse.deepseekadventure.ui.common.MarkdownText

@Composable
fun MessageBubble(
    message: Message,
    isStreaming: Boolean = false,
    streamingContent: String = "",
    streamingReasoning: String = "",
    reasoningExpanded: Boolean = false,
    onToggleReasoning: () -> Unit = {},
    onRetry: (String) -> Unit = {},
    onBranch: (String) -> Unit = {},
    onEdit: (String, String) -> Unit = { _, _ -> },
) {
    val isUser = message.role == MessageRole.USER
    val reasoning = if (isStreaming) streamingReasoning else message.reasoningContent
    val content = if (isStreaming) streamingContent else message.content

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        // 思考内容：默认折叠，可展开
        if (reasoning.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { onToggleReasoning() },
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        "💭 思考过程（${reasoning.length} 字）${if (reasoningExpanded) " ▴" else " ▾"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (reasoningExpanded) {
                        Spacer(Modifier.height(4.dp))
                        Text(reasoning, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                MarkdownText(content)
                if (message.status == MessageStatus.INTERRUPTED) {
                    Spacer(Modifier.height(4.dp))
                    Text("⚠️ 回复中断，已保留到达内容，可点“重试”", color = MaterialTheme.colorScheme.error)
                }
                if (message.branchLabel != null) {
                    Text("分支：${message.branchLabel}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (!isStreaming && !isUser) {
            Row(horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = { onRetry(message.id) }) { Text("重试") }
                TextButton(onClick = { onBranch(message.id) }) { Text("从此分支") }
            }
        }

        if (!isStreaming && isUser) {
            var editing by remember { mutableStateOf(false) }
            var draft by remember { mutableStateOf(message.content) }
            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                )
                Row {
                    Button(onClick = { onEdit(message.id, draft); editing = false }) { Text("重新生成") }
                    TextButton(onClick = { editing = false }) { Text("取消") }
                }
            } else {
                TextButton(onClick = { editing = true }) { Text("编辑并从此处重生成") }
            }
        }
    }
}
