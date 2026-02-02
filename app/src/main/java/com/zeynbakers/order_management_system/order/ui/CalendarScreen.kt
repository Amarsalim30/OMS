package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.combinedClickable
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.normalizePickupTime
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.core.ui.VoiceCalculatorOverlay
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import java.math.BigDecimal
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private const val TAG_SHEET_BACK = "SheetBack"

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
    val overlaySuppressed = LocalVoiceOverlaySuppressed.current
    val voiceCalcAccess = LocalVoiceCalcAccess.current
    val voiceRouter = LocalVoiceInputRouter.current
    val scope = rememberCoroutineScope()

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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CalendarTopAppBar(
                monthTitle = monthTitle,
                onPrevMonth = {
                    scope.launch {
                        val targetPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                        pagerState.animateScrollToPage(targetPage)
                    }
                },
                onNextMonth = {
                    scope.launch {
                        val targetPage = (pagerState.currentPage + 1).coerceAtMost(pageCount - 1)
                        pagerState.animateScrollToPage(targetPage)
                    }
                },
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
            FloatingActionButton(
                onClick = {
                    val date = selectedDate ?: today
                    if (selectedDate == null) {
                        onSelectDate(date)
                    }
                    isQuickAddOpen = true
                }
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add order")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthSummaryCard(
                monthTotal = displayedMonthTotal,
                dueCount = displayedBadgeCount
            )

            WeekdayHeaderRow()

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
                    val monthDays = remember(pageYear, pageMonth, snapshot, orderData, today) {
                        snapshot?.days
                            ?: buildMonthGrid(
                                year = pageYear,
                                month = pageMonth,
                                today = today,
                                orderData = orderData
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

    if (isQuickAddOpen && activeDate != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val density = LocalDensity.current
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val amountRegistry = LocalAmountFieldRegistry.current
        val notesRequester = remember { FocusRequester() }
        val totalRequester = remember { FocusRequester() }
        val pickupRequester = remember { FocusRequester() }
        val nameRequester = remember { FocusRequester() }
        val phoneRequester = remember { FocusRequester() }
        val notesState by rememberUpdatedState(notes)
        val setNotes by rememberUpdatedState<(String) -> Unit>({ notes = it })
        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        val imeVisibleState by rememberUpdatedState(imeVisible)
        val keyboardControllerState by rememberUpdatedState(keyboardController)
        val focusManagerState by rememberUpdatedState(focusManager)
        DisposableEffect(Unit) {
            voiceRouter.registerNotesTarget(getNotes = { notesState }, setNotes = setNotes)
            onDispose { voiceRouter.clearNotesTarget() }
        }
        LaunchedEffect(activeDate) {
            notesRequester.requestFocus()
        }
        val dismissSheet = {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            isQuickAddOpen = false
        }
        val handleBackPress: () -> Unit = {
            val sheetOpen = isQuickAddOpen
            if (Log.isLoggable(TAG_SHEET_BACK, Log.DEBUG)) {
                Log.d(TAG_SHEET_BACK, "back pressed imeVisible=$imeVisibleState sheetOpen=$sheetOpen")
            }
            if (imeVisibleState) {
                keyboardControllerState?.hide()
                focusManagerState.clearFocus(force = true)
            } else {
                isQuickAddOpen = false
            }
        }
        val handleDismissRequest: () -> Unit = {
            if (Log.isLoggable(TAG_SHEET_BACK, Log.DEBUG)) {
                Log.d(TAG_SHEET_BACK, "dismiss request imeVisible=$imeVisible sheetOpen=true")
            }
            dismissSheet()
        }
        ModalBottomSheet(
            onDismissRequest = handleDismissRequest,
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
        ) {
            BackHandler(onBack = handleBackPress)
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
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Add order on ${activeDate.dayOfMonth} ${activeDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleMedium
                    )
            Spacer(Modifier.height(6.dp))

                val trimmedNotes = notes.trim()
                val parsedTotal = totalText.trim().takeIf { it.isNotEmpty() }?.let {
                    runCatching { BigDecimal(it) }.getOrNull()
                }
                val formattedTotal = parsedTotal?.let { formatKes(it) }
                val normalizedPickupTime =
                    if (pickupTimeText.isBlank()) null else normalizePickupTime(pickupTimeText)
                val isPickupTimeInvalid = pickupTimeText.isNotBlank() && normalizedPickupTime == null
                val hasCustomerMismatch = customerName.isNotBlank() && customerPhone.isBlank()
                val canSave =
                    trimmedNotes.isNotEmpty() &&
                        parsedTotal != null &&
                        parsedTotal > BigDecimal.ZERO &&
                        !hasCustomerMismatch &&
                        !isPickupTimeInvalid

                fun submitOrder() {
                    when {
                        trimmedNotes.isEmpty() -> {
                            notesError = "Notes are required"
                            totalError = null
                            customerError = null
                        }
                        parsedTotal == null || parsedTotal <= BigDecimal.ZERO -> {
                            notesError = null
                            totalError = "Enter a valid total"
                            customerError = null
                        }
                        hasCustomerMismatch -> {
                            notesError = null
                            totalError = null
                            customerError = "Phone is required to attach a customer"
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
                                customerName.trim(),
                                customerPhone.trim(),
                                normalizedPickupTime
                            )
                            notes = ""
                            totalText = ""
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
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                voiceRouter.onFocusTarget(VoiceTarget.Notes)
                            }
                        }
                )
                notesError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(6.dp))

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
                    supportingText = {
                        when {
                            parsedTotal == null && totalText.isNotBlank() -> Text("Enter a valid total")
                            formattedTotal != null -> Text("Will save as $formattedTotal")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { pickupRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(totalRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                amountRegistry.update(setTotalText)
                                voiceRouter.onFocusTarget(VoiceTarget.Total)
                            }
                        }
                )
                totalError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = pickupTimeText,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() || ch == ':' || ch == '.' }
                        if (filtered.length <= 5) {
                            pickupTimeText = filtered
                        }
                    },
                    label = { Text("Pickup time (optional)") },
                    placeholder = { Text("09:00") },
                    isError = isPickupTimeInvalid,
                    supportingText = {
                        if (isPickupTimeInvalid) {
                            Text("Use HH:MM (e.g., 09:30 or 930).")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { nameRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pickupRequester)
                )

                Spacer(Modifier.height(6.dp))

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
                Spacer(Modifier.height(6.dp))
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
                    Spacer(Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
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
                }

                customerError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { dismissSheet() }
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                notes = ""
                                totalText = ""
                                customerName = ""
                                customerPhone = ""
                                pickupTimeText = ""
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
                            },
                            enabled = canSave
                        ) {
                            Text("Save")
                        }
                    }
                }

                VoiceCalculatorOverlay(
                    hasPermission = voiceCalcAccess.hasPermission,
                    onRequestPermission = voiceCalcAccess.onRequestPermission,
                    lockToRightOnIdle = true,
                    lockToTopOnIdle = true,
                    peekWidthDp = 18.dp,
                    allowDrag = true
                )
            }
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CalendarTopAppBar(
    monthTitle: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSummaryClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onSummaryClick) {
                Icon(imageVector = Icons.Filled.BarChart, contentDescription = "Summary")
            }
        },
        actions = {
            IconButton(onClick = onPrevMonth) {
                Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            IconButton(onClick = onNextMonth) {
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
            IconButton(onClick = onToday) {
                Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = "Today")
            }
        }
    )
}

