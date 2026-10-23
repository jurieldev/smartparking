package com.smartpark.scheduler;

import com.smartpark.entity.parking.ParkingSession;
import com.smartpark.entity.parking.SessionStatus;
import com.smartpark.repository.ParkingSessionRepository;
import com.smartpark.service.ParkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCheckoutScheduler {

    private final ParkingSessionRepository sessionRepo;
    private final ParkingService parkingService;   // separate bean → each autoRemove gets its own transaction
    private final Clock clock;

    @Value("${parking.max-duration-minutes}")
    private long maxDurationMinutes;

    @Scheduled(fixedDelayString = "${parking.auto-checkout-interval-ms}",
               initialDelayString = "${parking.auto-checkout-interval-ms}")
    public void removeExpiredSessions() {
        Instant cutoff = clock.instant().minus(Duration.ofMinutes(maxDurationMinutes));
        List<ParkingSession> expired =
                sessionRepo.findExpiredSessions(SessionStatus.ACTIVE, cutoff);

        int removed = 0;
        for (ParkingSession s : expired) {
            String plateId = s.getActivePlate();
            try {
                if (parkingService.autoRemove(s.getId())) {
                    removed++;
                    log.info("Car {} is removed ", plateId);
                }
            } catch (Exception e) {
                log.warn("Auto-removal failed for session {}", s.getId(), e);
            }
        }
        if (removed > 0) {
            log.info("Auto-removed {} vehicle(s) parked longer than {} min", removed, maxDurationMinutes);
        }
    }
}