package org.example.backend.repository;

import org.example.backend.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Integer> {

}
