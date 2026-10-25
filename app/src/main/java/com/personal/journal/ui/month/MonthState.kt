package com.personal.journal.ui.month

import com.personal.journal.bento.DaySummary
import com.personal.journal.bento.PlacedTile
import com.personal.journal.bento.pack
import com.personal.journal.data.db.EntryType
import com.personal.journal.data.db.EntryWithTags
import java.time.YearMonth

data class TileUi(
    val placed: PlacedTile,
    val thumbs: List<String>,
    val firstText: String?,
    val dimmed: Boolean,
    val isToday: Boolean,
    val label: String,
)

data class MonthUiState(
    val month: YearMonth,
    val title: String,
    val subtitle: String,
    val filterTags: List<String>,
    val activeTag: String?,
    val tiles: List<TileUi>,
    val isEmpty: Boolean,
    val isCurrentMonth: Boolean,
)

private fun count(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

/** Pure: groups a month's entries into bento tiles. Filtering only dims, so the layout never jumps. */
fun buildMonthUi(
    month: YearMonth,
    entries: List<EntryWithTags>,
    today: String,
    activeTag: String?,
    filterTags: List<String>,
): MonthUiState {
    val byDay = entries.groupBy { it.entry.localDate }.toSortedMap(compareByDescending { it })
    val summaries = byDay.map { (date, list) ->
        val photos = list.count { it.entry.type == EntryType.PHOTO }
        val texts = list.filter { it.entry.type == EntryType.TEXT }
        DaySummary(date, photos, texts.size, texts.firstOrNull()?.entry?.text?.length ?: 0, date == today)
    }
    val placed = pack(summaries).associateBy { it.localDate }
    val tiles = byDay.map { (date, list) ->
        val day = date.substring(8).trimStart('0')
        TileUi(
            placed = placed.getValue(date),
            thumbs = list.mapNotNull { it.entry.thumbFile }.take(3),
            firstText = list.firstOrNull { it.entry.type == EntryType.TEXT }?.entry?.text,
            dimmed = activeTag != null && list.none { e -> e.tags.any { it.name == activeTag } },
            isToday = date == today,
            label = if (date == today) "Today, $day" else day,
        )
    }
    val tagDays = if (activeTag == null) 0 else tiles.count { !it.dimmed }
    val subtitle = when {
        entries.isEmpty() -> "Nothing logged yet"
        activeTag != null -> "$activeTag on ${count(tagDays, "day", "days")} this month"
        else -> "${count(byDay.size, "day", "days")}, ${count(entries.size, "entry", "entries")}"
    }
    return MonthUiState(
        month = month,
        title = month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH),
        subtitle = subtitle,
        filterTags = filterTags,
        activeTag = activeTag,
        tiles = tiles,
        isEmpty = entries.isEmpty(),
        isCurrentMonth = today.startsWith(month.toString()),
    )
}
