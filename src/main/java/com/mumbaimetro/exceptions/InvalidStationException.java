package com.mumbaimetro.exceptions;

/**
 * Custom Exception thrown when invalid stations are chosen
 * (e.g. Origin and Destination are identical or station doesn't exist)
 */
public class InvalidStationException extends Exception {
    public InvalidStationException(String message) {
        super(message);
    }
}
