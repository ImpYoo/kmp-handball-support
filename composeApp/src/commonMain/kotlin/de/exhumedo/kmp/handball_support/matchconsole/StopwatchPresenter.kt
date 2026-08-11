package de.exhumedo.kmp.handball_support.matchconsole

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * State holder for a simple match stopwatch.
 *
 * Time is tracked against a monotonic clock rather than by accumulating timer
 * ticks, so the displayed value stays accurate even if [tick] is called
 * irregularly (e.g. while the screen is backgrounded). [tick] only refreshes the
 * observable [elapsedMillis]; the source of truth is the monotonic mark.
 */
class StopwatchPresenter(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    /** Elapsed time in milliseconds, exposed for the UI to format as mm:ss. */
    var elapsedMillis by mutableStateOf(0L)
        private set

    /** Whether the stopwatch is currently counting up. */
    var isRunning by mutableStateOf(false)
        private set

    private var accumulatedMillis = 0L
    private var startMark: TimeMark? = null

    /** Toggles between running and paused. */
    fun toggle() {
        if (isRunning) stop() else start()
    }

    fun start() {
        if (isRunning) return
        startMark = timeSource.markNow()
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        accumulatedMillis = currentElapsed()
        startMark = null
        isRunning = false
        elapsedMillis = accumulatedMillis
    }

    /** Stops and clears the stopwatch. */
    fun reset() {
        isRunning = false
        startMark = null
        accumulatedMillis = 0L
        elapsedMillis = 0L
    }

    /** Overrides the elapsed time (e.g. from the edit dialog), keeping the run state. */
    fun setElapsed(millis: Long) {
        val safe = millis.coerceAtLeast(0L)
        accumulatedMillis = safe
        startMark = if (isRunning) timeSource.markNow() else null
        elapsedMillis = safe
    }

    /** Refreshes [elapsedMillis] from the monotonic clock while running. */
    fun tick() {
        if (isRunning) elapsedMillis = currentElapsed()
    }

    private fun currentElapsed(): Long =
        accumulatedMillis + (startMark?.elapsedNow()?.inWholeMilliseconds ?: 0L)
}

