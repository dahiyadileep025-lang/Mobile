package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MobileTubeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(
    viewModel: MobileTubeViewModel,
    onPublishSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUserState.collectAsState()
    val isGenerating by viewModel.isGeneratingMetadata.collectAsState()
    val aiStatusText by viewModel.isAIPartsStatus.collectAsState()

    var designIdea by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Chill Wave") }
    
    var finalTitle by remember { mutableStateOf("") }
    var finalDesc by remember { mutableStateOf("") }
    var publishAsShort by remember { mutableStateOf(false) }
    var publishSuccessShow by remember { mutableStateOf(false) }

    val styleOptions = listOf("Chill Wave", "Tech Wave", "Retro Grid", "Neon Pulse", "Cyberpunk Cosmic")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎬", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Creator Studio Hub",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🔒", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Gmail Account Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "To access Creator Studio monetization and post content to the MobileTube feed, you must sign in with Google.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Button(
                        onClick = { viewModel.showSignIn() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("studio_lock_signin_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Sign In", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Gmail Account")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Publish New Stream",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enter a basic video concept, category, and utilize our on-board Gemini Artificial Intelligence to draft high RPM viral content formats instantly!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (publishSuccessShow) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F5132)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉 Video Published Successfully!", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("It is now live in the global feed. Viewers are starting to generate ad earnings for you!", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { publishSuccessShow = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Text("Upload Another", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = "Step 1: Ideate concept outline",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = designIdea,
                onValueChange = { designIdea = it },
                label = { Text("Rough video outlines / Keywords") },
                placeholder = { Text("e.g. Chill space lofi beats to rest with planets visualizer loops") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .testTag("studio_idea_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Text(
                text = "Step 2: Choose player animation template",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ScrollableStyleSelector(
                options = styleOptions,
                selected = selectedStyle,
                onSelect = { selectedStyle = it }
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isGenerating) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.optimizeMetadataWithGemini(designIdea, selectedStyle) { title, desc ->
                                finalTitle = title
                                finalDesc = desc
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("studio_ai_optimize_btn")
                    ) {
                        Text("✨ Draft Viral Metadata via Gemini Co-Pilot", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Text(
                text = "Step 3: Review publication details",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = finalTitle,
                onValueChange = { finalTitle = it },
                label = { Text("Viral Video Title") },
                placeholder = { Text("AI generated catch title will appear here") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_final_title"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = finalDesc,
                onValueChange = { finalDesc = it },
                label = { Text("Optimized Video Description") },
                placeholder = { Text("SEO rich description tags will populate automatically") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("studio_final_desc"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { publishAsShort = !publishAsShort }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (publishAsShort) "📱" else "📺", fontSize = 24.sp)
                    Column {
                        Text(
                            text = if (publishAsShort) "Publish as Vertical Short (9:16)" else "Publish as Regular Video (16:9)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (publishAsShort) "Appears in dynamic Shorts vertical feed." else "Appears in primary landscape Home feed.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = publishAsShort,
                    onCheckedChange = { publishAsShort = it },
                    modifier = Modifier.testTag("publish_short_switch")
                )
            }

            Button(
                onClick = {
                    if (finalTitle.isNotBlank()) {
                        viewModel.publishCustomVideo(finalTitle, finalDesc, selectedStyle, isShort = publishAsShort) {
                            designIdea = ""
                            finalTitle = ""
                            finalDesc = ""
                            publishAsShort = false
                            publishSuccessShow = true
                            onPublishSuccess()
                        }
                    }
                },
                enabled = finalTitle.isNotBlank() && !isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 16.dp)
                    .testTag("studio_publish_submit_btn")
            ) {
                Text("☁ Publish to MobileTube Feed", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScrollableStyleSelector(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { style ->
            val isSelected = selected == style
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .clickable { onSelect(style) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.replace(" ", "\n"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
