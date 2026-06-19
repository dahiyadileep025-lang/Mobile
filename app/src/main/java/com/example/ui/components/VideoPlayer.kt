package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoItem
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0.12f) }
    var isMuted by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var controlsTimeoutSec by remember { mutableStateOf(4) }

    // Controls display auto-fade
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            while (controlsTimeoutSec > 0) {
                delay(1000)
                controlsTimeoutSec -= 1
            }
            showControls = false
        }
    }

    // Incremental progress loop when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(500)
                progress += 0.005f
                if (progress >= 1.0f) {
                    progress = 0.0f
                }
            }
        }
    }

    // Continuous float value to drive animations inside the Canvas drawing loop
    val infiniteTransition = rememberInfiniteTransition(label = "playerAnimation")
    val animationFrameTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frameTime"
    )

    // Calculate elapsed and remaining times
    val currentSecs = (video.durationSec * progress).toInt()
    val totalSecs = video.durationSec
    
    val currentFormatted = formatDuration(currentSecs)
    val totalFormatted = formatDuration(totalSecs)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .clickable {
                showControls = !showControls
                controlsTimeoutSec = 4
            }
            .testTag("video_player_container")
    ) {
        // --- 60FPS VECTOR CANVAS VISUALIZER ---
        val style = video.videoStyle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2

            when (style) {
                "Chill Wave" -> {
                    // Slow moving serene gradient waves
                    val gradient = Brush.radialGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                        center = Offset(centerX, centerY),
                        radius = width
                    )
                    drawRect(gradient)

                    val pathCount = 3
                    for (i in 0 until pathCount) {
                        val path = Path()
                        val waveColor = when (i) {
                            0 -> Color(0xFF38BDF8).copy(alpha = 0.4f)
                            1 -> Color(0xFF818CF8).copy(alpha = 0.3f)
                            else -> Color(0xFFC084FC).copy(alpha = 0.2f)
                        }
                        
                        val phase = animationFrameTime + (i * Math.PI.toFloat() / 2f)
                        val amplitude = height * 0.15f * (1f - i * 0.2f)
                        val frequency = 0.015f - (i * 0.002f)

                        path.moveTo(0f, centerY)
                        for (x in 0..width.toInt() step 5) {
                            val y = centerY + amplitude * sin(x * frequency + phase)
                            path.lineTo(x.toFloat(), y)
                        }
                        path.lineTo(width, height)
                        path.lineTo(0f, height)
                        path.close()
                        drawPath(path, waveColor)
                    }
                }

                "Tech Wave" -> {
                    // Futuristic green code scanning bars
                    drawRect(SolidColor(Color(0xFF06150F)))

                    // Draw grid
                    val gridUnits = 12
                    for (i in 1..gridUnits) {
                        val x = width * (i.toFloat() / gridUnits)
                        drawLine(
                            color = Color(0xFF10B981).copy(alpha = 0.07f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                        val y = height * (i.toFloat() / gridUnits)
                        drawLine(
                            color = Color(0xFF10B981).copy(alpha = 0.07f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Equalizer bars
                    val barCount = 18
                    val barSpacing = width / (barCount + 1)
                    for (j in 1..barCount) {
                        val barX = j * barSpacing
                        val customPhase = animationFrameTime * 1.5f + j * 0.7f
                        val heightMultiplier = 0.3f + 0.35f * sin(customPhase)
                        val barHeight = height * 0.6f * heightMultiplier
                        
                        drawLine(
                            color = Color(0xFF10B981),
                            start = Offset(barX, centerY + barHeight / 2),
                            end = Offset(barX, centerY - barHeight / 2),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                "Retro Grid" -> {
                    // Infinite synthwave perspective grid and rising sun
                    val spaceBackground = Brush.linearGradient(
                        colors = listOf(Color(0xFF03001E), Color(0xFF7303C0), Color(0xFFEC38BC)),
                        start = Offset(0f, 0f),
                        end = Offset(0f, height)
                    )
                    drawRect(spaceBackground)

                    // Draw rising sun
                    val sunRadius = height * 0.28f
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF0D72), Color(0xFFFFD300)),
                            startY = centerY - sunRadius,
                            endY = centerY
                        ),
                        radius = sunRadius,
                        center = Offset(centerX, centerY - 15f)
                    )

                    // Retrowave ground horizon divider
                    val horizonY = centerY + 15f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D0221), Color(0xFF000000)),
                            startY = horizonY,
                            endY = height
                        ),
                        topLeft = Offset(0f, horizonY),
                        size = androidx.compose.ui.geometry.Size(width, height - horizonY)
                    )

                    // Perspective grid lines
                    val lineCount = 14
                    for (k in 0 until lineCount) {
                        val ratio = k.toFloat() / (lineCount - 1)
                        val xEnd = width * ratio
                        drawLine(
                            color = Color(0xFFEC38BC).copy(alpha = 0.8f),
                            start = Offset(centerX, horizonY),
                            end = Offset(xEnd, height),
                            strokeWidth = 3f
                        )
                    }

                    // Flowing horizontal bars
                    val horizCount = 5
                    for (h in 0 until horizCount) {
                        val flowRatio = ((h.toFloat() / horizCount) + ((animationFrameTime / (2 * Math.PI.toFloat())) * 0.2f)) % 1f
                        val gridHorizonY = horizonY + (height - horizonY) * flowRatio
                        drawLine(
                            color = Color(0xFFEC38BC).copy(alpha = 0.6f * flowRatio),
                            start = Offset(0f, gridHorizonY),
                            end = Offset(width, gridHorizonY),
                            strokeWidth = 1f + 5f * flowRatio
                        )
                    }
                }

                "Neon Pulse" -> {
                    // Techno pulsing circles
                    drawRect(SolidColor(Color(0xFF080112)))

                    val circleCount = 4
                    for (c in 1..circleCount) {
                        val sizeMultiplier = ((c.toFloat() / circleCount) + ((animationFrameTime / (2 * Math.PI.toFloat())) * 0.25f)) % 1f
                        val radius = width * 0.35f * sizeMultiplier
                        val strokeColor = when (c % 3) {
                            0 -> Color(0xFFFF007F).copy(alpha = 1f - sizeMultiplier)
                            1 -> Color(0xFF00F5FF).copy(alpha = 1f - sizeMultiplier)
                            else -> Color(0xFFFFFF00).copy(alpha = 1f - sizeMultiplier)
                        }

                        drawCircle(
                            color = strokeColor,
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 2f + sizeMultiplier * 6f)
                        )
                    }

                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = 40f,
                        center = Offset(centerX, centerY)
                    )
                }

                else -> {
                    // Cyberpunk cosmic rotating rings
                    val darkSpace = Brush.verticalGradient(colors = listOf(Color(0xFF0B0014), Color(0xFF16002A)))
                    drawRect(darkSpace)

                    val rotationRad = animationFrameTime
                    val numPoints = 8
                    val orbitRadius = height * 0.22f
                    for (p in 0 until numPoints) {
                        val angle = rotationRad + (p.toFloat() / numPoints) * 2 * Math.PI.toFloat()
                        val orbitingX = centerX + orbitRadius * cos(angle)
                        val orbitingY = centerY + orbitRadius * sin(angle)

                        drawCircle(
                            color = Color(0xFFFF3B30).copy(alpha = 0.8f),
                            radius = 12f,
                            center = Offset(orbitingX, orbitingY)
                        )
                        drawLine(
                            color = Color(0xFFFFCC00).copy(alpha = 0.2f),
                            start = Offset(centerX, centerY),
                            end = Offset(orbitingX, orbitingY),
                            strokeWidth = 2f
                        )
                    }

                    drawCircle(
                        color = Color(0xFFFFCC00),
                        radius = 35f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        // --- OVERLAY CONTROLS (FADED OUT IF IDLE) ---
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                // Header (App and playing title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE VISUALS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Play / Pause core buttons using unicode cinematic symbols
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    // Rewind
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                progress = (progress - 0.05f).coerceAtLeast(0.0f)
                                controlsTimeoutSec = 4
                            }
                    ) {
                        Text("◀◀", color = Color.White, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Play/Pause Toggle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                isPlaying = !isPlaying
                                controlsTimeoutSec = 4
                            }
                            .testTag("play_pause_button")
                    ) {
                        Text(
                            text = if (isPlaying) "❚❚" else "▶",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(x = if (isPlaying) 0.dp else 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Fast Forward
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                progress = (progress + 0.05f).coerceAtMost(1.0f)
                                controlsTimeoutSec = 4
                            }
                    ) {
                        Text("▶▶", color = Color.White, fontSize = 14.sp)
                    }
                }

                // Volume and video progression footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$currentFormatted / $totalFormatted",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Mute button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    isMuted = !isMuted
                                    controlsTimeoutSec = 4
                                }
                        ) {
                            Text(
                                text = if (isMuted) "🔇" else "🔊",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Scrubber track slider
                    Slider(
                        value = progress,
                        onValueChange = {
                            progress = it
                            controlsTimeoutSec = 4
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("video_player_slider")
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%d:%02d", mins, secs)
    }
}
