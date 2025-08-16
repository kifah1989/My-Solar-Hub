package com.example.mysolarhub

import android.Manifest
import android.content.Context
import android.location.Location
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.core.app.ActivityCompat

class WeatherRepository {
    suspend fun fetchWeather(lat: Double, lon: Double): Weather? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val current = json.getJSONObject("current_weather")
                Weather(
                    temperature = current.getDouble("temperature"),
                    windspeed = current.getDouble("windspeed")
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun fetchLocation(context: Context): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        return withContext(Dispatchers.IO) {
            try {
                val fineGranted = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!fineGranted && !coarseGranted) {
                    // Permission not granted
                    return@withContext null
                }
                Tasks.await(
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    )
                )
            } catch (e: SecurityException) {
                // Permission revoked at runtime
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}
