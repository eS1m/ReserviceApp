package com.example.firebaseauthtesting

import android.app.Application
import org.osmdroid.config.Configuration
import com.example.firebaseauthtesting.BuildConfig
import java.io.File

class osmDroid : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set the osmdroid configuration
        // This is important to prevent issues with tile caching
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = BuildConfig.APPLICATION_ID
        val baseDir = filesDir
        osmConfig.osmdroidBasePath = File(baseDir, "osmdroid")
        osmConfig.osmdroidTileCache = File(osmConfig.osmdroidBasePath, "tiles")
    }
}