package com.smartpark.repository;

import com.smartpark.entity.lot.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, String> {

    /** Atomic: increments only while there is space. Returns 0 when the lot is full. */
    @Modifying
    @Query("UPDATE ParkingLot l SET l.occupiedSpaces = l.occupiedSpaces + 1 " +
            "WHERE l.lotId = :lotId AND l.occupiedSpaces < l.capacity")
    int tryOccupy(@Param("lotId") String lotId);

    /** Atomic: never goes below zero. */
    @Modifying
    @Query("UPDATE ParkingLot l SET l.occupiedSpaces = l.occupiedSpaces - 1 " +
            "WHERE l.lotId = :lotId AND l.occupiedSpaces > 0")
    int release(@Param("lotId") String lotId);
}
