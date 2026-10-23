package com.smartpark.service;

import com.smartpark.common.TextUtils;
import com.smartpark.common.error.ConflictException;
import com.smartpark.dto.parking.*;
import com.smartpark.entity.lot.ParkingLot;
import com.smartpark.entity.parking.ParkingSession;
import com.smartpark.entity.parking.SessionStatus;
import com.smartpark.entity.vehicle.Vehicle;
import com.smartpark.repository.ParkingLotRepository;
import com.smartpark.repository.ParkingSessionRepository;
import com.smartpark.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.smartpark.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingService {

    private final ParkingLotRepository lotRepo;
    private final VehicleRepository vehicleRepo;
    private final ParkingSessionRepository sessionRepo;
    private final Clock clock;
    private final ParkingFeeCalculator parkingFeeCalculator;

    @Transactional
    public CheckInResponse checkIn(CheckInRequest req) throws NotFoundException {
        String plate = TextUtils.collapseSpaces(req.licensePlate());
        String lotId = req.lotId().trim();

        Vehicle vehicle = vehicleRepo.findById(plate)
                .orElseThrow(() -> new NotFoundException("Vehicle " + plate + " is not registered."));
        ParkingLot lot = lotRepo.findById(lotId)
                .orElseThrow(() -> new NotFoundException("Parking lot " + lotId + " not found."));

        // Friendly pre-check. The unique active_plate constraint is the real guarantee.
        if (sessionRepo.existsByActivePlate(plate)) {
            throw new ConflictException("ALREADY_PARKED", "Vehicle " + plate + " is already parked.");
        }
        // Atomic capacity check + increment. 0 rows = full.
        if (lotRepo.tryOccupy(lotId) == 0) {
            throw new ConflictException("LOT_FULL", "Parking lot " + lotId + " is full.");
        }

        ParkingSession s = new ParkingSession();
        s.setVehicle(vehicle);
        s.setLot(lot);
        s.setCheckInAt(clock.instant());
        s.setStatus(SessionStatus.ACTIVE);
        s.setActivePlate(plate);
        // Flush now so a unique violation surfaces here and rolls back the +1 above.
        sessionRepo.saveAndFlush(s);

        return new CheckInResponse(s.getId(), plate, lotId, s.getCheckInAt());
    }

    @Transactional
    public CheckOutResponse checkOut(CheckOutRequest req) {
        String plate = TextUtils.collapseSpaces(req.licensePlate());
        ParkingSession s = sessionRepo.findByActivePlate(plate)
                .orElseThrow(() -> notParked(plate));

        Instant now = clock.instant();
        Instant billingEnd = parkingFeeCalculator.billingEnd(s.getCheckInAt(), now);

        return tryClose(s, billingEnd, SessionStatus.COMPLETED).orElseThrow(() -> notParked(plate));
    }

    /** Called by the scheduler, one transaction per session. Returns false if someone else closed it first. */
    @Transactional
    public boolean autoRemove(Long sessionId) {
        return sessionRepo.findById(sessionId)
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .flatMap(s -> tryClose(s,
                        parkingFeeCalculator.autoRemovalEnd(s.getCheckInAt()),
                        SessionStatus.AUTO_REMOVED))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public List<ParkedVehicleResponse> parkedVehicles(String lotId) throws NotFoundException {
        if (!lotRepo.existsById(lotId)) {
            throw new NotFoundException("Parking lot " + lotId + " not found.");
        }
        return sessionRepo.findByLotAndStatusWithVehicle(lotId, SessionStatus.ACTIVE).stream()
                .map(ParkedVehicleResponse::from)
                .toList();
    }

    private Optional<CheckOutResponse> tryClose(ParkingSession s, Instant end, SessionStatus status) {
        ParkingLot lot = s.getLot();
        long billedMinutes = parkingFeeCalculator.billedMinutes(s.getCheckInAt(), end);
        BigDecimal cost = parkingFeeCalculator.cost(lot.getCostPerMinute(), billedMinutes);

        int updated = sessionRepo.close(s.getId(), status, end, cost, SessionStatus.ACTIVE);
        if (updated == 0) {
            return Optional.empty();   // lost the race: already closed by check-out or the scheduler
        }
        lotRepo.release(lot.getLotId());

        return Optional.of(new CheckOutResponse(s.getId(), s.getVehicle().getLicensePlate(),
                lot.getLotId(), s.getCheckInAt(), end, billedMinutes, lot.getCostPerMinute(), cost, status));
    }

    private ConflictException notParked(String plate) {
        return new ConflictException("NOT_PARKED", "Vehicle " + plate + " is not currently parked.");
    }
}
