package com.smartpark.controller;

import com.smartpark.common.error.NotFoundException;
import com.smartpark.dto.lot.LotRequest;
import com.smartpark.dto.lot.LotResponse;
import com.smartpark.dto.parking.ParkedVehicleResponse;
import com.smartpark.service.ParkingLotService;
import com.smartpark.service.ParkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService lotService;
    private final ParkingService parkingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LotResponse register(@Valid @RequestBody LotRequest req) {
        return lotService.register(req);
    }

    @GetMapping
    public List<LotResponse> list() {
        return lotService.findAll();
    }

    @GetMapping("/{lotId}/occupancy")
    public LotResponse occupancy(@PathVariable String lotId) {
        return lotService.get(lotId);
    }



    @GetMapping("/{lotId}/vehicles")
    public List<ParkedVehicleResponse> parkedVehicles(@PathVariable String lotId) throws NotFoundException {
        return parkingService.parkedVehicles(lotId);
    }
}
