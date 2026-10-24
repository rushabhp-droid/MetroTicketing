package com.mumbaimetro;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.data.MetroDatabaseManager;
import com.mumbaimetro.model.Card;
import com.mumbaimetro.model.Station;
import com.mumbaimetro.model.Ticket;
import com.mumbaimetro.service.MetroService;
import com.mumbaimetro.ui.MetroAppGUI;

import javax.swing.*;
import java.awt.GraphicsEnvironment;

/**
 * Mumbai Metro Smart Card & Fare Ticketing System
 * Academic Mini-Project for Mumbai University (SE Semester 3 - OOPM)
 *
 * Concepts Demonstrated:
 * 1. Object-Oriented Principles: Encapsulation, Abstraction, Inheritance, Polymorphism
 * 2. Interfaces: Payable, PrintableReceipt
 * 3. Custom Exceptions: InsufficientBalanceException, MinimumBalanceException,
 *                      InvalidStationException, CardBlockedException
 * 4. Multithreading: PeakHourMonitorThread (concurrency & daemon threads)
 * 5. Collections Framework: List, Map, HashMap, ArrayList
 * 6. File I/O & Serialization: ObjectInputStream/OutputStream, BufferedWriter
 * 7. JDBC Connectivity: MetroDatabaseManager with PreparedStatements
 * 8. GUI: Java Swing with Live AFC Gate Simulation (with Headless Fallback)
 */
public class Main {

    public static void main(String[] args) {
        printBanner();

        // Apply global anti-aliasing and modern UI styling before any Swing components are initialized
        com.mumbaimetro.ui.UITheme.applyGlobalAntiAliasing();

        // Initialize SQLite Relational Database tables & constraints
        MetroDatabaseManager.initializeDatabase();

        boolean isHeadless = GraphicsEnvironment.isHeadless();

        if (!isHeadless) {
            // Desktop environment with GUI Display detected
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            SwingUtilities.invokeLater(() -> {
                try {
                    MetroAppGUI frame = new MetroAppGUI();
                    frame.setVisible(true);
                    System.out.println(">>> GUI launched successfully on desktop.");
                } catch (Exception e) {
                    System.err.println("Failed to initialize UI: " + e.getMessage());
                    runTerminalSimulation();
                }
            });
        } else {
            // Headless terminal environment (SSH / Container without X11/Wayland display)
            System.out.println(">>> Headless environment detected (no active GUI display).");
            System.out.println(">>> Running automated Metro Ticketing demonstration in terminal:\n");
            runTerminalSimulation();
        }
    }

    private static void printBanner() {
        System.out.println("========================================================================");
        System.out.println("   MUMBAI METRO ONE (MMOPL) - SMART FARE & TICKETING SYSTEM            ");
        System.out.println("   Mumbai University - SE Sem 3 OOPM Mini Project                      ");
        System.out.println("========================================================================");
        System.out.println(" [OK] OOP Hierarchy        : Card -> RegularCard, StudentCard, TouristPass");
        System.out.println(" [OK] Interfaces           : Payable, PrintableReceipt");
        System.out.println(" [OK] Custom Exceptions    : InsufficientBalance, MinimumBalance, etc.");
        System.out.println(" [OK] Multithreading       : PeakHourMonitorThread (Background Worker)");
        System.out.println(" [OK] Collections          : ArrayList, HashMap, Collections.unmodifiable");
        System.out.println(" [OK] Persistence          : File Serialization & JDBC Manager");
        System.out.println(" [OK] User Interface       : Java Swing GUI with Live AFC Gate Simulation");
        System.out.println("========================================================================\n");
    }

    /**
     * Fallback terminal simulation for headless environments
     */
    private static void runTerminalSimulation() {
        MetroService service = new MetroService();
        DataStore ds = DataStore.getInstance();

        Station versova = ds.getStationByName("Versova");
        Station andheri = ds.getStationByName("Andheri");
        Station ghatkopar = ds.getStationByName("Ghatkopar");

        System.out.println("--- 1. BOOKING SINGLE JOURNEY QR TOKEN ---");
        try {
            Ticket tkt = service.bookSingleJourneyToken(versova, andheri, 2, false, false);
            System.out.println(tkt.generateReceipt());
        } catch (Exception e) {
            System.err.println("Booking Error: " + e.getMessage());
        }

        System.out.println("\n--- 2. AFC SMART CARD GATE SIMULATION ---");
        Card student = ds.getCard("MMC-1002"); // Priya Deshmukh (Student)
        System.out.println("Passenger: " + student.getHolderName() + " (" + student.getCardType() + ")");
        System.out.println("Initial Balance: INR " + student.getBalance());

        try {
            System.out.println("\n>> Tap-In at: " + versova.getName());
            service.processTapIn(student.getCardId(), versova);
            System.out.println("[PASS] Gate Status: OPENED (Tap-In Successful)");

            System.out.println("\n>> Tap-Out at: " + ghatkopar.getName());
            MetroService.JourneyResult result = service.processTapOut(student.getCardId(), ghatkopar, false);
            System.out.println("[PASS] Gate Status: OPENED (Tap-Out Successful)");
            System.out.println(String.format("   Distance: %.2f km | Base: INR %.2f | Student Discount: INR %.2f | Fare: INR %.2f",
                    result.distanceKm, result.baseFare, result.discount, result.finalFare));
            System.out.println("   Remaining Card Balance: INR " + result.remainingBalance);
        } catch (Exception e) {
            System.err.println("Gate Error: " + e.getMessage());
        }

        System.out.println("\n--- 3. CUSTOM EXCEPTION DEMONSTRATION ---");
        Card lowBalCard = ds.getCard("MMC-1004"); // Neha Kulkarni (low balance)
        System.out.println("Testing Tap-In with Low Balance Card [" + lowBalCard.getCardId() + ", Balance: INR " + lowBalCard.getBalance() + "]:");
        try {
            service.processTapIn(lowBalCard.getCardId(), versova);
        } catch (Exception e) {
            System.out.println("[CAUGHT] Expected Exception Caught: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        }

        System.out.println("\n========================================================================");
        System.out.println(" To launch the Interactive Java Swing Desktop GUI, run:");
        System.out.println("   java -jar target/mumbai-metro-ticketing-1.0.0.jar (on any desktop machine)");
        System.out.println("========================================================================");
    }
}
