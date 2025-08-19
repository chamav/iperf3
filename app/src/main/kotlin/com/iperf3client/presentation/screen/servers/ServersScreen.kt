package com.iperf3client.presentation.screen.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.iperf3client.domain.model.ServerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    modifier: Modifier = Modifier,
    viewModel: ServersViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_servers),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            FloatingActionButton(
                onClick = viewModel::showAddServerDialog,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add server")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error Display
        uiState.validationError?.let { error ->
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
        
        // Servers List
        if (uiState.servers.isEmpty()) {
            EmptyServersView(
                onAddServer = viewModel::showAddServerDialog,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.servers) { server ->
                    ServerCard(
                        server = server,
                        onEdit = viewModel::showEditServerDialog,
                        onDelete = viewModel::deleteServer,
                        onSetDefault = viewModel::setDefaultServer
                    )
                }
            }
        }
    }
    
    // Add/Edit Server Dialog
    if (uiState.showAddDialog) {
        AddServerDialog(
            form = uiState.newServerForm,
            isEditing = uiState.editingServer != null,
            onFormChange = viewModel::updateServerForm,
            onSave = viewModel::saveServer,
            onDismiss = viewModel::hideAddServerDialog
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerItem,
    onEdit: (ServerItem) -> Unit,
    onDelete: (ServerItem) -> Unit,
    onSetDefault: (ServerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with name and default star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(
                        onClick = { onSetDefault(server) }
                    ) {
                        Icon(
                            if (server.isDefault) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (server.isDefault) "Default server" else "Set as default",
                            tint = if (server.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = { onEdit(server) }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit server")
                    }
                    
                    IconButton(
                        onClick = { onDelete(server) }
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete server",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Server details
            Text(
                text = server.getFullAddress(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Protocol and default indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = server.defaultProtocol.toString(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    enabled = false
                )
                
                if (server.isDefault) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        enabled = false
                    )
                }
            }
            
            // Note if present
            server.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyServersView(
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No servers configured",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add your first iperf3 server to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onAddServer) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Server")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerDialog(
    form: NewServerForm,
    isEditing: Boolean,
    onFormChange: (NewServerForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Server" else "Add Server")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    label = { Text("Server Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = form.host,
                    onValueChange = { onFormChange(form.copy(host = it)) },
                    label = { Text(stringResource(R.string.server_host)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = form.port,
                    onValueChange = { onFormChange(form.copy(port = it)) },
                    label = { Text(stringResource(R.string.server_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Protocol Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default Protocol",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row {
                        FilterChip(
                            selected = form.protocol == Protocol.TCP,
                            onClick = { onFormChange(form.copy(protocol = Protocol.TCP)) },
                            label = { Text("TCP") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = form.protocol == Protocol.UDP,
                            onClick = { onFormChange(form.copy(protocol = Protocol.UDP)) },
                            label = { Text("UDP") }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = form.note,
                    onValueChange = { onFormChange(form.copy(note = it)) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = form.isValid
            ) {
                Text(if (isEditing) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}