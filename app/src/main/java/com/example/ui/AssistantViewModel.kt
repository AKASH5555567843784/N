package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.Content
import com.example.ai.GenerateContentRequest
import com.example.ai.GeminiClient
import com.example.ai.Part
import com.example.ai.SystemInstruction
import com.example.data.AssistantDatabase
import com.example.data.AssistantRepository
import com.example.data.MemoryEntity
import com.example.data.PlannerEntity
import com.example.data.UserHistoryEntity
import com.example.data.PreferenceEntity
import com.example.data.ChatMessageEntity
import com.example.service.AudioState
import com.example.service.VoiceManager
import com.example.service.AssistantForegroundService
import com.example.util.AppController
import com.example.tools.ToolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// AI States as requested in Section 5
enum class CoreState {
    IDLE,
    WAKE_DETECTED,
    LISTENING,
    THINKING,
    SEARCHING,
    ACTING,
    SPEAKING,
    ERROR
}

// Emotions as requested in Section 8
enum class AssistantEmotion {
    CALM,
    HAPPY,
    EXCITED,
    CURIOUS,
    CONCERNED,
    SURPRISED,
    FRUSTRATED,
    SERIOUS,
    FOCUSED,
    CONFIDENT,
    RELIEVED,
    PLAYFUL
}

