package com.smartpark.repository;

import com.smartpark.entity.parking.ParkingSession;
import com.smartpark.entity.parking.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    boolean existsByActivePlate(String activePlate);

    Optional<ParkingSession> findByActivePlate(String activePlate);

    /** "Before" is a strict < , matching "parked more than 15 minutes". */
    @Query("SELECT s FROM ParkingSession s " +
            "WHERE s.status = :status AND s.checkInAt < :cutoff")
    List<ParkingSession> findExpiredSessions(@Param("status") SessionStatus status,
                                             @Param("cutoff") Instant cutoff);

    @Query("SELECT s FROM ParkingSession s JOIN FETCH s.vehicle " +
            "WHERE s.lot.lotId = :lotId AND s.status = :status ORDER BY s.checkInAt")
    List<ParkingSession> findByLotAndStatusWithVehicle(@Param("lotId") String lotId,
                                                       @Param("status") SessionStatus status);

    /** Only the caller that flips ACTIVE gets 1 row; everyone else gets 0. */
    @Modifying
    @Query("UPDATE ParkingSession s SET s.status = :newStatus, s.activePlate = null, " +
            "s.checkOutAt = :checkOutAt, s.cost = :cost " +
            "WHERE s.id = :id AND s.status = :active")
    int close(@Param("id") Long id,
              @Param("newStatus") SessionStatus newStatus,
              @Param("checkOutAt") Instant checkOutAt,
              @Param("cost") BigDecimal cost,
              @Param("active") SessionStatus active);
}