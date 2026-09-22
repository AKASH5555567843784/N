package com.example.ui

import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun FloatingOverlayView(
    coreStateFlow: StateFlow<CoreState>,
    rmsDbFlow: StateFlow<Float>,
    windowManager: WindowManager,
    layoutParams: WindowManager.LayoutParams,
    composeView: android.view.View,
    onIndicatorClicked: () -> Unit
) {
    val coreState by coreStateFlow.collectAsState()
    val rmsDb by rmsDbFlow.collectAsState()

    // Smooth color gradients depending on the current active CoreState
    val colors = when (coreState) {
        CoreState.WAKE_DETECTED -> listOf(Color(0xFFFFB300), Color(0xFFFF6D00)) // Golden transition
        CoreState.LISTENING -> listOf(Color(0xFF00E5FF), Color(0xFF00838F))    // Glowing Cyan
        CoreState.THINKING, CoreState.SEARCHING -> listOf(Color(0xFFE040FB), Color(0xFF6A1B9A)) // Magenta/Purple
        CoreState.ACTING -> listOf(Color(0xFF00E676), Color(0xFF00C853))      // Matrix Green
        CoreState.SPEAKING -> listOf(Color(0xFF2979FF), Color(0xFF1565C0))    // Vibrant Blue
        CoreState.ERROR -> listOf(Color(0xFFFF1744), Color(0xFFB71C1C))       // Alert Red
        else -> listOf(Color(0xFF1A237E), Color(0xFF0D47A1))                  // Deep Space Blue (Idle)
    }

    // Standard infinite transitions for visual rich breathing effects
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Compute direct dynamic audio pulse scale
    val audioMultiplier = if (coreState == CoreState.LISTENING && rmsDb > 0) {
        1.0f + (rmsDb / 35.0f).coerceIn(0.0f, 0.4f)
    } else {
        1.0f
    }

    val finalScale = breathingScale * audioMultiplier

    Box(
        modifier = Modifier
            .size(68.dp)
            .scale(finalScale)
            .pointerInput(Unit) {
                // Handle physical drag updates back to WindowManager instantly
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    layoutParams.x += dragAmount.x.toInt()
                    layoutParams.y += dragAmount.y.toInt()
                    try {
                        windowManager.updateViewLayout(composeView, layoutParams)
                    } catch (ignored: Exception) {}
                }
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(colors[0].copy(alpha = 0.9f), colors[1].copy(alpha = 0.5f), Color.Transparent),
                    radius = 110f
                )
            )
            .clickable { onIndicatorClicked() },
        contentAlignment = Alignment.Center
    ) {
        // Futuristic Circular Border Glow
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(colors[0], colors[1], colors[0])
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dark elegant center core
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0E14)),
                contentAlignment = Alignment.Center
            ) {
                // Minimalist branding text indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "95",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = when (coreState) {
                            CoreState.LISTENING -> "LIVE"
                            CoreState.THINKING, CoreState.SEARCHING -> "THINK"
                            CoreState.SPEAKING -> "TALK"
                            CoreState.ACTING -> "WORK"
                            CoreState.ERROR -> "ERR"
                            else -> "95"
                        },
                        color = colors[0].copy(alpha = 0.85f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