@Composable
private fun MonthSummaryCard(
    monthTotal: BigDecimal?,
    dueCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Month total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val totalLabel = monthTotal?.let { formatKes(it) } ?: "Loading..."
                Text(text = totalLabel, style = MaterialTheme.typography.titleMedium)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dueCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Due $dueCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        StatusLegendRow()
    }
}

@Composable
private fun StatusLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(label = "Unpaid", state = PaymentState.UNPAID)
        LegendItem(label = "Partial", state = PaymentState.PARTIAL)
        LegendItem(label = "Paid", state = PaymentState.PAID)
        LegendItem(label = "Overpaid", state = PaymentState.OVERPAID)
    }
}

@Composable
private fun LegendItem(label: String, state: PaymentState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = paymentStateColor(state),
            shape = CircleShape,
            modifier = Modifier.size(6.dp)
        ) {}
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeekdayHeaderRow() {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
private fun MonthGrid(
    days: List<CalendarDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onQuickAdd: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier,
        userScrollEnabled = true,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = days, key = { it.date.toString() }) { day ->
            DayCell(
                day = day,
                isSelected = selectedDate == day.date,
                onSelectDate = onSelectDate,
                onOpenDay = onOpenDay,
                onQuickAdd = onQuickAdd,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .aspectRatio(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCell(
    day: CalendarDayUi,
    isSelected: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onQuickAdd: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasOrders = day.orderCount > 0
    val markerStates = remember(day.orderStates, day.orderCount, day.paymentState) {
        resolveMarkerStates(day)
    }
    val overflowCount = (markerStates.size - 3).coerceAtLeast(0)
    val markersToShow = markerStates.take(3)
    val dayDescription = remember(day, markerStates, isSelected) {
        buildDayContentDescription(day, markerStates, isSelected)
    }
    val dayTag = remember(day) { "day-cell-${day.date}" }
    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val baseTextColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val dayTextColor =
        if (day.isInCurrentMonth) {
            baseTextColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    val todayRingColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        }
    val handleClick = {
        onSelectDate(day.date)
        onOpenDay(day.date)
    }

    Surface(
        modifier = modifier
            .testTag(dayTag)
            .semantics(mergeDescendants = true) {
                contentDescription = dayDescription
                role = Role.Button
                selected = isSelected
            }
            .alpha(if (day.isInCurrentMonth) 1f else 0.45f)
            .combinedClickable(
                enabled = day.isInCurrentMonth,
                onClick = { handleClick() },
                onLongClick = { onQuickAdd(day.date) }
            ),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = if (day.isToday) BorderStroke(1.5.dp, todayRingColor) else null,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = dayTextColor,
                        maxLines = 1
                    )
                }
            }
            if (hasOrders) {
                DayMarkers(
                    markers = markersToShow,
                    overflowCount = overflowCount,
                    tag = "day-markers-${day.date}"
                )
            }
        }
    }
}

