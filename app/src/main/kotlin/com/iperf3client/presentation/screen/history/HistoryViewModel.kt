package com.iperf3client.presentation.screen.history

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3client.IperfApplication
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.TestResult
import com.iperf3client.domain.usecase.ManageHistoryUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as IperfApplication
    private val manageHistoryUseCase = ManageHistoryUseCase(
        historyRepository = app.historyRepository
    )
    
    var uiState by mutableStateOf(HistoryUiState())
        private set
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        manageHistoryUseCase.observeTestResults().onEach { results ->
            val filtered = filterResults(results)
            uiState = uiState.copy(
                allResults = results,
                filteredResults = filtered
            )
        }.launchIn(viewModelScope)
    }
    
    private fun filterResults(results: List<TestResult>): List<TestResult> {
        var filtered = results
        
        // Filter by protocol
        if (uiState.protocolFilter != null) {
            filtered = filtered.filter { it.params.protocol == uiState.protocolFilter }
        }
        
        // Filter by host
        if (uiState.hostFilter.isNotBlank()) {
            filtered = filtered.filter { 
                it.params.host.contains(uiState.hostFilter, ignoreCase = true) 
            }
        }
        
        // Sort by date (newest first)
        filtered = filtered.sortedByDescending { it.startedAt }
        
        return filtered
    }
    
    fun setProtocolFilter(protocol: Protocol?) {
        uiState = uiState.copy(protocolFilter = protocol)
        uiState = uiState.copy(filteredResults = filterResults(uiState.allResults))
    }
    
    fun setHostFilter(host: String) {
        uiState = uiState.copy(hostFilter = host)
        uiState = uiState.copy(filteredResults = filterResults(uiState.allResults))
    }
    
    fun clearFilters() {
        uiState = uiState.copy(
            protocolFilter = null,
            hostFilter = ""
        )
        uiState = uiState.copy(filteredResults = filterResults(uiState.allResults))
    }
    
    fun deleteTestResult(testResult: TestResult) {
        viewModelScope.launch {
            try {
                manageHistoryUseCase.deleteTestResult(testResult)
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = e.message ?: "Failed to delete test result")
            }
        }
    }
    
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                manageHistoryUseCase.clearAllHistory()
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = e.message ?: "Failed to clear history")
            }
        }
    }
    
    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }
}

data class HistoryUiState(
    val allResults: List<TestResult> = emptyList(),
    val filteredResults: List<TestResult> = emptyList(),
    val protocolFilter: Protocol? = null,
    val hostFilter: String = "",
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = allResults.isEmpty()
    
    val hasFilters: Boolean
        get() = protocolFilter != null || hostFilter.isNotBlank()
    
    val filterCount: Int
        get() = allResults.size - filteredResults.size
}