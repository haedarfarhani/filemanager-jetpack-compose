package ir.heydar.nexfolder.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun format(timeMillis: Long): String {
        if (timeMillis <= 0) return ""
        val now = System.currentTimeMillis()
        val diff = now - timeMillis

        val nowCal = Calendar.getInstance()
        val fileCal = Calendar.getInstance().apply { timeInMillis = timeMillis }

        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> {
                val minutes = (diff / 60_000).toInt()
                "$minutes min ago"
            }
            isSameDay(nowCal, fileCal) -> "Today ${timeFormat.format(Date(timeMillis))}"
            isYesterday(nowCal, fileCal) -> "Yesterday"
            diff < 7 * 24 * 3_600_000 -> {
                val days = (diff / (24 * 3_600_000)).toInt()
                "$days days ago"
            }
            isSameYear(nowCal, fileCal) -> dateFormat.format(Date(timeMillis))
            else -> fullDateFormat.format(Date(timeMillis))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(now: Calendar, other: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, other)
    }

    private fun isSameYear(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
}
