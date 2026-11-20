package com.uusse.deepseekadventure.core.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object TimeUtil {
    private val BEIJING = ZoneId.of("Asia/Shanghai")

    /**
     * 高峰时段：北京时间周一至周五 9:00-12:00、14:00-18:00。
     * 周末与中国法定节假日全天为空闲。节假日由设置页维护（yyyy-MM-dd 逗号分隔）。
     */
    fun isPeak(holidaysCsv: String = ""): Boolean {
        val now = ZonedDateTime.now(BEIJING)
        val date: LocalDate = now.toLocalDate()
        val holidays = holidaysCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (date.toString() in holidays) return false
        if (now.dayOfWeek.value >= 6) return false
        val h = now.hour
        return (h in 9..11) || (h in 14..17)
    }
}
