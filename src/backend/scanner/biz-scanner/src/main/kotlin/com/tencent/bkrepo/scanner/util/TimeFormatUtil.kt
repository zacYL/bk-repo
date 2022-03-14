package com.tencent.bkrepo.scanner.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeFormatUtil {

    private val DATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    fun getUtcTime(): String {
        return Instant.now().toString()
    }

    fun formatLocalTime(localDateTime: LocalDateTime): String {
        return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    fun convertToLocalTime(utcTime: String): LocalDateTime {
        return convertToLocalTime(LocalDateTime.parse(utcTime, DateTimeFormatter.ISO_DATE_TIME))
    }

    private fun convertToLocalTime(utcTime: LocalDateTime): LocalDateTime {
        return utcTime.plusHours(8)
    }

    fun convertToUtcTime(localDateTime: LocalDateTime): String {
        return toUtc(localDateTime).format(DATA_TIME_FORMATTER)
    }

    fun toUtc(time: LocalDateTime): LocalDateTime {
        return toUtc(time, ZoneId.systemDefault())
    }

    private fun toUtc(time: LocalDateTime, fromZone: ZoneId): LocalDateTime {
        return toZone(time, fromZone, ZoneOffset.UTC)
    }

    private fun toZone(time: LocalDateTime, fromZone: ZoneId, toZone: ZoneId): LocalDateTime {
        val zonedTime = time.atZone(fromZone)
        val converted = zonedTime.withZoneSameInstant(toZone)
        return converted.toLocalDateTime()
    }
}
