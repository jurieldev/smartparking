package com.smartpark.service;

import com.smartpark.common.TextUtils;
import com.smartpark.common.error.ConflictException;
import com.smartpark.common.error.NotFoundException;
import com.smartpark.dto.parking.CheckInRequest;
import com.smartpark.dto.parking.CheckInResponse;
import com.smartpark.dto.parking.CheckOutRequest;
import com.smartpark.dto.parking.CheckOutResponse;
import com.smartpark.entity.lot.ParkingLot;
import com.smartpark.entity.parking.ParkingSession;
import com.smartpark.entity.parking.SessionStatus;
import com.smartpark.entity.vehicle.Vehicle;
import com.smartpark.repository.ParkingLotRepository;
import com.smartpark.repository.ParkingSessionRepository;
import com.smartpark.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-24T08:00:00Z");
    private static final String PLATE = "ABC-1234";
    private static final String LOT_ID = "LOT-1";

    @Mock ParkingLotRepository lotRepo;
    @Mock VehicleRepository vehicleRepo;
    @Mock ParkingSessionRepository sessionRepo;
    @Mock Clock clock;

    ParkingService parkingService;

    @BeforeEach
    void setUp() {
        // real calculator, fake repositories and clock
        parkingService = new ParkingService(lotRepo, vehicleRepo, sessionRepo, clock,
                new ParkingFeeCalculator(15));
    }

    // ---------- check-in ----------

    @Test
    void checkInSucceeds() {
        when(vehicleRepo.findById(PLATE)).thenReturn(Optional.of(vehicle()));
        when(lotRepo.findById(LOT_ID)).thenReturn(Optional.of(lot("2.00")));
        when(sessionRepo.existsByActivePlate(PLATE)).thenReturn(false);
        when(lotRepo.tryOccupy(LOT_ID)).thenReturn(1);                 // 1 row updated = got a space
        when(clock.instant()).thenReturn(T0);

        CheckInResponse res = parkingService.checkIn(new CheckInRequest(PLATE, LOT_ID));

        assertEquals(PLATE, res.licensePlate());
        assertEquals(LOT_ID, res.lotId());
        assertEquals(T0, res.checkInAt());
        verify(sessionRepo).saveAndFlush(any(ParkingSession.class));   // session was saved
    }

    @Test
    void checkInAcceptsLowercasePlate() {
        String requestLoweredPlate = " abc-1234 ";
        String plate = TextUtils.collapseSpaces(requestLoweredPlate);
        when(vehicleRepo.findById(plate)).thenReturn(Optional.of(vehicle()));
        when(lotRepo.findById(LOT_ID)).thenReturn(Optional.of(lot("2.00")));
        when(lotRepo.tryOccupy(LOT_ID)).thenReturn(1);
        when(clock.instant()).thenReturn(T0);

        CheckInResponse res = parkingService.checkIn(new CheckInRequest(requestLoweredPlate, LOT_ID));

        assertEquals(PLATE, res.licensePlate());                       // normalized to ABC-1234
    }

    @Test
    void checkInFailsWhenVehicleNotRegistered() {
        when(vehicleRepo.findById(PLATE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> parkingService.checkIn(new CheckInRequest(PLATE, LOT_ID)));
        verify(lotRepo, never()).tryOccupy(anyString());               // no space was taken
    }

    @Test
    void checkInFailsWhenLotNotFound() {
        when(vehicleRepo.findById(PLATE)).thenReturn(Optional.of(vehicle()));
        when(lotRepo.findById(LOT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> parkingService.checkIn(new CheckInRequest(PLATE, LOT_ID)));
    }

    @Test
    void checkInFailsWhenVehicleAlreadyParked() {
        when(vehicleRepo.findById(PLATE)).thenReturn(Optional.of(vehicle()));
        when(lotRepo.findById(LOT_ID)).thenReturn(Optional.of(lot("2.00")));
        when(sessionRepo.existsByActivePlate(PLATE)).thenReturn(true);  // already parked

        ConflictException ex = assertThrows(ConflictException.class,
                () -> parkingService.checkIn(new CheckInRequest(PLATE, LOT_ID)));

        assertEquals("ALREADY_PARKED", ex.getCode());
        verify(lotRepo, never()).tryOccupy(anyString());
    }

    @Test
    void checkInFailsWhenLotIsFull() {
        when(vehicleRepo.findById(PLATE)).thenReturn(Optional.of(vehicle()));
        when(lotRepo.findById(LOT_ID)).thenReturn(Optional.of(lot("2.00")));
        when(sessionRepo.existsByActivePlate(PLATE)).thenReturn(false);
        when(lotRepo.tryOccupy(LOT_ID)).thenReturn(0);                 // 0 rows updated = full

        ConflictException ex = assertThrows(ConflictException.class,
                () -> parkingService.checkIn(new CheckInRequest(PLATE, LOT_ID)));

        assertEquals("LOT_FULL", ex.getCode());
        verify(sessionRepo, never()).saveAndFlush(any());              // nothing was saved
    }

    // ---------- check-out ----------

    @Test
    void checkOutBillsRoundedUpMinutes() {
        when(sessionRepo.findByActivePlate(PLATE)).thenReturn(Optional.of(activeSession("2.50")));
        when(clock.instant()).thenReturn(T0.plusSeconds(7 * 60 + 10)); // left after 7m10s
        when(sessionRepo.close(eq(1L), eq(SessionStatus.COMPLETED), any(), any(), eq(SessionStatus.ACTIVE)))
                .thenReturn(1);

        CheckOutResponse res = parkingService.checkOut(new CheckOutRequest(PLATE));

        assertEquals(8, res.durationMinutes());                        // 7m10s → 8 minutes
        assertEquals(new BigDecimal("20.00"), res.totalCost());        // 8 × 2.50
        assertEquals(SessionStatus.COMPLETED, res.status());
        verify(lotRepo).release(LOT_ID);                               // space was freed
    }

    @Test
    void checkOutIsCappedAt15Minutes() {
        when(sessionRepo.findByActivePlate(PLATE)).thenReturn(Optional.of(activeSession("1.00")));
        when(clock.instant()).thenReturn(T0.plusSeconds(40 * 60));     // left after 40 minutes
        when(sessionRepo.close(eq(1L), eq(SessionStatus.COMPLETED), any(), any(), eq(SessionStatus.ACTIVE)))
                .thenReturn(1);

        CheckOutResponse res = parkingService.checkOut(new CheckOutRequest(PLATE));

        assertEquals(15, res.durationMinutes());
        assertEquals(T0.plusSeconds(15 * 60), res.checkOutAt());
    }

    @Test
    void checkOutFailsWhenNotParked() {
        when(sessionRepo.findByActivePlate(PLATE)).thenReturn(Optional.empty());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> parkingService.checkOut(new CheckOutRequest(PLATE)));

        assertEquals("NOT_PARKED", ex.getCode());
    }

    @Test
    void checkOutFailsWhenSchedulerClosedItFirst() {
        when(sessionRepo.findByActivePlate(PLATE)).thenReturn(Optional.of(activeSession("1.00")));
        when(clock.instant()).thenReturn(T0.plusSeconds(5 * 60));
        when(sessionRepo.close(any(), any(), any(), any(), any())).thenReturn(0);   // lost the race

        ConflictException ex = assertThrows(ConflictException.class,
                () -> parkingService.checkOut(new CheckOutRequest(PLATE)));

        assertEquals("NOT_PARKED", ex.getCode());
        verify(lotRepo, never()).release(anyString());                 // space NOT freed twice
    }

    // ---------- auto-removal ----------

    @Test
    void autoRemoveBillsExactly15Minutes() {
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(activeSession("2.00")));
        when(sessionRepo.close(any(), any(), any(), any(), any())).thenReturn(1);

        assertTrue(parkingService.autoRemove(1L));

        verify(sessionRepo).close(1L, SessionStatus.AUTO_REMOVED,
                T0.plusSeconds(15 * 60), new BigDecimal("30.00"), SessionStatus.ACTIVE);
        verify(lotRepo).release(LOT_ID);
    }

    @Test
    void autoRemoveSkipsAlreadyClosedSession() {
        ParkingSession closed = activeSession("2.00");
        closed.setStatus(SessionStatus.COMPLETED);
        when(sessionRepo.findById(1L)).thenReturn(Optional.of(closed));

        assertFalse(parkingService.autoRemove(1L));
        verifyNoInteractions(lotRepo);                                 // nothing released
    }

    // ---------- helpers ----------

    private Vehicle vehicle() {
        Vehicle v = new Vehicle();
        v.setLicensePlate(PLATE);
        return v;
    }

    private ParkingLot lot(String ratePerMinute) {
        ParkingLot l = new ParkingLot();
        l.setLotId(LOT_ID);
        l.setCapacity(10);
        l.setCostPerMinute(new BigDecimal(ratePerMinute));
        return l;
    }

    private ParkingSession activeSession(String ratePerMinute) {
        ParkingSession s = new ParkingSession();
        s.setId(1L);
        s.setVehicle(vehicle());
        s.setLot(lot(ratePerMinute));
        s.setCheckInAt(T0);
        s.setStatus(SessionStatus.ACTIVE);
        s.setActivePlate(PLATE);
        return s;
    }
}