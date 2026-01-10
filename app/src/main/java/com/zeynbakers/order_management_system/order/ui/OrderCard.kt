@Composable
fun OrderCard(
    order: OrderEntity,
    onSave: (OrderEntity) -> Unit
) {
    var notes by remember { mutableStateOf(order.notes) }
    var total by remember { mutableStateOf(order.totalAmount.toPlainString()) }
    var paid by remember { mutableStateOf(order.amountPaid.toPlainString()) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                    onSave(order.copy(notes = it))
                },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Row {
                OutlinedTextField(
                    value = total,
                    onValueChange = {
                        total = it
                        onSave(order.copy(totalAmount = BigDecimal(it)))
                    },
                    label = { Text("Total") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                OutlinedTextField(
                    value = paid,
                    onValueChange = {
                        paid = it
                        onSave(order.copy(amountPaid = BigDecimal(it)))
                    },
                    label = { Text("Paid") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
