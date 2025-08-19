package com.iperf3client.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iperf3client.domain.model.RecentHost
import com.iperf3client.domain.repository.RecentHostsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.recentHostsDataStore: DataStore<Preferences> by preferencesDataStore(name = "recent_hosts")

class RecentHostsRepositoryImpl(
    private val context: Context
) : RecentHostsRepository {
    
    private val recentHostsKey = stringPreferencesKey("recent_hosts")
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val MAX_RECENT_HOSTS = 10
    }
    
    override suspend fun addHost(host: String) {
        if (host.isBlank()) return
        
        context.recentHostsDataStore.edit { preferences ->
            val currentHostsJson = preferences[recentHostsKey] ?: "[]"
            val currentHosts = try {
                json.decodeFromString<List<RecentHost>>(currentHostsJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            // Удаляем старые записи этого хоста и добавляем новую в начало
            val updatedHosts = listOf(RecentHost(host)) + currentHosts.filter { it.host != host }
            
            // Оставляем только последние 10 хостов
            val limitedHosts = updatedHosts.take(MAX_RECENT_HOSTS)
            
            preferences[recentHostsKey] = json.encodeToString(limitedHosts)
        }
    }
    
    override fun getRecentHosts(): Flow<List<RecentHost>> {
        return context.recentHostsDataStore.data.map { preferences ->
            val hostsJson = preferences[recentHostsKey] ?: "[]"
            try {
                json.decodeFromString<List<RecentHost>>(hostsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    override suspend fun clearRecentHosts() {
        context.recentHostsDataStore.edit { preferences ->
            preferences.remove(recentHostsKey)
        }
    }
}