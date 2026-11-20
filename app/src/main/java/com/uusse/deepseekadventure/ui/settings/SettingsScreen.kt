package com.uusse.deepseekadventure.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.uusse.deepseekadventure.data.AppContainer
import com.uusse.deepseekadventure.domain.repository.AppSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer) {
    val settings by container.settingsRepository.settings.collectAsState(AppSettings())
    var keyInput by remember { mutableStateOf("") }
    var masked by remember { mutableStateOf(container.settingsRepository.maskedApiKey()) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        ) {
            Text("API Key（加密存储，仅显示尾 4 位）", style = MaterialTheme.typography.titleMedium)
            Text("当前：${masked}")
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("粘贴 sk- 开头的 Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (keyInput.isNotBlank()) {
                        scope.launch {
                            container.settingsRepository.saveApiKey(keyInput)
                            keyInput = ""
                            masked = container.settingsRepository.maskedApiKey()
                            status = "已保存"
                        }
                    }
                },
            ) { Text("保存 Key") }
            Text(status, style = MaterialTheme.typography.bodySmall)

            SectionTitle("模型（官方当前模型）")
            Row {
                listOf("deepseek-flash", "deepseek-v4-pro").forEach { m ->
                    FilterChip(
                        selected = settings.model == m,
                        onClick = { scope.launch { container.settingsRepository.update { it.copy(model = m) } } },
                        label = { Text(m) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            SectionTitle("思考模式（本轮默认）")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = settings.thinkingEnabled,
                    onCheckedChange = { v -> scope.launch { container.settingsRepository.update { it.copy(thinkingEnabled = v) } } },
                )
                Text("启用思考（token 按输出价计费，更慢）", modifier = Modifier.padding(start = 8.dp))
            }
            Row {
                listOf("low", "high", "max").forEach { effort ->
                    FilterChip(
                        selected = settings.reasoningEffort == effort,
                        onClick = { scope.launch { container.settingsRepository.update { it.copy(reasoningEffort = effort) } } },
                        label = { Text(effort) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            SectionTitle("外观")
            Row {
                listOf("system", "light", "dark").forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { scope.launch { container.settingsRepository.update { it.copy(themeMode = mode) } } },
                        label = { Text(mode) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            SectionTitle("上下文预算（tokens，达到后提示）")
            OutlinedTextField(
                value = settings.contextBudget.toString(),
                onValueChange = { v ->
                    v.toIntOrNull()?.let { n -> scope.launch { container.settingsRepository.update { it.copy(contextBudget = n) } } }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            SectionTitle("系统提示词 / 冒险设定")
            OutlinedTextField(
                value = settings.systemPrompt,
                onValueChange = { v -> scope.launch { container.settingsRepository.update { it.copy(systemPrompt = v) } } },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
            )

            SectionTitle("价格表（CNY / 百万 tokens，空闲价；高峰为 2 倍）")
            PriceField("flash 命中", settings.priceFlashHit) { v -> scope.launch { container.settingsRepository.update { it.copy(priceFlashHit = v) } } }
            PriceField("flash 未命中", settings.priceFlashMiss) { v -> scope.launch { container.settingsRepository.update { it.copy(priceFlashMiss = v) } } }
            PriceField("flash 输出", settings.priceFlashOut) { v -> scope.launch { container.settingsRepository.update { it.copy(priceFlashOut = v) } } }
            PriceField("pro 命中", settings.priceProHit) { v -> scope.launch { container.settingsRepository.update { it.copy(priceProHit = v) } } }
            PriceField("pro 未命中", settings.priceProMiss) { v -> scope.launch { container.settingsRepository.update { it.copy(priceProMiss = v) } } }
            PriceField("pro 输出", settings.priceProOut) { v -> scope.launch { container.settingsRepository.update { it.copy(priceProOut = v) } } }

            SectionTitle("中国法定节假日（yyyy-MM-dd，逗号分隔；空闲时段）")
            OutlinedTextField(
                value = settings.holidaysCsv,
                onValueChange = { v -> scope.launch { container.settingsRepository.update { it.copy(holidaysCsv = v) } } },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun PriceField(label: String, value: Double, onValue: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let(onValue)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
