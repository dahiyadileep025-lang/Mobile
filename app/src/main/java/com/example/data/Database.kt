package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "creator_profiles")
data class CreatorProfile(
    @PrimaryKey val email: String,
    val name: String,
    val avatarUrl: String,
    val subscribersCount: Int = 0,
    val balance: Double = 0.0,
    val viewsCount: Int = 0,
    val isVerified: Boolean = false
)

@Entity(tableName = "video_items")
data class VideoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val creatorEmail: String,
    val creatorName: String,
    val creatorAvatar: String,
    val videoStyle: String, // e.g., "Tech Wave", "Chill Wave", "Neon Pulse", "Retro Grid", "Cyberpunk Cosmic"
    val views: Int = 0,
    val likes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSec: Int = 180,
    val revenueGenerated: Double = 0.0,
    val isShort: Boolean = false
)

@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["subscriberEmail", "subscribedToEmail"], unique = true)]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriberEmail: String,
    val subscribedToEmail: String
)

@Entity(
    tableName = "video_likes",
    indices = [Index(value = ["userEmail", "videoId"], unique = true)]
)
data class VideoLike(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val videoId: Int
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: Int,
    val userEmail: String,
    val userName: String,
    val userAvatar: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderEmail: String,
    val senderName: String,
    val senderAvatar: String,
    val recipientEmail: String, // "global" or a creator's email address
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TubeDao {
    // Creator Profiles
    @Query("SELECT * FROM creator_profiles WHERE email = :email")
    fun getCreatorProfileFlow(email: String): Flow<CreatorProfile?>

    @Query("SELECT * FROM creator_profiles WHERE email = :email")
    suspend fun getCreatorProfile(email: String): CreatorProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreatorProfile(profile: CreatorProfile)

    @Query("UPDATE creator_profiles SET balance = balance + :amount WHERE email = :email")
    suspend fun addEarnings(email: String, amount: Double)

    @Query("UPDATE creator_profiles SET subscribersCount = subscribersCount + :delta WHERE email = :email")
    suspend fun updateSubscribersCount(email: String, delta: Int)

    @Query("UPDATE creator_profiles SET viewsCount = viewsCount + :delta WHERE email = :email")
    suspend fun updateViewsCount(email: String, delta: Int)

    // Videos
    @Query("SELECT * FROM video_items ORDER BY id DESC")
    fun getAllVideosFlow(): Flow<List<VideoItem>>

    @Query("SELECT * FROM video_items WHERE creatorEmail = :email ORDER BY id DESC")
    fun getVideosByCreatorFlow(email: String): Flow<List<VideoItem>>

    @Query("SELECT * FROM video_items WHERE id = :id")
    suspend fun getVideoById(id: Int): VideoItem?

    @Query("SELECT * FROM video_items WHERE id = :id")
    fun getVideoByIdFlow(id: Int): Flow<VideoItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoItem): Long

    @Query("UPDATE video_items SET views = views + 1, revenueGenerated = revenueGenerated + :revenueDelta WHERE id = :id")
    suspend fun incrementViewsAndRevenue(id: Int, revenueDelta: Double)

    @Query("UPDATE video_items SET likes = likes + :delta WHERE id = :id")
    suspend fun updateLikesCount(id: Int, delta: Int)

    // Subscriptions
    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE subscriberEmail = :subscriberEmail AND subscribedToEmail = :subscribedToEmail)")
    fun isSubscribedFlow(subscriberEmail: String, subscribedToEmail: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubscription(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE subscriberEmail = :subscriberEmail AND subscribedToEmail = :subscribedToEmail")
    suspend fun removeSubscription(subscriberEmail: String, subscribedToEmail: String)

    @Query("SELECT * FROM video_items WHERE creatorEmail IN (SELECT subscribedToEmail FROM subscriptions WHERE subscriberEmail = :email) ORDER BY id DESC")
    fun getSubscriptionFeedFlow(email: String): Flow<List<VideoItem>>

    // Likes
    @Query("SELECT EXISTS(SELECT 1 FROM video_likes WHERE userEmail = :userEmail AND videoId = :videoId)")
    fun isLikedFlow(userEmail: String, videoId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: VideoLike)

    @Query("DELETE FROM video_likes WHERE userEmail = :userEmail AND videoId = :videoId")
    suspend fun removeLike(userEmail: String, videoId: Int)

    // Comments
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY id DESC")
    fun getCommentsFlow(videoId: Int): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    // Chat messages
    @Query("SELECT * FROM chat_messages WHERE recipientEmail = :recipientEmail ORDER BY timestamp ASC")
    fun getGlobalChatFlow(recipientEmail: String = "global"): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE (senderEmail = :user1 AND recipientEmail = :user2) OR (senderEmail = :user2 AND recipientEmail = :user1) ORDER BY timestamp ASC")
    fun getDirectChatFlow(user1: String, user2: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)
}

@Database(
    entities = [
        CreatorProfile::class,
        VideoItem::class,
        Subscription::class,
        VideoLike::class,
        Comment::class,
        ChatMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TubeDatabase : RoomDatabase() {
    abstract fun dao(): TubeDao

    companion object {
        @Volatile
        private var INSTANCE: TubeDatabase? = null

        fun getDatabase(context: Context): TubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TubeDatabase::class.java,
                    "mobiletube_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
