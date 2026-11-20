package com.uusse.deepseekadventure.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationEntity)

    @Delete
    suspend fun delete(entity: ConversationEntity)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC")
    fun observeByConversation(cid: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE parentId = :pid")
    suspend fun children(pid: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MessageEntity)

    @Update
    suspend fun update(entity: MessageEntity)
}

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UsageRecordEntity)

    @Query("SELECT * FROM usage_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_records WHERE conversationId = :cid ORDER BY timestamp DESC")
    fun observeByConversation(cid: String): Flow<List<UsageRecordEntity>>
}

@Dao
interface WorldBookDao {
    @Query("SELECT * FROM world_book ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<WorldBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorldBookEntity)

    @Delete
    suspend fun delete(entity: WorldBookEntity)

    @Query("DELETE FROM world_book WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface BalanceDao {
    @Query("SELECT * FROM balance_snapshot WHERE singleton = 1")
    fun observe(): Flow<BalanceSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: BalanceSnapshotEntity)
}
