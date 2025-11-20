package com.example.firebaseauthtesting.Pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulingDialog(
    onDismiss: () -> Unit,
    onConfirm: (date: String, time: String) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(true) }

    val timePickerState = rememberTimePickerState(is24Hour = false)
    var showTimePicker by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val finalTime = formatTime(timePickerState.hour, timePickerState.minute)
                    onConfirm(selectedDate, finalTime)
                },
                // The confirm button is only enabled after a date and time have been selected.
                enabled = selectedDate.isNotEmpty() && showTimePicker
            ) {
                Text("Confirm Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(if (showDatePicker) "Select a Date" else "Select a Time") },
        text = {
            if (showDatePicker) {
                DatePicker(
                    state = datePickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false // Simplifies the UI
                )
                // Button to move to the time selection step
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedDate = formatDate(it)
                                showDatePicker = false
                                showTimePicker = true
                            }
                        }
                    ) {
                        Text("Next")
                    }
                }
            } else if (showTimePicker) {
                // FIX: Wrap the TimePicker in a Column to provide the correct scope for .align()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(
                        state = timePickerState,
                        layoutType = TimePickerLayoutType.Vertical
                    )
                }
            }
        }
    )
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(calendar.time)
}
