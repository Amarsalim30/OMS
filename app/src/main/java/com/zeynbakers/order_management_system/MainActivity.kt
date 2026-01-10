package com.zeynbakers.order_management_system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zeynbakers.order_management_system.core.ui.theme.Order_management_systemTheme
import kotlinx.datetime.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel = remember { /* inject OrderViewModel */ }

            var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

            CalendarScreen(days = calendarDays) {
                selectedDate = it
            }

            selectedDate?.let {
                // Next phase: OrderEditorSheet
            }
        }

        // setContent {
        //     Order_management_systemTheme {
        //         Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        //             Greeting(
        //                 name = "Android",
        //                 modifier = Modifier.padding(innerPadding)
        //             )
        //         }
        //     }
        // }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Order_management_systemTheme {
        Greeting("Android")
    }
}