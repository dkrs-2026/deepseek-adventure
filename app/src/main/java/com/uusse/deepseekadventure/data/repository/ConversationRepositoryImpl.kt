package com.uusse.deepseekadventure.data.repository

import com.uusse.deepseekadventure.data.local.ConversationDao
import com.uusse.deepseekadventure.data.local.ConversationEntity
import com.uusse.deepseekadventure.data.local.MessageDao
import com.uusse.deepseekadventure.data.local.MessageEntity
import com.uusse.deepseekadventure.domain.model.Conversation
import com.uusse.deepseekadventure.domain.model.Message
import com.uusse.deepseekadventure.domain.model.MessageRole
import com.uusse.deepseekadventure.domain.model.MessageStatus
import com.uusse.deepseekadventure.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) : ConversationRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun create(title: String, model: String, systemPrompt: String): Conversation {
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = now,
            updatedAt = now,
            rootMessageId = null,
            activeLeafId = null,
            modelId = model,
            systemPrompt = systemPrompt,
        )
        conversationDao.upsert(entity)
        return entity.toModel()
    }

    override suspend fun get(id: String): Conversation? = conversationDao.get(id)?.toModel()

    override suspend fun save(conversation: Conversation) {
        conversationDao.upsert(conversation.toEntity())
    }

    override suspend fun delete(id: String) {
        conversationDao.get(id)?.let { conversationDao.delete(it) }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeByConversation(conversationId).map { list -> list.map { it.toModel() } }

    override suspend fun getMessage(id: String): Message? = messageDao.get(id)?.toModel()

    override suspend fun insertMessage(message: Message) {
        messageDao.upsert(message.toEntity())
    }

    override suspend fun updateMessage(message: Message) {
        messageDao.update(message.toEntity())
    }

    override suspend fun pathTo(leafId: String): List<Message> {
        val result = mutableListOf<Message>()
        var current: String? = leafId
        val guard = mutableSetOf<String>()
        while (current != null && guard.add(current)) {
            val entity = messageDao.get(current) ?: break
            result.add(entity.toModel())
            current = entity.parentId
        }
        return result.asReversed()
    }

    private fun ConversationEntity.toModel() = Conversation(
        id, title, createdAt, updatedAt, rootMessageId, activeLeafId, modelId, systemPrompt
    )

    private fun Conversation.toEntity() = ConversationEntity(
        id, title, createdAt, updatedAt, rootMessageId, activeLeafId, modelId, systemPrompt
    )

    private fun MessageEntity.toModel() = Message(
        id = id,
        conversationId = conversationId,
        parentId = parentId,
        role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.USER),
        content = content,
        reasoningContent = reasoningContent,
        thinkingEnabled = thinkingEnabled,
        reasoningEffort = reasoningEffort,
        modelId = modelId,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.COMPLETE),
        branchLabel = branchLabel,
        createdAt = createdAt,
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        conversationId = conversationId,
        parentId = parentId,
        role = role.name,
        content = content,
        reasoningContent = reasoningContent,
        thinkingEnabled = thinkingEnabled,
        reasoningEffort = reasoningEffort,
        modelId = modelId,
        status = status.name,
        branchLabel = branchLabel,
        createdAt = createdAt,
    )
}
