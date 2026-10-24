package com.mumbaimetro.exceptions;

/**
 * Custom Exception thrown when tap-in is attempted with balance below minimum security deposit
 */
public class MinimumBalanceException extends Exception {
    private final double minRequired;
    private final double currentBalance;

    public MinimumBalanceException(double minRequired, double currentBalance) {
        super(String.format("Minimum balance of ₹%.2f required to enter station! Current balance: ₹%.2f",
                minRequired, currentBalance));
        this.minRequired = minRequired;
        this.currentBalance = currentBalance;
    }

    public double getMinRequired() {
        return minRequired;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }
}
