package com.example.planetracker;

import com.example.planetracker.model.Flight;
import com.example.planetracker.provider.SimulatedFlightProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedFlightProviderTest {

    @Test
    void returnsConfiguredNumberOfValidFlights() {
        SimulatedFlightProvider provider = new SimulatedFlightProvider(10, 20);
        List<Flight> flights = provider.fetch();

        assertEquals(10, flights.size());
        for (Flight f : flights) {
            assertTrue(f.latitude() > 0 && f.latitude() < 40);
            assertTrue(f.longitude() > 60 && f.longitude() < 100);
            assertTrue(f.heading() >= 0 && f.heading() < 360);
            assertFalse(f.callsign().isBlank());
        }
    }
}
