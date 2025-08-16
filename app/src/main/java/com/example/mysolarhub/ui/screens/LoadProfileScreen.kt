package com.example.mysolarhub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mysolarhub.data.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadProfileScreen(
    loadProfile: HomeLoadProfile,
    onProfileUpdate: (HomeLoadProfile) -> Unit,
    onAddAppliance: (ApplianceLoad) -> Unit,
    onRemoveAppliance: (Int) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProfileType by remember(loadProfile) { mutableStateOf(loadProfile.profileType) }
    var simpleAverage by remember(loadProfile) { mutableStateOf(loadProfile.simpleAverageDailyKwh.toString()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Home Load Profile",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        // Profile Type Selection
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "How would you like to define your electricity usage?",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedProfileType == LoadProfileType.SIMPLE_AVERAGE,
                        onClick = {
                            selectedProfileType = LoadProfileType.SIMPLE_AVERAGE
                            onProfileUpdate(
                                HomeLoadProfile(
                                    profileType = LoadProfileType.SIMPLE_AVERAGE,
                                    simpleAverageDailyKwh = simpleAverage.toDoubleOrNull() ?: 0.0
                                )
                            )
                        },
                        label = { Text("Simple Average") },
                        leadingIcon = {
                            Icon(Icons.Filled.TrendingUp, contentDescription = null)
                        }
                    )

                    FilterChip(
                        selected = selectedProfileType == LoadProfileType.DETAILED_SCHEDULE,
                        onClick = {
                            selectedProfileType = LoadProfileType.DETAILED_SCHEDULE
                            onProfileUpdate(
                                HomeLoadProfile(
                                    profileType = LoadProfileType.DETAILED_SCHEDULE,
                                    detailedLoads = loadProfile.detailedLoads
                                )
                            )
                        },
                        label = { Text("Detailed Schedule") },
                        leadingIcon = {
                            Icon(Icons.Filled.AccessTime, contentDescription = null)
                        }
                    )
                }
            }
        }

        // Profile Configuration
        when (selectedProfileType) {
            LoadProfileType.SIMPLE_AVERAGE -> {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Average Daily Usage",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = simpleAverage,
                            onValueChange = {
                                simpleAverage = it
                                onProfileUpdate(
                                    HomeLoadProfile(
                                        profileType = LoadProfileType.SIMPLE_AVERAGE,
                                        simpleAverageDailyKwh = it.toDoubleOrNull() ?: 0.0
                                    )
                                )
                            },
                            label = { Text("Daily Energy Usage (kWh)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Text("Enter your typical daily electricity consumption")
                            }
                        )
                    }
                }
            }

            LoadProfileType.DETAILED_SCHEDULE -> {
                // Detailed appliance list
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Appliances & Loads",
                                style = MaterialTheme.typography.titleMedium
                            )

                            FilledTonalButton(
                                onClick = { showAddDialog = true }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Add Appliance")
                            }
                        }

                        if (loadProfile.detailedLoads.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Power,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text("No appliances added yet")
                                        Text(
                                            "Add your appliances to see detailed energy analysis",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(loadProfile.detailedLoads) { index, appliance ->
                                    ApplianceCard(
                                        appliance = appliance,
                                        onRemove = { onRemoveAppliance(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Next Button
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = when (selectedProfileType) {
                LoadProfileType.SIMPLE_AVERAGE -> simpleAverage.toDoubleOrNull()?.let { it > 0 } == true
                LoadProfileType.DETAILED_SCHEDULE -> loadProfile.detailedLoads.isNotEmpty()
            }
        ) {
            Text("Continue to Dashboard")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ShowChart, contentDescription = null)
        }
    }

    // Add Appliance Dialog
    if (showAddDialog) {
        AddApplianceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { appliance ->
                onAddAppliance(appliance)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ApplianceCard(
    appliance: ApplianceLoad,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Power,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    appliance.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${appliance.powerWatts.toInt()}W",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (appliance.isAllDay) {
                    Text(
                        "24/7 Operation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "${String.format(Locale.getDefault(), "%02d:00", appliance.startHour)} - ${String.format(Locale.getDefault(), "%02d:00", appliance.endHour)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove appliance",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApplianceDialog(
    onDismiss: () -> Unit,
    onAdd: (ApplianceLoad) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf("0") }
    var endHour by remember { mutableStateOf("23") }
    var isAllDay by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Appliance") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Appliance Name") },
                    placeholder = { Text("e.g., Refrigerator, TV, Heater") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = power,
                    onValueChange = { power = it },
                    label = { Text("Power (Watts)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                    Text("Runs 24/7")
                }

                if (!isAllDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { if (it.toIntOrNull()?.let { h -> h in 0..23 } != false) startHour = it },
                            label = { Text("Start Hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = endHour,
                            onValueChange = { if (it.toIntOrNull()?.let { h -> h in 0..23 } != false) endHour = it },
                            label = { Text("End Hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val appliance = ApplianceLoad(
                        name = name,
                        powerWatts = power.toDoubleOrNull() ?: 0.0,
                        startHour = if (isAllDay) 0 else (startHour.toIntOrNull() ?: 0),
                        endHour = if (isAllDay) 23 else (endHour.toIntOrNull() ?: 23),
                        isAllDay = isAllDay
                    )
                    onAdd(appliance)
                },
                enabled = name.isNotBlank() && power.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
