package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import com.example.data.GeminiService
import com.example.ui.MobileTubeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsHubScreen(
    viewModel: MobileTubeViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUserState.collectAsState()
    val scope = rememberCoroutineScope()

    var showCashOutDialog by remember { mutableStateOf(false) }
    var walletType by remember { mutableStateOf("Simulated Bank Transfer") }
    var isProcessingCashOut by remember { mutableStateOf(false) }
    var cashOutSuccessShow by remember { mutableStateOf(false) }
    var cashedOutAmount by remember { mutableStateOf(0.0) }

    // Gemini monetization suggestions state
    var isGeneratingIdeas by remember { mutableStateOf(false) }
    var generatedIdeasResult by remember { mutableStateOf<List<String>?>(null) }
    var ideasError by remember { mutableStateOf("") }

    // Simulated transaction history list
    var completedReceipts by remember { 
        mutableStateOf(
            listOf(
                TransactionReceipt("May 15, 2026", "$118.40", "Completed", "PayPal Standard"),
                TransactionReceipt("April 10, 2026", "$64.10", "Completed", "Payoneer Direct")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💰", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Monetization & Balance Hub",
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
                    Text("💰", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Connect Gmail to Unlock Earnings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Once logged in, views on your published clips instantly generate revenue under MobileTube's $18.00 CPM partner program!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Button(
                        onClick = { viewModel.showSignIn() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("earnings_lock_signin_btn")
                    ) {
                        Text("Connect Gmail Account")
                    }
                }
            }
            return@Scaffold
        }

        val user = currentUser!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Balance card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%.2f", user.balance)}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4CAF50),
                            letterSpacing = (-1).sp
                        )

                        Text(
                            text = "Revenue rate: $18.00 CPM (per 1,000 views)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showCashOutDialog = true },
                            enabled = user.balance > 0.05,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(46.dp)
                                .testTag("cashout_trigger_btn")
                        ) {
                            Text("💳 Withdraw / Cash Out Earnings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Confetti banner
            if (cashOutSuccessShow) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF155724)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cashout_success_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "💸 Bank Cash-out Successful!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                "Your payout of $${String.format("%.2f", cashedOutAmount)} has been wired to your selected account. Confetti launched!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { cashOutSuccessShow = false }) {
                                Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 2. Statistics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBox(
                        title = "Estimated Views",
                        value = "${user.viewsCount}",
                        iconUnicode = "📈",
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )

                    MetricBox(
                        title = "Subscribers",
                        value = "${user.subscribersCount}",
                        iconUnicode = "👥",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Play Button milestones
            item {
                Column {
                    Text(
                        text = "Creator Milestones Progress",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    MilestoneRow(
                        title = "Silver Play Button Award",
                        subtitle = "Target: 100,000 Subscribers",
                        progress = (user.subscribersCount.toFloat() / 100_000f).coerceAtMost(1.0f),
                        isUnlocked = user.subscribersCount >= 100_000,
                        rewardIcon = "🔘"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MilestoneRow(
                        title = "Gold Play Button Award",
                        subtitle = "Target: 1,000,000 Subscribers",
                        progress = (user.subscribersCount.toFloat() / 1_000_000f).coerceAtMost(1.0f),
                        isUnlocked = user.subscribersCount >= 1_000_000,
                        rewardIcon = "🟡"
                    )
                }
            }

            // 4. Gemini Content Ideas Agent
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Text("🤖", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Gemini Revenue Growth Agent",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Get personalized, high-paying niche content recommendations based on your current channel size, tailored exclusively by the Gemini generative model.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isGeneratingIdeas) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Gemini scanning CPM indexes...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else if (generatedIdeasResult != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "High CPM Topic Blueprints:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                generatedIdeasResult!!.forEachIndexed { index, idea ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = idea,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { generatedIdeasResult = null },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Reset Ideas", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    isGeneratingIdeas = true
                                    scope.launch {
                                        try {
                                            val prompt = "Give me 3 short, specific, viral YouTube video concepts with high ad payout rates. Keep lines concise."
                                            val response = GeminiService.generateContent(prompt)
                                            val rawIdeas = response.lines()
                                                .map { it.replace(Regex("^[\\d*.\\s-]+"), "").trim() }
                                                .filter { it.isNotBlank() && it.length > 10 }
                                                .take(3)
                                            
                                            generatedIdeasResult = if (rawIdeas.size >= 2) rawIdeas else listOf(
                                                "Mastering Retro Wave Synthesizers: How to Create Epic Cyberpunk Soundtracks ($22 Estimated CPM)",
                                                "Jetpack Compose Best State Hoisting Blueprints to optimize app performance ($18 Estimated CPM)",
                                                "Quantum Entanglement Explained for Dummies using simple Vector Animations ($14 Estimated CPM)"
                                            )
                                        } catch (e: Exception) {
                                            ideasError = "Fallback used"
                                        } finally {
                                            isGeneratingIdeas = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_ideas_btn")
                            ) {
                                Text("Generate Viral Ideas via Gemini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. Historic Receipts
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recent Withdrawal Receipts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    completedReceipts.forEach { receipt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(receipt.method, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(receipt.date, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(receipt.amount, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                                Text(receipt.status, fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Cash out dialog
        if (showCashOutDialog) {
            val balanceAmount = user.balance
            AlertDialog(
                onDismissRequest = { if (!isProcessingCashOut) showCashOutDialog = false },
                title = { Text("Simulate Bank Cash Out", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "You are requesting a direct cash transfer of $${String.format("%.2f", balanceAmount)} to your chosen account.",
                            fontSize = 12.sp
                        )

                        Text("Select transfer ledger:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        val walletOptions = listOf("Simulated Bank Transfer", "PayPal Standard", "Payoneer Direct")
                        walletOptions.forEach { wallet ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { walletType = wallet }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = walletType == wallet, onClick = { walletType = wallet })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(wallet, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isProcessingCashOut = true
                            scope.launch {
                                delay(2000)
                                cashedOutAmount = balanceAmount
                                viewModel.requestCashOut(balanceAmount)
                                completedReceipts = listOf(
                                    TransactionReceipt("Today", "$${String.format("%.2f", balanceAmount)}", "Completed", walletType)
                                ) + completedReceipts
                                isProcessingCashOut = false
                                showCashOutDialog = false
                                cashOutSuccessShow = true
                            }
                        },
                        enabled = !isProcessingCashOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("cashout_confirm_btn")
                    ) {
                        if (isProcessingCashOut) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Confirm Withdrawal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!isProcessingCashOut) {
                        TextButton(onClick = { showCashOutDialog = false }) {
                            Text("Cancel", fontSize = 11.sp)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    iconUnicode: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = iconUnicode, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun MilestoneRow(
    title: String,
    subtitle: String,
    progress: Float,
    isUnlocked: Boolean,
    rewardIcon: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = rewardIcon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(6.dp))
            
            LinearProgressIndicator(
                progress = progress,
                color = if (isUnlocked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = "Status",
            tint = if (isUnlocked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

data class TransactionReceipt(
    val date: String,
    val amount: String,
    val status: String,
    val method: String
)
