package com.uusse.deepseekadventure.data.repository

import com.uusse.deepseekadventure.data.local.UsageDao
import com.uusse.deepseekadventure.data.local.UsageRecordEntity
import com.uusse.deepseekadventure.domain.model.UsageRecord
import com.uusse.deepseekadventure.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UsageRepositoryImpl(private val dao: UsageDao) : UsageRepository {
    override suspend fun insert(record: UsageRecord) = dao.upsert(record.toEntity())

    override fun observeAll(): Flow<List<UsageRecord>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    private fun UsageRecordEntity.toModel() = UsageRecord(
        id, messageId, conversationId, timestamp, modelId, isPeak,
        promptTokens, cacheHitTokens, cacheMissTokens, completionTokens,
        reasoningTokens, estimatedCostMicro,
    )

    private fun UsageRecord.toEntity() = UsageRecordEntity(
        id, messageId, conversationId, timestamp, modelId, isPeak,
        promptTokens, cacheHitTokens, cacheMissTokens, completionTokens,
        reasoningTokens, estimatedCostMicro,
    )
}
