package com.mumbaimetro.model;

/**
 * Unlimited day pass card for visitors exploring Mumbai
 * Demonstrates: Inheritance, Method Overriding
 */
public class TouristPassCard extends Card {
    private static final long serialVersionUID = 1L;
    private static final double MIN_BALANCE = 0.0;

    private int validityDays;

    public TouristPassCard(String cardId, String holderName, String contactNumber, double initialBalance, int validityDays) {
        super(cardId, holderName, contactNumber, initialBalance);
        this.validityDays = validityDays > 0 ? validityDays : 1;
    }

    @Override
    public double calculateDiscount(double baseFare) {
        // Flat 10% discount on single fares plus zero minimum balance barrier
        return baseFare * 0.10;
    }

    @Override
    public double getMinimumBalanceRequired() {
        return MIN_BALANCE;
    }

    @Override
    public String getCardType() {
        return "Mumbai Darshan Tourist Card (" + validityDays + "-Day Pass)";
    }

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }
}
