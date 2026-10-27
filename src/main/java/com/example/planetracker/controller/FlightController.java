package com.example.planetracker.controller;

import com.example.planetracker.model.Flight;
import com.example.planetracker.model.PositionHistory;
import com.example.planetracker.service.FlightTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class FlightController {

    private final FlightTrackingService service;

    public FlightController(FlightTrackingService service) {
        this.service = service;
    }

    /** All tracked flights. Optional filters: ?q=callsign/country/icao24 and ?airborne=true */
    @GetMapping("/flights")
    public List<Flight> flights(@RequestParam(required = false) String q,
                                @RequestParam(required = false) Boolean airborne) {
        return service.getAll().stream()
                .filter(f -> airborne == null || f.onGround() != airborne)
                .filter(f -> q == null || q.isBlank() || matches(f, q.toLowerCase()))
                .toList();
    }

    @GetMapping("/flights/{icao24}")
    public ResponseEntity<Flight> flight(@PathVariable String icao24) {
        return service.get(icao24).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** Recent positions of one aircraft, newest first. */
    @GetMapping("/flights/{icao24}/history")
    public List<PositionHistory> history(@PathVariable String icao24) {
        return service.history(icao24);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }

    private boolean matches(Flight f, String q) {
        return f.callsign().toLowerCase().contains(q)
                || f.icao24().toLowerCase().contains(q)
                || (f.originCountry() != null && f.originCountry().toLowerCase().contains(q));
    }
}