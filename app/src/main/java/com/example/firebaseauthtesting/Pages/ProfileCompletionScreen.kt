package com.example.firebaseauthtesting.Pages

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.firebaseauthtesting.R
import com.example.firebaseauthtesting.Screen
import com.example.firebaseauthtesting.ViewModels.ProfileCompletionViewModel
import com.example.firebaseauthtesting.ViewModels.SaveStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileCompletionScreen(
    navController: NavController,
    fullName: String, // This is passed from Signup, but we'll use a local state for the UI
    email: String,
    password: String,
    viewModel: ProfileCompletionViewModel = viewModel()
) {
    val context = LocalContext.current
    val saveStatus by viewModel.saveStatus.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var localFullName by remember { mutableStateOf(fullName) }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var initialMapCenter by remember { mutableStateOf(GeoPoint(4.2105, 101.9758)) } // Default
    var isBusiness by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Request permission on launch
    LaunchedEffect(locationPermissionState) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // When permission is granted, get location
    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    initialMapCenter = GeoPoint(it.latitude, it.longitude)
                }
            }
        }
    }

    // Navigation logic on successful save
    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is SaveStatus.Success -> {
                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
            is SaveStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                viewModel.resetSaveStatus()
            }
            is SaveStatus.Idle -> {}
        }
    }

    val interphasesFamily = FontFamily(Font(R.font.interphases))
    val gradientColors = listOf(Color(0xFF354f52), Color(0xFF84a98c))

    Scaffold(
        modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(colors = gradientColors)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Complete your profile",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = localFullName,
                onValueChange = { localFullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Register as a Business/Service?", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Switch(
                    checked = isBusiness,
                    onCheckedChange = { isBusiness = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF84a98c),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Your Location", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            MiniMapView(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                initialCenter = initialMapCenter,
                onLocationSelected = { geoPoint -> selectedLocation = geoPoint }
            )
            Spacer(modifier = Modifier.height(16.dp))

            val isFormComplete = localFullName.isNotBlank() && phoneNumber.isNotBlank() && selectedLocation != null
            Button(
                onClick = {
                    selectedLocation?.let { location ->
                        // Call the correct ViewModel function with all the data
                        viewModel.createAccountAndProfile(
                            email = email,
                            password = password,
                            fullName = localFullName,
                            phoneNumber = phoneNumber,
                            location = location,
                            isBusiness = isBusiness
                        )
                    }
                },
                enabled = isFormComplete && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Account & Save Profile")
                }
            }
        }
    }
}

@Composable
fun MiniMapView(
    modifier: Modifier = Modifier,
    initialCenter: GeoPoint,
    onLocationSelected: (GeoPoint) -> Unit
) {
    var selectedMarker: Marker? by remember { mutableStateOf(null) }

    Card(modifier = modifier, elevation = CardDefaults.cardElevation(4.dp)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(initialCenter)

                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let { point ->
                                selectedMarker?.let { overlays.remove(it) }

                                val newMarker = Marker(this@apply).apply {
                                    position = point
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                overlays.add(newMarker)
                                invalidate()

                                selectedMarker = newMarker
                                onLocationSelected(point)
                            }
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint?) = false
                    })
                    overlays.add(eventsOverlay)
                }
            },
            update = { mapView ->
                // This will smoothly move the map when the user's location is found
                if (mapView.mapCenter != initialCenter) {
                    mapView.controller.animateTo(initialCenter)
                }
            }
        )
    }
}
