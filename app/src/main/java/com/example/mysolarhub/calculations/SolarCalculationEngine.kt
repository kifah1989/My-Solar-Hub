package com.example.mysolarhub.calculations

import com.example.mysolarhub.data.*
import kotlin.math.*

class SolarCalculationEngine {

    // Standard Test Conditions (STC) irradiance
    private val STC_IRRADIANCE = 1000.0 // W/m²

    // Temperature coefficient for silicon panels (typical)
    private val TEMP_COEFFICIENT = -0.004 // per °C

    fun calculateDailyForecast(
        config: SolarSystemConfig,
        loadProfile: HomeLoadProfile,
        irradianceData: List<SolarIrradiance>,
        temperatureData: List<Double>, // Hourly temperatures in °C
        latitude: Double
    ): DailyEnergyForecast {

        val hourlyData = mutableListOf<HourlyEnergyData>()
        var currentBatterySoc = config.currentBatterySoc

        val hourlyLoadConsumption = calculateHourlyLoadConsumption(loadProfile)

        for (hour in 0..23) {
            val irradiance = irradianceData.getOrNull(hour) ?: SolarIrradiance(hour, 0.0, 0.0, 0.0)
            val temperature = temperatureData.getOrNull(hour) ?: 25.0

            // Calculate solar production for this hour
            val solarProduction = calculateSolarProduction(
                config, irradiance, temperature, latitude, hour
            )

            val loadConsumption = hourlyLoadConsumption[hour]

            // Calculate energy balance and battery state
            val energyBalance = solarProduction - loadConsumption

            var batteryCharge = 0.0
            var gridUsage = 0.0
            var excessEnergy = 0.0

            if (energyBalance > 0) {
                // Surplus energy - charge battery or export
                val maxBatteryCharge = calculateMaxBatteryCharge(config, currentBatterySoc)
                batteryCharge = minOf(energyBalance, maxBatteryCharge)
                currentBatterySoc += (batteryCharge / config.batteryCapacityKwh) * 100

                excessEnergy = energyBalance - batteryCharge
            } else {
                // Energy deficit - discharge battery or use grid
                val maxBatteryDischarge = calculateMaxBatteryDischarge(config, currentBatterySoc)
                val deficit = -energyBalance

                batteryCharge = -minOf(deficit, maxBatteryDischarge)
                currentBatterySoc += (batteryCharge / config.batteryCapacityKwh) * 100

                val remainingDeficit = deficit + batteryCharge // batteryCharge is negative
                gridUsage = maxOf(0.0, remainingDeficit)
            }

            hourlyData.add(
                HourlyEnergyData(
                    hour = hour,
                    solarProductionKwh = solarProduction,
                    loadConsumptionKwh = loadConsumption,
                    batterySocPercent = currentBatterySoc,
                    batteryChargeKwh = batteryCharge,
                    gridUsageKwh = gridUsage,
                    excessEnergyKwh = excessEnergy
                )
            )
        }

        return DailyEnergyForecast(
            hourlyData = hourlyData,
            totalSolarProductionKwh = hourlyData.sumOf { it.solarProductionKwh },
            totalConsumptionKwh = hourlyData.sumOf { it.loadConsumptionKwh },
            totalGridUsageKwh = hourlyData.sumOf { it.gridUsageKwh },
            totalExcessEnergyKwh = hourlyData.sumOf { it.excessEnergyKwh },
            finalBatterySocPercent = currentBatterySoc
        )
    }

    private fun calculateSolarProduction(
        config: SolarSystemConfig,
        irradiance: SolarIrradiance,
        temperature: Double,
        latitude: Double,
        hour: Int
    ): Double {
        if (config.panelCapacityKwp <= 0) return 0.0

        // Calculate optimal tilt angle if not provided
        val tiltAngle = config.panelAngle ?: calculateOptimalTiltAngle(latitude)

        // Calculate solar irradiance on tilted surface
        val tiltedIrradiance = calculateTiltedIrradiance(
            irradiance, tiltAngle, latitude, hour
        )

        // Temperature derating
        val tempDeratingFactor = 1 + TEMP_COEFFICIENT * (temperature - 25)

        // Calculate DC power production
        val dcPowerKw = config.panelCapacityKwp * (tiltedIrradiance / STC_IRRADIANCE) * tempDeratingFactor

        // Apply system losses (inverter efficiency, wiring losses, etc.)
        val systemEfficiency = 0.85 // Typical system efficiency

        return maxOf(0.0, dcPowerKw * systemEfficiency)
    }

