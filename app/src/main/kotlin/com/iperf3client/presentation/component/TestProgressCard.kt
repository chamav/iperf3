package com.iperf3client.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iperf3client.R
import com.iperf3client.domain.model.LiveMetricsTick
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.TestParams

@Composable
fun TestProgressCard(
    currentTick: LiveMetricsTick?,
    timeline: List<LiveMetricsTick>,
    testParams: TestParams,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Test in Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Progress bar
            val progress = if (testParams.durationSec > 0 && timeline.isNotEmpty()) {
                timeline.size.toFloat() / testParams.durationSec.toFloat()
            } else 0f
            
            Column {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${timeline.size}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${testParams.durationSec}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Current metrics
            currentTick?.let { tick ->
                CurrentMetricsDisplay(
                    tick = tick,
                    protocol = testParams.protocol
                )
            }
            
            // Summary statistics
            if (timeline.isNotEmpty()) {
                TestStatistics(
                    timeline = timeline,
                    protocol = testParams.protocol
                )
            }
        }
    }
}

@Composable
private fun CurrentMetricsDisplay(
    tick: LiveMetricsTick,
    protocol: Protocol,
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
                text = "Current Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Speed - always present
            MetricRow(
                label = "Speed",
                value = "${String.format("%.2f", tick.throughputMbps)} Mbps",
                isHighlighted = true
            )
            
            // Protocol specific metrics
            when (protocol) {
                Protocol.TCP -> {
                    tick.retransmits?.let { retransmits ->
                        MetricRow(
                            label = "Retransmits",
                            value = retransmits.toString()
                        )
                    }
                    
                    tick.rttMs?.let { rtt ->
                        MetricRow(
                            label = "RTT",
                            value = "${String.format("%.2f", rtt)} ms"
                        )
                    }
                }
                Protocol.UDP -> {
                    tick.jitterMs?.let { jitter ->
                        MetricRow(
                            label = "Jitter",
                            value = "${String.format("%.2f", jitter)} ms"
                        )
                    }
                    
                    tick.packetLossPct?.let { loss ->
                        MetricRow(
                            label = "Packet Loss",
                            value = "${String.format("%.2f", loss)}%"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestStatistics(
    timeline: List<LiveMetricsTick>,
    protocol: Protocol,
    modifier: Modifier = Modifier
) {
    val speeds = timeline.map { it.throughputMbps }
    val avgSpeed = speeds.average().toFloat()
    val maxSpeed = speeds.maxOrNull() ?: 0f
    val minSpeed = speeds.minOrNull() ?: 0f
    
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
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Avg",
                    value = "${String.format("%.1f", avgSpeed)} Mbps"
                )
                StatisticItem(
                    label = "Max",
                    value = "${String.format("%.1f", maxSpeed)} Mbps"
                )
                StatisticItem(
                    label = "Min",
                    value = "${String.format("%.1f", minSpeed)} Mbps"
                )
            }
            
            // Protocol specific statistics
            when (protocol) {
                Protocol.UDP -> {
                    val jitters = timeline.mapNotNull { it.jitterMs }
                    val losses = timeline.mapNotNull { it.packetLossPct }
                    
                    if (jitters.isNotEmpty() || losses.isNotEmpty()) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (jitters.isNotEmpty()) {
                                StatisticItem(
                                    label = "Avg Jitter",
                                    value = "${String.format("%.2f", jitters.average())} ms"
                                )
                            }
                            if (losses.isNotEmpty()) {
                                StatisticItem(
                                    label = "Avg Loss",
                                    value = "${String.format("%.2f", losses.average())}%"
                                )
                            }
                        }
                    }
                }
                Protocol.TCP -> {
                    val retransmits = timeline.mapNotNull { it.retransmits }
                    val rtts = timeline.mapNotNull { it.rttMs }
                    
                    if (retransmits.isNotEmpty() || rtts.isNotEmpty()) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (retransmits.isNotEmpty()) {
                                StatisticItem(
                                    label = "Total Retr",
                                    value = retransmits.sum().toString()
                                )
                            }
                            if (rtts.isNotEmpty()) {
                                StatisticItem(
                                    label = "Avg RTT",
                                    value = "${String.format("%.2f", rtts.average())} ms"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = if (isHighlighted) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (isHighlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}