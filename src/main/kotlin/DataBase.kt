import dev.inmo.kslog.common.e
import dev.inmo.kslog.common.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

val dispatcherIO = Dispatchers.IO

class DataBase {
    private var connection: Connection = DriverManager.getConnection("jdbc:sqlite:askar_film.db")

    init {
        try {
            connection.createStatement().use {
                it.executeUpdate(CREATE_USER_TABLE)
                it.executeUpdate(CREATE_VIDEO_TABLE)
            }
        } catch (e: SQLException) {
            logger.e(e.message.toString())
        }
    }

    suspend fun addUser(user: User): Boolean = withContext(dispatcherIO) {
        connection.prepareStatement(INSERT_USER).use { statement ->
            statement.setLong(1, user.userId)
            statement.setString(2, user.username)
            statement.setString(3, user.firstName)
            statement.setString(4, user.lastName)

            return@withContext statement.executeUpdate() > 0
        }
    }

    suspend fun getUser(id: Long): User? = withContext(dispatcherIO) {
        connection.prepareStatement(SELECT_USER).use { statement ->
            statement.setLong(1, id)
            val resultSet = statement.executeQuery()

            return@withContext if (resultSet.next()) {
                User(
                    id = resultSet.getLong(1),
                    userId = resultSet.getLong(2),
                    username = resultSet.getString(3),
                    firstName = resultSet.getString(4),
                    lastName = resultSet.getString(5),
                    agreement = resultSet.getBoolean(6)
                )
            } else {
                null
            }
        }
    }

    suspend fun updateUser(user: User): Boolean = withContext(dispatcherIO) {
        connection.prepareStatement(UPDATE_USER).use { statement ->
            statement.setString(1, user.username)
            statement.setString(2, user.firstName)
            statement.setString(3, user.lastName)
            statement.setBoolean(4, user.agreement)
            statement.setLong(5, user.userId)

            return@withContext statement.executeUpdate() > 0
        }
    }

    suspend fun getAllUsers(): List<User> = withContext(dispatcherIO) {
        val users = mutableListOf<User>()
        connection.prepareStatement(SELECT_ALL_USERS).use { statement ->
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val user = User(
                    id = resultSet.getLong(1),
                    userId = resultSet.getLong(2),
                    username = resultSet.getString(3),
                    firstName = resultSet.getString(4),
                    lastName = resultSet.getString(5),
                    agreement = resultSet.getBoolean(6)
                )
                users.add(user)
            }
        }

