package com.example.core.ai

import com.example.api.GeminiGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiAiProvider(private val apiKey: String) : AiProvider {
    override val id: String = "ai.gemini"
    override val name: String = "Google Gemini Pro"
    
    override suspend fun chat(prompt: String): String {
        return com.example.api.GeminiGenerator.chat(prompt)
    }

    override fun streamChat(prompt: String): Flow<String> = flow {
        emit(com.example.api.GeminiGenerator.chat(prompt))
    }
}
