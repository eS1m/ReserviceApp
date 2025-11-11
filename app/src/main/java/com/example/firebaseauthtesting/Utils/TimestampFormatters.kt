package com.example.firebaseauthtesting.Utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale


fun formatScheduledTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:00 a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}