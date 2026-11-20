package com.uusse.deepseekadventure.data.repository

import com.uusse.deepseekadventure.core.network.SseClient
import com.uusse.deepseekadventure.core.network.apiJson
import com.uusse.deepseekadventure.data.local.BalanceDao
import com.uusse.deepseekadventure.data.local.BalanceSnapshotEntity
import com.uusse.deepseekadventure.domain.model.BalanceInfo
import com.uusse.deepseekadventure.domain.repository.BalanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString

class BalanceRepositoryImpl(
    private val balanceDao: BalanceDao,
    private val sse: SseClient,
) : BalanceRepository {

    override suspend fun refresh(apiKey: String): BalanceInfo {
        val info = sse.fetchBalance(apiKey)
        balanceDao.save(BalanceSnapshotEntity(1, apiJson.encodeToString(BalanceInfo.serializer(), info)))
        return info
    }

    override fun observe(): Flow<BalanceInfo?> = balanceDao.observe().map { entity ->
        entity?.let { runCatching { apiJson.decodeFromString<BalanceInfo>(it.json) }.getOrNull() }
    }
}
