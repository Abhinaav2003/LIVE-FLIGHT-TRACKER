package com.example.planetracker.repository;

import com.example.planetracker.model.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findTop200ByIcao24OrderByRecordedAtDesc(String icao24);

    void deleteByRecordedAtBefore(Instant cutoff);
}
