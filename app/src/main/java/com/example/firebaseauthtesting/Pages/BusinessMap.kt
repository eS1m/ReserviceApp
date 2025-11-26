package com.example.firebaseauthtesting.Pages

import android.Manifest
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebaseauthtesting.Composables.OpenStreetMap
import com.example.firebaseauthtesting.Models.Business
import com.example.firebaseauthtesting.ViewModels.BusinessMapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import org.osmdroid.util.GeoPoint
import com.example.firebaseauthtesting.Pages.BusinessDetailsSheet

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BusinessMapScreen(
    navController: NavController,
    serviceCategory: String,
    customServiceName: String?,
    mapViewModel: BusinessMapViewModel = viewModel()
) {
    val businesses by mapViewModel.businesses.collectAsStateWithLifecycle()
    val isLoading by mapViewModel.isLoading.collectAsStateWithLifecycle()
    val error by mapViewModel.error.collectAsStateWithLifecycle()

    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var selectedBusiness by remember { mutableStateOf<Business?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    RememberDeviceLocation { location ->
        userLocation = location
    }

    LaunchedEffect(serviceCategory) {
        if (serviceCategory.isNotBlank()) {
            mapViewModel.fetchBusinesses(serviceCategory)
        }
    }

    val mapBusinesses = remember(businesses) {
        businesses.mapNotNull { business ->
            business.location?.let { firestoreGeoPoint ->
                Pair(
                    GeoPoint(firestoreGeoPoint.latitude, firestoreGeoPoint.longitude),
                    business
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$serviceCategory Near You") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading || userLocation == null -> {
                    CircularProgressIndicator()
                }
                error != null -> {
                    Text(text = "Error: $error")
                }
                else -> {
                    OpenStreetMap(
                        modifier = Modifier.fillMaxSize(),
                        businesses = businesses,
                        onMarkerClick = { business ->
                            selectedBusiness = business
                            showSheet = true
                        },
                        startPoint = userLocation ?: GeoPoint(40.7128, -74.0060)
                    )
                }
            }
        }
    }

    if (showSheet && selectedBusiness != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            BusinessDetailsSheet(
                businessId = selectedBusiness!!.uid,
                serviceCategory = serviceCategory,
                customServiceName = customServiceName
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RememberDeviceLocation(onLocationGranted: (GeoPoint) -> Unit) {
    val context = LocalContext.current
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // Trigger permission request if not granted.
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Fetch location once permissions are granted.
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        onLocationGranted(GeoPoint(location.latitude, location.longitude))
                    } else {
                        onLocationGranted(GeoPoint(40.7128, -74.0060)) // Fallback
                    }
                }.addOnFailureListener {
                    onLocationGranted(GeoPoint(40.7128, -74.0060)) // Fallback on error
                }
            } catch (e: SecurityException) {
                // This should not happen if permissions are granted, but it's a safe catch.
                Log.e("Location", "Location permission check failed.", e)
            }
        }
    }
}
