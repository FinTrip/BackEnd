package org.example.backend.repository;

import org.example.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Integer> {
    Optional<Location> findByName(String name);
}
