package dev.albertus.expensms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.header.MonthState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TransactionCalendar(
    availableDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    selectedDate: LocalDate?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
    ) {
        SelectableCalendar(
            modifier = Modifier.padding(8.dp),
            dayContent = { dayState ->
                CalendarDay(
                    state = dayState,
                    isAvailable = availableDates.contains(dayState.date),
                    onDateSelected = onDateSelected,
                    isSelected = dayState.date == selectedDate
                )
            },
            monthHeader = { monthState ->
                MonthHeader(monthState)
            }
        )
    }
}

@Composable
fun MonthHeader(monthState: MonthState) {
    val month = monthState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val year = monthState.currentMonth.year
    Text(
        text = "$month $year",
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun CalendarDay(
    state: DayState<DynamicSelectionState>,
    isAvailable: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    isSelected: Boolean
) {
    val date = state.date

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isAvailable -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .clickable(
                enabled = isAvailable,
                onClick = { onDateSelected(date) }
            )
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isAvailable -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    }
}