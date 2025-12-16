package com.example.firebaseauthtesting.Pages

import AddReviewDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Money
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.ViewModels.ProfileRequestsUiState
import com.example.firebaseauthtesting.ViewModels.ProfileRequestsViewModel
import com.example.firebaseauthtesting.viewModel.AddReviewViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileRequests(
    viewModel: ProfileRequestsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    addReviewViewModel: AddReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showDialog by viewModel.showPaymentDialog.collectAsState()

    val fullName by authViewModel.fullName.collectAsState()

    val showReviewDialog by viewModel.showReviewDialog.collectAsState()
    val requestToReview by viewModel.requestToReview.collectAsState()
    val rating by viewModel.reviewRating.collectAsState()
    val comment by viewModel.reviewComment.collectAsState()
    val reviewSubmissionState by addReviewViewModel.submissionState.collectAsState()

    if (showReviewDialog && requestToReview != null) {
        AddReviewDialog(
            rating = rating,
            comment = comment,
            onRatingChange = viewModel::onRatingChange,
            onCommentChange = viewModel::onCommentChange,
            onDismissRequest = {
                viewModel.onDismissReviewDialog()
                addReviewViewModel.resetState() // Reset submission state on dismiss
            },
            onReviewSubmit = { submittedRating, submittedComment ->
                val currentUser = authViewModel.currentUser.value
                // Ensure all data is available before submitting
                if (currentUser != null && fullName != null && requestToReview != null) {
                    addReviewViewModel.submitReview(
                        businessId = requestToReview!!.businessId,
                        businessName = requestToReview!!.businessName,
                        clientId = currentUser.uid,
                        clientName = fullName!!,
                        rating = submittedRating,
                        comment = submittedComment,
                        serviceRequestId = requestToReview!!.id
                    )
                }
            }
        )
    }

    LaunchedEffect(reviewSubmissionState) {
        if (reviewSubmissionState is AddReviewViewModel.ReviewSubmissionState.Success) {
            viewModel.onDismissReviewDialog()
            addReviewViewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchSentRequests()
    }

    if (showDialog) {
        PaymentMethodDialog(
            onDismiss = { viewModel.dismissPaymentDialog() },
            onPaymentSelected = { method ->
                viewModel.setPaymentMethodAndProceed(method)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "My Service Requests",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            when (val state = uiState) {
                is ProfileRequestsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileRequestsUiState.Success -> {
                    if (state.sentRequests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("You have not made any requests.", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.sentRequests) { request ->
                                UserRequestCard(
                                    request = request,
                                    onCancel = { viewModel.cancelRequest(request.id) },
                                    onPay = { viewModel.checkAndInitiatePayment(request.id) },
                                    onReview = { viewModel.onReviewClick(request) }

                                )
                            }
                        }
                    }
                }
                is ProfileRequestsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodDialog(
    onDismiss: () -> Unit,
    onPaymentSelected: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Choose Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .clickable { onPaymentSelected("Cash") }
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Money,
                        contentDescription = "Cash Icon",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Cash", style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}


@Composable
fun UserRequestCard(
    request: ServiceRequest,
    onCancel: () -> Unit,
    onPay: () -> Unit,
    onReview: () -> Unit
) {
    val requestedDateFormatted = request.timestamp?.let {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(it)
    } ?: "Not available"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = request.businessName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (request.service.isNotBlank()) {
                Text(
                    text = "Service: ${request.service}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            request.amount?.let {
                Text(
                    text = "Amount: $${String.format("%.2f", it)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

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
                horizontalArrangement = Arrangement.End
            ) {
                when (request.status) {
                    "Pending" -> {
                        OutlinedButton(onClick = onCancel) {
                            Text("Cancel Request")
                        }
                    }
                    "Pending Payment" -> {
                        Button(
                            onClick = onPay,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077b6))
                        ) {
                            Text("Pay for Service")
                        }
                    }
                    "Reservice Accomplished!" -> {
                        Button(
                            onClick = onReview, // Use the new onReview handler
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386641)) // Green color
                        ) {
                            Text("Leave a Review")
                        }
                    }
                    else -> {}
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