    private fun calculateOptimalTiltAngle(latitude: Double): Double {
        // Rule of thumb: optimal tilt ≈ latitude for year-round production
        return abs(latitude)
    }

    private fun calculateTiltedIrradiance(
        irradiance: SolarIrradiance,
        tiltAngle: Double,
        latitude: Double,
        hour: Int
    ): Double {
        // Simplified calculation - in reality this would involve complex solar geometry
        // For now, use a basic model that adjusts GHI based on tilt
        val tiltRadians = Math.toRadians(tiltAngle)
        val latRadians = Math.toRadians(latitude)

        // Simple approximation - multiply by tilt factor
        val tiltFactor = cos(tiltRadians - latRadians)
        val adjustedIrradiance = irradiance.ghi * maxOf(0.1, tiltFactor)

        // Apply sun angle correction for hour of day
        val solarElevationFactor = calculateSolarElevationFactor(hour)

        return adjustedIrradiance * solarElevationFactor
    }

    private fun calculateSolarElevationFactor(hour: Int): Double {
        // Simplified solar elevation based on time of day
        // Peak at solar noon (12:00), zero at night
        return when (hour) {
            in 6..17 -> {
                val hoursFromNoon = abs(hour - 12)
                maxOf(0.0, cos(hoursFromNoon * PI / 12))
            }
            else -> 0.0
        }
    }

    private fun calculateHourlyLoadConsumption(loadProfile: HomeLoadProfile): List<Double> {
        return when (loadProfile.profileType) {
            LoadProfileType.SIMPLE_AVERAGE -> {
                val hourlyAverage = loadProfile.simpleAverageDailyKwh / 24.0
                List(24) { hourlyAverage }
            }
            LoadProfileType.DETAILED_SCHEDULE -> {
                val hourlyConsumption = MutableList(24) { 0.0 }

                for (appliance in loadProfile.detailedLoads) {
                    if (appliance.isAllDay) {
                        // Add continuous load for all hours
                        for (hour in 0..23) {
                            hourlyConsumption[hour] += appliance.powerWatts / 1000.0 // Convert W to kW
                        }
                    } else {
                        // Add load for specific time range
                        val startHour = appliance.startHour
                        val endHour = appliance.endHour

                        if (startHour <= endHour) {
                            // Same day
                            for (hour in startHour..endHour) {
                                hourlyConsumption[hour] += appliance.powerWatts / 1000.0
                            }
                        } else {
                            // Crosses midnight
                            for (hour in startHour..23) {
                                hourlyConsumption[hour] += appliance.powerWatts / 1000.0
                            }
                            for (hour in 0..endHour) {
                                hourlyConsumption[hour] += appliance.powerWatts / 1000.0
                            }
                        }
                    }
                }

                hourlyConsumption
            }
        }
    }

    private fun calculateMaxBatteryCharge(config: SolarSystemConfig, currentSoc: Double): Double {
        if (config.batteryCapacityKwh <= 0) return 0.0

        val maxSoc = 100.0 // Maximum safe SOC
        val remainingCapacity = (maxSoc - currentSoc) / 100.0 * config.batteryCapacityKwh

        // Apply charging efficiency
        return remainingCapacity / config.batteryType.efficiency
    }

    private fun calculateMaxBatteryDischarge(config: SolarSystemConfig, currentSoc: Double): Double {
        if (config.batteryCapacityKwh <= 0) return 0.0

        val minSoc = 10.0 // Minimum safe SOC (protect battery)
        val availableCapacity = maxOf(0.0, (currentSoc - minSoc) / 100.0 * config.batteryCapacityKwh)

        // Apply discharging efficiency
        return availableCapacity * config.batteryType.efficiency
    }
}
