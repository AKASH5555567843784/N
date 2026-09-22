package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class LocalWakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private val TAG = "LocalWakeWord"
    
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var workerJob: Job? = null
    
    // Audio recording configuration (standard low-latency spec)
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)

    // Syllable/energy-envelope tracker variables
    private var noiseFloor = 100.0 // Self-adapting background noise level
    private val stateMachine = SyllableStateMachine()

    fun start() {
        if (isListening) return
        isListening = true
        stateMachine.reset()
        
        workerJob = CoroutineScope(Dispatchers.IO).launch {
            runDetectorLoop()
        }
    }

    fun stop() {
        isListening = false
        workerJob?.cancel()
        workerJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recorder", e)
        }
        audioRecord = null
    }

    @SuppressLint("MissingPermission")
    private suspend fun runDetectorLoop() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord")
                return
            }

            audioRecord?.startRecording()
            Log.d(TAG, "Local wake word detector started recording.")

            val buffer = ShortArray(640) // 40ms frames at 16kHz
            
            while (kotlinx.coroutines.currentCoroutineContext().isActive && isListening) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (readBytes > 0) {
                    processFrame(buffer, readBytes)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission is missing or denied for local wake word", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error in local wake word detector loop", e)
        }
    }

    private fun processFrame(buffer: ShortArray, size: Int) {
        // Calculate Root Mean Square (RMS) energy for the frame
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / size)

        // Adapt noise floor slowly during silence
        if (rms < noiseFloor) {
            noiseFloor = noiseFloor * 0.95 + rms * 0.05
        } else {
            noiseFloor = noiseFloor * 0.999 + rms * 0.001
        }
        noiseFloor = noiseFloor.coerceAtLeast(30.0) // Clamp lower bound to prevent noise floor under-estimation

        val relativeEnergy = rms / noiseFloor
        val currentTimeMs = System.currentTimeMillis()

        // Feed energy levels into the syllable state machine
        val triggered = stateMachine.feed(relativeEnergy, currentTimeMs)
        if (triggered) {
            Log.i(TAG, "LOCAL WAKE WORD MATCH DETECTED ('Ninety Five') via adaptive acoustic signature!")
            stateMachine.reset()
            onWakeWordDetected()
        }
    }

    // A lightweight state machine classifier for the three syllables of "Nine-ty Five"
    private class SyllableStateMachine {
        private enum class State {
            SILENT,
            SYLLABLE_1, // "Nine" - High energy
            GAP,        // "ty" - Mid transition / dip
            SYLLABLE_2  // "Five" - High energy finish
        }

        private var currentState = State.SILENT
        private var stateStartTime = 0L

        fun reset() {
            currentState = State.SILENT
            stateStartTime = System.currentTimeMillis()
        }

        fun feed(relativeEnergy: Double, timeMs: Long): Boolean {
            val duration = timeMs - stateStartTime

            // Timeout states if they persist too long
            if (currentState != State.SILENT && duration > 1200) {
                reset()
                return false
            }

            val thresholdHigh = 2.5   // Threshold for speech peaks
            val thresholdLow = 1.6    // Threshold for transition gaps

            when (currentState) {
                State.SILENT -> {
                    if (relativeEnergy > thresholdHigh) {
                        currentState = State.SYLLABLE_1
                        stateStartTime = timeMs
                    }
                }
                State.SYLLABLE_1 -> {
                    // "Nine" syllable usually lasts 150ms - 500ms
                    if (relativeEnergy < thresholdLow) {
                        if (duration in 120..550) {
                            currentState = State.GAP
                            stateStartTime = timeMs
                        } else {
                            reset()
                        }
                    }
                }
                State.GAP -> {
                    // Gap/dip "ty" usually lasts 60ms - 350ms
                    if (relativeEnergy > thresholdHigh) {
                        if (duration in 50..400) {
                            currentState = State.SYLLABLE_2
                            stateStartTime = timeMs
                        } else {
                            reset()
                        }
                    } else if (duration > 400) {
                        reset()
                    }
                }
                State.SYLLABLE_2 -> {
                    // "Five" syllable completes when energy drops back down or sustains
                    if (relativeEnergy < thresholdLow || duration > 500) {
                        if (duration in 150..600) {
                            return true // Syllable sequence Peak-Dip-Peak is fully completed!
                        }
                        reset()
                    }
                }
            }
            return false
        }
    }
}
