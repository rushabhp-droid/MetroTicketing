package com.mumbaimetro.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model class representing any financial or journey transaction
 * Demonstrates: Encapsulation, Audit Trails
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        CARD_RECHARGE,
        CARD_JOURNEY_DEDUCTION,
        TOKEN_PURCHASE,
        CARD_ISSUED,
        PENALTY_DEDUCTION
    }

    private final String txnId;
    private final String referenceId; // Card ID or Ticket ID
    private final Type type;
    private final double amount;
    private final String sourceStation;
    private final String destStation;
    private final String timestamp;
    private final String status;
    private final double balanceAfter;

    public Transaction(String txnId, String referenceId, Type type, double amount,
                       String sourceStation, String destStation, double balanceAfter, String status) {
        this.txnId = txnId;
        this.referenceId = referenceId;
        this.type = type;
        this.amount = amount;
        this.sourceStation = sourceStation != null ? sourceStation : "-";
        this.destStation = destStation != null ? destStation : "-";
        this.balanceAfter = balanceAfter;
        this.status = status;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
    }

    public String getTxnId() {
        return txnId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getRefId() {
        return referenceId;
    }

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getSourceStation() {
        return sourceStation;
    }

    public String getDestStation() {
        return destStation;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getCreatedAt() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | ₹%.2f | %s -> %s | %s",
                txnId, referenceId, type, amount, sourceStation, destStation, status);
    }
}
