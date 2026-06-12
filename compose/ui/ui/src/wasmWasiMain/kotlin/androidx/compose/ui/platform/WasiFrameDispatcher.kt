/*
 * Single-threaded frame-pumped CoroutineDispatcher + Delay for wasi.
 *
 * Why this exists:
 *   `BaseComposeScene` defaults its coroutineContext to `Dispatchers.Unconfined`.
 *   Upstream flags this with a `// TODO` in the factory signature. On wasi:
 *
 *   1. With `Unconfined`, continuations resumed by `BroadcastFrameClock.sendFrame`
 *      execute synchronously on the stack of the sender, and the
 *      `Transition.animateTo` coroutine loop (`while (isActive) { withFrameNanos … }`)
 *      re-suspends on the next `withFrameNanos` synchronously, all on the same
 *      call stack. `derivedStateOf { runFrameLoop }` then doesn't re-evaluate
 *      cleanly across animation cycles — Material3 transition widgets toggle
 *      once and stick.
 *
 *   2. kotlinx-coroutines' default `Delay` implementation on wasmWasi falls
 *      back to a polling spin when no dispatcher in the coroutine context
 *      implements `Delay`. Compose Foundation's `CursorAnimationState` opens
 *      a `while(true){ delay(500); … }` blink loop on text-field focus — with
 *      no real Delay it busy-loops at 100% CPU, freezing the UI thread.
 *      (CursorAnimationState's own kdoc says "pure coroutine delays will not
 *      cause any work until the delay is over" — that contract is what we
 *      provide here.)
 *
 * Architecture:
 *   - `dispatch` queues a Runnable onto the per-frame queue.
 *   - `flush()` (called by the renderer after `scene.render`) drains the
 *     queue and runs each Runnable. Swap-buffer pattern: new dispatches that
 *     happen DURING flush land back in the main queue (run next frame), so a
 *     `withFrameNanos`-resumer that re-suspends on the next withFrameNanos
 *     doesn't tight-loop within a single flush.
 *   - `scheduleResumeAfterDelay` (Delay impl): records a deadline (in cached
 *     frame-nanos) and resumes the continuation when `flush()` sees the
 *     deadline has passed.
 *   - `flush()` is the heartbeat for time-based wake-ups, so the renderer
 *     must keep calling it every frame even if the visible scene is idle —
 *     `host/lib.rs` does this via `request_redraw()` at the end of every
 *     RedrawRequested handler.
 *
 * wasi is single-threaded so we don't need synchronization.
 */
package androidx.compose.ui.platform

import androidx.compose.ui.cachedNanoTime
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable

@OptIn(InternalCoroutinesApi::class)
public class WasiFrameDispatcher : CoroutineDispatcher(), Delay {
    private val queue = ArrayDeque<Runnable>()
    // Swap-buffer pattern: drain the current queue into `running` and execute
    // from there, so any new dispatches that happen DURING flush land in the
    // main queue (run next frame) and not in this iteration (which would
    // tight-loop withFrameNanos awaiters indefinitely).
    private val running = ArrayDeque<Runnable>()

    // Delayed tasks: a flat list of (deadline-millis, runnable). Compose has
    // few concurrent delays in flight at once (cursor blink, snackbar timeout,
    // etc.) so an unsorted list with linear scan is fine. If this grows,
    // swap for a priority queue.
    private class DelayedTask(
        var deadlineMillis: Long,
        var runnable: Runnable?,  // nulled out on cancel
    )
    private val delayed = ArrayList<DelayedTask>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.add(block)
    }

    /// Read the most recent monotonic time the renderer captured. This is
    /// fed from `Main.kt`'s renderDelegate before each `scene.render` call,
    /// via `androidx.compose.ui.updateCachedNanoTime(nanos)`. We use the
    /// cached value rather than calling `currentNanoTime()` ourselves because
    /// of the realloc-allocator pollution noted in
    /// `feedback_currentnanotime_pollutes.md`.
    private fun nowMillis(): Long = cachedNanoTime() / 1_000_000L

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>,
    ) {
        val task = DelayedTask(
            deadlineMillis = nowMillis() + timeMillis,
            runnable = Runnable {
                with(continuation) {
                    if (isActive) resumeUndispatched(Unit)
                }
            },
        )
        delayed.add(task)
        continuation.invokeOnCancellation { task.runnable = null }
    }

    /// `withTimeout(...)` registers an `invokeOnTimeout` callback that fires
    /// after the given millis to cancel the inner job. Same machinery as
    /// scheduleResumeAfterDelay — just a different completion path.
    override fun invokeOnTimeout(
        timeMillis: Long,
        block: Runnable,
        context: CoroutineContext,
    ): DisposableHandle {
        val task = DelayedTask(
            deadlineMillis = nowMillis() + timeMillis,
            runnable = block,
        )
        delayed.add(task)
        return DisposableHandle { task.runnable = null }
    }

    /**
     * Drains every Runnable queued so far and runs them in FIFO order. Also
     * resumes any delayed tasks whose deadline has passed (per the cached
     * monotonic nanos last set by the renderer).
     *
     * New Runnables enqueued during execution will be picked up by the NEXT
     * flush, not this one.
     */
    /**
     * Task 64 — on-demand rendering support. Milliseconds from `now` until the
     * next moment `flush()` would have work to do:
     *   - `0` if there are already-queued runnables or a delayed task is due
     *     (the host should render the next frame immediately),
     *   - else the smallest remaining delay over pending delayed tasks
     *     (cursor blink, `withTimeout`, snackbar timeout, …),
     *   - else [Long.MAX_VALUE] when fully idle (no timed wake pending).
     *
     * The frame-pacing export combines this with `ComposeScene.hasInvalidations()`
     * so a static scene with a pending `delay()` still gets woken on time
     * (`flush()` is the only heartbeat for timed resumes — see the class kdoc).
     */
    public fun nextDeadlineMillis(now: Long): Long {
        if (queue.isNotEmpty() || running.isNotEmpty()) return 0L
        var min = Long.MAX_VALUE
        for (t in delayed) {
            if (t.runnable == null) continue
            val remaining = t.deadlineMillis - now
            if (remaining <= 0L) return 0L
            if (remaining < min) min = remaining
        }
        return min
    }

    public fun flush() {
        // 1. Materialize any due delayed tasks into the main queue.
        if (delayed.isNotEmpty()) {
            val now = nowMillis()
            var i = 0
            while (i < delayed.size) {
                val t = delayed[i]
                val r = t.runnable
                if (r == null) {
                    // Cancelled — drop.
                    delayed.removeAt(i)
                } else if (t.deadlineMillis <= now) {
                    delayed.removeAt(i)
                    queue.add(r)
                } else {
                    i++
                }
            }
        }
        if (queue.isEmpty()) return
        // 2. Swap-buffer drain (existing logic).
        while (queue.isNotEmpty()) running.add(queue.removeFirst())
        while (running.isNotEmpty()) {
            val r = running.removeFirst()
            try {
                r.run()
            } catch (t: Throwable) {
                // Surface the error to host log so we can see if a queued
                // task is throwing (would otherwise be swallowed silently
                // by the coroutine machinery).
                org.jetbrains.skiko.wasi.shell.Logging.Import.log(
                    org.jetbrains.skiko.wasi.shell.Logging.Level.WARN, "compose-ui",
                    "WasiFrameDispatcher task threw: ${t::class.simpleName}: ${t.message}"
                )
            }
        }
    }
}
