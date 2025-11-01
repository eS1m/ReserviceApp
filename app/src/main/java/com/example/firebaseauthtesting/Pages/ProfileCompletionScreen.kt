package com.example.firebaseauthtesting.Pages

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import com.example.firebaseauthtesting.R
import com.example.firebaseauthtesting.ViewModels.AuthState
import com.example.firebaseauthtesting.ViewModels.ProfileCompletionViewModel
import com.example.firebaseauthtesting.ViewModels.ProfileUpdateState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import com.google.accompanist.permissions.isGranted
import com.example.firebaseauthtesting.ViewModels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileCompletionScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    email: String,
    password: String
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var initialLocation by remember { mutableStateOf(GeoPoint(51.5074, -0.1278)) }
    var isBusiness by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(locationPermissionState) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            // Suppressing the missing permission check because we just confirmed it's granted.
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // Update the initial location state for the map
                    initialLocation = GeoPoint(it.latitude, it.longitude)
                }
            }
        }
    }

    val interphasesFamily = FontFamily(
        Font(com.example.firebaseauthtesting.R.font.interphases)
    )
    val pantonFamily = FontFamily(
        Font(R.font.panton)
    )
    val gradientColors = listOf(
        Color(0xFF354f52),
        Color(0xFF84a98c)
    )


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            ),
        topBar = {
            TopAppBar(
                title = {
                    Text("Complete your profile",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp))
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
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
                Text(
                    text = "Register as a Business/Service?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
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

            // Mini-map for location selection
            Text("Select Your Location", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(8.dp))
            MiniMapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                initialCenter = initialLocation,
                onLocationSelected = { geoPoint ->
                    selectedLocation = geoPoint
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            val isFormComplete = fullName.isNotBlank() && phoneNumber.isNotBlank() && selectedLocation != null
            Button(
                onClick = {
                    selectedLocation?.let { location ->
                        val firebaseGeoPoint = com.google.firebase.firestore.GeoPoint(location.latitude, location.longitude)
                        authViewModel.createUserWithProfile(
                            email = email,
                            password = password,
                            fullName = fullName,
                            phoneNumber = phoneNumber,
                            location = firebaseGeoPoint,
                            isBusiness = isBusiness
                        )
                    }
                },
                enabled = isFormComplete && authState !is AuthState.Loading, // Check the correct state
                modifier = Modifier.fillMaxWidth()
            ) {
                if (authState is AuthState.Loading) { // Check the correct state
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
    onLocationSelected: (GeoPoint) -> Unit) {
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

                                val marker = Marker(this@apply).apply {
                                    position = point
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                overlays.add(marker)
                                invalidate() // Redraw the map

                                selectedMarker = marker
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
                mapView.controller.animateTo(initialCenter)
            }
        )
    }
}