package com.smartpark.dto.parking;

import com.smartpark.entity.parking.ParkingSession;
import com.smartpark.entity.vehicle.Vehicle;
import com.smartpark.entity.vehicle.VehicleType;

import java.time.Instant;

public record ParkedVehicleResponse(Long sessionId, String licensePlate, VehicleType type,
                                    String ownerName, Instant checkInAt) {

    public static ParkedVehicleResponse from(ParkingSession s) {
        Vehicle v = s.getVehicle();
        return new ParkedVehicleResponse(s.getId(), v.getLicensePlate(), v.getType(),
                v.getOwnerName(), s.getCheckInAt());
    }
}