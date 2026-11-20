package com.uusse.deepseekadventure.data

import android.content.Context
import androidx.room.Room
import com.uusse.deepseekadventure.core.network.NetworkMonitor
import com.uusse.deepseekadventure.core.network.SseClient
import com.uusse.deepseekadventure.core.security.SecurePrefs
import com.uusse.deepseekadventure.data.local.AppDatabase
import com.uusse.deepseekadventure.data.preferences.SettingsDataStore
import com.uusse.deepseekadventure.data.repository.BalanceRepositoryImpl
import com.uusse.deepseekadventure.data.repository.ChatRepositoryImpl
import com.uusse.deepseekadventure.data.repository.ConversationRepositoryImpl
import com.uusse.deepseekadventure.data.repository.SettingsRepositoryImpl
import com.uusse.deepseekadventure.data.repository.UsageRepositoryImpl
import com.uusse.deepseekadventure.data.repository.WorldBookRepositoryImpl
import com.uusse.deepseekadventure.domain.repository.BalanceRepository
import com.uusse.deepseekadventure.domain.repository.ChatRepository
import com.uusse.deepseekadventure.domain.repository.ConversationRepository
import com.uusse.deepseekadventure.domain.repository.SettingsRepository
import com.uusse.deepseekadventure.domain.repository.UsageRepository
import com.uusse.deepseekadventure.domain.repository.WorldBookRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/** 手动依赖注入容器：避免 Hilt/KSP 版本冲突，降低云端构建失败风险 */
class AppContainer(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "deepseek_adventure.db",
    ).build()

    private val securePrefs = SecurePrefs(context.applicationContext)
    private val settingsDataStore = SettingsDataStore(context.applicationContext)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)   // SSE 停滞看门狗
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)      // POST 不自动重试，避免重复计费
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        })
        .build()

    private val sseClient = SseClient(okHttpClient)

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsDataStore, securePrefs)
    val conversationRepository: ConversationRepository =
        ConversationRepositoryImpl(db.conversationDao(), db.messageDao())
    val chatRepository: ChatRepository = ChatRepositoryImpl(sseClient)
    val usageRepository: UsageRepository = UsageRepositoryImpl(db.usageDao())
    val balanceRepository: BalanceRepository = BalanceRepositoryImpl(db.balanceDao(), sseClient)
    val worldBookRepository: WorldBookRepository = WorldBookRepositoryImpl(db.worldBookDao())
    val networkMonitor = NetworkMonitor(context.applicationContext)
}
