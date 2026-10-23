package com.smartpark.repository;

import com.smartpark.entity.lot.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, String> {}
