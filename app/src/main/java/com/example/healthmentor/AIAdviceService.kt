package com.example.healthmentor

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object AIAdviceService {
    private const val TAG = "AIAdviceService"
    private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
    private val client = OkHttpClient()

    fun getAIAdvice(prompt: String, callback: (String) -> Unit) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        Log.d(TAG, "getAIAdvice called with prompt: $prompt")
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            .put("max_tokens", 50)
            .put("temperature", 0.7)
            .toString()
            .toRequestBody(mediaType)

        Log.d(TAG, "Request Body: $requestBody")

        val request = Request.Builder()
            .url(OPENAI_API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        Log.d(TAG, "Request: $request")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get AI advice", e)
                callback("Failed to get AI advice: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Response code: ${response.code}, Response body: $responseBody")
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Unexpected response code: ${response.code}, Response body: $responseBody")
                        callback("Failed to get AI advice: ${response.message}")
                    } else {
                        val advice = JSONObject(responseBody).getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        callback(advice)
                    }
                }
            }
        })
    }
}
