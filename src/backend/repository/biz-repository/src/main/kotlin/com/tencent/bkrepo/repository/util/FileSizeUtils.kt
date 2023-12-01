package com.tencent.bkrepo.repository.util

import java.util.*

object FileSizeUtils {
    private const val UNIT = 1024
    private const val KB = UNIT.toLong()
    private const val MB = KB * UNIT
    private const val GB = MB * UNIT
    private const val TB = GB * UNIT
    private const val KB_UNIT = "KB"
    private const val MB_UNIT = "MB"
    private const val GB_UNIT = "GB"
    private const val TB_UNIT = "TB"

    fun formatFileSize(size: Long): String {
        return when {
            size < KB -> "$size B"
            size < MB -> String.format(Locale.US, "%.2f $KB_UNIT", size.toDouble() / KB)
            size < GB -> String.format(Locale.US, "%.2f $MB_UNIT", size.toDouble() / MB)
            size < TB -> String.format(Locale.US, "%.2f $GB_UNIT", size.toDouble() / GB)
            else -> String.format(Locale.US, "%.2f $TB_UNIT", size.toDouble() / TB)
        }
    }
}
