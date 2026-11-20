package com.uusse.deepseekadventure.core.tokenizer

/**
 * 本地启发式 token 估算（DeepSeek 未公开 token 计数接口，故只能估算）。
 * 发送前显示；实际用量以响应 usage 为准，并可用真实值反向校准。
 */
object TokenEstimator {

    /** 估算文本 token 数，含 15% 缓冲 */
    fun estimate(text: String): Int {
        var units = 0.0
        for (ch in text) {
            val cp = ch.code
            units += when {
                cp in 0x4E00..0x9FFF -> 1.0          // CJK 统一表意文字
                cp in 0x3040..0x30FF -> 1.0          // 日文假名
                cp in 0xAC00..0xD7AF -> 1.0          // 韩文
                ch.isWhitespace() -> 0.0
                ch.isDigit() -> 0.5
                else -> 0.25                        // 英文约 4 字符/token
            }
        }
        return (units * 1.15).toInt().coerceAtLeast(1)
    }

    /** 估算一组消息的 token 总数（含少量结构开销） */
    fun estimateMessages(messages: List<Pair<String, String>>): Int =
        messages.sumOf { estimate(it.second) + 4 }
}
