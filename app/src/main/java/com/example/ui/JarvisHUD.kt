package com.example.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.CameraHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.lazy.LazyRow
import kotlin.math.roundToInt

data class AttachedFile(
    val id: String,
    val name: String,
    val uri: Uri,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisHUD(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel collect flows
    val coreState by viewModel.coreState.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isOnline by viewModel.isOnlineMode.collectAsState()
    val isPrivacy by viewModel.isPrivacyMode.collectAsState()
    val micRmsDb by viewModel.micRmsDb.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(coreState) {
        if (coreState == CoreState.LISTENING) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (coreState == CoreState.IDLE || coreState == CoreState.THINKING) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Preference state values synced with DB
    val liveCaptions by viewModel.liveCaptionsEnabled.collectAsState()
    val speakerOn by viewModel.speakerEnabled.collectAsState()
    val speedValue by viewModel.speechSpeed.collectAsState()
    val pitchValue by viewModel.speechPitch.collectAsState()
    val volumeValue by viewModel.speechVolume.collectAsState()
    val activeModel by viewModel.selectedAiModel.collectAsState()
    val selectedLanguage by viewModel.selectedVoiceLanguage.collectAsState()
    val conversationTimeout by viewModel.conversationTimeoutSeconds.collectAsState()

    // Local UI control flows
    var isBooting by remember { mutableStateOf(true) }
    var activeSheet by remember { mutableStateOf<String?>(null) } // "MORE", "CHAT", "SETTINGS", "VOICE", "MODEL", "LANG"
    var chatInputText by remember { mutableStateOf("") }
    val attachedFiles = remember { mutableStateListOf<AttachedFile>() }
    var unreadMessagesCount by remember { mutableStateOf(0) }

    // Media overlays
    var isCameraActive by remember { mutableStateOf(false) }
    var isScreenShareActive by remember { mutableStateOf(false) }

    // Caption tracking
    val statusSubText = remember { mutableStateOf("") }

    val chatListState = rememberLazyListState()
    val cameraHelper = remember { CameraHelper(context) }

    // Keep track of unread messages when Chat Sheet is closed
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty() && activeSheet != "CHAT") {
            unreadMessagesCount++
        }
        if (activeSheet == "CHAT") {
            unreadMessagesCount = 0
            chatListState.animateScrollToItem(chatHistory.size.coerceAtMost(100).coerceAtLeast(1))
        }
    }

    // Dynamic grid movement offset
    val infiniteTransition = rememberInfiniteTransition(label = "holographic_movement")
    val gridDriftOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (coreState) {
                    CoreState.LISTENING, CoreState.SPEAKING -> 800
                    CoreState.THINKING, CoreState.SEARCHING, CoreState.ACTING, CoreState.WAKE_DETECTED -> 1200
                    else -> 4000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_movement"
    )

    // Breathing glow aura scale value
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = when (coreState) {
            CoreState.LISTENING -> 0.24f
            CoreState.THINKING, CoreState.SEARCHING, CoreState.ACTING -> 0.18f
            CoreState.SPEAKING, CoreState.WAKE_DETECTED -> 0.22f
            else -> 0.08f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    // Launcher for Upload File
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            uris.forEach { uri ->
                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    Log.w("JarvisHUD", "Could not persist read permission for URI: $uri", e)
                }
                val name = getFileName(context, uri)
                val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val fileObj = AttachedFile(System.currentTimeMillis().toString(), name, uri, type)
                attachedFiles.add(fileObj)
                viewModel.showNotification("Attached file: $name")
            }
        }
    )

    // Launcher for Take Photo
    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                val file = saveBitmapToCache(context, bitmap)
                val uri = Uri.fromFile(file)
                val fileObj = AttachedFile(System.currentTimeMillis().toString(), "camera_photo.jpg", uri, "image/jpeg")
                attachedFiles.add(fileObj)
                viewModel.showNotification("Photo attached successfully.")
            }
        }
    )

    // Launcher for Gallery selection
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            uris.forEach { uri ->
                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    Log.w("JarvisHUD", "Could not persist read permission for URI: $uri", e)
                }
                val name = getFileName(context, uri)
                val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                val fileObj = AttachedFile(System.currentTimeMillis().toString(), name, uri, type)
                attachedFiles.add(fileObj)
            }
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050709))
    ) {
        // Glowing Edge Status Feedback (Section 3)
        EdgeGlowEffect(state = coreState, rmsDb = micRmsDb)

        // Dynamic Radial Glowing Background Layers (Section 2. RESPONSIVE MOBILE APP UI)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3A7BFF).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(x = 540f, y = 200f),
                        radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4FD8FF).copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(x = 540f, y = 1400f),
                        radius = 800f
                    )
                )
        )

        // Holographic Coordinate Line Grid Layout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 44.dp.toPx()
            val gridColor = Color(0xFF4FD8FF).copy(alpha = 0.022f)
            val offsetVal = gridDriftOffset % step

            // Vertical Coordinate lines
            var x = offsetVal
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5.dp.toPx())
                x += step
            }
            // Horizontal Coordinate lines
            var y = offsetVal
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                y += step
            }
        }

        // ===================== CORE HUB STAGE =====================
        if (!isBooting) {
            if (!onboardingCompleted) {
                OnboardingScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                // 1. TOP HEADER (Branding Badge & Action Gear)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glassmorphic Branding Badge and Premium Crown Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFFFFFFF).copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4FD8FF))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "95",
                                color = Color(0xFFF2F6F8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }

                        // Premium CROWN button
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFD97706).copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                .clickable { activeSheet = "95_PRO" }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "👑", fontSize = 14.sp)
                            Text(
                                text = "Premium",
                                color = Color(0xFFFBBF24),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    // Row for top actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // PREMIUM HUD Dashboard button
                        IconButton(
                            onClick = { activeSheet = "PREMIUM_HUD" },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF4FD8FF).copy(alpha = 0.12f), CircleShape)
                                .border(1.dp, Color(0xFF4FD8FF).copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Premium HUD Dashboard",
                                tint = Color(0xFF4FD8FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings Circle Button (gear icon)
                        IconButton(
                            onClick = { activeSheet = "SETTINGS" },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFFFFFFF).copy(alpha = 0.06f), CircleShape)
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF8B98A3),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. MAIN VIEWPORT GRID
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Backing aura breath element
                    Box(
                        modifier = Modifier
                            .size(320.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF4FD8FF).copy(alpha = auraAlpha), Color.Transparent)
                                )
                            )
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Floating Interactive Activity Indicator
                        AnimatedVisibility(
                            visible = coreState != CoreState.IDLE,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(bottom = 24.dp)
                                    .background(Color(0xFF141A20).copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                                    .border(1.dp, Color(0xFF78C8FF).copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color.Transparent, CircleShape)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 1.8.dp,
                                        color = Color(0xFF4FD8FF),
                                        trackColor = Color(0xFF4FD8FF).copy(alpha = 0.15f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (coreState) {
                                        CoreState.WAKE_DETECTED -> "🤖 Wake word..."
                                        CoreState.LISTENING -> "🎤 Listening..."
                                        CoreState.THINKING -> "🧠 Thinking..."
                                        CoreState.SEARCHING -> "🌐 Searching..."
                                        CoreState.ACTING -> "🌀 Acting..."
                                        CoreState.SPEAKING -> "🔊 Speaking..."
                                        CoreState.ERROR -> "⚠️ Warning Alert"
                                        else -> "System Standby"
                                    },
                                    color = Color(0xFFF2F6F8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Premium Holographic core orb element
                        JarvisOrb(
                            state = coreState,
                            rmsDb = micRmsDb,
                            modifier = Modifier.size(240.dp)
                        )

                        // Real-time Mic Waveform
                        RealtimeMicWaveform(
                            rmsDb = micRmsDb,
                            isListening = coreState == CoreState.LISTENING,
                            modifier = Modifier
                                .padding(top = 16.dp, start = 32.dp, end = 32.dp)
                        )

                        // Visual Status indicator message
                        Text(
                            text = when (coreState) {
                                CoreState.WAKE_DETECTED -> "Wake Word Detected"
                                CoreState.LISTENING -> "Listening..."
                                CoreState.THINKING -> "Thinking..."
                                CoreState.SEARCHING -> "Searching..."
                                CoreState.ACTING -> "Acting..."
                                CoreState.SPEAKING -> "Speaking..."
                                CoreState.ERROR -> "Something went wrong"
                                else -> "Say \"Hello 95\" to talk"
                            },
                            color = Color(0xFF8B98A3),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 24.dp)
                        )

                        // Subtitle Live Caption Tracker (Section 11)
                        if (liveCaptions) {
                            Text(
                                text = if (coreState == CoreState.LISTENING && micRmsDb > 1f) "Capturing vocal streams..." else chatHistory.lastOrNull()?.text ?: "",
                                color = Color(0xFF5B6670),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .widthIn(max = 280.dp)
                            )
                        }
                    }
                }

                // 3. HORIZONTAL MULTIMEDIA ATTACHMENTS LIST
                AnimatedVisibility(
                    visible = attachedFiles.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { 30 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 30 }) + fadeOut(),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        attachedFiles.forEach { file ->
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF141A20).copy(alpha = 0.72f))
                                    .border(1.dp, Color(0xFF78C8FF).copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            file.type.startsWith("image/") -> Icons.Default.Image
                                            file.type.startsWith("video/") -> Icons.Default.Videocam
                                            file.type.startsWith("audio/") -> Icons.Default.AudioFile
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = "File Type",
                                        tint = Color(0xFF4FD8FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = file.name,
                                        color = Color(0xFF8B98A3),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Remove File badge button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .clickable { attachedFiles.remove(file) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. FLOATING BOTTOM CONTROLS ROW (Section 2 & Section 17)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .background(Color(0xFF141A20).copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                            .border(1.dp, Color(0xFF78C8FF).copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // A. Microphone Trigger Button (Section 4)
                        val isListening = coreState == CoreState.LISTENING
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isListening) Color.White else Color(0xFFFFFFFF).copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.06f), CircleShape)
                                .clickable {
                                    if (isListening) {
                                        viewModel.stopListening()
                                    } else {
                                        viewModel.stopTTS()
                                        viewModel.startListening()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = if (isListening) Color(0xFF0A0D11) else Color(0xFFF2F6F8),
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // B. Camera Toggle Button (Section 8)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isCameraActive) Color(0xFF4FD8FF).copy(alpha = 0.25f) else Color(0xFFFFFFFF).copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.06f), CircleShape)
                                .clickable {
                                    if (isCameraActive) {
                                        isCameraActive = false
                                        cameraHelper.shutdown()
                                    } else {
                                        isCameraActive = true
                                        isScreenShareActive = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Camera",
                                tint = if (isCameraActive) Color(0xFF4FD8FF) else Color(0xFFF2F6F8),
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // C. Screen Capture Button (Section 9)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isScreenShareActive) Color(0xFF4FD8FF).copy(alpha = 0.25f) else Color(0xFFFFFFFF).copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.06f), CircleShape)
                                .clickable {
                                    if (isScreenShareActive) {
                                        isScreenShareActive = false
                                    } else {
                                        isScreenShareActive = true
                                        isCameraActive = false
                                        viewModel.showNotification("Initiating screen capture mode.")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenShare,
                                contentDescription = "Screen Share",
                                tint = if (isScreenShareActive) Color(0xFF4FD8FF) else Color(0xFFF2F6F8),
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // D. Chat Sheet Button (Section 7)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFFFFF).copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.06f), CircleShape)
                                .clickable { activeSheet = "CHAT" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Chat",
                                tint = Color(0xFFF2F6F8),
                                modifier = Modifier.size(21.dp)
                            )

                            // Unread count badge dot
                            if (unreadMessagesCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 2.dp, end = 2.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4FD8FF))
                                )
                            }
                        }

                        // E. More Options Button (Section 10)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFFFFF).copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.06f), CircleShape)
                                .clickable { activeSheet = "MORE" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More",
                                tint = Color(0xFFF2F6F8),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
        } else {
            // ===================== INTRO / BOOT SCREEN (Section 16) =====================
            var bootProgress by remember { mutableStateOf(0f) }
            var bootStatusMsg by remember { mutableStateOf("Calibrating...") }
            var countdownNumber by remember { mutableStateOf("3") }

            LaunchedEffect(Unit) {
                // Calibrated steps progress sequence
                coroutineScope.launch {
                    val steps = listOf(
                        0.34f to "Calibrating modules...",
                        0.68f to "Loading visual models...",
                        1.00f to "95 online."
                    )
                    for (step in steps) {
                        while (bootProgress < step.first) {
                            bootProgress += 0.02f
                            delay(40)
                        }
                        bootStatusMsg = step.second
                        delay(200)
                    }
                    delay(400)
                    isBooting = false
                }

                // Digital countdown sequence: 3... 2... 1...
                coroutineScope.launch {
                    delay(300)
                    countdownNumber = "3"
                    delay(800)
                    countdownNumber = "2"
                    delay(800)
                    countdownNumber = "1"
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isBooting = false } // Tap immediately to skip
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Branding name
                Text(
                    text = "9 5",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7FE9EA),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Rotating Boot HUD rings
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bootTransition = rememberInfiniteTransition(label = "boot_animation")
                    val bootRotationCW by bootTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(9000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "cw"
                    )
                    val bootRotationCCW by bootTransition.animateFloat(
                        initialValue = 360f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(6500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ccw"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        // Outer scanning ring
                        rotate(bootRotationCW) {
                            drawCircle(
                                color = Color(0xFF2FBDB8),
                                radius = 80.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(80f, 120f)
                                    )
                                )
                            )
                        }

                        // Inner mechanical ring
                        rotate(bootRotationCCW) {
                            drawCircle(
                                color = Color(0xFF7FE9EA).copy(alpha = 0.7f),
                                radius = 68.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(
                                    width = 1.3f.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(40f, 80f)
                                    )
                                )
                            )
                        }

                        // Inner glowing core
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF7FE9EA).copy(alpha = 0.5f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = 40.dp.toPx()
                            ),
                            radius = 40.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }

                    // Centered countdown badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF001416).copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF7FE9EA).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = countdownNumber,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEAFCFF),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Calibration status
                Text(
                    text = bootStatusMsg.uppercase(),
                    fontSize = 12.sp,
                    color = Color(0xFF7FE9EA),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Calibration Progress loading bar
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF7FE9EA).copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(bootProgress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1C8F92), Color(0xFF7FE9EA))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Skip interactive tip
                Text(
                    text = "TAP TO SKIP",
                    fontSize = 11.sp,
                    color = Color(0xFF7FE9EA).copy(alpha = 0.5f),
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ===================== MEDIA VIEWPORT OVERLAY (Section 8 & Section 9) =====================
        if (isCameraActive || isScreenShareActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000).copy(alpha = 0.94f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Overlay Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5A5A))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCameraActive) "Camera Preview" else "Screen Share Preview",
                            color = Color(0xFF8B98A3),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Content Viewport
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF4FD8FF).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCameraActive) {
                            // Camera preview feed integration (Section 8)
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                        cameraHelper.bindCamera(
                                            lifecycleOwner = lifecycleOwner,
                                            surfaceProvider = this.surfaceProvider,
                                            onError = {
                                                viewModel.showNotification("Camera Feed Suspended")
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Animated Screen share visualization (Section 9)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val matrixTransition = (System.currentTimeMillis() / 200.0) % 100
                                drawRect(Color.Black)
                                // Drawing dynamic futuristic network nodes
                                drawCircle(
                                    color = Color(0xFF4FD8FF).copy(alpha = 0.35f),
                                    radius = 32.dp.toPx(),
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )
                                drawLine(
                                    color = Color(0xFF4FD8FF).copy(alpha = 0.15f),
                                    start = Offset(0f, size.height * 0.4f),
                                    end = Offset(size.width, size.height * 0.6f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }

                    // Bottom Action stop button
                    Button(
                        onClick = {
                            isCameraActive = false
                            isScreenShareActive = false
                            cameraHelper.shutdown()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF).copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.12f), RoundedCornerShape(999.dp)),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Stop Preview", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // ===================== GLASSMORPHIC SLIDING SHEETS LAYER (Section 11) =====================
        AnimatedVisibility(
            visible = activeSheet != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(0xFF0E1216).copy(alpha = 0.98f))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFFFFFFF).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (activeSheet != "PREMIUM_HUD" && activeSheet != "95_PRO" && activeSheet != "95_BYOK" && activeSheet != "SETTINGS") {
                        // Sheet Drag Handle Swipe
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp)
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFFFFFF).copy(alpha = 0.18f))
                        )

                        // Sheet Header with dismiss cross
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeSheet) {
                                    "MORE" -> "More Options"
                                    "CHAT" -> "Chat with 95"
                                    "SETTINGS" -> "Assistant Settings"
                                    "VOICE" -> "Voice Settings"
                                    "MODEL" -> "AI Model"
                                    "LANG" -> "Language"
                                    else -> ""
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { activeSheet = null },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFFFFFF).copy(alpha = 0.06f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Divider(color = Color(0xFFFFFFFF).copy(alpha = 0.06f), modifier = Modifier.padding(bottom = 14.dp))
                    }

                    // Sheet Contents
                    Box(modifier = Modifier.weight(1f)) {
                        when (activeSheet) {
                            "PREMIUM_HUD" -> {
                                PremiumOSDashboard(
                                    viewModel = viewModel,
                                    onClose = { activeSheet = null },
                                    onNavigateToPro = { activeSheet = "95_PRO" },
                                    onNavigateToByok = { activeSheet = "95_BYOK" }
                                )
                            }
                            "95_PRO" -> {
                                SubscriptionSheet(
                                    viewModel = viewModel,
                                    onClose = { activeSheet = null }
                                )
                            }
                            "95_BYOK" -> {
                                ApiKeySheet(
                                    viewModel = viewModel,
                                    onClose = { activeSheet = null }
                                )
                            }
                            "MORE" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SheetItem(icon = Icons.Default.UploadFile, label = "Upload File") {
                                        activeSheet = null
                                        filePickerLauncher.launch(arrayOf("*/*"))
                                    }
                                    SheetItem(icon = Icons.Default.PhotoCamera, label = "Take Photo") {
                                        activeSheet = null
                                        cameraPhotoLauncher.launch()
                                    }
                                    SheetItem(icon = Icons.Default.Image, label = "Gallery") {
                                        activeSheet = null
                                        galleryPickerLauncher.launch(arrayOf("image/*"))
                                    }
                                    SheetItem(icon = Icons.Default.ScreenShare, label = "Share Screen") {
                                        activeSheet = null
                                        isScreenShareActive = true
                                    }
                                }
                            }

                            "CHAT" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Chat History lists
                                    LazyColumn(
                                        state = chatListState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (chatHistory.isEmpty()) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "No messages yet. Say \"Hello 95\" or type below.",
                                                        color = Color(0xFF5B6670),
                                                        fontSize = 13.sp,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        } else {
                                            items(chatHistory, key = { it.dbId }) { msg ->
                                                SwipeableChatItem(
                                                    msg = msg,
                                                    onDismiss = {
                                                        viewModel.deleteChatMessage(msg.dbId)
                                                    }
                                                ) {
                                                    val isUser = msg.sender == "USER"
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .widthIn(max = 260.dp)
                                                                .clip(
                                                                    RoundedCornerShape(
                                                                        topStart = 16.dp,
                                                                        topEnd = 16.dp,
                                                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                                                    )
                                                                )
                                                                .background(
                                                                    if (isUser) {
                                                                        Brush.horizontalGradient(
                                                                            colors = listOf(Color(0xFF3A7BFF), Color(0xFF2C5FD0))
                                                                        )
                                                                    } else {
                                                                        Brush.horizontalGradient(
                                                                            colors = listOf(
                                                                                Color(0xFF4FD8FF).copy(alpha = 0.10f),
                                                                                Color(0xFF4FD8FF).copy(alpha = 0.10f)
                                                                            )
                                                                        )
                                                                    }
                                                                )
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = if (isUser) Color.Transparent else Color(0xFF4FD8FF).copy(alpha = 0.35f),
                                                                    shape = RoundedCornerShape(
                                                                        topStart = 16.dp,
                                                                        topEnd = 16.dp,
                                                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                                                    )
                                                                )
                                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                                        ) {
                                                            Text(
                                                                text = msg.text,
                                                                color = Color.White,
                                                                fontSize = 14.sp,
                                                                lineHeight = 20.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Real-time typing indicators (Section 7)
                                        if (coreState == CoreState.THINKING || coreState == CoreState.SEARCHING || coreState == CoreState.ACTING) {
                                            item {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF4FD8FF).copy(alpha = 0.08f))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val loaderTransition = rememberInfiniteTransition(label = "dots")
                                                    val dotAlpha1 by loaderTransition.animateFloat(
                                                        initialValue = 0.3f, targetValue = 1f,
                                                        animationSpec = infiniteRepeatable(tween(400, easing = EaseInOutSine), RepeatMode.Reverse),
                                                        label = "d1"
                                                    )
                                                    val dotAlpha2 by loaderTransition.animateFloat(
                                                        initialValue = 0.3f, targetValue = 1f,
                                                        animationSpec = infiniteRepeatable(tween(400, delayMillis = 150, easing = EaseInOutSine), RepeatMode.Reverse),
                                                        label = "d2"
                                                    )
                                                    val dotAlpha3 by loaderTransition.animateFloat(
                                                        initialValue = 0.3f, targetValue = 1f,
                                                        animationSpec = infiniteRepeatable(tween(400, delayMillis = 300, easing = EaseInOutSine), RepeatMode.Reverse),
                                                        label = "d3"
                                                    )

                                                    Box(modifier = Modifier.size(6.dp).graphicsLayer { alpha = dotAlpha1 }.background(Color(0xFF4FD8FF), CircleShape))
                                                    Box(modifier = Modifier.size(6.dp).graphicsLayer { alpha = dotAlpha2 }.background(Color(0xFF4FD8FF), CircleShape))
                                                    Box(modifier = Modifier.size(6.dp).graphicsLayer { alpha = dotAlpha3 }.background(Color(0xFF4FD8FF), CircleShape))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Persistent horizontal row of Quick Action chips
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val quickActions = listOf(
                                            "Check weather" to "Check the current weather conditions",
                                            "Set alarm" to "Set an alarm for 7:00 AM",
                                            "Summarize notes" to "Summarize my active notes",
                                            "System status" to "Provide a complete system diagnostics report",
                                            "Show help" to "What core functions can you perform, Ninety Five?"
                                        )
                                        items(quickActions) { (label, prompt) ->
                                            AssistChip(
                                                onClick = {
                                                    viewModel.stopTTS()
                                                    viewModel.processUserInput(prompt)
                                                },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        color = Color(0xFF4FD8FF),
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = Color(0xFF4FD8FF).copy(alpha = 0.08f),
                                                    labelColor = Color(0xFF4FD8FF)
                                                ),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = Color(0xFF4FD8FF).copy(alpha = 0.35f)
                                                ),
                                                shape = RoundedCornerShape(99.dp)
                                            )
                                        }
                                    }

                                    // Chat input entry bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextField(
                                            value = chatInputText,
                                            onValueChange = { chatInputText = it },
                                            placeholder = { Text("Message 95...", color = Color(0xFF5B6670), fontSize = 14.sp) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(999.dp))
                                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f), RoundedCornerShape(999.dp)),
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFFFFFFFF).copy(alpha = 0.06f),
                                                unfocusedContainerColor = Color(0xFFFFFFFF).copy(alpha = 0.06f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                if (chatInputText.isNotBlank()) {
                                                    viewModel.stopTTS()
                                                    viewModel.processUserInput(chatInputText)
                                                    chatInputText = ""
                                                }
                                            },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(Color(0xFF4FD8FF), Color(0xFF3A7BFF))
                                                    )
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Send",
                                                tint = Color(0xFF04121A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            "SETTINGS" -> {
                                SettingsSheet(
                                    viewModel = viewModel,
                                    onClose = { activeSheet = null }
                                )
                            }

                            "VOICE" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Voice description label
                                    Text(
                                        text = "95 automatically selects the most optimal and professional male vocal stream configured in your device. Customized voice parameters can be altered below.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8B98A3),
                                        lineHeight = 18.sp
                                    )

                                    // Rate speed slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Speech Rate", color = Color.White, fontSize = 14.sp)
                                            Text("${String.format("%.2f", speedValue)}x", color = Color(0xFF4FD8FF), fontSize = 14.sp)
                                        }
                                        Slider(
                                            value = speedValue,
                                            onValueChange = { viewModel.speechSpeed.value = it },
                                            valueRange = 0.5f..2.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF4FD8FF),
                                                inactiveTrackColor = Color(0xFF1E293B)
                                            )
                                        )
                                    }

                                    // Pitch deepness slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Speech Pitch", color = Color.White, fontSize = 14.sp)
                                            Text("${String.format("%.2f", pitchValue)}x", color = Color(0xFF4FD8FF), fontSize = 14.sp)
                                        }
                                        Slider(
                                            value = pitchValue,
                                            onValueChange = { viewModel.speechPitch.value = it },
                                            valueRange = 0.5f..2.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF4FD8FF),
                                                inactiveTrackColor = Color(0xFF1E293B)
                                            )
                                        )
                                    }

                                    // Volume level slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Volume Level", color = Color.White, fontSize = 14.sp)
                                            Text("${String.format("%.1f", volumeValue * 100)}%", color = Color(0xFF4FD8FF), fontSize = 14.sp)
                                        }
                                        Slider(
                                            value = volumeValue,
                                            onValueChange = { viewModel.speechVolume.value = it },
                                            valueRange = 0f..1f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF4FD8FF),
                                                inactiveTrackColor = Color(0xFF1E293B)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Test Voice trigger (Section 14)
                                    Button(
                                        onClick = {
                                            viewModel.stopTTS()
                                            viewModel.processUserInput("Test voice. Running full vocal streams sequence diagnostics, sir.")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FD8FF).copy(alpha = 0.08f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFF4FD8FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF4FD8FF))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Test voice stream", color = Color(0xFF4FD8FF))
                                    }
                                }
                            }

                            "MODEL" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ModelOptionCard(
                                        name = "Auto Select (Recommended)",
                                        desc = "Switches online neural and offline models dynamically.",
                                        selected = activeModel == "auto"
                                    ) {
                                        viewModel.selectedAiModel.value = "auto"
                                        viewModel.showNotification("Model config updated: Auto")
                                    }
                                    ModelOptionCard(
                                        name = "Online AI",
                                        desc = "Use cloud API core for complex semantic tasks.",
                                        selected = activeModel == "online"
                                    ) {
                                        viewModel.selectedAiModel.value = "online"
                                        viewModel.showNotification("Model config updated: Online")
                                    }
                                    ModelOptionCard(
                                        name = "Local / Offline AI",
                                        desc = "Directly run rule-based local model core securely.",
                                        selected = activeModel == "local"
                                    ) {
                                        viewModel.selectedAiModel.value = "local"
                                        viewModel.showNotification("Model config updated: Offline Fallback")
                                    }
                                }
                            }

                            "LANG" -> {
                                val languagesList = listOf(
                                    "en-US" to "English",
                                    "es-ES" to "Español",
                                    "fr-FR" to "Français",
                                    "de-DE" to "Deutsch",
                                    "hi-IN" to "Hindi"
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    languagesList.forEach { pair ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selectedLanguage == pair.first) Color(0xFF4FD8FF).copy(alpha = 0.08f) else Color.Transparent)
                                                .clickable {
                                                    viewModel.selectedVoiceLanguage.value = pair.first
                                                    viewModel.showNotification("Language configured to ${pair.second}")
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF4FD8FF))
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Text(pair.second, color = Color.White, fontSize = 15.sp)
                                            }
                                            if (selectedLanguage == pair.first) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4FD8FF))
                                            }
                                        }
                                    }
                                }
                            }

                            "TIMEOUT" -> {
                                val timeoutsList = listOf(5, 8, 10, 15)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Configure the duration of silence before Ninety Five automatically ends the active voice-command session, Sir.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8B98A3),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    timeoutsList.forEach { secs ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (conversationTimeout == secs) Color(0xFF4FD8FF).copy(alpha = 0.08f) else Color.Transparent)
                                                .clickable {
                                                    viewModel.conversationTimeoutSeconds.value = secs
                                                    viewModel.showNotification("Inactivity timeout: ${secs}s")
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.HourglassFull, contentDescription = null, tint = Color(0xFF4FD8FF))
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Text("$secs Seconds", color = Color.White, fontSize = 15.sp)
                                            }
                                            if (conversationTimeout == secs) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4FD8FF))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF4FD8FF).copy(alpha = 0.10f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF4FD8FF), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, color = Color.White, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF5B6670), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    label: String,
    value: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF4FD8FF))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, color = Color.White, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(value, color = Color(0xFF5B6670), fontSize = 12.5.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF5B6670), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ModelOptionCard(
    name: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF4FD8FF).copy(alpha = 0.08f) else Color(0xFFFFFFFF).copy(alpha = 0.02f))
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF4FD8FF).copy(alpha = 0.35f) else Color(0xFFFFFFFF).copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = Color(0xFF5B6670), fontSize = 11.5.sp, lineHeight = 16.sp)
        }
        if (selected) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF4FD8FF), modifier = Modifier.size(18.dp))
        }
    }
}

