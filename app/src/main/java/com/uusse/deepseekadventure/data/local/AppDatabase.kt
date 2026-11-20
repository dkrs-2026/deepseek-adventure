package com.uusse.deepseekadventure.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        UsageRecordEntity::class,
        WorldBookEntity::class,
        BalanceSnapshotEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun usageDao(): UsageDao
    abstract fun worldBookDao(): WorldBookDao
    abstract fun balanceDao(): BalanceDao
}
