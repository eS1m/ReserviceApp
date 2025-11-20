package com.example.firebaseauthtesting.Pages

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebaseauthtesting.Composables.OpenStreetMap
import com.example.firebaseauthtesting.ViewModels.BusinessMapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ServiceMapScreen(
    navController: NavController,
    serviceCategory: String,
    mapViewModel: BusinessMapViewModel = viewModel()
) {
    var showSheet by remember { mutableStateOf(false) }
    var selectedBusinessId by remember { mutableStateOf<String?>(null) }

    // Collect all necessary states from the ViewModel
    val businesses by mapViewModel.businesses.collectAsStateWithLifecycle()
    val isLoading by mapViewModel.isLoading.collectAsStateWithLifecycle()
    val error by mapViewModel.error.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(serviceCategory) {
        locationPermissions.launchMultiplePermissionRequest()
        mapViewModel.fetchBusinesses(serviceCategory)
    }

    Scaffold { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {

            OpenStreetMap(
                modifier = Modifier.fillMaxSize(),
                businesses = businesses, // This will now contain the filtered list!
                onMarkerClick = { business ->
                    selectedBusinessId = business.uid
                    showSheet = true
                },
                startPoint = GeoPoint(40.7128, -74.0060) // Default start point
            )

            if (isLoading) {
                CircularProgressIndicator()
            }

            error?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            selectedBusinessId?.let { businessId ->
                BusinessDetailsSheet(
                    businessId = businessId
                )
            }
        }
    }
}
