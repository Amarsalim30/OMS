package com.zeynbakers.order_management_system.order.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    days: List<CalendarDayUi>,
    monthLabel: String,
    monthKey: Int,
    monthTotal: java.math.BigDecimal,
    monthBadgeCount: Int,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onSaveOrder: (LocalDate, String, java.math.BigDecimal, String, String) -> Unit,
    searchCustomers: suspend (String) -> List<CustomerEntity>,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val dragAmount = remember { mutableStateOf(0f) }
        val monthTitle = monthLabel.split(" ").firstOrNull()?.take(3)?.uppercase() ?: "MON"

    var isQuickAddOpen by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var notesError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }

    LaunchedEffect(customerName, customerPhone) {
        val query = when {
            customerName.isNotBlank() -> customerName
            customerPhone.isNotBlank() -> customerPhone
            else -> ""
        }
        suggestions = if (query.isBlank()) emptyList() else searchCustomers(query)
    }

    val activeDate = selectedDate

    Scaffold(
        topBar = {
            CalendarTopBar(
                monthTitle = monthTitle,
                badgeCount = monthBadgeCount,
                onMenuClick = onMenuClick,
                onSearchClick = onSearchClick
            )
        },
        bottomBar = {
            BottomQuickAddBar(
                selectedDate = selectedDate,
                onClick = { _ ->
                    isQuickAddOpen = true
                }
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
                    Text(
                        text = formatKes(monthTotal),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            WeekdayHeaderRow()

            AnimatedContent(
                targetState = monthKey,
                transitionSpec = {
                    val isForward = targetState > initialState
                    val direction = if (isForward) -1 else 1
                    slideInHorizontally(
                        animationSpec = tween(200),
                        initialOffsetX = { it * direction }
                    ) + fadeIn(animationSpec = tween(200)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(200),
                            targetOffsetX = { it * -direction }
                        ) + fadeOut(animationSpec = tween(200))
                },
                label = "monthTransition"
            ) {
                CalendarMonthGrid(
                    days = days,
                    selectedDate = selectedDate,
                    onSelectDate = {
                        onSelectDate(it)
                    },
                    onOpenDay = onOpenDay,
                    onQuickAdd = {
                        onSelectDate(it)
                        isQuickAddOpen = true
                    },
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    dragAmount = dragAmount
                )
            }
                    }
    }

    if (isQuickAddOpen && activeDate != null) {
        ModalBottomSheet(onDismissRequest = { isQuickAddOpen = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add order on ${activeDate.dayOfMonth} ${activeDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        notesError = null
                    },
                    label = { Text("Notes (required)") },
                    modifier = Modifier.fillMaxWidth()
                )
                notesError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalText,
                    onValueChange = {
                        totalText = it
                        totalError = null
                    },
                    label = { Text("Total amount (KES)") },
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = {
                        customerPhone = it
                        customerError = null
                    },
                    label = { Text("Customer phone") },
                    modifier = Modifier.fillMaxWidth()
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
                            val trimmedNotes = notes.trim()
                            val parsedTotal = totalText.trim().takeIf { it.isNotEmpty() }?.let {
                                runCatching { java.math.BigDecimal(it) }.getOrNull()
                            }
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
                                (customerName.isBlank() xor customerPhone.isBlank()) -> {
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
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CalendarTopBar(
    monthTitle: String,
    badgeCount: Int,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit
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
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
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
    onQuickAdd: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    dragAmount: androidx.compose.runtime.MutableState<Float>
) {
    val weeks = days.chunked(7)
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, drag ->
                        dragAmount.value += drag
                        change.consume()
                    },
                    onDragEnd = {
                        val threshold = 80f
                        when {
                            dragAmount.value > threshold -> onPrevMonth()
                            dragAmount.value < -threshold -> onNextMonth()
                        }
                        dragAmount.value = 0f
                    },
                    onDragCancel = { dragAmount.value = 0f }
                )
            }
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
private fun BottomQuickAddBar(selectedDate: LocalDate?, onClick: (LocalDate) -> Unit) {
    val dateLabel = selectedDate?.let { "${it.dayOfMonth} ${it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() }}" }
        ?: "Select a day"
    Surface(
        modifier = Modifier
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
    val totalLine = "${formatKes(day.totalAmount)} ? ${if (day.orderCount == 1) "1 order" else "${day.orderCount} orders"}"
    val statusLine =
        when (day.paymentState) {
            PaymentState.UNPAID -> "Unpaid ? ${formatKes(day.totalAmount)}"
            PaymentState.PARTIAL -> "Partial ? ${formatKes(day.totalAmount)}"
            PaymentState.PAID -> "Paid ? ${formatKes(day.totalAmount)}"
            PaymentState.OVERPAID -> "Overpaid ? ${formatKes(day.totalAmount)}"
            null -> totalLine
        }
    return listOf(statusLine, totalLine).distinct()
}

