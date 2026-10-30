package com.personal.journal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.personal.journal.AppContainer
import com.personal.journal.JournalApp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as JournalApp).container

private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val shortDay = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val longDay = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

/** "Today, 08:42" or "Thu 23 Oct, 19:05". */
fun whenLabel(millis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
    val dt = Instant.ofEpochMilli(millis).atZone(zone)
    val day = if (dt.toLocalDate() == today) "Today" else shortDay.format(dt)
    return "$day, ${time.format(dt)}"
}

/** "Sunday 25 October, 08:42". */
fun longWhenLabel(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val dt = Instant.ofEpochMilli(millis).atZone(zone)
    return "${longDay.format(dt)}, ${time.format(dt)}"
}

/** "Monday, 26", the title of a day's page. */
fun dayTitle(localDate: String): String {
    val date = LocalDate.parse(localDate)
    return "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, ${date.dayOfMonth}"
}

fun timeLabel(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    time.format(Instant.ofEpochMilli(millis).atZone(zone))

fun plural(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"
