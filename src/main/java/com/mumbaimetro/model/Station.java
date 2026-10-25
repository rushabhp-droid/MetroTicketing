package com.mumbaimetro.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a Mumbai Metro Station along a line
 * Demonstrates: Encapsulation, Immutability, Serializable, Comparable
 */
public class Station implements Serializable, Comparable<Station> {
    private static final long serialVersionUID = 2L;

    private final int stationId;
    private final String name;
    private final double distanceKm;
    private final String lineCode;
    private final String lineName;
    private final String interchange;

    public Station(int stationId, String name, double distanceKm, String lineCode, String lineName, String interchange) {
        this.stationId = stationId;
        this.name = name;
        this.distanceKm = distanceKm;
        this.lineCode = lineCode != null ? lineCode : "LINE_1";
        this.lineName = lineName != null ? lineName : "Line 1 (Blue)";
        this.interchange = interchange != null ? interchange : "None";
    }

    public Station(int stationId, String name, double distanceKm, String lineName, String interchange) {
        this(stationId, name, distanceKm, deriveLineCode(lineName), lineName, interchange);
    }

    private static String deriveLineCode(String lineName) {
        if (lineName == null) return "LINE_1";
        if (lineName.contains("2B")) return "LINE_2B";
        if (lineName.contains("2A") || lineName.toLowerCase().contains("yellow")) return "LINE_2A";
        if (lineName.contains("9")) return "LINE_9";
        if (lineName.contains("7") || lineName.toLowerCase().contains("red")) return "LINE_7";
        if (lineName.contains("3") || lineName.toLowerCase().contains("aqua")) return "LINE_3";
        if (lineName.contains("NMM") || lineName.toLowerCase().contains("navi mumbai")) return "NMM_1";
        return "LINE_1";
    }

    public int getStationId() {
        return stationId;
    }

    public String getName() {
        return name;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public String getLineCode() {
        return lineCode;
    }

    public String getLineName() {
        return lineName;
    }

    public String getInterchange() {
        return interchange;
    }

    @Override
    public int compareTo(Station other) {
        return Double.compare(this.distanceKm, other.distanceKm);
    }

    @Override
    public String toString() {
        if (!interchange.equals("None")) {
            return name + " [" + interchange + "]";
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return stationId == station.stationId && Objects.equals(name, station.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationId, name);
    }
}
