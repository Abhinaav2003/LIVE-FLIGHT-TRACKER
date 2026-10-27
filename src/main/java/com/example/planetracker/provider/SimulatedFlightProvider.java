package com.example.planetracker.provider;

import com.example.planetracker.model.Flight;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Offline demo data: planes flying between major Indian airports.
 * Default provider (tracker.provider=simulated).
 */
@Component
@ConditionalOnProperty(name = "tracker.provider", havingValue = "simulated", matchIfMissing = true)
public class SimulatedFlightProvider implements FlightProvider {

    private record Airport(String code, double lat, double lon) { }

    private static final List<Airport> AIRPORTS = List.of(
            new Airport("DEL", 28.5562, 77.1000),
            new Airport("BOM", 19.0896, 72.8656),
            new Airport("BLR", 13.1986, 77.7066),
            new Airport("MAA", 12.9941, 80.1709),
            new Airport("CCU", 22.6547, 88.4467),
            new Airport("HYD", 17.2403, 78.4294),
            new Airport("GOI", 15.3808, 73.8314),
            new Airport("JAI", 26.8242, 75.8122),
            new Airport("AMD", 23.0772, 72.6347),
            new Airport("COK", 10.1520, 76.4019),
            new Airport("LKO", 26.7606, 80.8893),
            new Airport("GAU", 26.1061, 91.5859));

    private static final String[] AIRLINES = {"AI", "6E", "UK", "SG", "IX", "QP"};
    private static final double CRUISE_ALTITUDE = 11000;

    private static class SimPlane {
        String icao24;
        String callsign;
        Airport from;
        Airport to;
        double progress;
        double speed; // m/s
        double lat, lon, heading, altitude, verticalRate;
    }

    private final List<SimPlane> planes = new ArrayList<>();
    private final Random random = new Random();
    private final double timeScale;
    private long lastTick = System.currentTimeMillis();

    public SimulatedFlightProvider(
            @Value("${tracker.simulation.plane-count:25}") int planeCount,
            @Value("${tracker.simulation.time-scale:20}") double timeScale) {
        this.timeScale = timeScale;
        for (int i = 0; i < planeCount; i++) {
            SimPlane p = new SimPlane();
            p.icao24 = String.format("8%05x", random.nextInt(0xFFFFF));
            p.callsign = AIRLINES[random.nextInt(AIRLINES.length)] + (100 + random.nextInt(900));
            p.from = AIRPORTS.get(random.nextInt(AIRPORTS.size()));
            p.to = otherAirport(p.from);
            p.progress = random.nextDouble();
            p.speed = 200 + random.nextDouble() * 50;
            update(p);
            planes.add(p);
        }
    }

    @Override
    public synchronized List<Flight> fetch() {
        long now = System.currentTimeMillis();
        double dt = (now - lastTick) / 1000.0 * timeScale;
        lastTick = now;

        List<Flight> out = new ArrayList<>(planes.size());
        for (SimPlane p : planes) {
            double distance = haversine(p.from.lat, p.from.lon, p.to.lat, p.to.lon);
            p.progress += p.speed * dt / distance;
            if (p.progress >= 1) { // landed -> next route
                p.from = p.to;
                p.to = otherAirport(p.from);
                p.progress = 0;
            }
            update(p);
            boolean onGround = p.altitude < 50;
            out.add(new Flight(p.icao24, p.callsign, "India", p.lat, p.lon,
                    p.altitude, onGround ? 0.0 : p.speed, p.heading, p.verticalRate,
                    onGround, Instant.now().getEpochSecond()));
        }
        return out;
    }

    private void update(SimPlane p) {
        p.lat = p.from.lat + (p.to.lat - p.from.lat) * p.progress;
        p.lon = p.from.lon + (p.to.lon - p.from.lon) * p.progress;
        p.heading = bearing(p.lat, p.lon, p.to.lat, p.to.lon);
        double pr = p.progress;
        if (pr < 0.15) {
            p.altitude = CRUISE_ALTITUDE * (pr / 0.15);
            p.verticalRate = 10;
        } else if (pr > 0.85) {
            p.altitude = CRUISE_ALTITUDE * ((1 - pr) / 0.15);
            p.verticalRate = -10;
        } else {
            p.altitude = CRUISE_ALTITUDE;
            p.verticalRate = 0;
        }
    }

    private Airport otherAirport(Airport not) {
        Airport a;
        do {
            a = AIRPORTS.get(random.nextInt(AIRPORTS.size()));
        } while (a == not);
        return a;
    }

    static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000;
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = p2 - p1, dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
