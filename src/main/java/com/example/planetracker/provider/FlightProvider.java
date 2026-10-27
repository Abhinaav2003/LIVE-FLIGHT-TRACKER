package com.example.planetracker.provider;

import com.example.planetracker.model.Flight;

import java.util.List;

/** Source of live flight data. Swap implementations with tracker.provider. */
public interface FlightProvider {
    List<Flight> fetch();
}
