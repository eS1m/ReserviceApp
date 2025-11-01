package com.example.firebaseauthtesting.Pages

import android.content.Context
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessMapScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    mapViewModel: BusinessMapViewModel = viewModel()
) {
    val uiState by mapViewModel.uiState.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Businesses Near You", color = Color.White) },
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
                        businesses = state.businesses,
                        userProfile = userProfile
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
    userProfile: UserProfile?
) {
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
                    title = business.fullName

                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}