import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long = 0,
    val userId: Long,
    val username: String,
    val firstName: String,
    val lastName: String,
    val agreement: Boolean
)