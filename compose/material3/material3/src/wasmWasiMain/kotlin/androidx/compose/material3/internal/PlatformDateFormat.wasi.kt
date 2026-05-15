/*
 * wasmWasi actual for material3 PlatformDateFormat. Uses kotlinx.datetime
 * for date math; defers locale-aware date *formatting* to a simple pattern
 * substitution that handles the patterns Material3 actually uses
 * (yyyy/MM/dd/E/MMM/MMMM/HH/mm/dd, etc.). Full ICU-style formatting requires
 * `icu4x_capi` bound to Kotlin, which we don't ship yet.
 *
 * - weekdayNames: stub (English week-day labels — enough for DatePicker
 *   layout; could be wired through wasi:android-locale later).
 * - is24HourFormat: routes through wasi:android-locale's getter.
 * - parse: only handles the input patterns shown in `getDateInputFormat`.
 */
@file:OptIn(ExperimentalTime::class)

package androidx.compose.material3.internal

import androidx.compose.material3.CalendarLocale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.skiko.wasi.wit.Locale as WitLocale

internal actual class PlatformDateFormat actual constructor(private val locale: CalendarLocale) {

    actual val firstDayOfWeek: Int = 1 // Monday — Material's day-of-week convention is 1..7

    actual val weekdayNames: List<Pair<String, String>> = listOf(
        "Monday"    to "Mon",
        "Tuesday"   to "Tue",
        "Wednesday" to "Wed",
        "Thursday"  to "Thu",
        "Friday"    to "Fri",
        "Saturday"  to "Sat",
        "Sunday"    to "Sun",
    )

    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        cache: MutableMap<String, Any>,
    ): String {
        val dt = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC)
        return substitute(dt, pattern)
    }

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        cache: MutableMap<String, Any>,
    ): String {
        val dt = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC)
        // Material3 passes skeletons like "yMMMd", "yMMMMd", "MMMd", "MMMM y", "yMd"
        // — convert each to a sensible pattern.
        val pattern = when (skeleton) {
            "yMMMd"   -> "MMM d, y"
            "yMMMMd"  -> "MMMM d, y"
            "MMMMd"   -> "MMMM d"
            "MMMd"    -> "MMM d"
            "yMMMM"   -> "MMMM y"
            "yMMM"    -> "MMM y"
            "MMMM y"  -> "MMMM y"
            "yMd"     -> "M/d/y"
            else      -> skeleton
        }
        return substitute(dt, pattern)
    }

    actual fun parse(
        date: String,
        pattern: String,
        locale: CalendarLocale,
        cache: MutableMap<String, Any>,
    ): CalendarDate? {
        // Material3's DateInputFormat is always 3 numeric fields y/M/d in some
        // order separated by a delimiter. Parse generically.
        val delim = pattern.firstOrNull { !it.isLetter() } ?: return null
        val patternParts = pattern.split(delim)
        val dateParts = date.split(delim)
        if (patternParts.size != 3 || dateParts.size != 3) return null
        var y = 0; var m = 0; var d = 0
        for (i in 0..2) {
            val n = dateParts[i].toIntOrNull() ?: return null
            when (patternParts[i].lowercase().firstOrNull()) {
                'y' -> y = n
                'm' -> m = n
                'd' -> d = n
                else -> return null
            }
        }
        return try {
            val ldt = LocalDateTime(y, m, d, 0, 0)
            val utc = ldt.toInstant(TimeZone.UTC).toEpochMilliseconds()
            CalendarDate(year = y, month = m, dayOfMonth = d, utcTimeMillis = utc)
        } catch (_: Throwable) {
            null
        }
    }

    actual fun getDateInputFormat(): DateInputFormat =
        DateInputFormat(patternWithDelimiters = "MM/dd/yyyy", delimiter = '/')

    actual fun is24HourFormat(): Boolean =
        try {
            WitLocale.Import.is24HourFormat()
        } catch (_: Throwable) {
            true
        }

    private fun substitute(dt: LocalDateTime, pattern: String): String {
        // Token-by-token: y/yy/yyyy, M/MM/MMM/MMMM, d/dd, H/HH, m/mm, E/EE/EEEE.
        val sb = StringBuilder(pattern.length + 8)
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            var run = 1
            while (i + run < pattern.length && pattern[i + run] == c) run++
            when (c) {
                'y' -> sb.append(dt.year.toString().padStart(if (run >= 4) 4 else run, '0'))
                'M' -> sb.append(formatMonth(dt.monthNumber, run))
                'd' -> sb.append(dt.day.toString().padStart(run, '0'))
                'H' -> sb.append(dt.hour.toString().padStart(run, '0'))
                'h' -> sb.append(((dt.hour % 12).let { if (it == 0) 12 else it }).toString().padStart(run, '0'))
                'm' -> sb.append(dt.minute.toString().padStart(run, '0'))
                's' -> sb.append(dt.second.toString().padStart(run, '0'))
                'E' -> sb.append(formatDayOfWeek(dt.dayOfWeek, run))
                'a' -> sb.append(if (dt.hour < 12) "AM" else "PM")
                'D' -> sb.append(dt.dayOfYear.toString().padStart(run, '0'))
                else -> {
                    sb.append(c)
                    run = 1
                }
            }
            i += run
        }
        return sb.toString()
    }

    private fun formatMonth(monthNumber: Int, run: Int): String {
        val name = monthName(monthNumber)
        return when (run) {
            1, 2 -> monthNumber.toString().padStart(run, '0')
            3    -> name.take(3)
            else -> name
        }
    }

    private fun formatDayOfWeek(dow: DayOfWeek, run: Int): String {
        val name = when (dow) {
            DayOfWeek.MONDAY    -> "Monday"
            DayOfWeek.TUESDAY   -> "Tuesday"
            DayOfWeek.WEDNESDAY -> "Wednesday"
            DayOfWeek.THURSDAY  -> "Thursday"
            DayOfWeek.FRIDAY    -> "Friday"
            DayOfWeek.SATURDAY  -> "Saturday"
            DayOfWeek.SUNDAY    -> "Sunday"
            else                -> "?"
        }
        return when (run) {
            1, 2, 3 -> name.take(3)
            else    -> name
        }
    }

    private fun monthName(m: Int): String = when (m) {
        1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
        5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
        9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
        else -> "?"
    }
}
