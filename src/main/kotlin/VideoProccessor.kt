import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random

enum class Poster(val path: String) {
    BLUE("poster_blue.png"),
    BLUE_BOTTOM_4_3("poster_blue_bottom_4:3.png"),
    BLUE_TOP_4_3("poster_blue_top_4:3.png"),
    BLUE_BOTTOM_16_9("poster_blue_bottom_16:9.png"),
    BLUE_TOP_16_9("poster_blue_top_16:9.png"),
    PINK("poster_pink.png"),
    PINK_BOTTOM_4_3("poster_pink_bottom_4:3.png"),
    PINK_TOP_4_3("poster_pink_top_4:3.png"),
    PINK_BOTTOM_16_9("poster_pink_bottom_16:9.png"),
    PINK_TOP_16_9("poster_pink_top_16:9.png"),
}

const val inputAudioDev = "$basePath/resources/Those_around_new.mp3"
const val inputAudio = "$basePath/resources/Those_around_new.mp3"
val random = Random(System.currentTimeMillis())

// Asynchronous function to process the video
suspend fun videoToVideoWithOverlay(
    inputVideo: File,
    height: Int,
    width: Int,
    overlayImage: String,
    mode: String
): File = withContext(Dispatchers.IO) {
    val fileName = inputVideo.nameWithoutExtension
    val fileExtension = inputVideo.extension
    val outputFolder = File(
        if (mode == "dev")
            "$basePath/output"
        else
            "$basePath/output"
    ) //
    val outputVideo = File("${outputFolder.absolutePath}/${fileName}_new.$fileExtension")

    if (!outputFolder.exists()) {
        outputFolder.mkdirs()
    }

    val inputAudio = if (mode == "dev")
        inputAudioDev
    else
        inputAudio

    // Construct the FFmpeg command
    val command = listOf(
        "ffmpeg",
        "-i", inputVideo.absolutePath,  // Входное видео
        "-i", overlayImage,             // Оверлей
        "-i", inputAudio,               // Аудио
        "-filter_complex",
        "[0:v]scale=$width:$height:force_original_aspect_ratio=decrease,pad=$width:$height:(ow-iw)/2:(oh-ih)/2[video]" + // Масштабируем основное видео
                "[1:v]scale=$width:$height[overlay];" + // Масштабируем оверлей
                "[video][overlay]overlay=0:0:shortest=1,noise=c0s=40:c0f=t+u[v];" + // Накладываем оверлей
                "[2:a]atrim=0:15,asetpts=PTS-STARTPTS[a]", // Обрезаем аудио до 15 сек
        "-map", "[v]",                  // Берем обработанное видео
        "-map", "[a]",                  // Берем обрезанное аудио
        "-c:v", "libx264",              // Кодек видео
        "-crf", "18",                   // Качество видео
        "-preset", "slow",              // Баланс скорости/качества
        "-c:a", "aac",                  // Кодек аудио
        "-b:a", "192k",                 // Битрейт аудио
        "-t", "15",                     // Жестко задаем продолжительность 15 сек
        "-movflags", "+faststart",      // Оптимизация для веба
        "-y",                           // Перезапись без подтверждения
        outputVideo.absolutePath
    )

    // Execute the command asynchronously
    val process = ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()

    // Read and print the output and error streams
    val output = process.inputStream.bufferedReader().readText()
    val error = process.errorStream.bufferedReader().readText()

    // Wait for the process to complete
    val exitCode = process.waitFor()

    if (exitCode != 0) {
        println("Output: $output")
        println("Error: $error")
        throw Exception("FFmpeg command failed with error: $error")
    }

    return@withContext outputVideo
}

suspend fun imageToVideo(inputPhoto: File, height: Int, width: Int, overlayImage: String, mode: String): File =
    withContext(Dispatchers.IO) {
        val fileName = inputPhoto.nameWithoutExtension
        val fileExtension = "mp4"
        val outputFolder = File(
            if (mode == "dev")
                "$basePath/output"
            else
                "$basePath/output"
        ) //
        val outputVideo = File("${outputFolder.absolutePath}/${fileName}_new.$fileExtension")

        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        val newHeight = if (height % 2 != 0) height - 1 else height
        val newWidth = if (width % 2 != 0) width - 1 else width
        //logger.i("width: $newWidth height: $newHeight")
        val scale = if (newWidth > 1500) {
            val targetWidth = 1280
            val h = (targetWidth.toDouble() / newWidth.toDouble() * newHeight).toInt()
            "w=$targetWidth:h=${if (h % 2 != 0) h - 1 else h}"
        } else {
            "w=$newWidth:h=$newHeight"
        }
        //logger.i("scale: $scale")

        val inputAudio = if (mode == "dev")
            inputAudioDev
        else
            inputAudio

        val command = listOf(
            "ffmpeg",
            "-i", inputPhoto.absolutePath,  // Основное фото
            "-i", overlayImage,            // Оверлей
            "-i", inputAudio,              // Аудио
            "-filter_complex",
            "[0:v]scale=$scale[video];" +
                    "[1:v]scale=$scale[overlay];" +
                    "[video][overlay]overlay=0:0,noise=c0s=40:c0f=t+u[v];" +
                    "[2:a]atrim=0:15,asetpts=PTS-STARTPTS[a]", // Обрезаем аудио до 15 сек
            "-map", "[v]",                 // Видеопоток
            "-map", "[a]",                 // Аудиопоток
            "-c:v", "libx264",
            "-crf", "18",
            "-preset", "slow",
            "-c:a", "aac",
            "-b:a", "192k",
            "-t", "15",                    // Фиксированная длительность 15 сек
            "-movflags", "+faststart",
            "-max_muxing_queue_size", "1024",
            "-y",
            outputVideo.absolutePath
        )

        // Execute the command asynchronously
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        // Read and print the output and error streams
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        // Wait for the process to complete
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            println("Output: $output")
            println("Error: $error")
            throw Exception("FFmpeg command failed with error: $error")
        }

        return@withContext outputVideo
    }

fun getImageDimensions(imageFile: File): Pair<Int, Int> {
    val bufferedImage = ImageIO.read(imageFile)
    val width = bufferedImage?.width
    val height = bufferedImage?.height
    return if (width != null && height != null) Pair(width, height) else Pair(720, 1280)
}