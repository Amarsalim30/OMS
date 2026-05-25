package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.product.data.ProductEntity
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch

@Composable
fun OrderCartEditor(
    notes: String,
    onNotesChange: (String) -> Unit,
    productMatches: List<ProductEntity>,
    onProductQueryChange: (String) -> Unit,
    onEnsureProduct: suspend (String, BigDecimal, String) -> ProductEntity,
    modifier: Modifier = Modifier
) {
    val cartItems = remember(notes) { OrderCartParser.parseNotesToCart(notes) }
    var productQuery by remember { mutableStateOf("") }
    var isProductDropdownExpanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<SelectedProductDraft?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var unitPriceText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val currencyPrefix = stringResource(R.string.order_editor_currency_prefix)

    val selectedLineTotal =
        remember(selectedProduct, quantity, unitPriceText) {
            val unitPrice = unitPriceText.toBigDecimalOrNull() ?: BigDecimal.ZERO
            unitPrice.multiply(BigDecimal.valueOf(quantity.toLong()))
        }

    val productPanelActive = selectedProduct != null || productQuery.isNotBlank()

    fun clearProductEntry() {
        productQuery = ""
        onProductQueryChange("")
        selectedProduct = null
        quantity = 1
        unitPriceText = ""
        isProductDropdownExpanded = false
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (cartItems.isNotEmpty()) {
            Text(
                text = stringResource(R.string.order_editor_cart_section_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cartItems.forEachIndexed { index, item ->
                    val label =
                        buildString {
                            if (item.emoji.isNotBlank()) append("${item.emoji} ")
                            append("${item.name} x ${item.quantity}")
                        }
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val updated = cartItems.toMutableList().apply { removeAt(index) }
                                    onNotesChange(OrderCartParser.serializeCartToNotes(updated))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.day_delete_order),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = stringResource(R.string.order_editor_product_section_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = OrderEditorFieldShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        if (productPanelActive) {
                            IconButton(onClick = { clearProductEntry() }, modifier = Modifier.size(40.dp)) {
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
                    modifier = Modifier.fillMaxWidth()
                )

                if (isProductDropdownExpanded && selectedProduct == null) {
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
                            quantity = 1
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
                            quantity = 1
                            isProductDropdownExpanded = false
                        }
                    )
                }

                if (selectedProduct != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.order_editor_quantity_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Remove,
                                        contentDescription =
                                            stringResource(R.string.order_editor_decrease_qty)
                                    )
                                }
                                Text(
                                    text = quantity.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription =
                                            stringResource(R.string.order_editor_increase_qty)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.order_editor_line_total, formatKes(selectedLineTotal)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            val draft = selectedProduct ?: return@Button
                            val unitPrice =
                                unitPriceText.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
                                    ?: BigDecimal.ZERO
                            scope.launch {
                                if (draft.isNewProduct) {
                                    onEnsureProduct(draft.name, unitPrice, draft.emoji)
                                }
                                val newItem =
                                    CartItem(
                                        emoji = draft.emoji,
                                        name = draft.name,
                                        quantity = quantity.coerceAtLeast(1),
                                        unitPrice = unitPrice
                                    )
                                onNotesChange(
                                    OrderCartParser.serializeCartToNotes(cartItems + newItem)
                                )
                                clearProductEntry()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = OrderEditorFieldShape
                    ) {
                        Text(stringResource(R.string.order_editor_add_item_to_order))
                    }
                }
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
                Surface(onClick = { onCreateProduct(trimmed) }) {
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
                Surface(onClick = { onProductSelected(product) }) {
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

private data class SelectedProductDraft(
    val emoji: String,
    val name: String,
    val unitPrice: BigDecimal,
    val isNewProduct: Boolean = false,
    val catalogProductId: Long? = null
)
