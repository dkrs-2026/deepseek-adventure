package com.uusse.deepseekadventure.data.repository

import com.uusse.deepseekadventure.data.local.WorldBookDao
import com.uusse.deepseekadventure.data.local.WorldBookEntity
import com.uusse.deepseekadventure.domain.model.WorldBookEntry
import com.uusse.deepseekadventure.domain.repository.WorldBookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorldBookRepositoryImpl(private val dao: WorldBookDao) : WorldBookRepository {
    override fun observeAll(): Flow<List<WorldBookEntry>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun insert(entry: WorldBookEntry) = dao.upsert(entry.toEntity())

    override suspend fun update(entry: WorldBookEntry) = dao.upsert(entry.toEntity())

    override suspend fun delete(id: String) = dao.deleteById(id)

    private fun WorldBookEntity.toModel() = WorldBookEntry(
        id, title, content, keywords, enabled, createdAt
    )

    private fun WorldBookEntry.toEntity() = WorldBookEntity(
        id, title, content, keywords, enabled, createdAt
    )
}
