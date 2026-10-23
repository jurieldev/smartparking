package com.smartpark.dto.vehicle;

import com.smartpark.entity.vehicle.Vehicle;
import com.smartpark.entity.vehicle.VehicleType;

public record VehicleResponse(String licensePlate, VehicleType type, String ownerName) {

    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(v.getLicensePlate(), v.getType(), v.getOwnerName());
    }
}
