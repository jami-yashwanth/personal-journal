package com.personal.journal.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val exifFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

/** Calendar day ("YYYY-MM-DD") of [epochMillis] in [zone]; stored once at save so travel never moves entries. */
fun localDateOf(epochMillis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()

/** Parses EXIF DateTimeOriginal ("yyyy:MM:dd HH:mm:ss"), interpreted in [zone]. */
fun exifDateToMillis(exifValue: String?, zone: ZoneId): Long? {
    if (exifValue.isNullOrBlank() || exifValue.startsWith("0000")) return null
    return try {
        LocalDateTime.parse(exifValue.trim(), exifFormat).atZone(zone).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
