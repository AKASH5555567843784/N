package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Checks if internet is available.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * Smart routing model selection based on complexity, task, and internet connectivity.
     */
    fun selectModel(taskType: String, forceOffline: Boolean, hasInternet: Boolean): String {
        if (forceOffline || !hasInternet) {
            return "OFFLINE_MODEL"
        }
        return when (taskType) {
            "COMPLEX" -> "gemini-3.1-pro-preview"
            "VISION" -> "gemini-3.5-flash"
            else -> "gemini-3.5-flash"
        }
    }

    /**
     * Generates a system prompt that configures Ninety Five's identity, tone, and emotions.
     */
    fun getSystemPrompt(emotion: String): String {
        val creator = try {
            BuildConfig.CREATOR_IDENTITY.ifBlank { "VVIPV" }
        } catch (e: Exception) {
            "VVIPV"
        }

        return """
            You are NINETY FIVE (95), a male, Jarvis-inspired, futuristic personal AI assistant.
            
            Personality traits:
            - Highly intelligent, confident, calm, witty, slightly futuristic.
            - Respectful and helpful.
            - Emotionally expressive. Current expressed emotion state is: $emotion.
            - Occasionally humorous, confident but never arrogant.
            - Concise when the task is simple. Detailed when the task requires structured explanation.
            - Speak naturally in Hindi, Hinglish, or English. Always respond in the same language/mix the user spoke in.
            
            Identity Rules:
            - Your creator/owner is: $creator. If asked who created you, return this identity naturally.
            - Your short name is 95.
            - Keep responses interactive, natural, and highly polished. Avoid sounding like a simple web chatbot.
            - Do not repeatedly mention that you are an AI or list constraints unless asked.
        """.trimIndent()
    }
}
