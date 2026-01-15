package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.BuildConfig
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.VoiceCalculatorOverlay
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import java.math.BigDecimal
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    days: List<CalendarDayUi>,
    currentYear: Int,
    currentMonth: Int,
    baseYear: Int,
    baseMonth: Int,
    monthSnapshots: Map<MonthKey, MonthSnapshot>,
    monthTotal: java.math.BigDecimal,
    monthBadgeCount: Int,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onSaveOrder: (LocalDate, String, java.math.BigDecimal, String, String) -> Unit,
    searchCustomers: suspend (String) -> List<CustomerEntity>,
    onCustomersClick: () -> Unit,
    onSummaryClick: () -> Unit,
    onMonthSettled: (Int, Int) -> Unit
) {
    var isQuickAddOpen by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var notesError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    val overlaySuppressed = LocalVoiceOverlaySuppressed.current
    val voiceCalcAccess = LocalVoiceCalcAccess.current

    LaunchedEffect(customerName, customerPhone) {
        val query = when {
            customerName.isNotBlank() -> customerName
            customerPhone.isNotBlank() -> customerPhone
            else -> ""
        }
        suggestions = if (query.isBlank()) emptyList() else searchCustomers(query)
    }

    LaunchedEffect(isQuickAddOpen) {
        overlaySuppressed.value = isQuickAddOpen
    }

    DisposableEffect(Unit) {
        onDispose {
            overlaySuppressed.value = false
        }
    }

    val activeDate = selectedDate
    val baseIndex = remember { 10_000 }
    val pageCount = remember { baseIndex * 2 }
    val pagerState = rememberPagerState(initialPage = baseIndex, pageCount = { pageCount })
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val anchorYear =
        when {
            baseYear > 0 -> baseYear
            currentYear > 0 -> currentYear
            else -> today.year
        }
    val anchorMonth =
        when {
            baseMonth > 0 -> baseMonth
            currentMonth > 0 -> currentMonth
            else -> today.monthNumber
        }
    val activeDaysByDate = remember(days) { days.associateBy { it.date } }

    LaunchedEffect(pagerState, anchorYear, anchorMonth) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                val (year, month) = shiftMonth(anchorYear, anchorMonth, settledPage - baseIndex)
                if (year != currentYear || month != currentMonth) {
                    onMonthSettled(year, month)
                }
            }
    }
    val visibleMonth = remember(pagerState.currentPage, anchorYear, anchorMonth) {
        shiftMonth(anchorYear, anchorMonth, pagerState.currentPage - baseIndex)
    }
    val monthTitle = formatMonthTitle(visibleMonth.first, visibleMonth.second)
    val visibleSnapshot = monthSnapshots[MonthKey(visibleMonth.first, visibleMonth.second)]
    val displayedMonthTotal =
        when {
            visibleSnapshot != null -> visibleSnapshot.total
            visibleMonth.first == currentYear && visibleMonth.second == currentMonth -> monthTotal
            else -> null
        }
    val displayedBadgeCount =
        when {
            visibleSnapshot != null -> visibleSnapshot.badgeCount
            visibleMonth.first == currentYear && visibleMonth.second == currentMonth -> monthBadgeCount
            else -> 0
        }

    Scaffold(
        topBar = {
            CalendarTopBar(
                monthTitle = monthTitle,
                badgeCount = displayedBadgeCount,
                onCustomersClick = onCustomersClick,
                onSummaryClick = onSummaryClick
            )
        },
        bottomBar = {
            BottomQuickAddBar(
                selectedDate = selectedDate,
                onClick = { _ -> isQuickAddOpen = true },
                modifier = Modifier.navigationBarsPadding()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isQuickAddOpen = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Month total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val totalLabel =
                        displayedMonthTotal?.let { formatKes(it) } ?: "Loading..."
                    Text(text = totalLabel, style = MaterialTheme.typography.titleLarge)
                }
            }

            WeekdayHeaderRow()

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1
            ) { page ->
                val (pageYear, pageMonth) =
                    remember(page, anchorYear, anchorMonth) {
                        shiftMonth(anchorYear, anchorMonth, page - baseIndex)
                    }
                val snapshot = monthSnapshots[MonthKey(pageYear, pageMonth)]
                val orderData = remember(snapshot, activeDaysByDate, pageYear, pageMonth, currentYear, currentMonth) {
                    when {
                        snapshot != null -> snapshot.days.associateBy { it.date }
                        pageYear == currentYear && pageMonth == currentMonth -> activeDaysByDate
                        else -> emptyMap()
                    }
                }
                val monthDays = remember(pageYear, pageMonth, snapshot, orderData, today) {
                    snapshot?.days
                        ?: buildMonthGrid(
                            year = pageYear,
                            month = pageMonth,
                            today = today,
                            orderData = orderData
                        )
                }

                CalendarMonthGrid(
                    days = monthDays,
                    selectedDate = selectedDate,
                    onSelectDate = onSelectDate,
                    onOpenDay = onOpenDay,
                    onQuickAdd = {
                        onSelectDate(it)
                        isQuickAddOpen = true
                    }
                )
            }
        }
    }

    if (isQuickAddOpen && activeDate != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val density = LocalDensity.current
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val amountRegistry = LocalAmountFieldRegistry.current
        val notesRequester = remember { FocusRequester() }
        val totalRequester = remember { FocusRequester() }
        val nameRequester = remember { FocusRequester() }
        val phoneRequester = remember { FocusRequester() }
        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        LaunchedEffect(activeDate) {
            notesRequester.requestFocus()
        }
        val handleSheetDismiss: () -> Unit = {
            val sheetOpen = isQuickAddOpen
            if (BuildConfig.DEBUG) {
                Log.d("SheetBack", "back pressed imeVisible=$imeVisible sheetOpen=$sheetOpen")
            }
            if (imeVisible) {
                keyboardController?.hide()
                focusManager.clearFocus()
            } else {
                isQuickAddOpen = false
            }
        }
        ModalBottomSheet(
            onDismissRequest = handleSheetDismiss,
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
        ) {
            BackHandler {
                handleSheetDismiss()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .imePadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Add order on ${activeDate.dayOfMonth} ${activeDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                val trimmedNotes = notes.trim()
                val parsedTotal = totalText.trim().takeIf { it.isNotEmpty() }?.let {
                    runCatching { java.math.BigDecimal(it) }.getOrNull()
                }
                val hasCustomerMismatch = (customerName.isBlank() xor customerPhone.isBlank())
                val canSave =
                    trimmedNotes.isNotEmpty() &&
                        parsedTotal != null &&
                        parsedTotal > java.math.BigDecimal.ZERO &&
                        !hasCustomerMismatch

                fun submitOrder() {
                    when {
                        trimmedNotes.isEmpty() -> {
                            notesError = "Notes are required"
                            totalError = null
                            customerError = null
                        }
                        parsedTotal == null || parsedTotal <= java.math.BigDecimal.ZERO -> {
                            notesError = null
                            totalError = "Enter a valid total"
                            customerError = null
                        }
                        hasCustomerMismatch -> {
                            notesError = null
                            totalError = null
                            customerError = "Enter both name and phone, or leave both blank"
                        }
                        else -> {
                            onSaveOrder(
                                activeDate,
                                trimmedNotes,
                                parsedTotal,
                                customerName.trim(),
                                customerPhone.trim()
                            )
                            notes = ""
                            totalText = ""
                            customerName = ""
                            customerPhone = ""
                            notesError = null
                            totalError = null
                            customerError = null
                            isQuickAddOpen = false
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        notesError = null
                    },
                    label = { Text("Notes (required)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { totalRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(notesRequester)
                )
                notesError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))

                val setTotalText by rememberUpdatedState<(String) -> Unit>({ totalText = it })
                OutlinedTextField(
                    value = totalText,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                        if (filtered.count { ch -> ch == '.' } <= 1) {
                            totalText = filtered
                        }
                        totalError = null
                    },
                    label = { Text("Total amount (KES)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { nameRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(totalRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                amountRegistry.update(setTotalText)
                            }
                        }
                )
                totalError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))

                Text(text = "Customer (optional)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        customerError = null
                    },
                    label = { Text("Customer name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { phoneRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameRequester)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = {
                        customerPhone = it
                        customerError = null
                    },
                    label = { Text("Customer phone") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (canSave) {
                                submitOrder()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(phoneRequester)
                )

                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    suggestions.forEach { customer ->
                        TextButton(
                            onClick = {
                                customerName = customer.name
                                customerPhone = customer.phone
                                suggestions = emptyList()
                            }
                        ) {
                            Text("${customer.name} - ${customer.phone}")
                        }
                    }
                }

                customerError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                notes = ""
                                totalText = ""
                                customerName = ""
                                customerPhone = ""
                                notesError = null
                                totalError = null
                                customerError = null
                            }
                        ) {
                            Text("Clear")
                        }
                        TextButton(
                            onClick = {
                                submitOrder()
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }

                VoiceCalculatorOverlay(
                    hasPermission = voiceCalcAccess.hasPermission,
                    onRequestPermission = voiceCalcAccess.onRequestPermission,
                    onApplyAmount = voiceCalcAccess.onApplyAmount,
                    lockToRightOnIdle = true,
                    lockToTopOnIdle = true,
                    peekWidthDp = 18.dp,
                    allowDrag = false
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CalendarTopBar(
    monthTitle: String,
    badgeCount: Int,
    onSummaryClick: () -> Unit,
    onCustomersClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            TextButton(onClick = onSummaryClick) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Summary")
                Spacer(Modifier.width(6.dp))
                Text("Summary")
            }
        },
        actions = {
            TextButton(onClick = onCustomersClick) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Customers")
                Spacer(Modifier.width(6.dp))
                Text("Customers")
            }
            if (badgeCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun WeekdayHeaderRow() {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEachIndexed { index, label ->
            val color =
                when (index) {
                    5 -> MaterialTheme.colorScheme.primary
                    6 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    days: List<CalendarDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onQuickAdd: (LocalDate) -> Unit
) {
    val weeks = remember(days) { days.chunked(7) }
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        weeks.forEachIndexed { index, week ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            WeekBandRow(
                week = week,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                onOpenDay = onOpenDay,
                onQuickAdd = onQuickAdd
            )
            if (index == weeks.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun WeekBandRow(
    week: List<CalendarDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onQuickAdd: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(96.dp)) {
        week.forEach { day ->
            key(day.date) {
                DayColumnCell(
                    day = day,
                    isSelected = selectedDate == day.date,
                    onSelectDate = onSelectDate,
                    onOpenDay = onOpenDay,
                    onQuickAdd = onQuickAdd,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayColumnCell(
    day: CalendarDayUi,
    isSelected: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onQuickAdd: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasOrders = day.orderCount > 0
    val chipLines = buildChipLines(day)
    val extraCount = (day.orderCount - 2).coerceAtLeast(0)

    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Box(
        modifier = modifier
            .padding(4.dp)
            .combinedClickable(
                enabled = day.isInCurrentMonth,
                onClick = { onSelectDate(day.date) },
                onLongClick = { onQuickAdd(day.date) }
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().alpha(if (day.isInCurrentMonth) 1f else 0.45f),
            color = containerColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayNumberChip(day = day, isSelected = isSelected)
                    if (day.isToday) {
                        TodayChip()
                    } else if (hasOrders) {
                        StatusDot(state = day.paymentState)
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (hasOrders) {
                    chipLines.take(2).forEach { chip ->
                        OrderChip(
                            label = chip,
                            state = day.paymentState,
                            onClick = { onOpenDay(day.date) }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (extraCount > 0) {
                        Text(
                            text = "+$extraCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayNumberChip(day: CalendarDayUi, isSelected: Boolean) {
    val textColor =
        if (day.isInCurrentMonth) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    if (isSelected) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    } else {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun TodayChip() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusDot(state: PaymentState?) {
    val color =
        when (state) {
            PaymentState.PAID -> MaterialTheme.colorScheme.tertiary
            PaymentState.PARTIAL -> MaterialTheme.colorScheme.secondary
            PaymentState.UNPAID -> MaterialTheme.colorScheme.error
            PaymentState.OVERPAID -> MaterialTheme.colorScheme.primary
            null -> MaterialTheme.colorScheme.outline
        }
    Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
}

@Composable
private fun OrderChip(label: String, state: PaymentState?, onClick: () -> Unit) {
    val dotColor =
        when (state) {
            PaymentState.PAID -> MaterialTheme.colorScheme.tertiary
            PaymentState.PARTIAL -> MaterialTheme.colorScheme.secondary
            PaymentState.UNPAID -> MaterialTheme.colorScheme.error
            PaymentState.OVERPAID -> MaterialTheme.colorScheme.primary
            null -> MaterialTheme.colorScheme.outline
        }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = dotColor,
                shape = CircleShape,
                modifier = Modifier.size(6.dp)
            ) {}
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BottomQuickAddBar(
    selectedDate: LocalDate?,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = selectedDate?.let { "${it.dayOfMonth} ${it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() }}" }
        ?: "Select a day"
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(enabled = selectedDate != null) {
                selectedDate?.let { onClick(it) }
            },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "Add order on $dateLabel",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildChipLines(day: CalendarDayUi): List<String> {
    if (day.orderCount == 0) return emptyList()
    val totalLine = "${formatKes(day.totalAmount)} - ${if (day.orderCount == 1) "1 order" else "${day.orderCount} orders"}"
    val statusLine =
        when (day.paymentState) {
            PaymentState.UNPAID -> "Unpaid - ${formatKes(day.totalAmount)}"
            PaymentState.PARTIAL -> "Partial - ${formatKes(day.totalAmount)}"
            PaymentState.PAID -> "Paid - ${formatKes(day.totalAmount)}"
            PaymentState.OVERPAID -> "Overpaid - ${formatKes(day.totalAmount)}"
            null -> totalLine
        }
    return listOf(statusLine, totalLine).distinct()
}

private fun buildMonthGrid(
    year: Int,
    month: Int,
    today: LocalDate,
    orderData: Map<LocalDate, CalendarDayUi>
): List<CalendarDayUi> {
    val start = LocalDate(year, month, 1)
    val daysInMonth = daysInMonth(year, month)
    val endOfMonth = LocalDate(year, month, daysInMonth)
    val leadingDays = start.dayOfWeek.ordinal
    val trailingDays = 6 - endOfMonth.dayOfWeek.ordinal
    val gridStart = start.plus(-leadingDays, DateTimeUnit.DAY)

    val totalDays = leadingDays + daysInMonth + trailingDays
    return (0 until totalDays).map { offset ->
        val date = gridStart.plus(offset, DateTimeUnit.DAY)
        val existing = orderData[date]
        if (existing != null) {
            existing.copy(isToday = date == today, isInCurrentMonth = date.monthNumber == month)
        } else {
            CalendarDayUi(
                date = date,
                orderCount = 0,
                totalAmount = BigDecimal.ZERO,
                isToday = date == today,
                isInCurrentMonth = date.monthNumber == month,
                paymentState = null
            )
        }
    }
}

private fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    if (month == 0 || year == 0) return Pair(year, month)
    val total = (year * 12) + (month - 1) + delta
    val newYear = total / 12
    val newMonth = (total % 12) + 1
    return Pair(newYear, newMonth)
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }
}

private fun formatMonthTitle(year: Int, month: Int): String {
    if (month !in 1..12 || year <= 0) return "Loading..."
    val monthName =
        when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Month"
        }
    return "$monthName $year"
}

