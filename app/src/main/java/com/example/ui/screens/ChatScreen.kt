package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.ChatMessage
import com.example.ui.MobileTubeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MobileTubeViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUserState.collectAsState()
    val activeRecipient by viewModel.activeChatRecipientEmail.collectAsState()
    val messages by viewModel.chatMessagesState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Scroll to bottom when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // List of active channels/creators to DM
    val recipientsList = listOf(
        ChatPartner(
            email = "global",
            name = "Global Dev Lounge",
            status = "💬 42 Online • Active Discussion",
            avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=global_lobby"
        ),
        ChatPartner(
            email = "lofi.beats@gmail.com",
            name = "Lofi Odyssey Music",
            status = "🎵 Streaming: Cosmic Chill beats",
            avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=lofi"
        ),
        ChatPartner(
            email = "tech_insider@gmail.com",
            name = "Tech Insider Lab",
            status = "💻 Coding: Compose State deep dive",
            avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=tech"
        ),
        ChatPartner(
            email = "dreamweaver@gmail.com",
            name = "Retro Dreamweaver",
            status = "💜 Vibing: Retro Grid shader tests",
            avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=synth"
        ),
        ChatPartner(
            email = "physic_guru@gmail.com",
            name = "Quantum Guru Studio",
            status = "🧠 Lecture: Coherent superpositions",
            avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=physics"
        )
    )

    val currentPartner = recipientsList.find { it.email == activeRecipient } ?: recipientsList[0]

    // Mobile layout: Sidebar collapsible, tablet layout: side-by-side
    var showMobileChannelsList by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxSize()) {
        // --- DM CHANNELS PANEL --- (Always visible on tablets, collapsible on mobile)
        Surface(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                .weight(if (showMobileChannelsList) 1f else 0.0001f) // Toggle for mobile collapsing
                .animateContentSize(),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header
                Text(
                    text = "Lobby & DM Channels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recipientsList) { partner ->
                        val isSelected = activeRecipient == partner.email
                        Card(
                            onClick = {
                                viewModel.selectChatRecipient(partner.email)
                                showMobileChannelsList = false // collapse after clicking on mobile
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_recipient_${partner.email}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = partner.avatarUrl,
                                    contentDescription = partner.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = partner.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = partner.status,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- ACTIVE CONVERSATION SHEET ---
        Column(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = currentPartner.avatarUrl,
                            contentDescription = currentPartner.name,
                            modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                        )
                        Column {
                            Text(
                                text = currentPartner.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentPartner.status,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    // Mobile back icon to toggle channels panel
                    IconButton(onClick = { showMobileChannelsList = !showMobileChannelsList }) {
                        Icon(
                            imageVector = if (showMobileChannelsList) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = "Channels Drawer Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (currentPartner.email != "global") {
                        IconButton(onClick = { /* Simulated call, fully compliant */ }) {
                            Icon(Icons.Default.Call, contentDescription = "Simulated Call", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )

            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Message Feed list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isMyMessage = currentUser != null && message.senderEmail == currentUser!!.email
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (!isMyMessage) {
                            AsyncImage(
                                model = message.senderAvatar,
                                contentDescription = message.senderName,
                                modifier = Modifier
                                    .padding(end = 6.dp, top = 4.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Column(
                            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
                        ) {
                            if (!isMyMessage) {
                                Text(
                                    text = message.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMyMessage) 16.dp else 0.dp,
                                    bottomEnd = if (isMyMessage) 0.dp else 16.dp
                                ),
                                color = if (isMyMessage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (isMyMessage) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.widthIn(max = 240.dp)
                            ) {
                                Text(
                                    text = message.content,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Input panel
            var messageText by remember { mutableStateOf("") }
            
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentUser != null) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Search or type messages to ${currentPartner.name}...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (messageText.isNotBlank()) {
                                            viewModel.sendChatMessage(messageText)
                                            messageText = ""
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send message icon",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    } else {
                        Button(
                            onClick = { viewModel.showSignIn() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Google Sign In to Join Discussion Lounge", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

data class ChatPartner(
    val email: String,
    val name: String,
    val status: String,
    val avatarUrl: String
)
