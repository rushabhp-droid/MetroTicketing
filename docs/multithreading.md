# ⏱️ Multithreading & Background Daemons

This system implements real-time background processing using core Java multithreading integrated cleanly within Spring Boot's application lifecycle.

---

## 🧵 The `PeakHourMonitorThread` Daemon

In real metro networks, transit authorities implement dynamic congestion pricing: fares increase during high-density rush hours to manage passenger flow and offset peak power and rolling stock costs.

Rather than computing peak status on every inbound HTTP request, a dedicated background worker thread ([`PeakHourMonitorThread.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/threads/PeakHourMonitorThread.java)) continually monitors the system clock and flags peak conditions.

### Thread Implementation

```java
public class PeakHourMonitorThread extends Thread {

    private volatile boolean isPeakHour = false;
    private volatile boolean simulatedPeak = false;
    private static final int CHECK_INTERVAL_MS = 15000; // 15 seconds

    public PeakHourMonitorThread() {
        super("PeakHourMonitor-WorkerThread");
        setDaemon(true); // Does not prevent JVM termination
    }

    @Override
    public void run() {
        System.out.println("[Thread: " + getName() + "] Started monitoring rush hour traffic.");
        while (!isInterrupted()) {
            try {
                evaluatePeakHour();
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                System.out.println("[Thread: " + getName() + "] Interrupted. Shutting down gracefully.");
                interrupt(); // Re-assert interrupt flag
                break;
            }
        }
    }

    private void evaluatePeakHour() {
        if (simulatedPeak) {
            this.isPeakHour = true;
            return;
        }

        LocalTime now = LocalTime.now();
        // Morning Peak: 08:00 to 11:30
        boolean morning = !now.isBefore(LocalTime.of(8, 0)) && !now.isAfter(LocalTime.of(11, 30));
        // Evening Peak: 17:30 to 21:00
        boolean evening = !now.isBefore(LocalTime.of(17, 30)) && !now.isAfter(LocalTime.of(21, 0));

        this.isPeakHour = morning || evening;
    }
}
```

---

## ⚙️ Spring Lifecycle Integration

In a web application, spawning unmanaged background threads with `new Thread().start()` can lead to thread leaks and Zombie processes when the application is reloaded or shut down.

To prevent this, the daemon is declared as a managed Spring bean in [`MetroConfig.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/config/MetroConfig.java):

```java
@Configuration
public class MetroConfig {

    @Bean(destroyMethod = "interrupt")
    public PeakHourMonitorThread peakHourMonitorThread() {
        PeakHourMonitorThread thread = new PeakHourMonitorThread();
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
```

### Lifecycle Benefits
1. **Automated Startup:** The thread starts automatically when Spring Boot's application context is fully initialized.
2. **Graceful Shutdown:** The `destroyMethod = "interrupt"` directive instructs Spring to call `thread.interrupt()` when the web server stops. The `InterruptedException` breaks the worker sleep loop and terminates cleanly.
3. **Dependency Injection:** The thread instance can be injected into REST controllers (e.g., [`StatusApiController.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/api/StatusApiController.java)) to provide live status metrics to the frontend.

---

## 🔒 Concurrency Patterns & Thread Safety

The project utilizes several fundamental concurrency mechanisms:

| Mechanism | Purpose | Location |
| :--- | :--- | :--- |
| `volatile` | Ensures memory visibility of `isPeakHour` and `simulatedPeak` across reader threads without CPU caching stale values. | [`PeakHourMonitorThread.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/threads/PeakHourMonitorThread.java#L15-L16) |
| `setDaemon(true)` | Prevents the background worker from holding the JVM alive when all web threads have completed. | [`PeakHourMonitorThread.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/threads/PeakHourMonitorThread.java#L21) |
| `synchronized` methods | Guarantees mutual exclusion and prevents race conditions during balance deductions and turnstile updates. | [`Card.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Card.java#L66-L84) |
| `synchronized` repository | Coordinates concurrent cache writes and transaction logging to SQLite. | [`DataStore.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/data/DataStore.java#L31-L98) |

### Frontend Polling
The React frontend ([`App.jsx`](file:///home/rushabhp/Projects/CNN/frontend/src/App.jsx#L68-L75)) periodically queries `/api/status` every 8 seconds, updating the top banner badge between:
- `PEAK RUSH SURCHARGE (+₹5)` (Red glowing badge)
- `STANDARD FARE HOURS` (Green steady badge)
