package com.example.firebaseauthtesting.Pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.example.firebaseauthtesting.ViewModels.BusinessViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BusinessRequests(
    businessViewModel: BusinessViewModel = viewModel()
) {
    val requests by businessViewModel.incomingRequests.collectAsState()
    val isLoading by businessViewModel.isLoading.collectAsState()
    val error by businessViewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        businessViewModel.fetchIncomingRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Incoming Service Requests",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "An unknown error occurred.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            } else if (requests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You have no incoming requests.", modifier = Modifier.padding(16.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), // Padding for the list content
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(requests) { request ->
                        BusinessRequestCard(
                            request = request,
                            onAccept = { businessViewModel.updateRequestStatus(request.id, "Accepted") },
                            onDecline = { businessViewModel.updateRequestStatus(request.id, "Declined") },
                            onComplete = { businessViewModel.updateRequestStatus(request.id, "Completed") },
                            onReceivePayment = { businessViewModel.updateRequestStatus(request.id, "Pending Payment") },
                            onConfirmPayment = { businessViewModel.updateRequestStatus(request.id, "Reservice Accomplished!") }
                        )
                    }
                }
            }
        }
    }
}

// The BusinessRequestCard and its helpers remain unchanged as they are perfect.
@Composable
fun BusinessRequestCard(
    request: ServiceRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onComplete: () -> Unit,
    onReceivePayment: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    val requestedDateFormatted = request.timestamp?.let {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(it)
    } ?: "Not available"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Request from: ${request.userName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Requested on: $requestedDateFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            InfoRow(label = "Scheduled for:", value = "${request.scheduledDate} at ${request.scheduledTime}")
            StatusTracker(status = request.status)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (request.status == "Pending") {
                    Button(onClick = onDecline, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFbc4749)))
                    { Text("Decline") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6a994e)))
                    { Text("Accept") }
                }

                if (request.status == "Accepted") {
                    Button(onClick = onReceivePayment, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077b6)))
                    { Text("Receive Payment") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onComplete, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6a994e)))
                    { Text("Mark as Complete") }
                }

                if (request.status == "Confirming Payment") {
                    Button(onClick = onConfirmPayment, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386641)))
                    { Text("Confirm Payment") }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(130.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatusTracker(status: String) {
    val progress = when (status) {
        "Pending" -> 0.2f
        "Accepted" -> 0.4f
        "Pending Payment" -> 0.6f
        "Confirming Payment" -> 0.8f
        "Reservice Accomplished!", "Completed" -> 1.0f
        else -> 0.0f
    }

    val statusColor = when (status) {
        "Accepted" -> Color(0xFF386641)
        "Declined" -> Color(0xFFef233c)
        "Cancelled" -> Color(0xFFd5bdaf)
        "Pending Payment" -> Color(0xFFfb8500)
        "Confirming Payment" -> Color(0xFF0096c7)
        "Reservice Accomplished!" -> Color(0xFFccff33)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when (status) {
        "Declined" -> "Declined by Business"
        "Cancelled" -> "Cancelled by You"
        "Pending Payment" -> "Pending Payment"
        "Confirming Payment" -> "Confirming Payment"
        "Reservice Accomplished!" -> "Reservice Accomplished!"
        else -> status
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Status: $statusText",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = statusColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (status in listOf("Pending", "Accepted", "Completed", "Pending Payment", "Confirming Payment", "Reservice Accomplished!")) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 500),
                label = "StatusProgressAnimation"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}