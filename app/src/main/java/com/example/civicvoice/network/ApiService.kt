package com.example.civicvoice.network

import com.example.civicvoice.network.Comment
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<UserResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<UserResponse>

    @POST("auth/anonymous")
    fun anonymousLogin(): Call<UserResponse>

    @GET("auth/{userId}")
    fun getUser(@Path("userId") userId: String): Call<AppUser>

    @Multipart
    @PUT("auth/profile")
    fun updateProfile(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Call<UserResponse>

    @PUT("auth/fcm-token")
    fun updateFcmToken(@Body token: Map<String, String>): Call<Void>

    @GET("posts")
    fun getPosts(): Call<List<Post>>

    @GET("posts/{id}")
    fun getPostById(@Path("id") id: String): Call<Post>

    @POST("posts")
    fun createPost(@Body post: PostRequestBody): Call<Post>

    @PUT("posts/{id}/status")
    fun updatePostStatus(@Path("id") id: String, @Body status: Map<String, String>): Call<Post>

    @POST("posts/{id}/upvote")
    fun upvotePost(@Path("id") id: String): Call<Post>

    @POST("posts/{id}/downvote")
    fun downvotePost(@Path("id") id: String): Call<Post>

    @POST("posts/{id}/comment")
    fun addComment(@Path("id") id: String, @Body comment: CommentRequest): Call<Comment>

    @GET("posts/comments/{id}")
    fun getComment(@Path("id") id: String): Call<Comment>

    @GET("complaints")
    fun getComplaints(): Call<List<Complaint>>

    @DELETE("complaints/{id}")
    fun deleteComplaint(@Path("id") id: String): Call<Void>

    @GET("events")
    fun getEvents(): Call<List<Event>>

    @POST("events")
    fun createEvent(@Body event: Map<String, String>): Call<Event>

    @GET("notifications")
    fun getNotifications(): Call<List<Notification>>

    @POST("feedback")
    fun submitFeedback(@Body feedback: FeedbackRequest): Call<Feedback>

    @Multipart
    @POST("upload")
    fun uploadImage(@Part image: MultipartBody.Part): Call<Map<String, String>>

    @GET("tasks")
    fun getTasks(): Call<List<Task>>

    @DELETE("tasks/{id}")
    fun deleteTask(@Path("id") id: String): Call<Void>

    @POST("tasks")
    fun createTask(@Body task: Map<String, String>): Call<Task>

    @POST("notifications")
    fun createNotification(@Body notification: NotificationRequest): Call<Notification>

    @PUT("notifications/{id}/read")
    fun markNotificationAsRead(@Path("id") id: String): Call<Void>

    @DELETE("notifications/{id}")
    fun deleteNotification(@Path("id") id: String): Call<Void>
}
data class PostRequestBody(
    val title: String,
    val description: String,
    val location: String,
    val department: String,
    val hashtags: List<String>,
    val imageId: String?
)

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val phone: String,
    val name: String,
    val userType: String,
    val department: String?
)
