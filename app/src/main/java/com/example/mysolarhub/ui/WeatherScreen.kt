package com.example.mysolarhub.ui

import android.location.Location
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mysolarhub.Weather

@Composable
fun PermissionRequestView(error: String?, onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(error ?: "Please grant location permission")
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
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
    when {
        loading -> Text("Loading weather...", modifier = modifier)
        error == "Failed to get location." -> {
            Column(modifier = modifier) {
                Text(error)
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
        error != null -> Text(error, modifier = modifier)
        weather != null && location != null -> Text("Weather at (${"%.2f".format(location.latitude)}, ${"%.2f".format(location.longitude)}): ${weather.temperature}°C, Wind: ${weather.windspeed} m/s", modifier = modifier)
        else -> Text("Weather: --", modifier = modifier)
    }
}

