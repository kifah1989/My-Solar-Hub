package com.example.mysolarhub

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysolarhub.ui.WeatherLocationView
import com.example.mysolarhub.ui.PermissionRequestView
import com.example.mysolarhub.ui.theme.MySolarHubTheme
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    private val TAG = "WeatherApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MySolarHubTheme {
                val viewModel: WeatherViewModel = viewModel()
                val weather by viewModel.weather
                val error by viewModel.error
                val location by viewModel.location
                val loading by viewModel.loading
                val permissionGranted by viewModel.permissionGranted

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    viewModel.checkPermission(isGranted)
                    if (isGranted) {
                        Log.d(TAG, "Permission granted")
                        viewModel.fetchWeatherAndLocation()
                    } else {
                        Log.d(TAG, "Permission denied")
                    }
                }

                // Initial permission check
                LaunchedEffect(Unit) {
                    val fineGranted = ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val coarseGranted = ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    viewModel.checkPermission(fineGranted || coarseGranted)
                    if (fineGranted || coarseGranted) {
                        viewModel.fetchWeatherAndLocation()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!permissionGranted) {
                        PermissionRequestView(
                            error,
                            onRequestPermission = {
                                Log.d(TAG, "Requesting permission via button")
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        WeatherLocationView(
                            weather,
                            location,
                            error,
                            loading,
                            onRetry = {
                                Log.d(TAG, "Retry button pressed")
                                viewModel.retry()
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
