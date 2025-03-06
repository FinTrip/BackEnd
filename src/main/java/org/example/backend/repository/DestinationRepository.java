package org.example.backend.repository;

import org.example.backend.entity.Destination;
import org.example.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Integer> {
    List<Destination> findByLocationAndAvgCostLessThan(Location location, BigDecimal maxCost);
    Optional<Destination> findByNameAndLocation(String name, Location location);
}