        return@withContext users
    }

    suspend fun addVideo(video: Video): Boolean = withContext(dispatcherIO) {
        connection.prepareStatement(INSERT_VIDEO).use { statement ->
            statement.setLong(1, video.userId)
            statement.setString(2, video.path)
            statement.setString(3, video.url)
            statement.setLong(4, video.chatId)
            statement.setLong(5, video.messageId)
            statement.setString(6, video.messageIds)

            return@withContext statement.executeUpdate() > 0
        }
    }

    suspend fun getVideoById(id: Long): Video? = withContext(dispatcherIO) {
        connection.prepareStatement(SELECT_VIDEO_BY_ID).use { statement ->
            statement.setLong(1, id)
            statement.executeQuery().use { resultSet ->
                return@withContext if (resultSet.next()) {
                    Video(
                        id = resultSet.getLong(1),
                        userId = resultSet.getLong(2),
                        path = resultSet.getString(3),
                        url = resultSet.getString(4),
                        chatId = resultSet.getLong(5),
                        messageId = resultSet.getLong(6),
                        isSent = resultSet.getBoolean(7),
                        messageIds = resultSet.getString(8)
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun getVideoByMessageId(id: Long): Video? = withContext(dispatcherIO) {
        connection.prepareStatement(SELECT_VIDEO_BY_MESSAGE_ID).use { statement ->
            statement.setLong(1, id)
            statement.executeQuery().use { resultSet ->
                return@withContext if (resultSet.next()) {
                    Video(
                        id = resultSet.getLong(1),
                        userId = resultSet.getLong(2),
                        path = resultSet.getString(3),
                        url = resultSet.getString(4),
                        chatId = resultSet.getLong(5),
                        messageId = resultSet.getLong(6),
                        isSent = resultSet.getBoolean(7),
                        messageIds = resultSet.getString(8)
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun getVideoByUserId(userId: Long): Video? = withContext(dispatcherIO) {
        connection.prepareStatement(SELECT_VIDEO_BY_USER_ID).use { statement ->
            statement.setLong(1, userId)
            statement.executeQuery().use { resultSet ->
                return@withContext if (resultSet.next()) {
                    Video(
                        id = resultSet.getLong(1),
                        userId = resultSet.getLong(2),
                        path = resultSet.getString(3),
                        url = resultSet.getString(4),
                        chatId = resultSet.getLong(5),
                        messageId = resultSet.getLong(6),
                        isSent = resultSet.getBoolean(7),
                        messageIds = resultSet.getString(8)
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun getVideosByUserIdAndIsSent(userId: Long): List<Video> = withContext(dispatcherIO) {
        connection.prepareStatement(SELECT_VIDEO_BY_USER_ID_AND_IS_SENT).use { statement ->
            val videos = mutableListOf<Video>()
            statement.setLong(1, userId)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    videos.add(
                        Video(
                            id = resultSet.getLong(1),
                            userId = resultSet.getLong(2),
                            path = resultSet.getString(3),
                            url = resultSet.getString(4),
                            chatId = resultSet.getLong(5),
                            messageId = resultSet.getLong(6),
                            isSent = resultSet.getBoolean(7),
                            messageIds = resultSet.getString(8)
                        )
                    )
                }

                return@withContext videos
            }
        }
    }

    suspend fun updateVideo(video: Video): Boolean = withContext(dispatcherIO) {
        connection.prepareStatement(UPDATE_VIDEO).use { statement ->
            statement.setLong(1, video.userId)
            statement.setString(2, video.path)
            statement.setString(3, video.url)
            statement.setLong(4, video.chatId)
            statement.setLong(5, video.messageId)
            statement.setBoolean(6, video.isSent)
            statement.setString(7, video.messageIds)
            statement.setLong(8, video.id)

            return@withContext statement.executeUpdate() > 0
        }
    }

    suspend fun deleteVideo(videoId: Long): Boolean = withContext(dispatcherIO) {
        connection.prepareStatement(DELETE_VIDEO).use { statement ->
            statement.setLong(1, videoId)

            return@withContext statement.executeUpdate() > 0
        }
    }

    companion object {
        const val CREATE_USER_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER UNIQUE,
                username TEXT,
                first_name TEXT,
                last_name TEXT,
                agreement BOOLEAN DEFAULT FALSE
            )
        """
        const val INSERT_USER = """
            INSERT OR IGNORE INTO users (user_id, username, first_name, last_name) VALUES (?, ?, ?, ?)
        """
        const val SELECT_USER = "SELECT * FROM users WHERE user_id = ?"
        const val SELECT_ALL_USERS = "SELECT * FROM users"
        const val UPDATE_USER = """
            UPDATE users 
            SET username = ?, first_name = ?, last_name = ?, agreement = ?
            WHERE user_id = ?
        """

        const val CREATE_VIDEO_TABLE = """
            CREATE TABLE IF NOT EXISTS videos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER REFERENCES users(user_id),
                paht TEXT,
                url TEXT,
                chatId INTEGER,
                messageId INTEGER,
                isSent BOOLEAN DEFAULT FALSE,
                messageIds TEXT DEFAULT ''
            )
        """
        const val INSERT_VIDEO = """
            INSERT INTO videos (user_id, paht, url, chatId, messageId, messageIds)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        const val SELECT_VIDEO_BY_ID = """
            SELECT * 
            FROM videos 
            WHERE id = ?
        """
        const val SELECT_VIDEO_BY_MESSAGE_ID = """
            SELECT * 
            FROM videos 
            WHERE messageId = ?
        """
        const val SELECT_VIDEO_BY_USER_ID = """
            SELECT * 
            FROM videos 
            WHERE user_id = ?
        """
        const val UPDATE_VIDEO = """
            UPDATE videos 
            SET user_id = ?, paht = ?, url = ?, chatId = ?, messageId = ?, isSent = ?, messageIds = ?
            WHERE id = ?
        """
        const val DELETE_VIDEO = "DELETE FROM videos WHERE id = ?"
        const val SELECT_VIDEO_BY_USER_ID_AND_IS_SENT = """
            SELECT * 
            FROM videos 
            WHERE isSent = TRUE AND user_id = ?
        """
    }
}