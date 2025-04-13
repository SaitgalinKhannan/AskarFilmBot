import dev.inmo.kslog.common.i
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FFMpeg {
    private val max: Int = 3
    private val countOfProcesses: AtomicInteger = AtomicInteger(0)
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private suspend fun CoroutineScope.waitForAvailableSlot() {
        while (isActive) {
            val current = countOfProcesses.get()
            logger.i("Current count: $current")
            if (current >= max) {
                logger.i("I`m waiting for available slot...")
                delay(2000)
            } else {
                break
            }
        }
    }

    suspend fun addImageToProcess(inputPhoto: File, height: Int, width: Int, overlayImage: String, mode: String): File =
        withContext(Dispatchers.IO) {
            waitForAvailableSlot()
            countOfProcesses.incrementAndGet()  // Increment when starting a new process
            try {
                val file = imageToVideo(
                    inputPhoto = inputPhoto,
                    height = height,
                    width = width,
                    overlayImage = overlayImage,
                    mode = mode
                )
                return@withContext file
            } finally {
                val count = countOfProcesses.decrementAndGet()  // Decrement after finishing the process
                logger.i("Process completed. Current count: $count")
            }
        }

    suspend fun addVideoToProcess(inputVideo: File, height: Int, width: Int, overlayImage: String, mode: String): File =
        withContext(Dispatchers.IO) {
            waitForAvailableSlot()
            countOfProcesses.incrementAndGet()  // Increment when starting a new process
            try {
                val file = videoToVideoWithOverlay(inputVideo, height, width, overlayImage, mode)
                return@withContext file
            } finally {
                val count = countOfProcesses.decrementAndGet()  // Decrement after finishing the process
                logger.i("Process completed. Current count: $count")
            }
        }

    suspend fun test() {
        /*for (i in 0 until 50) {
            coroutineScope.launch {
                addImageToProcess(inputPhoto = File("/home/faye/Projects/RNT/AskarFilmBot/sources/538321015-12.04.25 19:25:35.jpg"), height = 1280, width = 960, overlayImage = "/home/faye/Projects/RNT/AskarFilmBot/resources/poster_blue_top_16:9.png", mode = "dev")
            }
        }*/
        val jobs = List(1000) {
            coroutineScope.launch {
                addImageToProcess(
                    inputPhoto = File("/home/faye/Projects/RNT/AskarFilmBot/sources/538321015-12.04.25 19:25:35.jpg"),
                    height = 1280,
                    width = 960,
                    overlayImage = "/home/faye/Projects/RNT/AskarFilmBot/resources/poster_blue_top_16:9.png",
                    mode = "dev"
                )
            }
        }
        jobs.forEach { it.join() }
    }
}