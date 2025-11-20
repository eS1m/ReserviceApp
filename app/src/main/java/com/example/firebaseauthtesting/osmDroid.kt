package com.example.firebaseauthtesting

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.osmdroid.config.Configuration
import com.example.firebaseauthtesting.BuildConfig
import java.io.File

class osmDroid : Application() {
    override fun onCreate() {
        super.onCreate()

        val osmConfig = Configuration.getInstance()
        osmConfig.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        osmConfig.userAgentValue = BuildConfig.APPLICATION_ID

        val baseDir = filesDir
        osmConfig.osmdroidBasePath = File(baseDir, "osmdroid")
        osmConfig.osmdroidTileCache = File(osmConfig.osmdroidBasePath, "tiles")

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
}
