package com.smartpark.controller;

import com.smartpark.dto.vehicle.VehicleRequest;
import com.smartpark.dto.vehicle.VehicleResponse;
import com.smartpark.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse register(@Valid @RequestBody VehicleRequest req) {
        return vehicleService.register(req);
    }

    @GetMapping
    public List<VehicleResponse> list() {
        return vehicleService.findAll();
    }
}