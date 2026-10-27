package com.example.planetracker.provider;

import com.example.planetracker.model.Flight;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Real flight data from https://opensky-network.org (free REST API).
 * Enabled with tracker.provider=opensky.
 */
@Component
@ConditionalOnProperty(name = "tracker.provider", havingValue = "opensky")
public class OpenSkyFlightProvider implements FlightProvider {

    private final RestClient client;
    private final double lamin, lomin, lamax, lomax;

    public OpenSkyFlightProvider(
            @Value("${tracker.opensky.base-url}") String baseUrl,
            @Value("${tracker.opensky.lamin}") double lamin,
            @Value("${tracker.opensky.lomin}") double lomin,
            @Value("${tracker.opensky.lamax}") double lamax,
            @Value("${tracker.opensky.lomax}") double lomax,
            @Value("${tracker.opensky.username:}") String username,
            @Value("${tracker.opensky.password:}") String password) {
        this.lamin = lamin;
        this.lomin = lomin;
        this.lamax = lamax;
        this.lomax = lomax;
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (!username.isBlank()) {
            builder.defaultHeaders(h -> h.setBasicAuth(username, password));
        }
        this.client = builder.build();
    }

    @Override
    public List<Flight> fetch() {
        JsonNode root = client.get()
                .uri("/states/all?lamin={a}&lomin={b}&lamax={c}&lomax={d}", lamin, lomin, lamax, lomax)
                .retrieve()
                .body(JsonNode.class);

        List<Flight> result = new ArrayList<>();
        if (root == null || !root.path("states").isArray()) {
            return result;
        }
        for (JsonNode s : root.get("states")) {
            if (s.get(5).isNull() || s.get(6).isNull()) {
                continue; // no position fix
            }
            Double altitude = num(s, 7) != null ? num(s, 7) : num(s, 13);
            String callsign = s.get(1).isNull() ? "" : s.get(1).asText().trim();
            result.add(new Flight(
                    s.get(0).asText(),
                    callsign.isEmpty() ? s.get(0).asText().toUpperCase() : callsign,
                    s.get(2).asText(),
                    s.get(6).asDouble(),
                    s.get(5).asDouble(),
                    altitude,
                    num(s, 9),
                    num(s, 10),
                    num(s, 11),
                    s.get(8).asBoolean(),
                    s.get(4).asLong()));
        }
        return result;
    }

    private static Double num(JsonNode node, int index) {
        JsonNode v = node.get(index);
        return (v == null || v.isNull()) ? null : v.asDouble();
    }
}
