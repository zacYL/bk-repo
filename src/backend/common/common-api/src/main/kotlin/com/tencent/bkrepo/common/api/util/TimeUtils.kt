package com.tencent.bkrepo.common.api.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz")

    /**
     * UTC转换为系统时区当前时间
     */
    fun toSystemZoneTime(time: String): LocalDateTime {
        val utcTime = LocalDateTime.parse(time, formatter)
        return toSystemZoneTime(utcTime)
    }

    /**
     * UTC转换为系统时区当前时间
     */
    fun toSystemZoneTime(time: LocalDateTime): LocalDateTime {
        val instant = time.atZone(ZoneId.of("UTC")).toInstant()
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    }
}
