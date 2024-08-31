import dev.inmo.kslog.common.i
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FFMpeg() {
    private val max: Int = 5
    private val countOfProcesses: AtomicInteger = AtomicInteger(0)

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
                val file = imageToVideo(inputPhoto, height, width, overlayImage, mode)
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

    /*suspend fun addImageToProcess(inputPhoto: File, height: Int, width: Int, overlayImage: String): File =
        withContext(Dispatchers.IO) {
            while (isActive) {
                val current = countOfProcesses.get()
                logger.i("current: $current")
                if (current > max) {
                    delay(2000)
                } else {
                    break
                }
            }

            val file = imageToVideo(inputPhoto, height, width, overlayImage)
            val count = countOfProcesses.decrementAndGet()
            logger.i("count: $count")
            return@withContext file
        }

    suspend fun addVideoToProcess(inputVideo: File, height: Int, width: Int, overlayImage: String): File =
        withContext(Dispatchers.IO) {
            while (isActive) {
                val current = countOfProcesses.get()
                logger.i("current: $current")
                if (current > max) {
                    delay(2000)
                } else {
                    break
                }
            }

            val file = videoToVideoWithOverlay(inputVideo, height, width, overlayImage)
            val count = countOfProcesses.decrementAndGet()
            logger.i("count: $count")
            return@withContext file
        }*/
}