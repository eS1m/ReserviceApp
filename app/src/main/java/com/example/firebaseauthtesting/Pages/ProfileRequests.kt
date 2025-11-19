package com.example.firebaseauthtesting.Pages

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.example.firebaseauthtesting.ViewModels.ProfileRequestsUiState
import com.example.firebaseauthtesting.ViewModels.ProfileRequestsViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Money
import androidx.compose.ui.platform.LocalContext
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.Utils.formatScheduledTimestamp
import com.example.firebaseauthtesting.Utils.formatTimestamp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRequestsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    requestsViewModel: ProfileRequestsViewModel = viewModel(),
) {
    val uiState by requestsViewModel.uiState.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val context = LocalContext.current

    val showPaymentDialog = uiState is ProfileRequestsUiState.NeedsPaymentMethodSetup

    if (showPaymentDialog) {
        PaymentMethodDialog(
            onDismiss = {
                // Simply call the new function in the ViewModel
                requestsViewModel.dismissPaymentDialog()
            },
            onSave = { method ->
                requestsViewModel.savePaymentMethod(method) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Sent Requests", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1b4332))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileRequestsUiState.Loading -> CircularProgressIndicator()
                is ProfileRequestsUiState.Error -> Text(text = state.message, color = Color.Red)
                is ProfileRequestsUiState.Success -> {
                    if (state.requests.isEmpty()) {
                        Text("You have not made any requests.", color = Color.Gray)
                    } else {
                        SentRequestList(
                            requests = state.requests,
                            requestsViewModel = requestsViewModel
                        )
                    }
                }
                is ProfileRequestsUiState.NeedsPaymentMethodSetup -> {
                    val requests = (uiState as? ProfileRequestsUiState.Success)?.requests
                    if (!requests.isNullOrEmpty()) {
                        SentRequestList(requests = requests, requestsViewModel = requestsViewModel)
                    } else {
                        Text("You have not made any requests.", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestList(
    requests: List<ServiceRequest>,
    requestsViewModel: ProfileRequestsViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            SentRequestItemCard(
                request = request,
                requestsViewModel = requestsViewModel
            )
        }
    }
}

    @Composable
    fun SentRequestItemCard(
        request: ServiceRequest,
        requestsViewModel: ProfileRequestsViewModel
    ) {
        val statusColor = when (request.status) {
            "Accepted" -> Color(0xFF2E7D32)
            "Declined" -> Color(0xFFC62828)
            "Paid" -> Color(0xFF1565C0)
            "Cancelled" -> Color(0xFFdad7cd)
            else -> Color.Gray
        }

        val context = LocalContext.current

        Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Request for: ${request.serviceCategory}",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))

                request.scheduledDateTime?.let { scheduledTime ->
                    Text(
                        text = "Scheduled for: ${formatScheduledTimestamp(scheduledTime)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    text = "Status: ${request.status}",
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Sent: ${formatTimestamp(request.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (request.status.equals("Accepted", ignoreCase = true)) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            requestsViewModel.initiatePayment()
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Pay Now")
                    }
                }

                if (request.status.equals("pending", ignoreCase = true)) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            requestsViewModel.cancelRequest(request.requestId) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel Request")
                    }
                }


            }
        }
    }

@Composable
fun PaymentMethodDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Up Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please select a payment method to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onSave("Cash")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Money, contentDescription = "Cash Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay with Cash")
                }
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}