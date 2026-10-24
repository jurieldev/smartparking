package com.smartpark.service;

import com.smartpark.common.error.ConflictException;
import com.smartpark.dto.parking.CheckInRequest;
import com.smartpark.entity.lot.ParkingLot;
import com.smartpark.entity.vehicle.Vehicle;
import com.smartpark.entity.vehicle.VehicleType;
import com.smartpark.repository.ParkingLotRepository;
import com.smartpark.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "parking.auto-checkout-interval-ms=3600000")   // keep the scheduler quiet
class ParkingConcurrencyTest {

    private static final int RUNNERS = 10;

    @Autowired ParkingService parkingService;
    @Autowired ParkingLotRepository lotRepo;
    @Autowired VehicleRepository vehicleRepo;

    @Test
    void tenCarsRaceForOneSpace_onlyOneGetsIn() throws Exception {
        ParkingLot lot = saveLot(1);                                  // one medal: capacity 1
        List<Callable<String>> runners = new ArrayList<>();
        for (int i = 0; i < RUNNERS; i++) {
            String plate = saveVehicle();                             // 10 different cars
            runners.add(() -> tryCheckIn(plate, lot.getLotId()));
        }

        List<String> results = race(runners);

        assertEquals(1, count(results, "OK"));
        assertEquals(RUNNERS - 1, count(results, "LOT_FULL"));
        assertEquals(1, occupied(lot));
    }

    @Test
    void oneCarRacesIntoTenLots_onlyOneSucceeds() throws Exception {
        String plate = saveVehicle();                                 // one car
        List<ParkingLot> lots = new ArrayList<>();
        List<Callable<String>> runners = new ArrayList<>();
        for (int i = 0; i < RUNNERS; i++) {
            ParkingLot lot = saveLot(5);                              // 10 different lots
            lots.add(lot);
            runners.add(() -> tryCheckIn(plate, lot.getLotId()));
        }

        List<String> results = race(runners);

        assertEquals(1, count(results, "OK"));                        // parked in exactly one lot
        int totalOccupied = lots.stream().mapToInt(this::occupied).sum();
        assertEquals(1, totalOccupied);                               // losers' +1 was rolled back
    }

    // ---------- the race ----------

    /** Lines every runner up on a virtual thread, fires the starting gun, collects the results. */
    private List<String> race(List<Callable<String>> runners) throws Exception {
        CountDownLatch startingGun = new CountDownLatch(1);
        List<Future<String>> finishLine = new ArrayList<>();

        try (ExecutorService track = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Callable<String> runner : runners) {
                finishLine.add(track.submit(() -> {
                    startingGun.await();                              // crouch at the line
                    return runner.call();                             // run
                }));
            }
            startingGun.countDown();                                  // bang!

            List<String> results = new ArrayList<>();
            for (Future<String> result : finishLine) {
                results.add(result.get(15, TimeUnit.SECONDS));        // collect each result
            }
            return results;
        }                                                             // track closes here
    }

    /** "OK", the conflict code (e.g. LOT_FULL, ALREADY_PARKED), or the exception name. */
    private String tryCheckIn(String plate, String lotId) {
        try {
            parkingService.checkIn(new CheckInRequest(plate, lotId));
            return "OK";
        } catch (ConflictException e) {
            return e.getCode();
        } catch (Exception e) {
            return e.getClass().getSimpleName();                      // e.g. DataIntegrityViolationException
        }
    }

    // ---------- helpers ----------

    private long count(List<String> results, String value) {
        return results.stream().filter(value::equals).count();
    }

    private int occupied(ParkingLot lot) {
        return lotRepo.findById(lot.getLotId()).orElseThrow().getOccupiedSpaces();
    }

    private ParkingLot saveLot(int capacity) {
        ParkingLot lot = new ParkingLot();
        lot.setLotId("RACE-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setLocation("Race test");
        lot.setCapacity(capacity);
        lot.setOccupiedSpaces(0);
        lot.setCostPerMinute(new BigDecimal("1.00"));
        return lotRepo.save(lot);
    }

    private String saveVehicle() {
        Vehicle v = new Vehicle();
        v.setLicensePlate("R" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        v.setType(VehicleType.CAR);
        v.setOwnerName("Race Tester");
        return vehicleRepo.save(v).getLicensePlate();
    }
}