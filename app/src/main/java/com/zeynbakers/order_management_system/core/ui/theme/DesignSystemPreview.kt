package com.zeynbakers.order_management_system.core.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DesignSystemContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "High-Fidelity UI Design System",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(text = "Typography", style = MaterialTheme.typography.titleLarge)
        Text(text = "Display Large", style = MaterialTheme.typography.displayLarge)
        Text(text = "Headline Medium", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Title Medium (Inter Font)", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Body Large: The quick brown fox jumps over the lazy dog.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Colors & Shapes", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { }, shape = MaterialTheme.shapes.small) {
                Text(text = "Primary Button")
            }
            Button(onClick = { }, shape = MaterialTheme.shapes.medium) {
                Text(text = "Medium Shape")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Surface Variant Card", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorBox(color = MaterialTheme.colorScheme.primary, label = "Primary")
            ColorBox(color = MaterialTheme.colorScheme.secondary, label = "Secondary")
            ColorBox(color = MaterialTheme.colorScheme.tertiary, label = "Tertiary")
        }
    }
}

@Composable
fun ColorBox(color: androidx.compose.ui.graphics.Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color, MaterialTheme.shapes.small)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun LightPreview() {
    Order_management_systemTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DesignSystemContent()
        }
    }
}

@Preview(showBackground = true, name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DarkPreview() {
    Order_management_systemTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DesignSystemContent()
        }
    }
}
