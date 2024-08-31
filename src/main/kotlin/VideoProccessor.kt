import dev.inmo.kslog.common.i
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random

val overlayImages = listOf(
    "1.png",
    "2.png",
    "3.png",
    "4.png",
    "5.png",
    "6.png"
)

val overlayImagesDev = listOf(
    "/home/rose/RNT/AskarFilmBot/src/main/resources/1.png",
    "/home/rose/RNT/AskarFilmBot/src/main/resources/2.png",
    "/home/rose/RNT/AskarFilmBot/src/main/resources/3.png",
    "/home/rose/RNT/AskarFilmBot/src/main/resources/4.png",
    "/home/rose/RNT/AskarFilmBot/src/main/resources/5.png",
    "/home/rose/RNT/AskarFilmBot/src/main/resources/6.png"
)

//const val inputAudio = "/home/rose/RNT/AskarFilmBot/src/main/resources/Those around.mp3"
const val inputAudioDev = "/home/rose/RNT/AskarFilmBot/src/main/resources/Those_around_new.mp3"
const val inputAudio = "Those_around_new.mp3"
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
            "/home/rose/RNT/AskarFilmBot/output"
        else
            "/output"
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
        "-i", inputVideo.absolutePath,  // Input video
        "-i", overlayImage,  // Overlay image
        "-i", inputAudio,  // Input audio
        "-filter_complex",
        "[0:v]trim=end=3,tpad=stop_mode=clone:stop_duration=12[vfrozen];" +  // Trim and pad the video
                "[1:v]scale=$width:$height[overlay];" +  // Scale the overlay image to match video size
                "[vfrozen][overlay]overlay=0:0:enable='gte(t,3)'[v];" +  // Overlay settings to cover the full video
                "[2:a]anull[a]",  // Use input audio
        "-map", "[v]",  // Map video stream
        "-map", "[a]",  // Map audio stream
        "-c:v", "libx264",  // Video codec
        "-crf", "18",  // Quality setting for video
        "-preset", "slow",  // Encoding speed/quality balance
        "-c:a", "aac",  // Audio codec
        "-b:a", "192k",  // Audio bitrate
        "-shortest",  // Trim video to the shortest input length
        "-movflags", "+faststart",  // Optimize for web playback
        outputVideo.absolutePath,
        "-y"  // Overwrite without asking
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
                "/home/rose/RNT/AskarFilmBot/output"
            else
                "/output"
        ) //
        val outputVideo = File("${outputFolder.absolutePath}/${fileName}_new.$fileExtension")

        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        val newHeight = if (height % 2 != 0) height - 1 else height
        val newWidth = if (width % 2 != 0) width - 1 else width
        //val scale = "$newWidth:$newHeight"
        val scale = if (newWidth > 1500) "w=1280:h=-2" else "w=$newWidth:h=$newHeight"

        logger.i("scale: $scale")

        val inputAudio = if (mode == "dev")
            inputAudioDev
        else
            inputAudio

        // Construct the FFmpeg command
        val command = listOf(
            "ffmpeg",
            "-i", inputPhoto.absolutePath,  // Input video
            "-i", overlayImage,  // Overlay image
            "-i", inputAudio,  // Input audio
            "-filter_complex",
            "[0:v]trim=end=3,tpad=stop_mode=clone:stop_duration=15,scale=$scale[vfrozen];" +  // Trim, pad, and adjust height to be divisible by 2
                    "[1:v]scale=$scale[overlay];" +  // Scale the overlay image to match video size
                    "[vfrozen][overlay]overlay=0:0:enable='gte(t,3)'[v];" +  // Overlay settings to cover the full video
                    "[2:a]anull[a]",  // Use input audio
            "-map", "[v]",  // Map video stream
            "-map", "[a]",  // Map audio stream
            "-c:v", "libx264",  // Video codec
            "-crf", "18",  // Quality setting for video
            "-preset", "slow",  // Encoding speed/quality balance
            "-c:a", "aac",  // Audio codec
            "-b:a", "192k",  // Audio bitrate
            "-shortest",  // Trim video to the shortest input length
            "-movflags", "+faststart",  // Optimize for web playback
            "-max_muxing_queue_size", "1024",
            outputVideo.absolutePath,
            "-y"  // Overwrite without asking
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

/*val command = listOf(
        "ffmpeg",
        "-i", inputVideo.absolutePath,  // Input video
        "-i", overlayImage,  // Overlay image
        "-i", inputAudio,  // Input audio
        "-filter_complex",
        "[0:v]trim=end=5,tpad=stop_mode=clone:stop_duration=10[vfrozen];" +  // Trim and pad the video
                "[1:v]scale=300:-1[overlay];" +  // Scale the overlay image
                "[vfrozen][overlay]overlay=(main_w-overlay_w)/2:(main_h-overlay_h)/2:enable='gte(t,5)'[v];" +  // Overlay settings
                "[2:a]anull[a]",  // Use input audio
        "-map", "[v]",  // Map video stream
        "-map", "[a]",  // Map audio stream
        "-c:v", "libx264",  // Video codec
        "-crf", "18",  // Quality setting for video
        "-preset", "slow",  // Encoding speed/quality balance
        "-c:a", "aac",  // Audio codec
        "-b:a", "192k",  // Audio bitrate
        "-shortest",  // Trim video to the shortest input length
        "-movflags", "+faststart",  // Optimize for web playback
        outputVideo.absolutePath,
        "-y"  // Overwrite without asking
    )*/

fun getImageDimensions(imageFile: File): Pair<Int, Int> {
    val bufferedImage = ImageIO.read(imageFile)
    val width = bufferedImage?.width
    val height = bufferedImage?.height
    return if (width != null && height != null) Pair(width, height) else Pair(720, 1280)
}
