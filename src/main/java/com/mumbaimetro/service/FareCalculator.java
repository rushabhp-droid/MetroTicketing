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
        return Math.abs(dest.getDistanceKm() - src.getDistanceKm());
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
