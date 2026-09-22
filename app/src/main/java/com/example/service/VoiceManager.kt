package com.example.service

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*

enum class RecognitionMode {
    NONE,
    WAKE_WORD,
    USER_COMMAND
}

class VoiceManager(
    private val context: Context,
    private val onSpeechInputDetected: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceManager"

    private val _audioState = MutableStateFlow(AudioState.MIC_OFF)
    val audioState: StateFlow<AudioState> = _audioState

    private val _rmsDbLevel = MutableStateFlow(0f)
    val rmsDbLevel: StateFlow<Float> = _rmsDbLevel

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var currentRecognitionMode = RecognitionMode.NONE
    private var consecutiveWakeWordErrors = 0

    private var mediaPlayer: MediaPlayer? = null
    private val httpClient = OkHttpClient()
    
    private var localWakeWordDetector: LocalWakeWordDetector? = null

    // Configuration settings
    var speechSpeed = 1.0f
    var speechPitch = 0.85f // Deep male pitch
    var currentLanguage = "en-US" // "en-US", "hi-IN", etc.

    private var ttsFinishedCallback: (() -> Unit)? = null

    init {
        initializeTTS()
        localWakeWordDetector = LocalWakeWordDetector(context) {
            CoroutineScope(Dispatchers.Main).launch {
                Log.d("VoiceManager", "Local Wake Word Detector triggered 'Ninety Five'!")
                playWakeBeep()
                onSpeechInputDetected("HEY_NINETY_FIVE_ACTIVATED")
            }
        }
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                tts.language = Locale.US
                // Configure deep male voice speed/pitch
                tts.setPitch(speechPitch)
                tts.setSpeechRate(speechSpeed)
                
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _audioState.value = AudioState.MIC_OUTPUT
                    }

                    override fun onDone(utteranceId: String?) {
                        _audioState.value = AudioState.MIC_OFF
                        ttsFinishedCallback?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _audioState.value = AudioState.MIC_ERROR
                    }
                })
                ttsReady = true
                Log.d(TAG, "TTS system initialized.")
            }
        } else {
            Log.e(TAG, "TTS initialization failed.")
        }
    }

    private suspend fun ensureRecognizerCreated(): SpeechRecognizer? = withContext(Dispatchers.Main) {
        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating speech recognizer", e)
            }
        }
        speechRecognizer
    }

    /**
     * Start speech recognizer (Listen to the user).
     */
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _audioState.value = AudioState.MIC_ERROR
            Log.e(TAG, "Speech recognition is not available on this device.")
            return
        }

        stopTTS() // Prevent hearing own output

        CoroutineScope(Dispatchers.Main).launch {
            if (currentRecognitionMode == RecognitionMode.USER_COMMAND && _audioState.value == AudioState.MIC_LISTENING) {
                Log.d(TAG, "Already listening in USER_COMMAND mode, skipping.")
                return@launch
            }

            currentRecognitionMode = RecognitionMode.USER_COMMAND
            val recognizer = ensureRecognizerCreated() ?: return@launch

            try {
                _audioState.value = AudioState.MIC_STARTING
                recognizer.cancel() // Safely cancel any active recognition session

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _audioState.value = AudioState.MIC_LISTENING
                        _rmsDbLevel.value = 0f
                    }

                    override fun onBeginningOfSpeech() {
                        _audioState.value = AudioState.MIC_LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsDbLevel.value = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _audioState.value = AudioState.MIC_PROCESSING
                    }

                    override fun onError(error: Int) {
                        Log.e(TAG, "STT error: $error")
                        _rmsDbLevel.value = 0f

                        if (currentRecognitionMode != RecognitionMode.USER_COMMAND) return

                        _audioState.value = AudioState.MIC_ERROR

                        // Report error to viewmodel so it can transition or retry gracefully
                        onSpeechInputDetected("ERROR_STT_FAILED")
                    }

                    override fun onResults(results: Bundle?) {
                        _rmsDbLevel.value = 0f
                        if (currentRecognitionMode != RecognitionMode.USER_COMMAND) return

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val transcript = matches[0]
                            Log.d(TAG, "STT Result: $transcript")
                            onSpeechInputDetected(transcript)
                        } else {
                            _audioState.value = AudioState.MIC_OFF
                            onSpeechInputDetected("")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                recognizer.startListening(intent)
            } catch (e: Exception) {
                _audioState.value = AudioState.MIC_ERROR
                Log.e(TAG, "Error starting user command speech recognition", e)
            }
        }
    }

    /**
     * Stop active Speech Recognition
     */
    fun stopListening() {
        currentRecognitionMode = RecognitionMode.NONE
        localWakeWordDetector?.stop()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                speechRecognizer?.let {
                    _audioState.value = AudioState.MIC_STOPPING
                    it.stopListening()
                    it.cancel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer", e)
            } finally {
                _audioState.value = AudioState.MIC_OFF
            }
        }
    }

    /**
     * TTS Male response.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val apiKey = BuildConfig.FISH_AUDIO_API_KEY
        val voiceId = BuildConfig.FISH_AUDIO_VOICE_ID

        // If a valid Fish Audio API key is provided, use Fish Audio API
        if (!apiKey.isNullOrEmpty() && apiKey != "YOUR_FISH_AUDIO_API_KEY" && !voiceId.isNullOrEmpty()) {
            Log.d(TAG, "Using Fish Audio for speech synthesis with voiceId: $voiceId")
            speakWithFishAudio(text, voiceId, apiKey, onDone)
        } else {
            Log.d(TAG, "Using Native Android TextToSpeech")
            speakWithNativeTTS(text, onDone)
        }
    }

    private fun speakWithFishAudio(text: String, voiceId: String, apiKey: String, onDone: (() -> Unit)?) {
        _audioState.value = AudioState.MIC_OUTPUT
        ttsFinishedCallback = onDone

        // Stop any currently playing audio
        stopMediaPlayer()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = com.example.ai.FishAudioClient.apiService.generateSpeech(
                    authorization = "Bearer $apiKey",
                    request = com.example.ai.FishAudioRequest(
                        text = text,
                        reference_id = voiceId,
                        format = "mp3",
                        latency = "normal"
                    )
                )

                if (!response.isSuccessful) {
                    val responseError = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Fish Audio API failed with code ${response.code()}: $responseError")
                    // Fall back to native TTS on the main thread
                    withContext(Dispatchers.Main) {
                        speakWithNativeTTS(text, onDone)
                    }
                    return@launch
                }

                val responseBody = response.body()
                if (responseBody == null) {
                    Log.e(TAG, "Fish Audio response body is null")
                    withContext(Dispatchers.Main) {
                        speakWithNativeTTS(text, onDone)
                    }
                    return@launch
                }

                // Save response stream to cache file
                val tempFile = File(context.cacheDir, "fish_audio_tts.mp3")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                
                FileOutputStream(tempFile).use { fos ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(fos)
                    }
                }

                // Play file using MediaPlayer on main thread
                withContext(Dispatchers.Main) {
                    playAudioFile(tempFile, onDone)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Fish Audio TTS API", e)
                withContext(Dispatchers.Main) {
                    speakWithNativeTTS(text, onDone)
                }
            }
        }
    }

    private fun escapeJsonString(value: String): String {
        val builder = StringBuilder()
        builder.append("\"")
        for (c in value) {
            when (c) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        builder.append(String.format("\\u%04x", c.code))
                    } else {
                        builder.append(c)
                    }
                }
            }
        }
        builder.append("\"")
        return builder.toString()
    }

    private fun playAudioFile(file: File, onDone: (() -> Unit)?) {
        try {
            stopMediaPlayer()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    _audioState.value = AudioState.MIC_OUTPUT
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    _audioState.value = AudioState.MIC_OFF
                    mp.release()
                    mediaPlayer = null
                    onDone?.invoke()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _audioState.value = AudioState.MIC_ERROR
                    mp.release()
                    mediaPlayer = null
                    onDone?.invoke()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            _audioState.value = AudioState.MIC_ERROR
            onDone?.invoke()
        }
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun speakWithNativeTTS(text: String, onDone: (() -> Unit)?) {
        if (!ttsReady) {
            Log.e(TAG, "TTS not ready yet")
            onDone?.invoke()
            return
        }

        // Apply updated pitch and speed settings dynamically
        textToSpeech?.setPitch(speechPitch)
        textToSpeech?.setSpeechRate(speechSpeed)

        ttsFinishedCallback = onDone
        _audioState.value = AudioState.MIC_OUTPUT

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "95_SPEECH_ID")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "95_SPEECH_ID")
    }

    /**
     * Stop speech response immediately (Interruption / Barge-in)
     */
    fun stopTTS() {
        stopMediaPlayer()
        if (textToSpeech?.isSpeaking == true) {
            Log.d(TAG, "Interrupted speech stream.")
            textToSpeech?.stop()
        }
        _audioState.value = AudioState.MIC_OFF
    }

    fun destroy() {
        currentRecognitionMode = RecognitionMode.NONE
        localWakeWordDetector?.stop()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        stopMediaPlayer()
    }

    fun playWakeBeep() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tone", e)
        }
    }

    fun startWakeWordListening() {
        stopTTS()

        CoroutineScope(Dispatchers.Main).launch {
            if (currentRecognitionMode == RecognitionMode.WAKE_WORD) {
                Log.d(TAG, "Already in WAKE_WORD mode, skipping redundant trigger.")
                return@launch
            }

            currentRecognitionMode = RecognitionMode.WAKE_WORD
            localWakeWordDetector?.start()
            Log.d(TAG, "Local wake word detector started successfully.")
        }
    }
}
