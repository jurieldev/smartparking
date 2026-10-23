package com.smartpark.service;

import com.smartpark.common.error.ConflictException;
import com.smartpark.dto.lot.LotRequest;
import com.smartpark.dto.lot.LotResponse;
import com.smartpark.entity.lot.ParkingLot;
import com.smartpark.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import com.smartpark.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository lotRepo;

    @Transactional
    public LotResponse register(LotRequest req) {
        String lotId = req.lotId().trim();
        if (lotRepo.existsById(lotId)) {
            throw new ConflictException("LOT_EXISTS", "Parking lot " + lotId + " already exists.");
        }
        ParkingLot lot = new ParkingLot();
        lot.setLotId(lotId);
        lot.setLocation(req.location().trim());
        lot.setCapacity(req.capacity());
        lot.setOccupiedSpaces(0);
        lot.setCostPerMinute(req.costPerMinute());
        return LotResponse.from(lotRepo.save(lot));
    }

    @Transactional(readOnly = true)
    public LotResponse get(String lotId) {
        return LotResponse.from(findOrThrow(lotId));
    }

    @Transactional(readOnly = true)
    public List<LotResponse> findAll() {
        return lotRepo.findAll().stream().map(LotResponse::from).toList();
    }

    ParkingLot findOrThrow(String lotId) {
        try {
            return lotRepo.findById(lotId)
                    .orElseThrow(() -> new NotFoundException("Parking lot " + lotId + " not found."));
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
