package miko.biblossurfer.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Debouncer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate),
) {
    private var job: Job? = null

    fun schedule(afterSeconds: Double, action: () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay((afterSeconds * 1000).toLong())
            action()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
