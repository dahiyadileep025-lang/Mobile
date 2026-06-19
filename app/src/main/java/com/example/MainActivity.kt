package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoItem
import com.example.ui.MobileTubeViewModel
import com.example.ui.components.GmailSignInDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WatchScreen
import com.example.ui.screens.CreatorStudioScreen
import com.example.ui.screens.EarningsHubScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.ChatScreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send

class MainActivity : ComponentActivity() {
    private val viewModel: MobileTubeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MobileTubeAppShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MobileTubeAppShell(viewModel: MobileTubeViewModel) {
    var activeTab by remember { mutableStateOf("Home") }
    val currentPlayingVideo by viewModel.currentPlayingVideo.collectAsState()
    val showGmailDialog by viewModel.showGmailSignInDialog.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                // Bottom layout bar
                if (currentPlayingVideo == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "Home",
                            onClick = { activeTab = "Home" },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        NavigationBarItem(
                            selected = activeTab == "Shorts",
                            onClick = { activeTab = "Shorts" },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Shorts") },
                            label = { Text("Shorts", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_shorts")
                        )

                        NavigationBarItem(
                            selected = activeTab == "Studio",
                            onClick = { activeTab = "Studio" },
                            icon = { Icon(Icons.Default.AddCircle, contentDescription = "Studio") },
                            label = { Text("Studio", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_studio")
                        )

                        NavigationBarItem(
                            selected = activeTab == "Chat",
                            onClick = { activeTab = "Chat" },
                            icon = { Icon(Icons.Default.Send, contentDescription = "Chat") },
                            label = { Text("Chats", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_chat")
                        )

                        NavigationBarItem(
                            selected = activeTab == "Earnings",
                            onClick = { activeTab = "Earnings" },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Earnings") },
                            label = { Text("Earnings", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_earnings")
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (currentPlayingVideo != null) 0.dp else innerPadding.calculateBottomPadding())
            ) {
                // Conditional screens
                when (activeTab) {
                    "Home" -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onVideoSelected = { video ->
                                viewModel.playVideo(video)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "Shorts" -> {
                        ShortsScreen(
                            viewModel = viewModel,
                            onDirectChatRequest = { creatorEmail ->
                                activeTab = "Chat"
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "Studio" -> {
                        CreatorStudioScreen(
                            viewModel = viewModel,
                            onPublishSuccess = {
                                activeTab = "Home"
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "Chat" -> {
                        ChatScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "Earnings" -> {
                        EarningsHubScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Overlay Watch Screen for active video immersion
                AnimatedVisibility(
                    visible = currentPlayingVideo != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentPlayingVideo != null) {
                        WatchScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.stopVideo()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Google simulated single tap pre-auth
                if (showGmailDialog) {
                    GmailSignInDialog(
                        onDismiss = { viewModel.hideSignIn() },
                        onAccountSelected = { email, name ->
                            viewModel.handleGmailLogin(email, name)
                        }
                    )
                }
            }
        }
    }
}
