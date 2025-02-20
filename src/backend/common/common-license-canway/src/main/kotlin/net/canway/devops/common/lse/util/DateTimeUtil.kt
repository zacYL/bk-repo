/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.canway.devops.common.lse.util

import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

/**
 *
 * Powered By Tencent
 */
fun LocalDateTime.timestamp(): Long {
    val zoneId = ZoneId.systemDefault()
    return this.atZone(zoneId).toInstant().epochSecond
}

fun LocalDateTime.timestampmilli(): Long {
    val zoneId = ZoneId.systemDefault()
    return this.atZone(zoneId).toInstant().toEpochMilli()
}

object DateTimeUtil {

    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    private val logger = LoggerFactory.getLogger(DateTimeUtil::class.java)

    /**
     * 单位转换，分钟转换秒
     */
    fun minuteToSecond(minutes: Int): Int {
        return minutes * 60
    }

    /**
     * 单位转化，秒转换分钟
     * 以Int为计算单位，有余数将省去
     */
    fun secondToMinute(seconds: Int): Int {
        return seconds / 60
    }

    /**
     * 获取从当前开始一定单位时间间隔的日期
     * @param unit 单位 Calendar.SECONDS
     * @param timeSpan 实际间隔
     * @return 日期类实例
     */
    fun getFutureDateFromNow(unit: Int, timeSpan: Int): Date {
        val cd = Calendar.getInstance()
        cd.time = Date()
        cd.add(unit, timeSpan)
        return cd.time
    }

    /**
     * 按指定日期时间格式格式化日期时间
     * @param date 日期时间
     * @param format 格式化字符串
     * @return 字符串
     */
    fun formatDate(date: Date, format: String = "yyyy-MM-dd HH:mm:ss"): String {
        val simpleDateFormat = SimpleDateFormat(format)
        return simpleDateFormat.format(date)
    }

    fun convertLocalDateTimeToTimestamp(localDateTime: LocalDateTime?): Long {
        return localDateTime?.toEpochSecond(ZoneOffset.ofHours(8)) ?: 0L
    }

    fun toDateTime(dateTime: LocalDateTime?, format: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (dateTime == null) {
            return ""
        }
        val zone = ZoneId.systemDefault()
        val instant = dateTime.atZone(zone).toInstant()
        val simpleDateFormat = SimpleDateFormat(format)
        return simpleDateFormat.format(Date.from(instant))
    }

    /**
     * 单位转化，秒转换时间戳
     * 2019-09-02T08:58:46+0000 -> xxxxx
     */
    fun zoneDateToTimestamp(timeStr: String?): Long {
        try {
            if (timeStr.isNullOrBlank()) return 0L
            return formatter.parse(timeStr).time
        } catch (e: Exception) {
            logger.error("fail to parse time string: $timeStr", e)
        }
        return 0L
    }

    /**
     * 毫秒时间
     * Long类型时间转换成视频时长
     */
    fun formatTime(timeStr: String): String {
        val timeGap = 60
        val million = 1000
        val zero = 0L
        val ten = 10
        val time = timeStr.toLong() * million
        val hour = time / (timeGap * timeGap * million)
        val minute = (time - hour * timeGap * timeGap * million) / (timeGap * million)
        val second = (time - hour * timeGap * timeGap * million - minute * timeGap * million) / million
        return (if (hour == zero) "00" else if (hour >= ten) hour.toString() else "0$hour").toString() + "时" +
            (if (minute == zero) "00" else if (minute >= ten) minute else "0$minute") + "分" +
            (if (second == zero) "00" else if (second >= ten) second.toShort() else "0$second") + "秒"
    }

    fun formatMilliTime(time: Long): String {
        return formatMilliTime(time.toString())
    }

    private fun formatMilliTime(timeStr: String): String {
        val time = timeStr.toLong()
        val hour = time / (60 * 60 * 1000)
        val minute = (time - hour * 60 * 60 * 1000) / (60 * 1000)
        val second = (time - hour * 60 * 60 * 1000 - minute * 60 * 1000) / 1000
        return (if (hour == 0L) "00" else if (hour >= 10) hour.toString() else "0$hour").toString() + "时" +
            (if (minute == 0L) "00" else if (minute >= 10) minute else "0$minute") + "分" +
            (if (second == 0L) "00" else if (second >= 10) second.toShort() else "0$second") + "秒"
    }

    fun formatMillSecond(mss: Long): String {
        if (mss == 0L) return "0秒"

        val days = mss / (1000 * 60 * 60 * 24)
        val hours = mss % (1000 * 60 * 60 * 24) / (1000 * 60 * 60)
        val minutes = mss % (1000 * 60 * 60) / (1000 * 60)
        val seconds = mss % (1000 * 60) / 1000
        val sb = StringBuilder()
        if (days != 0L) {
            sb.append(days.toString() + "天")
        }
        if (hours != 0L) {
            sb.append(hours.toString() + "时")
        }
        if (minutes != 0L) {
            sb.append(minutes.toString() + "分")
        }
        if (seconds != 0L) {
            sb.append(seconds.toString() + "秒")
        }
        return sb.toString()
    }

    /**
     * 将格式化的日期时间字符串转换为LocalDateTime对象
     */
    fun stringToLocalDateTime(dateTimeStr: String, formatStr: String = "yyyy-MM-dd HH:mm:ss"): LocalDateTime {
        val formatter = DateTimeFormatter.ofPattern(formatStr)
        return LocalDateTime.parse(dateTimeStr, formatter)
    }
}
