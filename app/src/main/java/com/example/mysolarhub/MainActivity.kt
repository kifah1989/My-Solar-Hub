package com.example.mysolarhub

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    @Composable
    fun PermissionRequestView(error: String?, onRequestPermission: () -> Unit, modifier: Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAE1DD))
            ) {
                Column (
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFFB5838D), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(error ?: stringResource(R.string.please_grant_location), color = Color(0xFF6D6875))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB5838D))) {
                        Text(stringResource(R.string.grant_permission), color = Color.White)
                    }
                }
            }
        }
    }

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
