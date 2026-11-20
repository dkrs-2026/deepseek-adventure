package com.uusse.deepseekadventure.ui.balance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uusse.deepseekadventure.core.util.Pricing
import com.uusse.deepseekadventure.data.AppContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BalanceScreen(container: AppContainer) {
    val balance by container.balanceRepository.observe().collectAsState(null)
    val usages by container.usageRepository.observeAll().collectAsState(emptyList())
    var error by remember { mutableStateOf<String?>(null) }
    var range by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    suspend fun refresh() {
        try {
            container.balanceRepository.refresh(container.settingsRepository.apiKey())
            error = null
        } catch (e: Exception) {
            error = e.message ?: "刷新失败"
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("余额与用量") },
                actions = { TextButton(onClick = { scope.launch { refresh() } }) { Text("刷新") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    balance?.let { b ->
                        Text("总余额：${b.currency} ${b.totalBalance}", style = MaterialTheme.typography.titleMedium)
                        Text("赠金余额：${b.grantedBalance}")
                        Text("充值余额：${b.toppedUpBalance}")
                        Text("状态：${if (b.isAvailable) "可用于 API 调用" else "不可用"}")
                        Text("查询时间：${fmt.format(Date(b.fetchedAt))}", style = MaterialTheme.typography.bodySmall)
                    } ?: Text("暂无余额数据，请点击右上角刷新")
                    error?.let { Text("⚠️ $it", color = MaterialTheme.colorScheme.error) }
                }
            }

            Text("统计范围", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row {
                listOf("今日", "7 天", "30 天", "全部").forEachIndexed { index, label ->
                    FilterChip(
                        selected = range == index,
                        onClick = { range = index },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            val cutoff = when (range) {
                0 -> Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                1 -> System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                2 -> System.currentTimeMillis() - 30L * 24 * 3600 * 1000
                else -> 0L
            }
            val list = usages.filter { it.timestamp >= cutoff }
            val totalCost = list.sumOf { it.estimatedCostMicro }
            val hit = list.sumOf { it.cacheHitTokens }
            val miss = list.sumOf { it.cacheMissTokens }
            val out = list.sumOf { it.completionTokens }
            val reasoning = list.sumOf { it.reasoningTokens }

            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("本地估算花费：${Pricing.formatMicro(totalCost)}", style = MaterialTheme.typography.titleMedium)
                    Text("缓存命中输入：$hit tokens")
                    Text("缓存未命中输入：$miss tokens")
                    Text("输出：$out tokens（其中思考 $reasoning tokens，按输出价计）")
                    Text("共 ${list.size} 条记录 · 数据来自响应 usage 字段，非官方账单", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("最近记录", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                items(list.take(50), key = { it.id }) { r ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${fmt.format(Date(r.timestamp))} · ${r.modelId} · 峰谷:${if (r.isPeak) "高峰" else "空闲"}")
                        Text(
                            "输入 ${r.promptTokens}(命中${r.cacheHitTokens}) / 输出 ${r.completionTokens}(思考${r.reasoningTokens}) · ${Pricing.formatMicro(r.estimatedCostMicro)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
