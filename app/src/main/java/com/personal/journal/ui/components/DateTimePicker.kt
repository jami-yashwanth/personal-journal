package com.personal.journal.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.personal.journal.ui.theme.JournalColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Two-step date then time picker. Calls [onPicked] with epoch millis in [zone]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initial: Long,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val initialDt = Instant.ofEpochMilli(initial).atZone(zone)
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val timeState = rememberTimePickerState(initialDt.hour, initialDt.minute, is24Hour = true)
    val colors = DatePickerDefaults.colors(containerColor = JournalColors.Card)

    if (pickedDate == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            colors = colors,
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                }) { Text("Next", color = JournalColors.TextPrimary) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = JournalColors.TextSecondary) } },
        ) { DatePicker(dateState, colors = colors) }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = JournalColors.Card,
            confirmButton = {
                TextButton(onClick = {
                    val t = LocalTime.of(timeState.hour, timeState.minute)
                    onPicked(pickedDate!!.atTime(t).atZone(zone).toInstant().toEpochMilli())
                }) { Text("Done", color = JournalColors.TextPrimary) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = JournalColors.TextSecondary) } },
            text = { TimePicker(timeState) },
        )
    }
}
