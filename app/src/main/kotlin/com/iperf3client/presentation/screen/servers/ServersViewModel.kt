package com.iperf3client.presentation.screen.servers

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3client.IperfApplication
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.ServerItem
import com.iperf3client.domain.usecase.ManageServersUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ServersViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as IperfApplication
    private val manageServersUseCase = ManageServersUseCase(
        serversRepository = app.serversRepository
    )
    
    var uiState by mutableStateOf(ServersUiState())
        private set
    
    init {
        loadServers()
    }
    
    private fun loadServers() {
        manageServersUseCase.observeServers().onEach { servers ->
            uiState = uiState.copy(servers = servers)
        }.launchIn(viewModelScope)
    }
    
    fun showAddServerDialog() {
        uiState = uiState.copy(
            showAddDialog = true,
            editingServer = null,
            newServerForm = NewServerForm()
        )
    }
    
    fun showEditServerDialog(server: ServerItem) {
        uiState = uiState.copy(
            showAddDialog = true,
            editingServer = server,
            newServerForm = NewServerForm(
                name = server.name,
                host = server.host,
                port = server.port.toString(),
                protocol = server.defaultProtocol,
                note = server.note ?: ""
            )
        )
    }
    
    fun hideAddServerDialog() {
        uiState = uiState.copy(
            showAddDialog = false,
            editingServer = null,
            validationError = null
        )
    }
    
    fun updateServerForm(form: NewServerForm) {
        uiState = uiState.copy(
            newServerForm = form,
            validationError = null
        )
    }
    
    fun saveServer() {
        val form = uiState.newServerForm
        
        // Validation
        if (form.name.isBlank()) {
            uiState = uiState.copy(validationError = "Server name is required")
            return
        }
        
        if (form.host.isBlank()) {
            uiState = uiState.copy(validationError = "Host is required")
            return
        }
        
        val port = form.port.toIntOrNull()
        if (port == null || port !in 1..65535) {
            uiState = uiState.copy(validationError = "Invalid port number")
            return
        }
        
        val server = if (uiState.editingServer != null) {
            // Update existing server
            uiState.editingServer!!.copy(
                name = form.name,
                host = form.host,
                port = port,
                defaultProtocol = form.protocol,
                note = form.note.takeIf { it.isNotBlank() }
            )
        } else {
            // Create new server
            ServerItem(
                id = 0L, // Room will generate ID
                name = form.name,
                host = form.host,
                port = port,
                defaultProtocol = form.protocol,
                note = form.note.takeIf { it.isNotBlank() },
                isDefault = false
            )
        }
        
        viewModelScope.launch {
            try {
                if (uiState.editingServer != null) {
                    manageServersUseCase.updateServer(server)
                } else {
                    manageServersUseCase.addServer(
                        name = form.name,
                        host = form.host,
                        port = port,
                        defaultProtocol = form.protocol,
                        note = form.note.takeIf { it.isNotBlank() }
                    )
                }
                hideAddServerDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(validationError = e.message ?: "Failed to save server")
            }
        }
    }
    
    fun deleteServer(server: ServerItem) {
        viewModelScope.launch {
            try {
                manageServersUseCase.deleteServer(server)
            } catch (e: Exception) {
                uiState = uiState.copy(validationError = e.message ?: "Failed to delete server")
            }
        }
    }
    
    fun setDefaultServer(server: ServerItem) {
        viewModelScope.launch {
            try {
                manageServersUseCase.setDefaultServer(server.id)
            } catch (e: Exception) {
                uiState = uiState.copy(validationError = e.message ?: "Failed to set default server")
            }
        }
    }
    
    fun clearError() {
        uiState = uiState.copy(validationError = null)
    }
}

data class ServersUiState(
    val servers: List<ServerItem> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingServer: ServerItem? = null,
    val newServerForm: NewServerForm = NewServerForm(),
    val validationError: String? = null
)

data class NewServerForm(
    val name: String = "",
    val host: String = "",
    val port: String = "5201",
    val protocol: Protocol = Protocol.TCP,
    val note: String = ""
) {
    val isValid: Boolean
        get() = name.isNotBlank() && 
                host.isNotBlank() && 
                port.toIntOrNull() != null &&
                port.toIntOrNull()!! in 1..65535
}