@Composable
private fun DayMarkers(
    markers: List<PaymentState>,
    overflowCount: Int,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        markers.forEach { state ->
            Surface(
                color = paymentStateColor(state),
                shape = CircleShape,
                modifier = Modifier.size(6.dp)
            ) {}
        }
        if (overflowCount > 0) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.testTag("$tag-overflow")
            ) {
                Text(
                    text = "+$overflowCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun paymentStateColor(state: PaymentState): Color {
    return when (state) {
        PaymentState.PAID -> MaterialTheme.colorScheme.tertiary
        PaymentState.PARTIAL -> MaterialTheme.colorScheme.secondary
        PaymentState.UNPAID -> MaterialTheme.colorScheme.error
        PaymentState.OVERPAID -> MaterialTheme.colorScheme.primary
    }
}

private fun resolveMarkerStates(day: CalendarDayUi): List<PaymentState> {
    if (day.orderStates.isNotEmpty()) {
        return day.orderStates
    }
    if (day.paymentState != null && day.orderCount > 0) {
        return List(day.orderCount) { day.paymentState }
    }
    return emptyList()
}

private fun buildDayContentDescription(
    day: CalendarDayUi,
    markerStates: List<PaymentState>,
    isSelected: Boolean
): String {
    val monthLabel = day.date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val builder = StringBuilder()
    builder.append("${day.date.dayOfMonth} $monthLabel ${day.date.year}")
    if (day.isToday) {
        builder.append(", today")
    }
    if (day.orderCount > 0) {
        builder.append(", ${day.orderCount} orders")
        val unpaidCount = markerStates.count { it == PaymentState.UNPAID }
        val partialCount = markerStates.count { it == PaymentState.PARTIAL }
        val paidCount = markerStates.count { it == PaymentState.PAID }
        val overpaidCount = markerStates.count { it == PaymentState.OVERPAID }
        if (unpaidCount > 0) builder.append(", $unpaidCount unpaid")
        if (partialCount > 0) builder.append(", $partialCount partial")
        if (paidCount > 0) builder.append(", $paidCount paid")
        if (overpaidCount > 0) builder.append(", $overpaidCount overpaid")
    } else {
        builder.append(", no orders")
    }
    if (isSelected) {
        builder.append(", selected")
    }
    return builder.toString()
}

@Composable
private fun BottomActionBar(
    selectedDate: LocalDate?,
    onViewClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = selectedDate?.let { "${it.dayOfMonth} ${it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() }}" }
        ?: "Select a day"
    val isEnabled = selectedDate != null
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(end = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selected day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { selectedDate?.let { onViewClick(it) } },
                    enabled = isEnabled
                ) {
                    Text("View day")
                }
            }
        }
    }
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

private fun monthOffset(anchorYear: Int, anchorMonth: Int, targetYear: Int, targetMonth: Int): Int {
    val anchorTotal = (anchorYear * 12) + (anchorMonth - 1)
    val targetTotal = (targetYear * 12) + (targetMonth - 1)
    return targetTotal - anchorTotal
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

