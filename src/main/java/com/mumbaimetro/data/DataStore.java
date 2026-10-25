package com.mumbaimetro.data;

import com.mumbaimetro.model.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Repository layer connecting business services directly to SQLite Relational Database.
 * Supports multi-line filtering across all 7 operational Mumbai & Navi Mumbai Metro lines:
 * Line 1, Line 2A, Line 7, Line 9, Line 3, Line 2B, and Navi Mumbai Line 1.
 */
public class DataStore {

    private static DataStore instance;

    private DataStore() {
        MetroDatabaseManager.initializeDatabase();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public synchronized void registerCard(Card card) {
        if (card != null) {
            MetroDatabaseManager.saveCard(card);
        }
    }

    public synchronized Card getCard(String cardId) {
        return MetroDatabaseManager.getCard(cardId);
    }

    public synchronized List<Card> getAllCards() {
        return MetroDatabaseManager.getAllCards();
    }

    public List<Station> getStations() {
        return MetroDatabaseManager.getAllStations();
    }

    public List<Station> getStationsByLine(String lineCode) {
        return MetroDatabaseManager.getStationsByLine(lineCode);
    }

    public Map<String, String> getAvailableLines() {
        return MetroDatabaseManager.getAvailableLines();
    }

    public Station getStationById(int id) {
        for (Station s : getStations()) {
            if (s.getStationId() == id) {
                return s;
            }
        }
        return null;
    }

    public Station getStationByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String trimmed = name.trim();
        try {
            int id = Integer.parseInt(trimmed);
            Station byId = getStationById(id);
            if (byId != null) return byId;
        } catch (NumberFormatException ignored) {}

        for (Station s : getStations()) {
            if (s.getName().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }
        return null;
    }

    public synchronized void logTransaction(Transaction txn) {
        if (txn != null) {
            MetroDatabaseManager.logTransaction(txn);
        }
    }

    public synchronized List<Transaction> getTransactions() {
        return MetroDatabaseManager.getAllTransactions();
    }

    public synchronized Map<String, Integer> getStationTraffic() {
        return MetroDatabaseManager.getStationTrafficSummary();
    }

    public synchronized double getTotalRevenue() {
        return MetroDatabaseManager.getTotalRevenue();
    }

    public synchronized void saveDataToDisk() {
        // Relational commits are executed immediately via SQL
    }

    public static void saveReceiptToFile(String filename, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(content);
        }
    }
}
