package com.mumbaimetro.exceptions;

/**
 * Custom Exception thrown when an operation is attempted on a blocked or blacklisted card
 */
public class CardBlockedException extends Exception {
    private final String cardId;

    public CardBlockedException(String cardId) {
        super(String.format("Card [%s] is BLOCKED/SUSPENDED. Please contact Customer Care at Metro Station.", cardId));
        this.cardId = cardId;
    }

    public String getCardId() {
        return cardId;
    }
}
