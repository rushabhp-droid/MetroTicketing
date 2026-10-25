package com.mumbaimetro.service;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.exceptions.*;
import com.mumbaimetro.model.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core business logic service orchestrating ticket issuing, gate validation, and card operations
 * Demonstrates: Layered Architecture, Exception Handling, Business Rule Validation
 */
public class MetroService {

    private final DataStore dataStore;
    private final FareCalculator fareCalculator;
    private String generateTxnId() {
        return "TXN-" + (System.currentTimeMillis() % 10000000) + (int)(Math.random() * 10);
    }

    private String generateTicketId() {
        return "TKT-" + (System.currentTimeMillis() % 1000000) + (int)(Math.random() * 10);
    }

    private String generateCardId() {
        return "MMC-" + (1000 + (int)(Math.random() * 8999));
    }

    public MetroService() {
        this.dataStore = DataStore.getInstance();
        this.fareCalculator = new FareCalculator();
    }

    public FareCalculator getFareCalculator() {
        return fareCalculator;
    }

    public List<Station> getStations() {
        return dataStore.getStations();
    }

    public List<Station> getStationsByLine(String lineCode) {
        return dataStore.getStationsByLine(lineCode);
    }

    public java.util.Map<String, String> getAvailableLines() {
        return dataStore.getAvailableLines();
    }

    public List<Card> getAllCards() {
        return dataStore.getAllCards();
    }

    public List<Transaction> getAllTransactions() {
        return dataStore.getTransactions();
    }

    public Card getCard(String cardId) {
        return dataStore.getCard(cardId);
    }

    /**
     * Issues a new smart card
     */
    public Card issueCard(String holderName, String contact, String type, double initialBalance,
                          String college, String rollNo, int validityDays) {
        String cardId = generateCardId();
        Card card;

        if ("STUDENT".equalsIgnoreCase(type)) {
            card = new StudentCard(cardId, holderName, contact, initialBalance, college, rollNo);
        } else if ("TOURIST".equalsIgnoreCase(type)) {
            card = new TouristPassCard(cardId, holderName, contact, initialBalance, validityDays);
        } else {
            card = new RegularCard(cardId, holderName, contact, initialBalance);
        }

        dataStore.registerCard(card);

        String txnId = generateTxnId();
        dataStore.logTransaction(new Transaction(
                txnId, cardId, Transaction.Type.CARD_ISSUED, initialBalance,
                "-", "-", initialBalance, "SUCCESS"
        ));

        return card;
    }

    /**
     * Recharges an existing card
     */
    public double rechargeCard(String cardId, double amount) throws CardBlockedException {
        Card card = dataStore.getCard(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Card with ID [" + cardId + "] does not exist.");
        }
        if (card.isBlocked()) {
            throw new CardBlockedException(cardId);
        }

        card.recharge(amount);
        dataStore.registerCard(card);

        String txnId = generateTxnId();
        dataStore.logTransaction(new Transaction(
                txnId, card.getCardId(), Transaction.Type.CARD_RECHARGE, amount,
                "-", "-", card.getBalance(), "SUCCESS"
        ));

        return card.getBalance();
    }

    /**
     * Toggles block/unblock status of a card
     */
    public boolean toggleBlockStatus(String cardId) {
        Card card = dataStore.getCard(cardId);
        if (card != null) {
            card.setBlocked(!card.isBlocked());
            dataStore.registerCard(card);
            return card.isBlocked();
        }
        return false;
    }

    /**
     * Simulates Automated Fare Collection (AFC) Gate Tap-In
     */
    public void processTapIn(String cardId, Station station)
            throws CardBlockedException, MinimumBalanceException {
        Card card = dataStore.getCard(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Card [" + cardId + "] not found.");
        }
        if (card.isBlocked()) {
            throw new CardBlockedException(cardId);
        }
        if (card.isInTransit()) {
            throw new IllegalStateException("Card is ALREADY tapped in at " + card.getTapInStation().getName()
                    + "! Complete journey or visit customer care.");
        }

        double minBalance = card.getMinimumBalanceRequired();
        if (card.getBalance() < minBalance) {
            throw new MinimumBalanceException(minBalance, card.getBalance());
        }

        card.tapIn(station);
        dataStore.registerCard(card);
    }

