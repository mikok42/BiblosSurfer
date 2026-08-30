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
    private var pending: (() -> Unit)? = null

    fun schedule(afterSeconds: Double, action: () -> Unit) {
        job?.cancel()
        pending = action
        job = scope.launch {
            delay((afterSeconds * 1000).toLong())
            val toRun = pending
            pending = null
            job = null
            toRun?.invoke()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        pending = null
    }

    fun flush() {
        val toRun = pending ?: return
        pending = null
        job?.cancel()
        job = null
        toRun()
    }
}
