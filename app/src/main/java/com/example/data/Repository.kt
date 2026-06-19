package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Repository(context: Context) {
    private val database = TubeDatabase.getDatabase(context)
    private val dao = database.dao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current active authenticated user
    private val _currentUser = MutableStateFlow<CreatorProfile?>(null)
    val currentUser: StateFlow<CreatorProfile?> = _currentUser.asStateFlow()

    init {
        // Seed initial data if database is empty
        scope.launch {
            seedInitialDataIfNeeded()
            // Auto login with a mock Gmail account for testing, or wait for sign-in
            // For outstanding UX, we can pre-auth with the tester's email profile
            val initialEmail = "dahiyadileep025@gmail.com"
            ensureUserCreated(initialEmail, "Dileep Dahiya")
            loginUser(initialEmail)
        }
    }

    // Auth Flows
    suspend fun loginUser(email: String) {
        val profile = dao.getCreatorProfile(email)
        if (profile != null) {
            _currentUser.value = profile
        } else {
            // Auto create profile for new Gmail
            val cleanName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val newProfile = CreatorProfile(
                email = email,
                name = cleanName,
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=${email.hashCode()}",
                subscribersCount = 280, // Start with a few fans to look fun!
                viewsCount = 1450,
                balance = 12.50, // Initial seed earnings
                isVerified = true // Verified Gmail member
            )
            dao.insertCreatorProfile(newProfile)
            _currentUser.value = newProfile
        }
    }

    suspend fun ensureUserCreated(email: String, name: String) {
        val existing = dao.getCreatorProfile(email)
        if (existing == null) {
            val newProfile = CreatorProfile(
                email = email,
                name = name,
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=${email.hashCode()}",
                subscribersCount = 140,
                viewsCount = 920,
                balance = 5.00,
                isVerified = true
            )
            dao.insertCreatorProfile(newProfile)
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    suspend fun addEarnings(email: String, amount: Double) = withContext(Dispatchers.IO) {
        dao.addEarnings(email, amount)
    }

    // Subscriptions
    fun isSubscribed(subscriberEmail: String, creatorEmail: String): Flow<Boolean> {
        return dao.isSubscribedFlow(subscriberEmail, creatorEmail)
    }
    
    suspend fun subscribe(subscriberEmail: String, creatorEmail: String) {
        withContext(Dispatchers.IO) {
            dao.insertSubscription(Subscription(subscriberEmail = subscriberEmail, subscribedToEmail = creatorEmail))
            dao.updateSubscribersCount(creatorEmail, 1)
        }
    }

    suspend fun unsubscribe(subscriberEmail: String, creatorEmail: String) {
        withContext(Dispatchers.IO) {
            dao.removeSubscription(subscriberEmail, creatorEmail)
            dao.updateSubscribersCount(creatorEmail, -1)
        }
    }

    // Likes
    fun isLiked(userEmail: String, videoId: Int): Flow<Boolean> = dao.isLikedFlow(userEmail, videoId)

    suspend fun likeVideo(userEmail: String, videoId: Int) {
        withContext(Dispatchers.IO) {
            dao.insertLike(VideoLike(userEmail = userEmail, videoId = videoId))
            dao.updateLikesCount(videoId, 1)
        }
    }

    suspend fun unlikeVideo(userEmail: String, videoId: Int) {
        withContext(Dispatchers.IO) {
            dao.removeLike(userEmail, videoId)
            dao.updateLikesCount(videoId, -1)
        }
    }

    // Videos
    val allVideos: Flow<List<VideoItem>> = dao.getAllVideosFlow()
    
    fun getVideosForCreator(email: String): Flow<List<VideoItem>> = dao.getVideosByCreatorFlow(email)
    
    fun getSubscriptionFeed(email: String): Flow<List<VideoItem>> = dao.getSubscriptionFeedFlow(email)

    fun getVideoFlow(id: Int): Flow<VideoItem?> = dao.getVideoByIdFlow(id)

    suspend fun incrementViewAndRevenue(videoId: Int, creatorEmail: String) = withContext(Dispatchers.IO) {
        // Increment video view count, add random monetized value ($0.05 to $0.25 per view!)
        val rpmRate = 18.00 // $18.00 RPM (Revenue Per Mille) -> $0.018 per view
        val earningsDelta = 0.018
        
        dao.incrementViewsAndRevenue(videoId, earningsDelta)
        // Also update creator's channel metrics
        dao.updateViewsCount(creatorEmail, 1)
        dao.addEarnings(creatorEmail, earningsDelta)
        
        // If active user viewed, make sure we reflect locally if they are the creator
        val current = _currentUser.value
        if (current != null && current.email == creatorEmail) {
            _currentUser.value = dao.getCreatorProfile(creatorEmail)
        }
    }

    suspend fun publishVideo(title: String, description: String, style: String, creatorEmail: String, isShort: Boolean = false): Long {
        return withContext(Dispatchers.IO) {
            val creator = dao.getCreatorProfile(creatorEmail) ?: CreatorProfile(
                email = creatorEmail,
                name = creatorEmail.substringBefore("@"),
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=${creatorEmail.hashCode()}"
            )
            
            val newVideo = VideoItem(
                title = title,
                description = description,
                creatorEmail = creatorEmail,
                creatorName = creator.name,
                creatorAvatar = creator.avatarUrl,
                videoStyle = style,
                views = 0,
                likes = 0,
                revenueGenerated = 0.0,
                timestamp = System.currentTimeMillis(),
                isShort = isShort
            )
            val videoId = dao.insertVideo(newVideo)
            videoId
        }
    }

    // Comments
    fun getComments(videoId: Int): Flow<List<Comment>> = dao.getCommentsFlow(videoId)

    suspend fun addComment(videoId: Int, userEmail: String, content: String) = withContext(Dispatchers.IO) {
        val userProfile = dao.getCreatorProfile(userEmail)
        val name = userProfile?.name ?: userEmail.substringBefore("@")
        val avatar = userProfile?.avatarUrl ?: "https://api.dicebear.com/7.x/pixel-art/png?seed=${userEmail.hashCode()}"

        dao.insertComment(
            Comment(
                videoId = videoId,
                userEmail = userEmail,
                userName = name,
                userAvatar = avatar,
                content = content
            )
        )
    }

    suspend fun generateAndAddAICommentReply(videoId: Int, originalComment: Comment) = withContext(Dispatchers.IO) {
        val replyText = GeminiService.generateAutoReply(originalComment.content, originalComment.userName)
        val video = dao.getVideoById(videoId) ?: return@withContext
        
        // Add dynamic comment reply on behalf of the creator
        dao.insertComment(
            Comment(
                videoId = videoId,
                userEmail = video.creatorEmail,
                userName = "${video.creatorName} [Creator]",
                userAvatar = video.creatorAvatar,
                content = replyText
            )
        )
    }

    // Chat room flows & messaging
    fun getGlobalChat(): Flow<List<ChatMessage>> = dao.getGlobalChatFlow()

    fun getDirectChat(user1: String, user2: String): Flow<List<ChatMessage>> = dao.getDirectChatFlow(user1, user2)

    suspend fun sendChatMessage(senderEmail: String, recipientEmail: String, content: String) = withContext(Dispatchers.IO) {
        val userProfile = dao.getCreatorProfile(senderEmail)
        val name = userProfile?.name ?: senderEmail.substringBefore("@")
        val avatar = userProfile?.avatarUrl ?: "https://api.dicebear.com/7.x/pixel-art/png?seed=${senderEmail.hashCode()}"

        dao.insertChatMessage(
            ChatMessage(
                senderEmail = senderEmail,
                senderName = name,
                senderAvatar = avatar,
                recipientEmail = recipientEmail,
                content = content
            )
        )

        // If message is sent to a creator, trigger a dynamic simulated reply from that creator!
        if (recipientEmail != "global" && recipientEmail != senderEmail) {
            val creator = dao.getCreatorProfile(recipientEmail)
            if (creator != null) {
                // Generate automated reply
                val reply = GeminiService.generateDirectChatReply(content, name, creator.name)
                // Insert after short delay or instantly
                dao.insertChatMessage(
                    ChatMessage(
                        senderEmail = creator.email,
                        senderName = creator.name,
                        senderAvatar = creator.avatarUrl,
                        recipientEmail = senderEmail,
                        content = reply
                    )
                )
            }
        } else if (recipientEmail == "global") {
            // Simulated global chatter replies sometimes!
            val otherUsers = listOf(
                Pair("Lofi Odyssey Music", "lofi.beats@gmail.com"),
                Pair("ComposeGuy", "viewer3@gmail.com"),
                Pair("Alex Dev", "viewer1@gmail.com"),
                Pair("Quantum Guru Studio", "physic_guru@gmail.com")
            ).filter { it.second != senderEmail }

            if (otherUsers.isNotEmpty()) {
                val chosen = otherUsers.random()
                val profile = dao.getCreatorProfile(chosen.second)
                val avatarUrl = profile?.avatarUrl ?: "https://api.dicebear.com/7.x/pixel-art/png?seed=${chosen.second.hashCode()}"
                
                val chatPrompt = "A user named $name said \"$content\" in a global YouTube style lounge dev chatroom. Reply as ${chosen.first} in a super-short, friendly single-line chat bubble style."
                val replyText = try {
                    GeminiService.generateContent(chatPrompt)
                } catch (e: Exception) {
                    ""
                }
                val finalReply = if (replyText.isBlank() || replyText.startsWith("Error:") || replyText.contains("Connection failed:")) {
                    listOf(
                        "Welcome to the lounge, $name! ☕ Let's code some Jetpack Compose!",
                        "Honestly, $name, same here. Lofi Odyssey is keeping me alive.",
                        "Woah, that is a hot take! But totally agreed. 👀",
                        "Wait, are we talkin' about the new Room Database models? They work so smoothly!",
                        "Hey $name, nice meeting you here! Keep building!"
                    ).random()
                } else {
                    replyText.trim().replace("\"", "")
                }
                
                dao.insertChatMessage(
                    ChatMessage(
                        senderEmail = chosen.second,
                        senderName = chosen.first,
                        senderAvatar = avatarUrl,
                        recipientEmail = "global",
                        content = finalReply
                    )
                )
            }
        }
    }

    // Seeding logic
    private suspend fun seedInitialDataIfNeeded() {
        // Since we are running in dispatcher IO inside init, we can do direct synchronous check safely
        val sampleCreator = dao.getCreatorProfile("lofi.beats@gmail.com")
        if (sampleCreator == null) {
            Log.d("Repository", "Seeding initial high quality mock database for MobileTube...")
            // Seed content creators
            val c1 = CreatorProfile(
                email = "lofi.beats@gmail.com",
                name = "Lofi Odyssey Music",
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=lofi",
                subscribersCount = 42900,
                viewsCount = 1290350,
                balance = 4590.25,
                isVerified = true
            )
            val c2 = CreatorProfile(
                email = "tech_insider@gmail.com",
                name = "Tech Insider Lab",
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=tech",
                subscribersCount = 89000,
                viewsCount = 3450912,
                balance = 12940.80,
                isVerified = true
            )
            val c3 = CreatorProfile(
                email = "dreamweaver@gmail.com",
                name = "Retro Dreamweaver",
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=synth",
                subscribersCount = 12500,
                viewsCount = 205000,
                balance = 520.40
            )
            val c4 = CreatorProfile(
                email = "physic_guru@gmail.com",
                name = "Quantum Guru Studio",
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/png?seed=physics",
                subscribersCount = 6800,
                viewsCount = 54000,
                balance = 190.10
            )

            dao.insertCreatorProfile(c1)
            dao.insertCreatorProfile(c2)
            dao.insertCreatorProfile(c3)
            dao.insertCreatorProfile(c4)

            // Seed videos
            val v1 = VideoItem(
                title = "Study Beats Lofi Odyssey 🪐 Atmospheric Cosmic Chill Out [3 Hours]",
                description = "Relax your mind and stay highly focused with our latest space-themed ambient lofi selections. Over 3 hours of carefully curated beats to study, sketch, or code your software projects.\n\n🎵 Tracklist:\n01:00 Cosmic Voyager\n04:15 Interstellar Coffee\n07:50 Nebula Dreams\n\nEnjoy the loop!",
                creatorEmail = "lofi.beats@gmail.com",
                creatorName = c1.name,
                creatorAvatar = c1.avatarUrl,
                videoStyle = "Chill Wave",
                views = 124500,
                likes = 8900,
                durationSec = 10800,
                revenueGenerated = 2241.00
            )

            val v2 = VideoItem(
                title = "The Developer Ecosystem of 2026: Android Jetpack Compose State Deep Dive",
                description = "Jetpack Compose has evolved dramatically! Today, we check out top architectural designs, unidirectional data flows, thread-safe Room integration, and state hoisting configurations that prevent heavy resource waste.\n\nSubscribe for weekly software design tips!",
                creatorEmail = "tech_insider@gmail.com",
                creatorName = c2.name,
                creatorAvatar = c2.avatarUrl,
                videoStyle = "Tech Wave",
                views = 42500,
                likes = 3400,
                durationSec = 1220,
                revenueGenerated = 765.00
            )

            val v3 = VideoItem(
                title = "Retrowave Vector Neon Grid Run 💜 Brutalist Retro 80s Visual Loop",
                description = "Put on your headphones, set screen brightness to high, and travel back in time with this infinite synthetics neon grid visualizer loop. Perfect for chill night drives or cyberpunk enthusiasts.",
                creatorEmail = "dreamweaver@gmail.com",
                creatorName = c3.name,
                creatorAvatar = c3.avatarUrl,
                videoStyle = "Retro Grid",
                views = 18900,
                likes = 1200,
                durationSec = 600,
                revenueGenerated = 340.20
            )

            val v4 = VideoItem(
                title = "Does Quantum Superposition Actually Happen inside Your Brain? 🧠 Physics Theory",
                description = "In today's lecture, we evaluate quantum coherence, neural microtubules, superpositions, and the core arguments on whether biology interacts at sub-atomic scales. Subscribe for simple physics modules!",
                creatorEmail = "physic_guru@gmail.com",
                creatorName = c4.name,
                creatorAvatar = c4.avatarUrl,
                videoStyle = "Neon Pulse",
                views = 5200,
                likes = 450,
                durationSec = 960,
                revenueGenerated = 93.60
            )

            dao.insertVideo(v1)
            dao.insertVideo(v2)
            dao.insertVideo(v3)
            dao.insertVideo(v4)

            // Seed Shorts
            val s1 = VideoItem(
                title = "Insane 60s Jetpack Compose Trick You Didn't Know! 🤯 #shorts #android",
                description = "Learn how to use Modifier.graphicsLayer to build incredible 3D flip card animations in less than 10 lines of Kotlin. Mind-blowing simplicity!",
                creatorEmail = "tech_insider@gmail.com",
                creatorName = c2.name,
                creatorAvatar = c2.avatarUrl,
                videoStyle = "Tech Wave",
                views = 98000,
                likes = 6400,
                durationSec = 45,
                revenueGenerated = 15.40,
                isShort = true
            )

            val s2 = VideoItem(
                title = "Cosmic Chill Beat for Coders 🛸 #shorts #lofi",
                description = "Take a breath, relax your shoulders, and watch the waves roll in with this atmospheric ambient lofi track.",
                creatorEmail = "lofi.beats@gmail.com",
                creatorName = c1.name,
                creatorAvatar = c1.avatarUrl,
                videoStyle = "Chill Wave",
                views = 153200,
                likes = 12900,
                durationSec = 30,
                revenueGenerated = 28.50,
                isShort = true
            )

            val s3 = VideoItem(
                title = "What is Quantum Tunneling anyway? 🧊 #shorts #physics #explain",
                description = "Particles literally walking through solid walls. Yes, it happens, and yes, it's why your USB drives can save files! Quantum mechanics is wild.",
                creatorEmail = "physic_guru@gmail.com",
                creatorName = c4.name,
                creatorAvatar = c4.avatarUrl,
                videoStyle = "Neon Pulse",
                views = 35000,
                likes = 2900,
                durationSec = 58,
                revenueGenerated = 4.80,
                isShort = true
            )

            dao.insertVideo(s1)
            dao.insertVideo(s2)
            dao.insertVideo(s3)

            // Seed some comments
            dao.insertComment(Comment(videoId = 1, userEmail = "viewer1@gmail.com", userName = "Alex Dev", userAvatar = "https://api.dicebear.com/7.x/pixel-art/png?seed=alex", content = "This is saving my computer science final exam coding sprint right now! 10/10 playlist."))
            dao.insertComment(Comment(videoId = 1, userEmail = "viewer2@gmail.com", userName = "CyberSam", userAvatar = "https://api.dicebear.com/7.x/pixel-art/png?seed=sam", content = "The animation paired with the soundwaves feels incredibly peaceful. Beautiful!"))
            dao.insertComment(Comment(videoId = 2, userEmail = "lofi.beats@gmail.com", userName = "Lofi Odyssey Music", userAvatar = "https://api.dicebear.com/7.x/pixel-art/png?seed=lofi", content = "Great architectural breakdown! Love the clean composition."))
            dao.insertComment(Comment(videoId = 2, userEmail = "viewer3@gmail.com", userName = "ComposeGuy", userAvatar = "https://api.dicebear.com/7.x/pixel-art/png?seed=compose", content = "Does using collectAsStateWithLifecycle prevent leaking resources when navigating? Yes, absolutely! Thanks for explaining!"))

            // Seed initial lounge chats
            dao.insertChatMessage(ChatMessage(senderEmail = "lofi.beats@gmail.com", senderName = "Lofi Odyssey Music", senderAvatar = c1.avatarUrl, recipientEmail = "global", content = "Hey everyone! Welcome to the MobileTube developer lounge! 🎧 Grab a cup of coffee and let's build something beautiful!"))
            dao.insertChatMessage(ChatMessage(senderEmail = "viewer1@gmail.com", senderName = "Alex Dev", senderAvatar = "https://api.dicebear.com/7.x/pixel-art/png?seed=alex", recipientEmail = "global", content = "Wow, is this chat actually real-time? Awesome! Love the lofi streams."))
            dao.insertChatMessage(ChatMessage(senderEmail = "tech_insider@gmail.com", senderName = "Tech Insider Lab", senderAvatar = c2.avatarUrl, recipientEmail = "global", content = "Indeed, built with fully responsive Jetpack Compose and local Room caching! Any coding or stream questions today? 💻"))
        }
    }
}