    public static class JourneyResult {
        public final Card card;
        public final Station origin;
        public final Station destination;
        public final double distanceKm;
        public final double baseFare;
        public final double discount;
        public final double finalFare;
        public final double remainingBalance;

        public JourneyResult(Card card, Station origin, Station destination, double distanceKm,
                             double baseFare, double discount, double finalFare, double remainingBalance) {
            this.card = card;
            this.origin = origin;
            this.destination = destination;
            this.distanceKm = distanceKm;
            this.baseFare = baseFare;
            this.discount = discount;
            this.finalFare = finalFare;
            this.remainingBalance = remainingBalance;
        }
    }

    /**
     * Simulates AFC Gate Tap-Out, computes fare, and deducts from card
     */
    public JourneyResult processTapOut(String cardId, Station exitStation, boolean isPeakHour)
            throws CardBlockedException, InvalidStationException, InsufficientBalanceException {
        Card card = dataStore.getCard(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Card [" + cardId + "] not found.");
        }
        if (card.isBlocked()) {
            throw new CardBlockedException(cardId);
        }
        if (!card.isInTransit()) {
            throw new IllegalStateException("Card [" + cardId + "] is NOT tapped in anywhere! Tap in at entry first.");
        }

        Station origin = card.getTapInStation();
        if (origin.equals(exitStation)) {
            throw new InvalidStationException("Same station tap-out (" + origin.getName() + ")! Minimum same-station fee applies.");
        }
        if (!origin.getLineCode().equals(exitStation.getLineCode())) {
            throw new InvalidStationException("Cannot select stations from two different lines (" 
                    + origin.getLineName() + " and " + exitStation.getLineName() + ")! Exit station must be on the same Metro line as entry.");
        }

        double distance = fareCalculator.calculateDistance(origin, exitStation);
        double baseFare = fareCalculator.calculateBaseFare(distance);
        if (isPeakHour) {
            baseFare += FareCalculator.PEAK_HOUR_SURCHARGE;
        }
        double discount = card.calculateDiscount(baseFare);
        double finalFare = Math.max(5.0, baseFare - discount);

        // Deduct fare (throws InsufficientBalanceException if deficit)
        card.deduct(finalFare);
        card.tapOut(); // Reset transit status
        dataStore.registerCard(card); // Commit state to SQLite

        String txnId = generateTxnId();
        dataStore.logTransaction(new Transaction(
                txnId, card.getCardId(), Transaction.Type.CARD_JOURNEY_DEDUCTION, finalFare,
                origin.getName(), exitStation.getName(), card.getBalance(), "SUCCESS"
        ));

        return new JourneyResult(card, origin, exitStation, distance, baseFare, discount, finalFare, card.getBalance());
    }

    /**
     * Books a single journey paper/QR token ticket
     */
    public Ticket bookSingleJourneyToken(Station src, Station dest, int passengers, boolean isReturn, boolean isPeakHour)
            throws InvalidStationException {
        if (src == null || dest == null) {
            throw new InvalidStationException("Please select valid origin and destination stations.");
        }
        double distance = fareCalculator.calculateDistance(src, dest);
        double totalFare = fareCalculator.calculateTokenFare(src, dest, passengers, isReturn, isPeakHour);
        double unitFare = totalFare / (passengers * (isReturn ? 2 : 1));

        String ticketId = generateTicketId();
        String ticketType = isReturn ? "QR Return Journey Token" : "QR Single Journey Token";

        Ticket ticket = new Ticket(ticketId, src, dest, distance, unitFare, passengers, isReturn, totalFare, ticketType);
        com.mumbaimetro.data.MetroDatabaseManager.saveTicket(ticket); // Relational DB commit

        String txnId = generateTxnId();
        dataStore.logTransaction(new Transaction(
                txnId, ticketId, Transaction.Type.TOKEN_PURCHASE, totalFare,
                src.getName(), dest.getName(), 0.0, "SUCCESS"
        ));

        return ticket;
    }
}
