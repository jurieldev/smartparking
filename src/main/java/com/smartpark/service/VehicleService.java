package com.smartpark.service;

import com.smartpark.common.error.ConflictException;
import com.smartpark.common.TextUtils;
import com.smartpark.dto.vehicle.VehicleRequest;
import com.smartpark.dto.vehicle.VehicleResponse;
import com.smartpark.entity.vehicle.Vehicle;
import com.smartpark.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepo;

    @Transactional
    public VehicleResponse register(VehicleRequest req) {
        String plate = TextUtils.collapseSpaces(req.licensePlate());
        if (vehicleRepo.existsById(plate)) {
            throw new ConflictException("VEHICLE_EXISTS", "Vehicle " + plate + " is already registered.");
        }
        Vehicle v = new Vehicle();
        v.setLicensePlate(plate);
        v.setType(req.type());
        v.setOwnerName(TextUtils.collapseSpaces(req.ownerName()));
        return VehicleResponse.from(vehicleRepo.save(v));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findAll() {
        return vehicleRepo.findAll().stream().map(VehicleResponse::from).toList();
    }
}
