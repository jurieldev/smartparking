package com.smartpark.dto;

import com.smartpark.entity.lot.ParkingLot;

import java.math.BigDecimal;

public record LotResponse(String lotId, String location, int capacity, int occupiedSpaces,
                          int availableSpaces, BigDecimal costPerMinute) {

    public static LotResponse from(ParkingLot lot) {
        return new LotResponse(lot.getLotId(), lot.getLocation(), lot.getCapacity(),
                lot.getOccupiedSpaces(), lot.getAvailableSpaces(), lot.getCostPerMinute());
    }
}
