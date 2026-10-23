package com.smartpark.controller;

import com.smartpark.dto.parking.CheckInRequest;
import com.smartpark.dto.parking.CheckInResponse;
import com.smartpark.dto.parking.CheckOutRequest;
import com.smartpark.dto.parking.CheckOutResponse;
import com.smartpark.service.ParkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.smartpark.common.error.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInResponse checkIn(@Valid @RequestBody CheckInRequest req) throws NotFoundException {
        return parkingService.checkIn(req);
    }

    @PostMapping("/check-out")
    public CheckOutResponse checkOut(@Valid @RequestBody CheckOutRequest req) {
        return parkingService.checkOut(req);
    }
}