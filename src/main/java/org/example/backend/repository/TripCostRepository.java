package org.example.backend.repository;

import org.example.backend.entity.TripCost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripCostRepository extends JpaRepository<TripCost, Integer> {
}
