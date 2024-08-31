import dev.inmo.kslog.common.i
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.extension

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(json)
    }
}
const val token: String = "y0_AgAAAAASYBT0AADLWwAAAAEPXYQAAADM07Tl7iBF971z2PetiGJtrFlmkw"
const val tokenDev: String = "y0_AgAAAAAd96Z7AADLWwAAAAEPNjdEAAAORiQSCZVN0ZeVugpltpc_rKghzA"

//"y0_AgAAAAAd96Z7AADLWwAAAAEPNjdEAAAORiQSCZVN0ZeVugpltpc_rKghzA" // my
val yFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy HH-mm-ss")

suspend fun uploadVideo(video: Video, user: User, mode: String): String {
    val extension = Path(video.path).fileName.extension
    val currentDateTime = LocalDateTime.now()
    val formattedDateTime = currentDateTime.format(yFormatter)
    val url = "${user.lastName}_${user.firstName}_${user.username}_${user.userId}_${formattedDateTime}.${extension}"
    val diskPath = "/VideoLGL/${url}"
    uploadFileToYaDisk(diskPath, video.path, mode)
    return url
}

suspend fun getUploadUrl(token: String, path: String): String {
    logger.i(path)
    val response: HttpResponse = client.get("https://cloud-api.yandex.net/v1/disk/resources/upload") {
        header(HttpHeaders.Authorization, "OAuth $token")
        parameter("path", path)
        parameter("overwrite", true)
    }

    logger.i(response)

    val bodyAsText = response.bodyAsText()
    val responseBody = if (response.status == HttpStatusCode.OK) {
        json.decodeFromString<UploadUrl>(bodyAsText)
    } else {
        logger.i(bodyAsText)
        throw Exception("Failed to get upload URL")
    }

    return responseBody.href
}

suspend fun uploadFile(uploadUrl: String, filePath: String) = withContext(Dispatchers.IO) {
    val inputString = FileInputStream(filePath)

    inputString.use {
        val response: HttpResponse = client.put(uploadUrl) {
            setBody(it.readBytes())
            header(HttpHeaders.ContentType, ContentType.Application.OctetStream)
        }

        if (response.status != HttpStatusCode.Created) {
            throw Exception("File upload failed with status: ${response.status}")
        }
    }
}

suspend fun uploadFileToYaDisk(diskPath: String, localFilePath: String, mode: String) {
    try {
        // Получение URL для загрузки
        val uploadUrl = getUploadUrl(if (mode == "dev") tokenDev else token, diskPath)
        println("Upload URL: $uploadUrl")

        // Загрузка файла по полученному URL
        uploadFile(uploadUrl, localFilePath)
        println("File uploaded successfully")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Serializable
data class UploadUrl(
    @SerialName("operation_id")
    val operationId: String,
    val href: String,
    val method: String,
    val templated: Boolean
)