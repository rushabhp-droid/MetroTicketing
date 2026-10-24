package com.mumbaimetro.interfaces;

/**
 * Interface representing items that can produce formatted text receipts
 * Demonstrates: Interface Polymorphism
 */
public interface PrintableReceipt {
    /**
     * Generates a formatted ASCII receipt for tickets, recharges, or journeys
     * @return Formatted multi-line receipt string
     */
    String generateReceipt();
}
