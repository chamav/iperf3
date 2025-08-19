package com.iperf3client.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iperf3client.R
import com.iperf3client.domain.model.RecentHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentHostsDropdown(
    recentHosts: List<RecentHost>,
    onHostSelected: (String) -> Unit,
    onClearRecentHosts: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    if (recentHosts.isEmpty()) return
    
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.recent_hosts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = stringResource(R.string.select_recent_host),
                onValueChange = { },
                readOnly = true,
                enabled = enabled,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.clickable(enabled = enabled) { 
                            expanded = !expanded 
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .clickable(enabled = enabled) { expanded = !expanded }
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                recentHosts.forEach { recentHost ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = recentHost.host,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            onHostSelected(recentHost.host)
                            expanded = false
                        }
                    )
                }
                
                if (recentHosts.isNotEmpty()) {
                    Divider()
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = stringResource(R.string.clear_recent_hosts),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onClearRecentHosts()
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}