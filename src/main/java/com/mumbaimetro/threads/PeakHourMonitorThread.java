package com.mumbaimetro.threads;

import java.time.LocalTime;

/**
 * Multithreaded background worker that monitors peak rush hours.
 * Demonstrates: Multithreading by extending Thread, Thread lifecycle (run, sleep, interrupt),
 * volatile keyword, and Listener Callback pattern.
 */
public class PeakHourMonitorThread extends Thread {

    public interface PeakHourListener {
        void onPeakHourStatusChanged(boolean isPeakHour, String reason);
    }

    private volatile boolean running = true;
    private volatile boolean isPeakHour = false;
    private boolean simulationMode = true; // Allows easy viva demo by toggling every 25 seconds
    private final PeakHourListener listener;

    public PeakHourMonitorThread(PeakHourListener listener) {
        super("PeakHourMonitor-WorkerThread");
        this.listener = listener;
        setDaemon(true); // Daemon thread exits when main application closes
    }

    @Override
    public void run() {
        System.out.println("[Thread: " + getName() + "] Started monitoring rush hour traffic.");
        boolean lastStatus = !isPeakHour;

        while (running) {
            try {
                if (simulationMode) {
                    // In simulation mode, toggle peak/off-peak to demonstrate dynamic fare in viva
                    isPeakHour = !isPeakHour;
                    String message = isPeakHour
                            ? "PEAK RUSH HOUR ACTIVE (+₹5 Surcharge applied)"
                            : "OFF-PEAK NORMAL HOURS (Standard Base Fares)";

                    if (listener != null) {
                        listener.onPeakHourStatusChanged(isPeakHour, message);
                    }
                    // Sleep for 25 seconds before toggling in demo
                    Thread.sleep(25000);
                } else {
                    // Real-time mode: Peak hours 08:30 - 11:30 and 17:30 - 20:30
                    LocalTime now = LocalTime.now();
                    boolean morningPeak = (now.isAfter(LocalTime.of(8, 30)) && now.isBefore(LocalTime.of(11, 30)));
                    boolean eveningPeak = (now.isAfter(LocalTime.of(17, 30)) && now.isBefore(LocalTime.of(20, 30)));
                    isPeakHour = morningPeak || eveningPeak;

                    if (isPeakHour != lastStatus) {
                        lastStatus = isPeakHour;
                        if (listener != null) {
                            String reason = isPeakHour
                                    ? "PEAK RUSH HOUR (8:30-11:30 AM / 5:30-8:30 PM)"
                                    : "OFF-PEAK HOURS (Standard Slabs)";
                            listener.onPeakHourStatusChanged(isPeakHour, reason);
                        }
                    }
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                System.out.println("[Thread: " + getName() + "] Interrupted. Shutting down.");
                break;
            }
        }
    }

    public void stopMonitoring() {
        this.running = false;
        interrupt();
    }

    public boolean isPeakHour() {
        return isPeakHour;
    }

    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    public boolean isSimulationMode() {
        return simulationMode;
    }

    public void togglePeakHourManually() {
        this.isPeakHour = !this.isPeakHour;
        if (listener != null) {
            listener.onPeakHourStatusChanged(isPeakHour, isPeakHour ? "MANUAL PEAK HOUR" : "MANUAL NORMAL");
        }
    }
}
