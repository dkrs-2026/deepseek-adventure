package com.uusse.deepseekadventure.core.util

import com.uusse.deepseekadventure.domain.model.Usage

/** 价格表（CNY / 百万 tokens），全部可在设置中修改 */
data class PriceTable(
    val flashHit: Double = 0.02,
    val flashMiss: Double = 1.0,
    val flashOut: Double = 4.0,
    val proHit: Double = 0.15,
    val proMiss: Double = 4.5,
    val proOut: Double = 13.5,
)

object Pricing {
    /** 返回 (命中输入价, 未命中输入价, 输出价) */
    fun pricesFor(model: String, table: PriceTable): Triple<Double, Double, Double> =
        if (model.contains("pro", ignoreCase = true)) {
            Triple(table.proHit, table.proMiss, table.proOut)
        } else {
            Triple(table.flashHit, table.flashMiss, table.flashOut)
        }

    /** 空闲时段单价（高峰为其 2 倍） */
    fun calculateCostMicro(
        usage: Usage,
        model: String,
        isPeak: Boolean,
        table: PriceTable,
    ): Long {
        val (hit, miss, out) = pricesFor(model, table)
        val factor = if (isPeak) 2.0 else 1.0
        // completion 已包含 reasoning token，按输出价计一次，不重复计费
        val yuan = (usage.cacheHitTokens / 1_000_000.0 * hit +
                usage.cacheMissTokens / 1_000_000.0 * miss +
                usage.completionTokens / 1_000_000.0 * out) * factor
        return (yuan * 1_000_000).toLong()
    }

    fun formatMicro(micro: Long): String {
        val yuan = micro / 1_000_000.0
        return "¥%.4f".format(yuan)
    }
}
