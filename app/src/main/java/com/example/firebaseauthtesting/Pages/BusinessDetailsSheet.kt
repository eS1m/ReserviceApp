package com.example.firebaseauthtesting.Pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebaseauthtesting.Models.Business
import com.example.firebaseauthtesting.Models.Review
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.ViewModels.BusinessDetailsViewModel
import com.example.firebaseauthtesting.ViewModels.ServiceRequestState
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDetailsSheet(
    businessId: String,
    detailsViewModel: BusinessDetailsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by detailsViewModel.uiState.collectAsState()
    val requestState by detailsViewModel.requestState.collectAsState()
    val fullName by authViewModel.fullName.collectAsStateWithLifecycle()
    var showSchedulingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(businessId) {
        if (businessId.isNotBlank()) {
            detailsViewModel.selectBusiness(businessId)
        }
    }

    if (showSchedulingDialog) {
        CustomSchedulingDialog(
            onDismiss = { showSchedulingDialog = false },
            onConfirm = { date: String, time: String ->
                uiState.business?.let {
                    detailsViewModel.createServiceRequest(
                        business = it,
                        userName = if (fullName.isNullOrBlank()) "Anonymous" else fullName!!,
                        scheduledDate = date,
                        scheduledTime = time
                    )
                }
                showSchedulingDialog = false
            }
        )
    }

    LaunchedEffect(requestState) {
        if (requestState is ServiceRequestState.Success) {
            detailsViewModel.resetRequestState()
        }
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.business != null -> {
                BusinessDetailsContent(
                    details = uiState.business!!,
                    reviews = uiState.recentReviews, // Pass reviews
                    requestState = requestState,
                    onRequestClick = {
                        showSchedulingDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun BusinessDetailsContent(
    details: Business,
    reviews: List<Review>,
    requestState: ServiceRequestState,
    onRequestClick: () -> Unit
) {
    LazyColumn {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = details.businessName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                        val formattedRating = DecimalFormat("#.#").format(details.recentAverageRating)
                        Text(
                            text = formattedRating,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                if (!details.managerName.isNullOrBlank()) {
                    Text(text = "Managed by: ${details.managerName}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                BusinessServices(services = details.services)

                Spacer(modifier = Modifier.height(16.dp))
                Divider(modifier = Modifier.padding(vertical = 16.dp))

                RecentReviews(reviews = reviews)
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // These calls are now valid because ContactInfoRow accepts nullable strings
                ContactInfoRow(icon = Icons.Default.Email, text = details.contactEmail)
                Spacer(modifier = Modifier.height(8.dp))
                ContactInfoRow(icon = Icons.Default.Phone, text = details.contactPhone)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request Service")
                }

                if (requestState is ServiceRequestState.Error) {
                    Text(
                        text = requestState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- FIX #2: Changed the parameter 'text' to be a nullable 'String?' and added a safe check ---
@Composable
fun ContactInfoRow(icon: ImageVector, text: String?) {
    // This check ensures the Row is only composed if the text is not null or blank
    if(!text.isNullOrBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            // Inside this block, the compiler knows 'text' is a non-null String
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun BusinessServices(services: List<String>) {
    if (services.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Services Offered:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = services.joinToString(", "),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSchedulingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule a Reservice") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedDate.isBlank()) "Date" else "Date: $selectedDate")
                }
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedTime.isBlank()) "Time" else "Time: $selectedTime")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedDate, selectedTime) },
                enabled = selectedDate.isNotBlank() && selectedTime.isNotBlank()
            ) {
                Text("Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = java.text.SimpleDateFormat("EEE, MMM dd, yyyy", java.util.Locale.getDefault())
                            selectedDate = formatter.format(millis)
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                showTimePicker = false
                val calendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(java.util.Calendar.MINUTE, timePickerState.minute)
                }
                val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                selectedTime = formatter.format(calendar.time)
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun RecentReviews(reviews: List<Review>) {
    if (reviews.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Recent Reviews:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            reviews.forEach { review ->
                ReviewCard(review = review)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${review.rating}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "- ${review.clientName}",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
