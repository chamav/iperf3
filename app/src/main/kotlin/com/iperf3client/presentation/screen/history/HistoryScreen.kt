package com.iperf3client.presentation.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iperf3client.R
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.TestResult
import com.iperf3client.presentation.component.TestResultCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onViewDetails: (TestResult) -> Unit = { },
    onExportCsv: (TestResult) -> Unit = { },
    onExportJson: (TestResult) -> Unit = { },
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    var showFilters by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.nav_history),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (uiState.hasFilters) {
                    Text(
                        text = "${uiState.filteredResults.size} of ${uiState.allResults.size} results",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                IconButton(
                    onClick = { showFilters = !showFilters }
                ) {
                    Icon(
                        Icons.Default.FilterList, 
                        contentDescription = "Filter",
                        tint = if (uiState.hasFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (uiState.allResults.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Clear all",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Panel
        if (showFilters) {
            FilterPanel(
                protocolFilter = uiState.protocolFilter,
                hostFilter = uiState.hostFilter,
                onProtocolFilterChange = viewModel::setProtocolFilter,
                onHostFilterChange = viewModel::setHostFilter,
                onClearFilters = viewModel::clearFilters,
                hasFilters = uiState.hasFilters
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Error Display
        uiState.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Content
        when {
            uiState.isEmpty -> {
                EmptyHistoryView(
                    modifier = Modifier.fillMaxSize()
                )
            }
            uiState.filteredResults.isEmpty() && uiState.hasFilters -> {
                NoResultsView(
                    onClearFilters = viewModel::clearFilters,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.filteredResults) { testResult ->
                        TestResultCard(
                            testResult = testResult,
                            onExportCsv = { onExportCsv(testResult) },
                            onExportJson = { onExportJson(testResult) },
                            onViewDetails = { onViewDetails(testResult) }
                        )
                    }
                }
            }
        }
    }
    
    // Clear All Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to delete all test results? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterPanel(
    protocolFilter: Protocol?,
    hostFilter: String,
    onProtocolFilterChange: (Protocol?) -> Unit,
    onHostFilterChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (hasFilters) {
                    TextButton(onClick = onClearFilters) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
            
            // Protocol Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Protocol:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    FilterChip(
                        selected = protocolFilter == null,
                        onClick = { onProtocolFilterChange(null) },
                        label = { Text("All") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = protocolFilter == Protocol.TCP,
                        onClick = { 
                            onProtocolFilterChange(
                                if (protocolFilter == Protocol.TCP) null else Protocol.TCP
                            ) 
                        },
                        label = { Text("TCP") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = protocolFilter == Protocol.UDP,
                        onClick = { 
                            onProtocolFilterChange(
                                if (protocolFilter == Protocol.UDP) null else Protocol.UDP
                            ) 
                        },
                        label = { Text("UDP") }
                    )
                }
            }
            
            // Host Filter
            OutlinedTextField(
                value = hostFilter,
                onValueChange = onHostFilterChange,
                label = { Text("Filter by host") },
                placeholder = { Text("Enter host name or IP") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyHistoryView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No test history",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Run your first iperf3 test to see results here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoResultsView(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Try adjusting your filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onClearFilters) {
            Icon(Icons.Default.Clear, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Filters")
        }
    }
}