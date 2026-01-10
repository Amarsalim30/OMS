@Composable
fun OrderEditor(
    order: OrderEntity,
    onSave: (OrderEntity) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                onSave(order.copy(notes = it))
            },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = total,
            onValueChange = {
                total = it
                if (it.isNotBlank())
                    onSave(order.copy(totalAmount = BigDecimal(it)))
            },
            label = { Text("Total") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
