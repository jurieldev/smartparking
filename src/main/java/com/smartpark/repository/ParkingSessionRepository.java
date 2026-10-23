package com.smartpark.repository;

import com.smartpark.entity.parking.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, String> {}