// Extract filename helper
fun getFileName(context: Context, uri: Uri): String {
    var name = ""
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1) {
                    name = it.getString(nameIdx) ?: ""
                }
            }
        }
    } catch (e: Exception) {
        Log.e("JarvisHUD", "Error resolving filename via content query", e)
    }
    
    if (name.isEmpty()) {
        try {
            val path = uri.path
            if (path != null) {
                val cut = path.lastIndexOf('/')
                name = if (cut != -1) {
                    path.substring(cut + 1)
                } else {
                    path
                }
                name = Uri.decode(name)
            }
        } catch (ignored: Exception) {}
    }
    
    if (name.isEmpty() || name == "null") {
        name = "attached_file"
    }
    return name
}

fun saveBitmapToCache(context: Context, bitmap: android.graphics.Bitmap): java.io.File {
    val file = java.io.File(context.cacheDir, "captured_photo_${System.currentTimeMillis()}.jpg")
    try {
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
    } catch (e: Exception) {
        Log.e("JarvisHUD", "Error saving bitmap", e)
    }
    return file
}

@Composable
fun EdgeGlowEffect(state: CoreState, rmsDb: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_transition")
    
    // Smooth pulse for LISTENING and SPEAKING
    val smoothAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smooth_alpha"
    )
    
    // Faster pulsing for THINKING / ACTING / SEARCHING
    val activeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "active_alpha"
    )
    
    // Search rotation/flow sweeps
    val searchSweepOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "search_sweep"
    )

    val (glowColor, glowAlpha, glowWidth) = when (state) {
        CoreState.IDLE -> Triple(Color(0xFF3A7BFF), 0.12f, 6.dp) // Subtle Blue, low brightness (Section 3)
        CoreState.WAKE_DETECTED -> Triple(Color(0xFF4FD8FF), 0.75f, 16.dp) // Bright cyan pulse
        CoreState.LISTENING -> Triple(Color(0xFF10B981), smoothAlpha, 14.dp) // Green, smooth pulsing (Section 3)
        CoreState.THINKING -> Triple(Color(0xFFF59E0B), activeAlpha, 16.dp) // Orange edge glow, flowing animation (Section 3)
        CoreState.SEARCHING -> Triple(Color(0xFF06B6D4), activeAlpha, 18.dp) // Cyan, moving search wave (Section 3)
        CoreState.ACTING -> Triple(Color(0xFF8B5CF6), activeAlpha, 18.dp) // Purple, task-progress effect (Section 3)
        CoreState.SPEAKING -> {
            // Pulse in synchronization with speech volume (rmsDb) (Section 3)
            val voiceAlpha = (0.35f + (rmsDb.coerceIn(0f, 10f) / 15f)).coerceIn(0.3f, 0.9f)
            val voiceWidth = (12.dp + (rmsDb.coerceIn(0f, 10f).dp * 1.5f)).coerceIn(12.dp, 28.dp)
            Triple(Color(0xFFEF4444), voiceAlpha, voiceWidth)
        }
        CoreState.ERROR -> Triple(Color(0xFFDC2626), 0.75f, 18.dp) // Alert Red (Section 3)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,
                color = glowColor.copy(alpha = glowAlpha * 1.2f),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = glowWidth.toPx()
            // Top edge glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    startY = 0f,
                    endY = strokePx
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, strokePx)
            )
            // Bottom edge glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, glowColor.copy(alpha = glowAlpha)),
                    startY = size.height - strokePx,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - strokePx),
                size = Size(size.width, strokePx)
            )
            // Left edge glow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    startX = 0f,
                    endX = strokePx
                ),
                topLeft = Offset(0f, 0f),
                size = Size(strokePx, size.height)
            )
            // Right edge glow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, glowColor.copy(alpha = glowAlpha)),
                    startX = size.width - strokePx,
                    endX = size.width
                ),
                topLeft = Offset(size.width - strokePx, 0f),
                size = Size(strokePx, size.height)
            )

            // Flowing search sweep animation across borders
            if (state == CoreState.SEARCHING) {
                val perimeter = (size.width + size.height) * 2
                val currentPos = searchSweepOffset * perimeter
                val dotCenter = when {
                    currentPos < size.width -> Offset(currentPos, 0f)
                    currentPos < size.width + size.height -> Offset(size.width, currentPos - size.width)
                    currentPos < size.width * 2 + size.height -> Offset(size.width - (currentPos - (size.width + size.height)), size.height)
                    else -> Offset(0f, size.height - (currentPos - (size.width * 2 + size.height)))
                }
                drawCircle(
                    color = Color(0xFFEAFCFF),
                    radius = 12.dp.toPx(),
                    center = dotCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.8f), Color.Transparent),
                        center = dotCenter,
                        radius = 48.dp.toPx()
                    ),
                    radius = 48.dp.toPx(),
                    center = dotCenter
                )
            }
        }
    }
}

