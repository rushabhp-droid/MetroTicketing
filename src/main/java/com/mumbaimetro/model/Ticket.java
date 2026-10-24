package com.mumbaimetro.model;

import com.mumbaimetro.interfaces.PrintableReceipt;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a Paper/QR Single Journey or Return Journey Metro Ticket
 * Demonstrates: Interface Implementation, Composition, Formatting
 */
public class Ticket implements PrintableReceipt, Serializable {
    private static final long serialVersionUID = 1L;

    private final String ticketId;
    private final Station source;
    private final Station destination;
    private final double distanceKm;
    private final double unitFare;
    private final int passengerCount;
    private final boolean isReturnTrip;
    private final double totalAmount;
    private final String bookingTime;
    private final String ticketType;

    public Ticket(String ticketId, Station source, Station destination, double distanceKm,
                  double unitFare, int passengerCount, boolean isReturnTrip, double totalAmount,
                  String ticketType) {
        this.ticketId = ticketId;
        this.source = source;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.unitFare = unitFare;
        this.passengerCount = passengerCount;
        this.isReturnTrip = isReturnTrip;
        this.totalAmount = totalAmount;
        this.bookingTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
        this.ticketType = ticketType != null ? ticketType : "Single Journey QR Token";
    }

    public String getTicketId() {
        return ticketId;
    }

    public Station getSource() {
        return source;
    }

    public Station getDestination() {
        return destination;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getUnitFare() {
        return unitFare;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public boolean isReturnTrip() {
        return isReturnTrip;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getBookingTime() {
        return bookingTime;
    }

    public String getTicketType() {
        return ticketType;
    }

    @Override
    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("        MUMBAI METRO ONE - TRAVEL TICKET           \n");
        sb.append("          (Versova - Andheri - Ghatkopar)          \n");
        sb.append("====================================================\n");
        sb.append(String.format(" Ticket ID    : %s\n", ticketId));
        sb.append(String.format(" Type         : %s\n", ticketType));
        sb.append(String.format(" From         : %s\n", source.getName()));
        sb.append(String.format(" To           : %s\n", destination.getName()));
        sb.append(String.format(" Distance     : %.2f km\n", distanceKm));
        sb.append(String.format(" Passengers   : %d\n", passengerCount));
        sb.append(String.format(" Trip Type    : %s\n", isReturnTrip ? "RETURN JOURNEY" : "SINGLE JOURNEY"));
        sb.append(String.format(" Rate / Person: ₹%.2f\n", unitFare));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format(" TOTAL FARE   : ₹%.2f  (Incl. GST)\n", totalAmount));
        sb.append(String.format(" Issued At    : %s\n", bookingTime));
        sb.append(" Validity     : Valid for 90 minutes from issuance\n");
        sb.append("----------------------------------------------------\n");
        sb.append(" [ QR CODE SIMULATION ]\n");
        sb.append("  +-----------------------------+\n");
        sb.append("  | [##] # #  ###  # #  # [##]  |\n");
        sb.append("  | [##] #  #  #   # #  # [##]  |\n");
        sb.append("  | #   #  " + ticketId.substring(Math.max(0, ticketId.length() - 8)) + "   #  #  # |\n");
        sb.append("  | [##]  # # # # # # #   [##]  |\n");
        sb.append("  +-----------------------------+\n");
        sb.append("       Scan at Gate to Enter / Exit         \n");
        sb.append("====================================================\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (₹%.2f)", ticketId, source.getName(), destination.getName(), totalAmount);
    }
}
