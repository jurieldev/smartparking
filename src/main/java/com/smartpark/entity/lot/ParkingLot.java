package com.smartpark.entity.lot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "parking_lot")
@Getter
@Setter
@NoArgsConstructor
public class ParkingLot {

    @Id
    @Column(name = "lot_id", length = 50)
    private String lotId;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "occupied_spaces", nullable = false)
    private int occupiedSpaces;

    @Column(name = "cost_per_minute", nullable = false, precision = 10, scale = 2)
    private BigDecimal costPerMinute;

    public int getAvailableSpaces() {
        return capacity - occupiedSpaces;
    }
}