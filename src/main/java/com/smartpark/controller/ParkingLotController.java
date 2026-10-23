package com.smartpark.controller;

import com.smartpark.dto.lot.LotRequest;
import com.smartpark.dto.lot.LotResponse;
import com.smartpark.service.ParkingLotService;
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
}
