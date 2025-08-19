package com.iperf3client.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iperf3client.R
import com.iperf3client.domain.model.Protocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(
                onClick = { showResetDialog = true }
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test Defaults Section
        SettingsSectionCard(
            title = "Test Defaults",
            description = "Default values for new tests"
        ) {
            // WiFi Only Default
            SettingsSwitch(
                title = stringResource(R.string.wifi_only),
                description = "Only run tests when connected to WiFi",
                checked = uiState.wifiOnlyDefault,
                onCheckedChange = viewModel::setWifiOnlyDefault
            )
            
            Divider()
            
            // Default Protocol
            SettingsProtocolSelector(
                title = stringResource(R.string.protocol),
                description = "Default protocol for new tests",
                selectedProtocol = uiState.defaultProtocol,
                onProtocolSelected = viewModel::setDefaultProtocol
            )
            
            Divider()
            
            // Default Duration
            SettingsNumberInput(
                title = stringResource(R.string.duration),
                description = "Default test duration in seconds",
                value = uiState.defaultTestDuration,
                onValueChange = viewModel::setDefaultTestDuration,
                suffix = "sec"
            )
            
            Divider()
            
            // Default Parallel Streams
            SettingsNumberInput(
                title = stringResource(R.string.parallel_streams),
                description = "Default number of parallel connections",
                value = uiState.defaultParallelStreams,
                onValueChange = viewModel::setDefaultParallelStreams
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App Behavior Section
        SettingsSectionCard(
            title = "App Behavior",
            description = "Control app behavior during tests"
        ) {
            // Keep Screen On
            SettingsSwitch(
                title = "Keep Screen On",
                description = "Prevent screen from turning off during tests",
                checked = uiState.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn
            )
            
            Divider()
            
            // Show Notifications During Test
            SettingsSwitch(
                title = "Show Progress Notifications",
                description = "Display notifications during test execution",
                checked = uiState.showNotificationsDuringTest,
                onCheckedChange = viewModel::setShowNotificationsDuringTest
            )
            
            Divider()
            
            // Auto Export Results
            SettingsSwitch(
                title = "Auto Export Results",
                description = "Automatically export completed test results",
                checked = uiState.autoExportResults,
                onCheckedChange = viewModel::setAutoExportResults
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Data Management Section
        SettingsSectionCard(
            title = "Data Management",
            description = "Manage app data and storage"
        ) {
            // Max History Size
            SettingsNumberInput(
                title = "Max History Size",
                description = "Maximum number of test results to keep",
                value = uiState.maxHistorySize,
                onValueChange = viewModel::setMaxHistorySize,
                suffix = "tests"
            )
        }
    }
    
    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Are you sure you want to reset all settings to their default values?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsProtocolSelector(
    title: String,
    description: String,
    selectedProtocol: Protocol,
    onProtocolSelected: (Protocol) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row {
            FilterChip(
                selected = selectedProtocol == Protocol.TCP,
                onClick = { onProtocolSelected(Protocol.TCP) },
                label = { Text("TCP") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = selectedProtocol == Protocol.UDP,
                onClick = { onProtocolSelected(Protocol.UDP) },
                label = { Text("UDP") }
            )
        }
    }
}

@Composable
private fun SettingsNumberInput(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    suffix: String? = null,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                newValue.toIntOrNull()?.let { intValue ->
                    if (intValue >= 0) {
                        onValueChange(intValue)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = suffix?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}