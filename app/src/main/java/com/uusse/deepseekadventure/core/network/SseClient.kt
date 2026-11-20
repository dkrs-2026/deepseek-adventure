package com.uusse.deepseekadventure.core.network

import com.uusse.deepseekadventure.domain.model.BalanceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 直连 api.deepseek.com：SSE 流式聊天 + 余额查询 + 模型列表。
 * 所有请求走 HTTPS；HTTP 状态码先判定，错误不进入流解析。
 */
class SseClient(private val client: OkHttpClient) {

    /** 流式聊天，通过回调逐步吐出事件 */
    suspend fun stream(
        request: ChatRequest,
        apiKey: String,
        onEvent: (StreamEvent) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            onEvent(StreamEvent.Error(ApiError.EmptyKey))
            return@withContext
        }
        val body = apiJson.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(CHAT_URL)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        var call: Call? = null
        try {
            call = client.newCall(req)
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = try { response.body?.string()?.take(200) ?: "" } catch (_: Exception) { "" }
                    onEvent(StreamEvent.Error(mapHttpError(response.code, response.header("Retry-After"), errBody)))
                    return@withContext
                }
                val source = response.body?.source()
                if (source == null) {
                    onEvent(StreamEvent.Error(ApiError.Parse("响应体为空")))
                    return@withContext
                }
                var sawDone = false
                source.use { buffered ->
                    while (true) {
                        val line = buffered.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.substringAfter("data:").trim()
                        if (data.isEmpty()) continue
                        if (data == "[DONE]") {
                            sawDone = true
                            break
                        }
                        try {
                            val chunk = apiJson.decodeFromString<ChatChunk>(data)
                            val delta = chunk.choices.firstOrNull()?.delta
                            if (delta != null) {
                                val r = delta.reasoningContent.orEmpty()
                                val c = delta.content.orEmpty()
                                if (r.isNotEmpty() || c.isNotEmpty()) onEvent(StreamEvent.Delta(r, c))
                            }
                            chunk.usage?.let { onEvent(StreamEvent.Usage(it.toUsage())) }
                        } catch (e: Exception) {
                            onEvent(StreamEvent.Error(ApiError.Parse(e.message ?: "SSE 数据解析失败")))
                            return@withContext
                        }
                    }
                }
                if (sawDone) onEvent(StreamEvent.Done)
                else onEvent(StreamEvent.Error(ApiError.Stalled))
            }
        } catch (e: SocketTimeoutException) {
            onEvent(StreamEvent.Error(ApiError.Stalled))
        } catch (e: IOException) {
            onEvent(StreamEvent.Error(ApiError.Network(e)))
        } catch (e: Exception) {
            onEvent(StreamEvent.Error(ApiError.Network(e)))
        } finally {
            call?.cancel()
        }
    }

    /** 非流式聊天（连续失败后的降级路径） */
    suspend fun complete(request: ChatRequest, apiKey: String): Pair<String, com.uusse.deepseekadventure.domain.model.Usage> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) throw ApiError.EmptyKey
            val body = apiJson.encodeToString(ChatRequest.serializer(), request.copy(stream = false))
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder()
                .url(CHAT_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body)
                .build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = try { response.body?.string()?.take(200) ?: "" } catch (_: Exception) { "" }
                    throw mapHttpError(response.code, response.header("Retry-After"), errBody)
                }
                val chunk = apiJson.decodeFromString<ChatChunk>(response.body?.string() ?: "{}")
                val content = chunk.choices.firstOrNull()?.message?.content.orEmpty()
                val usage = chunk.usage?.toUsage()
                    ?: com.uusse.deepseekadventure.domain.model.Usage()
                content to usage
            }
        }

    suspend fun fetchBalance(apiKey: String): BalanceInfo = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw ApiError.EmptyKey
        val req = Request.Builder()
            .url(BALANCE_URL)
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = try { response.body?.string()?.take(200) ?: "" } catch (_: Exception) { "" }
                throw mapHttpError(response.code, response.header("Retry-After"), errBody)
            }
            val dto = apiJson.decodeFromString<BalanceDto>(response.body?.string() ?: "{}")
            val info = dto.balance_infos.firstOrNull()
            BalanceInfo(
                isAvailable = dto.is_available,
                currency = info?.currency ?: "CNY",
                totalBalance = info?.total_balance ?: "0",
                grantedBalance = info?.granted_balance ?: "0",
                toppedUpBalance = info?.topped_up_balance ?: "0",
            )
        }
    }

    suspend fun fetchModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw ApiError.EmptyKey
        val req = Request.Builder()
            .url(MODELS_URL)
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw mapHttpError(response.code, response.header("Retry-After"), "")
            val dto = apiJson.decodeFromString<ModelsDto>(response.body?.string() ?: "{}")
            dto.data.map { it.id }
        }
    }

    private fun mapHttpError(code: Int, retryAfter: String?, body: String): ApiError = when (code) {
        401, 403 -> ApiError.Unauthorized(code)
        402 -> ApiError.InsufficientBalance(code)
        429 -> ApiError.RateLimited(retryAfter)
        else -> ApiError.Http(code, body)
    }

    companion object {
        private const val CHAT_URL = "https://api.deepseek.com/chat/completions"
        private const val BALANCE_URL = "https://api.deepseek.com/user/balance"
        private const val MODELS_URL = "https://api.deepseek.com/models"
    }
}
