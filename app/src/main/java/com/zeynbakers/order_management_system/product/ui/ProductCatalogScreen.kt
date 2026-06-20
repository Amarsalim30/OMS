@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.zeynbakers.order_management_system.product.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.product.data.ProductEntity
import java.math.BigDecimal

@Composable
fun ProductCatalogScreen(
    viewModel: ProductViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var pendingArchiveProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.product_catalog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.product_count,
                                products.size,
                                products.size
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.product_add_new)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { showAddSheet = true },
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            )
        }
    ) { padding ->
        if (products.isEmpty()) {
            ProductEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAddProduct = { showAddSheet = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 100.dp // space for FAB
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onEdit = { editingProduct = product },
                        onDelete = { pendingArchiveProduct = product }
                    )
                }
            }
        }
    }

    // Add / Edit sheet
    val editTarget = editingProduct
    if (showAddSheet || editTarget != null) {
        ProductFormSheet(
            existing = editTarget,
            onDismiss = {
                showAddSheet = false
                editingProduct = null
            },
            onSave = { name, price, emoji ->
                if (editTarget != null) {
                    viewModel.updateProduct(
                        editTarget.copy(name = name, defaultPrice = price, emoji = emoji)
                    )
                } else {
                    viewModel.addProduct(name, price, emoji)
                }
                showAddSheet = false
                editingProduct = null
            }
        )
    }

    // Archive confirmation dialog
    pendingArchiveProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingArchiveProduct = null },
            title = { Text(stringResource(R.string.product_remove_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.product_remove_message,
                        product.emoji,
                        product.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveProduct(product.id)
                        pendingArchiveProduct = null
                    }
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingArchiveProduct = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

// ─── Product Card ────────────────────────────────────────────────────────────

@Composable
private fun ProductCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.emoji.ifBlank { "📦" },
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.product_default_price_format,
                        product.defaultPrice.toPlainString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.notes_history_action_edit),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.notes_history_action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun ProductEmptyState(
    modifier: Modifier = Modifier,
    onAddProduct: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🛒",
            fontSize = 56.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.product_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.product_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddProduct) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.product_add_first))
        }
    }
}

// ─── Add / Edit Form Sheet ────────────────────────────────────────────────────

@Composable
private fun ProductFormSheet(
    existing: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, price: BigDecimal, emoji: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var priceText by rememberSaveable {
        mutableStateOf(
            if (existing != null && existing.defaultPrice > BigDecimal.ZERO) {
                existing.defaultPrice.toPlainString()
            } else {
                ""
            }
        )
    }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    val isEditing = existing != null

    val suggestedEmojis = listOf(
        "🥧", "🍞", "🥐", "🍰", "🍩", "🍪", "🎂", "🥖",
        "🧁", "🍫", "🥜", "🧃", "📦", "🍗", "🥩", "🌮"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isEditing) stringResource(R.string.product_edit_title) else stringResource(
                    R.string.product_add_title
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            // Emoji picker row
            Text(
                text = stringResource(R.string.product_pick_emoji),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedEmojis.take(8).forEach { e ->
                    val isSelected = emoji == e
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = e, fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedEmojis.drop(8).take(8).forEach { e ->
                    val isSelected = emoji == e
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = e, fontSize = 20.sp)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Custom emoji text field
            OutlinedTextField(
                value = emoji,
                onValueChange = { if (it.length <= 4) emoji = it },
                label = { Text(stringResource(R.string.product_custom_emoji_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("📦") }
            )

            Spacer(Modifier.height(16.dp))

            // Product name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.product_name_label)) },
                placeholder = { Text(stringResource(R.string.product_name_placeholder)) },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.product_name_required)) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Default price
            OutlinedTextField(
                value = priceText,
                onValueChange = {
                    priceText = it
                    priceError = false
                },
                label = { Text(stringResource(R.string.product_price_label)) },
                placeholder = { Text(stringResource(R.string.product_price_placeholder)) },
                singleLine = true,
                isError = priceError,
                supportingText = if (priceError) {
                    { Text(stringResource(R.string.product_price_invalid)) }
                } else {
                    { Text(stringResource(R.string.product_price_hint)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                prefix = { Text(stringResource(R.string.order_editor_currency_prefix) + " ") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        nameError = name.isBlank()
                        val parsedPrice =
                            priceText.trim().toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
                        priceError = parsedPrice == null && priceText.isNotBlank()
                        if (!nameError) {
                            onSave(
                                name.trim(),
                                parsedPrice ?: BigDecimal.ZERO,
                                emoji.trim().ifBlank { "📦" }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEditing) stringResource(R.string.action_save) else stringResource(R.string.action_save))
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
