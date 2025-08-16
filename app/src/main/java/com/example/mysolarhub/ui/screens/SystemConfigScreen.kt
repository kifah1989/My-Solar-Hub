package com.example.mysolarhub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mysolarhub.data.SolarSystemConfig
import com.example.mysolarhub.data.BatteryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemConfigScreen(
    config: SolarSystemConfig,
    onConfigUpdate: (SolarSystemConfig) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var panelCapacity by remember(config) { mutableStateOf(config.panelCapacityKwp.toString()) }
    var panelAngle by remember(config) { mutableStateOf(config.panelAngle?.toString() ?: "") }
    var batteryCapacity by remember(config) { mutableStateOf(config.batteryCapacityKwh.toString()) }
    var batterySoc by remember(config) { mutableStateOf(config.currentBatterySoc.toString()) }
    var selectedBatteryType by remember(config) { mutableStateOf(config.batteryType) }
    var batteryTypeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Solar System Configuration",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        // Solar Panel Configuration
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WbSunny, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Solar Panels",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedTextField(
                    value = panelCapacity,
                    onValueChange = { panelCapacity = it },
                    label = { Text("Total Panel Capacity (kWp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = panelAngle,
                    onValueChange = { panelAngle = it },
                    label = { Text("Panel Angle (degrees) - Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("Leave empty for optimal angle") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Battery Configuration
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BatteryChargingFull, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Battery System",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = batteryTypeExpanded,
                    onExpandedChange = { batteryTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedBatteryType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Battery Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = batteryTypeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = batteryTypeExpanded,
                        onDismissRequest = { batteryTypeExpanded = false }
                    ) {
                        BatteryType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(type.displayName)
                                        Text(
                                            "Efficiency: ${(type.efficiency * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedBatteryType = type
                                    batteryTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = batteryCapacity,
                    onValueChange = { batteryCapacity = it },
                    label = { Text("Battery Capacity (kWh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = batterySoc,
                    onValueChange = { batterySoc = it },
                    label = { Text("Current Battery Charge (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Save and Continue Button
        Button(
            onClick = {
                val updatedConfig = SolarSystemConfig(
                    panelCapacityKwp = panelCapacity.toDoubleOrNull() ?: 0.0,
                    panelAngle = panelAngle.toDoubleOrNull(),
                    batteryType = selectedBatteryType,
                    batteryCapacityKwh = batteryCapacity.toDoubleOrNull() ?: 0.0,
                    currentBatterySoc = batterySoc.toDoubleOrNull() ?: 50.0
                )
                onConfigUpdate(updatedConfig)
                onNext()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Continue to Load Profile")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
