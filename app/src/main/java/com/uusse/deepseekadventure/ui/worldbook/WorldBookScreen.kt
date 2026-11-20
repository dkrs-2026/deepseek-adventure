package com.uusse.deepseekadventure.ui.worldbook

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import com.uusse.deepseekadventure.domain.model.WorldBookEntry
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun WorldBookScreen(container: AppContainer) {
    val entries by container.worldBookRepository.observeAll().collectAsState(emptyList())
    var title by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var editingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("世界书 / 设定集") }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        ) {
            Text("设定条目会按开关注入到每次请求的上下文中", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("条目名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(
                value = keywords, onValueChange = { keywords = it },
                label = { Text("关键词（仅备注，当前按开关注入）") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                label = { Text("设定内容") }, modifier = Modifier.fillMaxWidth(), maxLines = 8,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("启用")
            }
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        scope.launch {
                            val entry = WorldBookEntry(
                                id = editingId ?: UUID.randomUUID().toString(),
                                title = title,
                                content = content,
                                keywords = keywords,
                                enabled = enabled,
                                createdAt = System.currentTimeMillis(),
                            )
                            container.worldBookRepository.insert(entry)
                            editingId = null
                            title = ""; keywords = ""; content = ""; enabled = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (editingId == null) "添加条目" else "保存修改") }

            entries.forEach { e ->
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(e.title, style = MaterialTheme.typography.titleMedium)
                        Text(e.content.take(160), style = MaterialTheme.typography.bodySmall)
                        Text("启用：${if (e.enabled) "是" else "否"}", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = {
                                editingId = e.id; title = e.title; keywords = e.keywords
                                content = e.content; enabled = e.enabled
                            }) { Text("编辑") }
                            TextButton(onClick = { scope.launch { container.worldBookRepository.delete(e.id) } }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
}
