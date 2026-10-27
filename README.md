<h1 align="center">LIVE PLANE TRACKING SYSTEM</h1>

<p align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,hibernate,postgres,maven" alt="Tech stack icons" />
</p>

A real-time aircraft tracking system. The backend polls a flight data source,
stores position history, and pushes live updates over WebSocket to a Leaflet
map in the browser.

## Features
- Live map with rotating plane icons, colour-coded by altitude
- Real-time push via WebSocket (STOMP + SockJS) on `/topic/flights`
- REST API for flights, a single flight, its position trail and statistics
- Two data providers, switchable in one line:
  - `simulated` (default): demo flights between Indian airports, works offline
  - `opensky`: real live data from the OpenSky Network
- Position history stored in H2 (auto-cleaned after 60 minutes)
- Search, flight details and flight trail in the UI

## Requirements
- JDK 17+
- Maven 3.8+

## Run
```bash
mvn spring-boot:run
```
Open http://localhost:8080

## Docker
```bash
docker build -t live-plane-tracker .
docker run -p 8080:10000 live-plane-tracker
```
Open http://localhost:8080

## Deploy on Render
1. Push this project to a GitHub repository.
2. In Render: **New > Blueprint**, pick the repo (it reads `render.yaml`).
   Or **New > Web Service**, choose the repo, and set **Runtime = Docker**.
3. Set the health check path to `/api/stats`.
4. Optional environment variables:

| Variable | Default | Meaning |
|---|---|---|
| `TRACKER_PROVIDER` | `simulated` | `simulated` or `opensky` |
| `TRACKER_POLL_INTERVAL_MS` | `5000` | Poll interval (use 10000+ for OpenSky) |
| `SIMULATION_PLANE_COUNT` | `25` | Number of demo planes |
| `OPENSKY_LAMIN/LOMIN/LAMAX/LOMAX` | India | Map area for OpenSky |
| `OPENSKY_USERNAME/PASSWORD` | empty | Optional OpenSky credentials |
| `H2_CONSOLE` | `false` | Enable the H2 web console |

Notes: the free Render plan sleeps after inactivity, so the first request can
take about a minute, and position history is in memory and resets on restart.
The first build may take a few minutes because Maven downloads dependencies.

## Use real flight data
In `src/main/resources/application.properties`:
```properties
tracker.provider=opensky
tracker.poll-interval-ms=10000
```
Change `tracker.opensky.lamin/lomin/lamax/lomax` to set the area on the map.
Anonymous OpenSky access is rate limited, so keep the interval at 10 s or more
and use a small bounding box. For higher limits, check OpenSky's current
authentication options and set `tracker.opensky.username/password`.

## REST API
| Endpoint | Description |
|---|---|
| `GET /api/flights?q=AI1&airborne=true` | All tracked flights (optional filters) |
| `GET /api/flights/{icao24}` | One flight |
| `GET /api/flights/{icao24}/history` | Last 200 recorded positions, newest first |
| `GET /api/stats` | Totals, airborne count, average altitude |

WebSocket: connect to `/ws` (SockJS), subscribe to `/topic/flights`.
H2 console (run with `H2_CONSOLE=true`): http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:planes`, user `sa`).

## Project structure
```
src/main/java/com/example/planetracker
├── PlaneTrackerApplication.java
├── config/WebSocketConfig.java
├── controller/FlightController.java
├── model/            Flight (record), PositionHistory (JPA entity)
├── provider/         FlightProvider, SimulatedFlightProvider, OpenSkyFlightProvider
├── repository/       PositionHistoryRepository
└── service/          FlightTrackingService (polling, broadcast, history)
src/main/resources
├── application.properties
└── static/index.html  (Leaflet map UI)
```

## Ideas to extend
- Persist to PostgreSQL instead of H2
- Add airport / route lookup and flight search by route
- Add Spring Security and user watchlists with alerts


---

## Authors

This project was designed and developed by:

- **Abhinav Tiwari**