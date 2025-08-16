package com.example.mysolarhub

import android.app.Application
import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WeatherViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = WeatherRepository()
    val weather = mutableStateOf<Weather?>(null)
    val error = mutableStateOf<String?>(null)
    val location = mutableStateOf<Location?>(null)
    val loading = mutableStateOf(false)
    val permissionGranted = mutableStateOf(false)

    fun checkPermission(granted: Boolean) {
        permissionGranted.value = granted
        if (granted) error.value = null
        else error.value = "Location permission not granted."
    }

    fun fetchWeatherAndLocation() {
        if (!permissionGranted.value) return
        loading.value = true
        viewModelScope.launch {
            val loc = repository.fetchLocation(getApplication())
            if (loc != null) {
                location.value = loc
                weather.value = repository.fetchWeather(loc.latitude, loc.longitude)
                error.value = null
            } else {
                error.value = "Failed to get location."
            }
            loading.value = false
        }
    }

    fun retry() {
        fetchWeatherAndLocation()
    }
}

