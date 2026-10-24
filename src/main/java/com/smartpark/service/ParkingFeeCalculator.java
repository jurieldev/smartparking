package com.smartpark.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Component
public class ParkingFeeCalculator {

    private final Duration maxDuration;

    public ParkingFeeCalculator(@Value("${parking.max-duration-minutes}") long maxDurationMinutes) {
        this.maxDuration = Duration.ofMinutes(maxDurationMinutes);
    }

    /** When billing stops: the actual time, capped at check-in + max duration. */
    public Instant billingEnd(Instant checkInAt, Instant checkOutAt) {
        Instant cappedTimeLimit = cappedTimeLimit(checkInAt);
        return cappedTimeLimit.isBefore(checkOutAt) ? cappedTimeLimit : checkOutAt;
    }

    /** End time used for auto-removed sessions. */
    public Instant cappedTimeLimit(Instant checkInAt) {
        return checkInAt.plus(maxDuration);
    }

    /** Ceil to whole minutes, minimum 1. */
    public long billedMinutes(Instant from, Instant to) {
        long seconds = Duration.between(from, to).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    public BigDecimal cost(BigDecimal costPerMinute, long minutes) {
        return costPerMinute.multiply(BigDecimal.valueOf(minutes)).setScale(2, RoundingMode.HALF_UP);
    }
}
