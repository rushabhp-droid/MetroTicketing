package com.mumbaimetro.model;

/**
 * Standard commuter smart card
 * Demonstrates: Inheritance, Method Overriding
 */
public class RegularCard extends Card {
    private static final long serialVersionUID = 1L;
    private static final double MIN_BALANCE = 50.0;

    public RegularCard(String cardId, String holderName, String contactNumber, double initialBalance) {
        super(cardId, holderName, contactNumber, initialBalance);
    }

    @Override
    public double calculateDiscount(double baseFare) {
        // Regular card gets 5% loyalty discount on fares over ₹30
        if (baseFare >= 30.0) {
            return baseFare * 0.05;
        }
        return 0.0;
    }

    @Override
    public double getMinimumBalanceRequired() {
        return MIN_BALANCE;
    }

    @Override
    public String getCardType() {
        return "Regular Commuter Card";
    }
}
