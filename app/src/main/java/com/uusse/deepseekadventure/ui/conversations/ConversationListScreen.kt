package com.uusse.deepseekadventure.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uusse.deepseekadventure.data.AppContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ConversationListScreen(container: AppContainer, onOpen: (String) -> Unit) {
    val conversations by container.conversationRepository.observeConversations().collectAsState(emptyList())
    var title by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(topBar = { TopAppBar(title = { Text("会话列表") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("新会话标题") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        scope.launch {
                            val conv = container.conversationRepository.create(
                                title.ifBlank { "新冒险" }, "deepseek-flash", ""
                            )
                            title = ""
                            onOpen(conv.id)
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("新建") }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(conversations, key = { it.id }) { conv ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(conv.id) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(conv.title)
                            Text(
                                "${conv.modelId} · ${fmt.format(conv.updatedAt)}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { scope.launch { container.conversationRepository.delete(conv.id) } }) {
                            Text("删除")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
