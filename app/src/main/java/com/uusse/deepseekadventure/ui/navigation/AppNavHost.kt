package com.uusse.deepseekadventure.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uusse.deepseekadventure.data.AppContainer
import com.uusse.deepseekadventure.ui.balance.BalanceScreen
import com.uusse.deepseekadventure.ui.chat.ChatScreen
import com.uusse.deepseekadventure.ui.chat.ChatViewModelFactory
import com.uusse.deepseekadventure.ui.conversations.ConversationListScreen
import com.uusse.deepseekadventure.ui.settings.SettingsScreen
import com.uusse.deepseekadventure.ui.worldbook.WorldBookScreen

private enum class Tab { CONVERSATIONS, CHAT, WORLD, BALANCE, SETTINGS }

@Composable
fun AppRoot(container: AppContainer) {
    var tab by remember { mutableStateOf(Tab.CONVERSATIONS) }
    var conversationId by remember { mutableStateOf<String?>(null) }
    val tabs = Tab.values()
    val labels = listOf("会话", "聊天", "设定集", "用量", "设置")

    Scaffold(
        topBar = {
            Column {
                Text("DeepSeek 冒险", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                TabRow(selectedTabIndex = tabs.indexOf(tab)) {
                    tabs.forEachIndexed { index, t ->
                        Tab(
                            selected = tab == t,
                            onClick = { tab = t },
                            text = { Text(labels[index]) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.CONVERSATIONS -> ConversationListScreen(
                    container = container,
                    onOpen = { id ->
                        conversationId = id
                        tab = Tab.CHAT
                    },
                )
                Tab.CHAT -> {
                    val id = conversationId
                    if (id != null) {
                        val factory = ChatViewModelFactory(container, id)
                        val vm: com.uusse.deepseekadventure.ui.chat.ChatViewModel = viewModel(factory = factory)
                        ChatScreen(vm = vm, onBack = { tab = Tab.CONVERSATIONS })
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("还没有打开会话")
                                Button(onClick = { tab = Tab.CONVERSATIONS }) {
                                    Text("去会话列表")
                                }
                            }
                        }
                    }
                }
                Tab.WORLD -> WorldBookScreen(container)
                Tab.BALANCE -> BalanceScreen(container)
                Tab.SETTINGS -> SettingsScreen(container)
            }
        }
    }
}
