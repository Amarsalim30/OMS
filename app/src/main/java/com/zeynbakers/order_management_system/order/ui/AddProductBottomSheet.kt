package com.zeynbakers.order_management_system.order.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.product.data.ProductEntity
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddProductBottomSheet(
    visible: Boolean,
    cartNotes: String,
    onCartNotesChange: (String) -> Unit,
    productMatches: List<ProductEntity>,
    onProductQueryChange: (String) -> Unit,
    onEnsureProduct: suspend (String, BigDecimal, String) -> ProductEntity,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val cartItems = remember(cartNotes) { OrderCartParser.parseNotesToCart(cartNotes) }
    var productQuery by remember { mutableStateOf("") }
    var isProductDropdownExpanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<SelectedProductDraft?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var unitPriceText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val currencyPrefix = stringResource(R.string.order_editor_currency_prefix)

    val showSuggestions = isProductDropdownExpanded && selectedProduct == null && productQuery.isNotBlank()

    val unitPrice = unitPriceText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val quantityValue = quantity.toLongOrNull() ?: 1L
    val lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantityValue))
    val productName = selectedProduct?.name?.trim().orEmpty().ifBlank { productQuery.trim() }
    val canAddItem = productName.isNotBlank() && unitPrice > BigDecimal.ZERO && quantityValue > 0

    val isDirty = remember(productQuery, selectedProduct, unitPriceText, quantity) {
        productQuery.isNotBlank() || selectedProduct != null || unitPriceText.isNotBlank() || quantity != "1"
    }
    var showDismissConfirm by remember { mutableStateOf(false) }

    // TRACK FOCUS STATES INSTEAD OF WINDOW INSETS
    var isSearchFocused by remember { mutableStateOf(false) }
    var isPriceFocused by remember { mutableStateOf(false) }
    val isAnyFieldFocused = isSearchFocused || isPriceFocused

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Wrap isDirty in a reactive state pointer to prevent the stale lambda bug
    val currentIsDirty by rememberUpdatedState(isDirty)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && currentIsDirty) {
                showDismissConfirm = true // Trigger the dialog safely
                false // Tell the sheet: "Nope, stay open!"
            } else {
                true // Allow closing if the form is pristine
            }
        }
    )

    fun resetForm() {
        productQuery = ""
        onProductQueryChange("")
        selectedProduct = null
        quantity = "1"
        unitPriceText = ""
        isProductDropdownExpanded = false
    }

    fun doDismiss() {
        resetForm()
        onDismiss()
    }

    LaunchedEffect(visible) {
        if (visible) {
            resetForm()
            delay(350)
        }
    }

    // FIXED: Use a minimum height setup instead of dynamic max scaling 
    // to avoid layout shifts when the keyboard collapses on submit.
    val contentModifier = when {
        showSuggestions && productMatches.isNotEmpty() -> Modifier.fillMaxSize()
        else -> Modifier.fillMaxHeight(0.85f)
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isDirty) {
                showDismissConfirm = true
            } else {
                doDismiss()
            }
        },
        sheetState = sheetState,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
    ) {
        // INTERCEPT BACK PRESS BASED ON FOCUS
        BackHandler {
            when {
                isAnyFieldFocused -> {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
                isDirty -> {
                    showDismissConfirm = true
                }
                else -> {
                    doDismiss()
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(contentModifier)
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    // FIXED: Tap outside text input elements explicitly closes keyboard/focus safely
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.order_editor_add_product_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (isDirty) {
                            showDismissConfirm = true
                        } else {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { doDismiss() }
                        }
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_cancel)
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.order_editor_product_search_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val searchBringIntoView = remember { BringIntoViewRequester() }
                OrderEditorOutlinedField(
                    value = if (selectedProduct != null) selectedProduct!!.name else productQuery,
                    onValueChange = {
                        productQuery = it
                        onProductQueryChange(it)
                        selectedProduct = null
                        isProductDropdownExpanded = it.isNotBlank()
                    },
                    placeholder = stringResource(R.string.order_editor_product_search_placeholder),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.action_search)
                        )
                    },
                    trailingIcon = {
                        if (productQuery.isNotBlank() || selectedProduct != null) {
                            IconButton(
                                onClick = { resetForm() },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_clear)
                                )
                            }
                        }
                    },
                    enabled = selectedProduct == null,
                    readOnly = selectedProduct != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(searchBringIntoView)
                            .onFocusChanged { focusState ->
                                // UPDATE SEARCH FOCUS STATE
                                isSearchFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    scope.launch { searchBringIntoView.bringIntoView() }
                                }
                            }
                )

                if (showSuggestions) {
                    ProductSuggestionDropdown(
                        query = productQuery,
                        matches = productMatches,
                        onCreateProduct = { name ->
                            selectedProduct =
                                SelectedProductDraft(
                                    emoji = "📦",
                                    name = name,
                                    unitPrice = BigDecimal.ZERO,
                                    isNewProduct = true
                                )
                            productQuery = name
                            unitPriceText = ""
                            quantity = "1"
                            isProductDropdownExpanded = false
                        },
                        onProductSelected = { product ->
                            selectedProduct =
                                SelectedProductDraft(
                                    emoji = product.emoji,
                                    name = product.name,
                                    unitPrice = product.defaultPrice,
                                    isNewProduct = false,
                                    catalogProductId = product.id
                                )
                            productQuery = product.name
                            unitPriceText =
                                product.defaultPrice
                                    .setScale(0, RoundingMode.HALF_UP)
                                    .stripTrailingZeros()
                                    .toPlainString()
                            quantity = "1"
                            isProductDropdownExpanded = false
                        }
                    )
                }

                if (selectedProduct != null || productName.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(0.45f)) {
                            Text(
                                text = stringResource(R.string.order_editor_quantity_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { newValue ->
                                    // Only allow positive numbers
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        quantity = newValue
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                placeholder = { Text("1") }
                            )
                        }

                        Column(modifier = Modifier.weight(0.45f)) {
                            Text(
                                text = stringResource(R.string.order_editor_unit_price_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            val priceBringIntoView = remember { BringIntoViewRequester() }
                            OrderEditorOutlinedField(
                                value = unitPriceText,
                                onValueChange = { unitPriceText = sanitizeAmountInput(it) },
                                placeholder = stringResource(R.string.order_editor_unit_price_label),
                                prefix = {
                                    Text(
                                        text = currencyPrefix,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(priceBringIntoView)
                                        .onFocusChanged { focusState ->
                                            // UPDATE PRICE FOCUS STATE
                                            isPriceFocused = focusState.isFocused
                                            if (focusState.isFocused) {
                                                scope.launch { priceBringIntoView.bringIntoView() }
                                            }
                                        }
                            )
                        }
                    }
                }
            }

            if (productName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.order_editor_line_total, formatKes(lineTotal)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    val draft =
                        selectedProduct
                            ?: SelectedProductDraft(
                                emoji = "📦",
                                name = productName,
                                unitPrice = unitPrice,
                                isNewProduct = true
                            )
                    val finalPrice =
                        unitPriceText.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
                            ?: BigDecimal.ZERO
                    // Safe sequencing: soft drop inputs first, run validation, modify notes upstream
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    scope.launch {
                        if (draft.isNewProduct) {
                            onEnsureProduct(draft.name, finalPrice, draft.emoji)
                        }
                        val newItem =
                            CartItem(
                                emoji = draft.emoji,
                                name = draft.name,
                                quantity = quantityValue.coerceAtLeast(1).toInt(),
                                unitPrice = finalPrice
                            )
                        onCartNotesChange(OrderCartParser.serializeCartToNotes(cartItems + newItem))
                        resetForm()
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onDismiss()
                    }
                },
                enabled = canAddItem,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = OrderEditorFieldShape
            ) {
                Text(stringResource(R.string.order_editor_add_item_to_order))
            }

            if (showDismissConfirm) {
                AlertDialog(
                    onDismissRequest = { showDismissConfirm = false },
                    title = { Text(stringResource(R.string.order_editor_discard_product_title)) },
                    text = { Text(stringResource(R.string.order_editor_discard_product_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                // RESET FIRST: Clears the dirty state so sheetState.hide() is permitted
                                resetForm()
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showDismissConfirm = false
                                    onDismiss()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.order_editor_discard_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDismissConfirm = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}
@Composable
private fun ProductSuggestionDropdown(
    query: String,
    matches: List<ProductEntity>,
    onCreateProduct: (String) -> Unit,
    onProductSelected: (ProductEntity) -> Unit
) {
    val trimmed = query.trim()
    val showCreate =
        trimmed.isNotBlank() && matches.none { it.name.equals(trimmed, ignoreCase = true) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OrderEditorFieldShape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            if (showCreate) {
                Surface(
                    onClick = { onCreateProduct(trimmed) },
                    modifier = Modifier.sizeIn(minHeight = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.order_editor_create_product, trimmed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
                if (matches.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            matches.forEach { product ->
                Surface(
                    onClick = { onProductSelected(product) },
                    modifier = Modifier.sizeIn(minHeight = 48.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

internal data class SelectedProductDraft(
    val emoji: String,
    val name: String,
    val unitPrice: BigDecimal,
    val isNewProduct: Boolean = false,
    val catalogProductId: Long? = null
)
