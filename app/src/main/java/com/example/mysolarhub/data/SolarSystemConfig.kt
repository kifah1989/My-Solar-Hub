package com.example.mysolarhub.data

data class SolarSystemConfig(
    val panelCapacityKwp: Double = 0.0, // Total solar panel capacity in kWp
    val panelAngle: Double? = null, // Panel angle in degrees (null for optimal)
    val batteryType: BatteryType = BatteryType.LI_ION,
    val batteryCapacityKwh: Double = 0.0, // Battery capacity in kWh
    val currentBatterySoc: Double = 50.0 // Current battery state of charge (0-100%)
)

enum class BatteryType(val displayName: String, val efficiency: Double) {
    LI_ION("Lithium-Ion", 0.95),
    LEAD_ACID("Lead-Acid", 0.85)
}

data class HomeLoadProfile(
    val profileType: LoadProfileType,
    val simpleAverageDailyKwh: Double = 0.0, // For simple profile
    val detailedLoads: List<ApplianceLoad> = emptyList() // For detailed profile
)

enum class LoadProfileType {
    SIMPLE_AVERAGE,
    DETAILED_SCHEDULE
}

data class ApplianceLoad(
    val name: String,
    val powerWatts: Double,
    val startHour: Int, // 0-23
    val endHour: Int, // 0-23
    val isAllDay: Boolean = false
)

data class SolarIrradiance(
    val hour: Int,
    val ghi: Double, // Global Horizontal Irradiance (W/m²)
    val dni: Double, // Direct Normal Irradiance (W/m²)
    val dhi: Double  // Diffuse Horizontal Irradiance (W/m²)
)

data class HourlyEnergyData(
    val hour: Int,
    val solarProductionKwh: Double,
    val loadConsumptionKwh: Double,
    val batterySocPercent: Double,
    val batteryChargeKwh: Double, // Positive for charging, negative for discharging
    val gridUsageKwh: Double, // Energy pulled from grid
    val excessEnergyKwh: Double // Surplus energy
)

data class DailyEnergyForecast(
    val hourlyData: List<HourlyEnergyData>,
    val totalSolarProductionKwh: Double,
    val totalConsumptionKwh: Double,
    val totalGridUsageKwh: Double,
    val totalExcessEnergyKwh: Double,
    val finalBatterySocPercent: Double
)
