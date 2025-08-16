package com.example.mysolarhub.repository

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
import com.example.mysolarhub.Weather
import com.example.mysolarhub.data.SolarIrradiance

class SolarWeatherRepository {

    suspend fun fetchWeatherAndSolarData(lat: Double, lon: Double): WeatherSolarData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildWeatherSolarUrl(lat, lon)
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = connection.inputStream.bufferedReader().readText()
                parseWeatherSolarResponse(response)
            } catch (e: Exception) {
                e.printStackTrace()
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
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val coarseGranted = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (!fineGranted && !coarseGranted) {
                    return@withContext null
                }

                Tasks.await(
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    )
                )
            } catch (@Suppress("UNUSED_PARAMETER") e: SecurityException) {
                null
            } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
                null
            }
        }
    }

    private fun buildWeatherSolarUrl(lat: Double, lon: Double): String {
        return "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&current_weather=true" +
                "&hourly=temperature_2m,shortwave_radiation,direct_radiation,diffuse_radiation" +
                "&forecast_days=1" +
                "&timezone=auto"
    }

    private fun parseWeatherSolarResponse(response: String): WeatherSolarData? {
        return try {
            val json = JSONObject(response)

            // Current weather
            val current = json.getJSONObject("current_weather")
            val weather = Weather(
                temperature = current.getDouble("temperature"),
                windspeed = current.getDouble("windspeed")
            )

            // Hourly data
            val hourly = json.getJSONObject("hourly")
            val temperatures = hourly.getJSONArray("temperature_2m")
            val shortwaveRadiation = hourly.getJSONArray("shortwave_radiation")
            val directRadiation = hourly.getJSONArray("direct_radiation")
            val diffuseRadiation = hourly.getJSONArray("diffuse_radiation")

            val hourlyTemperatures = mutableListOf<Double>()
            val irradianceData = mutableListOf<SolarIrradiance>()

            // Take first 24 hours (today)
            for (i in 0 until minOf(24, temperatures.length())) {
                hourlyTemperatures.add(
                    if (temperatures.isNull(i)) 20.0 else temperatures.getDouble(i)
                )

                val ghi = if (shortwaveRadiation.isNull(i)) 0.0 else shortwaveRadiation.getDouble(i)
                val dni = if (directRadiation.isNull(i)) 0.0 else directRadiation.getDouble(i)
                val dhi = if (diffuseRadiation.isNull(i)) 0.0 else diffuseRadiation.getDouble(i)

                irradianceData.add(SolarIrradiance(i, ghi, dni, dhi))
            }

            WeatherSolarData(weather, hourlyTemperatures, irradianceData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class WeatherSolarData(
    val weather: Weather,
    val hourlyTemperatures: List<Double>,
    val irradianceData: List<SolarIrradiance>
)
