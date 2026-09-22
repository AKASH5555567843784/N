package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun JarvisOrb(
    state: CoreState,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    // 1. Idle breathing scale (4400ms)
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_breathe"
    )

    // 2. Physical rig vertical translation (4600ms)
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rig_float"
    )

    // 3. Mechanical rotation timelines (matching HTML speeds)
    val rotationCW1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CoreState.LISTENING) 4000 else 13000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_cw_1"
    )

    val rotationCCW2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CoreState.LISTENING) 3000 else 9500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_ccw_2"
    )

    val rotationCW3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CoreState.SPEAKING) 2500 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_cw_3"
    )

    // Tilted glass discs
    val rotationTiltCW by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_tilt_cw"
    )

    val rotationTiltCCW by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_tilt_ccw"
    )

    // Core fast pulse animations
    val fastPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CoreState.SPEAKING) 250 else 550, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_pulse"
    )

    // Particle drift timers
    val driftY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_particles"
    )

    // Scanning sweep effect
    val scanSweepY by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CoreState.THINKING || state == CoreState.SEARCHING || state == CoreState.ACTING) 800 else 2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanning_sweep"
    )

    // Majestic Jarvis-like breathing aura bloom (1800ms slow pulse)
    val auraBloom by infiniteTransition.animateFloat(
        initialValue = 55f,
        targetValue = 95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_bloom"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    // Color theme overrides based on states
    val isOffline = false
    val isError = state == CoreState.ERROR

    val cyanColor = if (isError) Color(0xFFFF5A5A) else Color(0xFF4FD8FF)
    val blueColor = if (isError) Color(0xFFEF4444) else Color(0xFF3A7BFF)
    val dimCyanColor = if (isOffline) Color(0xFF5B6670) else cyanColor

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("animated_central_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2.1f
            val rigY = cy + floatY

            // 1. Holographic Platform Glow at the bottom
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cyanColor.copy(alpha = if (isOffline) 0.15f else 0.55f),
                        cyanColor.copy(alpha = 0f)
                    ),
                    center = Offset(cx, size.height * 0.88f),
                    radius = 72.dp.toPx()
                ),
                topLeft = Offset(cx - 72.dp.toPx(), size.height * 0.88f - 11.dp.toPx()),
                size = Size(144.dp.toPx(), 22.dp.toPx())
            )
            drawOval(
                color = cyanColor.copy(alpha = if (isOffline) 0.1f else 0.35f),
                topLeft = Offset(cx - 46.dp.toPx(), size.height * 0.88f - 4.5f.dp.toPx()),
                size = Size(92.dp.toPx(), 9.dp.toPx())
            )

            // 2. Tilted Glass Discs (Outer ellipse rx=104 ry=44, Mid rx=88 ry=38)
            rotate(rotationTiltCW, pivot = Offset(cx, rigY)) {
                drawOval(
                    color = dimCyanColor.copy(alpha = if (isOffline) 0.15f else 0.55f),
                    topLeft = Offset(cx - 104.dp.toPx(), rigY - 44.dp.toPx()),
                    size = Size(208.dp.toPx(), 88.dp.toPx()),
                    style = Stroke(
                        width = 1.1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(40f, 300f),
                            phase = 0f
                        )
                    )
                )
            }

            rotate(rotationTiltCCW, pivot = Offset(cx, rigY)) {
                drawOval(
                    color = blueColor.copy(alpha = if (isOffline) 0.15f else 0.5f),
                    topLeft = Offset(cx - 88.dp.toPx(), rigY - 38.dp.toPx()),
                    size = Size(176.dp.toPx(), 76.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(20f, 240f),
                            phase = 0f
                        )
                    )
                )
            }

            // 3. Mechanical Layered Circles (Ring-Mech-1 r=72, Ring-Mech-2 r=58, Ring-Mech-3 r=46)
            rotate(rotationCW1, pivot = Offset(cx, rigY)) {
                drawCircle(
                    color = dimCyanColor.copy(alpha = if (isOffline) 0.1f else 0.4f),
                    radius = 72.dp.toPx(),
                    center = Offset(cx, rigY),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(25f, 50f, 10f, 150f),
                            phase = 0f
                        )
                    )
                )
            }

            rotate(rotationCCW2, pivot = Offset(cx, rigY)) {
                drawCircle(
                    color = blueColor.copy(alpha = if (isOffline) 0.1f else 0.45f),
                    radius = 58.dp.toPx(),
                    center = Offset(cx, rigY),
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(40f, 170f),
                            phase = 0f
                        )
                    )
                )
            }

            rotate(rotationCW3, pivot = Offset(cx, rigY)) {
                drawCircle(
                    color = dimCyanColor.copy(alpha = if (isOffline) 0.12f else 0.55f),
                    radius = 46.dp.toPx(),
                    center = Offset(cx, rigY),
                    style = Stroke(
                        width = 1.1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(15f, 130f),
                            phase = 0f
                        )
                    )
                )
            }

            // 4. Drifting Space Particles (Dynamic drift mapping)
            val particles = listOf(
                Offset(cx - 56.dp.toPx(), rigY - 40.dp.toPx() + driftY),
                Offset(cx + 60.dp.toPx(), rigY - 48.dp.toPx() + driftY * 0.7f),
                Offset(cx + 68.dp.toPx(), rigY + 42.dp.toPx() + driftY * 1.2f),
                Offset(cx - 64.dp.toPx(), rigY + 46.dp.toPx() + driftY * 0.5f)
            )
            particles.forEachIndexed { i, offset ->
                drawCircle(
                    color = if (i % 2 == 0) cyanColor else blueColor,
                    radius = (if (i % 2 == 0) 1.5f else 1.1f).dp.toPx(),
                    center = offset,
                    alpha = if (isOffline) 0.15f else 0.65f
                )
            }

            // Majestic Jarvis-like outer breathing aura bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cyanColor.copy(alpha = if (isOffline) auraAlpha * 0.3f else auraAlpha),
                        cyanColor.copy(alpha = if (isOffline) auraAlpha * 0.12f else auraAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, rigY),
                    radius = auraBloom.dp.toPx()
                ),
                radius = auraBloom.dp.toPx(),
                center = Offset(cx, rigY)
            )

            // 5. Central Glowing Core Assembly
            // Outer cloud glow (radius = 46)
            val currentCoreScale = when (state) {
                CoreState.LISTENING -> 1.0f + (rmsDb.coerceIn(0f, 10f) / 25f)
                CoreState.SPEAKING -> fastPulse
                CoreState.ERROR -> 0.98f + sin(System.currentTimeMillis() / 80.0).toFloat() * 0.04f
                else -> breatheScale
            }

            scale(currentCoreScale, pivot = Offset(cx, rigY)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cyanColor.copy(alpha = if (isOffline) 0.2f else 0.75f),
                            cyanColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(cx, rigY),
                        radius = 46.dp.toPx()
                    ),
                    radius = 46.dp.toPx(),
                    center = Offset(cx, rigY)
                )

                // Mid core (radius = 26)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            cyanColor.copy(alpha = if (isOffline) 0.3f else 0.85f),
                            blueColor.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(cx, rigY),
                        radius = 26.dp.toPx()
                    ),
                    radius = 26.dp.toPx(),
                    center = Offset(cx, rigY)
                )

                // Scanning sweep line (horizontal laser glow clipped dynamically inside the core boundary)
                val scanLineY = rigY + scanSweepY.coerceIn(-24f, 24f)
                val coreRadius = 26.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isOffline) 0.2f else 0.9f),
                            cyanColor.copy(alpha = if (isOffline) 0.1f else 0.8f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(cx - coreRadius * 0.9f, scanLineY),
                    end = Offset(cx + coreRadius * 0.9f, scanLineY),
                    strokeWidth = 2.dp.toPx()
                )

                // Hot core (radius = 13)
                drawCircle(
                    color = if (isOffline) Color(0xFF5B6670) else Color(0xFFF5FEFF),
                    radius = 13.dp.toPx(),
                    center = Offset(cx, rigY)
                )
            }
        }

        // Central text identity overlay: "95"
        val centerRigY = 240.dp / 2.1f + floatY.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = centerRigY - 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "95",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isOffline) Color(0xFF1E293B) else Color(0xFF030712),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.alpha(if (state == CoreState.THINKING) 0.35f else 0.85f)
            )
        }

        // Holographic voice wave visualizer overlay (appears when LISTENING or SPEAKING)
        if (state == CoreState.LISTENING || state == CoreState.SPEAKING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Render 5 animated visualizer bars
                val barCount = 5
                for (i in 0 until barCount) {
                    val scaleOffset = sin((System.currentTimeMillis() / 150.0) + i * 0.7).toFloat()
                    val heightMultiplier = if (state == CoreState.LISTENING) {
                        (rmsDb.coerceIn(0f, 10f) * 1.5f + 4f)
                    } else {
                        16f
                    }
                    val barHeight = (8f + (scaleOffset + 1.1f) * heightMultiplier).coerceIn(4f, 40f)

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFEAFCFF),
                                        Color(0xFF4FD8FF)
                                    )
                                ),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
    }
}
