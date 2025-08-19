package com.iperf3client.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iperf3client.R
import com.iperf3client.domain.model.ServerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelector(
    servers: List<ServerItem>,
    onServerSelected: (ServerItem) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<ServerItem?>(null) }
    
    // Set default server if available
    LaunchedEffect(servers) {
        if (selectedServer == null && servers.isNotEmpty()) {
            selectedServer = servers.find { it.isDefault } ?: servers.first()
        }
    }
    
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.nav_servers),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded && enabled }
        ) {
            OutlinedTextField(
                value = selectedServer?.getDisplayName() ?: "",
                onValueChange = { },
                readOnly = true,
                enabled = enabled,
                placeholder = { Text("Select server...") },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                servers.forEach { server ->
                    DropdownMenuItem(
                        text = {
                            ServerDropdownItem(server = server)
                        },
                        onClick = {
                            selectedServer = server
                            onServerSelected(server)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerDropdownItem(
    server: ServerItem,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = server.getDisplayName(),
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (server.isDefault) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    enabled = false
                )
            }
        }
        
        Text(
            text = server.getFullAddress(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (!server.note.isNullOrBlank()) {
            Text(
                text = server.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}