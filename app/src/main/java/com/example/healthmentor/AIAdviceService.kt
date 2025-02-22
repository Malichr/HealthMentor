package com.example.healthmentor

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AIAdviceService {
    private const val API_KEY = "AIzaSyDu66m_g1uRBpKq3oeX59VY7G8YLj9M8cY"
    private val model = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = API_KEY
    )

    suspend fun getAIAdvice(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                response.text ?: "Nem sikerült tanácsot generálni."
            } catch (e: Exception) {
                "Hiba történt: ${e.message}"
            }
        }
    }
}
