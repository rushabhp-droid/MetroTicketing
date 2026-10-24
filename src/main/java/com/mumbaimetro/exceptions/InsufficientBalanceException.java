package com.mumbaimetro.exceptions;

/**
 * Custom Exception thrown when a smart card lacks sufficient funds for travel
 * Demonstrates: User-defined Exception, State preservation in Exceptions
 */
public class InsufficientBalanceException extends Exception {
    private final double requiredAmount;
    private final double currentBalance;

    public InsufficientBalanceException(double requiredAmount, double currentBalance) {
        super(String.format("Insufficient card balance! Required: ₹%.2f, Available: ₹%.2f. Please recharge.",
                requiredAmount, currentBalance));
        this.requiredAmount = requiredAmount;
        this.currentBalance = currentBalance;
    }

    public double getRequiredAmount() {
        return requiredAmount;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getDeficit() {
        return requiredAmount - currentBalance;
    }
}
