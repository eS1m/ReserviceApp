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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.ViewModels.BusinessDetailsViewModel
import com.example.firebaseauthtesting.Models.Business
import com.example.firebaseauthtesting.ViewModels.ServiceRequestState
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDetailsSheet(
    businessId: String,
    detailsViewModel: BusinessDetailsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val selectedBusiness by detailsViewModel.selectedBusiness.collectAsState()
    val requestState by detailsViewModel.requestState.collectAsState()
    val fullName by authViewModel.fullName.collectAsStateWithLifecycle()
    var showSchedulingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(businessId) {
        if (businessId.isNotBlank()) {
            detailsViewModel.selectBusiness(businessId)
        }
    }

    if (showSchedulingDialog) {
        selectedBusiness?.let { business ->
            SchedulingDialog(
                onDismiss = { showSchedulingDialog = false },
                onConfirm = { date, time ->
                    detailsViewModel.createServiceRequest(
                        businessId = business.uid,
                        businessName = business.businessName,
                        userName = fullName ?: "Anonymous",
                        scheduledDate = date,
                        scheduledTime = time
                    )
                    showSchedulingDialog = false
                }
            )
        }
    }

    LaunchedEffect(requestState) {
        if (requestState is ServiceRequestState.Success) {
            detailsViewModel.resetRequestState()
        }
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        when {
            selectedBusiness == null && businessId.isNotBlank() -> {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            selectedBusiness != null -> {
                BusinessDetailsContent(
                    details = selectedBusiness!!,
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
                            tint = Color(0xFFFFC107), // Amber color for the star
                            modifier = Modifier.size(20.dp)
                        )
                        // Format the rating to one decimal place
                        val formattedRating = DecimalFormat("#.#").format(details.rating)
                        Text(
                            text = formattedRating,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                if (details.managerName.isNotBlank()) {
                    Text(text = "Managed by: ${details.managerName}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // FIX 1: Instead of duplicating logic, we now call the dedicated BusinessServices composable.
                BusinessServices(services = details.services)

                Spacer(modifier = Modifier.height(16.dp))
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

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

@Composable
fun ContactInfoRow(icon: ImageVector, text: String) {
    // This check correctly hides the row if the contact info is blank, cleaning up the UI.
    if(text.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun BusinessServices(services: List<String>) {
    if (services.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth() // Removed unnecessary padding to align with other content
        ) {
            Text(
                text = "Services Offered:", // Changed text to be consistent
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = services.joinToString(", "),
                style = MaterialTheme.typography.bodyLarge // Matched style for consistency
            )
        }
    }
}
