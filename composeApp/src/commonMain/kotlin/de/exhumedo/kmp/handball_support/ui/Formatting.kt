package de.exhumedo.kmp.handball_support.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Formats a Unix timestamp as e.g. "Sat 7 Jun 2026, 18:30" in the device's time zone. */
@Suppress("DEPRECATION")
internal fun formatMatchDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    // The API returns seconds in some places and milliseconds in others. Year-2000 in ms is
    // ~1×10¹², so anything below that is treated as seconds.
    val epochMillis = if (timestamp < 1_000_000_000_000L) timestamp * 1000L else timestamp
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val weekday = WEEKDAYS[local.dayOfWeek.ordinal]
    val month = MONTHS[local.month.ordinal]
    val hh = local.hour.toString().padStart(2, '0')
    val mm = local.minute.toString().padStart(2, '0')
    return "$weekday ${local.dayOfMonth} $month ${local.year}, $hh:$mm"
}



