package com.example.ai

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

data class FishAudioRequest(
    val text: String,
    val reference_id: String,
    val format: String = "mp3",
    val latency: String = "normal"
)

interface FishAudioApiService {
    @Streaming
    @POST("v1/tts")
    suspend fun generateSpeech(
        @Header("Authorization") authorization: String,
        @Body request: FishAudioRequest
    ): Response<ResponseBody>
}
