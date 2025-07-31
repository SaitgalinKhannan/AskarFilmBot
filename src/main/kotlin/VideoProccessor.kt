import dev.inmo.kslog.common.i
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
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
    val outputVideo = File("${outputFolder.absolutePath}/${fileName}_new_${UUID.randomUUID()}.$fileExtension")
    /*val tempFile =
        File.createTempFile("${outputFolder.absolutePath}/${fileName}_temp_${UUID.randomUUID()}", ".$fileExtension")*/

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
        "-i", inputVideo.absolutePath,         // Входное видео
        "-i", overlayImage,                      // Изображение для оверлея
        "-i", inputAudio,                        // Входное аудио
        "-filter_complex",
        // Обработка видео:
        // 1. Обрезаем видео до 15 секунд (если длиннее) и сбрасываем временные метки.
        // 2. Масштабируем изображение под размер видео.
        // 3. Накладываем оверлей с самого начала.
        // 4. Применяем фильтр шума.
        "[0:v]trim=duration=15,setpts=PTS-STARTPTS[vmain];" +
                "[1:v]scale=$width:$height[overlay];" +
                "[vmain][overlay]overlay=0:0,noise=c0s=40:c0f=t+u[v];" + //noise=c0s=40:c0f=t+u
                // Обработка аудио:
                // Обрезаем аудио до 15 секунд (если длиннее) и сбрасываем временные метки.
                "[2:a]atrim=duration=15,asetpts=PTS-STARTPTS[a]",
        "-map", "[v]",                         // Выбираем обработанное видео
        "-map", "[a]",                         // Выбираем обработанное аудио
        "-c:v", "libx265",                      // Кодек для видео h264_nvenc libx264 libx265
        "-crf", "28",                           // Качество видео
        "-preset", "medium",                      // Баланс скорости и качества
        "-c:a", "aac",                          // Кодек для аудио
        "-b:a", "128k",                         // Битрейт аудио
        "-shortest",                            // Продолжительность по самому короткому потоку
        "-movflags", "+faststart",              // Оптимизация для веб-плееров
        //"-vf", "noise=alls=60:allf=t+u",
        "-y",                                    // Перезапись выходного файла
        outputVideo.absolutePath,
    )
    /*val commandAddNoise = listOf(
        "ffmpeg",
        "-i", tempFile.absolutePath,
        "-vf", "noise=c0s=40:c0f=t+u",
        "-c:a", "copy",  // Копируем аудио без перекодирования
        "-y",
        outputVideo.absolutePath
    )*/
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
        /*runCatching {
            tempFile.delete()
        }*/
        println("Output: $output")
        println("Error: $error")
        throw Exception("FFmpeg command failed with error: $error")
    }

    /*// Execute the command asynchronously
    val processAddingNoise = ProcessBuilder(commandAddNoise)
        .redirectErrorStream(true)
        .start()
    // Read and print the output and error streams
    val outputAddingNoise = processAddingNoise.inputStream.bufferedReader().readText()
    val errorAddingNoise = processAddingNoise.errorStream.bufferedReader().readText()
    // Wait for the process to complete
    val exitCodeAddingNoise = process.waitFor()

    if (exitCodeAddingNoise != 0) {
        println("Output: $outputAddingNoise")
        println("Error: $errorAddingNoise")
        throw Exception("FFmpeg command failed with error: $errorAddingNoise")
    }

    runCatching {
        tempFile.delete() // Всегда удаляем временный файл
    }*/

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
        val outputVideo = File("${outputFolder.absolutePath}/${fileName}_new_${UUID.randomUUID()}.$fileExtension")

        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        val newHeight = if (height % 2 != 0) height - 1 else height
        val newWidth = if (width % 2 != 0) width - 1 else width
        //logger.i("width: $newWidth height: $newHeight")
        val scale = if (newWidth > 1500) {
            val targetWidth = 1280
            val h = (targetWidth.toDouble() / newWidth.toDouble() * newHeight).toInt()
            val targetHeight = if (h % 2 != 0) h - 1 else h
            if (targetHeight > targetWidth) {
                "w=$targetWidth:h=$targetHeight"
            } else {
                "w=$targetHeight:h=$targetWidth"
            }
        } else {
            "w=$newWidth:h=$newHeight"
        }
        logger.i("scale: $scale")

        val inputAudio = if (mode == "dev")
            inputAudioDev
        else
            inputAudio

        val command = listOf(
            "ffmpeg",
            "-loop", "1",                             // Зацикливаем статичное изображение
            "-i", inputPhoto.absolutePath,            // Основное фото
            "-i", overlayImage,                       // Оверлей
            "-i", inputAudio,                         // Аудио
            "-filter_complex",
            "[0:v]scale=$scale[video];" +
                    "[1:v]scale=$scale[overlay];" +
                    "[video][overlay]overlay=0:0,noise=c0s=40:c0f=t+u,fps=15[v];" + // накладываем оверлей, шум и задаем fps=24
                    "[2:a]atrim=0:15,asetpts=PTS-STARTPTS[a]",                   // обрезаем аудио до 15 сек
            "-map", "[v]",                           // Видеопоток
            "-map", "[a]",                           // Аудиопоток
            "-c:v", "libx265",
            "-crf", "28",
            "-preset", "medium",
            "-c:a", "aac",
            "-b:a", "128k",
            "-t", "15",                              // Фиксированная длительность 15 сек
            "-movflags", "+faststart",
            "-max_muxing_queue_size", "1024",
            "-y",
            outputVideo.absolutePath
        )

        /*
        val command = listOf(
            "ffmpeg",
            "-i", inputPhoto.absolutePath,  // Основное фото
            "-i", overlayImage,            // Оверлей
            "-i", inputAudio,              // Аудио
            "-filter_complex",
            "[0:v]scale=$scale[video];" +
                    "[1:v]scale=$scale[overlay];" +
                    "[video][overlay]overlay=0:0,noise=c0s=50:c0f=t+u[v];" +
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
        */

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