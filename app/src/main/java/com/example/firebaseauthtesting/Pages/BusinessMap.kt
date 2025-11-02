package com.example.firebaseauthtesting.Pages

import com.example.firebaseauthtesting.R
import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.ViewModels.BusinessMapViewModel
import com.example.firebaseauthtesting.ViewModels.BusinessMarker
import com.example.firebaseauthtesting.ViewModels.MapUiState
import com.example.firebaseauthtesting.ViewModels.UserProfile
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import android.widget.Button
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.firebaseauthtesting.ViewModels.RequestsViewModel
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessMapScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    mapViewModel: BusinessMapViewModel = viewModel(),
    requestViewModel: RequestsViewModel = viewModel(),
    serviceCategory: String
) {
    val uiState by mapViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val userProfile by authViewModel.userProfile.collectAsState()

    LaunchedEffect(serviceCategory) {
        mapViewModel.fetchBusinessesByService(serviceCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$serviceCategory Near You", color = Color.White) },
                navigationIcon = {

                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1b4332)
                )
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
                is MapUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is MapUiState.Success -> {
                    MapViewContainer(
                        modifier = Modifier.padding(innerPadding),
                        businesses = state.businesses,
                        userProfile = userProfile,
                        serviceCategory = serviceCategory,
                        mapViewModel = mapViewModel,
                        navController = navController,
                        onResult = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                is MapUiState.Error -> {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun MapViewContainer(
    modifier: Modifier = Modifier,
    businesses: List<BusinessMarker>,
    userProfile: UserProfile?,
    serviceCategory: String,
    mapViewModel: BusinessMapViewModel,
    navController: NavController,
    onResult: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedBusiness by remember { mutableStateOf<BusinessMarker?>(null) }

    if (showDialog && selectedBusiness != null) {
        val business = selectedBusiness!!
        ScheduleRequestDialog(
            onDismiss = { showDialog = false },
            onConfirm = { scheduledDateTime ->
                mapViewModel.createServiceRequest(
                    businessId = business.uid,
                    serviceCategory = serviceCategory,
                    scheduledDateTime = scheduledDateTime
                ) { success, message ->

                    onResult(message)

                    if (success) {
                        navController.navigate("home") {

                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
                showDialog = false
            }
        )
    }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                val startCenter = if (userProfile != null) {
                    GeoPoint(userProfile.location.latitude, userProfile.location.longitude)
                } else {
                    GeoPoint(40.0, -95.0)
                }
                controller.setZoom(12.0)
                controller.setCenter(startCenter)
            }
        },
        update = { mapView ->
            mapView.overlays.removeAll { it is Marker }

            businesses.forEach { business ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(business.location.latitude, business.location.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }

                marker.infoWindow = object : InfoWindow(R.layout.layout_info_window, mapView) {
                    override fun onOpen(item: Any?) {
                        val titleView: TextView = mView.findViewById(R.id.bubble_title)
                        val descriptionView: TextView = mView.findViewById(R.id.bubble_description)
                        val requestButton: Button = mView.findViewById(R.id.bubble_request_button)

                        titleView.text = business.fullName
                        descriptionView.text = "Services: ${business.services.joinToString(", ")}"

                        requestButton.setOnClickListener {
                            selectedBusiness = business
                            showDialog = true
                            close()
                        }
                    }
                    override fun onClose() { /* No action needed */ }
                }

                marker.setOnMarkerClickListener { clickedMarker, _ ->
                    if (clickedMarker.isInfoWindowShown) {
                        clickedMarker.closeInfoWindow()
                    } else {
                        clickedMarker.showInfoWindow()
                    }
                    true
                }


                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}

@Composable
fun ScheduleRequestDialog(
    onDismiss: () -> Unit,
    onConfirm: (Timestamp) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(-1) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, _: Int ->
            selectedHour = hour

        },
        calendar.get(Calendar.HOUR_OF_DAY),
        0,
        false
    )

    // --- Date Picker Dialog ---
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            timePickerDialog.show()
        },
        selectedYear,
        selectedMonth,
        selectedDay
    )
    datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Schedule Service") },
        text = {
            Column {
                Text("Please select a date and time for the service.")
                Spacer(Modifier.height(16.dp))
                val buttonText = if (selectedHour != -1) {
                    val tempCal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay, selectedHour, 0) }
                    SimpleDateFormat("MMM dd, hh:00 a", Locale.getDefault()).format(tempCal.time)
                } else {
                    "Click to pick a Date & Time"
                }

                androidx.compose.material3.Button(onClick = { datePickerDialog.show() }) {
                    Text(buttonText)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalCalendar = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay, selectedHour, 0, 0)
                    }
                    onConfirm(Timestamp(finalCalendar.time))
                },
                enabled = selectedHour != -1
            ) {
                Text("Confirm Request")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}