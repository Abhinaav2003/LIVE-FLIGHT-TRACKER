package com.example.planetracker.service;

import com.example.planetracker.model.Flight;
import com.example.planetracker.model.PositionHistory;
import com.example.planetracker.provider.FlightProvider;
import com.example.planetracker.repository.PositionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls the configured FlightProvider, keeps the latest state of every
 * aircraft in memory, stores a position trail in the DB and pushes the
 * full snapshot to all WebSocket subscribers on /topic/flights.
 */
@Service
public class FlightTrackingService {

    private static final Logger log = LoggerFactory.getLogger(FlightTrackingService.class);

    private final FlightProvider provider;
    private final SimpMessagingTemplate messaging;
    private final PositionHistoryRepository historyRepo;

    private final Map<String, Flight> flights = new ConcurrentHashMap<>();
    private volatile Instant lastPoll;

    @Value("${tracker.history.retention-minutes:60}")
    private long retentionMinutes;

    @Value("${tracker.history.max-flights-per-poll:300}")
    private int maxHistoryPerPoll;

    public FlightTrackingService(FlightProvider provider,
                                 SimpMessagingTemplate messaging,
                                 PositionHistoryRepository historyRepo) {
        this.provider = provider;
        this.messaging = messaging;
        this.historyRepo = historyRepo;
    }

    @Scheduled(fixedDelayString = "${tracker.poll-interval-ms:5000}")
    public void poll() {
        List<Flight> fetched;
        try {
            fetched = provider.fetch();
        } catch (Exception e) {
            log.warn("Could not fetch flights: {}", e.getMessage());
            return;
        }

        Set<String> seen = new HashSet<>();
        for (Flight f : fetched) {
            flights.put(f.icao24(), f);
            seen.add(f.icao24());
        }
        flights.keySet().retainAll(seen);
        lastPoll = Instant.now();

        Instant now = Instant.now();
        List<PositionHistory> batch = fetched.stream()
                .filter(f -> !f.onGround())
                .limit(maxHistoryPerPoll)
                .map(f -> new PositionHistory(f, now))
                .toList();
        historyRepo.saveAll(batch);

        messaging.convertAndSend("/topic/flights", List.copyOf(flights.values()));
        log.debug("Tracking {} flights", flights.size());
    }

    @Scheduled(fixedRate = 600_000, initialDelay = 600_000)
    @Transactional
    public void cleanOldHistory() {
        historyRepo.deleteByRecordedAtBefore(Instant.now().minus(retentionMinutes, ChronoUnit.MINUTES));
    }

    public List<Flight> getAll() {
        return List.copyOf(flights.values());
    }

    public Optional<Flight> get(String icao24) {
        return Optional.ofNullable(flights.get(icao24.toLowerCase()));
    }

    public List<PositionHistory> history(String icao24) {
        return historyRepo.findTop200ByIcao24OrderByRecordedAtDesc(icao24.toLowerCase());
    }

    public Map<String, Object> stats() {
        Collection<Flight> all = flights.values();
        long airborne = all.stream().filter(f -> !f.onGround()).count();
        OptionalDouble avgAlt = all.stream().filter(f -> !f.onGround() && f.altitude() != null)
                .mapToDouble(Flight::altitude).average();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", all.size());
        m.put("airborne", airborne);
        m.put("onGround", all.size() - airborne);
        m.put("averageAltitudeMeters", avgAlt.isPresent() ? Math.round(avgAlt.getAsDouble()) : null);
        m.put("lastPoll", lastPoll);
        return m;
    }
}
