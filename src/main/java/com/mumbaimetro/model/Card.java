package com.mumbaimetro.model;

import com.mumbaimetro.exceptions.InsufficientBalanceException;
import com.mumbaimetro.interfaces.Payable;
import com.mumbaimetro.interfaces.PrintableReceipt;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Abstract base class for all Mumbai Metro Smart Cards
 * Demonstrates: Abstraction, Encapsulation, Interface Implementation,
 * Thread-safety (synchronized methods), and Polymorphism.
 */
public abstract class Card implements Payable, PrintableReceipt, Serializable {
    private static final long serialVersionUID = 1L;

    protected final String cardId;
    protected String holderName;
    protected String contactNumber;
    protected double balance;
    protected final String issueDate;
    protected boolean isBlocked;
    protected Station tapInStation;
    protected LocalDateTime tapInTime;

    public Card(String cardId, String holderName, String contactNumber, double initialBalance) {
        if (cardId == null || cardId.trim().isEmpty()) {
            throw new IllegalArgumentException("Card ID cannot be empty");
        }
        if (holderName == null || holderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Holder Name cannot be empty");
        }
        this.cardId = cardId.trim().toUpperCase();
        this.holderName = holderName.trim();
        this.contactNumber = contactNumber != null ? contactNumber.trim() : "";
        this.balance = Math.max(0.0, initialBalance);
        this.issueDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"));
        this.isBlocked = false;
        this.tapInStation = null;
        this.tapInTime = null;
    }

    // --- Abstract Methods to be implemented by child classes (Polymorphism) ---

    /**
     * Calculates concession/discount percentage or amount on base fare
     * @param baseFare calculated standard fare
     * @return discount amount in rupees
     */
    public abstract double calculateDiscount(double baseFare);

    /**
     * Returns the minimum security threshold needed to enter gates
     * @return minimum required balance in rupees
     */
    public abstract double getMinimumBalanceRequired();

    /**
     * Identifies card category
     * @return type name string
     */
    public abstract String getCardType();

    // --- Concrete Synchronized Methods (Thread-Safe Financial Operations) ---

    @Override
    public synchronized void recharge(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Recharge amount must be greater than zero");
        }
        this.balance += amount;
    }

    @Override
    public synchronized void deduct(double amount) throws InsufficientBalanceException {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduction amount cannot be negative");
        }
        if (this.balance < amount) {
            throw new InsufficientBalanceException(amount, this.balance);
        }
        this.balance -= amount;
    }

    public synchronized void tapIn(Station station) {
        this.tapInStation = station;
        this.tapInTime = LocalDateTime.now();
    }

    public synchronized Station tapOut() {
        Station departure = this.tapInStation;
        this.tapInStation = null;
        this.tapInTime = null;
        return departure;
    }

    public boolean isInTransit() {
        return this.tapInStation != null;
    }

    public Station getTapInStation() {
        return tapInStation;
    }

    public LocalDateTime getTapInTime() {
        return tapInTime;
    }

    // --- Getters and Setters ---

    @Override
    public synchronized double getBalance() {
        return balance;
    }

    public String getCardId() {
        return cardId;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        if (holderName != null && !holderName.trim().isEmpty()) {
            this.holderName = holderName.trim();
        }
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
    }

    @Override
    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("         MUMBAI METRO SMART CARD         \n");
        sb.append("=========================================\n");
        sb.append(String.format(" Card ID      : %s\n", cardId));
        sb.append(String.format(" Holder       : %s\n", holderName));
        sb.append(String.format(" Card Type    : %s\n", getCardType()));
        sb.append(String.format(" Balance      : ₹%.2f\n", balance));
        sb.append(String.format(" Status       : %s\n", isBlocked ? "BLOCKED" : "ACTIVE"));
        sb.append(String.format(" Issue Date   : %s\n", issueDate));
        sb.append("=========================================\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - ₹%.2f", cardId, holderName, getCardType(), balance);
    }
}
