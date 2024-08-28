import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// File paths for overlay image and input audio
const val overlayImage = "/home/rose/RNT/AskarFilmBot/src/main/resources/lalaland.png"
const val inputAudio = "/home/rose/RNT/AskarFilmBot/src/main/resources/thunder.mp3"

// Asynchronous function to process the video
suspend fun processVideo(inputVideo: File): File = withContext(Dispatchers.IO) {
    val fileName = inputVideo.nameWithoutExtension
    val fileExtension = inputVideo.extension
    val outputVideo = File("${fileName}_new.$fileExtension")

    // Construct the FFmpeg command
    val command = listOf(
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