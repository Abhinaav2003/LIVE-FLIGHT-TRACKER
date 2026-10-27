package com.example.planetracker.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "position_history", indexes = {
        @Index(name = "idx_history_icao_time", columnList = "icao24, recordedAt")
})
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String icao24;
    private String callsign;
    private double latitude;
    private double longitude;
    private Double altitude;
    private Double velocity;
    private Double heading;
    private Instant recordedAt;

    protected PositionHistory() {
    }

    public PositionHistory(Flight f, Instant recordedAt) {
        this.icao24 = f.icao24();
        this.callsign = f.callsign();
        this.latitude = f.latitude();
        this.longitude = f.longitude();
        this.altitude = f.altitude();
        this.velocity = f.velocity();
        this.heading = f.heading();
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public String getIcao24() { return icao24; }
    public String getCallsign() { return callsign; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Double getAltitude() { return altitude; }
    public Double getVelocity() { return velocity; }
    public Double getHeading() { return heading; }
    public Instant getRecordedAt() { return recordedAt; }
}
