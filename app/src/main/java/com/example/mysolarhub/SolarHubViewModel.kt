package com.example.mysolarhub

import android.app.Application
import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.mysolarhub.data.*
import com.example.mysolarhub.calculations.SolarCalculationEngine
import com.example.mysolarhub.repository.SolarWeatherRepository

class SolarHubViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SolarWeatherRepository()
    private val calculationEngine = SolarCalculationEngine()

    // System configuration states
    val solarSystemConfig = mutableStateOf(SolarSystemConfig())
    val homeLoadProfile = mutableStateOf(HomeLoadProfile(LoadProfileType.SIMPLE_AVERAGE))

    // Weather and forecast states
    val weather = mutableStateOf<Weather?>(null)
    val location = mutableStateOf<Location?>(null)
    val dailyForecast = mutableStateOf<DailyEnergyForecast?>(null)

    // UI states
    val error = mutableStateOf<String?>(null)
    val loading = mutableStateOf(false)
    val permissionGranted = mutableStateOf(false)
    val currentScreen = mutableStateOf(SolarHubScreen.SYSTEM_CONFIG)

    fun checkPermission(granted: Boolean) {
        permissionGranted.value = granted
        if (granted) {
            error.value = null
        } else {
            error.value = getApplication<Application>().getString(R.string.location_permission_not_granted)
        }
    }

    fun fetchWeatherAndLocation() {
        if (!permissionGranted.value) return
        loading.value = true
        viewModelScope.launch {
            try {
                val loc = repository.fetchLocation(getApplication())
                if (loc != null) {
                    location.value = loc
                    val weatherSolarData = repository.fetchWeatherAndSolarData(loc.latitude, loc.longitude)
                    if (weatherSolarData != null) {
                        weather.value = weatherSolarData.weather
                        calculateEnergyForecast(weatherSolarData, loc.latitude)
                        error.value = null
                    } else {
                        error.value = "Failed to fetch weather and solar data"
                    }
                } else {
                    error.value = getApplication<Application>().getString(R.string.failed_location)
                }
            } catch (e: Exception) {
                error.value = "Error: ${e.message}"
            } finally {
                loading.value = false
            }
        }
    }

    private fun calculateEnergyForecast(
        weatherSolarData: com.example.mysolarhub.repository.WeatherSolarData,
        latitude: Double
    ) {
        try {
            val forecast = calculationEngine.calculateDailyForecast(
                config = solarSystemConfig.value,
                loadProfile = homeLoadProfile.value,
                irradianceData = weatherSolarData.irradianceData,
                temperatureData = weatherSolarData.hourlyTemperatures,
                latitude = latitude
            )
            dailyForecast.value = forecast
        } catch (e: Exception) {
            error.value = "Calculation error: ${e.message}"
        }
    }

    fun updateSolarSystemConfig(config: SolarSystemConfig) {
        solarSystemConfig.value = config
        // Recalculate if we have weather data
        location.value?.let { loc ->
            weather.value?.let {
                fetchWeatherAndLocation()
            }
        }
    }

    fun updateHomeLoadProfile(profile: HomeLoadProfile) {
        homeLoadProfile.value = profile
        // Recalculate if we have weather data
        location.value?.let { loc ->
            weather.value?.let {
                fetchWeatherAndLocation()
            }
        }
    }

    fun addApplianceLoad(appliance: ApplianceLoad) {
        val currentProfile = homeLoadProfile.value
        if (currentProfile.profileType == LoadProfileType.DETAILED_SCHEDULE) {
            val updatedLoads = currentProfile.detailedLoads + appliance
            homeLoadProfile.value = currentProfile.copy(detailedLoads = updatedLoads)
            // Recalculate if we have weather data
            location.value?.let { loc ->
                weather.value?.let {
                    fetchWeatherAndLocation()
                }
            }
        }
    }

    fun removeApplianceLoad(index: Int) {
        val currentProfile = homeLoadProfile.value
        if (currentProfile.profileType == LoadProfileType.DETAILED_SCHEDULE) {
            val updatedLoads = currentProfile.detailedLoads.toMutableList()
            if (index in updatedLoads.indices) {
                updatedLoads.removeAt(index)
                homeLoadProfile.value = currentProfile.copy(detailedLoads = updatedLoads)
                // Recalculate if we have weather data
                location.value?.let { loc ->
                    weather.value?.let {
                        fetchWeatherAndLocation()
                    }
                }
            }
        }
    }

    fun navigateToScreen(screen: SolarHubScreen) {
        currentScreen.value = screen
    }

    fun retry() {
        fetchWeatherAndLocation()
    }
}

enum class SolarHubScreen {
    SYSTEM_CONFIG,
    LOAD_PROFILE,
    DASHBOARD
}
