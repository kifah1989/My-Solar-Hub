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
import com.example.mysolarhub.ui.PermissionRequestView
import com.example.mysolarhub.ui.screens.SystemConfigScreen
import com.example.mysolarhub.ui.screens.LoadProfileScreen
import com.example.mysolarhub.ui.screens.DashboardScreen
import com.example.mysolarhub.ui.theme.MySolarHubTheme
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    private val TAG = "SolarHubApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MySolarHubTheme {
                val viewModel: SolarHubViewModel = viewModel()
                val permissionGranted by viewModel.permissionGranted
                val error by viewModel.error
                val loading by viewModel.loading
                val solarSystemConfig by viewModel.solarSystemConfig
                val homeLoadProfile by viewModel.homeLoadProfile
                val dailyForecast by viewModel.dailyForecast

                val navController = rememberNavController()

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
                        NavHost(
                            navController = navController,
                            startDestination = "system_config",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("system_config") {
                                SystemConfigScreen(
                                    config = solarSystemConfig,
                                    onConfigUpdate = viewModel::updateSolarSystemConfig,
                                    onNext = { navController.navigate("load_profile") },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            composable("load_profile") {
                                LoadProfileScreen(
                                    loadProfile = homeLoadProfile,
                                    onProfileUpdate = viewModel::updateHomeLoadProfile,
                                    onAddAppliance = viewModel::addApplianceLoad,
                                    onRemoveAppliance = viewModel::removeApplianceLoad,
                                    onNext = { navController.navigate("dashboard") },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            composable("dashboard") {
                                DashboardScreen(
                                    forecast = dailyForecast,
                                    onRefresh = { viewModel.retry() },
                                    onBackToConfig = { navController.popBackStack("system_config", false) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
