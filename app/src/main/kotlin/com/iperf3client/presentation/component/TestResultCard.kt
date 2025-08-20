package com.iperf3client.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iperf3client.R
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.TestResult
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TestResultCard(
    testResult: TestResult,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Debug logging
    LaunchedEffect(testResult) {
        println("TestResultCard - summary: ${testResult.summary}")
        println("TestResultCard - avgMbps: ${testResult.summary?.avgMbps}")
        println("TestResultCard - timeline size: ${testResult.timeline.size}")
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.notification_test_completed),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = testResult.params.protocol.toString(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    enabled = false
                )
            }
            
            // Test Information
            TestInfoSection(testResult = testResult)
            
            // Summary Metrics
            if (testResult.summary != null) {
                SummaryMetricsSection(
                    summary = testResult.summary,
                    protocol = testResult.params.protocol,
                    parallelStreams = testResult.params.parallelStreams
                )
            } else {
                // Show message when no summary available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "No performance data available",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Action Buttons
            ActionButtonsSection(
                onExportCsv = onExportCsv,
                onExportJson = onExportJson,
                onViewDetails = onViewDetails
            )
        }
    }
}

@Composable
private fun TestInfoSection(
    testResult: TestResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Test Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            InfoRow(
                label = "Server",
                value = "${testResult.params.host}:${testResult.params.port}"
            )
            
            InfoRow(
                label = "Duration",
                value = "${testResult.durationMs?.div(1000) ?: 0}s"
            )
            
            InfoRow(
                label = "Streams",
                value = testResult.params.parallelStreams.toString()
            )
            
            if (testResult.params.reverse) {
                InfoRow(
                    label = "Mode",
                    value = "Reverse"
                )
            }
            
            if (testResult.params.protocol == Protocol.UDP) {
                testResult.params.udpBitrateMbps?.let { bitrate ->
                    InfoRow(
                        label = "Target Bitrate",
                        value = "${bitrate} Mbps"
                    )
                }
            }
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            InfoRow(
                label = "Started",
                value = dateFormat.format(Date.from(testResult.startedAt))
            )
        }
    }
}

@Composable
private fun SummaryMetricsSection(
    summary: com.iperf3client.domain.model.TestSummary,
    protocol: Protocol,
    parallelStreams: Int = 1,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (parallelStreams > 1) "Performance Summary ($parallelStreams streams total)" else "Performance Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Speed metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricDisplay(
                    label = if (parallelStreams > 1) "Avg Total" else "Average",
                    value = "${String.format("%.2f", summary.avgMbps)} Mbps",
                    isHighlighted = true
                )
                MetricDisplay(
                    label = "Maximum",
                    value = "${String.format("%.2f", summary.maxMbps)} Mbps"
                )
                MetricDisplay(
                    label = "Minimum",
                    value = "${String.format("%.2f", summary.minMbps)} Mbps"
                )
            }
            
            // Show per-stream average if multiple streams
            if (parallelStreams > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    MetricDisplay(
                        label = "Avg per Stream",
                        value = "${String.format("%.2f", summary.avgMbps / parallelStreams)} Mbps"
                    )
                }
            }
            
            // Protocol specific metrics
            when (protocol) {
                Protocol.TCP -> {
                    summary.retransmits?.let { retransmits ->
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MetricDisplay(
                                label = "Retransmits",
                                value = retransmits.toString()
                            )
                            summary.rttMs?.let { rtt ->
                                MetricDisplay(
                                    label = "RTT",
                                    value = "${String.format("%.2f", rtt)} ms"
                                )
                            }
                        }
                    }
                }
                Protocol.UDP -> {
                    if (summary.jitterMs != null || summary.packetLossPct != null) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            summary.jitterMs?.let { jitter ->
                                MetricDisplay(
                                    label = "Jitter",
                                    value = "${String.format("%.3f", jitter)} ms"
                                )
                            }
                            summary.packetLossPct?.let { loss ->
                                MetricDisplay(
                                    label = "Packet Loss",
                                    value = "${String.format("%.2f", loss)}%"
                                )
                            }
                        }
                    }
                }
            }
            
            // Data transfer
            summary.totalBytes?.let { bytes ->
                Divider()
                val mbytes = bytes / (1024.0 * 1024.0)
                InfoRow(
                    label = "Data Transferred",
                    value = "${String.format("%.2f", mbytes)} MB"
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onViewDetails,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Details")
        }
        
        OutlinedButton(
            onClick = onExportCsv,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("CSV")
        }
        
        OutlinedButton(
            onClick = onExportJson,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("JSON")
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MetricDisplay(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlighted) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            },
            color = if (isHighlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}