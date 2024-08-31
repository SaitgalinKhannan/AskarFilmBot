data class Video(
    val id: Long = 0,
    val userId: Long,
    val path: String,
    val url: String?,
    val chatId: Long,
    val messageId: Long,
    val isSent: Boolean,
    val messageIds: String
)