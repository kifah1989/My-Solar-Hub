# MySolarHub App Features

This document summarizes all the features implemented in your Android app, with explanations for each feature and the architectural approach used.

---

## 1. Fetch and Display Weather Data
**Description:**
- The app fetches current weather data (temperature and windspeed) from the public Open-Meteo API.
- Weather data is displayed in the main activity using Jetpack Compose.

**How it works:**
- The API is called with latitude and longitude parameters.
- The response is parsed and shown in the UI.

---

## 2. Location-Based Weather (GPS/Network)
**Description:**
- The app uses the device's current location (via GPS or network) to fetch weather data for the user's actual position.

**How it works:**
- Location permissions are requested at runtime.
- The app uses FusedLocationProviderClient to get the current location.
- Weather is fetched for the detected latitude and longitude.

---

## 3. Interactive Permission Request
**Description:**
- If location permission is not granted, the app shows a message and a button to request permission interactively.

**How it works:**
- Uses Jetpack Compose's permission launcher to request permission.
- UI updates automatically based on permission state.
- If permission is denied, the user can retry by pressing the button.

---

## 4. Retry Location Fetch
**Description:**
- If location fetching fails (e.g., location is null), the app shows a "Retry" button next to the error message.

**How it works:**
- Pressing "Retry" triggers another attempt to fetch the location and weather data.
- Useful for emulator testing and real device edge cases.

---

## 5. MVVM Architecture Refactor
**Description:**
- The app was refactored to use the MVVM (Model-View-ViewModel) architecture for better separation of concerns and maintainability.

**How it works:**
- **Model:** Data classes (e.g., Weather) are in their own files.
- **Repository:** API and location logic are in WeatherRepository.
- **ViewModel:** State and business logic are managed in WeatherViewModel.
- **View/UI:** Composables are in dedicated UI files (e.g., WeatherScreen.kt).
- **MainActivity:** Only sets up the UI and connects to the ViewModel.

---

## 6. Robust Permission Handling
**Description:**
- The app explicitly checks for location permissions before calling location APIs and handles SecurityException to prevent crashes.

**How it works:**
- Permission checks are performed in the repository before accessing location APIs.
- SecurityException is caught and handled gracefully.
- Prevents runtime crashes if permissions are revoked or denied.

---

## 7. Logging for Debugging
**Description:**
- The app uses Logcat debug statements to help diagnose actions and state changes, especially for permission, location, and weather fetching.

**How it works:**
- Log statements are added at key points (permission request, location fetch, weather fetch, errors).
- Developers can use Logcat in Android Studio to monitor app behavior and debug issues.

---

## 8. Emulator Support and Guidance
**Description:**
- The app and documentation provide guidance for setting and sending location in the Android emulator for testing location-based features.

**How it works:**
- Instructions are provided for using the emulator's Extended Controls to set and send a location.
- The app's retry logic and logging help diagnose emulator-specific issues.

---

## 9. MainActivity (MVVM Entry Point)
**Code:**
```kotlin
// MainActivity.kt
package com.example.mysolarhub
// ...existing imports...
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
```

---

## 10. WeatherViewModel (Business Logic & State)
**Code:**
```kotlin
// WeatherViewModel.kt
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
```

---

## 11. WeatherRepository (API & Location)
**Code:**
```kotlin
// WeatherRepository.kt
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
                    return@withContext null
                }
                Tasks.await(
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    )
                )
            } catch (e: SecurityException) {
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}
```

---

## 12. Weather Data Class
**Code:**
```kotlin
// Weather.kt
data class Weather(val temperature: Double, val windspeed: Double)
```

---

## 13. UI Composables (WeatherScreen)
**Code:**
```kotlin
// WeatherScreen.kt
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
```

---

## Summary
Your app now features:
- Live weather data fetching and display
- Location-based weather using device GPS/network
- Interactive permission requests and robust error handling
- Retry logic for location fetch failures
- Clean MVVM architecture for maintainability
- Debug logging and emulator support

This structure makes your app robust, scalable, and easy to extend with new features.
