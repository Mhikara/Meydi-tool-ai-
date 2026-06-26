package com.example.api.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class ApiRegistryManager {
    private val _endpoints = MutableStateFlow<List<ApiEndpoint>>(emptyList())
    val endpoints: StateFlow<List<ApiEndpoint>> = _endpoints.asStateFlow()

    private val _apiKeys = MutableStateFlow<List<ApiKeyModel>>(emptyList())
    val apiKeys: StateFlow<List<ApiKeyModel>> = _apiKeys.asStateFlow()

    private val usageStats = ConcurrentHashMap<String, Int>()

    fun registerEndpoint(endpoint: ApiEndpoint) {
        _endpoints.value = _endpoints.value + endpoint
    }

    fun registerApiKey(apiKey: ApiKeyModel) {
        _apiKeys.value = _apiKeys.value + apiKey
    }

    fun getHealthyEndpoint(provider: String): ApiEndpoint? {
        return _endpoints.value
            .filter { it.isHealthy && it.name.contains(provider, ignoreCase = true) }
            .sortedByDescending { it.priority }
            .firstOrNull()
    }

    fun getValidKey(provider: String): ApiKeyModel? {
        return _apiKeys.value
            .filter { it.status == ApiKeyStatus.VALID && it.provider == provider }
            .firstOrNull()
    }

    fun reportError(endpointId: String) {
        _endpoints.value = _endpoints.value.map {
            if (it.id == endpointId) it.copy(isHealthy = false) else it
        }
    }

    fun updateKeyStatus(keyId: String, status: ApiKeyStatus) {
        _apiKeys.value = _apiKeys.value.map {
            if (it.id == keyId) it.copy(status = status) else it
        }
    }
}
