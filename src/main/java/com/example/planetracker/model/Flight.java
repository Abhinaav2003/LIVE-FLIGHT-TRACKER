package com.example.planetracker.model;

/**
 * A single aircraft state at a point in time.
 *
 * @param icao24       unique 24-bit transponder address (hex)
 * @param callsign     flight callsign, e.g. AI101
 * @param altitude     metres
 * @param velocity     ground speed in m/s
 * @param heading      degrees clockwise from north
 * @param verticalRate m/s (positive = climbing)
 * @param lastUpdate   epoch seconds
 */
public record Flight(
        String icao24,
        String callsign,
        String originCountry,
        double latitude,
        double longitude,
        Double altitude,
        Double velocity,
        Double heading,
        Double verticalRate,
        boolean onGround,
        long lastUpdate) {
}
