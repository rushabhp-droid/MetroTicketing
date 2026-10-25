package com.mumbaimetro.service;

import com.mumbaimetro.exceptions.InvalidStationException;
import com.mumbaimetro.model.Card;
import com.mumbaimetro.model.Station;

/**
 * Service class handling distance and dynamic fare computation
 * Demonstrates: Method Overloading (Compile-Time Polymorphism), Business Rules
 */
public class FareCalculator {

    public static final double PEAK_HOUR_SURCHARGE = 5.0; // ₹5 surcharge during peak rush

    /**
     * Calculates distance in kilometers between two stations
     */
    public double calculateDistance(Station src, Station dest) throws InvalidStationException {
        if (src == null || dest == null) {
            throw new InvalidStationException("Source and Destination stations must not be null.");
        }
        if (src.equals(dest)) {
            throw new InvalidStationException("Origin and Destination cannot be the same station (" + src.getName() + ")!");
        }
        if (!src.getLineCode().equals(dest.getLineCode())) {
            throw new InvalidStationException("Cannot select stations from two different lines (" 
                    + src.getLineName() + " and " + dest.getLineName() + "). Both stations must be on the same Metro line.");
        }
        return Math.abs(dest.getDistanceKm() - src.getDistanceKm());
    }

    private double calculateInterLineDistance(Station src, Station dest) {
        String l1 = src.getLineCode();
        String l2 = dest.getLineCode();

        // Line 1 <-> Line 2A (Interchange at DN Nagar / Andheri West)
        if ((l1.equals("LINE_1") && l2.equals("LINE_2A")) || (l1.equals("LINE_2A") && l2.equals("LINE_1"))) {
            Station s1 = l1.equals("LINE_1") ? src : dest;
            Station s2 = l1.equals("LINE_1") ? dest : src;
            return Math.max(1.0, Math.abs(s1.getDistanceKm() - 1.1) + Math.abs(s2.getDistanceKm() - 18.6));
        }

        // Line 1 <-> Line 7 (Interchange at WEH / Gundavali)
        if ((l1.equals("LINE_1") && l2.equals("LINE_7")) || (l1.equals("LINE_7") && l2.equals("LINE_1"))) {
            Station s1 = l1.equals("LINE_1") ? src : dest;
            Station s2 = l1.equals("LINE_1") ? dest : src;
            return Math.max(1.0, Math.abs(s1.getDistanceKm() - 4.8) + Math.abs(s2.getDistanceKm() - 16.5));
        }

        // Line 1 <-> Line 3 (Interchange at Marol Naka)
        if ((l1.equals("LINE_1") && l2.equals("LINE_3")) || (l1.equals("LINE_3") && l2.equals("LINE_1"))) {
            Station s1 = l1.equals("LINE_1") ? src : dest;
            Station s2 = l1.equals("LINE_1") ? dest : src;
            return Math.max(1.0, Math.abs(s1.getDistanceKm() - 8.3) + Math.abs(s2.getDistanceKm() - 4.2));
        }

        // Line 2A <-> Line 7 (Interchange at Dahisar East)
        if ((l1.equals("LINE_2A") && l2.equals("LINE_7")) || (l1.equals("LINE_7") && l2.equals("LINE_2A"))) {
            return Math.max(1.0, src.getDistanceKm() + dest.getDistanceKm());
        }

        // Line 7 <-> Line 9 (Interchange at Dahisar East)
        if ((l1.equals("LINE_7") && l2.equals("LINE_9")) || (l1.equals("LINE_9") && l2.equals("LINE_7"))) {
            return Math.max(1.0, src.getDistanceKm() + dest.getDistanceKm());
        }

        // Line 2A <-> Line 9 (Interchange at Dahisar East)
        if ((l1.equals("LINE_2A") && l2.equals("LINE_9")) || (l1.equals("LINE_9") && l2.equals("LINE_2A"))) {
            return Math.max(1.0, src.getDistanceKm() + dest.getDistanceKm());
        }

        // Line 3 <-> Line 7 (Interchange at Aarey JVLR)
        if ((l1.equals("LINE_3") && l2.equals("LINE_7")) || (l1.equals("LINE_7") && l2.equals("LINE_3"))) {
            Station s3 = l1.equals("LINE_3") ? src : dest;
            Station s7 = l1.equals("LINE_7") ? src : dest;
            return Math.max(1.0, s3.getDistanceKm() + Math.abs(s7.getDistanceKm() - 11.5));
        }

        // General multi-line transit estimate
        double est = Math.abs(dest.getDistanceKm() - src.getDistanceKm()) + 4.5;
        return Math.max(2.5, est);
    }

    /**
     * Method Overloading: Computes base fare from distance based on Mumbai Metro fare slabs
     * @param distanceKm journey distance in km
     * @return base fare in rupees
     */
    public double calculateBaseFare(double distanceKm) {
        if (distanceKm <= 3.0) {
            return 10.0;
        } else if (distanceKm <= 6.0) {
            return 20.0;
        } else if (distanceKm <= 9.0) {
            return 30.0;
        } else if (distanceKm <= 12.0) {
            return 40.0;
        } else if (distanceKm <= 15.0) {
            return 50.0;
        } else if (distanceKm <= 18.0) {
            return 60.0;
        } else if (distanceKm <= 21.0) {
            return 70.0;
        } else if (distanceKm <= 24.0) {
            return 80.0;
        } else if (distanceKm <= 27.0) {
            return 90.0;
        } else if (distanceKm <= 30.0) {
            return 100.0;
        } else {
            return 110.0;
        }
    }

    /**
     * Method Overloading: Computes base fare directly from Station objects
     */
    public double calculateBaseFare(Station src, Station dest) throws InvalidStationException {
        double dist = calculateDistance(src, dest);
        return calculateBaseFare(dist);
    }

    /**
     * Method Overloading: Calculates token purchase fare with passengers and return trip
     */
    public double calculateTokenFare(Station src, Station dest, int passengers, boolean isReturn, boolean isPeakHour)
            throws InvalidStationException {
        if (passengers <= 0) {
            throw new IllegalArgumentException("Passenger count must be at least 1");
        }
        double base = calculateBaseFare(src, dest);
        if (isPeakHour) {
            base += PEAK_HOUR_SURCHARGE;
        }
        double totalOneWay = base * passengers;
        return isReturn ? (totalOneWay * 1.90) : totalOneWay; // 10% discount on round-trip token
    }

    /**
     * Method Overloading: Calculates final smart card fare utilizing polymorphic card discount
     */
    public double calculateCardFare(Station src, Station dest, Card card, boolean isPeakHour)
            throws InvalidStationException {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null");
        }
        double base = calculateBaseFare(src, dest);
        if (isPeakHour) {
            base += PEAK_HOUR_SURCHARGE;
        }

        // Polymorphic call: StudentCard gives 20% discount, Regular gives 5% (if > 30), etc.
        double discount = card.calculateDiscount(base);
        double finalFare = base - discount;
        return Math.max(5.0, finalFare); // Minimum fare threshold of ₹5
    }
}
