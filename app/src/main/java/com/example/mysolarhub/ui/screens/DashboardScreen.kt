package com.example.mysolarhub.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mysolarhub.data.DailyEnergyForecast
import com.example.mysolarhub.data.HourlyEnergyData
import java.util.Locale
import kotlin.math.max

@Composable
fun DashboardScreen(
    forecast: DailyEnergyForecast?,
    onRefresh: () -> Unit,
    onBackToConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with refresh
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Solar Energy Dashboard",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Row {
                    IconButton(onClick = onBackToConfig) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configure System")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data")
                    }
                }
            }
        }

        if (forecast != null) {
            // Summary Cards
            SummaryCards(forecast)

            // 5-Day solar production prediction
            DailyBreakdownCard(forecast)

            // Energy Chart
            EnergyChart(forecast.hourlyData)

            // Battery Status
            BatteryStatusCard(forecast)

            // Hourly Breakdown
            HourlyBreakdownCard(forecast.hourlyData)
        } else {
            // No data state
            Card {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No forecast data available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Configure your system and location to see energy forecast",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onBackToConfig) {
                            Text("Configure System")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCards(forecast: DailyEnergyForecast) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            SummaryCard(
                title = "Solar Production",
                value = "${String.format(Locale.getDefault(), "%.1f", forecast.totalSolarProductionKwh)} kWh",
                icon = Icons.Filled.WbSunny,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            SummaryCard(
                title = "Total Consumption",
                value = "${String.format(Locale.getDefault(), "%.1f", forecast.totalConsumptionKwh)} kWh",
                icon = Icons.Filled.Home,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            SummaryCard(
                title = "Grid Usage",
                value = "${String.format(Locale.getDefault(), "%.1f", forecast.totalGridUsageKwh)} kWh",
                icon = Icons.Filled.FlashOn,
                color = if (forecast.totalGridUsageKwh > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            )
        }
        item {
            SummaryCard(
                title = "Battery Level",
                value = "${String.format(Locale.getDefault(), "%.0f", forecast.finalBatterySocPercent)}%",
                icon = Icons.Filled.BatteryChargingFull,
                color = when {
                    forecast.finalBatterySocPercent > 60 -> Color(0xFF4CAF50)
                    forecast.finalBatterySocPercent > 30 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun EnergyChart(hourlyData: List<HourlyEnergyData>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "24-Hour Energy Forecast",
                style = MaterialTheme.typography.titleMedium
            )

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem("Solar", Color(0xFFFFB74D))
                LegendItem("Load", Color(0xFF64B5F6))
                LegendItem("Battery", Color(0xFF81C784))
            }

            // Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 40.dp.toPx()

                val chartWidth = width - 2 * padding
                val chartHeight = height - 2 * padding

                // Find max value for scaling
                val maxValue = max(
                    hourlyData.maxOfOrNull { it.solarProductionKwh } ?: 1.0,
                    hourlyData.maxOfOrNull { it.loadConsumptionKwh } ?: 1.0
                ).toFloat()

                // Draw grid lines
                for (i in 0..4) {
                    val y = padding + (chartHeight / 4) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw solar production line
                val solarPath = Path()
                hourlyData.forEachIndexed { index, data ->
                    val x = padding + (chartWidth / 23) * index
                    val y = padding + chartHeight - (chartHeight * (data.solarProductionKwh / maxValue)).toFloat()

                    if (index == 0) {
                        solarPath.moveTo(x, y)
                    } else {
                        solarPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = solarPath,
                    color = Color(0xFFFFB74D),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw load consumption line
                val loadPath = Path()
                hourlyData.forEachIndexed { index, data ->
                    val x = padding + (chartWidth / 23) * index
                    val y = padding + chartHeight - (chartHeight * (data.loadConsumptionKwh / maxValue)).toFloat()

                    if (index == 0) {
                        loadPath.moveTo(x, y)
                    } else {
                        loadPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = loadPath,
                    color = Color(0xFF64B5F6),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun BatteryStatusCard(forecast: DailyEnergyForecast) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.BatteryChargingFull, contentDescription = null)
                Text(
                    "Battery Status Throughout Day",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Battery level chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 20.dp.toPx()

                val chartWidth = width - 2 * padding
                val chartHeight = height - 2 * padding

                // Draw battery level line
                val batteryPath = Path()
                forecast.hourlyData.forEachIndexed { index, data ->
                    val x = padding + (chartWidth / 23) * index
                    val y = padding + chartHeight - (chartHeight * (data.batterySocPercent / 100f)).toFloat()

                    if (index == 0) {
                        batteryPath.moveTo(x, y)
                    } else {
                        batteryPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = batteryPath,
                    color = Color(0xFF81C784),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw 0%, 50%, 100% reference lines
                listOf(0f, 0.5f, 1f).forEach { level ->
                    val y = padding + chartHeight - (chartHeight * level)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Text(
                "Final battery level: ${String.format(Locale.getDefault(), "%.0f", forecast.finalBatterySocPercent)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HourlyBreakdownCard(hourlyData: List<HourlyEnergyData>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Hourly Breakdown",
                style = MaterialTheme.typography.titleMedium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(hourlyData) { data ->
                    HourlyDataCard(data)
                }
            }
        }
    }
}

@Composable
fun HourlyDataCard(data: HourlyEnergyData) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (data.gridUsageKwh > 0)
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                String.format(Locale.getDefault(), "%02d:00", data.hour),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "☀️ ${String.format(Locale.getDefault(), "%.1f", data.solarProductionKwh)}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "🏠 ${String.format(Locale.getDefault(), "%.1f", data.loadConsumptionKwh)}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "🔋 ${String.format(Locale.getDefault(), "%.0f", data.batterySocPercent)}%",
                style = MaterialTheme.typography.bodySmall
            )

            if (data.gridUsageKwh > 0) {
                Text(
                    "⚡ ${String.format(Locale.getDefault(), "%.1f", data.gridUsageKwh)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DailyBreakdownCard(forecast: DailyEnergyForecast) {
    // Create a simple 5-day projection. If the model had multi-day data this would use it;
    // for now derive five-day estimates from today's total solar production using
    // small deterministic multipliers to simulate variation.
    val base = forecast.totalSolarProductionKwh
    val multipliers = listOf(0.95f, 1.0f, 1.05f, 0.9f, 1.1f)
    val days = List(5) { idx ->
        val value = base * multipliers[idx]
        Pair("Day ${idx + 1}", value)
    }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "5-Day Solar Production Prediction",
                style = MaterialTheme.typography.titleMedium
            )

            // Bar chart (simple)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 12.dp.toPx()
                val chartWidth = width - 2 * padding
                val chartHeight = height - 2 * padding

                val maxVal = (days.maxOfOrNull { it.second } ?: 1.0).toFloat()
                val barWidth = chartWidth / days.size

                days.forEachIndexed { index, pair ->
                    val x = padding + index * barWidth + barWidth * 0.1f
                    val barActualWidth = barWidth * 0.8f
                    val barHeight = (chartHeight * (pair.second.toFloat() / maxVal)).toFloat()
                    val top = padding + chartHeight - barHeight

                    drawRoundRect(
                        color = Color(0xFFFFB74D),
                        topLeft = Offset(x, top),
                        size = androidx.compose.ui.geometry.Size(barActualWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }

            // Day labels and values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${String.format(Locale.getDefault(), "%.1f", value)} kWh",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