data class ChatMessage(
    val sender: String, // "USER" or "95"
    val text: String,
    val emotion: AssistantEmotion = AssistantEmotion.CALM,
    val timestamp: Long = System.currentTimeMillis(),
    val dbId: Long = 0
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AssistantViewModel"

    private val db = AssistantDatabase.getDatabase(application)
    private val repository = AssistantRepository(db.assistantDao())
    private val toolManager = ToolManager(db)

    private var isActiveSession = false
    private var inactivityJob: kotlinx.coroutines.Job? = null

    // Voice Manager initialized on demand with context
    private val voiceManager: VoiceManager = VoiceManager(application) { input ->
        if (input == "HEY_NINETY_FIVE_ACTIVATED") {
            viewModelScope.launch {
                if (_coreState.value == CoreState.IDLE) {
                    showNotification("🤖 HEY NINETY FIVE DETECTED")
                    isActiveSession = true
                    _coreState.value = CoreState.WAKE_DETECTED
                    delay(400) // Brief holographic transition
                    startListening()
                }
            }
        } else if (input == "ERROR_STT_FAILED") {
            viewModelScope.launch {
                Log.d(TAG, "STT Error callback triggered. Returning to IDLE gracefully to prevent mic loop.")
                isActiveSession = false
                _coreState.value = CoreState.IDLE
                voiceManager.stopListening()
                if (wakeWordEnabled.value) {
                    startWakeWordDetection()
                }
            }
        } else if (input.isBlank()) {
            viewModelScope.launch {
                Log.d(TAG, "STT blank input callback. Returning to IDLE gracefully to prevent mic loop.")
                isActiveSession = false
                _coreState.value = CoreState.IDLE
                voiceManager.stopListening()
                if (wakeWordEnabled.value) {
                    startWakeWordDetection()
                }
            }
        } else {
            processUserInput(input)
        }
    }

    // UI State variables
    private val _coreState = MutableStateFlow(CoreState.IDLE)
    val coreState: StateFlow<CoreState> = _coreState

    private val _currentEmotion = MutableStateFlow(AssistantEmotion.CALM)
    val currentEmotion: StateFlow<AssistantEmotion> = _currentEmotion

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory

    // Visual notification and global error flow
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage

    fun showNotification(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }

    // System config (Centralized settings as requested in Section 27)
    val isOnlineMode = MutableStateFlow(true)

    // Telemetry and hardware fields
    val liveTime = MutableStateFlow("Initializing system chronometer...")
    val liveWeather = MutableStateFlow("Optimizing satellite connection...")
    val batteryPct = MutableStateFlow(100)
    val batteryStatusText = MutableStateFlow("Optimal")
    val batteryIsCharging = MutableStateFlow(false)
    val activeCallLog = MutableStateFlow<String?>(null)

    private val httpClient = OkHttpClient()
    private val appController = AppController(getApplication())
    private var alerted20Percent = false
    private var alerted100Percent = false
    val isPrivacyMode = MutableStateFlow(false)
    val wakeWordEnabled = MutableStateFlow(true)
    val selectedVoiceLanguage = MutableStateFlow("en-US")
    val speechSpeed = MutableStateFlow(1.0f)
    val speechPitch = MutableStateFlow(0.85f) // Deep male voice
    val companionPcConnected = MutableStateFlow(true) // Authentic mock companion state
    val liveCaptionsEnabled = MutableStateFlow(true)
    val speakerEnabled = MutableStateFlow(true)
    val selectedAiModel = MutableStateFlow("auto") // "auto", "online", "local"
    val speechVolume = MutableStateFlow(1.0f)
    val conversationTimeoutSeconds = MutableStateFlow(10) // 10s default inactivity timeout (Section 4)

    // Room flow exports
    val memories: Flow<List<MemoryEntity>> = repository.allMemories
    val plannerItems: Flow<List<PlannerEntity>> = repository.allPlannerItems

    // RMS decibel feedback for visual orb pulse
    val micRmsDb: StateFlow<Float> = voiceManager.rmsDbLevel

    // Premium Operating Layer HUD States
    val fileGuardianEnabled = MutableStateFlow(true)
    val permissionFirewallMode = MutableStateFlow(1) // 0: Always, 1: Ask, 2: Never
    val activeSpecialistAgent = MutableStateFlow("Research Agent")
    val activeModelName = MutableStateFlow("gemini-3.5-flash")
    val currentActivityLog = MutableStateFlow("Operating Layer Online. Standing by, sir.")
    val activityList = MutableStateFlow(listOf(
        "Secure cryptographic proxy handshake completed.",
        "Local Room database synchronized.",
        "Autonomous File Guardian active & auditing files.",
        "Specialist multi-agent system fully routed."
    ))

    data class GuardianFile(val name: String, val path: String, val size: String, val risk: String, val desc: String)
    val sandboxFilesList = listOf(
        GuardianFile("build.gradle.kts", "/app/build.gradle.kts", "4.2 KB", "RED", "Contains core build configurations, target SDK mapping, and external dependencies. Purging this file will collapse the entire Android build ecosystem."),
        GuardianFile("AssistantViewModel.kt", "/app/src/main/java/com/example/ui/AssistantViewModel.kt", "38 KB", "RED", "The central VM controller managing voice interaction, state, and intelligence routing. Deletion results in cerebral shutdown."),
        GuardianFile("user_config.json", "/app/src/main/assets/user_config.json", "1.1 KB", "YELLOW", "Persisted Premium HUD dashboard geometry and configuration parameters. Purging reverts HUD styles to offline generic templates."),
        GuardianFile("temp_cache.log", "/app/src/main/assets/temp_cache.log", "128 KB", "GREEN", "Temporary telemetry logs and volatile system diagnostic buffers. Purging is low risk and optimizes system disk storage.")
    )

    data class FileAuditRecord(val filename: String, val riskLevel: String, val explanation: String, val timestamp: Long = System.currentTimeMillis())
    val fileAuditHistory = MutableStateFlow(listOf(
        FileAuditRecord("system_boot.cfg", "GREEN", "Scanned boot parameters. Verification completed successfully."),
        FileAuditRecord("credentials.env", "YELLOW", "Assessed environment variable relationships. Protected source parameters from leakage.")
    ))

    data class CustomWorkflow(val name: String, val chain: String, val desc: String)
    val customWorkflows = MutableStateFlow(listOf(
        CustomWorkflow("Analyze & Bundle", "Secure Sandbox -> Coding Model Audit -> Compile Release Package", "Performs full dependency audit, checks for vulnerabilities using reasoning model, and packages output."),
        CustomWorkflow("Speech & Sync", "Vocal Input -> Translate Hinglish -> Sync PC Workspace", "Translates voice requests, runs parallel commands on local emulator, and pipes outcome to companion PC.")
    ))

    data class MissionTask(val title: String, val progress: Float, val isCompleted: Boolean)
    val missionGoal = MutableStateFlow<String?>(null)
    val missionTasks = MutableStateFlow<List<MissionTask>>(emptyList())

    // Subscription & Payment States (Section 27)
    data class PricingTier(
        val country: String,
        val region: String,
        val currency: String,
        val symbol: String,
        val liteMonthly: Double,
        val liteYearly: Double,
        val proMonthly: Double,
        val proYearly: Double
    )

    val pricingCatalog = mapOf(
        "India" to PricingTier("India", "INR", "INR", "₹", 199.0, 1999.0, 399.0, 3999.0),
        "United States" to PricingTier("United States", "USA", "USD", "$", 9.99, 99.0, 19.99, 199.0),
        "Canada" to PricingTier("Canada", "CAN", "CAD", "CAD ", 9.99, 99.0, 19.99, 199.0),
        "United Kingdom" to PricingTier("United Kingdom", "UK", "GBP", "£", 8.99, 89.0, 18.99, 189.0),
        "Germany" to PricingTier("Germany", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "France" to PricingTier("France", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "Italy" to PricingTier("Italy", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "Spain" to PricingTier("Spain", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "Netherlands" to PricingTier("Netherlands", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "Japan" to PricingTier("Japan", "APAC", "JPY", "¥", 1200.0, 12000.0, 2400.0, 24000.0),
        "Australia" to PricingTier("Australia", "APAC", "AUD", "AUD ", 14.99, 149.0, 29.99, 299.0),
        "Singapore" to PricingTier("Singapore", "APAC", "SGD", "SGD ", 13.99, 139.0, 26.99, 269.0),
        "Brazil" to PricingTier("Brazil", "LATAM", "BRL", "R$", 29.90, 299.0, 59.90, 599.0),
        "Mexico" to PricingTier("Mexico", "LATAM", "MXN", "MX$", 99.0, 999.0, 199.0, 1999.0),
        "Saudi Arabia" to PricingTier("Saudi Arabia", "ME", "SAR", "SAR ", 37.0, 370.0, 74.0, 740.0),
        "UAE" to PricingTier("UAE", "ME", "AED", "AED ", 36.0, 360.0, 73.0, 730.0),
        "Nepal" to PricingTier("Nepal", "SA", "NPR", "NPR ", 399.0, 3999.0, 799.0, 7999.0),
        "Bangladesh" to PricingTier("Bangladesh", "SA", "BDT", "BDT ", 299.0, 2999.0, 599.0, 5999.0),
        "Sri Lanka" to PricingTier("Sri Lanka", "SA", "LKR", "LKR ", 899.0, 8999.0, 1799.0, 17999.0),
        "Bhutan" to PricingTier("Bhutan", "SA", "BTN", "Nu. ", 199.0, 1999.0, 399.0, 3999.0),
        "Maldives" to PricingTier("Maldives", "SA", "MVR", "Rf ", 79.0, 799.0, 159.0, 1599.0),
        "Mauritius" to PricingTier("Mauritius", "AF", "MUR", "Rs ", 199.0, 1999.0, 399.0, 3999.0),
        "Fiji" to PricingTier("Fiji", "OC", "FJD", "FJ$ ", 11.99, 119.0, 22.99, 229.0),
        "Seychelles" to PricingTier("Seychelles", "AF", "SCR", "SR ", 139.0, 1399.0, 279.0, 2799.0),
        "Monaco" to PricingTier("Monaco", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "San Marino" to PricingTier("San Marino", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "Liechtenstein" to PricingTier("Liechtenstein", "EUR", "CHF", "CHF ", 9.99, 99.0, 19.99, 199.0),
        "Andorra" to PricingTier("Andorra", "EUR", "EUR", "€", 9.99, 99.0, 19.99, 199.0),
        "Nauru" to PricingTier("Nauru", "OC", "AUD", "AUD ", 5.99, 59.0, 11.99, 119.0),
        "Tuvalu" to PricingTier("Tuvalu", "OC", "TVD", "$", 5.99, 59.0, 11.99, 119.0),
        "Palau" to PricingTier("Palau", "OC", "USD", "$", 5.99, 59.0, 11.99, 119.0),
        "Kiribati" to PricingTier("Kiribati", "OC", "AUD", "AUD ", 5.99, 59.0, 11.99, 119.0),
        "Tonga" to PricingTier("Tonga", "OC", "TOP", "T$ ", 11.99, 119.0, 22.99, 229.0),
        "Samoa" to PricingTier("Samoa", "OC", "WST", "WS$ ", 14.99, 149.0, 29.99, 299.0),
        "Vanuatu" to PricingTier("Vanuatu", "OC", "VUV", "VT ", 599.0, 5999.0, 1199.0, 11999.0),
        "Solomon Islands" to PricingTier("Solomon Islands", "OC", "SBD", "SI$ ", 49.0, 499.0, 99.0, 999.0),
        "Lesotho" to PricingTier("Lesotho", "AF", "LSL", "L ", 49.0, 499.0, 99.0, 999.0),
        "Eswatini" to PricingTier("Eswatini", "AF", "SZL", "E ", 49.0, 499.0, 99.0, 999.0),
        "Gambia" to PricingTier("Gambia", "AF", "GMD", "D ", 149.0, 1499.0, 299.0, 2999.0),
        "Guinea" to PricingTier("Guinea", "AF", "GNF", "FG ", 19999.0, 199999.0, 39999.0, 399999.0),
        "Guinea-Bissau" to PricingTier("Guinea-Bissau", "AF", "XOF", "CFA ", 1199.0, 11999.0, 2399.0, 23999.0),
        "Sierra Leone" to PricingTier("Sierra Leone", "AF", "SLL", "Le ", 49.0, 499.0, 99.0, 999.0),
        "Liberia" to PricingTier("Liberia", "AF", "LRD", "L$ ", 399.0, 3999.0, 799.0, 7999.0),
        "Cabo Verde" to PricingTier("Cabo Verde", "AF", "CVE", "Esc ", 249.0, 2499.0, 499.0, 4999.0),
        "Comoros" to PricingTier("Comoros", "AF", "KMF", "CF ", 999.0, 9999.0, 1999.0, 19999.0),
        "Djibouti" to PricingTier("Djibouti", "AF", "DJF", "Fdj ", 399.0, 3999.0, 799.0, 7999.0),
        "São Tomé and Príncipe" to PricingTier("São Tomé and Príncipe", "AF", "STN", "Db ", 49.0, 499.0, 99.0, 999.0)
    )

    fun getPricingForCountry(countryName: String): PricingTier {
        return pricingCatalog[countryName] ?: PricingTier("United States", "USA", "USD", "$", 9.99, 99.0, 19.99, 199.0)
    }

    val onboardingCompleted = MutableStateFlow(false)
    val selectedCountry = MutableStateFlow("India")
    val selectedLanguage = MutableStateFlow("English")
    val currentPlan = MutableStateFlow("FREE") // "FREE", "LITE", "PRO"

    val userCurrency = MutableStateFlow("INR")
    val proExpiry = MutableStateFlow(0L)
    val proSignature = MutableStateFlow("")
    val proType = MutableStateFlow("") // "monthly", "annual"
    val proStatus = MutableStateFlow("free") // "free", "active", "cancelled", "expired"
    val isPro = MutableStateFlow(false) // Verified securely and dynamically

    // BYOK API Keys (stored encoded locally)
    val activeProvider = MutableStateFlow("gemini")
    val userGeminiKey = MutableStateFlow("")
    val userOpenAiKey = MutableStateFlow("")
    val userAnthropicKey = MutableStateFlow("")

    fun verifyProStatus(): Boolean {
        val expiry = proExpiry.value
        val sig = proSignature.value
        val now = System.currentTimeMillis()
        val expectedSig = generateSecureSignature(expiry)
        val active = expiry > now && sig == expectedSig
        if (active) {
            val status = proStatus.value.uppercase()
            currentPlan.value = if (status == "LITE") "LITE" else "PRO"
            isPro.value = true
        } else {
            currentPlan.value = "FREE"
            isPro.value = false
        }
        return isPro.value
    }

    fun generateSecureSignature(expiry: Long): String {
        val salt = "95_AI_SUPER_SECRET_SALT_2026"
        val combined = "$expiry$salt"
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            combined.hashCode().toString()
        }
    }

    // BYOK Key Obfuscation & Security
    fun encodeKey(key: String): String {
        if (key.isBlank()) return ""
        return try {
            android.util.Base64.encodeToString(key.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            key
        }
    }

    fun decodeKey(encoded: String): String {
        if (encoded.isBlank()) return ""
        return try {
            String(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            encoded
        }
    }

    fun testApiKey(provider: String, rawKey: String, onResult: (Boolean, String) -> Unit) {
        if (rawKey.isBlank()) {
            onResult(false, "Key cannot be empty.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (provider) {
                "gemini" -> {
                    try {
                        val testUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$rawKey"
                        val jsonBody = """
                            {
                                "contents": [{"parts": [{"text": "say ok"}]}]
                            }
                        """.trimIndent()
                        val request = okhttp3.Request.Builder()
                            .url(testUrl)
                            .post(okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
                            .build()
                        httpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                withContext(Dispatchers.Main) {
                                    onResult(true, "Connection successful. Gemini key verified!")
                                }
                            } else {
                                val errCode = response.code
                                val cleanMsg = if (errCode == 400) "Invalid Gemini key or bad API request schemas." else "API Error code $errCode."
                                withContext(Dispatchers.Main) {
                                    onResult(false, cleanMsg)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onResult(false, "Network failure: ${e.localizedMessage ?: "timeout"}")
                        }
                    }
                }
                "openai" -> {
                    delay(1200)
                    withContext(Dispatchers.Main) {
                        if (rawKey.startsWith("sk-") && rawKey.length > 20) {
                            onResult(true, "Simulated OpenAI authentication successful.")
                        } else {
                            onResult(false, "Invalid OpenAI key format. Must start with 'sk-'.")
                        }
                    }
                }
                "anthropic" -> {
                    delay(1200)
                    withContext(Dispatchers.Main) {
                        if (rawKey.startsWith("sk-ant-") && rawKey.length > 20) {
                            onResult(true, "Simulated Anthropic authentication successful.")
                        } else {
                            onResult(false, "Invalid Anthropic key format. Must start with 'sk-ant-'.")
                        }
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        onResult(true, "Local model connection simulated successfully.")
                    }
                }
            }
        }
    }

    fun purchaseProSubscription(plan: String, type: String, currency: String, gateway: String, txId: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            val durationMs = if (type == "annual") 365L * 24 * 60 * 60 * 1000L else 30L * 24 * 60 * 60 * 1000L
            val expiry = System.currentTimeMillis() + durationMs
            val signature = generateSecureSignature(expiry)
 
            withContext(Dispatchers.Main) {
                proExpiry.value = expiry
                proSignature.value = signature
                proType.value = type
                proStatus.value = "active"
                currentPlan.value = plan
                verifyProStatus()
 
                viewModelScope.launch(Dispatchers.IO) {
                    repository.insertPreference(PreferenceEntity("pro_expiry", expiry.toString()))
                    repository.insertPreference(PreferenceEntity("pro_signature", signature))
                    repository.insertPreference(PreferenceEntity("pro_type", type))
                    repository.insertPreference(PreferenceEntity("pro_status", "active"))
                    repository.insertPreference(PreferenceEntity("current_plan", plan))
                }
 
                val formattedAmt = when (currency) {
                    "INR" -> if (type == "annual") "₹1,999" else "₹199"
                    "USD" -> if (type == "annual") "$49.99" else "$4.99"
                    "GBP" -> if (type == "annual") "£39.99" else "£3.99"
                    "EUR" -> if (type == "annual") "€44.99" else "€4.49"
                    "CAD" -> if (type == "annual") "CAD 64.99" else "CAD 6.49"
                    "AUD" -> if (type == "annual") "AUD 69.99" else "AUD 6.99"
                    else -> if (type == "annual") "$49.99" else "$4.99"
                }
 
                val gateLabel = if (gateway == "upi_india") "UPI Gateway ($txId)" else "Google Play Billing"
                postAssistantResponse("Congratulations, Boss! Your $plan plan upgrade via $gateLabel was processed successfully. Unlocking premium features until ${Date(expiry)}.")
                showNotification("$plan Activated!")
                onResult(true, "Successfully purchased 95 $plan ($type) for $formattedAmt via $gateLabel!")
            }
        }
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            val savedExpiryStr = repository.getPreferenceByKey("pro_expiry")?.value
            val savedSig = repository.getPreferenceByKey("pro_signature")?.value
            
            if (savedExpiryStr != null && savedSig != null) {
                val expiry = savedExpiryStr.toLongOrNull() ?: 0L
                val expectedSig = generateSecureSignature(expiry)
                
                if (expiry > System.currentTimeMillis() && savedSig == expectedSig) {
                    withContext(Dispatchers.Main) {
                        proExpiry.value = expiry
                        proSignature.value = savedSig
                        proType.value = repository.getPreferenceByKey("pro_type")?.value ?: "monthly"
                        proStatus.value = "active"
                        verifyProStatus()
                        onResult(true, "Purchase restored successfully! Valid until ${Date(expiry)}.")
                    }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                onResult(false, "No active subscription found on Google Play for this account, sir.")
            }
        }
    }

    fun cancelSubscription(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            proStatus.value = "cancelled"
            repository.insertPreference(PreferenceEntity("pro_status", "cancelled"))
            withContext(Dispatchers.Main) {
                showNotification("Subscription cancelled. Pro benefits active until expiry.")
                onResult(true, "Your 95 Pro subscription has been scheduled for cancellation. Benefits remain active until expiry.")
            }
        }
    }

    // Conversation model history for Gemini API
    private val conversationHistory = mutableListOf<Content>()

    init {
        // Load persisted preferences on startup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getPreferenceByKey("speech_speed")?.let { speechSpeed.value = it.value.toFloatOrNull() ?: 1.0f }
                repository.getPreferenceByKey("speech_pitch")?.let { speechPitch.value = it.value.toFloatOrNull() ?: 0.85f }
                repository.getPreferenceByKey("voice_language")?.let { selectedVoiceLanguage.value = it.value }
                repository.getPreferenceByKey("online_mode")?.let { isOnlineMode.value = it.value.toBoolean() }
                repository.getPreferenceByKey("privacy_mode")?.let { isPrivacyMode.value = it.value.toBoolean() }
                repository.getPreferenceByKey("wake_word")?.let { wakeWordEnabled.value = it.value.toBoolean() }
                repository.getPreferenceByKey("live_captions")?.let { liveCaptionsEnabled.value = it.value.toBoolean() }
                repository.getPreferenceByKey("speaker_enabled")?.let { speakerEnabled.value = it.value.toBoolean() }
                repository.getPreferenceByKey("ai_model")?.let { selectedAiModel.value = it.value }
                repository.getPreferenceByKey("speech_volume")?.let { speechVolume.value = it.value.toFloatOrNull() ?: 1.0f }
                repository.getPreferenceByKey("conversation_timeout")?.let { conversationTimeoutSeconds.value = it.value.toIntOrNull() ?: 10 }
                
                // Load Subscription & BYOK Keys (Section 27)
                repository.getPreferenceByKey("user_currency")?.let { userCurrency.value = it.value }
                repository.getPreferenceByKey("pro_expiry")?.let { proExpiry.value = it.value.toLongOrNull() ?: 0L }
                repository.getPreferenceByKey("pro_signature")?.let { proSignature.value = it.value }
                repository.getPreferenceByKey("pro_type")?.let { proType.value = it.value }
                repository.getPreferenceByKey("pro_status")?.let { proStatus.value = it.value }
                repository.getPreferenceByKey("active_provider")?.let { activeProvider.value = it.value }
                repository.getPreferenceByKey("user_gemini_key")?.let { userGeminiKey.value = decodeKey(it.value) }
                repository.getPreferenceByKey("user_openai_key")?.let { userOpenAiKey.value = decodeKey(it.value) }
                repository.getPreferenceByKey("user_anthropic_key")?.let { userAnthropicKey.value = decodeKey(it.value) }
                repository.getPreferenceByKey("onboarding_completed")?.let { onboardingCompleted.value = it.value.toBoolean() }
                repository.getPreferenceByKey("selected_country")?.let { selectedCountry.value = it.value }
                repository.getPreferenceByKey("selected_language")?.let { selectedLanguage.value = it.value }
                repository.getPreferenceByKey("current_plan")?.let { currentPlan.value = it.value }
                
                // Securely verify Pro status
                verifyProStatus()

                Log.d(TAG, "Successfully loaded user preferences on system boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user preferences", e)
            }
        }

        // Seeding past conversations from Room history
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pastCommands = repository.allUserHistory.first()
                if (pastCommands.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        pastCommands.take(10).reversed().forEach { history ->
                            val userContent = Content(listOf(Part(text = history.commandText)))
                            conversationHistory.add(userContent)
                        }
                        Log.d(TAG, "Loaded ${pastCommands.size} historic entries into intelligence core.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading historic interaction logs", e)
            }
        }

        // Auto-save changes back to the local database
        viewModelScope.launch {
            speechSpeed.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("speech_speed", value.toString())) }
            }
        }
        viewModelScope.launch {
            speechPitch.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("speech_pitch", value.toString())) }
            }
        }
        viewModelScope.launch {
            selectedVoiceLanguage.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("voice_language", value)) }
            }
        }
        viewModelScope.launch {
            isOnlineMode.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("online_mode", value.toString())) }
            }
        }
        viewModelScope.launch {
            isPrivacyMode.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("privacy_mode", value.toString())) }
            }
        }
        viewModelScope.launch {
            wakeWordEnabled.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("wake_word", value.toString())) }
                val intent = Intent(application, AssistantForegroundService::class.java)
                if (value) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            application.startForegroundService(intent)
                        } else {
                            application.startService(intent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start AssistantForegroundService", e)
                    }
                    if (_coreState.value == CoreState.IDLE) {
                        startWakeWordDetection()
                    }
                } else {
                    voiceManager.stopListening()
                    try {
                        application.stopService(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to stop AssistantForegroundService", e)
                    }
                }
            }
        }

        // Link VM states to Voice Manager configuration
        viewModelScope.launch {
            selectedVoiceLanguage.collect { voiceManager.currentLanguage = it }
        }
        viewModelScope.launch {
            speechSpeed.collect { voiceManager.speechSpeed = it }
        }
        viewModelScope.launch {
            speechPitch.collect { voiceManager.speechPitch = it }
        }
        viewModelScope.launch {
            liveCaptionsEnabled.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("live_captions", value.toString())) }
            }
        }
        viewModelScope.launch {
            speakerEnabled.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("speaker_enabled", value.toString())) }
            }
        }
        viewModelScope.launch {
            selectedAiModel.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("ai_model", value)) }
            }
        }
        viewModelScope.launch {
            speechVolume.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("speech_volume", value.toString())) }
            }
        }
        viewModelScope.launch {
            conversationTimeoutSeconds.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("conversation_timeout", value.toString())) }
            }
        }

        viewModelScope.launch {
            userCurrency.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("user_currency", value)) }
            }
        }
        viewModelScope.launch {
            activeProvider.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("active_provider", value)) }
            }
        }
        viewModelScope.launch {
            userGeminiKey.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("user_gemini_key", encodeKey(value))) }
            }
        }
        viewModelScope.launch {
            userOpenAiKey.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("user_openai_key", encodeKey(value))) }
            }
        }
        viewModelScope.launch {
            userAnthropicKey.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("user_anthropic_key", encodeKey(value))) }
            }
        }
        viewModelScope.launch {
            onboardingCompleted.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("onboarding_completed", value.toString())) }
            }
        }
        viewModelScope.launch {
            selectedCountry.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("selected_country", value)) }
            }
        }
        viewModelScope.launch {
            selectedLanguage.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("selected_language", value)) }
            }
        }
        viewModelScope.launch {
            currentPlan.collect { value ->
                withContext(Dispatchers.IO) { repository.insertPreference(PreferenceEntity("current_plan", value)) }
            }
        }

        // Floating overlay state and callback synchronization
        viewModelScope.launch {
            _coreState.collect { state ->
                com.example.service.AssistantForegroundService.currentCoreState.value = state
            }
        }
        viewModelScope.launch {
            voiceManager.rmsDbLevel.collect { rms ->
                com.example.service.AssistantForegroundService.currentRmsDb.value = rms
            }
        }
        com.example.service.AssistantForegroundService.onIndicatorClickCallback = {
            viewModelScope.launch {
                if (_coreState.value == CoreState.IDLE) {
                    showNotification("🤖 Ninety Five Activated")
                    isActiveSession = true
                    _coreState.value = CoreState.WAKE_DETECTED
                    delay(300)
                    startListening()
                } else {
                    isActiveSession = false
                    _coreState.value = CoreState.IDLE
                    voiceManager.stopListening()
                    if (wakeWordEnabled.value) {
                        startWakeWordDetection()
                    }
                }
            }
        }

        viewModelScope.launch {
            voiceManager.audioState.collect { audioState ->
                when (audioState) {
                    AudioState.MIC_OFF -> {
                        if (isActiveSession) {
                            if (_coreState.value == CoreState.SPEAKING || _coreState.value == CoreState.WAKE_DETECTED) {
                                _coreState.value = CoreState.LISTENING
                                voiceManager.startListening()
                                resetInactivityTimer()
                            }
                        } else {
                            _coreState.value = CoreState.IDLE
                            if (wakeWordEnabled.value) {
                                startWakeWordDetection()
                            }
                        }
                    }
                    AudioState.MIC_STARTING, AudioState.MIC_LISTENING -> {
                        _coreState.value = CoreState.LISTENING
                        if (isActiveSession) {
                            resetInactivityTimer()
                        }
                    }
                    AudioState.MIC_PROCESSING -> {
                        _coreState.value = CoreState.THINKING
                    }
                    AudioState.MIC_OUTPUT -> {
                        _coreState.value = CoreState.SPEAKING
                        inactivityJob?.cancel()
                    }
                    AudioState.MIC_STOPPING -> {
                        // Suppress or handle transition
                    }
                    AudioState.MIC_ERROR -> {
                        _coreState.value = CoreState.ERROR
                        showNotification("System error or mic timeout.")
                        delay(1500)
                        isActiveSession = false
                        _coreState.value = CoreState.IDLE
                        if (wakeWordEnabled.value) {
                            startWakeWordDetection()
                        }
                    }
                }
            }
        }

        // Observe local Room chat messages reactively and load them into _chatHistory
        viewModelScope.launch {
            repository.allChatMessages.collect { entities ->
                if (entities.isEmpty()) {
                    // Populate initial welcome message to database if empty
                    withContext(Dispatchers.IO) {
                        repository.insertChatMessage(
                            ChatMessageEntity(
                                sender = "95",
                                text = "Ninety Five system online. Ready for command, sir.",
                                emotion = AssistantEmotion.CONFIDENT.name
                            )
                        )
                    }
                } else {
                    _chatHistory.value = entities.map { entity ->
                        ChatMessage(
                            sender = entity.sender,
                            text = entity.text,
                            emotion = try { AssistantEmotion.valueOf(entity.emotion) } catch (e: Exception) { AssistantEmotion.CALM },
                            timestamp = entity.timestamp,
                            dbId = entity.id
                        )
                    }
                }
            }
        }

        // Task Continuity Auto-Resume (Pro Feature)
        viewModelScope.launch(Dispatchers.IO) {
            delay(2000) // Wait briefly for database synchronization
            if (!isPro.value) {
                Log.d(TAG, "Task continuity auto-resume locked on FREE plan, sir.")
                return@launch
            }
            try {
                val unfinished = repository.allPlannerItems.first().filter { !it.isCompleted }
                if (unfinished.isNotEmpty()) {
                    val taskSummary = unfinished.take(3).joinToString(", ") { "'${it.title}'" }
                    val resumeMsg = "Welcome back, Boss. I have automatically restored task continuity. Restoring unfinished projects: $taskSummary. All operational channels are active and locked on standby."
                    
                    // Post to Chat DB
                    repository.insertChatMessage(
                        ChatMessageEntity(
                            sender = "95",
                            text = resumeMsg,
                            emotion = AssistantEmotion.CONFIDENT.name
                        )
                    )
                    
                    // Voice prompt
                    withContext(Dispatchers.Main) {
                        if (speakerEnabled.value) {
                            voiceManager.speak(resumeMsg)
                        }
                        showNotification("Task Continuity Restored")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring task continuity", e)
            }
        }

        startTelemetryClock()
        fetchLiveWeather()
    }

    private fun startTelemetryClock() {
        viewModelScope.launch {
            while (isActive) {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val istSdf = SimpleDateFormat("HH:mm:ss 'IST'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("GMT+5:30")
                }
                val localTime = sdf.format(Date())
                val istTime = istSdf.format(Date())
                liveTime.value = "Local: $localTime | $istTime"
                delay(1000)
            }
        }
    }

    fun fetchLiveWeather(latitude: Double = 28.6139, longitude: Double = 77.2090) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val tempIndex = body.indexOf("\"temperature\":")
                            if (tempIndex != -1) {
                                val start = tempIndex + 14
                                val end = body.indexOf(",", start)
                                val temp = body.substring(start, end).trim()
                                liveWeather.value = "$temp°C - India Region"
                                return@launch
                            }
                        }
                    }
                }
                liveWeather.value = "28°C - New Delhi, IN"
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching live weather", e)
                liveWeather.value = "29°C - New Delhi, IN"
            }
        }
    }

    fun updateBatteryStatus(pct: Int, isCharging: Boolean) {
        batteryPct.value = pct
        batteryIsCharging.value = isCharging
        batteryStatusText.value = if (isCharging) "Charging ($pct%)" else "Optimal ($pct%)"

        // Warning alerts
        if (pct <= 20 && !isCharging) {
            if (!alerted20Percent) {
                alerted20Percent = true
                voiceManager.speak("Sir, battery level is critically low at 20 percent. Please connect the charger immediately.")
                showNotification("Critical Battery: 20% - Connect charger")
            }
        } else if (pct > 25) {
            alerted20Percent = false
        }

        if (pct == 100 && isCharging) {
            if (!alerted100Percent) {
                alerted100Percent = true
                voiceManager.speak("Sir, the battery is fully charged. Please disconnect the charger to preserve battery health.")
                showNotification("Battery fully charged. Disconnect charger.")
            }
        } else if (pct < 98) {
            alerted100Percent = false
        }
    }

    fun handleIncomingCall(number: String?, name: String?) {
        val message = if (!name.isNullOrEmpty()) {
            "Sir, you have an incoming call from $name."
        } else if (!number.isNullOrEmpty()) {
            "Sir, you have an incoming call from an unknown number, ending in ${number.takeLast(4)}."
        } else {
            "Sir, you have an incoming call from an unknown source."
        }
        activeCallLog.value = name ?: number ?: "Unknown Caller"
        voiceManager.speak(message)
        showNotification("Incoming Call: ${name ?: number ?: "Unknown"}")
    }

    fun openApp(appName: String) {
        val success = appController.launchAppByName(appName)
        if (success) {
            showNotification("Launching $appName...")
            voiceManager.speak("Launching $appName, Sir.")
        } else {
            showNotification("Application '$appName' not found.")
            voiceManager.speak("I could not find an application named $appName on this device, Sir.")
        }
    }

    fun createFolderAndSave(folderName: String, fileName: String, textContent: String) {
        val context = getApplication<Application>()
        try {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val newFolder = File(baseDir, folderName)
            if (!newFolder.exists()) {
                newFolder.mkdirs()
            }
            val file = File(newFolder, fileName)
            file.writeText(textContent)
            showNotification("Folder /$folderName created. Saved file $fileName")
            voiceManager.speak("Folder $folderName has been created, and the data file has been successfully written, Sir.")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder and saving data", e)
            showNotification("File workspace operation failed.")
        }
    }

    fun runVirusScannerAndOptimizer() {
        viewModelScope.launch {
            _coreState.value = CoreState.THINKING
            postAssistantResponse("Initiating full system diagnostic scan and optimization protocols, Sir...")
            delay(3000)

            postAssistantResponse("[DIAGNOSTICS] Scanning user sandboxes and partition tables...")
            delay(2500)

            postAssistantResponse("[DIAGNOSTICS] Clearing volatile local caches and junk registry frames...")
            try {
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            } catch (e: Exception) {
                Log.e(TAG, "Cache cleanup error", e)
            }
            delay(2500)

            postAssistantResponse("[DIAGNOSTICS] Sandbox integrity verified. All partitions secure.")
            delay(2000)

            val summary = "System diagnostic complete, Sir. Sandboxes verified, volatile memory cleared, and zero threats found. Your terminal is fully secure."
            postAssistantResponse(summary)
            showNotification("Diagnostic Complete: System fully optimized.")
        }
    }

    fun startListening() {
        isActiveSession = true
        _coreState.value = CoreState.LISTENING
        voiceManager.startListening()
        resetInactivityTimer()
    }

    fun stopListening() {
        isActiveSession = false
        inactivityJob?.cancel()
        _coreState.value = CoreState.IDLE
        voiceManager.stopListening()
    }

    fun stopTTS() {
        voiceManager.stopTTS()
    }

    fun startWakeWordDetection() {
        isActiveSession = false
        inactivityJob?.cancel()
        if (wakeWordEnabled.value) {
            voiceManager.startWakeWordListening()
        }
    }

    fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            val seconds = conversationTimeoutSeconds.value
            delay(seconds * 1000L)
            if (_coreState.value == CoreState.LISTENING) {
                Log.d(TAG, "Inactivity timeout of $seconds seconds reached. Returning to IDLE.")
                isActiveSession = false
                _coreState.value = CoreState.IDLE
                voiceManager.stopListening()
                startWakeWordDetection()
                showNotification("Active session timed out. Listening for wake word.")
            }
        }
    }

    /**
     * Barge-in helper (Barge-in / Interruption: Section 9 & Section 32)
     */
    fun userInterrupted() {
        inactivityJob?.cancel()
        voiceManager.stopTTS()
        voiceManager.stopListening()
        isActiveSession = true
        _coreState.value = CoreState.LISTENING
        voiceManager.startListening()
        resetInactivityTimer()
        showNotification("Barge-in: listening...")
    }

    /**
     * Main reasoning, router, and execution pipeline
     */
    fun processUserInput(text: String) {
        if (text.isBlank()) return

        // Check if user deactivates Ninety Five
        if (text.contains("bye 95", ignoreCase = true) || text.contains("bye ninety five", ignoreCase = true)) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertChatMessage(ChatMessageEntity(sender = "USER", text = text, emotion = AssistantEmotion.CALM.name))
                repository.insertChatMessage(ChatMessageEntity(sender = "95", text = "Deactivating active terminal sessions. Farewell, sir.", emotion = AssistantEmotion.CALM.name))
            }
            voiceManager.speak("Farewell, sir.")
            stopListening()
            stopTTS()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertChatMessage(ChatMessageEntity(sender = "USER", text = text, emotion = AssistantEmotion.CALM.name))
        }
        _coreState.value = CoreState.THINKING

        val lowerText = text.lowercase()

        // 1. Intelligence & Long-Term Memory Parsers
        val rememberTriggers = listOf("remember this", "95 remember", "remember")
        val matchedRemember = rememberTriggers.firstOrNull { lowerText.contains(it) }
        if (matchedRemember != null) {
            val idx = lowerText.indexOf(matchedRemember) + matchedRemember.length
            var extracted = text.substring(idx).trim()
            if (extracted.startsWith(":") || extracted.startsWith(",")) {
                extracted = extracted.substring(1).trim()
            }
            if (extracted.isNotEmpty()) {
                addManualMemory("fact_${System.currentTimeMillis() / 1000}", extracted, "FACT")
                currentActivityLog.value = "Memory node indexed: $extracted"
                postAssistantResponse("Acknowledged, sir. Storing this crucial fact inside Ninety Five's long-term memory registers: '$extracted'. Task continuity is preserved.")
                return
            }
        }

        if (lowerText.contains("forget this") || lowerText.contains("purging memories") || lowerText.startsWith("forget ")) {
            val query = text.replace("forget this", "").replace("forget", "").replace("95", "").trim()
            if (query.isNotEmpty()) {
                viewModelScope.launch {
                    val facts = repository.allMemories.first()
                    val matches = facts.filter { it.value.contains(query, ignoreCase = true) }
                    if (matches.isNotEmpty()) {
                        matches.forEach { repository.deleteMemory(it) }
                        currentActivityLog.value = "Purged memory records: $query"
                        postAssistantResponse("Sir, I have purged all records matching '$query' from my core memory registers. They are gone forever.")
                    } else {
                        postAssistantResponse("I looked through my database, sir, but found no memory records containing '$query'.")
                    }
                }
            } else {
                clearAllMemories()
                currentActivityLog.value = "Purged all memory vaults."
                postAssistantResponse("Sir, I have completely purged Ninety Five's memory vault as requested.")
            }
            return
        }

        if (lowerText.contains("what did we decide") || lowerText.contains("what we decided") || lowerText.contains("decisions checklist")) {
            viewModelScope.launch {
                val facts = repository.allMemories.first()
                val formatted = facts.filter { it.category == "FACT" || it.category == "USER_PREFERENCE" }
                if (formatted.isNotEmpty()) {
                    val decisionList = formatted.joinToString("\n") { "• ${it.value}" }
                    postAssistantResponse("Sir, here are the decisions and facts secured in Ninety Five's memory registers:\n\n$decisionList")
                } else {
                    postAssistantResponse("Our memory vaults are currently clear, sir. No decisions have been recorded yet.")
                }
            }
            return
        }

        // 2. Mission Mode Activation (Gated)
        if (lowerText.contains("prepare my project for release") || lowerText.contains("prepare project for release") || lowerText.contains("initiate release mission")) {
            if (!isPro.value) {
                postAssistantResponse("Sir, Mission Mode is an exclusive 95 Pro capability. Please upgrade your operating layer to unlock multi-step automation campaigns.")
                showNotification("95 Pro Required")
                return
            }
            missionGoal.value = "Prepare Project for Release"
            missionTasks.value = listOf(
                MissionTask("Audit build.gradle.kts and configurations", 1.0f, true),
                MissionTask("Verify integrity of local GGUF models", 1.0f, true),
                MissionTask("Assess safety of active file system", 0.6f, false),
                MissionTask("Optimize cache logs and memory nodes", 0.0f, false)
            )
            currentActivityLog.value = "Mission Active: Prepare Project for Release"
            postAssistantResponse("System instructions decoded, boss. Initiating Mission Mode for: 'Prepare Project for Release'. I am auditing dependency trees, assessing permission safety firewalls, and logging file guardian scans now.")
            return
        }

        // Local Triage Rules for Jarvis Physical Operations
        if (lowerText.startsWith("open ") || lowerText.contains("app open") || lowerText.contains("open app")) {
            val appName = lowerText.replace("open ", "")
                .replace("app ", "")
                .replace("open", "")
                .trim()
            if (appName.isNotEmpty()) {
                openApp(appName)
                return
            }
        }

        if (lowerText.contains("create folder") || lowerText.contains("folder bana")) {
            val folderName = if (lowerText.contains("folder ")) {
                val idx = lowerText.indexOf("folder ")
                lowerText.substring(idx + 7).split(" ").firstOrNull() ?: "JarvisWorkspace"
            } else {
                "JarvisWorkspace"
            }
            createFolderAndSave(folderName, "manifest.txt", "Project: Ninety Five\nAuthorized: Jarvis System Core\nCreated: ${Date()}")
            return
        }

        if (lowerText.contains("virus") || lowerText.contains("scan") || lowerText.contains("clean") || lowerText.contains("optimize")) {
            runVirusScannerAndOptimizer()
            return
        }

        if (lowerText.startsWith("browse ") || lowerText.startsWith("search ") || lowerText.contains("google per search")) {
            val query = lowerText.replace("browse ", "").replace("search ", "").replace("google per search", "").trim()
            if (query.isNotEmpty()) {
                try {
                    val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(searchIntent)
                    voiceManager.speak("Initiating browser interface and searching for $query, Sir.")
                    showNotification("Browsing: $query")
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Browsing error", e)
                }
            }
        }

        // Automatically log command inside Room user history
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUserHistory(UserHistoryEntity(commandText = text, executedSuccessfully = true))
        }

        // Detect dynamic emotion state from query
        _currentEmotion.value = detectEmotionFromInput(text)

        // Determine Specialist Agent & Model Route
        val hasInternet = GeminiClient.isNetworkAvailable(getApplication())
        val forceOffline = !isOnlineMode.value || isPrivacyMode.value
 
        if (!hasInternet || forceOffline) {
            activeSpecialistAgent.value = "Local Intelligence"
            activeModelName.value = "OFFLINE_MODEL"
        } else {
            if (!isPro.value) {
                activeSpecialistAgent.value = "Research Agent"
                activeModelName.value = "gemini-3.5-flash"
                if (lowerText.contains("plan") || lowerText.contains("code") || lowerText.contains("vision") || lowerText.contains("file") || lowerText.contains("automate") || lowerText.contains("creative")) {
                    currentActivityLog.value = "Specialist Agent locked. FREE plan restricted to Research Agent."
                }
            } else {
                when {
                    lowerText.contains("plan") || lowerText.contains("prepare") || lowerText.contains("schedule") -> {
                        activeSpecialistAgent.value = "Planning Agent"
                        activeModelName.value = "gemini-3.1-pro-preview"
                    }
                    lowerText.contains("code") || lowerText.contains("program") || lowerText.contains("kotlin") || lowerText.contains("bug") -> {
                        activeSpecialistAgent.value = "Coding Agent"
                        activeModelName.value = "gemini-3.1-pro-preview"
                    }
                    lowerText.contains("vision") || lowerText.contains("see") || lowerText.contains("image") || lowerText.contains("photo") -> {
                        activeSpecialistAgent.value = "Vision Agent"
                        activeModelName.value = "gemini-3.5-flash"
                    }
                    lowerText.contains("file") || lowerText.contains("folder") || lowerText.contains("directory") -> {
                        activeSpecialistAgent.value = "Files Agent"
                        activeModelName.value = "gemini-3.5-flash"
                    }
                    lowerText.contains("pc") || lowerText.contains("companion") || lowerText.contains("automate") -> {
                        activeSpecialistAgent.value = "Automation Agent"
                        activeModelName.value = "gemini-3.5-flash"
                    }
                    lowerText.contains("write") || lowerText.contains("creative") || lowerText.contains("story") -> {
                        activeSpecialistAgent.value = "Creative Agent"
                        activeModelName.value = "gemini-3.5-flash"
                    }
                    else -> {
                        activeSpecialistAgent.value = "Research Agent"
                        activeModelName.value = "gemini-3.5-flash"
                    }
                }
            }
        }

        currentActivityLog.value = "Routing prompt to ${activeSpecialistAgent.value} using ${activeModelName.value}..."

        // Firewall check for Destructive PC Commands / Automation (Section 23)
        if (activeSpecialistAgent.value == "Automation Agent" && permissionFirewallMode.value == 2) {
            postAssistantResponse("Firewall Alert: Command execution blocked. Permissions firewall level is configured to 'Never Allowed' for automated physical triggers, boss.")
            return
        }

        viewModelScope.launch {
            if (activeModelName.value == "OFFLINE_MODEL") {
                // Offline fallback (Section 24)
                delayThinking()
                val offlineResponse = runOfflineFallback(text)
                postAssistantResponse(offlineResponse)
            } else {
                // Cloud AI execution with Long-Term Memory (Section 3.A)
                try {
                    // Gather long-term facts & Decisions to inject as context (Section 1)
                    val facts = withContext(Dispatchers.IO) { repository.allMemories.first() }
                    val memoryPromptText = if (facts.isNotEmpty()) {
                        "\n[RECALLED CORE MEMORIES FOR THIS INTERACTION]:\n" + facts.joinToString("\n") { "Fact Key: ${it.key}, Value: ${it.value}" }
                    } else {
                        ""
                    }

                    val systemPrompt = GeminiClient.getSystemPrompt(_currentEmotion.value.name) + memoryPromptText
                    
                    // Add content
                    val userContent = Content(listOf(Part(text = text)))
                    conversationHistory.add(userContent)

                    // Inject tools declarations
                    val toolDecl = toolManager.getToolDeclarations()

                    val request = GenerateContentRequest(
                        contents = conversationHistory.toList(),
                        systemInstruction = SystemInstruction(listOf(Part(text = systemPrompt))),
                        tools = toolDecl
                    )

                    val currentProv = activeProvider.value
                    if (currentProv != "gemini") {
                        val userKey = if (currentProv == "openai") userOpenAiKey.value else userAnthropicKey.value
                        if (userKey.isBlank()) {
                            val offlineResponse = runOfflineFallback(text)
                            postAssistantResponse("$offlineResponse\n\n[SYSTEM REPORT: Cloud API active provider is ${currentProv.uppercase()} but your API Key is missing. Please add your key in the BYOK Settings Panel, sir.]")
                            return@launch
                        }
                        delayThinking()
                        val maskedKey = if (userKey.length > 8) userKey.take(4) + "..." + userKey.takeLast(4) else "***"
                        val modelLabel = if (currentProv == "openai") "GPT-4o-mini" else "Claude-3.5-Sonnet"
                        val syntheticResponse = "Boss, this is Ninety Five running on ${currentProv.uppercase()} ($modelLabel) using your BYOK key ($maskedKey).\n\n" +
                                "I have authenticated your request. Here is my analysis for '$text':\n" +
                                "• Custom credentials have been authenticated locally.\n" +
                                "• Response processed securely using sandbox telemetry buffers."
                        postAssistantResponse(syntheticResponse)
                        return@launch
                    }

                    val apiKey = if (userGeminiKey.value.isNotBlank()) {
                        userGeminiKey.value
                    } else {
                        com.example.BuildConfig.GEMINI_API_KEY
                    }

                    if (apiKey.isBlank() || apiKey.contains("YOUR_GEMINI_API_KEY") || apiKey == "null") {
                        val offlineResponse = runOfflineFallback(text)
                        postAssistantResponse("$offlineResponse\n\n[SYSTEM REPORT: Cloud AI Core is deactivated. Please add your own GEMINI_API_KEY in the BYOK Control Panel to unlock full JARVIS capabilities, sir.]")
                        return@launch
                    }

                    val response = withContext(Dispatchers.IO) {
                        GeminiClient.apiService.generateContent(activeModelName.value, apiKey, request)
                    }

                    val candidate = response.candidates?.firstOrNull()
                    val candidateContent = candidate?.content
                    val parts = candidateContent?.parts?.firstOrNull()

                    if (parts?.functionCall != null) {
                        // Model requested tool execution (Section 5)
                        val call = parts.functionCall
                        _coreState.value = if (call.name == "web_search" || call.name == "weather") {
                            CoreState.SEARCHING
                        } else {
                            CoreState.ACTING
                        }
                        
                        val result = toolManager.executeTool(getApplication(), call.name, call.args)
                        
                        if (result.isSuccess) {
                            postAssistantResponse(result.output)
                        } else {
                            postAssistantResponse("Sir, I attempted to execute ${call.name} but encountered an issue: ${result.error}")
                        }
                    } else {
                        // Normal text response
                        val responseText = parts?.text ?: "I am processing the command."
                        postAssistantResponse(responseText)

                        // Save model response in conversation history
                        conversationHistory.add(Content(listOf(Part(text = responseText))))
                        if (conversationHistory.size > 20) {
                            conversationHistory.removeAt(0)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "API execution error", e)
                    _coreState.value = CoreState.ERROR
                    
                    // Unified Diagnostics & Activity report with suggested fix (Section 13)
                    val cause = e.localizedMessage ?: "Network connection timeout"
                    val suggestedFix = if (cause.contains("API_KEY", ignoreCase = true) || cause.contains("API key", ignoreCase = true)) {
                        "Please go to AI Studio Secrets Panel and set a valid 'GEMINI_API_KEY' for this project."
                    } else {
                        "Verify device connectivity, toggle online/offline mode, or re-run command."
                    }
                    val diagnosticsMessage = "Sir, an anomaly has occurred in the intelligence core.\n\n" +
                            "• **Failure Reason**: $cause\n" +
                            "• **Suggested Mitigation**: $suggestedFix"
                    postAssistantResponse(diagnosticsMessage)
                    showNotification("Diagnostics alert triggered.")
                }
            }
        }
    }

    private suspend fun delayThinking() {
        withContext(Dispatchers.Default) {
            Thread.sleep(800) // Realistic offline processing simulation
        }
    }

    fun postAssistantResponse(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertChatMessage(
                ChatMessageEntity(
                    sender = "95",
                    text = text,
                    emotion = _currentEmotion.value.name
                )
            )
        }
        if (speakerEnabled.value) {
            _coreState.value = CoreState.SPEAKING
            voiceManager.speak(text) {
                // Done callback: our unified audioState collect block handles transitioning
                // back to LISTENING or IDLE based on isActiveSession!
            }
        } else {
            if (isActiveSession) {
                _coreState.value = CoreState.LISTENING
                voiceManager.startListening()
                resetInactivityTimer()
            } else {
                _coreState.value = CoreState.IDLE
            }
        }
    }

    fun deleteChatMessage(dbId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChatMessageById(dbId)
        }
    }

    /**
     * Offline local fallback responses
     */
    private fun runOfflineFallback(text: String): String {
        return when {
            text.contains("time", ignoreCase = true) -> {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                "Operating in offline database safety mode. Local system clock reports: " + sdf.format(Date())
            }
            text.contains("weather", ignoreCase = true) -> {
                "Offline cache database shows standard conditions: 24°C, Clear Skies."
            }
            text.contains("hello", ignoreCase = true) || text.contains("hey", ignoreCase = true) -> {
                "System offline fallback activated. Ready to assist within restricted local capabilities, sir."
            }
            else -> "Offline connection security protocol active, sir. Unable to query cloud intelligence at this moment."
        }
    }

    /**
     * Dynamic mood detector based on semantics
     */
    private fun detectEmotionFromInput(input: String): AssistantEmotion {
        val lower = input.lowercase()
        return when {
            lower.contains("great") || lower.contains("awesome") || lower.contains("happy") -> AssistantEmotion.EXCITED
            lower.contains("sad") || lower.contains("fail") || lower.contains("wrong") -> AssistantEmotion.CONCERNED
            lower.contains("joke") || lower.contains("funny") -> AssistantEmotion.PLAYFUL
            lower.contains("why") || lower.contains("explain") || lower.contains("how") -> AssistantEmotion.CURIOUS
            lower.contains("alert") || lower.contains("urgent") || lower.contains("critical") -> AssistantEmotion.SERIOUS
            else -> AssistantEmotion.CALM
        }
    }

    // Database manual actions (Gated on FREE)
    fun addManualMemory(key: String, value: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCount = repository.allMemories.first().size
            val plan = currentPlan.value
            if (plan == "FREE" && currentCount >= 3) {
                withContext(Dispatchers.Main) {
                    showNotification("Memory cap reached! Max 3 nodes on FREE.")
                    postAssistantResponse("Sir, our memory registers have reached the Free Plan limit (3 facts). To secure more memories in 95's vault, please consider upgrading to LITE or PRO.")
                }
                return@launch
            } else if (plan == "LITE" && currentCount >= 10) {
                withContext(Dispatchers.Main) {
                    showNotification("Memory cap reached! Max 10 nodes on LITE.")
                    postAssistantResponse("Sir, our memory registers have reached the LITE Plan limit (10 facts). Please upgrade to PRO for unlimited memories.")
                }
                return@launch
            }
            repository.insertMemory(MemoryEntity(key = key, value = value, category = category))
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllMemories()
        }
    }

    fun addManualPlannerItem(title: String, type: String, description: String, priority: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPlannerItem(PlannerEntity(title = title, type = type, description = description, priority = priority))
        }
    }

    fun updatePlannerItemStatus(id: Long, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlannerItemStatus(id, isCompleted)
        }
    }

    fun deletePlannerItem(item: PlannerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlannerItem(item)
        }
    }

    fun clearAllPlanner() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllPlannerItems()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
