package com.tencent.bkrepo.repository.util

object FileSizeUtils {
    private const val UNIT = 1024
    private const val KB = UNIT.toLong()
    private const val MB = KB * UNIT
    private const val GB = MB * UNIT
    private const val TB = GB * UNIT
    private const val PB = TB * UNIT
    private const val EB = PB * UNIT
    private const val ZB = EB * UNIT
    private const val YB = ZB * UNIT
    private const val KB_UNIT = "KB"
    private const val MB_UNIT = "MB"
    private const val GB_UNIT = "GB"
    private const val TB_UNIT = "TB"
    private const val PB_UNIT = "PB"
    private const val EB_UNIT = "EB"
    private const val ZB_UNIT = "ZB"
    private const val YB_UNIT = "YB"

    fun formatFileSize(size: Long): String {
        return when {
            size < KB -> "$size B"
            size < MB -> String.format("%.2f $KB_UNIT", size.toDouble() / KB)
            size < GB -> String.format("%.2f $MB_UNIT", size.toDouble() / MB)
            size < TB -> String.format("%.2f $GB_UNIT", size.toDouble() / GB)
            size < PB -> String.format("%.2f $TB_UNIT", size.toDouble() / TB)
            size < EB -> String.format("%.2f $PB_UNIT", size.toDouble() / PB)
            size < ZB -> String.format("%.2f $EB_UNIT", size.toDouble() / EB)
            size < YB -> String.format("%.2f $ZB_UNIT", size.toDouble() / ZB)
            else -> String.format("%.2f $YB_UNIT", size.toDouble() / YB)
        }
    }
}
