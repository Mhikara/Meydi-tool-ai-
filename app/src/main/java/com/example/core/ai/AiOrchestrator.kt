package com.example.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface AiProvider {
    val id: String
    val name: String
    suspend fun chat(prompt: String): String
    fun streamChat(prompt: String): Flow<String> = flowOf("")
}

object AiOrchestrator {
    private val providers = mutableMapOf<String, AiProvider>()
    private var primaryProviderId: String? = null

    fun registerProvider(provider: AiProvider) {
        providers[provider.id] = provider
        if (primaryProviderId == null) primaryProviderId = provider.id
    }

    fun setPrimaryProvider(id: String) {
        if (providers.containsKey(id)) primaryProviderId = id
    }

    suspend fun ask(prompt: String): String {
        val provider = providers[primaryProviderId] ?: return "No AI provider configured."
        return try {
            provider.chat(prompt)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
