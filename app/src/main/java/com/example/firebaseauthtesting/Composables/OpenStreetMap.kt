package com.example.firebaseauthtesting.Composables

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
// FIX 1: Import necessary classes for resizing the drawable
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.example.firebaseauthtesting.Models.Business
import com.example.firebaseauthtesting.R
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OpenStreetMap(
    modifier: Modifier = Modifier,
    businesses: List<Business>,
    onMarkerClick: (Business) -> Unit,
    startPoint: GeoPoint
) {
    val context = LocalContext.current
    val resources = context.resources

    val markerIcon: Drawable? = remember {
        val originalDrawable = ContextCompat.getDrawable(context, R.drawable.marker)
        if (originalDrawable != null) {
            val width = 48
            val height = 48

            val bitmap = (originalDrawable as BitmapDrawable).bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
            BitmapDrawable(resources, scaledBitmap)
        } else {
            null // Return null if the original drawable couldn't be loaded
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            Configuration.getInstance().load(it, it.getSharedPreferences("osmdroid", 0))
            MapView(it).apply {
                setMultiTouchControls(true)
                setBuiltInZoomControls(true)
                controller.setZoom(14.0)
                controller.setCenter(startPoint)
            }
        },
        update = { view ->
            view.overlays.removeAll { it is Marker }

            businesses.forEach { business ->
                business.location?.let { firestoreGeoPoint ->
                    val marker = Marker(view).apply {
                        this.position = GeoPoint(firestoreGeoPoint.latitude, firestoreGeoPoint.longitude)
                        this.title = business.businessName
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        this.icon = markerIcon
                        this.setOnMarkerClickListener { _, _ ->
                            onMarkerClick(business)
                            true
                        }
                    }
                    view.overlays.add(marker)
                }
            }
            view.invalidate()
        }
    )
}
