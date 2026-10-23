package com.smartpark.entity.parking;


import com.smartpark.entity.lot.ParkingLot;
import com.smartpark.entity.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "parking_session")
@Getter
@Setter
@NoArgsConstructor
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "license_plate")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id")
    private ParkingLot lot;

    @Column(name = "check_in_at", nullable = false)
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    /** Plate while ACTIVE, null once closed. Unique: one active session per vehicle. */
    @Column(name = "active_plate", unique = true, length = 20)
    private String activePlate;

    /** Ceil to whole minutes, minimum 1. */
    public static long billedMinutes(Instant from, Instant to) {
        long seconds = Duration.between(from, to).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    public Long getBilledMinutes() {
        return checkOutAt == null ? null : billedMinutes(checkInAt, checkOutAt);
    }
}