@Composable
fun SwipeableChatItem(
    msg: ChatMessage,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(msg.dbId) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 200f || offsetX < -200f) {
                                isDismissed = true
                                onDismiss()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun RealtimeMicWaveform(
    rmsDb: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isListening) return

    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("mic_waveform_canvas")
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val amplitude = (rmsDb.coerceIn(0f, 12f) * 3f + 4f).dp.toPx()

        val path = androidx.compose.ui.graphics.Path()
        val path2 = androidx.compose.ui.graphics.Path()

        for (x in 0..width.toInt() step 4) {
            val progress = x.toFloat() / width
            val envelope = sin(progress * Math.PI.toFloat())
            
            val y = midY + sin(progress * 3f * Math.PI.toFloat() + phaseShift) * amplitude * envelope
            val y2 = midY + sin(progress * 4f * Math.PI.toFloat() - phaseShift * 1.2f) * (amplitude * 0.6f) * envelope

            if (x == 0) {
                path.moveTo(x.toFloat(), y)
                path2.moveTo(x.toFloat(), y2)
            } else {
                path.lineTo(x.toFloat(), y)
                path2.lineTo(x.toFloat(), y2)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF4FD8FF),
            style = Stroke(width = 2.dp.toPx())
        )

        drawPath(
            path = path2,
            color = Color(0xFF3A7BFF).copy(alpha = 0.5f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
