package com.uusse.deepseekadventure.core.network

/** 统一错误类型，UI 按类型给出明确提示 */
sealed class ApiError(message: String) : RuntimeException(message) {
    data class Unauthorized(val code: Int) : ApiError("API Key 无效或已过期（HTTP $code）")
    data class InsufficientBalance(val code: Int) : ApiError("账户余额不足（HTTP $code）")
    data class RateLimited(val retryAfter: String?) : ApiError(
        "请求过于频繁（HTTP 429）" + (retryAfter?.let { "，建议 $it 秒后重试" } ?: "")
    )
    data class Http(val code: Int, val body: String) : ApiError("请求失败（HTTP $code）${body.take(120)}")
    data class Network(val cause: Throwable) : ApiError("网络异常：${cause.message ?: "未知错误"}")
    data object Stalled : ApiError("回复中断：长时间未收到数据，已保留已到达内容")
    data class Parse(val detail: String) : ApiError("数据解析失败：$detail")
    data object EmptyKey : ApiError("请先在设置页输入 API Key")
}

/** 流式事件 */
sealed interface StreamEvent {
    data class Delta(val reasoning: String, val content: String) : StreamEvent
    data class Usage(val usage: com.uusse.deepseekadventure.domain.model.Usage) : StreamEvent
    data object Done : StreamEvent
    data class Error(val error: ApiError) : StreamEvent
}
