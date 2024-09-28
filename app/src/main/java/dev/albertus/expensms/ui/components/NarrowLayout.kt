package dev.albertus.expensms.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.viewModels.MainViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NarrowLayout(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    groupedTransactions: Map<LocalDate, List<Transaction>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    selectedMonth: YearMonth,
    showMonthlyTotal: Boolean,
    isAmountVisible: Boolean,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val headerHeight = 450.dp
    val headerHeightPx = with(LocalDensity.current) { headerHeight.toPx() }
    val lazyListState = rememberLazyListState()

    val headerOffsetHeightPx = remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffsetHeightPx.value + delta
                headerOffsetHeightPx.value = newOffset.coerceIn(-headerHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = headerHeight)
        ) {
            groupedTransactions.forEach { (date, transactions) ->
                stickyHeader(key = date) {
                    DayHeader(date)
                }
                items(
                    count = transactions.size,
                    key = { index -> transactions[index].id }
                ) { index ->
                    TransactionItem(
                        transaction = transactions[index],
                        isAmountVisible = isAmountVisible
                    )
                }
                item(key = "${date}_divider") {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .height(headerHeight)
                .offset { IntOffset(x = 0, y = headerOffsetHeightPx.value.roundToInt()) }
                .fillMaxWidth()
        ) {
            Column {
                if (showMonthlyTotal) {
                    MonthlyTotalSpending(
                        month = selectedMonth,
                        totalSpending = viewModel.getMonthlyTotalSpending(),
                        isWideLayout = false,
                        isAmountVisible = isAmountVisible
                    )
                }
                TransactionCalendar(
                    availableDates = groupedTransactions.keys,
                    onDateSelected = onDateSelected,
                    selectedDate = selectedDate,
                    onMonthChanged = viewModel::setSelectedMonth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                )
            }
        }
    }
}