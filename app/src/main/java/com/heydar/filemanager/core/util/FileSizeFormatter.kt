package com.heydar.filemanager.core.util

import kotlin.math.log10
import kotlin.math.pow

object FileSizeFormatter {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            "%.1f %s".format(value, units[digitGroups])
        }
    }

    fun formatDetailed(usedBytes: Long, totalBytes: Long): String {
        return "${format(usedBytes)} of ${format(totalBytes)}"
    }
}
