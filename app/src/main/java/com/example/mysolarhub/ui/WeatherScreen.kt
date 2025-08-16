package com.example.mysolarhub.ui

import android.location.Location
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mysolarhub.Weather
import com.example.mysolarhub.R

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

@Composable
fun WeatherLocationView(
    weather: Weather?,
    location: Location?,
    error: String?,
    loading: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFBDE0FE), Color(0xFFB5838D))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = loading) { isLoading ->
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF6D6875))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.loading_weather), color = Color(0xFF6D6875))
                    }
                }
                error == stringResource(R.string.failed_location) -> {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF08080))) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(error ?: "", color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFFF08080))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.retry), color = Color(0xFFF08080))
                            }
                        }
                    }
                }
                error != null -> {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF08080))) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(error, color = Color.White)
                        }
                    }
                }
                weather != null && location != null -> {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFBDE0FE)),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB4A2), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(
                                    R.string.weather_at,
                                    "%.2f".format(location.latitude),
                                    "%.2f".format(location.longitude),
                                    weather.temperature,
                                    weather.windspeed
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF6D6875)
                            )
                        }
                    }
                }
                else -> {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFBDE0FE))) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB4A2), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.weather_default), color = Color(0xFF6D6875))
                        }
                    }
                }
            }
        }
    }
}
