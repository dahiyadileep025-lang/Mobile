package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VideoItem
import com.example.data.Comment
import com.example.ui.MobileTubeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: MobileTubeViewModel,
    onDirectChatRequest: (String) -> Unit, // Direct callback to trigger chat tab with selected creator email
    modifier: Modifier = Modifier
) {
    val shortsList by viewModel.shortsState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var currentIdx by remember { mutableStateOf(0) }
    var dragAccumulator by remember { mutableStateOf(0f) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    if (shortsList.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Ensure index stays in bounds
    val activeIdx = currentIdx.coerceIn(0, shortsList.size - 1)
    val activeShort = shortsList[activeIdx]

    // Bottom-sheet style comments flow
    val currentPlayingVideoId by viewModel.currentPlayingVideoId.collectAsState()
    val comments by viewModel.currentVideoComments.collectAsState()

    // Sync playing ID with viewModel so context comments are fetched
    LaunchedEffect(activeShort.id) {
        viewModel.playVideo(activeShort)
    }

    // Capture swipe gestures for vertical scrolling
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (dragAccumulator < -80f) {
                            if (currentIdx < shortsList.size - 1) {
                                currentIdx += 1
                            }
                        } else if (dragAccumulator > 80f) {
                            if (currentIdx > 0) {
                                currentIdx -= 1
                            }
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = { dragAccumulator = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount.y
                    }
                )
            }
            .testTag("shorts_screen_container")
    ) {
        // --- ANIMATED VECTOR CANVAS BACKDROP (9:16 vertical layout) ---
        ShortsCanvasVisualizer(style = activeShort.videoStyle)

        // Gradient cover for readable overlay text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // --- ACCESSIBILITY NEXT/PREV NAVIGATION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Shorts",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Red)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Arrow Taps for seamless accessibility
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { if (currentIdx > 0) currentIdx -= 1 },
                    enabled = currentIdx > 0,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Short")
                }

                IconButton(
                    onClick = { if (currentIdx < shortsList.size - 1) currentIdx += 1 },
                    enabled = currentIdx < shortsList.size - 1,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Short")
                }
            }
        }

        // --- BOTTOM INFO OVERLAY ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(bottom = 80.dp) // Leave room for standard bottom navigation bars
                .padding(horizontal = 16.dp)
                .fillMaxWidth(0.78f)
        ) {
            // Profile & Subscribe Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = activeShort.creatorAvatar,
                    contentDescription = activeShort.creatorName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                )
                
                Column {
                    Text(
                        text = activeShort.creatorName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeShort.creatorEmail,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Inline Subscribe button
                val isSubscribed by viewModel.isSubscribedToActiveCreator.collectAsState()
                Button(
                    onClick = { viewModel.toggleSubscriptionActiveCreator() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) Color.White.copy(alpha = 0.25f) else Color.Red,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Short title & hashtags
            Text(
                text = activeShort.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = activeShort.description,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Running audio track loop signifier
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Music Loop",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Original Sound - ${activeShort.creatorName}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- RIGHT OVERLAY ACTION BUTTONS COLUMN ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 90.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. LIKE BUTTON
            val isLiked by viewModel.isLikedActiveVideo.collectAsState()
            IconButton(
                onClick = { viewModel.toggleLikeActiveVideo() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like Short",
                    tint = if (isLiked) Color.Red else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "${activeShort.likes + (if (isLiked) 1 else 0)}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // 2. COMMENTS BOTTOM TRAY TRIGGER
            IconButton(
                onClick = { showCommentSheet = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Comments button",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "${comments.size}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // 3. SECURE DIRECT CHAT TO "CHEAT/CHAT" WITH CREATOR
            IconButton(
                onClick = { 
                    viewModel.selectChatRecipient(activeShort.creatorEmail)
                    onDirectChatRequest(activeShort.creatorEmail)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Chat with Creator",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Chat",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            // 4. SHARE TOOL
            IconButton(
                onClick = { showShareDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Share",
                color = Color.White,
                fontSize = 11.sp
            )

            // 5. DECORATIVE ROTATING VINYL MUSIC DISC
            val infiniteTransition = rememberInfiniteTransition(label = "discRotate")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angle"
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer(rotationZ = angle)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = activeShort.creatorAvatar,
                    contentDescription = "Disc",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
            }
        }
    }

    // --- DIALOGS AND BOTTOM SHEET COMMENT SLIDE UP OVERLAYS ---
    if (showCommentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showCommentSheet = false }) {
                        Icon(Icons.Default.Clear, contentDescription = "Close comments")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                // List of Comments
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No comments yet. Start the conversation! 😎",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(comments) { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AsyncImage(
                                    model = comment.userAvatar,
                                    contentDescription = comment.userName,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = comment.userName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "• 1m ago",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comment.content,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Creator Quick Smart Reply suggestions
                                    val currentUser by viewModel.currentUserState.collectAsState()
                                    if (currentUser != null && activeShort.creatorEmail == currentUser!!.email && !comment.userName.contains("[Creator]")) {
                                        Text(
                                            text = "✨ Co-pilot Auto-Reply",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.triggerAICommentReply(comment)
                                                }
                                                .padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Comment entry field
                var commentInput by remember { mutableStateOf("") }
                val currentUser by viewModel.currentUserState.collectAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUser != null) {
                        AsyncImage(
                            model = currentUser!!.avatarUrl,
                            contentDescription = "Me",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("Add interactive comment...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (commentInput.isNotBlank()) {
                                        viewModel.postComment(commentInput)
                                        commentInput = ""
                                    }
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "PostComment", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    } else {
                        Button(
                            onClick = { viewModel.showSignIn() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Sign In using Google to Add Comment", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share this Viral Short") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "https://mobiletube.ai/shorts/${activeShort.id}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Copy link to clipboard to share beautiful 9:16 vertical stream with friends!", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showShareDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun ShortsCanvasVisualizer(style: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "shortsVisualizer")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        when (style) {
            "Tech Wave" -> {
                // Vector grid and coding stream matrices
                drawRect(Color(0xFF0D0F12))
                
                // Neon blue lines moving up
                val lineSpacing = 80f
                val offset = (animTime / (2 * Math.PI).toFloat() * lineSpacing)
                for (y in 0..height.toInt() step lineSpacing.toInt()) {
                    val currentY = y + offset
                    drawLine(
                        color = Color(0xFF00FFCC).copy(alpha = 0.15f),
                        start = Offset(0f, currentY),
                        end = Offset(width, currentY),
                        strokeWidth = 2f
                    )
                }
                
                // Glowing vector waveforms
                val path = Path()
                path.moveTo(0f, height * 0.5f)
                for (x in 0..width.toInt() step 5) {
                    val sine = sin(x * 0.015f + animTime * 1.5f) * cos(x * 0.005f)
                    val yY = height * 0.5f + sine * 180f
                    path.lineTo(x.toFloat(), yY)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF00E6FF),
                    style = Stroke(width = 6f)
                )

                // Secondary overlapping wave
                val path2 = Path()
                path2.moveTo(0f, height * 0.55f)
                for (x in 0..width.toInt() step 5) {
                    val sine = sin(x * 0.02f - animTime * 2.0f) * 120f
                    path2.lineTo(x.toFloat(), height * 0.55f + sine)
                }
                drawPath(
                    path = path2,
                    color = Color(0xFFFF007F).copy(alpha = 0.5f),
                    style = Stroke(width = 3f)
                )
            }
            "Chill Wave" -> {
                // Calm purple stars and floating dust visualizer
                val gradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1035), Color(0xFF0F051D))
                )
                drawRect(brush = gradient)
                
                // Floating starry particles
                val starCount = 20
                for (i in 0 until starCount) {
                    val angleOffset = i * (2 * Math.PI / starCount)
                    val starX = (width * 0.5f) + cos(animTime + angleOffset).toFloat() * (width * 0.35f)
                    val starY = (height * 0.45f) + sin(animTime * 0.5f + angleOffset).toFloat() * (height * 0.3f)
                    val sizePt = (sin(animTime * 2 + i) * 3f + 5f).coerceAtLeast(2f)
                    
                    drawCircle(
                        color = Color(0xFFFFCC33).copy(alpha = 0.6f),
                        radius = sizePt,
                        center = Offset(starX, starY)
                    )
                }

                // Breathing circular sound disk in center
                val baseRadius = width * 0.3f
                val breathe = baseRadius + sin(animTime * 3f) * 20f
                drawCircle(
                    color = Color(0xFF9E00FF).copy(alpha = 0.15f),
                    radius = breathe + 40f,
                    center = Offset(width * 0.5f, height * 0.45f)
                )
                drawCircle(
                    color = Color(0xFFFF00AA).copy(alpha = 0.25f),
                    radius = breathe,
                    center = Offset(width * 0.5f, height * 0.45f)
                )
            }
            "Retro Grid" -> {
                // Classic 80s synth grid
                drawRect(Color(0xFF050014))
                
                // Draw glowing Sun in center
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF007F), Color(0xFFFFD700))
                    ),
                    radius = width * 0.35f,
                    center = Offset(width * 0.5f, height * 0.45f)
                )

                // Sun horizontal slices (80s look!)
                val sliceHeight = 8f
                for (y in (height * 0.3f).toInt()..(height * 0.5f).toInt() step 16) {
                    drawRect(
                        color = Color(0xFF050014),
                        topLeft = Offset(0f, y.toFloat()),
                        size = Size(width, sliceHeight)
                    )
                }

                // Perspective Neon Grid moving towards viewer
                val gridYStart = height * 0.55f
                val gridHeight = height - gridYStart
                val vanishingPoint = Offset(width * 0.5f, gridYStart)
                
                // Perspective vertical rays
                val rayCount = 14
                for (i in 0..rayCount) {
                    val pct = i.toFloat() / rayCount
                    val bottomX = width * pct
                    drawLine(
                        color = Color(0xFF00FFCC).copy(alpha = 0.4f),
                        start = vanishingPoint,
                        end = Offset(bottomX, height),
                        strokeWidth = 3f
                    )
                }

                // Moving horizontal grid lines
                val horizontalCount = 8
                val cycleOffset = (animTime / (2 * Math.PI).toFloat())
                for (i in 0 until horizontalCount) {
                    val rawIndex = (i + cycleOffset) / horizontalCount
                    // Exponential spacing for 3D realism
                    val progressY = Math.pow(rawIndex.toDouble(), 2.0).toFloat()
                    val lineY = gridYStart + progressY * gridHeight
                    drawLine(
                        color = Color(0xFFFF007F).copy(alpha = 0.5f),
                        start = Offset(0f, lineY),
                        end = Offset(width, lineY),
                        strokeWidth = 2f
                    )
                }
            }
            else -> {
                // Cosmic dark neon default visualizer
                val gradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFF070B19), Color(0xFF02040A))
                )
                drawRect(brush = gradient)

                // Abstract orbital rings
                val numRings = 4
                for (r in 1..numRings) {
                    translate(left = width / 2, top = height * 0.45f) {
                        val ringAngle = animTime * (1.2f / r) * (if (r % 2 == 0) 1 else -1)
                        rotate(degrees = ringAngle * 57.295f) {
                            drawOval(
                                color = Color(0xFF00E6FF).copy(alpha = 0.3f / r),
                                size = Size(200f * r, 80f * r),
                                topLeft = Offset(-100f * r, -40f * r),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }

                // Core pulsing particle
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 12f + sin(animTime * 4f) * 4f,
                    center = Offset(width * 0.5f, height * 0.45f)
                )
            }
        }
    }
}
