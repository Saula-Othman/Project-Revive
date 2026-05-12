package pro.revive.utils.TriageUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared background thread pool for all controller async tasks.
 *
 * All threads are daemon threads so they do not prevent the JVM from
 * exiting when the JavaFX window is closed.
 *
 * Usage:
 *   AppExecutor.run(() -> {
 *       // background work ...
 *       Platform.runLater(() -> { /* UI update *&#47; });
 *   });
 */
public final class AppExecutor {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(new DaemonThreadFactory());

    private AppExecutor() {}

    /** Submit a task to the shared pool. */
    public static void run(Runnable task) {
        POOL.submit(task);
    }

    /** Gracefully shut down the pool (call from App.stop() if needed). */
    public static void shutdown() {
        POOL.shutdown();
    }

    // ── Daemon thread factory ────────────────────────────────────
    private static class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "revive-bg-" + count.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
