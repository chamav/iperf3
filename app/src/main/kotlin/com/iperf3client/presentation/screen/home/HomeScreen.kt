package com.iperf3client.presentation.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iperf3client.R
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.TestStatus
import com.iperf3client.presentation.component.RecentHostsDropdown
import com.iperf3client.presentation.component.ServerSelector
import com.iperf3client.presentation.component.TestProgressCard
import com.iperf3client.presentation.component.TestResultCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Test Configuration Card
        TestConfigurationCard(
            testParams = uiState.testParams,
            isTestRunning = uiState.isTestRunning,
            availableServers = uiState.availableServers,
            recentHosts = uiState.recentHosts,
            onHostChange = viewModel::onHostChange,
            onPortChange = viewModel::onPortChange,
            onProtocolChange = viewModel::onProtocolChange,
            onDurationChange = viewModel::onDurationChange,
            onParallelStreamsChange = viewModel::onParallelStreamsChange,
            onReverseChange = viewModel::onReverseChange,
            onUdpBitrateChange = viewModel::onUdpBitrateChange,
            onWifiOnlyChange = viewModel::onWifiOnlyChange,
            onServerSelected = viewModel::onServerSelected,
            onClearRecentHosts = viewModel::clearRecentHosts
        )
        
        // Control Buttons
        TestControlButtons(
            canStartTest = uiState.canStartTest,
            isTestRunning = uiState.isTestRunning,
            onStartTest = viewModel::startTest,
            onStopTest = viewModel::stopTest
        )
        
        // Error Display
        uiState.validationError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Progress Display
        AnimatedVisibility(visible = uiState.isTestRunning) {
            TestProgressCard(
                currentTick = uiState.currentTick,
                timeline = uiState.timeline,
                testParams = uiState.testParams
            )
        }
        
        // Results Display
        uiState.currentTestResult?.let { result ->
            TestResultCard(
                testResult = result,
                onExportCsv = { /* TODO */ },
                onExportJson = { /* TODO */ },
                onViewDetails = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun TestConfigurationCard(
    testParams: com.iperf3client.domain.model.TestParams,
    isTestRunning: Boolean,
    availableServers: List<com.iperf3client.domain.model.ServerItem>,
    recentHosts: List<com.iperf3client.domain.model.RecentHost>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onProtocolChange: (Protocol) -> Unit,
    onDurationChange: (String) -> Unit,
    onParallelStreamsChange: (String) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onUdpBitrateChange: (String) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onServerSelected: (com.iperf3client.domain.model.ServerItem) -> Unit,
    onClearRecentHosts: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.test_configuration),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Server Selection
            if (availableServers.isNotEmpty()) {
                ServerSelector(
                    servers = availableServers,
                    onServerSelected = onServerSelected,
                    enabled = !isTestRunning
                )
            }
            
            // Recent Hosts Dropdown
            if (recentHosts.isNotEmpty()) {
                RecentHostsDropdown(
                    recentHosts = recentHosts,
                    onHostSelected = onHostChange,
                    onClearRecentHosts = onClearRecentHosts,
                    enabled = !isTestRunning
                )
            }
            
            // Host and Port
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = testParams.host,
                    onValueChange = onHostChange,
                    label = { Text(stringResource(R.string.server_host)) },
                    enabled = !isTestRunning,
                    modifier = Modifier.weight(2f)
                )
                
                OutlinedTextField(
                    value = testParams.port.toString(),
                    onValueChange = onPortChange,
                    label = { Text(stringResource(R.string.server_port)) },
                    enabled = !isTestRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Protocol Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.protocol),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    @OptIn(ExperimentalMaterial3Api::class)
                    FilterChip(
                        selected = testParams.protocol == Protocol.TCP,
                        onClick = { onProtocolChange(Protocol.TCP) },
                        label = { Text("TCP") },
                        enabled = !isTestRunning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    @OptIn(ExperimentalMaterial3Api::class)
                    FilterChip(
                        selected = testParams.protocol == Protocol.UDP,
                        onClick = { onProtocolChange(Protocol.UDP) },
                        label = { Text("UDP") },
                        enabled = !isTestRunning
                    )
                }
            }
            
            // UDP Bitrate (only for UDP)
            AnimatedVisibility(visible = testParams.protocol == Protocol.UDP) {
                OutlinedTextField(
                    value = testParams.udpBitrateMbps?.toString() ?: "50",
                    onValueChange = onUdpBitrateChange,
                    label = { Text(stringResource(R.string.udp_bitrate)) },
                    enabled = !isTestRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Duration and Streams
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = testParams.durationSec.toString(),
                    onValueChange = onDurationChange,
                    label = { Text(stringResource(R.string.duration)) },
                    enabled = !isTestRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = testParams.parallelStreams.toString(),
                    onValueChange = onParallelStreamsChange,
                    label = { Text(stringResource(R.string.parallel_streams)) },
                    enabled = !isTestRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = testParams.reverse,
                        onCheckedChange = onReverseChange,
                        enabled = !isTestRunning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reverse_mode))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = testParams.onlyWifi,
                        onCheckedChange = onWifiOnlyChange,
                        enabled = !isTestRunning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.wifi_only))
                }
            }
        }
    }
}

@Composable
private fun TestControlButtons(
    canStartTest: Boolean,
    isTestRunning: Boolean,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isTestRunning) {
            Button(
                onClick = onStopTest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.stop_test))
            }
        } else {
            Button(
                onClick = onStartTest,
                enabled = canStartTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.start_test))
            }
        }
    }
}