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
            }
        } catch (e: SQLException) {
            logger.e(e.message.toString())
        }
    }

    suspend fun addUser(user: User): Boolean = withContext(dispatcherIO) {
        connection.prepareStatement(INSERT_USER).use { statement ->
            statement.setLong(1, user.id)
            statement.setString(2, user.username)
            statement.setString(3, user.firstName)
            statement.setString(4, user.lastName)

            return@withContext statement.executeUpdate() > 0
        }
    }

    suspend fun getUser(id: Long): User? = withContext(dispatcherIO) {
        connection.prepareStatement(SELECT_USERS).use { statement ->
            statement.setLong(1, id)
            val resultSet = statement.executeQuery()

            return@withContext if (resultSet.next()) {
                User(
                    id = resultSet.getLong(1),
                    username = resultSet.getString(2),
                    firstName = resultSet.getString(3),
                    lastName = resultSet.getString(4),
                    agreement = resultSet.getBoolean(5)
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
            statement.setLong(5, user.id)

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
                    username = resultSet.getString(2),
                    firstName = resultSet.getString(3),
                    lastName = resultSet.getString(4),
                    agreement = resultSet.getBoolean(5)
                )
                users.add(user)
            }
        }

        return@withContext users
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
        const val INSERT_USER =
            "INSERT OR IGNORE INTO users (user_id, username, first_name, last_name) VALUES (?, ?, ?, ?)"
        const val SELECT_USERS = "SELECT * FROM users WHERE id = ?"
        const val SELECT_ALL_USERS = "SELECT * FROM users"
        const val UPDATE_USER = """
            UPDATE users 
            SET username = ?, first_name = ?, last_name = ?, agreement = ?
            WHERE user_id = ?
        """
    }
}