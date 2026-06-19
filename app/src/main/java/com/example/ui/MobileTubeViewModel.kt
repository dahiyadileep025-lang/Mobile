package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MobileTubeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)

    // Current logged in user
    val currentUserState: StateFlow<CreatorProfile?> = repository.currentUser

    // All available videos feed
    val allVideosState: StateFlow<List<VideoItem>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All short videos (Shorts Feed)
    val shortsState: StateFlow<List<VideoItem>> = allVideosState
        .map { list -> list.filter { it.isShort } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Selection State
    private val _activeChatRecipientEmail = MutableStateFlow("global")
    val activeChatRecipientEmail: StateFlow<String> = _activeChatRecipientEmail.asStateFlow()

    // Current Active Chat History
    val chatMessagesState: StateFlow<List<ChatMessage>> = combine(
        activeChatRecipientEmail,
        currentUserState
    ) { recipient, user ->
        Pair(recipient, user)
    }.flatMapLatest { (recipient, user) ->
        if (recipient == "global") {
            repository.getGlobalChat()
        } else if (user != null) {
            repository.getDirectChat(user.email, recipient)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active creator user's own videos in Studio
    val creatorVideosState: StateFlow<List<VideoItem>> = currentUserState
        .flatMapLatest { profile ->
            if (profile != null) repository.getVideosForCreator(profile.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Subscription feed
    val subscriptionFeedState: StateFlow<List<VideoItem>> = currentUserState
        .flatMapLatest { profile ->
            if (profile != null) repository.getSubscriptionFeed(profile.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current active playing video ID
    private val _currentPlayingVideoId = MutableStateFlow<Int?>(null)
    val currentPlayingVideoId: StateFlow<Int?> = _currentPlayingVideoId.asStateFlow()

    // Current active playing video details
    val currentPlayingVideo: StateFlow<VideoItem?> = _currentPlayingVideoId
        .flatMapLatest { id ->
            if (id != null) repository.getVideoFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Comments for active video
    val currentVideoComments: StateFlow<List<Comment>> = _currentPlayingVideoId
        .flatMapLatest { id ->
            if (id != null) repository.getComments(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Subscription status for playing video creator
    val isSubscribedToActiveCreator: StateFlow<Boolean> = combine(
        currentUserState,
        currentPlayingVideo
    ) { user, video ->
        if (user != null && video != null) {
            user.email == video.creatorEmail // Can't subscribe to yourself, but we handle status
        }
        Pair(user?.email, video?.creatorEmail)
    }.flatMapLatest { (userEmail, creatorEmail) ->
        if (userEmail != null && creatorEmail != null && userEmail != creatorEmail) {
            repository.isSubscribed(userEmail, creatorEmail)
        } else {
            flowOf(false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Like status for active video
    val isLikedActiveVideo: StateFlow<Boolean> = combine(
        currentUserState,
        _currentPlayingVideoId
    ) { user, videoId ->
        Pair(user?.email, videoId)
    }.flatMapLatest { (email, videoId) ->
        if (email != null && videoId != null) {
            repository.isLiked(email, videoId)
        } else {
            flowOf(false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Creator Studio State (For video publication) ---
    private val _isGeneratingMetadata = MutableStateFlow(false)
    val isGeneratingMetadata: StateFlow<Boolean> = _isGeneratingMetadata.asStateFlow()

    private val _isAIPartsStatus = MutableStateFlow("")
    val isAIPartsStatus: StateFlow<String> = _isAIPartsStatus.asStateFlow()

    // --- Auth Dialog State ---
    private val _showGmailSignInDialog = MutableStateFlow(false)
    val showGmailSignInDialog: StateFlow<Boolean> = _showGmailSignInDialog.asStateFlow()

    // Video Player & Feed actions
    fun playVideo(video: VideoItem) {
        _currentPlayingVideoId.value = video.id
        viewModelScope.launch {
            repository.incrementViewAndRevenue(video.id, video.creatorEmail)
        }
    }

    fun stopVideo() {
        _currentPlayingVideoId.value = null
    }

    fun toggleLikeActiveVideo() {
        val user = currentUserState.value ?: return
        val videoId = _currentPlayingVideoId.value ?: return
        val currentlyLiked = isLikedActiveVideo.value

        viewModelScope.launch {
            if (currentlyLiked) {
                repository.unlikeVideo(user.email, videoId)
            } else {
                repository.likeVideo(user.email, videoId)
            }
        }
    }

    fun toggleSubscriptionActiveCreator() {
        val user = currentUserState.value ?: return
        val video = currentPlayingVideo.value ?: return
        if (user.email == video.creatorEmail) return // Can't self subscribe

        val currentlySubscribed = isSubscribedToActiveCreator.value

        viewModelScope.launch {
            if (currentlySubscribed) {
                repository.unsubscribe(user.email, video.creatorEmail)
            } else {
                repository.subscribe(user.email, video.creatorEmail)
            }
        }
    }

    fun postComment(content: String) {
        val user = currentUserState.value ?: return
        val videoId = _currentPlayingVideoId.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            repository.addComment(videoId, user.email, content)
        }
    }

    fun triggerAICommentReply(originalComment: Comment) {
        val videoId = _currentPlayingVideoId.value ?: return
        viewModelScope.launch {
            repository.generateAndAddAICommentReply(videoId, originalComment)
        }
    }

    // Google Sign-In Simulation Flows
    fun showSignIn() {
        _showGmailSignInDialog.value = true
    }

    fun hideSignIn() {
        _showGmailSignInDialog.value = false
    }

    fun handleGmailLogin(email: String, name: String) {
        viewModelScope.launch {
            repository.ensureUserCreated(email, name)
            repository.loginUser(email)
            _showGmailSignInDialog.value = false
        }
    }

    fun handleSignOut() {
        repository.logout()
    }

    // Creator Studio: Publish content
    fun publishCustomVideo(title: String, description: String, style: String, isShort: Boolean = false, onFinished: () -> Unit) {
        val user = currentUserState.value ?: return
        if (title.isBlank()) return

        viewModelScope.launch {
            val videoId = repository.publishVideo(
                title = title,
                description = description,
                style = style,
                creatorEmail = user.email,
                isShort = isShort
            )
            onFinished()
        }
    }

    // Chat room messaging actions
    fun selectChatRecipient(email: String) {
        _activeChatRecipientEmail.value = email
    }

    fun sendChatMessage(content: String) {
        val user = currentUserState.value ?: return
        val recipient = activeChatRecipientEmail.value
        if (content.isBlank()) return

        viewModelScope.launch {
            repository.sendChatMessage(user.email, recipient, content)
        }
    }

    // Creator Studio: AI Optimize Title & Description with Gemini Co-Pilot
    fun optimizeMetadataWithGemini(ideaOutline: String, category: String, onResults: (String, String) -> Unit) {
        if (ideaOutline.isBlank()) return
        
        viewModelScope.launch {
            _isGeneratingMetadata.value = true
            _isAIPartsStatus.value = "Analyzing video outline..."
            
            try {
                _isAIPartsStatus.value = "Structuring viral hooks with Gemini..."
                val (optimizedTitle, optimizedDesc) = GeminiService.getOptimizedMetadata(ideaOutline, category)
                
                _isAIPartsStatus.value = "Injecting revenue keywords..."
                onResults(optimizedTitle, optimizedDesc)
            } catch (e: Exception) {
                _isAIPartsStatus.value = "Fallback activated due to offline..."
                onResults(
                    "How I Mastered $ideaOutline in 30 Days",
                    "Welcome back to MobileTube! 🚀 In today's session, we map out: $ideaOutline. Subscribe for weekly updates!"
                )
            } finally {
                _isGeneratingMetadata.value = false
                _isAIPartsStatus.value = ""
            }
        }
    }

    // Revenue Module: Cashout Sim
    fun requestCashOut(amount: Double) {
        val user = currentUserState.value ?: return
        if (amount <= 0.0 || amount > user.balance) return

        viewModelScope.launch {
            // Deduct from balance
            repository.addEarnings(user.email, -amount)
            // Force reload current user
            repository.loginUser(user.email)
        }
    }
}
