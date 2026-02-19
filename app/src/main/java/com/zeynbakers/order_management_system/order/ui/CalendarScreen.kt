package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.normalizePickupTime
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.ui.LocalVoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.core.calendar.CalendarPreferences
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import java.math.BigDecimal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.util.Calendar
import java.util.Locale
import java.time.format.DateTimeFormatter
import kotlinx.datetime.toJavaLocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    days: List<CalendarDayUi>,
    currentYear: Int,
    currentMonth: Int,
    baseYear: Int,
    baseMonth: Int,
    monthSnapshots: Map<MonthKey, MonthSnapshot>,
    monthTotal: BigDecimal,
    monthBadgeCount: Int,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onSaveOrder: (LocalDate, String, BigDecimal, String, String, String?) -> Unit,
    searchCustomers: suspend (String) -> List<CustomerEntity>,
    onSummaryClick: () -> Unit,
    onMonthSettled: (Int, Int) -> Unit,
    openQuickAddDate: LocalDate?,
    onQuickAddConsumed: () -> Unit
) {
    var isQuickAddOpen by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var pickupTimeText by remember { mutableStateOf("") }
    var notesError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    var isMonthPickerOpen by remember { mutableStateOf(false) }
    var isLegendOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val calendarPrefs = remember { CalendarPreferences(context) }
    var weekStartPreference by remember { mutableStateOf(calendarPrefs.readWeekStart()) }
    val overlaySuppressed = LocalVoiceOverlaySuppressed.current
    val voiceCalcAccess = LocalVoiceCalcAccess.current
    val voiceRouter = LocalVoiceInputRouter.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { customerName.trim() to customerPhone.trim() }
            .debounce(250)
            .distinctUntilChanged()
            .collectLatest { (query, selectedPhone) ->
                suggestions =
                    if (query.isBlank() || selectedPhone.isNotBlank()) {
                        emptyList()
                    } else {
                        searchCustomers(query)
                    }
            }
    }

    LaunchedEffect(isQuickAddOpen) {
        overlaySuppressed.value = isQuickAddOpen
    }

    LaunchedEffect(openQuickAddDate) {
        val target = openQuickAddDate ?: return@LaunchedEffect
        onSelectDate(target)
        isQuickAddOpen = true
        onQuickAddConsumed()
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
    val today = rememberCurrentDate()
    val systemWeekStart = remember { Calendar.getInstance(Locale.getDefault()).firstDayOfWeek }
    val weekStart = weekStartPreference ?: systemWeekStart
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
    val visibleMonth by remember(pagerState, anchorYear, anchorMonth) {
        derivedStateOf { shiftMonth(anchorYear, anchorMonth, pagerState.currentPage - baseIndex) }
    }
    val monthTitle by remember(visibleMonth) {
        derivedStateOf { formatMonthTitle(visibleMonth.first, visibleMonth.second) }
    }
    val visibleSnapshot by remember(visibleMonth, monthSnapshots) {
        derivedStateOf { monthSnapshots[MonthKey(visibleMonth.first, visibleMonth.second)] }
    }
    val displayedMonthTotal by remember(
        visibleSnapshot,
        visibleMonth,
        currentYear,
        currentMonth,
        monthTotal
    ) {
        derivedStateOf {
            val snapshot = visibleSnapshot
            when {
                snapshot != null -> snapshot.total
                visibleMonth.first == currentYear && visibleMonth.second == currentMonth -> monthTotal
                else -> null
            }
        }
    }
    val displayedBadgeCount by remember(
        visibleSnapshot,
        visibleMonth,
        currentYear,
        currentMonth,
        monthBadgeCount
    ) {
        derivedStateOf {
            val snapshot = visibleSnapshot
            when {
                snapshot != null -> snapshot.badgeCount
                visibleMonth.first == currentYear && visibleMonth.second == currentMonth -> monthBadgeCount
                else -> 0
            }
        }
    }
    val hasOrdersInVisibleMonth by remember(
        visibleSnapshot,
        visibleMonth,
        currentYear,
        currentMonth,
        days
    ) {
        derivedStateOf {
            val snapshot = visibleSnapshot
            when {
                snapshot != null ->
                    snapshot.days.any { it.isInCurrentMonth && it.orderCount > 0 }
                visibleMonth.first == currentYear && visibleMonth.second == currentMonth ->
                    days.any { it.isInCurrentMonth && it.orderCount > 0 }
                else -> false
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CalendarTopAppBar(
                monthTitle = monthTitle,
                todayDay = today.dayOfMonth,
                onMonthPickerClick = { isMonthPickerOpen = true },
                onToday = {
                    scope.launch {
                        val jump = monthOffset(anchorYear, anchorMonth, today.year, today.monthNumber)
                        pagerState.animateScrollToPage(baseIndex + jump)
                        onSelectDate(today)
                    }
                },
                onSummaryClick = onSummaryClick
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = {
                    val date = selectedDate ?: today
                    if (selectedDate == null) {
                        onSelectDate(date)
                    }
                    isQuickAddOpen = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.calendar_add_order)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthSummaryCard(
                ownerTitle = stringResource(R.string.calendar_owner_title),
                ownerSubtitle = stringResource(R.string.calendar_owner_subtitle),
                monthTotal = displayedMonthTotal,
                dueCount = displayedBadgeCount,
                hasOrders = hasOrdersInVisibleMonth,
                onDueClick = onSummaryClick,
                onAddOrder = {
                    val targetDate =
                        if (visibleMonth.first == today.year && visibleMonth.second == today.monthNumber) {
                            today
                        } else {
                            LocalDate(visibleMonth.first, visibleMonth.second, 1)
                        }
                    onSelectDate(targetDate)
                    isQuickAddOpen = true
                },
                onLegendClick = { isLegendOpen = true }
            )

            WeekdayHeaderRow(weekStart = weekStart)

            Box(modifier = Modifier.weight(1f, fill = true)) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
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
                    val monthDays = remember(pageYear, pageMonth, orderData, today, weekStart) {
                        buildMonthGrid(
                            year = pageYear,
                            month = pageMonth,
                            today = today,
                            orderData = orderData,
                            weekStart = weekStart
                        )
                    }

                    MonthGrid(
                        days = monthDays,
                        selectedDate = selectedDate,
                        onSelectDate = onSelectDate,
                        onOpenDay = onOpenDay,
                        onQuickAdd = {
                            onSelectDate(it)
                            isQuickAddOpen = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (isMonthPickerOpen) {
        MonthPickerSheet(
            initialYear = visibleMonth.first,
            initialMonth = visibleMonth.second,
            onDismiss = { isMonthPickerOpen = false },
            onMonthSelected = { year, month ->
                val jump = monthOffset(anchorYear, anchorMonth, year, month)
                scope.launch {
                    pagerState.animateScrollToPage(baseIndex + jump)
                }
                isMonthPickerOpen = false
            }
        )
    }

    if (isLegendOpen) {
        LegendInfoSheet(
            weekStart = weekStart,
            systemWeekStart = systemWeekStart,
            onDismiss = { isLegendOpen = false },
            onWeekStartChange = { selection ->
                weekStartPreference = selection
                calendarPrefs.setWeekStart(selection)
            }
        )
    }

    if (isQuickAddOpen && activeDate != null) {
        val amountRegistry = LocalAmountFieldRegistry.current
        val notesRequiredMessage = stringResource(R.string.day_editor_notes_required)
        val validTotalRequiredMessage = stringResource(R.string.day_editor_valid_total_required)
        val addOrderDateLabel = remember(activeDate) {
            DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()).format(activeDate.toJavaLocalDate())
        }
        val notesState by rememberUpdatedState(notes)
        val setNotes by rememberUpdatedState<(String) -> Unit>({ notes = it })
        DisposableEffect(Unit) {
            voiceRouter.registerNotesTarget(getNotes = { notesState }, setNotes = setNotes)
            onDispose { voiceRouter.clearNotesTarget() }
        }
        val trimmedNotes = notes.trim()
        val parsedTotal = totalText.trim().takeIf { it.isNotEmpty() }?.let {
            runCatching { BigDecimal(it) }.getOrNull()
        }
        val formattedTotal = parsedTotal?.let { formatKes(it) }
        val normalizedPickupTime =
            if (pickupTimeText.isBlank()) null else normalizePickupTime(pickupTimeText)
        val isPickupTimeInvalid = pickupTimeText.isNotBlank() && normalizedPickupTime == null
        val attachedCustomerPhone = customerPhone.trim()
        val attachedCustomerName =
            if (attachedCustomerPhone.isNotBlank()) {
                customerName.trim()
            } else {
                ""
            }
        val canSave =
            trimmedNotes.isNotEmpty() &&
                parsedTotal != null &&
                parsedTotal > BigDecimal.ZERO &&
                !isPickupTimeInvalid

        fun submitOrder() {
            when {
                trimmedNotes.isEmpty() -> {
                    notesError = notesRequiredMessage
                    totalError = null
                    customerError = null
                }
                parsedTotal == null || parsedTotal <= BigDecimal.ZERO -> {
                    notesError = null
                    totalError = validTotalRequiredMessage
                    customerError = null
                }
                isPickupTimeInvalid -> {
                    notesError = null
                    totalError = null
                    customerError = null
                }
                else -> {
                    onSaveOrder(
                        activeDate,
                        trimmedNotes,
                        parsedTotal,
                        attachedCustomerName,
                        attachedCustomerPhone,
                        normalizedPickupTime
                    )
                    notes = ""
                    totalText = ""
                    suggestions = emptyList()
                    customerName = ""
                    customerPhone = ""
                    pickupTimeText = ""
                    notesError = null
                    totalError = null
                    customerError = null
                    isQuickAddOpen = false
                }
            }
        }

        OrderEditorSheet(
            title = stringResource(R.string.calendar_order_add_title, addOrderDateLabel),
            notes = notes,
            onNotesChange = {
                notes = it
                notesError = null
            },
            notesError = notesError,
            totalText = totalText,
            onTotalTextChange = {
                totalText = sanitizeAmountInput(it)
                totalError = null
            },
            isTotalInvalid = parsedTotal == null && totalText.isNotBlank(),
            totalSupportingText = when {
                parsedTotal == null && totalText.isNotBlank() -> validTotalRequiredMessage
                formattedTotal != null -> stringResource(R.string.calendar_total_will_save, formattedTotal)
                else -> null
            },
            totalError = totalError,
            statusText = null,
            pickupTimeText = pickupTimeText,
            onPickupTimeChange = { pickupTimeText = sanitizePickupTimeInput(it) },
            isPickupTimeInvalid = isPickupTimeInvalid,
            customerName = customerName,
            onCustomerNameChange = {
                customerName = it
                customerError = null
            },
            customerPhone = customerPhone,
            onCustomerPhoneChange = {
                customerPhone = it
                customerError = null
            },
            suggestions = suggestions,
            onSuggestionSelected = { customer ->
                customerName = customer.name
                customerPhone = customer.phone
                suggestions = emptyList()
            },
            customerError = customerError,
            canSave = canSave,
            onSave = ::submitOrder,
            focusNotesInitially = false,
            onClear = {
                notes = ""
                totalText = ""
                customerName = ""
                customerPhone = ""
                pickupTimeText = ""
                notesError = null
                totalError = null
                customerError = null
            },
            onCancel = { isQuickAddOpen = false },
            onNotesFocused = { voiceRouter.onFocusTarget(VoiceTarget.Notes) },
            onTotalFocused = { setter ->
                amountRegistry.update(setter)
                voiceRouter.onFocusTarget(VoiceTarget.Total)
            },
            voiceHasPermission = voiceCalcAccess.hasPermission,
            onRequestVoicePermission = voiceCalcAccess.onRequestPermission
        )
    }
}



