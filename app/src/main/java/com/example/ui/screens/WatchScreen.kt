package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Comment
import com.example.data.VideoItem
import com.example.ui.MobileTubeViewModel
import com.example.ui.components.VideoPlayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    viewModel: MobileTubeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVideo by viewModel.currentPlayingVideo.collectAsState()
    val comments by viewModel.currentVideoComments.collectAsState()
    val isLiked by viewModel.isLikedActiveVideo.collectAsState()
    val isSubscribed by viewModel.isSubscribedToActiveCreator.collectAsState()
    val currentUser by viewModel.currentUserState.collectAsState()

    val scope = rememberCoroutineScope()
    var commentText by remember { mutableStateOf("") }
    var isDescExpanded by remember { mutableStateOf(false) }
    var aiReplyGeneratingCommentId by remember { mutableStateOf<Int?>(null) }

    if (currentVideo == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onBack) {
                Text("Select video to play")
            }
        }
        return
    }

    val video = currentVideo!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("watch_back_btn")) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Playing: ${video.videoStyle}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // 2. Playback Animation
        VideoPlayer(
            video = video,
            modifier = Modifier.fillMaxWidth()
        )

        // Metadata scroll panel
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = video.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 22.sp
                    )

                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatViews(video.views)} views  •  June 2026",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Actions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button
                        val likeTint by animateColorAsState(
                            targetValue = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        InputChip(
                            selected = isLiked,
                            onClick = { viewModel.toggleLikeActiveVideo() },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Like button",
                                    tint = likeTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isLiked) "${video.likes + 1}" else "${video.likes}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = likeTint
                                )
                            },
                            border = null,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("video_like_btn")
                        )

                        // Dislike Button
                        InputChip(
                            selected = false,
                            onClick = {},
                            leadingIcon = {
                                Text(
                                    text = "👎",
                                    fontSize = 14.sp
                                )
                            },
                            label = { Text("Dislike", fontSize = 11.sp) },
                            border = null,
                            shape = RoundedCornerShape(16.dp)
                        )

                        // Share Button
                        InputChip(
                            selected = false,
                            onClick = {},
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share video",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Share", fontSize = 11.sp) },
                            border = null,
                            shape = RoundedCornerShape(16.dp)
                        )
                        
                        // Revenue Indicator
                        if (currentUser != null && video.creatorEmail == currentUser?.email) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "Your Upload ($${String.format("%.2f", video.revenueGenerated)} revenue)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }

                    // Creator info row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = video.creatorAvatar,
                            contentDescription = "Creator Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = video.creatorName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(0xFF2E7D32), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✔",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = "Subscribers coming active",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Subscribe Button
                        if (currentUser == null || video.creatorEmail != currentUser?.email) {
                            Button(
                                onClick = { viewModel.toggleSubscriptionActiveCreator() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("subscribe_creator_btn")
                            ) {
                                Text(
                                    text = if (isSubscribed) "Subscribed" else "Subscribe",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Collapsible Description
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { isDescExpanded = !isDescExpanded }
                            .testTag("description_expand_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Description Details",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isDescExpanded) "▲" else "▼",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = video.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isDescExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Fan Comments (${comments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (currentUser != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = currentUser!!.avatarUrl,
                                contentDescription = "My avatar",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Add comment as creator...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("comment_input_box"),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (commentText.isNotBlank()) {
                                                viewModel.postComment(commentText)
                                                commentText = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("comment_submit_arrow_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Post",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { viewModel.showSignIn() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Sign in", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with Google to post comments & subscribe", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Comments lists items
            if (comments.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text("💬", fontSize = 28.sp)
                        Text(
                            text = "No comments yet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Be the first to share your thoughts on this loop!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    CommentItemView(
                        comment = comment,
                        isMyCreatedVideo = currentUser?.email == video.creatorEmail,
                        isGeneratingReply = aiReplyGeneratingCommentId == comment.id,
                        onTriggerAIReply = {
                            aiReplyGeneratingCommentId = comment.id
                            scope.launch {
                                viewModel.triggerAICommentReply(comment)
                                aiReplyGeneratingCommentId = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItemView(
    comment: Comment,
    isMyCreatedVideo: Boolean,
    isGeneratingReply: Boolean,
    onTriggerAIReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("comment_item_${comment.id}")
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = comment.userAvatar,
                contentDescription = "${comment.userName} Comment Avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.userName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1 day ago",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = comment.content,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )

                if (!comment.userName.contains("[Creator]")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isGeneratingReply) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Gemini AI thinking...",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable { onTriggerAIReply() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Smart toy / robot emoji representation
                                Text("🤖", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMyCreatedVideo) "Creator Reply with AI" else "Developer Auto-Reply with AI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
    }
}

fun formatViews(views: Int): String {
    return when {
        views >= 1_000_000 -> "${String.format("%.1f", views / 1_000_000.0)}M"
        views >= 1_000 -> "${String.format("%.1f", views / 1_000.0)}K"
        else -> views.toString()
    }
}
